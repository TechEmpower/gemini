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
package com.techempower.gemini.simulation;

import com.techempower.gemini.*;

/**
 * An implementation of the {@link Cookie} interface for simulated cookies.
 */
public class SimCookie
  implements Cookie
{
  private String  name;
  private String  value;
  private String  path;
  private int     age;
  private boolean secure;

  /**
   * Constructs a new simulated cookie with the given parameters.
   *
   * @param name the name of the cookie
   * @param value the value of the cookie
   * @param path the path of the cookie
   * @param age the age of the cookie
   * @param secure whether the cookie should be marked secure
   */
  public SimCookie(String name, String value, String path, int age, boolean secure)
  {
    this.name = name;
    this.value = value;
    this.path = path;
    this.age = age;
    this.secure = secure;
  }

  @Override
  public String getValue()
  {
    return this.value;
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  /**
   * Returns the path of this cookie.
   *
   * @return the path of this cookie
   */
  public String getPath()
  {
    return this.path;
  }

  /**
   * Returns the age of this cookie.
   *
   * @return the age of this cookie
   */
  public int getAge()
  {
    return this.age;
  }

  /**
   * Returns {@code true} if this cookie is secure.
   *
   * @return {@code true} if this cookie is secure
   */
  public boolean isSecure()
  {
    return this.secure;
  }
}
