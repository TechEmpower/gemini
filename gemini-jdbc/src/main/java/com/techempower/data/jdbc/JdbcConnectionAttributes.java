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

package com.techempower.data.jdbc;

import com.techempower.*;
import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.LoggerFactory;

/**
 * A set of attributes about JDBC Connections: URL prefix, username, etc.
 *   <p>
 * Applications do not typically interact directly with this class, but
 * rather interact with BasicConnectorFactory to get connectors and 
 * JdbcConnector to execute queries.
 * 
 * @see JdbcConnector
 * @see BasicConnectorFactory
 */
class JdbcConnectionAttributes
{
  
  //
  // Constants.
  //
  
  public static final String  COMPONENT_CODE = "jdCA";
  public static final int     DEFAULT_MINIMUM_POOL_SIZE = 3;
  public static final int     DEFAULT_MAXIMUM_POOL_MULTIPLIER = 5;
  public static final DatabaseAffinity DEFAULT_DATABASE_AFFINITY = DatabaseAffinity.MYSQL;
  public static final String  DEFAULT_TEST_QUERY = "SELECT 1 AS Result";
  public static final String  DEFAULT_TEST_VALUE = "1";
  public static final long    DEFAULT_TEST_INTERVAL = UtilityConstants.MINUTE;
  public static final long    MAXIMUM_TEST_INTERVAL = UtilityConstants.DAY;
  public static final long    MINIMUM_TEST_INTERVAL = UtilityConstants.MINUTE;
  public static final long    DEFAULT_STALE_TIMEOUT = 10 * UtilityConstants.MINUTE;
  public static final long    DEFAULT_ABORT_TIMEOUT = UtilityConstants.HOUR;
 
  //
  // Member variables.
  //
  
  private final String                     jdbcURLPrefix;
  private final String                     connectString;
  private final String                     displayName;
  private final String                     username;
  private final String                     password;
  private final String                     driverClassName;
  private final TechEmpowerApplication     application;
  private final int                        minimumPoolSize;
  private final int                        maximumPoolSize;
  private final DatabaseAffinity           databaseAffinity;

  private final boolean                    testEnabled;
  private final String                     testQuery;
  private final String                     testValue;
  private final long                       testInterval;
  
  private final long                       staleTimeout;
  private final long                       abortTimeout;
  
  private final DatabaseConnectionListener listener;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor that builds from an EnhancedProperties reference.
   */
  public JdbcConnectionAttributes(
      EnhancedProperties props, 
      DatabaseConnectionListener listener, 
      TechEmpowerApplication application, 
      String prefix)
  {
    this.application = application;

    EnhancedProperties.Focus focus = props.focus(prefix);
    this.connectString = focus.get("ConnectString");
    this.displayName = focus.get("DisplayName", StringHelper.truncateEllipsis(this.connectString, 25));
    this.username = focus.get("LoginName");
    this.password = focus.get("LoginPass");
    this.jdbcURLPrefix = focus.get("Driver.UrlPrefix");
    this.minimumPoolSize = focus.getInt("Driver.Pooling", DEFAULT_MINIMUM_POOL_SIZE);
    this.maximumPoolSize = focus.getInt("Driver.MaxPooling", DEFAULT_MAXIMUM_POOL_MULTIPLIER * this.minimumPoolSize);
    this.driverClassName = focus.get("Driver.Class");
    this.databaseAffinity = focus.getEnum("DatabaseAffinity",
        DatabaseAffinity.class, DatabaseAffinity.MYSQL);
    this.testEnabled = focus.getBoolean("TestEnabled", true);
    this.testQuery = focus.get("TestQuery", DEFAULT_TEST_QUERY);
    this.testValue = focus.get("TestValue", DEFAULT_TEST_VALUE);
    this.testInterval = focus.getLong("TestInterval", DEFAULT_TEST_INTERVAL, 
        MINIMUM_TEST_INTERVAL, MAXIMUM_TEST_INTERVAL);
    this.staleTimeout = focus.getLong("Driver.StaleTimeout", DEFAULT_STALE_TIMEOUT);
    this.abortTimeout = focus.getLong("Driver.AbortTimeout", DEFAULT_ABORT_TIMEOUT);
    this.listener = listener;
    
    // Load driver
    JdbcHelper.loadDriver(this.driverClassName,
        LoggerFactory.getLogger(COMPONENT_CODE));
  }
  
  /**
   * @return String
   */
  protected String getConnectString()
  {
    return connectString;
  }

  /**
   * @return String
   */
  protected String getJdbcURLPrefix()
  {
    return jdbcURLPrefix;
  }

  /**
   * @return String
   */
  protected String getPassword()
  {
    return password;
  }

  /**
   * @return String
   */
  protected String getUsername()
  {
    return username;
  }
  
  /**
   * @return the minimumPoolSize
   */
  public int getMinimumPoolSize()
  {
    return minimumPoolSize;
  }

  /**
   * @return the maximumPoolSize
   */
  public int getMaximumPoolSize()
  {
    return maximumPoolSize;
  }
  
  /**
   * Gets the Database Connection Listener.
   */
  protected DatabaseConnectionListener getListener()
  {
    return listener;
  }
  
  /**
   * Gets the TechEmpowerApplication reference.
   */
  protected TechEmpowerApplication getApplication()
  {
    return this.application;
  }

  /**
   * Gets the database affinity for this connection.
   */
  public DatabaseAffinity getDatabaseAffinity()
  {
    return this.databaseAffinity;
  }

  /**
   * @return the testQuery
   */
  public String getTestQuery()
  {
    return this.testQuery;
  }
  
  /**
   * Is keep-alive testing enabled?
   */
  public boolean isTestEnabled()
  {
    return this.testEnabled;
  }

  /**
   * @return the testValue
   */
  public String getTestValue()
  {
    return this.testValue;
  }

  /**
   * @return the testInterval
   */
  public long getTestInterval()
  {
    return this.testInterval;
  }
  
  /**
   * Gets the stale timeout.
   */
  public long getStaleTimeout()
  {
    return this.staleTimeout;
  }

  /**
   * Gets the abort timeout.
   */
  public long getAbortTimeout()
  {
    return this.abortTimeout;
  }

  /**
   * Gets the display name.
   */
  public String getDisplayName()
  {
    return this.displayName;
  }

  /**
   * toString.
   */
  @Override
  public String toString()
  {
    return "JdbcConnectionAttributes [" 
        + getConnectString() 
        + "; " + this.driverClassName 
        + "; " + getMinimumPoolSize() 
        + "; " + getMaximumPoolSize() 
        + "]"; 
  }

}  // End JdbcConnectionAttributes.
