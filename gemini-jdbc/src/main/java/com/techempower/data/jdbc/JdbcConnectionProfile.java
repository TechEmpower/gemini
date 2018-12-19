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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * Encapsulates each instance of a connection to a database.
 *   <p>
 * Applications do not typically interact directly with this class, but
 * rather interact with BasicConnectorFactory to get connectors and 
 * JdbcConnector to execute queries.
 *
 * @see JdbcConnector
 * @see BasicConnectorFactory
 */
public class JdbcConnectionProfile
{
  
  //
  // Constants.
  //
  
  public static final long CLOSE_DELAY = 10 * UtilityConstants.SECOND;
  
  private static final long UNUSED = -1;
  
  //
  // Member variables.
  //
  
  /**
   * The identifier number for this ConnectionProfile.
   */
  private int id = 0;
  
  /**
   * The total number of uses for this profile.
   */
  private long useCount = 1;

  /**
   * Number of connections closed (historical).
   */
  private int closeCount = 0;

  /**
   * Number of connections established (historical).
   */
  private int connectCount = 0;

  /**
   * Last used time stamp.
   */
  private volatile long lastUsed = System.currentTimeMillis();
  
  /**
   * A reference to the connection profile manager.
   */
  private final JdbcConnectionManager manager;
  
  /**
   * A ComponentLog for debugging.
   */
  private final ComponentLog log;

  /**
   * A reference to the connection.
   */
  private volatile ConnectionWrapper connection;
  
  /**
   * Should the Connection be closed immediately once the client releases this
   * Profile?
   */
  private boolean closeOnRelease;
  
  /**
   * The ID of the thread that currently has this profile reserved.
   */
  private final AtomicLong reservedForThread = new AtomicLong(UNUSED);
  
  /**
   * A ConnectionMonitor available for quick/lightweight access to the raw 
   * JDBC Connection.
   */
  private final Monitor connectionMonitor = new Monitor();
  
  /**
   * Constructor.
   */
  protected JdbcConnectionProfile(int id, JdbcConnectionManager manager)
  {
    this.manager = manager;
    this.log = manager.getLog();
    this.id = id;
  }
  
  /**
   * Gets the Profile's ID number.
   */
  public int getId()
  {
    return id;
  }
  
  /**
   * Sets the close-on-release flag.
   */
  protected void setCloseOnRelease(boolean closeOnRelease)
  {
    this.closeOnRelease = closeOnRelease;
  }

  /**
   * Establishes a new connection.
   */
  protected synchronized void establishDatabaseConnection()
  {
    // Close any existing connection.
    close(false);

    if (this.connection == null)
    {
      // Increment the historical connections counter.
      connectCount++;
      
      final JdbcConnectionAttributes attributes = manager.getAttributes();

      // Just some sanity checking (this isn't really necessary, but it
      // doesn't hurt).
      if (  (attributes != null)
         && (StringHelper.isNonEmpty(attributes.getJdbcURLPrefix()))
         && (StringHelper.isNonEmpty(attributes.getConnectString()))
         )
      {
        final String connectionUrl = attributes.getJdbcURLPrefix() 
            + attributes.getConnectString();

        // Connect to the database.
        try
        {
          log("Establishing database connection: [" 
            + connectionUrl + ", " 
            + attributes.getUsername() + "]", 
            LogLevel.MINIMUM);
          
          connection = new ConnectionWrapper(this, DriverManager.getConnection(connectionUrl,
            attributes.getUsername(), attributes.getPassword()));
        }
        catch (SQLException sqlexc)
        {
          log("SQL Exception while connecting.", LogLevel.ALERT, sqlexc);
          connection = null;
        }
      }
      else
      {
        log("JDBC URL prefix or connect string is empty; cannot connect to database.", LogLevel.ALERT);
      }
    }
  }

  /**
   * Gets the SQL Connection object.  Records the last used time to be now.
   */
  protected Connection getConnection()
  {
    // If the Connection has not been established or was closed, try to
    // establish a new Connection.
    if (!isConnectionAvailable())
    {
      establishDatabaseConnection();
    }
    return connection;
  }
  
  /**
   * Write something to the log, with a prefix identifying the current
   * connection number and reservation-holding thread ID.
   */
  protected void log(String string)
  {
    log.log("[c" + getId() + ";t" + reservedForThread.get() + "] " + string);
  }
  protected void log(String string, int level)
  {
    log.log("[c" + getId() + ";t" + reservedForThread.get() + "] " + string, level);
  }
  protected void log(String string, int level, Throwable throwable) 
  {
    log.log("[c" + getId() + ";t" + reservedForThread.get() + "] " + string, level, throwable);
  }
  protected void log(String string, Throwable throwable) 
  {
    log.log("[c" + getId() + ";t" + reservedForThread.get() + "] " + string, throwable);
  }
  
