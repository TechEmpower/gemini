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

package com.techempower.helper;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.techempower.data.*;

/**
 * DatabaseHelper provides low-level utility functions for constructing and 
 * working with database queries, such as escaping Strings.
 */
public final class DatabaseHelper
{
  
  //
  // Static variables.
  //

  private static final String[] MYSQL_ESCAPE_FINDS    = new String[] { "'",  "\\" };
  private static final String[] MYSQL_ESCAPE_REPLACES = new String[] { "''", "\\\\" };

  //
  // Static methods.
  //

  /**
   * Query-prepares a String parameter.  This escapes any single-quotes as
   * double-single quotes and then surrounds the resulting String in single
   * quotes.  It is analogous to doing the following:
   *   <p>
   * String result = "'" + escapeSingleQuotes("I said, 'hello.'") + "'";
   */
  public static String prepare(String inputString)
  {
    return '\'' + StringHelper.escapeSingleQuotes(inputString) + '\'';
  }

  /**
   * Query-prepares a String parameter for MySQL. MySQL needs to have both
   * single-quotes and backslashes escaped.
   */
  public static String prepareMySql(String inputString)
  {
    return '\'' + StringHelper.replaceSubstrings(inputString, MYSQL_ESCAPE_FINDS, MYSQL_ESCAPE_REPLACES) + '\'';
  }

  /**
   * Query-prepares a long parameter for insertion as a timestamp/date into a
   * database query.
   */
  public static String prepareDate(long datetime)
  {
    java.sql.Timestamp ts = new java.sql.Timestamp(datetime);
    return '\'' + ts.toString() + '\'';
  }
  
  /**
   * Query-prepares a date parameter for insertion into a sql database
   * in the proper MSSQL format.
   */
  public static String prepare(Date date)
  {
    return prepareDate(date.getTime());
  }
  
  /**
   * Query-prepares a Calendar parameter.
   */
  public static String prepare(Calendar cal)
  {
    return prepareDate(cal.getTimeInMillis());
  }

  /**
   * Query-prepares a String parameter.  This escapes any single-quotes as
   * double-single quotes and then surrounds the resulting String in single
   * quotes.  It is analogous to doing the following:
   *   <p>
   * String result = "\"" + escapeDoubleQuotes("I said, \"hello.\"") + "\"";
   */
  public static String prepareDoubleQuote(String inputString)
  {
    return '\"' + StringHelper.escapeDoubleQuotes(inputString) + '\"';
  }

  /**
   * Takes a collection of objects and tries to _intelligently_ create an IN clause out of it,
   * returning items surrounded by parentheses and delimited by commas.
   * 
   * @param obs The collection of objects that need to be in-clause-ified
   * @param mysql Is this for mysql?
   * @return a String that looks like ('blah', 'blah2') or null if the collection is empty
   */
  public static <E> String prepareInClause(Collection<E> obs, boolean mysql)
  {
    final StringBuilder sb = new StringBuilder();
    
    if (CollectionHelper.isNonEmpty(obs))
    {
      for (E o : obs)
      {
        if (sb.length() > 0)
        {
          sb.append(",");
        }
        
        if (StringHelper.isAllNumeric(String.valueOf(o)))
        {
          sb.append(o);
        }
        else if (o instanceof Date)
        {
          sb.append(DatabaseHelper.prepare((Date)o));
        }
        else if (o instanceof Calendar)
        {
          sb.append(DatabaseHelper.prepare((Calendar)o));
        }
        else
        {
          if (!mysql)
          {
            sb.append(DatabaseHelper.prepare((String)o));
          }
          else
          {
            sb.append(DatabaseHelper.prepareMySql((String)o));
          }
        }
      }
      sb.insert(0, "(");
      sb.append(")");
      return sb.toString();
    }
    return null;
  }

  /**
   * Wraps a SQL table name or column name in the identifier quote strings used
   * by the database.  For example, MySQL uses the "`" character.  Table or
   * column names wrapped in these characters can be used safely in SQL queries
   * even if they are reserved keywords.
   *
   * @param connectorFactory the connector factory being used, which knows the
   *                         identifier quote string
   * @param tableOrColumn the name of the table or column being quoted
   * @return the table or column name wrapped in the identifier quotes
   */
  public static String quoteTableOrColumn(ConnectorFactory connectorFactory,
      String tableOrColumn)
  {
    Objects.requireNonNull(connectorFactory);
    Objects.requireNonNull(tableOrColumn);
    String quote = connectorFactory.getIdentifierQuoteString();
    return quote + tableOrColumn + quote;
  }

  /**
   * You may not instantiate this class.
   */
  private DatabaseHelper()
  {
    // Does nothing.
  }

  public static String getString(ResultSet resultSet, String fieldName, String defaultValue) throws SQLException
  {
    String result = resultSet.getString(fieldName);
    if (result != null)
    {
      return result;
    }
    return defaultValue;
  }

}  // End DatabaseHelper.

