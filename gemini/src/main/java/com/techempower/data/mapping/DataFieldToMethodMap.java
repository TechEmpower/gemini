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

import java.lang.reflect.*;
import java.time.*;
import java.util.*;

/**
 * Used by {@link com.techempower.data.EntityGroup}.  Maps a Method to a database
 * field name.
 */
public class DataFieldToMethodMap
  extends    DataFieldToObjectEntityMap
{
  
  //
  // Enumerations.
  //
  
  public enum Type {
      Unknown,
      BooleanPrimitive,
      BytePrimitive,
      CharPrimitive,
      DoublePrimitive,
      FloatPrimitive,
      IntPrimitive,
      LongPrimitive,
      ShortPrimitive,
      String,
      IntegerObject,
      ByteObject,
      LongObject,
      ShortObject,
      BooleanObject,
      CharacterObject,
      DoubleObject,
      FloatObject,
      Enum,
      Date,
      Calendar,
      LocalDate,
      LocalTime,
      LocalDateTime,
      OffsetDateTime
  }
  
  //
  // Variables.
  //
  
  private final Method   method;
  private final int      methodIndex;
  private final Class<?> javaFieldType;
  private final int      columnIndex;
  private final boolean  primitive;
  private final Type     type;

  //
  // Methods.
  //

  /**
   * Constructor.
   */
  public DataFieldToMethodMap(Method method, String columnName, 
      int columnIndex, int fieldType, int methodIndex)
  {
    super(columnName, fieldType);
    this.method      = method;
    this.methodIndex = methodIndex;
    this.columnIndex = columnIndex;
    
    if (method.getParameterTypes().length > 0)
    {
      this.javaFieldType = method.getParameterTypes()[0];
      
      // Translate the Java field type to our Type enumeration.
      if (this.javaFieldType.isPrimitive())
      {
        this.primitive = true;
        if (this.javaFieldType == boolean.class)
        {
          this.type = Type.BooleanPrimitive;
        }
        else if (this.javaFieldType == byte.class)
        {
          this.type = Type.BytePrimitive;
        }
        else if (this.javaFieldType == char.class)
        {
          this.type = Type.CharPrimitive;
        }
        else if (this.javaFieldType == double.class)
        {
          this.type = Type.DoublePrimitive;
        }
        else if (this.javaFieldType == float.class)
        {
          this.type = Type.FloatPrimitive;
        }
        else if (this.javaFieldType == int.class)
        {
          this.type = Type.IntPrimitive;
        }
        else if (this.javaFieldType == long.class)
        {
          this.type = Type.LongPrimitive;
        }
        else if (this.javaFieldType == short.class)
        {
          this.type = Type.ShortPrimitive;
        }
        else
        {
          throw new AssertionError("Unknown primitive type: " + this.javaFieldType.getName());
        }
      }
      else
      {
        this.primitive = false;
        if (this.javaFieldType == String.class)
        {
          this.type = Type.String;
        }
        else if (this.javaFieldType == Integer.class)
        {
          this.type = Type.IntegerObject;
        }
        else if (this.javaFieldType == Long.class)
        {
          this.type = Type.LongObject;
        }
        else if (this.javaFieldType == Short.class)
        {
          this.type = Type.ShortObject;
        }
        else if (this.javaFieldType == Boolean.class)
        {
          this.type = Type.BooleanObject;
        }
        else if (this.javaFieldType == Byte.class)
        {
          this.type = Type.ByteObject;
        }
        else if (this.javaFieldType == Character.class)
        {
          this.type = Type.CharacterObject;
        }
        else if (this.javaFieldType == Double.class)
        {
          this.type = Type.DoubleObject;
        }
        else if (this.javaFieldType == Float.class)
        {
          this.type = Type.FloatObject;
        }
        else if (this.javaFieldType.isEnum())
        {
          this.type = Type.Enum;
        }
        else if (this.javaFieldType == Date.class)
        {
          this.type = Type.Date;
        }
        else if (this.javaFieldType.isAssignableFrom(java.util.Calendar.class))
        {
          this.type = Type.Calendar;
        }
        else if (this.javaFieldType == LocalDate.class)
        {
          this.type = Type.LocalDate;
        }
        else if (this.javaFieldType == LocalTime.class)
        {
          this.type = Type.LocalTime;
        }
        else if (this.javaFieldType == LocalDateTime.class)
        {
          this.type = Type.LocalDateTime;
        }
        else if (this.javaFieldType == OffsetDateTime.class)
        {
          this.type = Type.OffsetDateTime;
        }
        // Unknown type.
        else
        {
          this.type = Type.Unknown;
        }
      }
    }
    else
    {
      // Method signature not available.
      this.javaFieldType = null;
      this.type = Type.Unknown;
      this.primitive = true;
    }
  }

  /**
   * Gets the method.
   */
  public Method getMethod()
  {
    return this.method;
  }
  
  /**
   * Gets the method index.
   */
  public int getMethodIndex()
  {
    return this.methodIndex;
  }
  
  /**
   * Gets the column index.
   */
  public int getColumnIndex() 
  {
    return this.columnIndex;
  }
  
  /**
   * Gets the Java field type.
   */
  public Class<?> getJavaFieldType()
  {
    return this.javaFieldType;
  }
  
  /**
   * Is the associated Java field type a primitive?
   */
  public boolean isPrimitive()
  {
    return this.primitive;
  }
  
  /**
   * Return our "Type" classification for use in deserializing objects from
   * a database.
   */
  public Type getType()
  {
    return this.type;
  }
  
  /**
   * Basic toString.
   */
  @Override
  public String toString()
  {
    return "DFTMM [" + getFieldName() + "; " + getFieldType() + "]";
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof DataFieldToMethodMap))
    {
      return false;
    }

    return ((DataFieldToMethodMap)o).getFieldName().equals(this.getFieldName()) 
        && ((DataFieldToMethodMap)o).method.equals(this.method);
  }

	@Override
  public int hashCode()
  {
	  return super.hashCode();
  }

}   // End DataFieldToMethodMap.

