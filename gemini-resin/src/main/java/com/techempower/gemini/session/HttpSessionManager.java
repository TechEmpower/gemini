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
package com.techempower.gemini.session;

import javax.servlet.http.*;

import com.techempower.gemini.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the creation of user session objects.  Initializes new sessions 
 * to the proper timeout, etc.
 *   <p>
 * The Context class uses SessionManager to create sessions.  This allows 
 * for any necessary initialization to happen on all new sessions.
 *   <p>
 * Reads the following configuration options from the .conf file:
 *    <ul>
 * <li>SessionTimeout - Timeout for sessions in seconds.  Default: 3600.
 * <li>StrictSessions - Attempts to prevent session hijacking by hashing
 *     request headers provided at the start of each session with those
 *     received on each subsequent request; resetting the session in the event
 *     of a mismatch.
 * <li>RefererTracking - Captures the HTTP "referer" (sic) request header
 *     provided when a session is new.
 *    </ul>
 */
public class HttpSessionManager
  implements SessionManager
{
  //
  // Constants.
  //

  public static final int    DEFAULT_TIMEOUT    = 3600;      // One hour
  public static final String SESSION_HASH       = "Gemini-Session-Hash";

  //
  // Member variables.
  //

  private int     timeoutSeconds     = DEFAULT_TIMEOUT;
  private Logger  log = LoggerFactory.getLogger(getClass());
  private boolean refererTracking    = false;
  private long              sessionAccumulator = 0L;
  private boolean           strictSessions     = false;

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public HttpSessionManager(GeminiApplication application)
  {
    application.getConfigurator().addConfigurable(this);
  }

  /**
   * Configure this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    setTimeoutSeconds(props.getInt("SessionTimeout", DEFAULT_TIMEOUT));
    log.info("Session timeout: {} seconds.", getTimeoutSeconds());
    
    refererTracking  = props.getBoolean("RefererTracking", refererTracking);
    if (refererTracking)
    {
      log.info("Referer tracking enabled.");
    }
    strictSessions   = props.getBoolean("StrictSessions", strictSessions);
    if (strictSessions)
    {
      log.info("Scrict sessions enabled.");
    }
  }

  /**
   * Sets the session timeout in minutes.  Note: only future sessions will be 
   * affected.
   */
  public void setTimeoutMinutes(int minutes)
  {
    timeoutSeconds = minutes * 60;
  }

  /**
   * Sets the session timeout in seconds.  Note: only future sessions will be 
   * affected.
   */
  public void setTimeoutSeconds(int seconds)
  {
    timeoutSeconds = seconds;
  }
  
  /**
   * Gets the session timeout in seconds.
   */
  @Override
  public int getTimeoutSeconds()
  {
    return timeoutSeconds;
  }

  @Override
  public Session getSession(Request request, boolean create)
  {
    return getSession((HttpRequest)request, create);
  }

  /**
   * Sets up an HttpSession.  This should not be called directly.  Use
   * Context.getSession() instead.  Places a semi-unique session ID into
   * the session under the value named "Gemini-Session-ID".  This session
   * ID is based off the sequence number (which is reset to 0 whenever the
   * application starts) and the current time.
   * @param create Force the creation of a new session.
   */
  private HttpSession getSession(HttpRequest request, boolean create)
  {
    HttpSession session = HttpSession.createSession(request.getRawRequest().getSession(create));

    if (session == null)
    {
      return null;
    }
    
    // Track if the session is new.
    boolean newSession = session.isNew();
    
    // If strict sessions is enabled, we will check additional headers of
    // each request to verify that the requests continue to appear from the
    // same browser over time to help counter session hijacking.
    if (strictSessions)
    {
      if (newSession)
      {
        // For a new session, generate a request hash for future comparisons.
        int requestHash = getRequestHash(request.getRawRequest());
        session.setAttribute(SESSION_HASH, "" + requestHash);
        //log.debug("Stored request hash as session hash: " + requestHash);
      }
      else
      {
        // For an existing session, compare a hash of the current request
        // versus what we have in the session.  If they don't match, clear
        // the session, which will result in the user needing to re-
        // authenticate.
        int requestHash = getRequestHash(request.getRawRequest());
        int sessionHash = 0;
        try
        {
          sessionHash = Integer.parseInt((String)session.getAttribute(SESSION_HASH));
        }
        catch (Exception exc)
        {
          // Do nothing.
        }
        
        // If the request hash doesn't match what we have on record within
        // the session, then this may be a different browser.
        if (requestHash != sessionHash)
        {
          //log.debug("Request hash: " + requestHash);
          //log.debug("Session hash: " + sessionHash);
          log.info("Session hash mismatch.  Invalidating session {}",
              session.getId());
          session.invalidate();
          
          // Probably not necessary to request a new session, but we'll do
          // so anyway.
          session = HttpSession.createSession(request.getRawRequest().getSession(true));
          
          //log.debug("New session: " + session);
          session.setAttribute(SESSION_HASH, "" + requestHash);
          
          // Session requires "new session" attributes.
          newSession = true;
        }
      }
    }

    // If this is a new session, set it up.
    if (newSession)
    {
      session.setMaxInactiveInterval(timeoutSeconds);

      session.setAttribute(GeminiConstants.SESSION_ID_NAME, constructSessionID());
      //log.debug("Session ID: " + session.getAttribute(SESSION_ID_NAME));
      
      // If we're tracking referrers, grab the referrer when the session is
      // first created.
      //log.debug("request.getHeader: " + request.getHeader("referer"));
      if ( (refererTracking)
        && (request.getHeader("referer") != null)
        )
      {
        //log.debug("Setting referrer.");
        session.setAttribute(GeminiConstants.SESSION_REFERER, request.getHeader("referer"));
      }
    }

    return session;
  }
  
  /**
   * Hashes a few request attributes together to determine a decent 
   * representation of the user's client.  This is used for the strict session
   * feature.
   */
  protected int getRequestHash(HttpServletRequest request)
  {
    String toHash = ""
      + request.getHeader("User-Agent")
      + request.getHeader("Accept-Charset")
      + request.getHeader("Keep-Alive");
    return toHash.hashCode();
  }

  /**
   * Constructs a semi-"unique" 13-digit session ID for the session based 
   * off of the session's sequence number and the current time.
   */
  protected String constructSessionID()
  {
    StringBuilder sessionID = new StringBuilder(13);
    
    // The first 5 digits are the session sequence number, as provided by
    // the session accumulator.
    sessionAccumulator++;
    String sessionSequenceNumber = Long.toString(sessionAccumulator, Character.MAX_RADIX);
    for (int i = sessionSequenceNumber.length(); i < 5; i++)
    {
      sessionID.append('0');
    }
    sessionID.append(sessionSequenceNumber);
    
    // The next 8 digits are the current time in milliseconds.
    String timeStamp = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
    for (int i = timeStamp.length(); i < 8; i++)
    {
      sessionID.append('0');
    }
    sessionID.append(timeStamp);

    return sessionID.toString();
  }
}
