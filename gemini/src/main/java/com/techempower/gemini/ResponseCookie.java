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
package com.techempower.gemini;

/**
 * Represents a new Cookie to be delivered to the client via the HTTP 
 * Response.
 *   <p>
 * This class was introduced to reduce some of the method signatures in
 * Context, effectively moving that bit of complexity to this class.
 */
public class ResponseCookie
{
  
  //
  // Variables.
  //
  
  private final String  name;
  private final String  value;
  private       boolean secure  = false;
  private       int     age     = GeminiConstants.DEFAULT_COOKIE_MAX_AGE;
  private       String  path    = "";
  private       String  domain  = "";
  
  /**
   * Constructor.  Builds a ResponseCookie taking the two required parameters:
   * name and value.  Set methods can be chained immediately following the
   * constructor to set the optional parameters.
   *
   * @param name the cookie's name
   * @param value the cookie's value
   */
  public ResponseCookie(String name, String value)
  {
    this.name = name;
    this.value = value;
  }
  
  /**
   * Sets the secure flag.  If secure, the cookie should only be sent by
   * the client when the request is secure (HTTPS).
   */
  public ResponseCookie setSecure(boolean secure)
  {
    this.secure = secure;
    return this;
  }
  
  /**
   * Sets the cookie's age or lifetime, in seconds.  This is the amount of 
   * time, in seconds, that the cookie should persist on the client.
   */
  public ResponseCookie setAge(int age)
  {
    this.age = age;
    return this;
  }
  
  /**
   * Sets the URL path for which the cookie applies.  Leave blank to not 
   * restrict the cookie to a path.
   */
  public ResponseCookie setPath(String path)
  {
    this.path = path;
    return this;
  }
  
  /**
   * Sets the domain cookie.
   */
  public ResponseCookie setDomain(String domain)
  {
    this.domain = domain;
    return this;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the value
   */
  public String getValue()
  {
    return value;
  }

  /**
   * @return the secure
   */
  public boolean isSecure()
  {
    return secure;
  }

  /**
   * @return the age
   */
  public int getAge()
  {
    return age;
  }

  /**
   * @return the path
   */
  public String getPath()
  {
    return path;
  }
  
  /**
   * @return the domain
   */
  public String getDomain()
  {
    return domain;
  }
  
}
