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
   * when performing the selection in {@link EntitySelector#get()} or
   * {@link EntitySelector#list()}.
   */
  public final EntitySelector<T> in(Collection<S> values)
  {
    return entitySelector.where(methodName, new WhereInSet(values));
  }
}
