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
import com.techempower.gemini.session.*;
import com.techempower.helper.*;

/**
 * An implementation of MutableNamedValues for covering a Context's Session.
 * A reference is fetched via context.session().
 */
public class SessionNamedValues
  implements MutableNamedValues
{
  
  private final Context context;
  
  /**
   * Constructor.
   */
  public SessionNamedValues(Context context)
  {
    this.context = context;
  }
  
  @Override
  public boolean has(String name)
  {
    final Session currentSession = context.getSession(false);
    return (  (currentSession != null) 
           && (currentSession.getAttribute(name) != null)
           );
  }

  @Override
  public Set<String> names()
  {
    final Set<String> toReturn = new HashSet<>();
    final Session currentSession = context.getSession(false);
    if (currentSession != null)
    {
      final Enumeration<String> attributeNames = 
          currentSession.getAttributeNames();
      while (attributeNames.hasMoreElements())
      {
        toReturn.add(attributeNames.nextElement());
      }
    }
    
    return toReturn;
  }

  /**
   * Get an arbitrary object from the Session.
   */
  @SuppressWarnings("unchecked")
  public <O extends Object> O getObject(String name, O defaultValue)
  {
    final Session currentSession = context.getSession(false);
    final O obj = (currentSession != null) 
        ? (O)currentSession.getAttribute(name)
        : null;
    return (obj != null)
        ? obj
        : defaultValue;
  }

  /**
   * Get an arbitrary object from the Session.  Returns null if the named
   * value is not present.
   */
  public <O extends Object> O getObject(String name)
  {
    return getObject(name, null);
  }

  /**
   * Put an arbitrary object into the Session.
   */
  public SessionNamedValues putObject(String name, Object value)
  {
    context.getSession(true).setAttribute(name, value);
    
    return this;
  }

  @Override
  public String get(String name)
  {
    return get(name, null);
  }

  @Override
  public String get(String name, String defaultValue)
  {
    final Session session = context.getSession(false);
    if (session != null)
    {
      try
      {
        final String value = (String)session.getAttribute(name);
        if (value != null)
        {
          return value;
        }
      }
      catch (ClassCastException ccexc)
      {
        // Do nothing.
      }
    }
    
    // Return null by default.
    return defaultValue;
  }

  @Override
  public int getInt(String name)
  {
    return getInt(name, 0);
  }

  @Override
  public int getInt(String name, int defaultValue)
  {
    final Session session = context.getSession(false);
    if (session != null)
    {
      try
      {
        final Integer value = (Integer)session.getAttribute(name);
        if (value != null)
        {
          return value;
        }
      }
      catch (ClassCastException ccexc)
      {
        // Do nothing.
      }
    }
    
    // Return null by default.
    return defaultValue;
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
    final Session session = context.getSession(false);
    if (session != null)
    {
      try
      {
        final Long value = (Long)session.getAttribute(name);
        if (value != null)
        {
          return value;
        }
      }
      catch (ClassCastException ccexc)
      {
        // Do nothing.
      }
    }
    
    // Return null by default.
    return defaultValue;
  }

  @Override
  public boolean getBoolean(String name)
  {
    return getBoolean(name, false);
  }

  @Override
  public boolean getBoolean(String name, boolean defaultValue)
  {
    final Session session = context.getSession(false);
    if (session != null)
    {
      try
      {
        final Boolean value = (Boolean)session.getAttribute(name);
        if (value != null)
        {
          return value;
        }
      }
      catch (ClassCastException ccexc)
      {
        // Do nothing.
      }
    }
    
    // Return null by default.
    return defaultValue;
  }

  @Override
  public SessionNamedValues put(String name, String value)
  {
    return putObject(name, value);
  }

  @Override
  public SessionNamedValues put(String name, int value)
  {
    return putObject(name, value);
  }

  @Override
  public SessionNamedValues put(String name, long value)
  {
    return putObject(name, value);
  }

  @Override
  public SessionNamedValues put(String name, boolean value)
  {
    return putObject(name, value);
  }

  @Override
  public SessionNamedValues remove(String name)
  {
    final Session session = context.getSession(false);
    if (session != null)
    {
      session.removeAttribute(name);
    }
    return this;
  }

  @Override
  public SessionNamedValues clear()
  {
    final Session session = context.getSession(false);
    if (session != null)
    {
      final Enumeration<String> names = session.getAttributeNames();
      final List<String> toRemove = new ArrayList<>();
      while (names.hasMoreElements())
      {
        toRemove.add(names.nextElement());
      }

      for (String name : toRemove)
      {
        session.removeAttribute(name);
      }
    }
    return this;
  }
  
  /**
   * Invalidate the session.  This is typically only done at logoff time.
   */
  public SessionNamedValues invalidate()
  {
    context.invalidateSession();
    return this;
  }

}
