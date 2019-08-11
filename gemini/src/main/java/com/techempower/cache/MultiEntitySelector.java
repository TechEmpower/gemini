package com.techempower.cache;

import com.techempower.util.Identifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * This class is meant to help keep store accesses easy to read, by clearly
 * separating different method-value pairs into different method calls. This is
 * a variant of {@link EntitySelector} that is capable of performing selections
 * on multiple entity groups at once.
 */
public class MultiEntitySelector<T extends Identifiable>
{
  private final EntityStore store;
  private final Collection<Class<? extends T>> types;
  private final List<String> methods = new ArrayList<>(4);
  private final List<Object> values = new ArrayList<>(4);

  MultiEntitySelector(Collection<Class<? extends T>> types, EntityStore store)
  {
    this.types = types;
    this.store = store;
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link #list()} and {@link #get()}.
   */
  MultiEntitySelector<T> where(String methodName, Object value)
  {
    this.methods.add(methodName);
    this.values.add(value);
    return this;
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link #list()} and {@link #get()}.
   */
  public <S> MultiEntitySelector<T> where(Function<? super T, S> method,
                                          String methodName,
                                          S value)
  {
    return where(methodName, value);
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link #list()} and {@link #get()}.
   */
  public <S> MultiEntityWhereChain<T, S> where(Function<? super T, S> method,
                                               String methodName)
  {
    return new MultiEntityWhereChain<>(this, methodName);
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
      for (Class<? extends T> type : types)
      {
        T object = this.store.list(type)
            .stream()
            .findFirst()
            .orElse(null);
        if (object != null)
        {
          return object;
        }
      }
      return null;
    }
    for (Class<? extends T> type : types)
    {
      T object = this.store.get(
          new FieldIntersection<>(type, methods, values));
      if (object != null)
      {
        return object;
      }
    }
    return null;
  }

  /**
   * Returns every entity of the selected class whose values match the
   * specified method-value pairs.
   *
   * @see #where(String, Object)
   */
  public List<T> list()
  {
    List<T> toReturn = new ArrayList<>();
    if (methods.isEmpty())
    {
      for (Class<? extends T> type : types)
      {
        toReturn.addAll(this.store.list(type));
      }
    }
    else
    {
      for (Class<? extends T> type : types)
      {
        toReturn.addAll(this.store.list(
            new FieldIntersection<>(type, methods, values)));
      }
    }
    return toReturn;
  }
}