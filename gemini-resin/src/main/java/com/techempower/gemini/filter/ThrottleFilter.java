/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.techempower.gemini.filter;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.helper.*;

/*
  To use, put this in your web.xml:

  <filter>
    <filter-name>ThrottleFilter</filter-name>
    <filter-class>com.techempower.gemini.filters.ThrottleFilter</filter-class>
    <init-param>
      <param-name>MaxConcurrentRequests</param-name>
      <param-value>2</param-value>
    </init-param>
    <init-param>
      <param-name>UnthrottledUserAgents</param-name>
      <param-value>Googlebot,msnbot,Yahoo! Slurp,FeedBurner,nagios</param-value>
    </init-param>
    <init-param>
      <param-name>BannedUserAgents</param-name>
      <param-value>fairshare</param-value>
    </init-param>
    <init-param>
      <param-name>UnthrottledUris</param-name>
      <param-value>/images/,/css/,/js/,/favicon.ico</param-value>
    </init-param>
    <init-param>
      <param-name>DebugEnabled</param-name>
      <param-value>false</param-value>
    </init-param>
  </filter>
  <filter-mapping>
    <filter-name>ThrottleFilter</filter-name>
     <url-pattern>/*</url-pattern>
  </filter-mapping>
*/

/**
 * Limits concurrent requests, except for configured User-Agents and URIs that
 * we don't want to throttle.
 */
