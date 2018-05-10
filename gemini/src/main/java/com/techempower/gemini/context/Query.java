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
package com.techempower.gemini.context;

import java.util.*;

import com.techempower.collection.*;
import com.techempower.gemini.*;
import com.techempower.helper.*;

/**
 * Provides access to a request's query parameters.  A reference is fetched
 * via context.query().
 */
public class   Query
    implements MutableNamedValues
{
  
  private final Request       request;
  
  /**
   * The override map is lazy-initialized and typically only used when an
   * Input Processor mutates user input.
   */
  private Map<String, String> override;
  
  /**
   * Constructor.
   */
  public Query(Request request)
  {
    this.request = request;
  }

  /**
   * Lazy-initialize the override map.
   */
  private void lazyInitializeOverride()
  {
    if (override == null)
    {
      override = new HashMap<>();
    }
  }

  @Override
  public boolean has(String name)
  {
    return (override != null && override.containsKey(name))
        || (request.getParameter(name) != null);
  }

  @Override
  public Set<String> names()
  {
    final Enumeration<String> parameterNames = request.getParameterNames(); 
    
    if (  (parameterNames.hasMoreElements()) 
       || (override != null)
       )
    {
      final Set<String> names = new HashSet<>();
      
      while (parameterNames.hasMoreElements())
      {
        names.add(parameterNames.nextElement());
      }
  
      if (override != null)
      {
        names.addAll(override.keySet());
      }
  
      return names;
    }
    else
    {
      return Collections.emptySet();
    }
  }
  
  /**
   * Returns a Map<String, String> of the input values.
   */
  public Map<String, String> asMap()
  {
    final Set<String> names = names();
    if (names.size() > 0)
    {
      final Map<String, String> result = new HashMap<>(names.size());
      for (String name : names)
      {
        result.put(name, get(name));
      }
      return result;
    }
    else
    {
      return Collections.emptyMap();
    }
  }

  @Override
  public String get(String name)
  {
    return get(name, null);
  }

  @Override
  public String get(String name, String defaultValue)
  {
    String value = (override != null ? override.get(name) : null);
    if (value == null)
    {
      value = request.getParameter(name);
    }
    return (value != null)
        ? value
        : defaultValue;
  }
  
  /**
   * Get an array of Strings from the request.
   */
  public String[] getStrings(String name)
  {
    return request.getParameterValues(name);
  }
  
  /**
   * Gets an array of ints from the request.  Any non-numeric values will
   * be converted to 0.
   */
  public int[] getInts(String name)
  {
    final String[] values = getStrings(name);
    return (values != null)
        ? CollectionHelper.toIntArray(values)
        : null;
  }

  /**
   * Gets an array of lpmgs from the request.  Any non-numeric values will
   * be converted to 0.
   */
  public long[] getLongs(String name)
  {
    final String[] values = getStrings(name);
    return (values != null)
        ? CollectionHelper.toLongArray(values)
        : null;
  }

  @Override
  public int getInt(String name)
  {
    return getInt(name, 0);
  }

  @Override
  public int getInt(String name, int defaultValue)
  {
    return NumberHelper.parseInt(get(name), defaultValue);
  }

  @Override
  public int getInt(String name, int defaultValue, int minimum, int maximum)
  {
    return NumberHelper.boundInteger(getInt(name, defaultValue), minimum, maximum);
  }

  @Override
  public long getLong(String name, long defaultValue, long minimum,
      long maximum)
  {
    return NumberHelper.boundLong(getLong(name, defaultValue), minimum, maximum);
  }

  @Override
  public long getLong(String name)
  {
    return getLong(name, 0L);
  }

  @Override
  public long getLong(String name, long defaultValue)
  {
    return NumberHelper.parseLong(get(name), defaultValue);
  }

  @Override
  public boolean getBoolean(String name)
  {
    return getBoolean(name, false);
  }

  @Override
  public boolean getBoolean(String name, boolean defaultValue)
  {
    return StringHelper.parseBooleanStrict(get(name), defaultValue);
  }
  
  /**
   * Gets a boolean value leniently, allowing the value to be provided
   * as "true", "yes", "y", "on", or "1".
   */
  public boolean getBooleanLenient(String name, boolean defaultValue)
  {
    return StringHelper.parseBoolean(get(name), defaultValue);
  }
  
  /**
   * Gets an enum request value.  If the HttpServletRequest returns null for
   * this parameter, or if the provided value is invalid, null will be returned.
   *
   * @param name the name of the request parameter
   * @param type the type of the request parameter
   * @return the value as an enum.
   */
  public <O extends Enum<O>> O getEnum(String name, Class<O> type)
  {
    return this.getEnum(name, type, null);
  }

  /**
   * Gets an enum request value, with a default value provided.  If the
   * HttpServletRequest returns null for this parameter, or if the provided
   * value is invalid, the default will be returned.
   *
   * @param name the name of the request parameter
   * @param type the type of the request parameter
   * @param defaultValue a value to be returned if no value is provided by
   *        the request, or if the provided value is invalid.
   * @return the value as an enum.
   */
  public <O extends Enum<O>> O getEnum(String name, Class<O> type, O defaultValue)
  {
    final String value = get(name);
    if (value == null)
    {
      return defaultValue;
    }
    try
    {
      return Enum.valueOf(type, value);
    }
    catch (IllegalArgumentException e)
    {
      return defaultValue;
    }
  }

  /**
   * Gets a double request value, with a default value of zero.  If the
   * HttpServletRequest returns null for this parameter, zero will
   * be returned.
   *
   * @param name the name of the request parameter
   *
   * @return the value as a double.
   */
  public double getDouble(String name)
  {
    return getDouble(name, 0.0d);
  }

  /**
   * Gets a double request value, with a default value provided.  If the
   * HttpServletRequest returns null for this parameter, the default will
   * be returned.
   *
   * @param name the name of the request parameter
   * @param defaultValue a value to be returned if no value is provided by
   *        the request.
   *
   * @return the value as a double.
   */
  public double getDouble(String name, double defaultValue)
  {
    double value = defaultValue;
    try
    {
      value = Double.parseDouble(get(name));
    }
    catch (Exception exc)
    {
      // Do nothing.
    }

    return value;
  }
  
  @Override
  public MutableNamedValues put(String name, String value) 
  {
    lazyInitializeOverride();
    override.put(name, value);
    return this;
  }

  @Override
  public MutableNamedValues put(String name, int value) 
  {
    put(name, Integer.toString(value));
    return this;
  }

  @Override
  public MutableNamedValues put(String name, long value) 
  {
    put(name, Long.toString(value));
    return this;
  }

  @Override
  public MutableNamedValues put(String name, boolean value) 
  {
    put(name, Boolean.toString(value));
    return this;
  }

  @Override
  public MutableNamedValues remove(String name) 
  {
    if (override != null)
    {
      override.remove(name);
    }
    return this;
  }

  /**
   * Instead of clearing, this will reset the override map.
   */
  @Override
  public MutableNamedValues clear() 
  {
    override = null;
    return this;
  }

}
