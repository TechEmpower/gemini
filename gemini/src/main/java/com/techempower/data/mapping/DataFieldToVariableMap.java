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

/**
 * Used by {@link com.techempower.data.EntityGroup}.  Maps a Field (variable) to a
 * database field name.
 */
public class DataFieldToVariableMap
  extends    DataFieldToObjectEntityMap
{
  
  //
  // Variables.
  //
  
  private final Field  variable;
  
  //
  // Methods.
  //

  /**
   * Utility constructor.
   */
  public DataFieldToVariableMap(Field variable, String fieldName, int fieldType)
  {
    super(fieldName, fieldType);
    this.variable  = variable;
  }

  /**
   * Get the variable.
   */
  public Field getVariable()
  {
    return this.variable;
  }

  /**
   * Basic toString.
   */
  @Override
  public String toString()
  {
    return "DFTVM [" + this.getFieldName() + "; " + this.getFieldType() + "]";
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof DataFieldToVariableMap))
    {
      return false;
    }

    return ((DataFieldToVariableMap)o).getFieldName().equals(this.getFieldName()) 
        && ((DataFieldToVariableMap)o).variable.equals(this.variable);
  }

	@Override
  public int hashCode()
  {
	  return super.hashCode();
  }

}   // End DataFieldToVariableMap.

