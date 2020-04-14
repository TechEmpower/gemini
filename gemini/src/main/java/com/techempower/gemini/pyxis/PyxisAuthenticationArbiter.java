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
package com.techempower.gemini.pyxis;

import com.techempower.gemini.*;

/**
 * PyxisArbiter is a simple interface for handling direct user actions within
 * the context of a PyxisSecurity. The default implementation is named 
 * SessionArbiter.
 * 
 * @see SessionAuthenticationArbiter
 * @see JsonWebTokenAuthenticationArbiter
 */
public interface PyxisAuthenticationArbiter
{
  /**
   * Begin masquerading as another user.  For hopefully-obvious reasons,
   * implementations should limit this functionality to administrators.  Once
   * in a masquerade, calls to getUser() should return the impersonated user,
   * rather than the administrator, until endMasquerade is called.
   *   <p>
   * Logout will end the masquerade and logout the system administrator.
   */
  void beginMasquerade(BasicContext context, PyxisUser impersonatedUser);

  /**
   * Ends masquerading, returning to a default state where calls to getUser
   * will return the administrator themselves.  Returns true if an existing
   * masquerade has been ended; returns false if nothing was done.
   */
  boolean endMasquerade(BasicContext context);

  /**
   * Gets the masquerading user, if a masquerade is active.  If there is
   * no active masquerade, returns null.
   */
  PyxisUser getMasqueradingUser(BasicContext context);

  /**
   * Gets the logged-in user from the Context's session.  Returns null
   * if no user is logged in.
   *
   * @param context the Context from which to retrieve a user.
   */
  PyxisUser getUser(BasicContext context);

  /**
   * Is a user logged in?
   */
  boolean isLoggedIn(BasicContext context);
  
  /**
   * Logs the user into the application.
   * 
   * @param context the request context
   * @param user the user to login.
   * @param save if permitted, should a cookie be sent to save the credentials
   *        on the client?
   */
  void login(BasicContext context, PyxisUser user, boolean save);

  /**
   * Logout a user from the provided Context.
   */
  void logout(BasicContext context);
}
