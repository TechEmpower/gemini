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

package com.techempower.data.util;

import gnu.trove.map.*;

import com.techempower.data.*;

/**
 * An implementation of TabularColumn that extracts field values from a
 * database connector.
 */
public class DatabaseColumn
  implements TabularColumn
{
  
  //
  // Member variables.
  //
  
  private final String displayFieldName;
  private final String dbFieldName;
  private final String defaultValue;
  
  /** 
   * labelFields is a lookup table from integer values stored in the database
   * and human-readable String values (e.g., user type 2 = "Subscriber").
   * This can be provided as either a String[] or an IntegerMap of Strings. 
   */
  private final Object labelFields;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   * 
   * @param displayName The display name of this column/field.
   * @param dbFieldName The name of the field in the database resultset.
   * @param defaultValue A default value to use if data is missing or bad.
   * @param labelFields an optional lookup table from integer values to
   *        Strings.  This can be either a String[] or IntegerMap of Strings.
   *        As integers are pulled from the database, values will be replaced
   *        with the Strings found in this lookup table.
   */
  public DatabaseColumn(String displayName, String dbFieldName, 
    String defaultValue, Object labelFields)
  {
    this.displayFieldName = displayName;
    this.labelFields      = labelFields;
    this.dbFieldName      = dbFieldName;
    this.defaultValue     = defaultValue;
  }

  /**
   * Constructor.
   * 
   * @param displayName The display name of this column/field.
   * @param dbFieldName The name of the field in the database resultset.
   * @param defaultValue A default value to use if data is missing or bad.
   */
  public DatabaseColumn(String displayName, String dbFieldName, 
    String defaultValue)
  {
    this.displayFieldName = displayName;
    this.dbFieldName      = dbFieldName;
    this.defaultValue     = defaultValue;
    this.labelFields      = null;
  }
  
  /**
   * Get the value for this column from the current resultset row in the
   * DatabaseConnector. 
   */
  @Override
  @SuppressWarnings("unchecked")
  public String getValue(Object source)
  {
    // This particular class only can get values from DatabaseConnectors.    
    if (source instanceof DatabaseConnector)
    {
      String dbValue;
      DatabaseConnector conn = (DatabaseConnector)source;
      
      // If we've been given a set of Strings that are mapped to database
      // integers, let's grab the integer and use the mapped String.
      if (labelFields != null)
      {
        if (labelFields instanceof TIntObjectMap)
        {
          final int dbInt = conn.getInt(dbFieldName);
          TIntObjectMap<String> labelFieldsMap = (TIntObjectMap<String>)labelFields;
          dbValue = labelFieldsMap.get(dbInt);
          if (dbValue == null)
          {
            dbValue = defaultValue;
          }
        }
        else if (labelFields instanceof TLongObjectMap)
        {
          final long dbLong = conn.getLong(dbFieldName);
          TLongObjectMap<String> labelFieldsMap = (TLongObjectMap<String>)labelFields;
          dbValue = labelFieldsMap.get(dbLong);
          if (dbValue == null)
          {
            dbValue = defaultValue;
          }
        }
        else
        {
          final int dbInt = conn.getInt(dbFieldName);
          String[] labelFieldsString = (String[])labelFields;
          if ( (dbInt >= 0)
            && (dbInt < labelFieldsString.length)
            )
          {
            dbValue = labelFieldsString[dbInt];
          }
          else
          {
            dbValue = defaultValue;
          }
        }
      }
      // Otherwise, get the value from the database directly.
      else
      {
        dbValue = conn.getField(dbFieldName, defaultValue);
      }
      
      return dbValue;
    }
    else
    {
      // If the source is not a DatabaseConnector, return the default value.
      return defaultValue;
    }
  }

  /**
   * Return the display name of this column.
   */
  @Override
  public String getDisplayName()
  {
    return displayFieldName;
  }

}  // End DatabaseColumn.
