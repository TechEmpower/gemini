package com.techempower.cache;

import com.techempower.util.Identifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is meant to help keep store accesses easy to read, by clearly
 * separating different method-value pairs into different method calls.
 *
 * @author ajohnston
 */
public class EntitySelector<T extends Identifiable>
{
  private final EntityStore store;
  private final Class<T>    type;
  private final List<FieldIntersection.MethodValuePair> methodValuePairs = new ArrayList<>(2);

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
    this.methodValuePairs.add(new FieldIntersection.MethodValuePair(methodName,
        value));
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
    if (methodValuePairs.isEmpty())
    {
      return this.store.list(this.type)
          .stream()
          .findFirst()
          .orElse(null);
    }
    return this.store.get(new FieldIntersection<T>(type, methodValuePairs));
  }

  /**
   * Returns every entity of the selected class whose values match the
   * specified method-value pairs.
   *
   * @see #where(String, Object)
   */
  public List<T> list()
  {
    if (methodValuePairs.isEmpty())
    {
      return this.store.list(this.type);
    }
    return this.store.list(new FieldIntersection<T>(type, methodValuePairs));
  }
}