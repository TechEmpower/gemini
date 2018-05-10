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

import org.mindrot.jbcrypt.*;

import com.techempower.data.*;
import com.techempower.gemini.*;
import com.techempower.gemini.manager.*;
import com.techempower.gemini.pyxis.listener.*;
import com.techempower.gemini.pyxis.password.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.scheduler.*;
import com.techempower.util.*;

/**
 * Manages authentication tokens for cookie-based logins.  These tokens are
 * persisted client-side as cookies, and server-side as rows in a database
 * table.  The server-side tokens include timestamps and a cryptographic hash of
 * the token.
 *
 * <h2>Database table</h2>
 *
 * <p>The name of the database table that stores these authentication tokens is
 * configurable (see below), but its column names and types are not.  The table
 * must contain these columns:
 * <dl>
 *   <dt>id</dt>
 *   <dd>integer, auto-incrementing primary key, never null</dd>
 *   <dt>username</dt>
 *   <dd>string, never null, should be the same size as the username column in
 *   the user table</dd>
 *   <dt>tokenhash</dt>
 *   <dd>string, never null, 100 characters should be enough</dd>
 *   <dt>created</dt>
 *   <dd>datetime, never null</dd>
 * </dl>
 *
 * <p>Here is an example SQL statement for creating this table in MySQL:
 * <pre>
 * CREATE TABLE `logintoken` (
 *   `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
 *   `username` VARCHAR(100) NOT NULL ,
 *   `tokenhash` VARCHAR(100) NOT NULL ,
 *   `created` DATETIME NOT NULL ,
 *   PRIMARY KEY (`id`)
 * );
 * </pre>
 *
 * <p>There is a configurable maximum number of tokens that may exist at one 
 * time per user.  Also, by default, a scheduled event that purges stale
 * authentication tokens from the database is enabled.
 *
 * <h2>Configurable properties</h2>
 * <dl>
 *   <dt>LoginTokenManager.DatabaseTableName</dt>
 *     <dd>string - default {@value #DEFAULT_DATABASE_TABLE_NAME}.  The name
 *     of the table in the database that stores cookie login tokens.</dd>
 *   <dt>LoginTokenManager.CookieSecure</dt>
 *     <dd>boolean - default {@value #DEFAULT_COOKIE_SECURE}.  Whether the
 *     "secure" flag on login cookies is set.  (If you override this default,
 *     you had better have a very good reason.)</dd>
 *   <dt>LoginTokenManager.MaxTokensPerUser</dt>
 *     <dd>integer - default {@value #DEFAULT_MAX_TOKENS_PER_USER}.
 *   <dt>LoginTokenManager.MaxTokenAgeInDays</dt>
 *     <dd>integer - default {@value #DEFAULT_PURGE_EVENT_MAX_TOKEN_AGE_IN_DAYS}.
 *     The maximum number of authentication tokens that can be owned by a
 *     given user.  Tokens beyond this number will be purged.</dd>
 *   <dt>LoginTokenManager.Event.Enabled</dt>
 *     <dd>boolean - default {@value #DEFAULT_PURGE_EVENT_ENABLED}.  Whether 
 *     the event to purge authentication tokens from the database is enabled.</dd>
 *   <dt>LoginTokenManager.PurgeEvent.MaxTokenAgeInDays</dt>
 *     <dd>integer - default {@value #DEFAULT_PURGE_EVENT_MAX_TOKEN_AGE_IN_DAYS}.
 *     The maximum age in days of an authentication token before it should be
 *     purged from the database.  Note that this purging <em>only</em> happens
 *     as a result of the event running.</dd>
 *   <dt>LoginTokenManager.PurgeEvent.IntervalInDays</dt>
 *     <dd>integer - default {@value #DEFAULT_PURGE_EVENT_INTERVAL_IN_DAYS}.
 *     The number of days between each execution of the event.  Also 
 *     determines how many days after the application is first started (or was
 *     last reconfigured) that the event will first execute.</dd>
 *   <dt>LoginTokenManager.PurgeEvent.Hour</dt>
 *     <dd>integer - default {@value #DEFAULT_PURGE_EVENT_HOUR}.  The hour of
 *     day (0 to 23) that the event should execute.</dd>
 *   <dt>LoginTokenManager.PurgeEvent.Minute</dt>
 *     <dd>integer - default {@value #DEFAULT_PURGE_EVENT_MINUTE}.  The minute
 *     within the hour (0 to 59) that the event should execute.</dd>
 *   <dt>LoginTokenManager.OneTimeUseTokens</dt>
 *     <dd>boolean - default {@value #DEFAULT_ONE_TIME_USE_TOKENS}.  If true,
 *     when a token is used, it will be replaced with a newly-generated token.
 *     That is, a single token cannot be used to login twice.</dd>
 * </dl>
 *
 * <p>Here is an example configuration:
 * <pre>
 * LoginTokenManager.DatabaseTableName = logintoken
 * LoginTokenManager.CookieSecure = no
 * LoginTokenManager.MaxTokensPerUser = 50
 * LoginTokenManager.OneTimeUseTokens = true
 * LoginTokenManager.PurgeEvent.Enabled = yes
 * LoginTokenManager.PurgeEvent.MaxTokenAgeInDays = 100
 * LoginTokenManager.PurgeEvent.IntervalInDays = 5
 * LoginTokenManager.PurgeEvent.Hour = 5
 * LoginTokenManager.PurgeEvent.Minute = 30
 * </pre>
 */
