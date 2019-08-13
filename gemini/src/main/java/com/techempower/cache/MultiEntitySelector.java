package com.techempower.cache;

import com.techempower.data.EntityGroup;
import com.techempower.util.Identifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is meant to help keep store accesses easy to read, by clearly
 * separating different method-value pairs into different method calls. This is
 * a variant of {@link EntitySelector} that is capable of performing selections
 * on multiple entity groups at once.
 */
public class MultiEntitySelector<T>
{
  private final EntityStore store;
  private final Collection<Class<? extends Identifiable>> types;
  private final List<String> methodNames = new ArrayList<>(4);
  private final List<Object> values = new ArrayList<>(4);

  private MultiEntitySelector(Collection<Class<? extends Identifiable>> types,
                              EntityStore store)
  {

    this.types = types;
    this.store = store;
  }

  static <T extends Identifiable> MultiEntitySelector<T> select(
      Collection<Class<? extends T>> types,
      EntityStore store)
  {
    Collection<Class<? extends Identifiable>> identifiableTypes = types
        .stream()
        .map(type -> (Class<? extends Identifiable>) type)
        .collect(Collectors.toList());
    return new MultiEntitySelector<>(identifiableTypes, store);
  }

  static <T> MultiEntitySelector<T> selectAnySubclass(
      Class<? extends T> type,
      EntityStore store)
  {
    List<Class<? extends Identifiable>> identifiableTypes = store
        .getGroupList()
        .stream()
        .map(EntityGroup::getType)
        .filter(type::isAssignableFrom)
        .collect(Collectors.toList());
    return new MultiEntitySelector<>(identifiableTypes, store);
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link #list()} and {@link #get()}.
   */
  public <S> MultiEntitySelector<T> where(
      MethodValue<? super T, S> methodValue)
  {
    this.methodNames.add(methodValue.getMethodName());
    this.values.add(methodValue.getValue());
    return this;
  }

  /**
   * Returns the first entity found of the selected class whose values match
   * the specified method-value pairs.
   *
   * @see #where(String, Object)
   */
  @SuppressWarnings("unchecked")
  public T get()
  {
    if (methodNames.isEmpty())
    {
      for (Class<? extends Identifiable> type : types)
      {
        Identifiable object = this.store.list(type)
            .stream()
            .findFirst()
            .orElse(null);
        if (object != null)
        {
          return (T) object;
        }
      }
      return null;
    }
    for (Class<? extends Identifiable> type : types)
    {
      T object = (T) this.store.get(
          new FieldIntersection<>(type, methodNames, values));
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
  @SuppressWarnings("unchecked")
  public List<T> list()
  {
    List<T> toReturn = new ArrayList<>();
    if (methodNames.isEmpty())
    {
      for (Class<? extends Identifiable> type : types)
      {
        toReturn.addAll((List<T>) this.store.list(type));
      }
    }
    else
    {
      for (Class<? extends Identifiable> type : types)
      {
        toReturn.addAll((List<T>) this.store.list(
            new FieldIntersection<>(type, methodNames, values)));
      }
    }
    return toReturn;
  }
}