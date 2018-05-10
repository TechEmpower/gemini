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
package com.techempower.gemini.context;

import com.techempower.gemini.*;

/**
 * An interface to a Context's request and response headers.  A reference
 * is fetched via context.headers().
 */
public class Headers
{

  private final Request request;
  
  /**
   * Constructor.
   */
  public Headers(Context context)
  {
    this.request = context.getRequest();
  }

  /**
   * Puts a response header.  This is a pass-through to response.setHeader.
   *
   * @param headerName the header name (e.g., "Content-disposition")
   * @param value the header value (e.g., "attachment; filename=blah.txt")
   */
  public Headers put(String headerName, String value)
  {
    request.setResponseHeader(headerName, value);
    
    return this;
  }
  
  /**
   * Put the Cache-Control response header.
   */
  public Headers cacheControl(String value)
  {
    put("Cache-Control", value);
    return this;
  }

  /**
   * Get a request header.  This is a convenience method to request.getHeader.
   */
  public String get(String name)
  {
    return request.getHeader(name);
  }
  
  /**
   * Gets a request header, returning a default value if the header was
   * not provided.
   */
  public String get(String name, String defaultValue)
  {
    final String value = get(name);
    return value != null
        ? value
        : defaultValue;
  }
  
  /**
   * Gets the referring URL via the "Referer" (sic) request header.
   */
  public String referrer()
  {
    return get("Referer");
  }
  
  /**
   * Gets the user agent via the "User-Agent" request header.
   */
  public String userAgent()
  {
    return get("User-Agent");
  }
  
  /**
   * Gets the acceptable context types via the "Accept" request header.
   */
  public String accept()
  {
    return get("Accept");
  }
  
  /**
   * Gets the host portion of the URL via the "Host" request header.
   */
  public String host()
  {
    return get("Host");
  }

  /**
   * Sets the "Expires" response header.  Provide a time-out period in 
   * seconds.  This can be useful with caching application servers in 
   * scenarios where the contents of an invoked page are not specific to a 
   * user and their content can be cached for a while.
   */
  public Headers expires(int secondsFromNow)
  {
    this.request.setExpiration(secondsFromNow);
    return this;
  }

}
