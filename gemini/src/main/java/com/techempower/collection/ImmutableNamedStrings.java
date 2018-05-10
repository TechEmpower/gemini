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
package com.techempower.collection;

import java.util.*;

import com.techempower.helper.*;

/**
 * An implementation of ImmutableNamedValues that uses a backing Map<String, 
 * String> as a data store.
 */
public class   ImmutableNamedStrings
    implements NamedValues
{
  
  private final Map<String, String> values;
  private boolean sealed;
  
  /**
   * Constructor.
   */
  public ImmutableNamedStrings(Map<String, String> values)
  {
    this.values = values;
    sealed = true;
  }
  
  /**
   * Constructor.
   */
  public ImmutableNamedStrings(int initialSize)
  {
    if (initialSize > 0)
    {
      this.values = new HashMap<>(initialSize);
      sealed = false;
    }
    else
    {
      this.values = null;
      sealed = true;
    }
  }
  
  /**
   * Seal this structure so that no more puts are permitted.
   */
  public ImmutableNamedStrings seal()
  {
    sealed = true;
    return this;
  }
  
  /**
   * Is this structure sealed?
   */
  public boolean isSealed()
  {
    return sealed;
  }
  
  /**
   * Puts a value.
   */
  public ImmutableNamedStrings put(String name, String value)
  {
    if (sealed)
    {
      throw new IllegalStateException("This BasicNamedValues is sealed; put is not permitted.");
    }
    
    values.put(name, value);
    return this;
  }
  
  @Override
  public boolean has(String name)
  {
    return get(name) != null;
  }

  @Override
  public Set<String> names()
  {
    return values.keySet();
  }

  @Override
  public String get(String name)
  {
    if (values == null)
    {
      return null;
    }
    else
    {
      return values.get(name);
    }
  }
  
  @Override
  public String get(String name, String defaultValue)
  {
    final String value = get(name);
    return value != null
        ? value
        : defaultValue;
  }

  @Override
  public int getInt(String name)
  {
    return NumberHelper.parseInt(get(name), 0);
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
    return StringHelper.parseBoolean(get(name), defaultValue);
  }

}
