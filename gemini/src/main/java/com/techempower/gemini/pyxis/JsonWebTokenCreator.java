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

import java.security.*;
import java.time.*;
import java.util.*;

import com.techempower.cache.*;
import com.techempower.gemini.*;
import com.techempower.scheduler.*;

/**
 * Basic implementation of TokenCreator which simply randomly sets up the 
 * user's validation hash and creates a JWT-compliant token.
 */
public class JsonWebTokenCreator
  implements TokenCreator
{
  //
  // PRIVATE VARIABLES
  //
  private final GeminiApplicationInterface application;
  private final EntityStore                store;
  private final Random                     random;

  //
  // CONSTRUCTOR
  //

  public JsonWebTokenCreator(GeminiApplicationInterface application)
  {
    this.application = application;
    this.store = application.getStore();
    this.random = new SecureRandom();
    this.application.getScheduler().scheduleEvent(
        new LoginPurgeEvent("LoginPurgeEvent",
            "Daily event for purging expired Logins",0,0));
  }

  //
  // PUBLIC METHODS
  //

  @Override
  public JsonWebToken create(Context context, PyxisUser user)
  {
    // Create a new Login for the user
    final Login login = new Login();
    final byte[] randomBytes = new byte[32];
    random.nextBytes(randomBytes);
    // Set the randomly generated validation hash
    login.setValidationHash(Base64.getEncoder().encodeToString(randomBytes));
    login.setCreated(new Date());
    login.setIpAddress(context.getClientId());
    // Persist the user's validation hash
    store.put(login);
    // Map the user to the login
    application.getSecurity().addUserLogin(user.getId(), login);
    
    return new JsonWebToken(application, user, login.getCreated().getTime(),
        login.getValidationHash());
  }

  
  //
  // PRIVATE CLASS
  //
  
  /**
   * LoginPurgeEvent exists because as JsonWebTokens are created by this class,
   * they will accumulate in the cache/database over time. Once they have
   * expired and the token is no longer considered valid, these Logins need to
   * be purged.
   */
  private class LoginPurgeEvent extends DailyEvent
  {
    
    //
    // CONSTRUCTOR
    //
    
    public LoginPurgeEvent(String name, String description, int hour,
        int minute)
    {
      super(name, description, hour, minute);
    }
    
    //
    // PUBLIC METHODS
    //

    @Override
    protected void doIt()
    {
      for(Login login: store.list(Login.class))
      {
        final LocalDateTime issued = LocalDateTime.ofInstant(
            login.getCreated().toInstant(), ZoneId.systemDefault());
        if (issued.isBefore(LocalDateTime.now().minusDays(
            application.getSecurity().getSettings().getAuthTokenExpiryDays())))
        {
          store.remove(login);
        }
      }
    }
  }
}
