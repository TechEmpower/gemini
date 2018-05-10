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
import com.techempower.helper.*;

/**
 * An interface for working with a Context's Cookies.  A reference is fetched
 * via context.cookies().
 */
public class Cookies
{

  private final Context context;
  
  /**
   * Constructor.
   */
  public Cookies(Context context)
  {
    this.context = context;
  }
  
  /**
   * Gets a cookie from the request by name.
   *
   * @param name the cookie name
   *
   * @return A cookie object or null if no such cookie is found
   */
  public <C extends Cookie> C get(String name)
  {
    return context.getRequest().getCookie(name);
  }

  /**
   * Gets a cookie's <i>value</i> from the request by cookie-name.
   *
   * @param name the cookie-name
   *
   * @return A String representation of the value of the cookie or
   *         null if no such cookie is found
   */
  public String getValue(String name)
  {
    final Cookie returnedCookie = get(name);

    return (returnedCookie != null)
        ? returnedCookie.getValue()
        : null;
  }

  /**
   * Tests to see if the cookie with the given name is set to the given value.
   * If no cookie exists with that name false is returned.
   *
   * @param name Name of the cookie
   * @param value Value expected for the cookie
   * 
   * @return If the actual value of the cookie matches the given value.
   */
  public boolean test(String name, String value)
  {
    return StringHelper.equals(value, getValue(name));
  }

  /**
   * Deletes a cookie from the user's browser.  This is achieved by setting
   * the cookie's lifetime to 0.  Uses the Servlet's path as the cookie's 
   * path.
   *
   * @param name the cookie's name
   */
  public Cookies remove(String name)
  {
    remove(name, context.getInfrastructure().getUrl());
    
    return this;
  }

  /**
   * Deletes a cookie from the user's browser.  This is achieved by setting 
   * the cookie's lifetime to 0.  Send empty path String to not specify a 
   * path for the cookie.
   *
   * @param name the cookie's name
   * @param path the server path to which the cookie applies.
   */
  public Cookies remove(String name, String path)
  {
    context.getRequest().deleteCookie(name, path);
    
    return this;
  }
  
  /**
   * Sends a cookie to the response.
   *
   * @param responseCookie the ResponseCookie to send to the client.
   */
  public Cookies put(ResponseCookie responseCookie)
  {
    context.getRequest().setCookie(
        responseCookie.getName(), 
        responseCookie.getValue(),
        responseCookie.getDomain(),
        responseCookie.getPath(),
        responseCookie.getAge(),
        responseCookie.isSecure()
        );
    
    return this;
  }

}
