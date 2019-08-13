package com.techempower.cache;

import com.techempower.util.Identifiable;

import java.util.*;
import java.util.function.Function;

/**
 * This class is meant to help keep store accesses easy to read, by clearly
 * separating different method-value pairs into different method calls.
 * Additionally, by requiring the a function (which should be a method
 * reference) in each of the where methods, static type checking can occur
 * as a sanity check.
 */
public class EntitySelector<T extends Identifiable>
{
  private final EntityStore store;
  private final Class<T> type;
  private final List<String> methodNames = new ArrayList<>(4);
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
  public T get(long id)
  {
    return this.store.get(this.type, id);
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link #list()} and {@link #get()}.
   */
  public <S> EntitySelector<T> where(MethodValue<? super T, S> methodValue)
  {
    this.methodNames.add(methodValue.getMethodName());
    this.values.add(methodValue.getValue());
    return this;
  }

  /**
   * Returns the first entity found of the selected class whose values match
   * the specified method-value pairs.
   *
   * @see #where(Function, String, Object)
   * @see #where(Function, String)
   */
  public T get()
  {
    if (methodNames.isEmpty())
    {
      return this.store.list(this.type)
          .stream()
          .findFirst()
          .orElse(null);
    }
    return this.store.get(new FieldIntersection<>(type, methodNames, values));
  }

  /**
   * Returns every entity of the selected class whose values match the
   * specified method-value pairs.
   *
   * @see #where(Function, String, Object)
   * @see #where(Function, String)
   */
  public List<T> list()
  {
    if (methodNames.isEmpty())
    {
      return this.store.list(this.type);
    }
    return this.store.list(new FieldIntersection<>(type, methodNames, values));
  }
}