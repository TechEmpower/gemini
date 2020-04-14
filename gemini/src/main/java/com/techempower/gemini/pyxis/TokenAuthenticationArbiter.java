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
 * TokenAuthenticationArbiter is an abstract representation of a token arbier.
 * <p>
 * TokenAuthenticationArbiter provides default implementations for most of the
 * methods in the PyxisAuthenticationArbiter interface, but subclasses should
 * override to add implementation-specific functionality.
 * <p>
 * TokenAuthenticationArbiter designates a user in the logged in state if an 
 * authentication token is provided, provides a signature valid on the payload, 
 * and falls within the validation (inside expiry window, etc).
 */
public abstract class TokenAuthenticationArbiter
    implements PyxisAuthenticationArbiter
{
  //
  // CONSTANTS
  //
  
  public static final String ACCESS_TOKEN = "access_token";
  
  //
  // PROTECTED VARIABLES
  //
  
  protected final GeminiApplicationInterface application;

  //
  // CONSTRUCTOR
  //
  
  public TokenAuthenticationArbiter(GeminiApplicationInterface application)
  {
    this.application = application;
  }

  //
  // PUBLIC METHODS
  //

  @Override
  public void beginMasquerade(Context context, PyxisUser impersonatedUser)
  {
    final AuthToken token = getTokenReader().read(context);
    
    if(token != null)
    {
      token.beginMasquerade(impersonatedUser.getId());

      context.delivery().put(ACCESS_TOKEN, token.tokenize());
    }
  }

  @Override
  public boolean endMasquerade(Context context)
  {
    final AuthToken token = getTokenReader().read(context);
    
    if(token != null && token.isMasquerading())
    {
      token.endMasquerade();
      return true;
    }
    
    return false;
  }

  @Override
  public PyxisUser getMasqueradingUser(Context context)
  {
    return getUser(context);
  }

  @Override
  public PyxisUser getUser(Context context)
  {
    final AuthToken token = getTokenReader().read(context);
      
    if(token != null)
    {
      return this.application.getSecurity().getUser(token.getUserId());
    }
    
    return null;
  }

  @Override
  public boolean isLoggedIn(Context context)
  {
    return getUser(context) != null;
  }

  @Override
  public void login(Context context, PyxisUser user, boolean save)
  {
    final AuthToken token = getTokenCreator().create(context, user);
    
    if(token != null)
    {
      context.delivery().put(ACCESS_TOKEN, token.tokenize());
    }
    
    // Save is ignored because login using auth tokens requires saving the
    // token on the client by default. Additionally, cookies are not supported
    // by any TokenAuthenticationArbiter for security reasons by default, so we 
    // do not set one regardless of the save flag.
  }

  @Override
  public void logout(Context context)
  {
    final AuthToken token = getTokenReader().read(context);
    
    if(token != null)
    {
      token.invalidate();
    }
  }
  
  //
  // ABSTRACT METHODS
  //
  
  /**
   * Returns a reference to the token creator.
   */
  public abstract TokenCreator getTokenCreator();
  
  /**
   * Return a reference to the token reader.
   */
  public abstract TokenReader getTokenReader();

}
