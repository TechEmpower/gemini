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

import java.lang.reflect.*;
import java.util.*;

import com.techempower.helper.*;

/**
 * Similar to ReflectiveColumn, except that this Composite version allows a
 * macro to be created, resulting in multiple method calls to the provided 
 * object.
 */
public class CompositeReflectiveColumn
  implements TabularColumn
{

  //
  // Constants.
  //
  
  private static final Class<?>[] ARGUMENT_TYPES = new Class[0];
  private static final Object[] ARGUMENTS = new Object[0];
  
  //
  // Member variables.
  //
  
  private final String[]             methodNames;
  private final String[]             methodFinds;
  private final String[]             defaultValues;
  private final String               columnHeader;
  private final Method[]             methods;
  private       boolean              initialized = false;
  private       String               template;
  
  //
  // Member methods.
  //
  
  /**
   * Standard Constructor.
   * 
   * @param methodNames an array of methods to call to retrieve values.
   * @param columnName the display name of the column.
   * @param defaultValues an array of default values, in the same order as
   *        the method names.
   * @param template a String representing how to render the gathered values.
   *        Values gathered from methods replace method names surrounded by
   *        { and }.  For example: "{getLastname}, {getFirstname}"
   */
  public CompositeReflectiveColumn(String[] methodNames, String columnName, 
    String defaultValues[], String template)
  {
    this.methodNames = methodNames.clone();
    this.methods = new Method[this.methodNames.length];
    this.methodFinds = new String[this.methodNames.length];
    this.columnHeader = columnName;
    this.defaultValues = defaultValues.clone();
    this.template = template;
    
    // Surround method names with { and } symbols so that we can quickly
    // find and replace them in the template.
    for (int i = 0; i < this.methodFinds.length; i++)
    {
      this.methodFinds[i] = "{" + this.methodNames[i] + "}";
    }
  }
  
  /**
   * Constructor that identifies method names and defaults by extracting
   * them from the template.  That is, "{getLastname}, {getFirstname}"
   * ascertains that the two methods, getLastname() and getFirstname() are
   * being called.  Default values can be provided by following the method
   * name with a colon.  For example: "{getLastname:Unknown}".
   */
  public CompositeReflectiveColumn(String columnHeader, String template)
  {
    List<String> newMethodNames = new ArrayList<>();
    List<String> newDefaultValues = new ArrayList<>();
    List<String> newMethodFinds = new ArrayList<>();
    
    if (StringHelper.isNonEmpty(template))
    {
      int pos = -1, endPos;
      do
      {
        pos = template.indexOf('{', pos + 1);
        if (pos >= 0)
        {
          endPos = template.indexOf('}', pos);
          String extract = template.substring(pos + 1, endPos);
          newMethodFinds.add("{" + extract + "}");
          
          if (extract.indexOf(':') >= 0)
          {
            String[] split = extract.split(":");
            newMethodNames.add(split[0]);
            newDefaultValues.add(split[1]);
          }
          else
          {
            newMethodNames.add(extract);
            newDefaultValues.add("");
          }
        }
      }
      while ( (pos >= 0) 
           && (pos < template.length())
           );
    }
    
    this.methodNames = CollectionHelper.toStringArray(newMethodNames);
    this.methods = new Method[this.methodNames.length];
    this.methodFinds = CollectionHelper.toStringArray(newMethodFinds);
    this.columnHeader = columnHeader;
    this.defaultValues = CollectionHelper.toStringArray(newDefaultValues);
    this.template = template;
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
   * Sets an optional string template to use for rendering values within the
   * get(Object) method.  If specified, the template will be populated with
   * the object's value where-ever the marker "$V" is found.  E.g.,
   * <tt>
   *   foo.setTemplate("/edit?id=$V");
   * </tt>
   * 
   * @param template the template to set
   */
  public void setTemplate(String template)
  {
    this.template = template;
  }

  /**
   * Gets the column's value for a provided object. 
   */
  @Override
  public String getValue(Object object)
  {
    String[] values = getRawValues(object);
    return StringHelper.replaceSubstrings(this.template, this.methodFinds, values);
  }
  
  /**
   * Gets the raw values for a provided object.  Note that Strings are
   * escaped for rendering as HTML as they are gathered from the source
   * object; it is not necessary to re-escape the resulting rendering.
   */
  public String[] getRawValues(Object object)
  {
    if (!this.initialized)
    {
      this.initialized = true;
      
      int i = 0;
      try
      {
        for (i = 0; i < this.methodNames.length; i++)
        {
          this.methods[i] = object.getClass().getMethod(this.methodNames[i], ARGUMENT_TYPES);
        }
      }
      catch (NoSuchMethodException exc)
      {
        throw new IllegalArgumentException("Could not find method " + this.methodNames[i], exc);
      }
    }

    String[] values = new String[this.methods.length];
    for (int i = 0; i < values.length; i++)
    {
      if (this.methods[i] != null)
      {
        try
        {
          Object objValue = this.methods[i].invoke(object, ARGUMENTS);
          
          // Render the value accordingly.
          if (objValue instanceof String)
          {
            final String string = (String)objValue;
            if (StringHelper.isNonEmpty(string))
            {
              values[i] = NetworkHelper.render(string);
            }
          }
          else if (objValue instanceof Integer || objValue instanceof Long)
          {
            values[i] = String.valueOf(objValue);
          }
        }
        catch (InvocationTargetException | IllegalAccessException exc)
        {
          // Do nothing.  Default will be used.
        }
      }
      
      if (values[i] == null)
      {
        values[i] = this.defaultValues[i];
      }
    }
    
    return values;
  }

  /**
   * Gets the header for this column.
   */
  @Override
  public String getDisplayName()
  {
    return this.columnHeader;
  }
  
}  // End CompositeReflectiveColumn.
