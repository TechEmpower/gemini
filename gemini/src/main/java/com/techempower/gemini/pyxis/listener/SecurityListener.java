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

package com.techempower.gemini.pyxis.listener;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.password.*;

/**
 * An interface for being notified of user logins, logouts, and authentication
 * failures.  Applications can implement this interface and use BasicSecurity.
 * addListener to receive notification of these events.
 */
public interface SecurityListener<C extends Context>
{
  
  /**
   * The provided user has successfully logged in.
   */
  void loginSuccessful(C context, PyxisUser user);
  
  /**
   * The provided user has successfully logged out.  If the context is null,
   * the logout has occurred as a result of the session expiring rather than
   * as a result of user action.
   */
  void logoutSuccessful(C context, PyxisUser user);
  
  /**
   * The provided Context provided a failed login attempt.
   */
  void loginFailed(C context);

  /**
   * As a listener attached to an application's Security, the listener
   * receives a notification that a user's password has been changed via
   * this method.  The parameter is a PasswordProposal as received by the
   * Security.
   *   <p>
   * Listeners are called <b>after</b> the security has successfully changed
   * the password.  This method will not be called if the proposal did not
   * meet security requirements.
   */
  void passwordChanged(PasswordProposal proposal);

}
