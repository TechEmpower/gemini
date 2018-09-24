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

import java.sql.*;
import java.util.concurrent.atomic.*;

import com.techempower.*;
import com.techempower.asynchronous.*;
import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * The "ConnectorFactory" object--that is either a BasicConnectorFactory
 * instance or an instance of a subclass--is a central means for an
 * application to gain access to a database.  The BasicConnectorFactory
 * assumes a JDBC-based driver will be used.  However, any driver could
 * be used as long as a DatabaseConnector subclass exists for the driver.
 *    <p>
 * A property-name prefix is provided to the constructor of this object
 * that specifies a usage-specific prefix to prepend to all property names.
 * This allows a single configuration file to hold database connectivity
 * properties for multiple uses (such as application, security, CMS, etc.)
 *    <p>
 * The following attributes are read from the properties file for
 * configuring this component:
 *    <ul>
 * <li> [prefix]ConnectString - the database connection string. </li>
 * <li> [prefix]LoginName - the database login account name. </li>
 * <li> [prefix]LoginPass - the database login password. </li>
 * <li> [prefix]SafeMode - optionally set the Safe Mode flag for exception
 *      handling within JdbcConnector.  Default is no (off/false). </li>
 * <li> [prefix]Driver.UrlPrefix - the JDBC driver's URL prefix. </li>
 * <li> [prefix]Driver.Class - the JDBC driver's class name, with
 *      packages. </li>
 * <li> [prefix]Driver.Pooling - How many connections should be provided
 *      as a minimum pool size for this database.  This is defaulted to 3. </li>
 * <li> [prefix]Driver.MaxPooling - How many connections should be permitted
 *      as a maximum pool size for this database.  This is defaulted to be
 *      equal to the minimum size * 5 unless specified. </li>
 * <li> [prefix]Driver.StaleTimeout - How long an idle connection should be
 *      retained in the pool (assuming the pool size is above minimum) in ms.
 *      Default is 3600000, or 1 hour.
 * <li> [prefix]QueryCounting - Yes or No, count the number of queries
 *      executed (actually the number of Connector objects constructed;
 *      reusing a Connector is not counted.)  Default is no. </li>
 * <li> [prefix]QueryCountFrequency - How often to show the query count as
 *      debug output.  Default is every 1000 queries. </li>
 * <li> [prefix]TestQuery - A test query to use to keep Connections alive;
 *      default is "SELECT 1 As Result" </li>
 * <li> [prefix]TestValue - The expected value to receive as a result set
 *      from the TestQuery; default is "1" </li>
 * <li> [prefix]TestInterval - The time between keep-alive queries in ms; the 
 *      default is 60000 (1 minute). </li>
 *    </ul>
 *   <p>
 * Note that the default prefix is 'db.', so unless you specify otherwise
 * in your constructor call, you would identify the Driver's class name by
 * setting the 'db.Driver.Class' property in your .conf file.
 */
