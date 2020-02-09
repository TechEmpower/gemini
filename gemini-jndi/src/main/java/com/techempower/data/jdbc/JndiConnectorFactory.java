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

import java.io.*;
import java.sql.*;

import javax.naming.*;
import javax.sql.*;

import com.techempower.*;
import com.techempower.asynchronous.*;
import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of ConnectorFactory that is a thin wrapper around a
 * connection pool defined in the servlet container (Resin, Tomcat, etc.) and
 * fetched via JNDI.
 * <p>
 * The following attributes are read from the properties file for configuring
 * this component:
 * <ul>
 * <li>[prefix]Enabled - whether this ConnectorFactory is enabled.</li>
 * <li>[prefix]Name - JNDI name defined in servlet container.</li>
 * </ul>
 * The remaining configuration for the data source is in the servlet container's
 * configuration.
 */
public class JndiConnectorFactory implements ConnectorFactory, Configurable, Asynchronous
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "JnCf";
  public static final String DEFAULT_PROPERTY_PREFIX = "db.Jndi.";

  //
  // Member variables.
  //

  private final String propertyPrefix;
  private final Logger log = LoggerFactory.getLogger(COMPONENT_CODE);

  private DataSource dataSource;

  private boolean enabled = true;
  private String identifierQuoteString = " ";
  private DatabaseAffinity databaseAffinity;

  //
  // Member methods.
  //

  /**
   * Constructor. If the propertyPrefix is passed as an empty String, the default
   * "db.Jndi." will be used.
   *
   * <p>
   * For a JNDI data source, configuration is done in the servlet container.
   * 
   * @param propertyPrefix
   *          the prefix to apply to all property names in the conf file; default
   *          is "db.Jndi."
   */
  public JndiConnectorFactory(TechEmpowerApplication application, String propertyPrefix)
  {
    // Use default prefix if an empty String is provided.
    if (StringHelper.isNonEmpty(propertyPrefix))
    {
      this.propertyPrefix = propertyPrefix;
    }
    else
    {
      this.propertyPrefix = DEFAULT_PROPERTY_PREFIX;
    }
  }

  /**
   * Configure this component. For a JNDI data source, configuration is done in
   * the servlet container.
   */
  @Override
  public void configure(EnhancedProperties rawProps)
  {
    // Read parameters.
    final EnhancedProperties.Focus props = rawProps.focus(propertyPrefix);
    enabled = props.getBoolean("Enabled", true);
    String jndiName = props.get("Name", "java:comp/env/jdbc/database");
    log.debug("JNDI name to be fetched: {}", jndiName);

    // Only proceed if this connector factory is enabled.
    if (enabled)
    {
      // Close existing DataSource, if exists.
      end();

      try
      {
        Context ic = new InitialContext();
        dataSource = (DataSource) ic.lookup(jndiName);
      }
      catch (NamingException e)
      {
        log.debug("Unable to fetch JNDI data source for name {}", jndiName, e);
      }

      try
      {
        // TODO: Hook this into slf4j
        dataSource.setLogWriter(new PrintWriter(System.out));
      }
      catch (SQLException e)
      {
        log.debug("Unable to set log writer", e);
      }

      // Find out what character is used to escape table and column names in
      // queries. Typically, this is ` or ".
      determineIdentifierQuoteString();

      // Find out what kind of database this is, for any non-standard SQL uses.
      determineDatabaseAffinity();
    }
    else
    {
      log.info("Database connector factory disabled.");
    }
  }

  @Override
  public void determineIdentifierQuoteString()
  {
    log.debug("Determining identifier quote string from database.");
    try (ConnectionMonitor monitor = getConnectionMonitor())
    {
      identifierQuoteString = monitor.getConnection().getMetaData().getIdentifierQuoteString();
    }
    catch (Exception e)
    {
      log.debug("Exception while reading identifier quote string.", e);
    }
    log.debug("Identifier quote string: {}", identifierQuoteString);
  }

  /**
   * Start this Asynchronous component.
   */
  @Override
  public void begin()
  {
    // Does nothing. The connection manager is started when we are
    // configured.
  }

  /**
   * Stop this Asynchronous component.
   */
  @Override
  public void end()
  {
  }

  /**
   * Is this Connector Factory enabled? In nearly all cases, this would be true;
   * but it will be false for applications that do not connect to a database at
   * all.
   */
  @Override
  public boolean isEnabled()
  {
    return enabled;
  }

  /**
   * Gets a ConnectionMonitor.
   */
  @Override
  public ConnectionMonitor getConnectionMonitor() throws SQLException
  {
    return new Monitor(this.dataSource.getConnection());
  }

  @Override
  public String getIdentifierQuoteString()
  {
    return identifierQuoteString;
  }

  @Override
  public DatabaseAffinity getDatabaseAffinity()
  {
    return this.databaseAffinity;
  }

  public void determineDatabaseAffinity()
  {
    log.debug("Determining DatabaseAffinity.");
    String dbProductName = null;
    try (ConnectionMonitor monitor = getConnectionMonitor())
    {
      dbProductName = identifierQuoteString = monitor.getConnection().getMetaData().getDatabaseProductName();
      this.databaseAffinity = DatabaseAffinity.getAffinityFromName(dbProductName);
    }
    catch (Exception e)
    {
      log.debug("Exception while determining database affinity.", e);
    }
    log.debug("DatabaseAffinity: {} from database product name {}",
        databaseAffinity, dbProductName);
  }

  /**
   * This is what client code uses to get and close connections.
   */
  private static class Monitor implements ConnectionMonitor
  {
    private Connection connection;

    public Monitor(Connection c)
    {
      this.connection = c;
    }

    @Override
    public Connection getConnection() throws SQLException
    {
      return this.connection;
    }

    @Override
    public void close() throws SQLException
    {
      this.connection.close();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException
    {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException
    {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
      throw new SQLFeatureNotSupportedException();
    }
  }
}