  /**
   * Executes a keep-alive query on the Connection without affecting the
   * "last used" time.
   */
  protected synchronized void keepAlive()
  {
    final Connection connect = connection;

    if (connect != null)
    {
      // Claim the Profile for ourselves right now.
      if (claim(false))
      {
        final JdbcConnectionManager mgr = manager;
  
        Runnable keepAlive = new Runnable()
        {
          @Override
          public void run()
          {
            try
            {
              final String query = mgr.getAttributes().getTestQuery();
              final String expectedResult = mgr.getAttributes().getTestValue();
  
              try (Statement statement = connect.createStatement(ResultSet.TYPE_FORWARD_ONLY, 
                    ResultSet.CONCUR_READ_ONLY))
              {
                try (ResultSet resultSet = statement.executeQuery(query))
                {
                  if (resultSet.next())
                  {
                    final String actualResult = resultSet.getString(1);
                    
                    // We don't actually have any defined behavior if the expected and
                    // actual results don't match.  We just ran the query to keep the
                    // connection alive.  But let's log something if they differ.
                    if (!expectedResult.equals(actualResult))
                    {
                      log("Expected \"" + expectedResult 
                          + "\" but received \"" + actualResult + "\".", LogLevel.ALERT);
                    }
                  }
                  else
                  {
                    log("No results from keep-alive query.", LogLevel.ALERT);
                  }
                }
              }
            }
            catch (SQLException sqlexc)
            {
              log("SQLException during keep-alive: ", LogLevel.ALERT, sqlexc);
              
              // Close this connection (it will be reconnected on next use).
              close(true);
            }
            finally
            {                
              // Release the profile for use by clients.
              close();
            }
          }
        };
        
        try
        {
          ThreadHelper.submit(keepAlive);
        }
        catch (RejectedExecutionException rje)
        {
          log("Cannot keep alive connection: ", rje);
          close();
        }
      }
      else
      {
        log("Unable to run keep-alive. This is normal if a query is running.", LogLevel.DEBUG);
        
        // If we were unable to claim this profile, let's see how long it has
        // been executing a query.  If it's longer than the query timeout, we
        // will close the connection.
        final long time = System.currentTimeMillis() - lastUsed;
        if (time > manager.getAttributes().getAbortTimeout())
        {
          log("Query timeout.  Stopping query running for " + time + "ms.");
          close(true);
        }
      }
    }
  }
  
  /**
   * Returns whether or not this profile is in use.
   */
  public boolean isInUse()
  {
    return reservedForThread.get() != UNUSED;
  }
  
  /**
   * Notifies listeners of a query starting.
   */
  protected void notifyListenerOnClaim()
  {
    if (manager.getListener() != null)
    {
      manager.getListener().queryStarting();
    }
  }
  
  /**
   * Notifies listeners of a query completing.
   */
  protected void notifyListenerOnRelease()
  {
    if (manager.getListener() != null)
    {
      manager.getListener().queryCompleting();
    }
  }

  /**
   * Claims this connection for use.
   */
  public boolean claim()
  {
    boolean claimed = false; 
    try
    {
      claimed = claim(true);
      return claimed;
    }
    finally
    {
      if (claimed)
      {
        notifyListenerOnClaim();
      }
    }
  }
  
  /**
   * Claims this connection for use, optionally without tracking this as a
   * real use.  The keep-alive code call claim(false) so that keep alive 
   * requests do not reset the last-use timestamp.  
   */
  public boolean claim(boolean trackUsage)
  {
    final long threadId = Thread.currentThread().getId();
    if (reservedForThread.compareAndSet(UNUSED, threadId))
    {
      // Record the last used time.
      if (trackUsage)
      {
        lastUsed = System.currentTimeMillis();
        useCount++;
      }

      return true;
    }
    
    return false;
  }
  
  /**
   * Releases this Connection Profile for use by other clients, but only if
   * we are called by the proper client.  This allows for "failsafe" 
   * extraneous calls to release.
   */
  public void close()
  {
    final boolean close = closeOnRelease;

    reservedForThread.set(UNUSED);
    
    try
    {
      if (close)
      {
        close(false);
      }
    }
    finally
    {
      notifyListenerOnRelease();
    }
  }
  
