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

/**
 * A TabularColumn represents a column to be rendered into a table.  There are
 * only two methods: (a) what is the column's name and (b) given an object,
 * what is the object's value in that column.
 *   <p>
 * The two most common contexts for usage are:
 * 
 * <ul>
 * <li>Describing data to be written to a tabular format such as CSV.</li>
 * <li>Describing custom columns of data to render into a tabular user 
 *     interface such as a table of documents.</li>
 * </ul>
 * 
 * In fact, this interface was preceded by two separate interfaces that were
 * functionally equivalent but created for each of the two contexts above.
 * This interface replaces those two previous interfaces.
 *   <p>
 * Implementations intended for use in rendered HTML lists should escape raw
 * values using NetworkHelper.escapeStringForHtml.
 */
public interface TabularColumn
{
  
  /**
   * Gets the column's display name (e.g., "EmailAddress" or "FirstName").
   */
  String getDisplayName();

  /**
   * Gets a value from the provided source object.  The source is provided as
   * an object since it could be a plain object (where the implementation
   * is simply calling a method on the object to fetch a value) or wilder 
   * things such as a DatabaseConnector (where conn.getField() is being used)
   * or a Map, etc.  We assume that the implementations of this interface will
   * do the proper instanceof checking.
   */
  String getValue(Object source);
  
}
