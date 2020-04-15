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

import java.sql.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.session.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TokenArbiter provides implementations for direct user actions within the
 * context of a PyxisSecurity.
 * <p>
 * SessionArbiter designates a user in the logged in state if there is a
 * cookie present that matches up with the session for that user.
 */
public class SessionAuthenticationArbiter 
  implements PyxisAuthenticationArbiter
{
  //
  // CONSTANTS
  //
  
  public static final String MULTISESSION_VIOLATION = "MultiSessionViolation";
  
  //
  // PRIVATE VARIABLES
  //
  
  private final GeminiApplication application;
  private final LoginTokenManager loginTokenManager;
  private final Map<Long,String>  userSessionIDs;
  private final Logger            log = LoggerFactory.getLogger(getClass());

  //
  // CONSTRUCTOR
  //
  
  public SessionAuthenticationArbiter(GeminiApplication application)
  {
    this.application = application;
    // Construct a login token manager for handling "remember me" and cookies.
    this.loginTokenManager = new LoginTokenManager(application);
    this.userSessionIDs = 
        Collections.synchronizedMap(new HashMap<Long,String>());
  }
  
  //
  // PUBLIC METHODS
  //
  
  @Override
  public void beginMasquerade(Context context, PyxisUser impersonatedUser)
  {
    final PyxisUser masqueradingUser = getUser(context);
    if (  (masqueradingUser != null)
       && (impersonatedUser != null)
       && (isMasqueradePermitted(masqueradingUser, impersonatedUser))
       )
    {
      log.info("{} now masquerading as {}",
          masqueradingUser, impersonatedUser);
      storeUserInSession(context, impersonatedUser, 
          PyxisConstants.SESSION_IMPERSONATED_USER);
    }
  }
  
  @Override
  public boolean endMasquerade(Context context)
  {
    final PyxisUser masquerading = getMasqueradingUser(context);
    if (masquerading != null)
    {
      log.info("{} ended masquerading.", masquerading);
      context.session().remove(PyxisConstants.SESSION_IMPERSONATED_USER);
      return true;
    }
    
    return false;
  }
  
  @Override
  public PyxisUser getMasqueradingUser(Context context)
  {
    if (getUserFromSession(context, PyxisConstants.SESSION_IMPERSONATED_USER)
        != null)
    {
      return getUserFromSession(context, PyxisConstants.SESSION_USER);
    }
    return null;
  }
  
  @Override
  public PyxisUser getUser(Context context)
  {
    final PyxisUser impersonated = getUserFromSession(context, 
        PyxisConstants.SESSION_IMPERSONATED_USER);
    
    if (impersonated != null)
    {
      return impersonated;
    }
    else
    {
      return getUserFromSession(context, PyxisConstants.SESSION_USER);
    }
  }

  /**
   * Gets the logged-in user from the session.  Returns null
   * if no user is logged in.
   *
   * @param session the HttpSession from which to retrieve a user.
   */
  public PyxisUser getUser(Session session)
  {
    final PyxisUser impersonated = getUserFromSession(session, 
        PyxisConstants.SESSION_IMPERSONATED_USER);
    
    if (impersonated != null)
    {
      return impersonated;
    }
    else
    {
      return getUserFromSession(session, PyxisConstants.SESSION_USER);
    }
  }
  
  @Override
  public boolean isLoggedIn(Context context)
  {
    final PyxisUser user = getUser(context);
    final boolean loggedIn = user != null;

    if (loggedIn && !application.getSecurity().getSettings().allowsMultipleSessions())
    {
      final String correctSessionID = userSessionIDs.get(user.getId());
      final String sessionID        = context.getSession(true).getId();

      if (correctSessionID != null)
      {
        if (  (!correctSessionID.equalsIgnoreCase("EXEMPT")) 
           && (!sessionID.equalsIgnoreCase(correctSessionID))
           )
        {
          // multiple sessions detected, kick this bozo.
          context.delivery().put(MULTISESSION_VIOLATION , true);
          context.session().clear();

          return false;
        }
      }
      else
      {
        // The session ID got knocked out somehow, so start using this session ID
        recordUserSessionId(context, user);
      }
    }
    
    if (!loggedIn)
    {
      return cookieLogin(context);
    }

    return loggedIn;
  }
  
  @Override
  public void login(Context context, PyxisUser user, boolean save)
  {
    // Logout an existing user session if one is present.
    final PyxisUser existingUserSession = getUser(context);
    if (existingUserSession != null)
    {
      logout(context);
    }
  
    storeUserInSession(context, user);

    // record the session id for tracking purposes.
    recordUserSessionId(context, user);

    // Save the cookie if requested and the current configuration allows
    // cookie-based logins to be saved.
    if (  (save)
       && (isCookieLoginPermitted(context))
       )
    {
      // Create the cookie.  If the automatic login cookie should
      // only be sent to secure connections, be sure to create it
      // as such.
      loginTokenManager.createAndPersistToken(context, 
          this.application.getSecurity().sanitizeUsername(user.getUserUsername()));
    }
  }
  
  @Override
  public void logout(Context context)
  {
    final PyxisUser user = getUser(context);
    
    removeUserFromSession(context);
    
    if (this.application.getSecurity().getSettings().isInvalidateSessionAtLogout())
    {
      context.session().invalidate();
    }

    // Delete the automatic login cookie if set.
    if (application.getSecurity().getSettings().logoutDeletesCookie())
    {
      loginTokenManager.clearCookie(context, user);
    }
  }
  
  /**
   * Processes a cookie-based login.
   */
  protected boolean cookieLogin(Context context)
  {
    // Proceed only if the configuration allows non-SSL cookie login or we're
    // in SSL.
    if (isCookieLoginPermitted(context))
    {
      // If the IP address is permitted to try, let's attempt to login.
      if (this.application.getSecurity().isLoginAttemptPermitted(context))
      {
        final LoginTokenManager.TokenValidation validation;
        try
        {
          validation = loginTokenManager.validateAndUpdateToken(context);
          
          // Don't log cookie login checks that are not actual attempts.
          if (validation.isAttempt())
          {
            log.debug("Cookie login attempt: {}", validation);
          }
        }
        catch (SQLException e)
        {
          log.warn("SQL exception while validating and updating token.", e);
          return false;
        }
        if (validation.isValid())
        {
          final PyxisUser user = this.application.getSecurity().getUser(
              validation.getUsername());

          // Attempt to login.
          if (this.application.getSecurity().login(context, user, false))
          {
            log.info("Successful cookie login for user {}.",
                user.getUserUsername());

            // Save an indicator that the current user logged in using a cookie.
            // Applications may want to distinguish sessions that began this way
            // from sessions that began from a regular login to restrict access
            // to sensitive functionality such as e-commerce transactions.
            context.session().put(PyxisConstants.SESSION_COOKIE_LOGIN, true);
            this.application.getSecurity().captureSuccessfulLoginAttempt(context);
            return true;
          }
          // Failed authentication attempt.
          else
          {
            this.application.getSecurity().captureFailedLoginAttempt(context);
          }
        }
        else if (validation.isAttempt())
        {
          this.application.getSecurity().captureFailedLoginAttempt(context);
        }
      }
      else
      {
        log.debug("Too many attempts from {}; cookie login blocked temporarily.",
            context.getClientId());
      }
    }

    return false;
  }

  //
  // PROTECTED METHODS
  //
  
  /**
   * Is the provided masquerade operation permitted?  By default, only
   * administrators may masquerade, and only non-administrators can be
   * impersonated.  Subclasses can override this method to provide custom
   * logic.
   * 
   * @param masqueradingUser The user doing the masquerading; by default this
   *        must be an administrator.  Will not be null.
   * @param impersonatedUser The user being impersonated; by default this
   *        must be a non-administrator.  Will not be null.
   */
  protected boolean isMasqueradePermitted(PyxisUser masqueradingUser,
      PyxisUser impersonatedUser)
  {
    return (  (masqueradingUser.isAdministrator())
           && (!impersonatedUser.isAdministrator())
           );
  }
  
  //
  // PRIVATE METHODS
  //

  private void recordUserSessionId(Context context, PyxisUser user)
  {
    userSessionIDs.put(user.getId(), context.getSession(true).getId());
  }
  
  /**
   * Store the user or user id (depending on configuration) into the session.
   */
  private void storeUserInSession(Context context, PyxisUser user)
  {
    storeUserInSession(context, user, PyxisConstants.SESSION_USER);
  }
  
  /**
   * Removes the user or user id (depending on configuration) from the
   * session.
   */
  private void removeUserFromSession(Context context)
  {
    // Indicate that we are closing this user session and then remove a bunch
    // of items from the session.
    context.session().put(PyxisConstants.SESSION_CLOSE_INDICATOR, 1)
        .remove(PyxisConstants.SESSION_USER)
        .remove(PyxisConstants.SESSION_CLOSE_INDICATOR)
        .remove(PyxisConstants.SESSION_IMPERSONATED_USER)
        .remove(PyxisConstants.SESSION_COOKIE_LOGIN)
        .remove(PyxisConstants.SESSION_EXPIRATION_WARNED);
  }

  private boolean isCookieLoginPermitted(Context context)
  {
    return (  (application.getSecurity().getSettings().isCookieLoginEnabled())
           && (  (!application.getSecurity().getSettings().isCookieLoginSslOnly())
              || (context.isSecure())
              )
           );
  }
  
  /**
   * Store the user or user id (depending on configuration) into the session,
   * with a session key provided (typically PyxisConstants.SESSION_USER).
   */
  private void storeUserInSession(Context context, PyxisUser user, String key)
  {
    if (application.getSecurity().getSettings().storeUserAsId())
    {
      context.session().put(key, user.getId());
    }
    else
    {
      context.session().putObject(key, user);
    }
  }
  
  /**
   * Gets a user from the session given a provided session key name.
   */
  private PyxisUser getUserFromSession(Context context, String key)
  {
    if (application.getSecurity().getSettings().storeUserAsId())
    {
      return this.application.getSecurity().getUser(
          context.session().getLong(key, 0));
    }
    else
    {
      return context.session().getObject(key);
    }
  }
  
  /**
   * Gets a user from the session given a provided session key name.
   */
  private PyxisUser getUserFromSession(Session session, String key)
  {
    if (application.getSecurity().getSettings().storeUserAsId())
    {
      final Long userID = (Long)session.getAttribute(key);
      if (userID != null)
      {
        return this.application.getSecurity().getUser(userID.longValue());
      }
      else
      {
        return null;
      }
    }
    else
    {
      return (PyxisUser)session.getAttribute(key);
    }
  }
}
