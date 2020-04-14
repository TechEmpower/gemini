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
import com.techempower.gemini.handler.*;
import com.techempower.util.*;

/**
 * An implementation of Handler that checks to see if a user's session is
 * active before handling a request.  This class helps keep users who have not
 * logged in from accessing portions of the site.
 *    <p>
 * SecureHandler works in conjunction with a Pyxis user model and the
 * LoginHandler found in this package.
 *    <p>
 * Use SecureHandler by subclassing it and implementing your acceptRequest and
 * handleRequest methods as such: 1) acceptRequest remains as it would in
 * a regular Handler.  Check to see if the command is something your Handler
 * is interested in.  2) handleRequest first needs to check with the
 * SecureHandler (the superclass) to see if it accepts the request.  If the
 * SecureHandler does not accept the request (that is to say that a user is
 * not logged in), ask the SecureHandler to handle the request by calling
 * super.handleRequest.  This forces the SecureHandler to redispatch the
 * request to the LoginHandler.  See the example code below:
 *    <p>
 * Note that any subclass of BasicSecureHandler does not need to add itself
 * to the Configurator's list of configurable components.  Doing so will cause
 * the subclass to be configured twice since it is already added by this 
 * parent class.
 *
 * <pre><br>
 *  if (super.acceptRequest(dispatcher, context, command))
 *  {
 *    if (command.equalsIgnoreCase(CMD_MY_COMMAND))
 *    {
 *      return handleMyCommand(dispatcher, context);
 *    }
 *  }
 *  else
 *  {
 *    return super.handleRequest(dispatcher, context, command);
 *  }
 *  return false;
 * </pre><br>
 *
 * @see PyxisSecurity
 */
public class BasicSecureHandler<D extends BasicDispatcher, C extends BasicContext>
     extends SecureHandler<D,C>
  implements Configurable
{  
  //
  // Constants.
  //

  public static final String LOCAL_COMPONENT_CODE = "hSec";

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public BasicSecureHandler(GeminiApplication application)
  {
    super(application);
    application.getConfigurator().addConfigurable(this);
  }

  /**
   * Configures the public commands.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    setPublicCommands(props.getArray(this.getConfigurationPrefix() + "PublicCommands"));
  }

  /**
   * Gets a description of the handler.
   */
  @Override
  public String getDescription()
  {
    return "Provides user authentication check before accepting.  Typically subclassed.";
  }

  /**
   * Gets the desired thread priority of this handler.  Default should be
   * Handler.PRIORITY_NO_CHANGE.
   *
   * @see java.lang.Thread
   */
  @Override
  public int getPriority()
  {
    return Handler.PRIORITY_NO_CHANGE;
  }

}   // End SecureHandler.