public class BasicConnectorFactory
  implements ConnectorFactory,
             Configurable,
             DatabaseConnectionListener,
             Asynchronous
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "cnfc";
  public static final String DEFAULT_PROPERTY_PREFIX = "db.";
  public static final long   DEFAULT_QUERY_COUNT_FREQUENCY = 1000L;
  public static final int    DEFAULT_MAX_RETRIES = 0;
  public static final long   DEFAULT_RETRY_SLEEP = 0L;  

  //
  // Member variables.
  //

  private final    String       propertyPrefix;
  private volatile JdbcConnectionAttributes attributes = null;
  private volatile JdbcConnectionManager    connectionManager = null;
  private final    TechEmpowerApplication app;
  private final    ComponentLog log;
  private final    AtomicLong   queryCount = new AtomicLong(0L);
  
  private boolean      enabled               = true;
  private boolean      queryCounting         = false;
  private long         queryCountFrequency   = DEFAULT_QUERY_COUNT_FREQUENCY;
  private int          maxRetries            = DEFAULT_MAX_RETRIES;
  private long         retrySleep            = DEFAULT_RETRY_SLEEP;
  private String       identifierQuoteString = " ";
  
  private DatabaseConnectionListener dbListener = null;

  //
  // Member methods.
  //

  /**
   * Constructor.  If the propertyPrefix is passed as an empty String, the
   * default "db." will be used.
   *
   * @param propertyPrefix the prefix to apply to all property names
   *   in the conf file; default is "db."
   */
  public BasicConnectorFactory(TechEmpowerApplication application, 
      String propertyPrefix)
  {
    // Changed to use default prefix if an empty String is provided.
    if (StringHelper.isNonEmpty(propertyPrefix))
    {
      this.propertyPrefix = propertyPrefix;
    }
    else
    {
      this.propertyPrefix = DEFAULT_PROPERTY_PREFIX;
    }
    
    this.app = application;
    this.log = application.getLog(COMPONENT_CODE);

    // By default the connector factory itself will be the listener.
    dbListener = this;
  }

  /**
   * Configure this component.  For a BasicConnectorFactory, it is expected
   * that the properties will contain elements that are useful in determining
   * how to connect to the database (such as connect string, password, etc.).
   * See the JavaDocs for this class for details on these properties' names.
   */
  @Override
  public void configure(EnhancedProperties rawProps)
  {
    // Read parameters.
    final EnhancedProperties.Focus props = rawProps.focus(propertyPrefix);
    enabled             = props.getBoolean("Enabled", true);
    queryCounting       = props.getBoolean("QueryCounting", false);
    queryCountFrequency = props.getLong("QueryCountFrequency", DEFAULT_QUERY_COUNT_FREQUENCY);
    maxRetries          = props.getInt("MaxRetries", DEFAULT_MAX_RETRIES);
    retrySleep          = props.getLong("RetrySleep", DEFAULT_RETRY_SLEEP);

    // Only proceed if this connector factory is enabled.
    if (enabled)
    {
      // Grab a reference to the old connection manager so that we can shut it
      // down.
      JdbcConnectionManager oldManager = connectionManager;
      
      // Reset the connection attributes object.
      attributes = new JdbcConnectionAttributes(
          rawProps, dbListener, app, propertyPrefix);
      connectionManager = new JdbcConnectionManager(attributes);
      connectionManager.begin();
      
      if (oldManager != null)
      {
        // Shut down the old JdbcConnectionManager.
        oldManager.end();
      }
      
      // Display information about the configuration.
      debug("Configured using \"" + propertyPrefix + "\" prefix.");
      if (StringHelper.isNonEmpty(attributes.getConnectString()))
      {
        debug("Database connect string: " + attributes.getConnectString());
        debug("Database connection pool size: " 
            + attributes.getMinimumPoolSize() + " to "
            + attributes.getMaximumPoolSize() + ".");
        debug("Database timers: " 
            + attributes.getStaleTimeout() + "ms until stale; "
            + attributes.getAbortTimeout() + "ms until abort.");
        checkForCommonConfigurationMistakes(attributes);
  
        //displayProperties();
      }
      else
      {
        debug("No connect string specified with \"" + propertyPrefix + "\" prefix.");
      }
  
      if (dbListener != null && dbListener != this)
      {
        dbListener.configure(rawProps);
      }
      
      // Find out what character is used to escape table and column names in
      // queries.  Typically, this is ` or ".
      determineIdentifierQuoteString();
    }
    else
    {
      log.log("Database connector factory disabled.");
    }
  }
  
  @Override
  public void determineIdentifierQuoteString()
  {
    debug("Determining identifier quote string from database.");
    try (ConnectionMonitor monitor = getConnectionMonitor())
    {
      identifierQuoteString = monitor.getConnection().getMetaData().getIdentifierQuoteString();
    }
    catch (Exception e)
    {
      debug("Exception while reading identifier quote string.", e);
    }
    debug("Identifier quote string: " + identifierQuoteString);
  }

  /**
   * Start this Asynchronous component.
   */
  @Override
  public void begin()
  {
    // Does nothing.  The connection manager is started when we are
    // configured.
  }
  
  /**
   * Stop this Asynchronous component.
   */
  @Override
  public void end()
  {
    if (connectionManager != null)
    {
      connectionManager.end();
    }
  }
  
  /**
   * Is this Connector Factory enabled?  In nearly all cases, this would be
   * true; but it will be false for applications that do not connect to a
   * database at all.
   */
  @Override
  public boolean isEnabled()
  {
    return enabled;
  }
  
  /**
   * Logs a String to the ComponentLog.
   *
   * @param logString string to log.
   */
  protected void debug(String logString)
  {
    debug(logString, null);
  }

  /**
   * Logs a String to the ComponentLog.
   *
   * @param logString string to log.
   * @param e an exception or error to log.
   */
  protected void debug(String logString, Throwable e)
  {
    if (log != null)
    {
      log.log(logString, e);
    }
  }

  /**
   * Checks for some common configuration mistakes and outputs debug messages
   * if found.
   */
  protected void checkForCommonConfigurationMistakes(JdbcConnectionAttributes jca)
  {
    String conn = jca.getConnectString();
    String un = jca.getUsername();
    String pw = jca.getPassword();
    
    // Display a message if only 1 connection is permitted.
    if (jca.getMinimumPoolSize() == 1)
    {
      debug("ONLY 1 database connection is specified by " + propertyPrefix + "Driver.Pooling!");
    }
    
    if (StringHelper.isEmpty(jca.getJdbcURLPrefix()))
    {
      debug("WARNING: " + propertyPrefix + "Driver.UrlPrefix not specified.");
    }

    if (!conn.equals(conn.trim()))
    {
      debug("WARNING: " + propertyPrefix + "ConnectString contains extraneous white space.");
    }

    if (!un.equals(un.trim()))
    {
      debug("WARNING: " + propertyPrefix + "LoginName contains extraneous white space.");
    }

    if (!pw.equals(pw.trim()))
    {
      debug("WARNING: " + propertyPrefix + "LoginPass contains extraneous white space.");
    }
  }
  
  /**
   * Gets the ConnectionManager.
   */
  public JdbcConnectionManager getConnectionManager()
  {
    if (connectionManager == null)
    {
      throw new IllegalStateException("BasicConnectorFactory used prior to configuration.");
    }
    return connectionManager;
  }
  
  /**
   * Gets a ConnectionMonitor.
   */
  @Override
  public ConnectionMonitor getConnectionMonitor() 
      throws SQLException
  {
    return getConnectionManager().getConnectionMonitor();
  }

  public void setDatabaseConnectionListener(
    DatabaseConnectionListener dbConnListener)
  {
    this.dbListener = dbConnListener;
  }

  /**
   * Counts this query.
   */
  protected void count()
  {
    if (queryCounting)
    {
      long count = queryCount.incrementAndGet();
      if (count % queryCountFrequency == 0)
      {
        log.log(count + " queries executed.");
      }
    }
  }

  /**
   * Exception occurred during runQuery() in non-safe mode.
   */
  @Override
  public int exceptionInRunQuery(SQLException exc, JdbcConnector conn)
  {
    if (checkRetry(exc, conn))
    {
      return delayedRetry();
    }

    return DatabaseConnectionListener.INSTRUCT_DO_NOTHING;
  }

  /**
   * Exception occurred during runUpdateQuery() in non-safe mode.
   */
  @Override
  public int exceptionInRunUpdateQuery(SQLException exc, JdbcConnector conn)
  {
    if (checkRetry(exc, conn))
    {
      return delayedRetry();
    }

    return DatabaseConnectionListener.INSTRUCT_DO_NOTHING;
  }

  /**
   * Exception occurred during executeBatch() in non-safe mode.
   */
  @Override
  public int exceptionInExecuteBatch(SQLException exc, JdbcConnector conn)
  {
    if (checkRetry(exc, conn))
    {
      return delayedRetry();
    }

    return DatabaseConnectionListener.INSTRUCT_DO_NOTHING;
  }

  @Override
  public void queryStarting()
  {
    // Does nothing here.
  }

  @Override
  public void queryCompleting()
  {
    // Does nothing here.
  }

  /**
   * Checks to see if the query should be retried
   */
  protected boolean checkRetry(SQLException exc, JdbcConnector conn)
  {
    return conn.getTryCount() <= maxRetries;
  }

  /**
   * If a sleep time is specified, will put the currently executing thread to sleep before
   * retrying. If interrupted the query will be aborted.
   */
  protected int delayedRetry()
  {
    if (retrySleep > 0)
    {
      log.log("Retrying query in " + retrySleep + "ms.", LogLevel.ALERT);

      try
      {
        Thread.sleep(retrySleep);
      }
      catch (InterruptedException e)
      {
        // Interrupted, do not retry
        return DatabaseConnectionListener.INSTRUCT_DO_NOTHING;
      }
    }

    log.log("Retrying query.", LogLevel.ALERT);

    return DatabaseConnectionListener.INSTRUCT_RETRY;
  }

  @Override
  public String getIdentifierQuoteString()
  {
    return identifierQuoteString;
  }

  @Override
  public DatabaseAffinity getDatabaseAffinity()
  {
    return attributes.getDatabaseAffinity();
  }

}   // End BasicConnectorFactory.
