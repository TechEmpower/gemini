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
import java.sql.Date;
import java.util.*;

import com.techempower.data.mapping.*;

/**
 * Provides simple result-set "paging" for interfaces, such as web sites,
 * where the results of a query should be displayed in small numbers per
 * page.
 *
 */
public class DatabaseResultsPager
  implements DatabaseConnector
{

  //
  // Constants
  //
  
  public static final int DEFAULT_PAGE_SIZE = 20;
  
  //
  // Member variables.
  //
  
  private final DatabaseConnector dbConn;
  private final int               rows;
  
  private int               pageSize = DEFAULT_PAGE_SIZE;
  private int               currentPage = 1;
  private boolean           moreBehavior = true;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public DatabaseResultsPager(DatabaseConnector dbConn)
  {
    this.dbConn = dbConn;

    dbConn.last();
    this.rows = dbConn.getRowNumber();
    dbConn.first();
    //this.initialized = true;
  }

  /**
   * Sets the page size.
   */
  public void setPageSize(int pageSize)
  {
    this.pageSize = pageSize;
  }
  
  /**
   * Gets the page size
   */
  public int getPageSize()
  {
    return this.pageSize;
  }
  
  /**
   * Gets the number of pages.
   */
  public int getPageCount()
  {
    // If the rows divides evenly, return that.
    if (this.rows % this.pageSize == 0)
    {
      return (this.rows / this.pageSize);
    }
    // Otherwise, return the division plus 1.
    else
    {
      return (this.rows / this.pageSize + 1);
    }
  }

  /**
   * Moves to and sets the current page number.  Page #1 is the first page.
   * 
   * @return true if successful.
   */
  public boolean setPage(int pageNumber)
  {
    if ( (pageNumber >= 1)
      && (pageNumber <= getPageCount())
      )
    {
      moveAbsolute(this.pageSize * (pageNumber - 1));
      this.currentPage = pageNumber;
      return true;
    }
    
    // No good.
    return false;
  }
  
  /**
   * Gets the current page number.  Page #1 is the first page.
   */
  public int getPage()
  {
    return this.currentPage;
  }
  
  /**
   * Determines which page a specified row would land in.
   */
  public int getPageForRow(int rowNumber)
  {
    return ((rowNumber - 1) / this.pageSize) + 1;
  }
  
  /**
   * Determines if the current row is the last row for the current page.
   */
  public boolean moreOnPage()
  {
    if (this.currentPage == getPageCount())
    {
      return (getRowNumber() < this.rows + 1);
    }
    else
    {
      return (getRowNumber() < getPage() * getPageSize() + 1);
    }
  }
  
  /**
   * Sets the behavior of the more() method.  If true, the more() method
   * will return false when the end of the current page is reached (basically,
   * the more() method becomes a call to moreOnPage behind the scenes).  If
   * false, the more() method will behave as normally, only returning false
   * when the end of the result set is reached.
   */
  public void setMorePagedBehavior(boolean pagedBehavior)
  {
    this.moreBehavior = pagedBehavior;
  }

  // -----------------------------------------------------------------------
  // DatabaseConnector methods.
  // -----------------------------------------------------------------------

  @Override
  public void close()
  {
    this.dbConn.close();
  }

  @Override
  public int[] executeBatch(Collection<String> sqlCommands)
  {
    return this.dbConn.executeBatch(sqlCommands);
  }

  @Override
  public void first()
  {
    this.dbConn.first();
  }

  @Override
  public boolean getBoolean(String fieldName)
  {
    return this.dbConn.getBoolean(fieldName);
  }

  @Override
  public BigDecimal getBigDecimal(String fieldName)
  {
    return this.dbConn.getBigDecimal(fieldName);
  }

  @Override
  public byte getByte(String fieldName)
  {
    return this.dbConn.getByte(fieldName);
  }

  @Override
  public String getClob(String fieldName)
  {
    return this.dbConn.getClob(fieldName);
  }

  @Override
  public Date getDate(String fieldName)
  {
    return this.dbConn.getDate(fieldName);
  }

  @Override
  public Calendar getDateAsCalendar(String fieldName)
  {
    return this.dbConn.getDateAsCalendar(fieldName);
  }

  @Override
  public double getDouble(String fieldName)
  {
    return this.dbConn.getDouble(fieldName);
  }

  @Override
  public String getField(String fieldName, String defaultValue)
  {
    return this.dbConn.getField(fieldName, defaultValue);
  }

  @Override
  public String getFieldByName(String fieldName)
  {
    return this.dbConn.getFieldByName(fieldName);
  }

  @Override
  public String[] getFieldNames()
  {
    return this.dbConn.getFieldNames();
  }

  @Override
  public int[] getFieldTypes()
  {
    return this.dbConn.getFieldTypes();
  }

  @Override
  public float getFloat(String fieldName)
  {
    return this.dbConn.getFloat(fieldName);
  }

  @Override
  public boolean getForwardOnly()
  {
    return this.dbConn.getForwardOnly();
  }

  @Override
  public int getInt(String fieldName)
  {
    return this.dbConn.getInt(fieldName);
  }

  @Override
  public long getLong(String fieldName)
  {
    return this.dbConn.getLong(fieldName);
  }

  @Override
  public String getQuery()
  {
    return this.dbConn.getQuery();
  }

  @Override
  public boolean getReadOnly()
  {
    return this.dbConn.getReadOnly();
  }

  @Override
  public int getRowCount()
  {
    return this.rows;
  }

  @Override
  public int getRowNumber()
  {
    return this.dbConn.getRowNumber();
  }

  @Override
  public short getShort(String fieldName)
  {
    return this.dbConn.getShort(fieldName);
  }

  @Override
  public Time getTime(String fieldName)
  {
    return this.dbConn.getTime(fieldName);
  }

  @Override
  public Timestamp getTimestamp(String fieldName)
  {
    return this.dbConn.getTimestamp(fieldName);
  }

  @Override
  public void last()
  {
    this.dbConn.last();
  }

  @Override
  public boolean more()
  {
    if (this.moreBehavior)
    {
      return (this.dbConn.more() && moreOnPage());
    }
    else
    {
      return this.dbConn.more();
    }
  }

  @Override
  public void moveAbsolute(int position)
  {
    this.dbConn.moveAbsolute(position);
  }

  @Override
  public void next()
  {
    this.dbConn.next();
  }

  @Override
  public int runQuery()
  {
    return this.dbConn.runQuery();
  }

  @Override
  public int runQuery(boolean safeMode)
  {
    return this.dbConn.runQuery(safeMode);
  }

  @Override
  public int runUpdateQuery()
  {
    return this.dbConn.runUpdateQuery();
  }
  
  @Override
  public int runUpdateQuery(boolean safeMode)
  {
    return this.dbConn.runUpdateQuery(safeMode);
  }

  @Override
  public void setForwardOnly(boolean forwardOnly)
  {
    this.dbConn.setForwardOnly(forwardOnly);
  }

  @Override
  public void setQuery(String query)
  {
    this.dbConn.setQuery(query);
  }

  @Override
  public void setReadOnly(boolean readOnly)
  {
    this.dbConn.setReadOnly(readOnly);
  }

  @Override
  public void setForceNewConnection(boolean newConnection)
  {
    this.dbConn.setForceNewConnection(newConnection);
  }

  @Override
  public Collection<DatabaseTableMetaData> getTableMetaData()
  {
    return this.dbConn.getTableMetaData();
  }
  
  @Override
  public DatabaseTableMetaData getTableMetaData(String tableName)
  {
    return this.dbConn.getTableMetaData(tableName);
  }
  
  @Override
  public Collection<DatabaseColumnMetaData> getColumnMetaDataForTable(String tableName)
  {
    return this.dbConn.getColumnMetaDataForTable(tableName);
  }
  
  @Override
  public Collection<DatabaseColumnMetaData> getColumnMetaDataFromResultSet()
  {
    return this.dbConn.getColumnMetaDataFromResultSet();
  }

  @Override
  public byte[] getBytes(String fieldName)
  {
    return this.dbConn.getBytes(fieldName);
  }

  @Override
  public boolean wasNull()
  {
    return this.dbConn.wasNull();
  }

  @Override
  public Connection getConnection() throws SQLException
  {
    return this.dbConn.getConnection();
  }
  
  @Override
  public ResultSet getResultSet()
  {
    return this.dbConn.getResultSet();
  }
  
  @Override
  public Object getObject(String fieldName)
  {
    return this.dbConn.getObject(fieldName);
  }

  @Override
  public void reconnect()
  {
    this.dbConn.reconnect();
  }

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException
  {
    return this.dbConn.prepareStatement(sql);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
      throws SQLException
  {
    return this.dbConn.prepareStatement(sql, autoGeneratedKeys);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
      throws SQLException
  {
    return this.dbConn.prepareStatement(sql, columnIndexes);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType,
      int resultSetConcurrency) throws SQLException
  {
    return this.dbConn.prepareStatement(sql, resultSetType,
        resultSetConcurrency);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType,
      int resultSetConcurrency, int resultSetHoldability) throws SQLException
  {
    return this.dbConn.prepareStatement(sql, resultSetType,
        resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames)
      throws SQLException
  {
    return this.dbConn.prepareStatement(sql, columnNames);
  }

}  // End DatabaseResultsPager.