  /**
   * Is a Connection reference available?
   */
  public boolean isConnectionAvailable()
  {
    return (connection != null);
  }
  
  /**
   * Gets the time of the last use.  The last use is determined to be the
   * last time getConnection was called.
   */
  public long getLastUse()
  {
    return lastUsed;
  }

  /**
   * Gets the manager reference.
   */
  protected JdbcConnectionManager getManager()
  {
    return manager;
  }
  
  /**
   * Gets the connection profile's closed state.  If the connection is null,
   * returns true.  Otherwise, returns false.
   */
  public boolean isClosed()
  {
    return (connection == null);
  }

  /**
   * Gets the connection's closed flag.  If the connection is null,
   * returns true.  If the parameter checkConnection is true, then this
   * method will also call the connection's isClosed method which
   * may result in a database hit of some kind.
   */
  public boolean isClosed(boolean checkConnection)
  {
    try
    {
      if (connection != null)
      {
        if (checkConnection)
        {
          return connection.isClosedUnderlyingConnection();
        }
        else
        {
          // Connection is non-null, but we're not going to call con.isClosed,
          // so we assume that it's not closed.
          return false;
        }
      }
    }
    catch (SQLException sqlexc)
    {
      log("SQLException while determining connection's closed status.", LogLevel.ALERT, sqlexc);
    }

    // Connection is null, or we got an exception when asking if the
    // Connection is closed.  So we'll assume it's closed.
    return true;
  }

  /**
   * Closes the connection.
   * 
   * @param onNewThread true if the Connection should be closed on a new
   * thread.
   */
  protected synchronized void close(boolean onNewThread)
  {
    if (connection != null)
    {
      // Increment the historical closes counter.
      closeCount++;

      final Runnable closer = new Closer(connection);
      if (onNewThread)
      {
        // Queue the closer to run in 10 seconds.
        ThreadHelper.schedule(closer, CLOSE_DELAY, TimeUnit.MILLISECONDS);
      }
      else
      {
        // Run the closer inline and immediately.
        closer.run();
      }
      
      connection = null;
    }
  }
  
  /**
   * Used by the close() method above. 
   */
  private class Closer implements Runnable
  {
    private final ConnectionWrapper localConnection;
    
    public Closer(ConnectionWrapper connection)
    {
      localConnection = connection;
    }
    
    @Override
    public void run()
    {
      debug("Closing connection profile " + JdbcConnectionProfile.this.id + ".");
      
      try
      {
        localConnection.closeUnderlyingConnection();
      }
      catch (SQLException sqlexc)
      {
        debug("SQLException while closing connection: " + sqlexc);
      }
    }
    
    public void debug(String debug)
    {
      JdbcConnectionProfile.this.log(debug);
    }
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    final StringList attributeList = new StringList("; ");

    synchronized (this)
    {
      if (isInUse())
      {
        attributeList.add("in-use (thread " + reservedForThread.get() + ")");
      }
      else
      {
        attributeList.add("idle");
      }
    }
    if (isClosed())
    {
      attributeList.add("CLOSED");
    }

    return "JdbcCP "
        + "[id: " + this.id 
        + "; " + attributeList 
        + "; uses: " + this.useCount 
        + "; connections: " + this.connectCount 
        + "; closes: " + this.closeCount 
        + (this.lastUsed > 0 ? "; last used " + DateHelper.getHumanDifference(this.lastUsed, 2) + " ago" : "")
        + "]";
  }
  
  /**
   * Returns the DatabaseMetaData for the current connection or 
   * null if there is no connection or there was an error retrieving the meta data 
   */
  protected DatabaseMetaData getConnectionMetaData()
  {
    if (this.connection != null)
    {
      try
      {
        return this.connection.getMetaData();
      }
      catch (SQLException exc)
      {
        log("Exception while fetching meta data.", LogLevel.ALERT, exc);
      }
    }
    
    return null;
  }
  
  /**
   * Gets the PassthroughConnector for this connection profile.
   */
  public ConnectionMonitor getMonitor()
  {
    return this.connectionMonitor;
  }
  
  /**
   * A special implementation of DatabaseConnector that allows pass-through
   * access to the Connection.
   */
  private class Monitor implements ConnectionMonitor
  {
    @Override
    public Connection getConnection()
    {
      return JdbcConnectionProfile.this.getConnection();
    }

    @Override
    public void close() throws SQLException
    {
      JdbcConnectionProfile.this.close();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
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

}  // End JdbcConnectionProfile.
