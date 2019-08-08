package com.techempower.cache;

import com.techempower.util.Identifiable;

import java.util.*;

/**
 * This class is meant to help keep store accesses easy to read, by clearly
 * separating different method-value pairs into different method calls.
 */
public class EntitySelector<T extends Identifiable>
{

  private final EntityStore store;
  private final Class<T>    type;
  private final List<String> methods = new ArrayList<>(4);
  private final List<Object> values = new ArrayList<>(4);

  EntitySelector(Class<T> type, EntityStore store)
  {
    this.type = type;
    this.store = store;
  }

  /**
   * Returns the entity of the selected class with the given id. Ignores any
   * specified method-value pairs.
   */
  public T get(int id)
  {
    return this.store.get(this.type, id);
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link #list()} and {@link #get()}.
   */
  public EntitySelector<T> where(String methodName, Object value)
  {
    this.methods.add(methodName);
    this.values.add(value);
    return this;
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link #list()} and {@link #get()}.
   */
  public EntitySelector<T> whereIn(String methodName, Collection<?> values)
  {
    this.methods.add(methodName);
    this.values.add(new WhereInSet(values));
    return this;
  }

  /**
   * Returns the first entity found of the selected class whose values match
   * the specified method-value pairs.
   *
   * @see #where(String, Object)
   */
  public T get()
  {
    if (methods.isEmpty())
    {
      return this.store.list(this.type)
          .stream()
          .findFirst()
          .orElse(null);
    }
    return this.store.get(new FieldIntersection<T>(type, methods, values));
  }

  /**
   * Returns every entity of the selected class whose values match the
   * specified method-value pairs.
   *
   * @see #where(String, Object)
   */
  public List<T> list()
  {
    if (methods.isEmpty())
    {
      return this.store.list(this.type);
    }
    return this.store.list(new FieldIntersection<T>(type, methods, values));
  }
}