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

import com.techempower.gemini.*;

/**
 * Implementation of ParameterInjector that knows how to pull objects from the request.
 * Supported types are:
 * - String
 * - File
 * - int/Integer
 * - double/Double
 * - boolean/Boolean
 * - long/Long
 */
public class ParamInjector<D extends BasicDispatcher, C extends Context>
  implements ParameterInjector<D, C> 
{
  @Override
  public Object getArgument(D dispatcher, C context, Annotation annotation, Class<?> c) 
  {
    Param p = (Param)annotation;
    
    return getType(c, p.value(), p.defaultValue(), context);
  }
  
  private Object getType(Class<?> c, String key, String defaultValue, C context)
  {
    if(c.equals(String.class))
    {
      return context.query().get(key, defaultValue);
    }
    else if(c.equals(int.class) || c.equals(Integer.class))
    {
      try
      {
        return context.query().getInt(key, Integer.parseInt(defaultValue));
      }
      catch(Exception e)
      {
        return context.query().getInt(key);
      }
    }
    else if(c.equals(double.class) || c.equals(Double.class))
    {
      try
      {
        return context.query().getDouble(key, Double.parseDouble(defaultValue));
      }
      catch (Exception e)
      {
        return context.query().getDouble(key);
      }
    }
    else if(c.equals(boolean.class) || c.equals(Boolean.class))
    {
      try
      {
        return context.query().getBoolean(key, Boolean.parseBoolean(defaultValue));
      }
      catch(Exception e)
      {
        return context.query().getBoolean(key, false);
      }
    }
    else if(c.equals(long.class) || c.equals(Long.class))
    {
      try
      {
        return context.query().getLong(key, Long.parseLong(defaultValue));
      }
      catch(Exception e)
      {
        return context.query().getLong(key);
      }
    }
    
    return null;
  }
}
