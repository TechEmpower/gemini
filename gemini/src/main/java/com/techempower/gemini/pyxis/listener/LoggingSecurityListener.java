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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simply logs calls to the listener methods.  Does nothing else.
 */
public class LoggingSecurityListener
  implements SecurityListener<Context>
{
  
  //
  // Constants.
  //
  
  public static final String COMPONENT_CODE       = "NoSL";
  
  //
  // Member variables.
  //
  
  private final Logger log = LoggerFactory.getLogger(COMPONENT_CODE);
  
  //
  // Member methods.
  //

  @Override
  public void loginFailed(Context context)
  {
    log.info("Login failed: {}", context);
  }

  @Override
  public void loginSuccessful(Context context, PyxisUser user)
  {
    log.info("Login successful: {}", user);
  }

  @Override
  public void logoutSuccessful(Context context, PyxisUser user)
  {
    if (context == null)
    {
      log.info("Logout by session expiration: {}", user);
    }
    else
    {
      log.info("Logout successful: {}", user);
    }
  }

  @Override
  public void passwordChanged(PasswordProposal proposal)
  {
    log.info("User {} changed password.", proposal.username);
  }

}