public class ThrottleFilter
     extends BasicFilter
{
  public  static final String  PARAM_CLIENT_IP          = "ClientIP";
  private final ConcurrentHashMap<String, AtomicInteger> addressCache      
              = new ConcurrentHashMap<>();
  private int                  maxConcurrentRequests    = 2;
  private String[]             welcomeIpAddressPatterns = null;
  private String[]             welcomeAbuserUserAgents  = null;
  private String[]             welcomeAbuserUris        = null;
  private String[]             bannedUserAgents         = null;
  private String[]             bannedIpAddressPatterns  = null;

  @Override
  public void init(FilterConfig config)
  {
    super.init(config);
    maxConcurrentRequests = (int)getInitParameter(config, "MaxConcurrentRequests",
      maxConcurrentRequests);
    log.info("max concurrent requests: {}", maxConcurrentRequests);
    
    String unthrottledIpAddressPatterns = getInitParameter(config, "UnthrottledIpAddressPatterns", null);
    welcomeIpAddressPatterns = StringHelper.splitTrimAndLower(unthrottledIpAddressPatterns, ",");
    if (CollectionHelper.isNonEmpty(welcomeIpAddressPatterns))
    {
      for (int i = 0; i < welcomeIpAddressPatterns.length; i++)
      {
        log.info("will not throttle this IP address pattern: {}", welcomeIpAddressPatterns[i]);
      }
    }
    else
    {
      log.info("will throttle all IP address patterns.");
    }

    String unthrottledUserAgentConfig = getInitParameter(config,
      "UnthrottledUserAgents", null);
    welcomeAbuserUserAgents = StringHelper.splitTrimAndLower(unthrottledUserAgentConfig, ",");
    if (CollectionHelper.isNonEmpty(welcomeAbuserUserAgents))
    {
      for (int i = 0; i < welcomeAbuserUserAgents.length; i++)
      {
        log.info("will not throttle this User-Agent: {}", welcomeAbuserUserAgents[i]);
      }
    }
    else
    {
      log.info("will throttle all User-Agents.");
    }

    String bannedUserAgentConfig = getInitParameter(config,
      "BannedUserAgents", null);
    bannedUserAgents = StringHelper.splitTrimAndLower(bannedUserAgentConfig, ",");
    if (CollectionHelper.isNonEmpty(bannedUserAgents))
    {
      for (int i = 0; i < bannedUserAgents.length; i++)
      {
        log.info("will ban this User-Agent: {}", bannedUserAgents[i]);
      }
    }
    else
    {
      log.info("will not ban any User-Agents.");
    }

    String bannedIpAddressConfig = getInitParameter(config, "BannedIpAddressPatterns", null);
    bannedIpAddressPatterns = StringHelper.splitTrimAndLower(bannedIpAddressConfig, ",");
    if (CollectionHelper.isNonEmpty(bannedIpAddressPatterns))
    {
      for (int i = 0; i < bannedIpAddressPatterns.length; i++)
      {
        log.info("will ban this IP address pattern: {}", bannedIpAddressPatterns[i]);
      }
    }
    else
    {
      log.info("will not ban any IP address patterns.");
    }

    String unthrottledUriConfig = getInitParameter(config, "UnthrottledUris", null);
    welcomeAbuserUris = StringHelper.splitTrimAndLower(unthrottledUriConfig, ",");
    if (CollectionHelper.isNonEmpty(welcomeAbuserUris))
    {
      for (int i = 0; i < welcomeAbuserUris.length; i++)
      {
        log.info("will not throttle this URI: {}", welcomeAbuserUris[i]);
      }
    }
    else
    {
      log.info("will throttle all URIs.");
    }
  }

  @Override
  protected void filter(HttpServletRequest request, HttpServletResponse response,
    FilterChain chain) throws ServletException, IOException
  {
    String ipAddress = request.getRemoteAddr();
    
    // The WordPress MU front end for BrowseMyStuff makes requests to Java
    // BMS. Since all of its requests come from the same IP (localhost) it
    // would normally be swiftly throttled. WordPress will pass along the
    // original client IP and User-Agent in request headers. When present, we
    // use those instead.
    final String clientIp = request.getHeader(PARAM_CLIENT_IP);
    if (clientIp != null && clientIp.length() > 0)
    {
      ipAddress = clientIp;
    }
    
    final String userAgent = request.getHeader("User-Agent");
    final String requestSignature = getRequestSignature(request);

    log.debug("ipAddress: {}", ipAddress);
    log.debug("User-Agent: {}", userAgent);
    log.debug("Request sig: {}", requestSignature);
    
    // If the IP address pattern is approved, then don't do any throttling.
    if (isWelcomeIpAddressPattern(ipAddress))
    {
      log.debug("{} is an approved IP address pattern.", ipAddress);
      chain.doFilter(request, response);
      return;
    }

    // A banned IP will be banned regardless of User-Agent or request URI.
    boolean bannedIp = isBannedIpAddress(ipAddress);

    // If the User-Agent is approved, then don't do any throttling.
    if (!bannedIp && isWelcomeAbuser(userAgent))
    {
      log.debug("{} has approved User-Agent: {}", ipAddress, userAgent);
      chain.doFilter(request, response);
      return;
    }
    
    // If the URI is approved, then don't do any throttling.
    if (!bannedIp && isWelcomeAbuserUri(requestSignature))
    {
      log.debug("{} has approved URI: {}", ipAddress, requestSignature);
      chain.doFilter(request, response);
      return;
    }

    if (bannedIp)
    {
      log.debug("{} has banned IP address", ipAddress);
    }
    else if (isBannedUserAgent(userAgent))
    {
      log.debug("{} has banned User-Agent: {}", ipAddress, userAgent);
    }
    else
    {
      int count = 0;
      
      // Our current request count.
      AtomicInteger atomic = addressCache.get(ipAddress);

      // No previous record of this IP address.
      if (atomic == null)
      {
        // Try to add a new atomic.
        if ((atomic = addressCache.putIfAbsent(ipAddress, new AtomicInteger(1))) != null)
        {
          // We purposefully don't call increment yet.
          count = atomic.get();
        }
        // For whatever reason, we got null back from putIfAbsent.  Let's
        // assume a count of 1.
        else
        {
          // Atomic was null => the put was successful but for reasons unknown,
          // what the javadocs suggest will happen did not - that object reference
          // was not returned; let's get it and ensure that atomic is not null.
          atomic = addressCache.get(ipAddress);
          count = 1;
        }
      }
      else
      {
        // We purposefully don't call increment yet.
        count = atomic.get() + 1;
      }

      // Would this request still be below the maximum concurrent requests
      // permitted?
      if (count < maxConcurrentRequests)
      {
        try
        {
          // Okay, this is an honest to goodness request
          // which isn't to be throttled; increment.
          count = atomic.incrementAndGet();
          // You can go about your business. Move along.
          chain.doFilter(request, response);
          
          return;
        }
        catch (Exception e)
        {
          log.info("Exception while filtering.", e);
        }
        finally
        {
          // Only proceed if we have an atomic reference.
          if (atomic != null)
          {
            count = atomic.decrementAndGet();
            if (count <= 0)
            {
              addressCache.remove(ipAddress);
            }
          }
        }
      }
    }
    
    // We don't increment here because this request
    // is going to get throttled and return immediately.
    log.info("{} - {} - {} - throttling request for {}",
        System.currentTimeMillis(), StringHelper.padSpace(ipAddress, 15),
        userAgent, requestSignature);
    
    response.sendError(503);
  }
  
  /**
   * Is the given IP address in our list of IP address patterns
   * that we never throttle?
   */
  private boolean isWelcomeIpAddressPattern(String ipAddress)
  {
    if (ipAddress != null)
    {
      final String lower = ipAddress.toLowerCase();          
      for (int i = 0; i < welcomeIpAddressPatterns.length; i++)
      {
        if (lower.contains(welcomeIpAddressPatterns[i]))
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Is the given User-Agent in our list of abusers that we welcome and never
   * throttle?
   * <p>
   * Note: Case sensitive.
   */
  private boolean isWelcomeAbuser(String userAgent)
  {
    if (userAgent != null)
    {
      final String lower = userAgent.toLowerCase();
      for (int i = 0; i < welcomeAbuserUserAgents.length; i++)
      {
        if (lower.contains(welcomeAbuserUserAgents[i]))
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Is the given User-Agent in our list of abusers that are always throttled?
   * <p>
   * Note: Case insensitive.
   */
  private boolean isBannedUserAgent(String userAgent)
  {
    if (userAgent != null)
    {
      final String lower = userAgent.toLowerCase();
      for (int i = 0; i < bannedUserAgents.length; i++)
      {
        if (lower.contains(bannedUserAgents[i]))
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Is the given IP address in our list of abusers that are always throttled?
   */
  private boolean isBannedIpAddress(String ipAddress)
  {
    if (ipAddress != null)
    {
      final String lower = ipAddress.toLowerCase();
      for (int i = 0; i < bannedIpAddressPatterns.length; i++)
      {
        if (lower.contains(bannedIpAddressPatterns[i]))
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Is the given URI in our list of abusers that we welcome and never
   * throttle?
   * <p>
   * Note: Case sensitive.
   */
  private boolean isWelcomeAbuserUri(String uri)
  {
    if (uri != null)
    {
      final String lower = uri.toLowerCase();
      for (int i = 0; i < welcomeAbuserUris.length; i++)
      {
        if (lower.contains(welcomeAbuserUris[i]))
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Get the request signature for filtering purposes.
   */
  public String getRequestSignature(HttpServletRequest request)
  {
    String queryString = request.getQueryString();
    if (StringHelper.isNonEmpty(queryString))
    {
      return request.getRequestURL() + "?" + queryString;
    }
    else
    {
      return request.getRequestURL().toString();
    }
  }
}
