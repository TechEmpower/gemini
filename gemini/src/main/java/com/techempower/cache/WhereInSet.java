package com.techempower.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * By wrapping a set of values in this class, it flags to the
 * {@link MethodValueCache} that any of the values can match when tested
 * against. In other words, this causes the value matching to behave like
 * `value IN (...)` in SQL.
 */
public class WhereInSet
{
  private Set<?> values;
  public WhereInSet(Collection<?> values)
  {
    this.values = new HashSet<>(values);
  }

  public boolean hasValue(Object value)
  {
    return values.contains(value);
  }

  public Object[] getValues()
  {
    return values.toArray();
  }
}
