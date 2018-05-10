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

import java.sql.*;

/**
 * Meta-data about database columns.
 */
public class DatabaseColumnMetaData {
  private String  catalogName;
  private String  schemaName;
  private String  tableName;
  private String  columnName;
  private int     dataType;
  private String  typeName;
  private int     columnSize;
  private int     decimalDigits;
  private int     radix;
  private boolean nullable;
  private String  defaultValue;
  private int     ordinalPosition;

  public DatabaseColumnMetaData() {
    this("");
  }

  public DatabaseColumnMetaData(String columnName) {
    this(columnName, Types.OTHER);
  }

  public DatabaseColumnMetaData(String columnName, int dataType) {
    this("", columnName, dataType);
  }

  public DatabaseColumnMetaData(String tableName, String columnName,
      int dataType) {
    this.catalogName = "";
    this.schemaName = "";
    this.tableName = tableName;
    this.columnName = columnName;
    this.dataType = dataType;
    this.typeName = "";
    this.columnSize = -1;
    this.decimalDigits = -1;
    this.radix = -1;
    this.nullable = false;
    this.defaultValue = "";
    this.ordinalPosition = -1;
  }

  /**
   * @return Returns the catalogName.
   */
  public String getCatalogName() {
    return this.catalogName;
  }

  /**
   * @param catalogName The catalogName to set.
   */
  public void setCatalogName(String catalogName) {
    this.catalogName = catalogName;
  }

  /**
   * @return Returns the columnName.
   */
  public String getColumnName() {
    return this.columnName;
  }

  /**
   * @param columnName The columnName to set.
   */
  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  /**
   * @return Returns the columnSize.
   */
  public int getColumnSize() {
    return this.columnSize;
  }

  /**
   * @param columnSize The columnSize to set.
   */
  public void setColumnSize(int columnSize) {
    this.columnSize = columnSize;
  }

  /**
   * @return Returns the dataType.
   */
  public int getDataType() {
    return this.dataType;
  }

  /**
   * @param dataType The dataType to set.
   */
  public void setDataType(int dataType) {
    this.dataType = dataType;
  }

  /**
   * @return Returns the decimalDigits.
   */
  public int getDecimalDigits() {
    return this.decimalDigits;
  }

  /**
   * @param decimalDigits The decimalDigits to set.
   */
  public void setDecimalDigits(int decimalDigits) {
    this.decimalDigits = decimalDigits;
  }

  /**
   * @return Returns the defaultValue.
   */
  public String getDefaultValue() {
    return this.defaultValue;
  }

  /**
   * @param defaultValue The defaultValue to set.
   */
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * @return Returns the nullable.
   */
  public boolean isNullable() {
    return this.nullable;
  }

  /**
   * @param nullable The nullable to set.
   */
  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  /**
   * @return Returns the ordinalPosition.
   */
  public int getOrdinalPosition() {
    return this.ordinalPosition;
  }

  /**
   * @param ordinalPosition The ordinalPosition to set.
   */
  public void setOrdinalPosition(int ordinalPosition) {
    this.ordinalPosition = ordinalPosition;
  }

  /**
   * @return Returns the radix.
   */
  public int getRadix() {
    return this.radix;
  }

  /**
   * @param radix The radix to set.
   */
  public void setRadix(int radix) {
    this.radix = radix;
  }

  /**
   * @return Returns the schemaName.
   */
  public String getSchemaName() {
    return this.schemaName;
  }

  /**
   * @param schemaName The schemaName to set.
   */
  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  /**
   * @return Returns the tableName.
   */
  public String getTableName() {
    return this.tableName;
  }

  /**
   * @param tableName The tableName to set.
   */
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * @return Returns the typeName.
   */
  public String getTypeName() {
    return this.typeName;
  }

  /**
   * @param typeName The typeName to set.
   */
  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  @Override
  public String toString() {
    return "DCMD [" + getColumnName() + "; " + getDataType() + "]";
  }
}
