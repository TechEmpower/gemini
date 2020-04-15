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

package com.techempower.gemini.handler;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.helper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subclasses of SecureHandler may easily disallow requests from 
 * unauthenticated users.
 */
public abstract class SecureHandler<D extends BasicDispatcher, C extends Context>
    implements Handler<D, C>
{
  
  //
  // Member variables.
  //

  private final GeminiApplication application;
  private final Logger            log = LoggerFactory.getLogger(getClass());
  private final PyxisSecurity     security;

  private String[] publicCommands = new String[0];
  
  /**
   * Constructor.
   */
  public SecureHandler(GeminiApplication application)
  {
    this.application = application;
    this.security = application.getSecurity();
  }

  @Override
  public abstract String getDescription();
  
  @Override
  public abstract int getPriority();

  /**
   * SecureHandler's implementation of acceptRequest should be called by
   * the subclass's acceptRequest.  SecureHandler will only accept the
   * request if the user is logged in or if the command is declared as public
   * in the configuration file.
   */
  @Override
  public boolean acceptRequest(D dispatcher, C context, String command)
  {
    return (  (this.isPublic(command))
           || (security.isLoggedIn(context))
           );
  }

  /**
   * A subclass should implement the handleRequest method normally.  Directing
   * a request to SecureHandler's handleRequest method will force the user
   * to login.  See the class documentation above for more information.
   */
  @Override
  public boolean handleRequest(D dispatcher, C context, String command)
  {
    security.getForceLoginRejector().reject(context, null);
    return true;
  }
  
  /**
   * Gets the application reference.
   */
  protected GeminiApplication getApplication()
  {
    return this.application;
  }
  
  /**
   * Gets the application's security.
   */
  protected PyxisSecurity getSecurity()
  {
    return this.security;
  }

  /**
   * Gets the public commands array.
   */
  public String[] getPublicCommands()
  {
    return this.publicCommands;
  }

  /**
   * Sets the public commands array.
   */
  public void setPublicCommands(String[] publicCommands)
  {
    this.publicCommands = publicCommands;
  }

  /**
   * If the given command should be allowed regardless of login state, then true will be returned.
   *
   * @param command The command to check
   * @return Whether the command is public
   */
  protected boolean isPublic(String command)
  {
    return this.publicCommands.length != 0 && StringHelper.equalsIgnoreCase(
        command, this.publicCommands);
  }

  /**
   * Gets the prefix of configuration variables meant for this handler.
   * By default this is set to "SecureHandler." and will be global across
   * all SecureHandlers. To define separate variables for your handler override
   * this method.
   */
  protected String getConfigurationPrefix()
  {
    return "SecureHandler.";
  }
}
