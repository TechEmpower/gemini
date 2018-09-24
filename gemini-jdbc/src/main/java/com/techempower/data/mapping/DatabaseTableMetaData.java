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

package com.techempower.data.mapping;

import java.util.*;

import com.techempower.data.*;
import com.techempower.helper.*;

/**
 * Container for holding meta data for database tables
 */
public class DatabaseTableMetaData
{
  private String                             catalogName;
  private String                             schemaName;
  private String                             tableName;
  private String                             tableType;

  private Collection<DatabaseColumnMetaData> columnData;

  public DatabaseTableMetaData()
  {
    this("", "", "", "");
  }

  public DatabaseTableMetaData(String tableName)
  {
    this("", "", tableName, "TABLE");
  }

  public DatabaseTableMetaData(String tableName, String tableType)
  {
    this("", "", tableName, tableType);
  }

  public DatabaseTableMetaData(String catalogName, String schemaName,
      String tableName, String tableType)
  {
    this.catalogName = catalogName;
    this.schemaName = schemaName;
    this.tableName = tableName;
    this.tableType = tableType;

    this.columnData = null;
  }

  public void loadColumnData(DatabaseConnector connector)
  {
    if (StringHelper.isNonEmpty(this.tableName))
    {
      this.columnData = connector.getColumnMetaDataForTable(this.tableName);
    }
  }

  /**
   * @return Returns the catalogName.
   */
  public String getCatalogName()
  {
    return this.catalogName;
  }

  /**
   * @param catalogName The catalogName to set.
   */
  public void setCatalogName(String catalogName)
  {
    this.catalogName = catalogName;
  }

  /**
   * @return Returns the schemaName.
   */
  public String getSchemaName()
  {
    return this.schemaName;
  }

  /**
   * @param schemaName The schemaName to set.
   */
  public void setSchemaName(String schemaName)
  {
    this.schemaName = schemaName;
  }

  /**
   * @return Returns the tableName.
   */
  public String getTableName()
  {
    return this.tableName;
  }

  /**
   * @param tableName The tableName to set.
   */
  public void setTableName(String tableName)
  {
    this.tableName = tableName;
  }

  /**
   * @return Returns the tableType.
   */
  public String getTableType()
  {
    return this.tableType;
  }

  /**
   * @param tableType The tableType to set.
   */
  public void setTableType(String tableType)
  {
    this.tableType = tableType;
  }

  /**
   * @return Returns the columnData.
   */
  public Collection<DatabaseColumnMetaData> getColumnData()
  {
    return this.columnData;
  }
}
