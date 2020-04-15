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

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * RequireHttpsFilter will redirect any request made without HTTPS to the
 * same URL, having replaced the presumed "http" prefix with "https".
 */
public class RequireHttpsFilter
  extends    BasicFilter
{

  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "hRqS";
  
  //
  // Member variables.
  //

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
    log.info("RequireHttpsFilter loaded.");
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
    //debug("URI: " + request.getRequestURL() + "; " + request.isSecure());
    
    // If the request is secure, we'll move along.  Otherwise redirect if 
    // the prefix is http:.  If the prefix is anything other than http:, log 
    // an error and chain anyway.
    if (!request.isSecure())
    {
      String url = request.getRequestURL().toString();
      String query = request.getQueryString();
      if (query == null)
      {
        query = "";
      }
      else
      {
        query = "?" + query;
      }
      
      if (url.toLowerCase().startsWith("http:"))
      {
        url = "https:" + url.substring(5);
        response.sendRedirect(url + query);
        return;
      }
      else
      {
        log.info("Cannot parse insecure request from {}", url);
      }
    }
    
    // Default behavior is to just proceed.
    chain.doFilter(request, response);
  }
  
}  // End RequireHttpsFilter.

