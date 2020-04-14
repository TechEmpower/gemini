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

import java.security.MessageDigest;
import java.time.*;
import java.util.*;

import com.techempower.gemini.*;

import io.jsonwebtoken.*;

import static com.techempower.gemini.HttpRequest.HEADER_AUTHORIZATION;

/**
 * Basic implementation of TokenReader which checks headers and post body
 * data for a bearer token.
 * <p>
 * A token will be found if included as an Authorization header with a bearer
 * of the token.
 * <p>
 * Example: Authorization: Bearer [token]
 */
public class JsonWebTokenReader
  implements TokenReader
{
  //
  // CONSTANTS
  //
  private static final String AUTHORIZATION_BEARER = "Bearer ";
  
  //
  // PRIVATE VARIABLES
  //
  
  private final GeminiApplicationInterface application;

  //
  // CONSTRUCTOR
  //
  
  public JsonWebTokenReader(GeminiApplicationInterface application)
  {
    this.application = application;
  }

  //
  // PUBLIC METHODS
  //
  
  @Override
  public JsonWebToken read(Context context)
  {
    String serialized = context.headers().get(HEADER_AUTHORIZATION);
    
    if (serialized != null && serialized.contains(AUTHORIZATION_BEARER))
    {
      serialized = serialized.substring(AUTHORIZATION_BEARER.length());

      try
      {
        final JsonWebToken token = new JsonWebToken(application, serialized);
        
        final LocalDateTime issued = LocalDateTime.ofInstant(
          new Date(token.getIssuedAt()).toInstant(),
          ZoneId.systemDefault());
        
        // Note: if the issued date is before the configured expiry amount, then
        // the token must be considered expired.
        if (issued.isAfter(LocalDateTime.now().minusDays(
            application.getSecurity().getSettings().getAuthTokenExpiryDays())))
        {
          final PyxisUser user = this.application.getSecurity().getUser(
              token.getUserId());
          // Note: we check the last time the user changed her password; if
          // it is equal to the value present in the token, then the user has
          // not changed her password since issuance of the token. However, if
          // the user has changed her password, then this token is invalid.
          final LocalDateTime lastPwdChanged = LocalDateTime.ofInstant(
              Instant.ofEpochMilli(token.getUserLastPasswordChange()),
              ZoneId.systemDefault());
          // Note: we check the validationHash of the user which is created
          // at the time of first login in order to allow manual logout.
          final Collection<Login> logins = application.getSecurity()
              .getUserLogins(user.getId());
          Login session = null;
          for(Login login : logins)
          {
            if (MessageDigest.isEqual(login.getValidationHash().getBytes(),
                token.getUserValidationHash().getBytes()))
            {
              session = login;
            }
          }
          if (lastPwdChanged.isEqual(LocalDateTime.ofInstant(
                user.getUserLastPasswordChange().toInstant(), 
                ZoneId.systemDefault()))
              && session != null
              && session.getValidationHash().equals(token.getUserValidationHash())
              && session.getCreated().getTime() == token.getIssuedAt())
          {
            return token;
          }
        }
      }
      catch(MalformedJwtException | IllegalArgumentException se)
      {
        // Cannot do anything about this on the server; this means the auth
        // token is not valid (very likely due to the client munging data and
        // sending it back to the server maliciously).
      }
    }
    
    return null;
  }
}
