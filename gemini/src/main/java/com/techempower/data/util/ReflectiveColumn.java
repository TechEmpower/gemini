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

import com.techempower.helper.*;

/**
 * Provides a means to include custom columns within standard lists, such as
 * a list of users.  The basic idea is that for a given set of objects being
 * displayed in a list, each column (such as "Color") is associated with some
 * method name (such as "getColor") that will be used to populate values
 * into the column.
 *    <p>
 * This data structure is the specification of a method name, column name,
 * and a default value if null or empty string is returned by the method.
 * Reflection is then used to get values from objects.
 */
public class ReflectiveColumn
  implements TabularColumn
{

  //
  // Constants.
  //
  
  public static final String TEMPLATE_MARKER = "$V";
  
  //
  // Member variables.
  //
  
  private final ObjectColumn odf;
  private final String template;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor with template.
   */
  public ReflectiveColumn(String methodName, String columnName, 
    String defaultValue, String template)
  {
    this.odf = new ObjectColumn(columnName, methodName, defaultValue);
    this.template = template;
  }
  
  /**
   * Constructor.
   */
  public ReflectiveColumn(String methodName, String columnName, 
    String defaultValue)
  {
    this(methodName, columnName, defaultValue, null);
  }
  
  /**
   * Gets the optional template to use for rendering values within the 
   * get(Object) method.
   * 
   * @return the template
   */
  public String getTemplate()
  {
    return this.template;
  }

  /**
   * Constructs an array of ReflectiveListColumn objects from a String array
   * composed of [method-name],[column-name],[default-value].  If a String
   * array of the incorrect size is passed, a zero-length result will be
   * returned.
   */
  public static ReflectiveColumn[] constructArray(String[] source)
  {
    int arrayInterval = 3;
    
    if (source.length % arrayInterval == 0)
    {
      ReflectiveColumn[] toReturn = new ReflectiveColumn[source.length / arrayInterval];
      int index = 0;
      for (int i = 0; i < source.length; i += arrayInterval)
      {
        toReturn[index++] = new ReflectiveColumn(source[i], source[i+1], source[i+2]); 
      }
      
      return toReturn;
    }
    
    return new ReflectiveColumn[0];
  }
  
  /**
   * Constructs an array of ReflectiveListColumn objects from a String array
   * composed of [method-name],[column-name],[default-value],[template-value].
   * If a String array of the incorrect size is passed, a zero-length result 
   * will be returned.
   */
  public static ReflectiveColumn[] constructArrayWithTemplates(String[] source)
  {
    int arrayInterval = 4;

    if (source.length % arrayInterval == 0)
    {
      ReflectiveColumn[] toReturn = new ReflectiveColumn[source.length / arrayInterval];
      int index = 0;
      for (int i = 0; i < source.length; i += arrayInterval)
      {
        toReturn[index++] = new ReflectiveColumn(source[i], source[i+1], source[i+2], source[i+3]); 
      }
      
      return toReturn;
    }
    
    return new ReflectiveColumn[0];
  }
  
  /**
   * Gets the column's value for a provided object. 
   */
  @Override
  public String getValue(Object object)
  {
    if (StringHelper.isNonEmpty(this.template))
    {
      return StringHelper.replaceSubstrings(this.template, TEMPLATE_MARKER, NetworkHelper.render(getRawValue(object)));
    }
    else
    { 
      return getRawValue(object);
    }
  }
  
  /**
   * Gets the header for this column.
   */
  @Override
  public String getDisplayName()
  {
    return this.odf.getDisplayName();
  }
  
  /**
   * Gets the column's raw value for a provided object.
   */
  public String getRawValue(Object object)
  {
    return this.odf.getValue(object);
  }

}  // End ReflectiveColumn.
