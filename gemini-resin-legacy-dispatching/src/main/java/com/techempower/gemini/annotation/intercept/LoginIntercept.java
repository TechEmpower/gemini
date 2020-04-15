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
package com.techempower.gemini.annotation.intercept;

import java.lang.annotation.*;
import java.lang.reflect.*;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;

/**
 * LoginIntercept is an implementation of Intercept that checks
 * to see if a user is logged in through GeminiSecurity
 *   
 * If the user is not logged in, they are returned to the login command to 
 * handle this.
 */
public class LoginIntercept
  implements HandlerIntercept<BasicDispatcher, Context>
{
  private final GeminiApplication application;
  //private ComponentLog log;
  
  public LoginIntercept(GeminiApplication application)
  {
    this.application = application;
    //this.log = application.getLog("lInt");
  }
  
  /**
   * Checks to see if the user is logged in.
   */
  @Override
  public boolean intercept(Method m, BasicDispatcher dispatcher, Context context, String command, Annotation annotation)
  {
    final PyxisSecurity security = application.getSecurity();
    return security != null && !security.isLoggedIn(context);
  }

  /**
   * Passes the request off to the roled login handler. By default this is "login"
   */
  @Override
  @SuppressWarnings("unchecked")
  public boolean handleIntercept(Method m, BasicDispatcher dispatcher, Context context, String command, Annotation annotation)
  {
    // dispatcher to the login command
    Handler<BasicDispatcher, Context> handler = 
      (Handler<BasicDispatcher, Context>)dispatcher.getRoledHandler(((RequireLogin)annotation).value());
    if (handler != null)
    {
      return handler.handleRequest(dispatcher, context, command);
    }
    
    // there's no roled login handler, so we can't pass off the request, returning false because we can't
    // handle this request
    return false;
  }

}
