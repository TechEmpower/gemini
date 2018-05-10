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

import java.lang.reflect.*;
import java.util.*;

import com.techempower.helper.*;

/**
 * An implementation of the TabularColumn interface that provides a simple
 * means to source fields from objects in cache.
 */
public class ObjectColumn
  implements TabularColumn
{
  //
  // Constants.
  //
  
  private static final Class<?>[]  PARAMETER_TYPES = new Class[0];
  private static final Object[] PARAMETER_VALUES = new Object[0];
  
  //
  // Member variables.
  //
  
  private final String displayFieldName;
  private final String objectMethodName;
  private final String defaultValue;
  private       Method objectMethod;
  
  /** 
   * labelFields is a lookup table from integer values stored in the object
   * and human-readable String values (e.g., user type 2 = "Subscriber").
   * This can be provided as either a String[] or an IntegerMap of Strings. 
   */
  private final Object labelFields;
  
  //
  // Member methods.
  //
  
  /**
   * Default constructor.
   * 
   * @param displayName The display name of this column/field.
   * @param objectMethodName A method to call on objects to get a value for 
   *        this field.
   * @param defaultValue A default value in case the method returns null or 
   *        fails.
   * @param labelFields an optional lookup table from integer values to
   *        Strings.  This can be either a String[] or IntegerMap of Strings.
   *        As integers are pulled from the object, values will be replaced
   *        with the Strings found in this lookup table.
   */
  public ObjectColumn(String displayName, String objectMethodName, 
    String defaultValue, Object labelFields)
  {
    this.displayFieldName = displayName;
    this.labelFields      = labelFields;
    this.objectMethodName = objectMethodName;
    this.defaultValue     = defaultValue;
  }

  /**
   * Simplified constructor.
   * 
   * @param displayName The display name of this column/field.
   * @param objectMethodName A method to call on objects to get a value for 
   *        this field.
   * @param defaultValue A default value in case the method returns null or 
   *        fails.
   */
  public ObjectColumn(String displayName, String objectMethodName, 
    String defaultValue)
  {
    this.displayFieldName = displayName;
    this.objectMethodName = objectMethodName;
    this.defaultValue     = defaultValue;
    this.labelFields      = null;
  }
  
  /**
   * Gets the value for this field from the provided object.
   */
  @Override
  @SuppressWarnings("unchecked")
  public String getValue(Object source)
  {
    // If we don't yet have a reference to the method we're using to fetch
    // values, try to get one here.
    if (this.objectMethod == null)
    {
      try
      {
        this.objectMethod = source.getClass().getMethod(this.objectMethodName, PARAMETER_TYPES);
      }
      catch (NoSuchMethodException nsmexc)
      {
        throw new IllegalArgumentException("Could not find method " + this.objectMethodName, nsmexc);
      }
    }
    
    try
    {
      String value;
      
      // If we've been given a set of Strings that are mapped to database
      // integers, let's grab the integer and use the mapped String.
      if (this.labelFields != null)
      {
        int intValue = (Integer)this.objectMethod.invoke(source, PARAMETER_VALUES);

        if (this.labelFields instanceof TIntObjectMap)
        {
          TIntObjectMap<String> labelFieldsMap = (TIntObjectMap<String>)this.labelFields;
          value = labelFieldsMap.get(intValue);
          if (value == null)
          {
            value = this.defaultValue;
          }
        }
        else
        {
          String[] labelFieldsString = (String[])this.labelFields;
          if ( (intValue >= 0)
            && (intValue < labelFieldsString.length)
            )
          {
            value = labelFieldsString[intValue];
          }
          else
          {
            value = this.defaultValue;
          }
        }
      }
      // Otherwise, get the value from the object directly.
      else
      {
        Object objValue = this.objectMethod.invoke(source, PARAMETER_VALUES);
        if (objValue != null)
        {
          if (objValue instanceof Date)
          {
            value = DateHelper.STANDARD_TECH_FORMAT.format(objValue);
          }
          else
          {
            value = "" + objValue;
          }
        }
        else
        {
          value = this.defaultValue;
        }
      }
      
      return value;
    }
    catch (IllegalAccessException iae)
    {
      // Return the default value.
      return this.defaultValue;
    }
    catch (InvocationTargetException ite)
    {
      // Return the default value.
      return this.defaultValue;
    }
  }

  @Override
  public String getDisplayName()
  {
    return this.displayFieldName;
  }
  
}  // End ObjectColumn.
