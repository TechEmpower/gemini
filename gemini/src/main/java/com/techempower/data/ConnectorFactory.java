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

import java.sql.*;

import com.techempower.util.*;

/**
 * The ConnectorFactory interface specifies a basic means by which a database
 * connector generator (factory) can provide DatabaseConnector instances to
 * applications.  These factory objects are expected to retain the necessary
 * connection information to establish connectivity such as the database name,
 * login name, and password.
 */
public interface ConnectorFactory
  extends Configurable
{

  /**
   * Gets a ConnectionMonitor, which is a simple abstraction over a raw
   * database Connection.  The ConnectionMonitor should be used within a
   * Java 7 try-with-resources block so that the automatic call to
   * ConnectionMonitor.close will result in the Connection being returned
   * to a connection pool (and not in fact closed).
   */
  ConnectionMonitor getConnectionMonitor() throws SQLException;
  
  /**
   * Gets a DatabaseConnector object for a query.  The default getConnector
   * method assumes a read-only, forward-only result set is necessary.
   *
   * @param query The database query (typically, as SQL).
   */
  DatabaseConnector getConnector(String query);

  /**
   * Gets a DatabaseConnector object without initializing its query.  It
   * is assumed that the DatabaseConnector's setQuery method will be 
   * invoked later.
   */
  DatabaseConnector getConnector();

  /**
   * Gets a DatabaseConnector object for a query.  This method returns a
   * connector that uses a scrolling resultset, allowing the use of the
   * first and getRowCount methods.
   *
   * @param query The database query (typically, as SQL).
   */
  DatabaseConnector getScrollingConnector(String query);

  /**
   * Gets a DatabaseConnector object.  This method returns a connector 
   * that uses a scrolling resultset, allowing the use of the first 
   * and getRowCount methods.  The connector returned by this method
   * will not have a query set.
   */
  DatabaseConnector getScrollingConnector();

  /**
   * Sets the DatabaseConnectionListener to use for all DatabaseConnector
   * objects created by this Factory.  Attaching a connection listener
   * to the connector factory should be considered optional.  Setting the
   * value to null is permitted, and should effectively disable listening.
   */
  void setDatabaseConnectionListener(
    DatabaseConnectionListener dbConnListener);

  /**
   * Attempt to communicate with the database to determine the database's 
   * identifier quote string.
   */
  void determineIdentifierQuoteString();

  /**
   * Retrieves the string used to quote SQL identifiers. This method returns a
   * space " " if identifier quoting is not supported.
   */
  String getIdentifierQuoteString();

  /**
   * Gets the affinity of the underlying SQL database.  This might be useful in
   * situations where vendor-specific functionality is required, and there is no
   * choice but to write conditional code based on the vendor.
   */
  DatabaseAffinity getDatabaseAffinity();

  /**
   * Is this Connector Factory enabled?  In nearly all cases, this would be
   * true; but it will be false for applications that do not connect to a
   * database at all.
   */
  public boolean isEnabled();

}   // End ConnectorFactory.
