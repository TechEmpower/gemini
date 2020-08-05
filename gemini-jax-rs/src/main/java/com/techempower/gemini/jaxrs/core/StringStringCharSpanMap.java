package com.techempower.gemini.jaxrs.core;

import org.checkerframework.checker.units.qual.K;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class StringStringCharSpanMap
    implements Map<String, String>
{
  private final Map<CharSpan, CharSpan> backingMap;
  private       Map<String, String>     stringMap;

  StringStringCharSpanMap(StringStringCharSpanMap sourceMap)
  {
    this.backingMap = new HashMap<>(sourceMap.backingMap);
  }

  public StringStringCharSpanMap(Map<CharSpan, CharSpan> backingMap)
  {
    this.backingMap = backingMap;
  }

  boolean isEvaluated()
  {
    return backingMap != null;
  }

  protected Map<String, String> getStringMap()
  {
    if (stringMap == null)
    {
      Map<String, String> map = new HashMap<>(backingMap.size());
      backingMap.forEach((key, value) ->
          map.put(key.toString(), value.toString()));
      stringMap = map;
    }
    return stringMap;
  }

  @Override
  public int size()
  {
    if (stringMap == null)
    {
      return backingMap.size();
    }
    return stringMap.size();
  }

  @Override
  public boolean isEmpty()
  {
    if (stringMap == null)
    {
      return backingMap.isEmpty();
    }
    return stringMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key)
  {
    return getStringMap().containsKey(key);
  }

  @Override
  public boolean containsValue(Object value)
  {
    return getStringMap().containsValue(value);
  }

  @Override
  public String get(Object key)
  {
    return getStringMap().get(key);
  }

  @Override
  public String put(String key, String value)
  {
    return getStringMap().put(key, value);
  }

  /**
   * Updates the internal CharSpan CharSpan backing map. Does not update the
   * String String map if it has been evaluated.
   */
  public void putInternal(CharSpan key, CharSpan value)
  {
    backingMap.put(key, value);
  }

  @Override
  public String remove(Object key)
  {
    return getStringMap().remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> m)
  {
    getStringMap().putAll(m);
  }

  @Override
  public void clear()
  {
    getStringMap().clear();
  }

  @Override
  public Set<String> keySet()
  {
    return getStringMap().keySet();
  }

  @Override
  public Collection<String> values()
  {
    return getStringMap().values();
  }

  @Override
  public Set<Entry<String, String>> entrySet()
  {
    return getStringMap().entrySet();
  }

  @Override
  public int hashCode()
  {
    return getStringMap().hashCode();
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
    {
      return true;
    }

    if (!(o instanceof Map))
    {
      return false;
    }
    Map<?, ?> m = (Map<?, ?>) o;
    if (m.size() != size())
    {
      return false;
    }

    try
    {
      for (Entry<String, String> e : entrySet())
      {
        String key = e.getKey();
        String value = e.getValue();
        if (value == null)
        {
          if (!(m.get(key) == null && m.containsKey(key)))
          {
            return false;
          }
        }
        else
        {
          if (!value.equals(m.get(key)))
          {
            return false;
          }
        }
      }
    }
    catch (ClassCastException | NullPointerException unused)
    {
      return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    return getStringMap().toString();
  }
}