public class LoginTokenManager
  extends    BasicManager<GeminiApplication>
{
  // Constants

  private static final int TOKEN_DIGITS = 30;

  // Defaults for configurable properties

  private static final String  DEFAULT_DATABASE_TABLE_NAME = "logintoken";
  private static final boolean DEFAULT_COOKIE_SECURE = true;
  private static final int     DEFAULT_COOKIE_EXPIRATION_IN_DAYS = 5;
  private static final int     DEFAULT_MAX_TOKENS_PER_USER = 50;
  private static final boolean DEFAULT_PURGE_EVENT_ENABLED = true;
  private static final int     DEFAULT_PURGE_EVENT_MAX_TOKEN_AGE_IN_DAYS = 365;
  private static final int     DEFAULT_PURGE_EVENT_INTERVAL_IN_DAYS = 5;
  private static final int     DEFAULT_PURGE_EVENT_HOUR = 5;
  private static final int     DEFAULT_PURGE_EVENT_MINUTE = 30;
  private static final boolean DEFAULT_ONE_TIME_USE_TOKENS = true;

  // Configurable properties

  private String  databaseTableName = DEFAULT_DATABASE_TABLE_NAME;
  private boolean cookieSecure = DEFAULT_COOKIE_SECURE;
  private int     maxTokensPerUser = DEFAULT_MAX_TOKENS_PER_USER;
  private int     tokenExpirationInDays = DEFAULT_COOKIE_EXPIRATION_IN_DAYS;
  private boolean purgeEventEnabled = DEFAULT_PURGE_EVENT_ENABLED;
  private int     purgeEventMaxTokenAgeInDays = DEFAULT_PURGE_EVENT_MAX_TOKEN_AGE_IN_DAYS;
  private int     purgeEventIntervalInDays = DEFAULT_PURGE_EVENT_INTERVAL_IN_DAYS;
  private int     purgeEventHour = DEFAULT_PURGE_EVENT_HOUR;
  private int     purgeEventMinute = DEFAULT_PURGE_EVENT_MINUTE;
  private boolean oneTimeUseTokens = DEFAULT_ONE_TIME_USE_TOKENS;

  // Assigned in the constructor

  private final PurgeEvent purgeEvent;
  private final ClearTokensOnPasswordChange clearTokenListener;

  public LoginTokenManager(GeminiApplication application)
  {
    super(application, "lTkn");
    purgeEvent = this.new PurgeEvent();
    clearTokenListener = new ClearTokensOnPasswordChange();
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    Objects.requireNonNull(props);

    // TODO: This remove call can be deleted if we remove reconfiguration
    // from Gemini.
    security().removeListener(clearTokenListener);
    security().addListener(clearTokenListener);

    final EnhancedProperties.Focus focus = props.focus("LoginTokenManager.");
    databaseTableName = focus.get("DatabaseTableName",
        DEFAULT_DATABASE_TABLE_NAME);
    cookieSecure = focus.getBoolean("CookieSecure",
        DEFAULT_COOKIE_SECURE);
    maxTokensPerUser = focus.getInt("MaxTokensPerUser",
        DEFAULT_MAX_TOKENS_PER_USER, 1, Integer.MAX_VALUE);
    if (focus.has("MaxTokenAgeInDays"))
    {
      tokenExpirationInDays = focus.getInt("MaxTokenAgeInDays", 
          DEFAULT_PURGE_EVENT_MAX_TOKEN_AGE_IN_DAYS, 0, Integer.MAX_VALUE);
      l("LoginTokenManager.MaxTokenAgeInDays is deprecated. Use LoginTokenManager.TokenExpirationInDays instead.");
    }
    tokenExpirationInDays = focus.getInt("TokenExpirationInDays", 
        DEFAULT_PURGE_EVENT_MAX_TOKEN_AGE_IN_DAYS, 0, Integer.MAX_VALUE);
    purgeEventEnabled = focus.getBoolean("PurgeEvent.Enabled",
        DEFAULT_PURGE_EVENT_ENABLED);
    purgeEventMaxTokenAgeInDays = focus.getInt("PurgeEvent.MaxTokenAgeInDays",
        DEFAULT_PURGE_EVENT_MAX_TOKEN_AGE_IN_DAYS, 0, Integer.MAX_VALUE);
    purgeEventIntervalInDays = focus.getInt("PurgeEvent.IntervalInDays",
        DEFAULT_PURGE_EVENT_INTERVAL_IN_DAYS, 1, Integer.MAX_VALUE);
    purgeEventHour = focus.getInt("PurgeEvent.Hour",
        DEFAULT_PURGE_EVENT_HOUR, 0, 23);
    purgeEventMinute = focus.getInt("PurgeEvent.Minute",
        DEFAULT_PURGE_EVENT_MINUTE, 0, 59);
    oneTimeUseTokens = focus.getBoolean("OneTimeUseTokens", 
        DEFAULT_ONE_TIME_USE_TOKENS);
    app().getScheduler().removeEvent(purgeEvent);
    if (purgeEventEnabled)
    {
      app().getScheduler().scheduleEvent(purgeEvent);
    }
    l("Configured " + this, LogLevel.NORMAL);
  }

  @Override
  public String toString()
  {
    return String.format(
        "%s{"
            + "databaseTableName=%s, "
            + "cookieSecure=%s, "
            + "oneTimeUseTokens=%s, "
            + "tokenExpirationInDays=%s, "
            + "maxTokensPerUser=%s, "
            + "purgeEventEnabled=%s, "
            + "purgeEventMaxTokenAgeInDays=%s, "
            + "purgeEventIntervalInDays=%s, "
            + "purgeEventHour=%s, "
            + "purgeEventMinute=%s}",
        getClass().getSimpleName(),
        databaseTableName,
        cookieSecure,
        oneTimeUseTokens,
        tokenExpirationInDays,
        maxTokensPerUser,
        purgeEventEnabled,
        purgeEventMaxTokenAgeInDays,
        purgeEventIntervalInDays,
        purgeEventHour,
        purgeEventMinute);
  }

  // Internals

  /**
   * Wraps a SQL table name or column name in the identifier quote strings used
   * by the database.  For example, MySQL uses the "`" character.  Table or
   * column names wrapped in these characters can be used safely in SQL queries
   * even if they are reserved keywords.
   *
   * @param tableOrColumn the name of the table or column to be surrounded with
   *                      quote strings
   * @return the table or column name surrounded with quote strings
   */
  private String enquote(String tableOrColumn)
  {
    Objects.requireNonNull(tableOrColumn);
    String quote = app().getConnectorFactory().getIdentifierQuoteString();
    return quote + tableOrColumn + quote;
  }

  /**
   * Returns a newly generated authentication token.  This token consists of
   * random characters and should be treated like a password.  It should only be
   * persisted client-side.
   *
   * @return a newly generated authentication token
   */
  private String generateToken()
  {
    return StringHelper.secureRandomString.alphanumeric(TOKEN_DIGITS);
  }

  /**
   * Returns the value of the login cookie for the given username and
   * authentication token.
   *
   * @param username the current user's username
   * @param token the current user's authentication token
   * @return the value of the login cookie for the given username and
   *         authentication token
   */
  private String generateCookieValue(String username, String token)
  {
    Objects.requireNonNull(username);
    Objects.requireNonNull(token);
    return username + "|" + token;
  }

  /**
   * Returns a hash for the given username and authentication token.  This hash
   * is generated with BCrypt.
   *
   * @param username the current user's username
   * @param token the current user's authentication token
   * @return a hash for the given username and authentication token
   */
  private String generateTokenHash(String username, String token)
  {
    Objects.requireNonNull(username);
    Objects.requireNonNull(token);
    return BCrypt.hashpw(
        generateCookieValue(username, token), BCrypt.gensalt());
  }

  /**
   * Returns the name of the login cookie.
   *
   * @return the name of the login cookie
   */
  private String generateCookieName()
  {
    return app().getVersion().getProductCode() + "-automatic-login";
  }

  /**
   * Returns the current timestamp as a string.
   *
   * @return the current timestamp as a string
   */
  private String generateTimestamp()
  {
    return new java.sql.Timestamp(System.currentTimeMillis()).toString();
  }

  /**
   * Returns a new login cookie with the given name and value.
   *
   * @param cookieName the name of the login cookie
   * @param cookieValue the value of the login cookie
   * @return a new login cookie with the given name and value
   */
  private ResponseCookie generateCookie(String cookieName, String cookieValue)
  {
    Objects.requireNonNull(cookieName);
    Objects.requireNonNull(cookieValue);
    final ResponseCookie cookie = new ResponseCookie(cookieName, cookieValue);
    cookie.setAge(tokenExpirationInDays * 60 * 60 * 24);  // Convert days to seconds.
    cookie.setPath(app().getInfrastructure().getUrl());
    if (cookieSecure)
    {
      cookie.setSecure(true);
    }
    return cookie;
  }

  /**
   * Removes authentication tokens from the database that are too old.
   *
   * @throws SQLException if an error occurs when updating the database
   */
  private void purgeTokens(int maximumTokenAgeInDays) throws SQLException
  {
    int staleTokensPurged;
    Calendar staleDate = DateHelper.getStartOfDay();
    staleDate.add(Calendar.DAY_OF_YEAR, -1 * maximumTokenAgeInDays);
    String staleTimestamp = new java.sql.Timestamp(staleDate.getTimeInMillis()).toString();
    try (ConnectionMonitor monitor = app().getConnectorFactory().getConnectionMonitor();
         PreparedStatement statement = monitor.getConnection().prepareStatement(
             "DELETE FROM " + enquote(databaseTableName)
                 + " WHERE " + enquote("created") + " < ?;"))
    {
      statement.setString(1, staleTimestamp);
      staleTokensPurged = statement.executeUpdate();
    }
    l("Purged " + staleTokensPurged + " stale tokens.", LogLevel.NORMAL);
  }

  // Public API

  /**
   * Clears the persistent login cookie from the current context.
   *
   * @param context the current context
   */
  public void clearCookie(Context context, PyxisUser user)
  {
    Objects.requireNonNull(context);
    if (user != null)
    {
      l("Deleting automatic login cookie for " 
          + user.getUserUsername() 
          + " [" + user.getId() + "].", LogLevel.MINIMUM);
    }
    else
    {
      l("Deleting automatic login cookie for null user.", LogLevel.MINIMUM);
    }
    context.cookies().remove(generateCookieName());
  }

  /**
   * Creates and persists an authentication token for the current user.  An
   * entry for this token is persisted in the database and is set as a cookie in
   * the response.  If the user has more than the maximum number of tokens, the
   * excess tokens are purged.  The interaction with the database is done
   * asynchronously so as not to hold up the current request.
   *
   * <p>Note: <strong>this method does not actually log the user in</strong>.
   * It only creates authentication tokens.
   *
   * @param context the current context
   * @param username the current user's username
   */
  public void createAndPersistToken(Context context, final String username)
  {
    Objects.requireNonNull(context);
    Objects.requireNonNull(username);
    final String token = generateToken();
    ThreadHelper.submit(new Runnable() {
      @Override
      public void run()
      {
        try (ConnectionMonitor monitor = app().getConnectorFactory().getConnectionMonitor();
             PreparedStatement statement = monitor.getConnection().prepareStatement(
                 "INSERT INTO " + enquote(databaseTableName)
                     + " (" + enquote("username")
                     + ", " + enquote("tokenhash")
                     + ", " + enquote("created") + ")"
                     + " VALUES (?, ?, ?);"))
        {
          statement.setString(1, username);
          statement.setString(2, generateTokenHash(username, token));
          statement.setString(3, generateTimestamp());
          statement.executeUpdate();
        }
        catch (SQLException e)
        {
          l("Error while persisting new token for user: " + username,
              LogLevel.ALERT, e);
        }
        // Unfortunately, we have need of "LIMIT" or "TOP" here, and that's not
        // standard across database vendors.
        if (app().getConnectorFactory().getDatabaseAffinity() == DatabaseAffinity.MS_SQL_SERVER)
        {
          try (ConnectionMonitor monitor = app().getConnectorFactory().getConnectionMonitor();
               PreparedStatement statement = monitor.getConnection().prepareStatement(
                   "DELETE FROM " + enquote(databaseTableName)
                       + " WHERE " + enquote("username")
                       + " = ? AND " + enquote("created")
                       // For whatever reason, at least one SQL server driver
                       // throws an error if you try to bind the "TOP" integer
                       // like a normal prepared statement parameter.
                       + " NOT IN (SELECT TOP " + maxTokensPerUser
                       + " " + enquote("created")
                       + " FROM " + enquote(databaseTableName)
                       + " WHERE " + enquote("username") + "=?"
                       + " ORDER BY " + enquote("created")
                       + " DESC);"))
          {
            statement.setString(1, username);
            statement.setString(2, username);
            statement.executeUpdate();
          }
          catch (SQLException e)
          {
            l("Error while trimming excess tokens for user: " + username,
                LogLevel.ALERT, e);
          }
        }
        else
        {
          try (ConnectionMonitor monitor = app().getConnectorFactory().getConnectionMonitor();
               PreparedStatement statement = monitor.getConnection().prepareStatement(
                   "DELETE FROM " + enquote(databaseTableName)
                       + " WHERE " + enquote("username")
                       + " = ? AND " + enquote("created")
                       + " NOT IN (SELECT * FROM (SELECT " + enquote("created")
                       + " FROM " + enquote(databaseTableName)
                       + " WHERE " + enquote("username") + "=?"
                       + " ORDER BY " + enquote("created")
                       + " DESC LIMIT ?) AS t);"))
          {
            statement.setString(1, username);
            statement.setString(2, username);
            statement.setInt(3, maxTokensPerUser);
            statement.executeUpdate();
          }
          catch (SQLException e)
          {
            l("Error while trimming excess tokens for user: " + username,
                LogLevel.ALERT, e);
          }
        }
      }
    });
    String cookieName = generateCookieName();
    String cookieValue = generateCookieValue(username, token);
    context.cookies().put(generateCookie(cookieName, cookieValue));
  }
  
  /**
   * Clears all saved tokens for a provided username.
   * 
   * @param username The username for which all saved tokens should be 
   * removed.
   */
  public void clearAllTokensForUser(String username)
  {
    Objects.requireNonNull(username);
    ThreadHelper.submit(new Runnable() {
      @Override
      public void run()
      {
        l("Clearing all cookie login tokens for " + username + ".", LogLevel.DEBUG);
        try (ConnectionMonitor monitor = app().getConnectorFactory().getConnectionMonitor();
            PreparedStatement statement = monitor.getConnection().prepareStatement(
                "DELETE FROM " + enquote(databaseTableName)
                    + " WHERE " + enquote("username")
                    + " = ?;"))
        {
          statement.setString(1, username);
          statement.executeUpdate();
        }
        catch (SQLException e)
        {
          l("Error while clearing tokens for user: " + username,
              LogLevel.ALERT, e);
        }
      }
    });
  }

  /**
   * Checks whether the current context contains a valid login cookie.
   *   <p>
   * If the token manager is configured to limit tokens to a single use (via
   * the OneTimeUseTokens option), valid tokens are "consumed" by this method.
   * That is, a new token is persisted in the database and the user receives 
   * an updated login cookie, and then this method returns {@code true}.  
   * If the current context has no cookie or has an invalid cookie, this 
   * method returns {@code false}.
   *   <p>
   * If the token manager is configured to allow multiple-use tokens, valid
   * tokens are acknowledged (and this method returns {@code true}, but not
   * consumed. 
   *
   * <p>Note: <strong>this method does not actually log the user in</strong>.
   * It only validates and updates authentication tokens.
   *
   * @param context the current context
   * @return {@code true} if the context had a valid login cookie
   * @throws SQLException if an error occurs when reading or updating the
   *                      database
   */
  public TokenValidation validateAndUpdateToken(Context context)
      throws SQLException
  {
    Objects.requireNonNull(context);
    final String cookieName = generateCookieName();
    final String cookieValue = context.cookies().getValue(cookieName);
    if (cookieValue == null)
    {
      return TokenValidation.FAILURE;
    }
    final int pipeIndex = cookieValue.indexOf('|');
    if (pipeIndex < 0 || pipeIndex == cookieValue.length() - 1)
    {
      l("Prompting client to remove improperly formatted login token.", 
          LogLevel.DEBUG);
      context.cookies().remove(cookieName);
      return TokenValidation.FAILURE;
    }
    final String username = cookieValue.substring(0, pipeIndex);
    try (ConnectionMonitor monitor = app().getConnectorFactory().getConnectionMonitor();
         PreparedStatement statement = monitor.getConnection().prepareStatement(
             "SELECT " + enquote("id")
                 + ", " + enquote("username")
                 + ", " + enquote("tokenhash")
                 + ", " + enquote("created")
                 + " FROM " + enquote(databaseTableName)
                 + " WHERE " + enquote("username") + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_UPDATABLE))
    {
      statement.setString(1, username);
      try (ResultSet resultSet = statement.executeQuery())
      {
        while (resultSet.next())
        {
          String tokenHash = resultSet.getString("tokenhash");
          if (BCrypt.checkpw(cookieValue, tokenHash))
          {
            if (oneTimeUseTokens)
            {
              final String newToken = generateToken();
              resultSet.updateString(3, generateTokenHash(username, newToken));
              resultSet.updateString(4, generateTimestamp());
              resultSet.updateRow();
              final String newCookieValue = generateCookieValue(username, newToken);
              context.cookies().put(generateCookie(cookieName, newCookieValue));
            }
            return new TokenValidation(true, username);
          }
        }
      }
    }
    l("Prompting client to remove invalid token for " + username + ".", 
        LogLevel.DEBUG);
    context.cookies().remove(cookieName);
    return new TokenValidation(false, username);
  }

  // Inner classes

  /**
   * A scheduled event that purges authentication tokens from the database.
   */
  private final class PurgeEvent extends ScheduledEvent
  {
    private PurgeEvent()
    {
      super("Login Token Purge Event",
          "Periodically removes old authentication tokens from the data store.");
    }
    
    @Override
    public long getDefaultScheduledTime()
    {
      Calendar scheduledTime = DateHelper.getStartOfDay();
      scheduledTime.add(Calendar.DAY_OF_YEAR, purgeEventIntervalInDays);
      scheduledTime.set(Calendar.HOUR_OF_DAY, purgeEventHour);
      scheduledTime.set(Calendar.MINUTE, purgeEventMinute);
      return scheduledTime.getTimeInMillis();
    }

    @Override
    public boolean requiresOwnThread()
    {
      return true;
    }

    @Override
    public void execute(Scheduler scheduler, boolean onDemandExecution)
    {
      try
      {
        purgeTokens(purgeEventMaxTokenAgeInDays);
      }
      catch (SQLException e)
      {
        l(getName() + " had an exception during scheduled execution.",
            LogLevel.ALERT, e);
      }
      finally
      {
        scheduler.scheduleEvent(this, getDefaultScheduledTime());
      }
    }
  }
  
  /**
   * A SecurityListener that clears all saved tokens for a user when the
   * user's password is changed.
   */
  private final class ClearTokensOnPasswordChange extends EmptySecurityListener
  {
    @Override
    public void passwordChanged(PasswordProposal proposal) 
    {
      clearAllTokensForUser(proposal.username);
    }
  }

  /**
   * The result of validating a cookie-based login token.
   */
  public static final class TokenValidation
  {
    /**
     * Singleton representing a failed cookie-based login.
     */
    private static final TokenValidation FAILURE = new TokenValidation(false, null);

    private final boolean valid;
    private final String username;

    private TokenValidation(boolean valid, String username)
    {
      this.valid = valid;
      this.username = username;
    }

    /**
     * Returns {@code true} if the login attempt was valid.
     *
     * @return {@code true} if the login attempt was valid
     */
    public boolean isValid()
    {
      return valid;
    }

    /**
     * Returns the current user's username if the login attempt was valid.
     * Otherwise, returns {@code null}.
     *
     * @return the current user's username if the login attempt was valid
     */
    public String getUsername()
    {
      return username;
    }
    
    /**
     * Returns {@code true} if the cookie provided a login attempt that was
     * considered and therefore should be tracked.  Specifically, a username 
     * was provided by a cookie the user's browser furnished, but the token
     * was not valid.
     */
    public boolean isAttempt()
    {
      return (username != null);
    }
    
    @Override
    public String toString()
    {
      return "TokenValidation [" + username + "; " + (valid ? "valid" : "invalid") + "]";
    }
  }
}
