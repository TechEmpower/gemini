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
package com.techempower.gemini.input.validator;

import com.techempower.cache.*;
import com.techempower.gemini.input.*;
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * Prevents collisions with existing entities.
 */
public class UniquenessValidator
     extends ElementValidator 
{

  private final Class<? extends Identifiable> entityClass;
  private final String methodName;
  private final Object self;
  
  /**
   * Constructor. 
   */
  public <T extends Identifiable> UniquenessValidator(
      String elementName, Class<T> entityClass, String methodName, 
      Object selfReference)
  {
    super(elementName);
    this.entityClass = entityClass;
    this.methodName = methodName;
    this.self = selfReference;
  }
  
  /**
   * Constructor. 
   */
  public <T extends Identifiable> UniquenessValidator(
      String elementName, Class<T> entityClass, String methodName)
  {
    this(elementName, entityClass, methodName, null);
  }

  @Override
  public void process(Input input) 
  {
    final EntityStore store = input.context().getApplication().getStore();
    final String value = getUserValue(input);
    
    // Assume empty Strings may fail validation elsewhere.
    if (StringHelper.isNonEmpty(value))
    {
      final Object found = store.get(this.entityClass, this.methodName, value);
      
      // If an object was found that doesn't match the self reference (or if
      // self is null and anything is found), the validation will fail.
      if (  ( (this.self != null) && (found != this.self) && (found != null) ) 
         || ( (this.self == null) && (found != null) )
         )
      {
        input.addError("The value provided in " + getElementName() 
            + " is already in use, please try another.");
      }
    }
  }

}
