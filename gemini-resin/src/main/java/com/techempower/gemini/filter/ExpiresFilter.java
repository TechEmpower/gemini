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
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.helper.*;
import com.techempower.text.*;
import com.techempower.util.*;

/**
 * ExpiresFilter adds Expires and Cache-Control headers to a response.
 */
public class ExpiresFilter
  extends    BasicFilter
{

  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "hExF";
  public static final long   RENDER_INTERVAL = UtilityConstants.SECOND * 15;
  
  //
  // Member variables.
  //

  private long              expirationDelta    = UtilityConstants.HOUR;  // 1 hour
  private int               expirationSeconds  = (int)(this.expirationDelta / UtilityConstants.SECOND);
  private long              nextRender         = 0L;
  private String            renderedExpiration = null;
  private SynchronizedSimpleDateFormat dateFormatter = new SynchronizedSimpleDateFormat(
      "EEE, dd MMM yyyy HH:mm:ss zzz");

  //
  // Member methods.
  //

  /**
   * Initializes the Filter aspect of this component.
   */
  @Override
  public void init(FilterConfig config)
  {
    super.init(config);
    setExpirationDelta(getInitParameter(config, "ExpirationDelta", expirationDelta));
    dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
  }
  
  /**
   * Handles a filtering request.
   */
  @Override
  protected void filter(HttpServletRequest request, HttpServletResponse response, 
                     FilterChain chain)
    throws IOException, 
           ServletException
  {
    final long now = System.currentTimeMillis();
    
    // Time to re-render the expiration?  We only do so every 15 seconds or
    // so in order to avoid contention for the formatter.
    if (  (now > nextRender)
       || (renderedExpiration == null)
       )
    {
      nextRender = now + RENDER_INTERVAL;
      
      final Calendar cal = DateHelper.getCalendarInstance(now + expirationDelta);
      renderedExpiration = dateFormatter.format(cal.getTime());
    }
    
    //debug(expirationTime);
    response.setHeader("Expires", renderedExpiration);
    response.setHeader("Cache-Control", "max-age=" + expirationSeconds + ", public");
    
    chain.doFilter(request, response);
  }
  
  /**
   * Sets the expiration time for requested resources to {@code expirationDelta}
   * milliseconds from the time of the request.
   */
  public void setExpirationDelta(long expirationDelta)
  {
    this.expirationDelta = expirationDelta;
    expirationSeconds  = (int)(expirationDelta / 1000L);
  }
  
}  // End ExpiresFilter.

