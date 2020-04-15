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

import static com.techempower.gemini.Configurator.*;
import static com.techempower.util.EnhancedProperties.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Map.*;

import com.techempower.*;
import com.techempower.asynchronous.*;
import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import com.zaxxer.hikari.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of ConnectorFactory that is a thin wrapper around the
 * HikariCP connection pool.
 * <p>
 * The following attributes are read from the properties file for configuring
 * this component:
 * <ul>
 * <li>[prefix]Enabled - whether this ConnectorFactory is enabled.</li>
 * <li>[prefix]PropertiesFile - where to find the hikari.properties file for
 * configuring HikariCP.</li>
 * </ul>
 * <p>
 * Note that the default prefix is 'db.HikariCP.', so unless you specify
 * otherwise in your constructor call, you would identify the hikari.properties
 * file location by setting the 'db.HikariCP.PropertiesFile' property in your
 * Gemini .conf file.
 */
public class HikariCPConnectorFactory implements ConnectorFactory, Configurable, Asynchronous
{
  //
  // Constants.
  //

  public static final String DEFAULT_PROPERTY_PREFIX = "db.HikariCP.";
  public static final String ENV_VAR_MACRO_START = MACRO_START + PROP_ENVIRONMENT_PREFIX;

  //
  // Member variables.
  //

  private final String propertyPrefix;
  private final Logger log = LoggerFactory.getLogger(getClass());

  private HikariConfig hikariConfig;
  private HikariDataSource dataSource;

  private boolean enabled = true;
  private String hikariPropsFile = null;
  private String identifierQuoteString = " ";
  private DatabaseAffinity databaseAffinity;

  //
  // Member methods.
  //

  /**
   * Constructor. If the propertyPrefix is passed as an empty String, the default
   * "db.HikariCP." will be used.
   *
   * @param propertyPrefix
   *          the prefix to apply to all property names in the conf file; default
   *          is "db.HikariCP."
   */
  public HikariCPConnectorFactory(String propertyPrefix)
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

  @Deprecated(forRemoval = true)
  public HikariCPConnectorFactory(TechEmpowerApplication application,
                                  String propertyPrefix)
  {
    this(propertyPrefix);
  }

  /**
   * Configure this component. For a HikariCPConnectorFactory, it is expected that
   * the db.HikariCP.PropertiesFile will define the location of the
   * hikari.properties file. See the HikariCP documentation for details on the
   * values to put in hikari.properties.
   */
  @Override
  public void configure(EnhancedProperties rawProps)
  {
    // Read parameters.
    final EnhancedProperties.Focus props = rawProps.focus(propertyPrefix);
    enabled = props.getBoolean("Enabled", true);
    hikariPropsFile = props.get("PropertiesFile", "src/main/webapp/WEB-INF/configuration/hikari.properties");

    // Only proceed if this connector factory is enabled.
    if (enabled)
    {
      // Close existing DataSource, if exists.
      end();

      // To support specifying environment variables inside the hikari.properties
      // file, we need to find environment variable references in the property values,
      // do the lookup, and pass along the environment variable values to HikariCP.
      final File propFile = new File(hikariPropsFile);
      final Properties hikariPropsReady = new Properties();

      // Specify System.out as the default logWriter.
      hikariPropsReady.put("dataSource.logWriter", new PrintWriter(System.out));

      try (final InputStream is = propFile.isFile() ? new FileInputStream(propFile)
          : this.getClass().getResourceAsStream(hikariPropsFile)) {
        if (is != null) {
          Properties hikariPropsRaw = new Properties();
          hikariPropsRaw.load(is);
          hikariPropsRaw.forEach((key, value) -> {
            String envVarName = extractEnvironmentVariableName(value.toString());
            if (envVarName != null) {
              // Read the environment variable.
              String envVarValue = System.getenv(envVarName);
              if (envVarValue != null) {
                // We have an environment variable, so use that to replace the relevant portion
                // of the string.
                hikariPropsReady.setProperty(key.toString(), StringHelper.replaceSubstrings(value.toString(),
                    ENV_VAR_MACRO_START + envVarName + MACRO_END, envVarValue));
              } else {
                // We do not have an environment variable, so use the original value.
                hikariPropsReady.setProperty(key.toString(), value.toString());
              }
            } else {
              // Pass along the property unchanged if not referencing an environment
              // variable.
              hikariPropsReady.setProperty(key.toString(), value.toString());
            }
          });
        } else {
          log.info("Cannot find property file: " + hikariPropsFile);
        }
      } catch (IOException io) {
        log.info("Failed to read property file", io);
      }

      // Pass to HikariCP our prepared Properties object with any relevant environment
      // variables in place.
      hikariConfig = new HikariConfig(hikariPropsReady);
      dataSource = new HikariDataSource(hikariConfig);
      log.info("Connected: {}", dataSource.getJdbcUrl());

      log.debug("HikariDataSource: {}", dataSource);
      for (Entry<Object, Object> e : dataSource.getDataSourceProperties().entrySet())
      {
        // Log all properties except password.
        if (!StringHelper.containsIgnoreCase(e.getKey() + "", "password"))
        {
          log.debug("{}: {}", e.getKey(), e.getValue());
        }
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

  /**
   * If there is an environment variable declared, return its name. Otherwise
   * return null.
   *
   * @param s A string like "${Environment.FOO}"
   * @return If s is "${Environment.FOO}", returns "FOO"
   */
  private String extractEnvironmentVariableName(String s)
  {
    int endLocation;
    String value = StringHelper.trim(s);

    // Find the macro beginning marker, ${{Environment.
    int markerLocation = s.indexOf(ENV_VAR_MACRO_START);
    while (markerLocation >= 0) {
      // Find the macro ending marker, }
      endLocation = value.indexOf(MACRO_END, markerLocation);
      if (endLocation >= 0) {
        // Get the name to lookup.
        return value.substring(markerLocation + ENV_VAR_MACRO_START.length(), endLocation);
      }
    }
    return null;
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
    if (this.dataSource != null)
    {
      this.dataSource.close();
    }
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
