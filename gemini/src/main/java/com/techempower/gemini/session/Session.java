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

import java.util.*;

/**
 * A simple abstraction of an HTTP session, with a few common getters and
 * setters.  The purpose of this interface is to allow real HTTP sessions and
 * simulated ones to implement a common API.
 */
public interface Session
{
  /**
   * Returns {@code true} if the session was created on this request.
   *
   * @return {@code true} if the session was created on this request
   */
  boolean isNew();

  /**
   * Returns the names of all session attributes.
   *
   * @return the names of all session attributes
   */
  Enumeration<String> getAttributeNames();

  /**
   * Returns the value of the session attribute with the given name.
   *
   * @param name the name of the attribute to be returned
   * @return the value of the session attribute with the given name
   */
  Object getAttribute(String name);

  /**
   * Returns the maximum interval for which this session may be inactive before
   * it can be terminated.
   *
   * @return the maximum interval for which this session may be inactive before
   *         it can be terminated
   */
  int getMaxInactiveInterval();

  /**
   * Sets the value of the session attribute with the given name.
   *
   * @param name the name of the attribute to set
   * @param o the value of the attribute
   */
  void setAttribute(String name, Object o);

  /**
   * Sets the value of the session attribute with the given name.
   * Wraps the object in your deployment's specific session listener proxy class
   *
   * @param name the name of the attribute to set
   * @param o the value of the attribute
   */
  void setAttribute(String name, SessionListener o);

  /**
   * Removes the session attribute with the given name.
   *
   * @param name the name of the attribute to be removed
   */
  void removeAttribute(String name);

  /**
   * Returns the id of this session.
   *
   * @return the id of this session
   */
  String getId();

  /**
   * Invalidates this session.
   */
  void invalidate();

  /**
   * Sets the maximum interval for which this session may be inactive before it
   * can be terminated.
   *
   * @param timeout the value of the interval
   */
  void setMaxInactiveInterval(int timeout);
}
