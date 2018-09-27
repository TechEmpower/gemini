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

package com.techempower.data;

import java.math.*;
import java.sql.*;
import java.util.*;

import com.techempower.data.mapping.*;

/**
 * An interface for providing a custom database connector for client
 * applications.  Existing implementations use JDBC, ADO, and RDO.
 *
 * Example use:
 *    <p>
 * <pre><br>
 *   DatabaseConnector connector = Helper.getConnector(connectString,
 *     "ehspGetMessagesReceived 1;");
 *   connector.setUsername("username");
 *   connector.setPassword("password");
 *   connector.runQuery();
 *   while (connector.more())
 *   {
 *     String messageText = connector.getField("MessageText");
 *     connector.next();
 *   }
 *   connector.close();
 * </pre>
 *    <p>
 * Calling close() at the end of every query is strongly encouraged and
 * implementations may <b>require</b> it.
 */
public interface DatabaseConnector
  extends AutoCloseable
{
  // Constants.
  int                   RUNQUERY_SUCCESS        = 0;
  int                   RUNQUERY_EXCEPTION      = -1;

  /**
   * Sets the read-only flag.  The read-only flag should be set to true for 
   * most applications.  Note that the implementation of DatabaseConnector 
   * does not need to implement updatable (non-read-only) result sets.
   *   <p>
   * By default, the read-only flag is true.
   *
   * @param readOnly the new state of the flag.
   */
  void setReadOnly(boolean readOnly);

  /**
   * Run an UPDATE or INSERT query and then automatically close the Connector.
   *    <p>
   * The query set by setQuery or the constructor is not actually executed
   * until a call to runUpdateQuery is made.
   */
  int runUpdateQuery();

  /**
   * Runs the query as an update. Necessary to get back the rows updated.
   *    <p>
   * The query set by setQuery or the constructor is not actually executed
   * until a call to runUpdateQuery is made.
   * 
   * @param safeMode Set to true to receive any exceptions thrown while
   * running the query.
   */
  int runUpdateQuery(boolean safeMode);

  /**
   * Gets the read-only flag.
   */
  boolean getReadOnly();

  /**
   * Sets the forward-only flag.  This can be used to increase the 
   * performance of the query and navigation through the result set.
   * However, use of moveAbsolute and first are not permitted in this
   * mode.  Note that the implementation of DatabaseConnector does not
   * need to implement forward-only resultsets.
   *
   * @param forwardOnly the new state of the flag.
   */
  void setForwardOnly(boolean forwardOnly);

  /**
   * Sets the force new connection flag.  When enabled, all queries will
   * be forced to run on new database Connections.  This is not ideal because
   * establishing connections is time consuming under heavy load.
   *  <p>
   * Forcing new connections is generally required in multi-threaded systems
   * that use temporary tables.  Temporary tables are usually scoped for
   * individual connections.
   *  <p>
   * The default is false (off).
   *  <p>
   * Not all implementations of DatabaseConnector will do anything with this
   * method.
   */
  void setForceNewConnection(boolean forceNewConnection);

  /**
   * Gets the forward-only flag.
   */
  boolean getForwardOnly();

  /**
   * Moves to an absolute position in the result set.
   *
   * @param position the absolute row number
   */
  void moveAbsolute(int position);

  /**
   * Gets the row count after a query has been executed.  Acceptable to
   * return 0 if a query has not been executed or -1 if the implementation
   * does not successfully provide this functionality.
   *
   * Also acceptable to move the cursor to the first element after this
   * operation.
   */
  int getRowCount();

  /**
   * Gets the row number in the result set.  Acceptable to return 0 in the
   * event of an error.
   */
  int getRowNumber();

  /**
   * Gets a String array representing the field names in the result set.
   */
  String[] getFieldNames();

  /**
   * Gets a int array representing the field types in the result set.
   */
  int[] getFieldTypes();

  /**
   * From the results set, returns a field's contents by the field's name.
   * Will return null if the field's value is null.
   *
   * @param fieldName the name of the field
   */
  String getFieldByName(String fieldName);

  /**
   * A variant of getFieldByName that will return a default value if null is
   * gathered from the database.
   *
   * @param fieldName the name of the field
   * @param defaultValue the default value to return in the event of a null
   */
  String getField(String fieldName, String defaultValue);

  /**
   * From the result set, returns a field's contents as a java.sql.Clob value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  String getClob(String fieldName);
  
  /**
   * From the result set, returns a field's contents as a Date value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  java.sql.Date getDate(String fieldName);
  
  /**
   * From the result set, returns a field's contents as a Calendar object.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  java.util.Calendar getDateAsCalendar(String fieldName);
  
  /**
   * From the result set, returns a field's contents as a Time value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  java.sql.Time getTime(String fieldName);

  /**
   * From the result set, returns a field's contents as a Timestamp value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  java.sql.Timestamp getTimestamp(String fieldName);

  /**
   * From the result set, returns a field's contents as a boolean value.
   * Returns false in the event of an error.
   *
   * @param fieldName the name of the field
   */
  boolean getBoolean(String fieldName);

  /**
   * From the result set, returns a field's contents as a BigDecimal value.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  BigDecimal getBigDecimal(String fieldName);

  /**
   * From the result set, returns a field's contents as a float value.
   * Returns 0.0F in the event of an error.
   *
   * @param fieldName the name of the field
   */
  float getFloat(String fieldName);

  /**
   * From the result set, returns a field's contents as a byte array value.
   * Returns an empty array in the event of an error.
   *
   * @param fieldName the name of the field
   */
  byte[] getBytes(String fieldName);

  /**
   * From the result set, returns a field's contents as a double value.
   * Returns 0.0D in the event of an error.
   *
   * @param fieldName the name of the field
   */
  double getDouble(String fieldName);

  /**
   * From the result set, returns a field's contents as a byte value.
   * Returns 0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  byte getByte(String fieldName);

  /**
   * From the result set, returns a field's contents as a short value.
   * Returns 0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  short getShort(String fieldName);

  /**
   * From the result set, returns a field's contents as a long value.
   * Returns 0L in the event of an error.
   *
   * @param fieldName the name of the field
   */
  long getLong(String fieldName);

  /**
   * A variant on getIntegerFieldByName that is just faster to type.
   * Returns 0 in the event of an error.
   *
   * @param fieldName the name of the field
   */
  int getInt(String fieldName);

  /**
   * Moves to first element in result set.  May not be supported by
   * certain connectors.
   */
  void first();

  /**
   * Moves to last element in result set.  May not be supported by
   * certain connectors.
   */
  void last();

  /**
   * Moves to next element in result set.
   */
  void next();

  /**
   * Is there more results in the result set?
   *
   * @return true if there are more rows in the result set.
   */
  boolean more();

  /**
   * Closes the result set.  Use a try-with-resources for Database Connectors
   * to ensure that the close method is called when your usage is complete.
   */
  @Override
  void close();
  
  /**
   * Set the SQL query for this DatabaseConnector object.  It is
   * acceptable for this method to close any current result sets.
   *
   * @param query the SQL query--not immediately executed.
   */
  void setQuery(String query);

  /**
   * Get the SQL query for this DatabaseConnector object.
   */
  String getQuery();

  /**
   * Execute the query.  Generate a result set for SELECT queries.
   *    <p>
   * The query set by setQuery or the constructor is not actually executed
   * until a call to runQuery is made.
   * 
   * @return An integer whose meaning is defined by the implementation;
   * typically '0' indicates success.
   */
  int runQuery();

  /**
   * Execute the query with optional "safeMode" parameter (if true, exceptions
   * will be not be captured by runQuery; they will propagate to the calling
   * method).
   *    <p>
   * The query set by setQuery or the constructor is not actually executed
   * until a call to runQuery is made.
   * 
   * @param safeMode Set to true to receive any exceptions thrown while
   * running the query.
   * 
   * @return An integer whose meaning is defined by the implementation;
   * typically '0' indicates success.
   */
  int runQuery(boolean safeMode);

  /**
   * Executes a batch of SQL commands using a single Statement and close the
   * Connector automatically.  Generally, a series of SQL commands will 
   * execute faster if executed as a batch.
   * 
   * @param sqlCommands The collection of Strings represented SQL commands.
   * @return The affected row counts for each command in the batch; can 
   *         be null.
   */
  int[] executeBatch(Collection<String> sqlCommands);
  
  /**
   * Gets the meta data for all tables in the database if available
   * 
   * @return A Collection containing a DatabaseTableMetaData object for each table
   *         or null if no meta data could be found
   */
  Collection<DatabaseTableMetaData> getTableMetaData();
  
  /**
   * Gets the meta data for a single table in the database if available
   * 
   * @return A DatabaseTableMetaData object for the given table
   *         or null if no meta data could be found
   */
  DatabaseTableMetaData getTableMetaData(String tableName);

  /**
   * Reports whether the last column read had a value of SQL NULL. Note that you 
   * must first call one of the getter methods on a column to try to read its 
   * value and then call the method wasNull to see if the value read was SQL NULL.
   * 
   * @see java.sql.ResultSet#wasNull()
   */
  boolean wasNull();

  /**
   * Gets a connection object.  This is useful for building prepared statements.
   */
  Connection getConnection() throws SQLException;
  
  /**
   * Gets a reference to the Java ResultSet object, assuming one has been
   * created (a query has been executed).  Otherwise, returning null is okay.
   */
  ResultSet getResultSet();

  /**
   * From the result set, returns a field's contents as an object.
   * Returns null in the event of an error.
   *
   * @param fieldName the name of the field
   */
  Object getObject(String fieldName);

  /**
   * Resets the connection to the database.  This is useful for dealing with
   * SQL state 08 errors, which are thrown when the connection is lost.
   */
  void reconnect();

  /**
   * Creates a new prepared statement.
   *
   * @see Connection#prepareStatement(String)
   */
  PreparedStatement prepareStatement(String sql) throws SQLException;

  /**
   * Creates a new prepared statement.
   *
   * @see Connection#prepareStatement(String, int)
   */
  PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
      throws SQLException;

  /**
   * Creates a new prepared statement.
   *
   * @see Connection#prepareStatement(String, int[])
   */
  PreparedStatement prepareStatement(String sql, int[] columnIndexes)
      throws SQLException;

  /**
   * Creates a new prepared statement.
   *
   * @see Connection#prepareStatement(String, int, int)
   */
  PreparedStatement prepareStatement(String sql, int resultSetType,
      int resultSetConcurrency) throws SQLException;

  /**
   * Creates a new prepared statement.
   *
   * @see Connection#prepareStatement(String, int, int, int)
   */
  PreparedStatement prepareStatement(String sql, int resultSetType,
      int resultSetConcurrency, int resultSetHoldability) throws SQLException;

  /**
   * Creates a new prepared statement.
   *
   * @see Connection#prepareStatement(String, String[])
   */
  PreparedStatement prepareStatement(String sql, String[] columnNames)
      throws SQLException;
  
}    // End DatabaseConnector
