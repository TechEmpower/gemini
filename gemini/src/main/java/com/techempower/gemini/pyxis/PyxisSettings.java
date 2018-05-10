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

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * PyxisSettings stores configuration options pertaining to security from the
 * application configuration file and makes these settings available to the
 * PyxisSecurity implementation.  Most applications will use a subclass of
 * BasicSecurity, so the configuration prefix shown below is "BasicSecurity."
 * If your application provides a different configuration prefix when
 * constructing the PyxisSettings object, adjust your configuration file
 * accordingly.
 *   <p>
 * Configuration options:
 * <ul>
 * <li>BasicSecurity.AuthenticateByEmail - Enables the option to allow users
 *     to authenticate by their email address in addition to by their 
 *     username.</li>
 * <li>BasicSecurity.UpdateLastLogin - Determines whether or not users'
 *     last login date will be automatically set when they login.  The
 *     default is yes.</li>
 * <li>BasicSecurity.PasswordExpirationDays - How many days should a 
 *     password be valid?  The default value of 0 disables this functionality
 *     wholly.</li>
 * <li>BasicSecurity.PasswordEarlyWarningDays - How many days before 
 *     expiration should a reminder be sent to the user?  The default value of
 *     0 disables the early warning function specifically.</li>
 * <li>BasicSecurity.NoPasswordChangeNeededUri - A URI to redirect to if 
 *     the user visits one of the password expiration URIs when a change is 
 *     not needed.  Default is /.</li>
 * <li>BasicSecurity.InvalidateSessionAtLogout - Should a user's session be
 *     invalidated when they logout?  Default is yes.</li>
 * </ul>
 *
 * @see BasicSecurity
 */
