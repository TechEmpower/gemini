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

import java.math.*;
import java.sql.*;
import java.util.*;

import com.techempower.data.*;
import com.techempower.data.mapping.*;
import com.techempower.helper.*;
import com.techempower.log.*;

/**
 * Provides a simple gateway to the database, through which queries can be
 * executed and result sets gathered.  Manages JDBC entities such as
 * Connections, Drivers, Statements, and ResultSets.
 *    <p>
 * In concert with BasicConnectorFactory and JdbcConnectionManager, a pool
 * of database Connections is maintained.
 *    <p>
 * JdbcConnector objects are not handled instantiated directly by an 
 * application, but rather through a ConnectorFactory such as the
 * BasicConnectorFactory.
 *    <p>
 * Note that calling close() at the end of every query is absolutely
 * mandatory and should be done in a "finally" clause.
 */
public class JdbcConnector
  implements DatabaseConnector
{

  // ------------------------------------------------------------------------
  // Constants.
  //

  public  static final String                COMPONENT_CODE          = "jdbc";  // Component code for debug.
  public  static final String                NULL_VALUE_REPLACE      = "[none]";
  
  //
  // Private member variables.
  //

  private final        JdbcConnectionManager manager;
  private final        ComponentLog          componentLog;  // A ComponentLog instance to use for logging.

  private              int                   tryCount                = 0;     // How many times has this connector tried the query?
  private              Statement             statement               = null;  // Statement object.
  private              ResultSet             resultSet               = null;  // The result set.
  private              boolean               nextThrewException      = true;  // Did the last next() call fail?
  private              boolean               readOnly                = true;  // Currently in read-only mode.
  private              boolean               forwardOnly             = false; // Currently in forward-only mode.
  private              boolean               safeMode                = false; // Safe mode for query execution.
  private              String                query                   = null;  // Reference to the query.
  private              int                   rowCount                = -1;    // The number of records in the results.  Cached if > -1.
  private              JdbcConnectionProfile connectionProfile       = null;  // The JdbcConnectionProfile in use.
  private              JdbcConnectionProfile forcedConnectionProfile = null;  // A reference to a forced-new-Connection Profile.
  private              boolean               forceNewConnection      = false; // Forces each query to run on a new Connection.
  private              Connection            forcedConnection        = null;  // Not initialized unless force new Connections is true.
  
  // ------------------------------------------------------------------------
  // Member methods.
  //

  /**
   * Constructor.
   * 
   * @param manager The JDBC Connection Manager.
   * @param query The query to execute; can be set via setQuery as well.
   * @param readOnly The query is not an updatable query; this is usually
   *        the case, so this parameter should usually be true.
   * @param forwardOnly The query's cursor will only support moving forward;
   *        used for increased speed.
   */
  protected JdbcConnector(JdbcConnectionManager manager, String query,
    boolean readOnly, boolean forwardOnly)
  {
    //
    // Copy parameters.
    //

    this.manager = manager;
    this.componentLog = manager.getLog();
    this.setReadOnly(readOnly);
    this.setForwardOnly(forwardOnly);
    this.query = query;
  }
  
  /**
   * Constructor.  Assumes read-only and forward-only result sets.
   * 
   * @param manager The JDBC Connection Manager.
   * @param query The query to execute; can be set via setQuery as well.
   */
  protected JdbcConnector(JdbcConnectionManager manager, String query)
  {
    this(manager, query, true, true);
  }
  
  /**
   * Set the SQL query for this connector object.  Doing so will
   * close any current ResultSet.
   *
   * @param query the SQL query--not immediately executed.
   */
  @Override
  public void setQuery(String query)
  {
    log("setQuery: " + query, LogLevel.DEBUG);

    // Close any statement or result set that may be open.
    close(false);

    // Set the query.
    this.query = query;

    // Reset nextThrewException.
    this.nextThrewException = true;

    // Reset try count
    this.tryCount = 0;
  }

  /**
   * Get the SQL query for this connector object.
   */
  @Override
  public String getQuery()
  {
    return this.query;
  }

  /**
   * Sets the read-only flag.  The read-only flag should be set to true for
   * most applications.  JdbcConnector does not itself support any features
   * of an updatable result set, anyway.
   *   <p>
   * By default, the read-only flag is true.
   *
   * @param readOnly the new state of the flag.
   */
  @Override
  public void setReadOnly(boolean readOnly)
  {
    this.readOnly = readOnly;
  }

  /**
   * Gets the read-only flag.
   */
  @Override
  public boolean getReadOnly()
  {
    return this.readOnly;
  }

  /**
   * Logs a String to the ComponentLog if it has been provided.
   */
  protected void log(String toLog, int logLevel)
  {
    this.componentLog.log(toLog, logLevel);
  }

  /**
   * Logs a String to the ComponentLog if it has been provided, plus a
   * Throwable if you are lucky enough to have one.
   */
  protected void log(String toLog, int logLevel, Throwable t)
  {
    this.componentLog.log(toLog, logLevel, t);
  }

  /**
   * Gets the number of times that this connector has tried to run the
   * query.
   */
  public int getTryCount()
  {
    return this.tryCount;
  }

  /**
   * Sets the force new connection flag.  When enabled, all queries will
   * be forced to run on new database Connections.  This is not ideal because
   * establishing connections is time consuming under heavy load.
   *  <p>
   * Forcing new connections is generally required in multi-threaded systems
   * that use temporary tables.  Temporary tables are usually scoped for
   * individual connections.
   */
  @Override
  public void setForceNewConnection(boolean forceNewConnection)
  {
    this.forceNewConnection = forceNewConnection;
  }

  /**
   * Gets the force new connection flag.
   */
  public boolean getForceNewConnection()
  {
    return this.forceNewConnection;
  }

  /**
   * Sets the forward-only flag.  This can be used to increase the
   * performance of the query and navigation through the result set.
   * However, use of moveAbsolute and first are not permitted in this
   * mode.
   *
   * @param forwardOnly the new state of the flag.
   */
  @Override
  public void setForwardOnly(boolean forwardOnly)
  {
    this.forwardOnly = forwardOnly;
  }

  /**
   * Gets the forward-only flag.
   */
  @Override
  public boolean getForwardOnly()
  {
    return this.forwardOnly;
  }

  /**
   * Sets the Safe Mode flag.  Queries that are executed in "safe mode" will
   * properly propagate JdbcExceptions encapsulated in JdbcConnectorExceptions.
   * Note that the default for the Safe Mode flag is FALSE, meaning that
   * exceptions are not propagated.  Also note that it is possible to override
   * the Safe Mode flag by calling runQuery(boolean) which allows a safe
   * mode flag to be passed as runQuery call time.
   *
   * @param safeMode the new state of the flag.
   */
  public void setSafeMode(boolean safeMode)
  {
    this.safeMode = safeMode;
  }

  /**
   * Gets the Safe Mode flag.
   */
  public boolean getSafeMode()
  {
    return this.safeMode;
  }

  /**
   * Moves to first element in result set.
   */
  @Override
  public void first()
  {
    if (this.forwardOnly)
    {
      log("Call to first on a forward-only query.", LogLevel.NORMAL);
      return;
    }

    try
    {
      this.nextThrewException = !this.resultSet.first();
    }
    catch (SQLException exc)
    {
      log("Exception on first(): " + exc, LogLevel.NORMAL);
      this.nextThrewException = true;
    }
  }

  /**
   * Moves to last element in result set.
   */
  @Override
  public void last()
  {
    if (this.forwardOnly)
    {
      log("Call to last on a forward-only query.", LogLevel.NORMAL);
      return;
    }

    try
    {
      this.nextThrewException = !this.resultSet.last();
    }
    catch (Exception exc)
    {
      log("Exception on last(): " + exc, LogLevel.NORMAL);
      this.nextThrewException = true;
    }
  }

  /**
   * Moves to the next element in the result set.
   */
  @Override
  public void next()
  {
    try
    {
      if (this.resultSet == null)
      {
        this.nextThrewException = true;
        return;
      }
      this.nextThrewException = !this.resultSet.next();
    }
    catch (SQLException exc)
    {
      this.nextThrewException = true;
    }
  }

  /**
   * Determines if there are more rows in the result set.
   *
   * @return true if there are more rows in the result set.
   */
  @Override
  public boolean more()
  {
    return this.resultSet != null && !this.nextThrewException;
  }

  /**
   * Moves to an absolute position in the result set.
   *
   * @param position the absolute row number
   */
  @Override
  public void moveAbsolute(int position)
  {
    if (this.forwardOnly)
    {
      log("Call to moveAbsolute on a forward-only query.", LogLevel.NORMAL);
      return;
    }

    try
    {
      this.resultSet.absolute(position + 1);
    }
    catch (SQLException exc)
    {
      log("Exception while moving to absolute position " + position + ": " + exc, LogLevel.NORMAL);
    }
  }

  /**
   * Gets the row number in the result set.  Will return 0 in the event of
   * an error.
   */
  @Override
  public int getRowNumber()
  {
    try
    {
      return this.resultSet.getRow();
    }
    catch (SQLException exc)
    {
      log("Exception while getting row number: " + exc, LogLevel.NORMAL);
    }

    return 0;
  }

  /**
   * Gets a String array representing the field names in the result set.
   */
  @Override
  public String[] getFieldNames()
  {
    try
    {
      ResultSetMetaData metadata = this.resultSet.getMetaData();
      int fieldCount = metadata.getColumnCount();

      // Gets the column names.
      String[] toReturn = new String[fieldCount];
      for (int i = 1; i < fieldCount + 1; i++)
      {
        toReturn[i - 1] = metadata.getColumnName(i);
      }

      return toReturn;
    }
    catch (SQLException exc)
    {
      log("Exception while gathering field names: " + exc, LogLevel.NORMAL);
    }
    return new String[0];
  }

  /**
   * Gets a String array representing the field names in the result set.
   */
  @Override
  public int[] getFieldTypes()
  {
    try
    {
      ResultSetMetaData metadata = this.resultSet.getMetaData();
      int fieldCount = metadata.getColumnCount();

      // Gets the column names.
      int[] toReturn = new int[fieldCount];
      for (int i = 1; i < fieldCount + 1; i++)
      {
        toReturn[i - 1] = metadata.getColumnType(i);
      }

      return toReturn;
    }
    catch (SQLException exc)
    {
      log("Exception while gathering field names: " + exc, LogLevel.NORMAL);
    }
    return new int[0];
  }

  /**
   * From the results set, returns a field's contents by the field's name.
   *
   * @param fieldName the name of the field
   */
  @Override
  public String getFieldByName(String fieldName)
  {
    try
    {
      return this.resultSet.getString(fieldName);
    }
    catch (SQLException exc)
    {
      log("Exception while retrieving field " + fieldName + ": " + exc, LogLevel.NORMAL);
      return null;
    }
  }

  /**
   * A variant of getFieldByName that will return a default value if null is
   * gathered from the database.
   *
   * @param fieldName the name of the field
   * @param defaultValue the default value to return in the event of a null
   */
  @Override
  public String getField(String fieldName, String defaultValue)
  {
    String result = getFieldByName(fieldName);
    if (result != null)
    {
      return result;
    }
    else
    {
      return defaultValue;
    }
  }

  /**
   * A variant on getIntegerFieldByName that is just faster to type.
   * Returns 0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public int getInt(String fieldName)
  {
    try
    {
      return this.resultSet.getInt(fieldName);
    }
    catch (SQLException exc)
    {
      return 0;
    }
  }

  /**
   * From the result set, returns a field's contents as a Clob value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public String getClob(String fieldName)
  {
    try
    {
      final long length = Math.min(resultSet.getClob(fieldName).length(), Integer.MAX_VALUE);
      return resultSet.getClob(fieldName).getSubString(1, (int)length);
    }
    catch (SQLException exc)
    {
      return null;
    }
  }


  /**
   * From the result set, returns a field's contents as a byte[].
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  public byte[] getBlob(String fieldName)
  {
    try
    {
      return this.resultSet.getBlob(fieldName).getBytes(0, (int)this.resultSet.getBlob(fieldName).length());
    }
    catch (SQLException exc)
    {
      return null;
    }
  }


  /**
   * From the result set, returns a field's contents as a Date value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public java.sql.Date getDate(String fieldName)
  {
    try
    {
      return this.resultSet.getDate(fieldName);
    }
    catch (SQLException exc)
    {
      return null;
    }
  }

  /**
   * From the result set, returns a field's contents as a Calendar object.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public java.util.Calendar getDateAsCalendar(String fieldName)
  {
    java.util.Date date = this.getTimestamp(fieldName);

    if(date != null)
    {
      Calendar cal = DateHelper.getCalendarInstance();
      cal.setTime(date);
      return cal;
    }

    return null;
  }

  /**
   * From the result set, returns a field's contents as a Time value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public java.sql.Time getTime(String fieldName)
  {
    try
    {
      return this.resultSet.getTime(fieldName);
    }
    catch (SQLException exc)
    {
      return null;
    }
  }

  /**
   * From the result set, returns a field's contents as a Timestamp value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public java.sql.Timestamp getTimestamp(String fieldName)
  {
    try
    {
      return this.resultSet.getTimestamp(fieldName);
    }
    catch (SQLException exc)
    {
      return null;
    }
  }

  /**
   * From the result set, returns a field's contents as a boolean value.
   * Returns false in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public boolean getBoolean(String fieldName)
  {
    try
    {
      return this.resultSet.getBoolean(fieldName);
    }
    catch (SQLException exc)
    {
      return false;
    }
  }

  /**
   * From the result set, returns a field's contents as a BigDecimal value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public BigDecimal getBigDecimal(String fieldName)
  {
    try
    {
      return this.resultSet.getBigDecimal(fieldName);
    }
    catch (SQLException exc)
    {
      return null;
    }
  }

  /**
   * From the result set, returns a field's contents as a float value.
   * Returns 0.0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public float getFloat(String fieldName)
  {
    try
    {
      return this.resultSet.getFloat(fieldName);
    }
    catch (SQLException exc)
    {
      return 0.0F;
    }
  }

  /**
   * From the result set, returns a field's contents as a double value.
   * Returns 0.0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public double getDouble(String fieldName)
  {
    try
    {
      return this.resultSet.getDouble(fieldName);
    }
    catch (SQLException exc)
    {
      return 0.0D;
    }
  }

  /**
   * From the result set, returns a field's contents as a byte value.
   * Returns 0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public byte getByte(String fieldName)
  {
    try
    {
      return this.resultSet.getByte(fieldName);
    }
    catch (SQLException exc)
    {
      return 0;
    }
  }

  /**
   * From the result set, returns a field's contents as a short value.
   * Returns 0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public short getShort(String fieldName)
  {
    try
    {
      return this.resultSet.getShort(fieldName);
    }
    catch (SQLException exc)
    {
      return 0;
    }
  }

  /**
   * From the result set, returns a field's contents as a long value.
   * Returns 0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public long getLong(String fieldName)
  {
    try
    {
      return this.resultSet.getLong(fieldName);
    }
    catch (SQLException exc)
    {
      return 0L;
    }
  }

  /**
   * Gets the row count after a query has been executed.  Acceptable to
   * return 0 if a query has not been executed or -1 if the implementation
   * does not successfully provide this functionality.
   *
   * Also acceptable to move the cursor to the first element after this
   * operation.
   */
  @Override
  public int getRowCount()
  {
    if (this.resultSet != null)
    {
      // If we have a cached value, return.
      if (this.rowCount > -1)
      {
        return this.rowCount;
      }

      this.rowCount = 0;

      // Count the rows!
      first();
      while (more())
      {
        this.rowCount++;
        next();
      }
      first();

      return this.rowCount;
    }
    else
    {
      // No result set!
      return 0;
    }
  }

  /**
   * Closes the result set.
   *   <p>
   * This version <i>will</i> close a forced connection if one is being used.
   */
  @Override
  public void close()
  {
    close(true);
  }

  /**
   * Closes the result set.
   *
   * @param closeConnection If a forced connection is being used, close the
   *        forced connection.
   */
  public void close(boolean closeConnection)
  {
    // Close the result set, if there is one.
    if (this.resultSet != null)
    {
      try
      {
        this.resultSet.close();
      }
      catch (SQLException sqlexc)
      {
        log("Exception while closing result set: " + sqlexc, LogLevel.ALERT);
      }
      this.resultSet = null;
    }

    // Close the statement, if there is one.
    if (this.statement != null)
    {
      try
      {
        this.statement.close();
      }
      catch (SQLException sqlexc)
      {
        log("Exception while closing statement: " + sqlexc, LogLevel.ALERT);
      }
      this.statement = null;
    }

    // Notify the Profile that we're done using it.
    if (this.connectionProfile != null)
    {
      this.connectionProfile.close();
      this.connectionProfile = null;
    }

    // Close the forced connection, if there is one.
    if ( (closeConnection)
      && (this.forcedConnection != null)
      )
    {
      try
      {
        this.forcedConnection.close();
      }
      catch (SQLException sqlexc)
      {
        log("Exception while closing forced connection: " + sqlexc, LogLevel.ALERT);
      }
      this.forcedConnection = null;
    }
  }

  /**
   * Runs an UPDATE or INSERT query and then automatically closes the
   * Connector.  The default mode is unsafe mode mode (that is, exceptions are
   * swallowed by this method).  To run an update query in safe mode, call 
   * runUpdateQuery(true) or call setSafeMode(true) prior to calling 
   * runUpdateQuery.
   *
   * Running a query using one of the runUpdate methods is necessary in
   * order to get back the number of rows that were updated.
   */
  @Override
  public int runUpdateQuery()
  {
    return runUpdateQuery(this.safeMode);
  }

  /**
   * Runs an UPDATE or INSERT query using safe mode (that is, exceptions are
   * packaged as JdbcConnectorErrors and re-thrown by this method).
   */
  public int runUpdateQuerySafe()
  {
    return runUpdateQuery(true);
  }

  /**
   * Runs an UPDATE or INSERT query using unsafe mode (that is, exceptions are
   * packaged as JdbcConnectorErrors and re-thrown by this method).
   */
  public int runUpdateQueryUnsafe()
  {
    return runUpdateQuery(false);
  }

  /**
   * Runs an UPDATE or INSERT query and then automatically closes the 
   * Connector.
   *
   * @param safe if true, exceptions will be thrown by this method.  If
   *        false, exceptions will be swallowed.
   */
  @Override
  public int runUpdateQuery(boolean safe)
  {
    // Close the query if necessary.
    close(false);

    // Remove the row count cache.
    int updateRowCount = -1;

    // Determine which connection to use.
    this.connectionProfile = getDbConnection();

    // Do we have a valid connection to use?
    if (this.connectionProfile != null)
    {
      // Ensure that the load is decremented.
      try
      {
        // Repeat until we're not instructed to try again.
        int instruction = DatabaseConnectionListener.INSTRUCT_RETRY;
        while (instruction == DatabaseConnectionListener.INSTRUCT_RETRY)
        {
          instruction = DatabaseConnectionListener.INSTRUCT_DO_NOTHING;

          // Increment the try count.
          this.tryCount++;

          try
          {
            // We don't care about resultsets for updates.
            Connection connection = this.connectionProfile.getConnection();
            if (connection != null)
            {
              this.statement = connection.createStatement();
              logWarnings(this.statement);
            }
            else
            {
              log("runUpdateQuery: ConnectionProfile's Connection is null!", LogLevel.CRITICAL);
              return RUNQUERY_EXCEPTION;
            }
          }
          catch (SQLException sqlexc)
          {
            if (safe)
            {
              throw new JdbcConnectorError("runUpdateQuery: Could not create statement.", sqlexc);
            }
            else
            {
              log("runUpdateQuery: SQL Exception while creating statement: " + sqlexc, LogLevel.ALERT);
              notifyListener(JdbcConnectorConstants.METHOD_RUN_UPDATE_QUERY, sqlexc);

              // If we could not create the Statement, we can't do anything
              // but leave this method.  (Revisit this in the future--can we
              // do anything else automatically?)
              return RUNQUERY_EXCEPTION;
            }
          }

          try
          {
            logWarnings(this.statement);
            // Run the update query.
            this.statement.execute(this.query);

            // Gather the count of affected rows.
            updateRowCount = this.statement.getUpdateCount();
            logWarnings(this.statement);
          }
          catch (SQLException sqlexc)
          {
            // Close the statement.
            try
            {
              this.statement.close();
            }
            catch(Exception e) {}
            this.statement = null;
            
            // Did we get disconnected?
            checkForDisconnectExceptions(sqlexc, this.connectionProfile);

            if (safe)
            {
              throw new JdbcConnectorError("runUpdateQuery: Could not execute the query.", sqlexc);
            }
            else
            {
              log("runUpdateQuery: SQL Exception executing query: " + sqlexc, LogLevel.ALERT);
              instruction = notifyListener(JdbcConnectorConstants.METHOD_RUN_UPDATE_QUERY, sqlexc);
              if (instruction == DatabaseConnectionListener.INSTRUCT_DO_NOTHING)
              {
                return RUNQUERY_EXCEPTION;
              }
            }
          }
        }
      }
      finally
      {
        // Automatically close the Connector.
        close(false);
      }
    }
    else
    {
      if (safe)
      {
        throw new JdbcConnectorError("runUpdateQuery: No valid connection available, aborting query.", null);
      }
      else
      {
        log("runUpdateQuery: No valid connection available, aborting query.", LogLevel.CRITICAL);
      }
    }

    return updateRowCount;
  }

  /**
   * Runs the query.  The default mode is unsafe mode, which swallows SQL
   * Exceptions.  To run a query in safe mode, call runQuery(true) or call
   * setSafeMode(true) prior to calling runQuery.
   *    <p>
   * The query set by setQuery or the constructor is not actually executed
   * until a call to runQuery is made.
   * 
   * @return An integer whose meaning is defined by the implementation;
   * typically '0' indicates success.
   */
  @Override
  public int runQuery()
  {
    return runQuery(this.safeMode);
  }

  /**
   * Runs a query with the safe mode flag set to true.  This is just a
   * convenience method for calling runQuery(true).
   * 
   * @return An integer whose meaning is defined by the implementation;
   * typically '0' indicates success.
   */
  public int runQuerySafe()
  {
    return runQuery(true);
  }

  /**
   * Runs a query with the safe mode flag set to false.  This is just a
   * convenience method for calling runQuery(false).
   * 
   * @return An integer whose meaning is defined by the implementation;
   * typically '0' indicates success.
   */
  public int runQueryUnsafe()
  {
    return runQuery(false);
  }

  /**
   * Runs a query with an optional safe-mode parameter.  Safe mode will
   * throw SQLExceptions as encapsulated Errors (be certain to catch
   * JdbcConnectorError).
   *    <p>
   * The query set by setQuery or the constructor is not actually executed
   * until a call to runQuery is made.
   *
   * @param safe Run the query in safe mode (exception throwing)?
   * 
   * @return An integer whose meaning is defined by the implementation;
   * typically '0' indicates success.
   */
  @Override
  public int runQuery(boolean safe)
  {
    // Close the query if necessary.
    close(false);

    // Remove the row count cache.
    this.rowCount = -1;

    // Determine which connection to use.
    this.connectionProfile = getDbConnection();

    // Do we have a valid connection to use?
    if (this.connectionProfile != null)
    {
      // Error-throwing mode
      if (safe)
      {
        // ---------------------
        // Create the statement.
        try
        {
          // Get the statement.
          generateStatement(this.connectionProfile);
        }
        catch (SQLException sqlexc)
        {
          log("SQL Exception while creating statement: " + sqlexc, LogLevel.ALERT);

          // Close connection; will try reopening on next request.
          this.connectionProfile.close();

          throw new JdbcConnectorError("Creation of statement failed.", sqlexc);
        }
        logWarnings(this.statement);

        // ----------------------
        // Create the result set.
        try
        {
          // Get the result set.
          generateResults();
        }
        catch (SQLException sqlexc)
        {
          try
          {
            this.statement.close();
          }
          catch(Exception e) {}
          this.statement = null;
          
          log("SQL Exception while running query: " + sqlexc, LogLevel.ALERT);
          log("Query was: " + getQuery(), LogLevel.ALERT);
          throw new JdbcConnectorError("runQuery failed.", sqlexc);
        }
      }

      // Exception-swallowing mode
      else
      {
        boolean exceptionEncountered = false;

        // Repeat until we're not instructed to try again.
        int instruction = DatabaseConnectionListener.INSTRUCT_RETRY;
        while (instruction == DatabaseConnectionListener.INSTRUCT_RETRY)
        {
          exceptionEncountered = false;
          instruction = DatabaseConnectionListener.INSTRUCT_DO_NOTHING;

          // Increment the try count.
          this.tryCount++;

          try
          {
            // Get the statement.
            generateStatement(this.connectionProfile);
          }
          catch (SQLException sqlexc)
          {
            try
            {
              this.statement.close();
            }
            catch(Exception e) {}
            this.statement = null;
            
            log("SQL Exception while creating statement: " + sqlexc, LogLevel.ALERT);

            // Close connection; will try reopening on next request.
            this.connectionProfile.close();

            exceptionEncountered = true;

            instruction = notifyListener(JdbcConnectorConstants.METHOD_RUN_QUERY, sqlexc);
          }

          if (!exceptionEncountered)
          {
            try
            {
              // Get the result set.
              generateResults();
            }
            catch (SQLException sqlexc)
            {
              try
              {
                this.statement.close();
              }
              catch(Exception e) {}
              this.statement = null;
              
              // Did we get disconnected?
              checkForDisconnectExceptions(sqlexc, this.connectionProfile);

              log("SQL Exception while running query: " + sqlexc, LogLevel.ALERT);
              log("Query was: " + getQuery(), LogLevel.ALERT);

              instruction = notifyListener(JdbcConnectorConstants.METHOD_RUN_QUERY, sqlexc);
            }
          }
        }
        
        // If there was an exception and the retry didn't help, return
        // an indication of that.
        if (exceptionEncountered)
        {            
          return RUNQUERY_EXCEPTION;
        }
      }  // End Exception-swallowing mode.
    }
    else
    {
      log("No valid connection available, aborting query.", LogLevel.CRITICAL);
      return RUNQUERY_EXCEPTION;
    }

    //log("Returning from runQuery.", LogLevel.MINIMUM);
    return 0;
  }

  /**
   * Checks for nasty exceptions by name and closes the connection (to be
   * re-opened on the next query) if these particular exceptions occur.
   */
  protected void checkForDisconnectExceptions(SQLException sqlexc,
    JdbcConnectionProfile profile)
  {
    // If the SQL state starts with "08", according to the SQL specs, this
    // indicates a connection issue.
    if (sqlexc.getSQLState().startsWith("08"))
    {
      log("SQL state: " + sqlexc.getSQLState() + "; closing connection.", LogLevel.DEBUG);
      profile.close();
    }
    else
    {
      // In case the SQL State alone doesn't capture what's going on,
      // let's take a look at the text of the exception.
      String sqlexcString = sqlexc.toString();
      if ( (sqlexcString.contains("connection reset by peer"))
        || (sqlexcString.contains("socket write error"))
        )
      {
        // Close connection; will try reopening on next request.
        log("Exception text suggests connection error; closing connection.", LogLevel.DEBUG);
        profile.close();
      }
    }
  }

  /**
   * Executes a batch of SQL commands using a single Statement and then closes
   * this JdbcConnector.  Generally, a series of SQL commands will execute 
   * faster if executed as a batch.  Safe mode will throw SQLExceptions as 
   * encapsulated Errors (be certain to catch JdbcConnectorError).
   *   <p>
   * Note that the JdbcConnector is automatically closed after the batch is
   * executed and it is unnecessary to call close in client code.
   *
   * @param sqlCommands The collection of Strings represented SQL commands.
   * @param safe Run the query in safe mode (exception throwing)?
   * @return The affected row counts for each command in the batch; can
   *         be null.
   */
  public int[] executeBatch(Collection<String> sqlCommands, boolean safe)
  {
    int[] affectedRowCounts = null;

    // Check for empty batch.
    if (sqlCommands == null || sqlCommands.isEmpty())
    {
      log("executeBatch: No batch commands defined.", LogLevel.ALERT);
    }

    // We've got commands; proceed.
    else
    {
      // Clear out any previous query's ResultSet.
      close(false);

      // Determine which connection to use, static or instance.
      this.connectionProfile = getDbConnection();

      // Do we have a valid connection to use?
      if (this.connectionProfile != null)
      {
        // Use a finally to ensure that the load is decremented.
        try
        {
          // Repeat until we're not instructed to try again.
          int instruction = DatabaseConnectionListener.INSTRUCT_RETRY;
          while (instruction == DatabaseConnectionListener.INSTRUCT_RETRY)
          {
            instruction = DatabaseConnectionListener.INSTRUCT_DO_NOTHING;

            // Increment the try count.
            this.tryCount++;

            // Error-throwing mode
            if (safe)
            {
              try
              {
                // Get the statement.
                generateStatement(this.connectionProfile);
              }
              catch (SQLException sqlexc)
              {
                log("SQL Exception while creating statement: " + sqlexc, LogLevel.ALERT);

                // Close connection; will try reopening on next request.
                this.connectionProfile.close();

                throw new JdbcConnectorError("Creation of statement failed.", sqlexc);
              }

              try
              {
                // Add each SQL command to the statement.
                for (String sqlCommand : sqlCommands)
                {
                  this.statement.addBatch(sqlCommand);
                }

                // Execute the batch.
                affectedRowCounts = this.statement.executeBatch();

                // Clear the batch.
                this.statement.clearBatch();
              }
              catch (SQLException sqlexc)
              {
                try
                {
                  this.statement.close();
                }
                catch(Exception e) {}
                this.statement = null;
                
                log("SQL Exception while running query: " + sqlexc, LogLevel.ALERT);
                log("Query was: " + getQuery(), LogLevel.ALERT);
                
                throw new JdbcConnectorError("executeBatch failed.", sqlexc);
              }
            }

            // Exception-swallowing mode
            else
            {
              boolean exceptionEncountered = false;

              try
              {
                // Get the statement.
                generateStatement(this.connectionProfile);
              }
              catch (SQLException sqlexc)
              {
                // Close connection; will try reopening on next request.
                this.connectionProfile.close();

                log("SQL Exception while creating statement: " + sqlexc, LogLevel.ALERT);
                
                instruction = notifyListener(JdbcConnectorConstants.METHOD_EXECUTE_BATCH, sqlexc);

                exceptionEncountered = true;
              }

              if (!exceptionEncountered)
              {
                try
                {
                  // Add each SQL command to the statement.
                  for (String batch : sqlCommands)
                  {
                    this.statement.addBatch(batch);
                  }

                  // Execute the batch.
                  affectedRowCounts = this.statement.executeBatch();

                  // Clear the batch.
                  this.statement.clearBatch();
                }
                catch (SQLException sqlexc)
                {
                  try
                  {
                    this.statement.close();
                  }
                  catch(Exception e) {}
                  this.statement = null;
                  
                  log("SQL Exception while running query: " + sqlexc, LogLevel.ALERT);
                  log("Query was: " + getQuery(), LogLevel.ALERT);
                  checkForDisconnectExceptions(sqlexc, this.connectionProfile);

                  instruction = notifyListener(JdbcConnectorConstants.METHOD_EXECUTE_BATCH, sqlexc);
                }
              }
            }   // End Exception-swallowing mode.
          }
        }   // End try.

        // Ensure that the load is decremented.
        finally
        {
          // Close will release the thread's hold on a Profile.
          close(false);
        }
      }
    }
    
    return affectedRowCounts;
  }

  /**
   * Gets the SQLWarning linked list from the Statement.  Returns null if
   * no Statement or no warnings.
   */
  public SQLWarning getWarnings()
  {
    if (this.statement != null)
    {
      try
      {
        return this.statement.getWarnings();
      }
      catch (SQLException sqlexc)
      {
        // Do nothing.
      }
    }

    return null;
  }

  /**
   * Gets the SQLWarning linked list from the Connection.  Returns null if
   * no Statement or no warnings.
   */
  public SQLWarning getConnectionWarnings()
  {
    if (this.statement != null)
    {
      try
      {
        return this.statement.getConnection().getWarnings();
      }
      catch (SQLException sqlexc)
      {
        // Do nothing.
      }
    }

    return null;
  }

  /**
   * Standard toString method.
   */
  @Override
  public String toString()
  {
    return "JdbcConn [q: " + this.query + "]";
  }

  /**
   * Executes a batch of SQL commands using a single Statement.  Generally,
   * a series of SQL commands will execute faster if executed as a batch.
   * This method uses the Safe Mode flag as specified by setSafeMode(boolean),
   * which is FALSE by default.
   *    <p>
   * See executeBatch(sqlCommands, safeMode) for more information.
   *
   * @param sqlCommands The collection of Strings represented SQL commands.
   * @return The affected row counts for each command in the batch; can
   *         be null.
   */
  @Override
  public int[] executeBatch(Collection<String> sqlCommands)
  {
    return executeBatch(sqlCommands, this.safeMode);
  }

  /**
   * Generates a statement and runs the query.
   */
  protected void generateStatement(JdbcConnectionProfile dbConnectionToUse)
    throws SQLException
  {
    try
    {
      //log("Creating the statement.", LogLevel.MINIMUM);
      logWarnings(dbConnectionToUse.getConnection());

      // Default: read only, scrollable.
      int resultSetType        = ResultSet.TYPE_SCROLL_INSENSITIVE;
      int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;

      // Set query modes, as appropriate.
      if (this.forwardOnly)
      {
        resultSetType = ResultSet.TYPE_FORWARD_ONLY;
      }
      if (!this.readOnly)
      {
        resultSetConcurrency = ResultSet.CONCUR_UPDATABLE;
      }

      // Get the statement.
      this.statement = dbConnectionToUse.getConnection().createStatement(resultSetType,
        resultSetConcurrency);

      logWarnings(this.statement);
    }
    catch (SQLException sqlexc)
    {
      try
      {
        if (this.statement != null)
        {
          this.statement.close();
        }
      }
      catch(SQLException exc) {}
      this.statement = null;
      
      log("SQLException in generateStatement: " + sqlexc, LogLevel.ALERT);

      this.nextThrewException = true;
      this.resultSet          = null;

      throw sqlexc;
    }
  }

  /**
   * Executes the query and generates the result set.
   */
  protected void generateResults()
    throws SQLException
  {
    try
    {
      this.resultSet = statement.executeQuery(this.query);

      next();
      logWarnings(this.resultSet);
    }
    catch (SQLException sqlexc)
    {
      this.nextThrewException = true;
      this.resultSet          = null;

      throw sqlexc;
    }
  }

  /**
   * Gets a connection to the database, establishing a connection if
   * necessary.  If a pooled connection exists, that will be the preferred
   * result of this method unless the forceNewConnection flag is set to
   * true.  Returns null in the event of a connection error.
   *    <p>
   * Note that if the forceNewConnection flag is set, a new connection to
   * the database will always be created and returned by this method.
   */
  protected JdbcConnectionProfile getDbConnection()
  {
    JdbcConnectionProfile theConnectionProfile;

    // If we're in force-new-connection mode, then we're not going to worry
    // about the connection profiles or the connection manager.  This is
    // because we're always going to return a newly established connection.
    if (this.forceNewConnection)
    {
      // If a forced connection has not already been made then make one.
      if (this.forcedConnectionProfile == null)
      {
        theConnectionProfile = this.manager.getDetachedProfile();

        // If the connection is null then null should be returned for the connection profile.
        if (!theConnectionProfile.isConnectionAvailable())
        {
          theConnectionProfile = null;
        }
        // If a Connection was established, it will be non-null.
        else
        {
          // Record this forced connection for next time.
          this.forcedConnectionProfile = theConnectionProfile;
          this.forcedConnection        = theConnectionProfile.getConnection();
        }
      }
      // We have an active connection so let's reuse it.
      else
      {
        theConnectionProfile = this.forcedConnectionProfile;
      }
    }

    // The default scenario is to reuse connections.  However, for systems
    // using temporary tables, reused connections will lead to concurrency
    // issues when working with those temporary tables.
    else
    {
      theConnectionProfile = this.manager.getProfile();
    }

    // Returns null if there's a problem.
    return theConnectionProfile;
  }

  /**
   * Notifies listeners of exception during a query.
   */
  protected int notifyListener(int methodType, SQLException exc)
  {
    final DatabaseConnectionListener listener = manager.getListener();
    if (listener != null)
    {
      if (methodType == JdbcConnectorConstants.METHOD_RUN_QUERY)
      {
        return listener.exceptionInRunQuery(exc, this);
      }
      if (methodType == JdbcConnectorConstants.METHOD_EXECUTE_BATCH)
      {
        return listener.exceptionInExecuteBatch(exc, this);
      }
      if (methodType == JdbcConnectorConstants.METHOD_RUN_UPDATE_QUERY)
      {
        return listener.exceptionInRunUpdateQuery(exc, this);
      }
    }

    return DatabaseConnectionListener.INSTRUCT_DO_NOTHING;
  }

  @Override
  public Collection<DatabaseTableMetaData> getTableMetaData()
  {
    return getTableMetaData(safeMode);
  }

  public Collection<DatabaseTableMetaData> getTableMetaData(boolean safe)
  {
    // Close the query if necessary.
    close(false);

    // Remove the row count cache.
    this.rowCount = -1;

    // Determine which connection to use.
    this.connectionProfile = getDbConnection();

    // Do we have a valid connection to use?
    if (this.connectionProfile != null)
    {
      // We use a try-finally to ensure that the load is always decremented.
      try
      {
        // Repeat until we're not instructed to try again.
        int instruction = DatabaseConnectionListener.INSTRUCT_RETRY;
        while (instruction == DatabaseConnectionListener.INSTRUCT_RETRY)
        {
          instruction = DatabaseConnectionListener.INSTRUCT_DO_NOTHING;

          // Increment the try count if not in safe mode.
          if (!safe)
          {
            this.tryCount++;
          }

          try
          {
            DatabaseMetaData metaData = this.connectionProfile.getConnectionMetaData();

            if (metaData == null)
            {
              return null;
            }

            Collection<DatabaseTableMetaData> tables = new ArrayList<>();

            this.resultSet = metaData.getTables(null, null, "%", new String [] {"TABLE"});
            this.next();

            for (; this.more(); this.next())
            {
              DatabaseTableMetaData tableInfo = new DatabaseTableMetaData(this.getField("TABLE_NAME", ""));
              tableInfo.setCatalogName(this.getField("TABLE_CAT", ""));
              tableInfo.setSchemaName(this.getField("TABLE_SCHEM", ""));
              tableInfo.setTableType(this.getField("TABLE_TYPE", "TABLE"));

              tables.add(tableInfo);
            }

            return tables;
          }
          catch (SQLException e)
          {
            log("SQL Exception while getting meta data for tables: " + e, LogLevel.ALERT);

            // Close connection; will try reopening on next request.
            this.connectionProfile.close();

            // swallow the exception if not in safe mode
            if (safe)
            {
              throw new JdbcConnectorError("Failed to get meta data for tables.", e);
            }
            else
            {
              instruction = notifyListener(JdbcConnectorConstants.METHOD_RUN_QUERY, e);
            }
          }
        }
      }  // End try.
      finally
      {
        // Close releases the current thread's hold of the Profile.
        this.close(false);
      }
    }
    else
    {
      log("No valid connection available, aborting query.", LogLevel.CRITICAL);
    }

    log("Returning from getTableMetaData.", LogLevel.MINIMUM);

    return null;
  }

  @Override
  public DatabaseTableMetaData getTableMetaData(String tableName)
  {
    return this.getTableMetaData(tableName, this.safeMode);
  }

  /**
   * Gets metadata for a table.
   */
  public DatabaseTableMetaData getTableMetaData(String tableName, boolean safe)
  {
    // Close the query if necessary.
    close(false);

    // Remove the row count cache.
    this.rowCount = -1;

    // Determine which connection to use.
    this.connectionProfile = getDbConnection();

    // Do we have a valid connection to use?
    if (this.connectionProfile != null)
    {
      // We use a try-finally to ensure that the load is always decremented.
      try
      {
        // Repeat until we're not instructed to try again.
        int instruction = DatabaseConnectionListener.INSTRUCT_RETRY;
        while (instruction == DatabaseConnectionListener.INSTRUCT_RETRY)
        {
          instruction = DatabaseConnectionListener.INSTRUCT_DO_NOTHING;

          // Increment the try count if not in safe mode.
          if (!safe)
          {
            this.tryCount++;
          }

          try
          {
            DatabaseMetaData metaData = this.connectionProfile.getConnectionMetaData();

            if (metaData == null)
            {
              return null;
            }

            this.resultSet = metaData.getTables(null, null, tableName, new String [] {"TABLE"});

            if (this.more())
            {
              DatabaseTableMetaData tableInfo = new DatabaseTableMetaData(this.getField("TABLE_NAME", ""));
              tableInfo.setCatalogName(this.getField("TABLE_CAT", ""));
              tableInfo.setSchemaName(this.getField("TABLE_SCHEM", ""));
              tableInfo.setTableType(this.getField("TABLE_TYPE", "TABLE"));

              return tableInfo;
            }
          }
          catch (SQLException e)
          {
            log("SQL Exception while getting meta data for tables: " + e, LogLevel.ALERT);

            // Close connection; will try reopening on next request.
            this.connectionProfile.close();

            // swallow the exception if not in safe mode
            if (safe)
            {
              throw new JdbcConnectorError("Failed to get meta data for tables.", e);
            }
            else
            {
              instruction = notifyListener(JdbcConnectorConstants.METHOD_RUN_QUERY, e);
            }
          }
        }
      }  // End try.
      finally
      {
        // Close releases the current thread's hold on the Profile.
        this.close(false);
      }
    }
    else
    {
      log("No valid connection available, aborting query.", LogLevel.CRITICAL);
    }

    log("Returning from getTableMetaData.", LogLevel.MINIMUM);

    return null;
  }

  @Override
  public byte[] getBytes(String fieldName)
  {
    try
    {
      return this.resultSet.getBytes(fieldName);
    }
    catch (SQLException exc)
    {
      return new byte[0];
    }
  }
  
  /**
   * Returns the raw JDBC Connection object in use within this JdbcConnector.
   */
  @Override
  public Connection getConnection() throws SQLException 
  {
    // Close the query if necessary.
    close(false);

    this.connectionProfile = getDbConnection();
    
    return this.connectionProfile.getConnection();
  }

  @Override
  public ResultSet getResultSet()
  {
    return this.resultSet;
  }

  /**
   * <p>
   * Reports whether the last column read had a value of SQL NULL. Note that you 
   * must first call one of the getter methods on a column to try to read its 
   * value and then call the method wasNull to see if the value read was SQL NULL.
   * 
   * <p>
   * If an exception is thrown, this method logs the exception and then returns
   * false.
   * 
   * @see java.sql.ResultSet#wasNull()
   */
  @Override
  public boolean wasNull()
  {
    try
    {
      return this.resultSet.wasNull();
    }
    catch (SQLException e)
    {
      log("Exception while calling wasNull() on the result set: " + e,
          LogLevel.NORMAL);
    }
    return false;
  }

  /**
   * From the result set, returns a field's contents as an object.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  @Override
  public Object getObject(String fieldName)
  {
    try
    {
      return this.resultSet.getObject(fieldName);
    }
    catch (SQLException exc)
    {
      return null;
    }
  }

  @Override
  public void reconnect()
  {
    if (this.connectionProfile != null)
    {
      this.connectionProfile.close();
    }
  }

  @Override
  public PreparedStatement prepareStatement(final String sql)
      throws SQLException
  {
    return JdbcConnector.this.getConnection().prepareStatement(sql);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql,
      final int autoGeneratedKeys) throws SQLException
  {
    return JdbcConnector.this.getConnection().prepareStatement(sql,
        autoGeneratedKeys);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql,
      final int[] columnIndexes) throws SQLException
  {
    return JdbcConnector.this.getConnection().prepareStatement(sql,
        columnIndexes);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql,
      final int resultSetType, final int resultSetConcurrency)
          throws SQLException
  {
    return JdbcConnector.this.getConnection().prepareStatement(sql,
        resultSetType, resultSetConcurrency);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql,
      final int resultSetType, final int resultSetConcurrency,
      final int resultSetHoldability) throws SQLException
  {
    return JdbcConnector.this.getConnection().prepareStatement(sql,
        resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql,
      final String[] columnNames) throws SQLException
  {
    return JdbcConnector.this.getConnection().prepareStatement(sql,
        columnNames);
  }

  /**
   * If there are any warnings on this Connection, log them. Safe to pass a
   * null Connection.
   */
  private void logWarnings(Connection c)
  {
    if (!this.manager.getAttributes().getLogWarnings())
    {
      return;
    }
    try
    {
      if (c != null)
      {
        logWarnings(c.getWarnings());
      }
    }
    catch (SQLException exc)
    {
      log("Exception while logging warnings on Connection " + c,
          LogLevel.ALERT, exc);
    }
  }

  /**
   * If there are any warnings on this Statement, log them. Attempt to log any
   * warnings on this Statement's connection as well. Safe to pass a null
   * Statement.
   */
  private void logWarnings(Statement s)
  {
    if (!this.manager.getAttributes().getLogWarnings())
    {
      return;
    }
    try
    {
      if (s != null)
      {
        logWarnings(s.getWarnings());
        logWarnings(s.getConnection());
      }
    }
    catch (SQLException exc)
    {
      log("Exception while logging warnings on Statement " + s,
          LogLevel.ALERT, exc);
    }
  }

  /**
   * If there are any warnings on this ResultSet, log them. Attempt to log any
   * warnings on this ResultSet's Statement as well. Safe to pass a null
   * ResultSet.
   */
  private void logWarnings(ResultSet rs)
  {
    if (!this.manager.getAttributes().getLogWarnings())
    {
      return;
    }
    try
    {
      if (rs != null)
      {
        logWarnings(rs.getWarnings());
        logWarnings(rs.getStatement());
      }
    }
    catch (SQLException exc)
    {
      log("Exception while logging warnings on ResultSet " + rs,
          LogLevel.ALERT, exc);
    }
  }

  /**
   * Given a SQLWarning, log it and any other chained warnings attached to it.
   */
  private void logWarnings(SQLWarning warning)
  {
    if (!this.manager.getAttributes().getLogWarnings())
    {
      return;
    }
    SQLWarning w = warning;
    while (w != null)
    {
      log("Warning: Error " + w.getErrorCode() + ", SQL state "
          + w.getSQLState() + ", message " + w.getMessage(),
          this.manager.getAttributes().getWarningLogLevel(), w.getCause());
      w = w.getNextWarning();
    }
  }
}    // End JdbcConnector.
