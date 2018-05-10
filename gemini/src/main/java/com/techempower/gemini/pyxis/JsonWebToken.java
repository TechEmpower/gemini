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
import java.time.temporal.*;
import java.util.*;

import com.techempower.collection.*;
import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.crypto.*;
import com.techempower.helper.*;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.*;

/**
 * JsonWebToken provides an implementation of the Json Web Token specification.
 * 
 * @see <a href="tools.ietf.org/html/draft-ietf-oauth-json-web-token-15"
 * >Json Web Token Specification Draft</a>
 */
public class JsonWebToken
  implements AuthToken, MutableNamedValues
{
  //
  // CONSTANTS
  //
  
  // JWT: Private Claim Names
  private static final String BEARER_ID             = "bearerId";
  private static final String USER_ID               = "userId";
  private static final String LAST_PASSWORD_CHANGED = "lastPwdChanged";
  private static final String VALIDATION_HASH       = "validationHash";
  
  // JWT: Registered Claim Names
  public static final String ISSUER     = "iss";
  public static final String SUBJECT    = "sub";
  public static final String AUDIENCE   = "aud";
  public static final String EXPIRATION = "exp";
  public static final String NOT_BEFORE = "nbf";
  public static final String ISSUED_AT  = "iat";
  public static final String JWT_ID     = "jti";
  
  //
  // PRIVATE VARIABLES
  //
  
  private final GeminiApplicationInterface application;
  private final long                       lastPasswordChange;
  private final long                       bearerUserId;
  private final long                       issuedAt;
  private final String                     validationHash;
  private final Map<String,Object>         claims = new HashMap<>();

  // user id differs from bearerUserId in that it can be set over and over
  // again for the purposes of masquerading as a different user than the one
  // to whom this JsonWebToken belongs/represents. However, the bearerUserId
  // can only be set in the constructor to force immutability.
  private long userId;

  //
  // CONSTRUCTOR
  //

  /**
   * Constructor for creating a JsonWebToken for the given user.
   */
  public JsonWebToken(GeminiApplicationInterface application, PyxisUser user,
      long issuedAt, String validationHash)
  {
    this.application = application;
    this.issuedAt = issuedAt;
    bearerUserId = user.getId();
    lastPasswordChange = user.getUserLastPasswordChange().getTime();
    this.validationHash = validationHash;
    userId = bearerUserId;
  }
  
  /**
   * Constructor for creating a JsonWebToken from the given serialized token.
   */
  public JsonWebToken(GeminiApplicationInterface application, String serialized)
    throws MalformedJwtException, SignatureException, IllegalArgumentException
  {
    this.application = application;
    
    final DefaultClaims data = (DefaultClaims)Jwts.parser()
        .setSigningKey(
            application.getSecurity().getSettings().getMacSigningKey())
        .parse(serialized)
        .getBody();
    
    issuedAt = (long)data.get(ISSUED_AT);
    bearerUserId = NumberHelper.parseLong(data.get(BEARER_ID).toString());
    lastPasswordChange = (long)data.get(LAST_PASSWORD_CHANGED);
    validationHash = (String)data.get(VALIDATION_HASH);
    userId = NumberHelper.parseLong(data.get(USER_ID).toString());
  }

  //
  // PUBLIC METHODS
  //
  
  @Override
  public void beginMasquerade(long id)
  {
    userId = id;
  }

  @Override
  public JsonWebToken clear()
  {
    claims.clear();
    return this;
  }
  
  @Override
  public void endMasquerade()
  {
    userId = bearerUserId;
  }

  @Override
  public String get(String name)
  {
    return get(name, null);
  }

  @Override
  public String get(String name, String defaultValue)
  {
    String value = (String)claims.get(name);
    if(value == null)
    {
      value = defaultValue;
    }
    return value;
  }

  @Override
  public boolean getBoolean(String name)
  {
    return getBoolean(name, false);
  }

  @Override
  public boolean getBoolean(String name, boolean defaultValue)
  {
    Boolean value = (Boolean)claims.get(name);
    if(value == null)
    {
      value = defaultValue;
    }
    return value;
  }

  @Override
  public int getInt(String name)
  {
    return getInt(name, 0);
  }

  @Override
  public int getInt(String name, int defaultValue)
  {
    Integer value = (Integer)claims.get(name);
    if(value == null)
    {
      value = defaultValue;
    }
    return value;
  }

  @Override
  public int getInt(String name, int defaultValue, int minimum, int maximum)
  {
    return NumberHelper.boundInteger(getInt(name,defaultValue), minimum, maximum);
  }
  
  @Override
  public long getUserLastPasswordChange()
  {
    return lastPasswordChange;
  }

  @Override
  public long getLong(String name)
  {
    return getLong(name, 0L);
  }

  @Override
  public long getLong(String name, long defaultValue)
  {
    Long value = (Long)claims.get(name);
    if(value == null)
    {
      value = defaultValue;
    }
    return value;
  }

  @Override
  public long getLong(String name, long defaultValue, long minimum,
      long maximum)
  {
    return NumberHelper.boundLong(getLong(name, defaultValue), minimum, maximum);
  }
  
  /**
   * Returns the decrypted string associated with the given name.
   */
  public String getSecret(String name)
  {
    String toRet = get(name);
    if(toRet != null)
    {
      try
      {
        toRet = new String(
            application.getSecurity().getCryptograph().decrypt(
                Base64.getDecoder().decode(toRet.getBytes())));
      }
      catch(EncryptionError ee)
      {
        // This means we failed to decrypt; very likely it is because the value
        // for this key was not actually encrypted; swallow and return the 
        // actual value.
      }
    }
     
     return toRet;
  }

  @Override
  public long getIssuedAt()
  {
    return issuedAt;
  }

  @Override
  public long getUserId()
  {
    return userId;
  }
  
  @Override
  public String getUserValidationHash()
  {
    return validationHash;
  }

  @Override
  public boolean has(String name)
  {
    return claims.containsKey(name);
  }
  
  @Override
  public void invalidate()
  {
    final PyxisUser user = application.getSecurity().getUser(userId);
    
    if(user != null)
    {
      final Collection<Login> logins = application.getSecurity()
          .getUserLogins(user.getId());
      Login session = null;
      for(Login login : logins)
      {
        if (MessageDigest.isEqual(login.getValidationHash().getBytes(),
            validationHash.getBytes()))
        {
          session = login;
        }
      }
      // Removing the Login session will effectively invalidate the token.
      application.getSecurity().removeUserLogin(user.getId(), session);
    }
  }
  
  @Override
  public boolean isMasquerading()
  {
    return userId != bearerUserId;
  }

  @Override
  public Set<String> names()
  {
    return claims.keySet();
  }

  @Override
  public JsonWebToken put(String name, String value)
  {
    claims.put(name, value);
    return this;
  }

  @Override
  public JsonWebToken put(String name, int value)
  {
    claims.put(name, value);
    return this;
  }

  @Override
  public JsonWebToken put(String name, long value)
  {
    claims.put(name, value);
    return this;
  }

  @Override
  public JsonWebToken put(String name, boolean value)
  {
    claims.put(name, value);
    return this;
  }
  
  /**
   * Encrypts the given value using the Cryptograph provided by PyxisSecurity
   * and puts it into the claims at the given name.
   */
  public JsonWebToken putSecret(String name, String value)
  {
    claims.put(name, Base64.getEncoder().encodeToString(
        application.getSecurity().getCryptograph().encrypt(value.getBytes())));
    return this;
  }

  @Override
  public JsonWebToken remove(String name)
  {
    claims.remove(name);
    return this;
  }
  
  @Override
  public String tokenize()
  {
    final JwtBuilder builder = Jwts.builder()
        // Public claims
        .claim(ISSUED_AT, getIssuedAt())
        .claim(EXPIRATION, Instant.ofEpochMilli(getIssuedAt())
            .plus(
              application.getSecurity().getSettings().getAuthTokenExpiryDays(),
              ChronoUnit.DAYS)
            .toEpochMilli())
        // Private claims
        .claim(BEARER_ID, bearerUserId)
        .claim(USER_ID, getUserId())
        .claim(LAST_PASSWORD_CHANGED, getUserLastPasswordChange())
        .claim(VALIDATION_HASH, getUserValidationHash());
    
    // Make dynamic claims
    for(String key : claims.keySet())
    {
      builder.claim(key, claims.get(key));
    }
        
    return builder
        // Sign the token
        .signWith(SignatureAlgorithm.HS256, 
            application.getSecurity().getSettings().getMacSigningKey())
        // Encode and compact
        .compact();
  }

}