public class PyxisSettings
  implements Configurable
{

  //
  // Default values.
  //

  public static final int DEFAULT_EXPIRATION_DAYS    = 0;
  public static final int DEFAULT_EARLY_WARNING_DAYS = 0;

  //
  // Member variables.
  //

  private final String propertiesPrefix;

  private boolean authenticateViaEmail          = false;
  private boolean updateLastLogin               = true;
  private boolean invalidateSessionAtLogout     = true;
  private int     expirationDays                = DEFAULT_EXPIRATION_DAYS;
  private int     earlyWarningDays              = DEFAULT_EARLY_WARNING_DAYS;
  private boolean allowMultipleSessions         = true;
  private boolean cookieLoginEnabled            = true;
  private boolean cookieLoginSslOnly            = true;
  private boolean storeUserAsID                 = false;
  private boolean logoutDeletesCookie           = true;
  private boolean allowCookieAuthToken          = false;
  private long    authTokenExpiryDays           = 30;
  private int     accessControlMaxAge           = 86400;
  private boolean accessControlAllowCredentials = false;
  private String  macSigningKey                 = null;
  
  private final Set<String> accessControlAllowedOrigins = new HashSet<>();
  private final Set<String> accessControlAllowedHeaders = new HashSet<>();
  private final Set<String> accessControlExposedHeaders = new HashSet<>();
  
  //
  // Member methods.
  //

  /**
   * Default constructor.  Uses the default "Pyxis." prefix.
   * 
   * @param application the application reference.
   */
  public PyxisSettings(GeminiApplication application)
  {
    this(application, PyxisConstants.PYXIS_PREFIX);
  }

  /**
   * Constructor.
   * 
   * @param application the application reference.
   * @param prefix the property value name prefix to use for reading 
   *        configuration settings.
   */
  public PyxisSettings(GeminiApplication application, String prefix)
  {
    this.propertiesPrefix = prefix;
  }   
  
  /**
   * Gets the authenticate-via-email option.  If enabled, implementations
   * of PyxisSecurity should allow users to authenticate by providing either
   * their username or email address.
   */
  public boolean isEmailAuthenticationEnabled()
  {
    return this.authenticateViaEmail;
  }
  
  /**
   * Should users' last login date be updated on login?
   */
  public boolean isLastLoginUpdate()
  {
    return this.updateLastLogin;
  }
  
  /**
   * Should users' sessions be invalidated when they logout?
   */
  public boolean isInvalidateSessionAtLogout()
  {
    return this.invalidateSessionAtLogout;
  }
  
  /**
   * Gets the configured password expiration length in days.
   */
  public int getPasswordExpirationDays()
  {
    return expirationDays;
  }
  
  /**
   * Gets the configured early warning days prior to a password expiration.
   */
  public int getEarlyWarningDays()
  {
    return earlyWarningDays;
  }
  
  /**
   * Returns whether the application allows multiple sessions.
   */
  public boolean allowsMultipleSessions()
  {
    return allowMultipleSessions;
  }
  
  /**
   * Returns whether a logout deletes cookies.
   */
  public boolean logoutDeletesCookie()
  {
    return logoutDeletesCookie;
  }
  
  /**
   * Whether cookie login is enabled.
   */
  public boolean isCookieLoginEnabled()
  {
    return cookieLoginEnabled;
  }

  /**
   * Whether cookie login is SSL only.
   */
  public boolean isCookieLoginSslOnly()
  {
    return cookieLoginSslOnly;
  }
  
  /**
   * Whether the user is stored as an id.
   */
  public boolean storeUserAsId()
  {
    return storeUserAsID;
  }
  
  /**
   * Whether the application allows cookie-based auth token authentication.
   */
  public boolean allowCookieAuthToken()
  {
    return allowCookieAuthToken;
  }
  
  /**
   * Whether the application allows credentials to be sent in CORS requests.
   */
  public boolean accessControlAllowCredentials()
  {
    return accessControlAllowCredentials;
  }
  
  /**
   * The number of days before auth tokens expire after issuing.
   */
  public long getAuthTokenExpiryDays()
  {
    return authTokenExpiryDays;
  }
  
  /**
   * Gets the configured access control allowed origin(s).
   */
  public Set<String> getAccessControlAllowedOrigins()
  {
    return accessControlAllowedOrigins;
  }
  
  /**
   * Gets the access control allowed header(s).
   */
  public Set<String> getAccessControlAllowedHeaders()
  {
    return accessControlAllowedHeaders;
  }

  /**
   * Gets the access control exposed header(s).
   */
  public Set<String> getAccessControlExposedHeaders()
  {
    return accessControlExposedHeaders;
  }
  
  /**
   * Gets the max age of the access control preflight OPTIONS requests.
   */
  public int getAccessControlMaxAge()
  {
    return accessControlMaxAge;
  }
  
  public String getMacSigningKey()
  {
    return macSigningKey;
  }
  
  /**
   * Gets the configured URI to direct users to when they request one of the
   */

  @Override
  public void configure(EnhancedProperties props)
  {
    final EnhancedProperties.Focus focus = props.focus(propertiesPrefix);
    
    authenticateViaEmail = focus.getBoolean("AuthenticateByEmail", false);
    updateLastLogin = focus.getBoolean("UpdateLastLogin", true);
    invalidateSessionAtLogout = focus.getBoolean("InvalidateSessionAtLogout", true);
    expirationDays = focus.getInt("PasswordExpirationDays", 
        DEFAULT_EXPIRATION_DAYS, 0, Integer.MAX_VALUE);
    earlyWarningDays = focus.getInt("PasswordEarlyWarningDays", 
        DEFAULT_EARLY_WARNING_DAYS, 0, expirationDays); // No greater than expirationDays.
    allowMultipleSessions = focus.getBoolean("AllowMultipleSessions", allowMultipleSessions);
    storeUserAsID = focus.getBoolean("StoreUserAsID", storeUserAsID);
    cookieLoginEnabled = focus.getBoolean("CookieLoginEnabled", cookieLoginEnabled);
    cookieLoginSslOnly = focus.getBoolean("CookieLoginSSLOnly", cookieLoginSslOnly);
    logoutDeletesCookie = focus.getBoolean("LogoutDeletesCookie", logoutDeletesCookie);
    allowCookieAuthToken = focus.getBoolean("AllowCookieAuthToken", allowCookieAuthToken);
    authTokenExpiryDays = focus.getLong("AuthTokenExpiryDays", authTokenExpiryDays);
    accessControlAllowedOrigins.addAll(Arrays.asList(StringHelper.splitTrimAndLower(focus.get("AccessControlAllowedOrigins", null), "\\s+")));
    accessControlAllowedHeaders.addAll(Arrays.asList(StringHelper.splitTrimAndLower(focus.get("AccessControlAllowedHeaders", null), "\\s+")));
    accessControlExposedHeaders.addAll(Arrays.asList(StringHelper.splitTrimAndLower(focus.get("AccessControlExposedHeaders", null), "\\s+")));
    accessControlMaxAge = focus.getInt("AccessControlMaxAge", accessControlMaxAge);
    accessControlAllowCredentials = focus.getBoolean("AccessControlAllowCredentials", accessControlAllowCredentials);
    macSigningKey = focus.get("MacSigningKey", macSigningKey);
  }
  
}   // End PyxisSettings
