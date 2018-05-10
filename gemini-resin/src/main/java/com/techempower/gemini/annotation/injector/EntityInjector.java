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
package com.techempower.gemini.annotation.injector;

import java.lang.annotation.*;

import com.techempower.cache.*;
import com.techempower.gemini.*;
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * This is the HandlerInjector class for the &#064;Entity annotation.
 * This class handles fetching DataEntities from the cache based.
 */
public class EntityInjector<D extends BasicDispatcher, C extends Context> 
  implements ParameterInjector<D, C>
{
  
  private EntityStore cache; 

  /**
   * Creates a new entity injector for the given application.
   *
   * @param application the application
   */
  public EntityInjector(GeminiApplication application)
  {
    this.cache = application.getStore();
  }
  
  /**
   * Returns the DataEntity of type c. Returns null if the object isn't in the cache, or if
   * there's an error in the request value.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Object getArgument(D dispatcher, C context, Annotation annotation, Class<?> c) 
  {
    // make sure that the annotation is the correct type
    if(annotation instanceof Entity)
    {
      // entity.value() contains the request key lookup for the dataentity id.
      Entity entity = (Entity)annotation;
      if(StringHelper.isNonEmpty(context.query().get(entity.value())))
      {
        try
        {
          // make sure the request value is an actual int value
          int id = Integer.parseInt(context.query().get(entity.value()));
          
          // try to retrieve the object from the cache.
          return this.cache.get((Class<Identifiable>)c, id);
        }
        catch(Exception e)
        {
          // the value from the request was not a string value, returning null
          return null;
        }
      }
      else
      {
        // the request value was empty, returning null
        return null;
      }
    }
    else
    {
      // the annotation was not of type Entity, returning null
      return null;
    }
  }
  
}
