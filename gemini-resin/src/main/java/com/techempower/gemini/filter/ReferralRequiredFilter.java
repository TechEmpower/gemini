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
 * ReferralRequiredFilter simply checks to see that an HTTP "Referer" header
 * is set appropriately before serving content.
 */
public class ReferralRequiredFilter
  extends    BasicFilter
{

  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "hExF";
  
  //
  // Member variables.
  //

  private String requiredReferrerValue = "";
  private boolean hashRequired = false;

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
    requiredReferrerValue = getInitParameter(config, "Required", "");
    hashRequired = getInitParameter(config, "HashRequired", false);
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
    // Get the referrer header.
    String referrer = request.getHeader("Referer");  // Yes, that is the spelling.
    if (referrer == null)
    {
      referrer = "";
    }
    log.info("Referrer: {}", referrer);
    
    if (referrer.startsWith(requiredReferrerValue))
    {
      if ( (!hashRequired)
        || (goodHash(request))
        )
      {
        // Good to go.
        chain.doFilter(request, response);
      }
      else
      {
        doError(request, response, 2, "Incorrect hash.");
      }
    }
    else
    {
      doError(request, response, 1, "Incorrect referrer.");
    }
  }
  
  /**
   * Determines if a valid hash was provided as an 'a' request variable.
   */
  protected boolean goodHash(HttpServletRequest request)
  {
    final String context = request.getContextPath();
    final String uri = request.getRequestURI();
    final String filename = uri.substring(context.length());
    final String hashValue = Integer.toString(Math.abs(filename.hashCode()), 36).toLowerCase();
    final String providedHash = request.getParameter("a");
    log.info("File: {}; desired hash: {}; received hash: {}",
        filename, hashValue, providedHash);
    return hashValue.equals(providedHash);
  }
  
}  // End ReferralRequiredFilter.

