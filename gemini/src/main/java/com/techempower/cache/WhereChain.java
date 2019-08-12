package com.techempower.cache;

import com.techempower.util.Identifiable;

import java.util.Collection;

/**
 * Provides a number of custom where filters, such as {@link #in(Collection)}
 *
 * @param <T> the type of value being selected
 * @param <S> the type of value to match against
 */
public class WhereChain<T extends Identifiable, S>
{
  private EntitySelector<T> entitySelector;
  private final String methodName;

  WhereChain(EntitySelector<T> entitySelector, String methodName)
  {
    this.entitySelector = entitySelector;
    this.methodName = methodName;
  }

  /**
   * Adds a filter for the given method which can match any of the given values
   * when performing the selection in {@link EntitySelector#get()} and
   * {@link EntitySelector#list()}.
   */
  public final EntitySelector<T> in(Collection<S> values)
  {
    return entitySelector.where(methodName, new WhereInSet(values));
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link EntitySelector#list()} and
   * {@link EntitySelector#get()}.
   * <p>
   */
  public final EntitySelector<T> is(S value)
  {
    return entitySelector.where(methodName, value);
  }
}
