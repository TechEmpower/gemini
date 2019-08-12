package com.techempower.cache;

import com.techempower.util.Identifiable;

import java.util.Collection;

/**
 * Identical to {@link WhereChain}, except for {@link MultiEntitySelector}
 */
public class MultiEntityWhereChain<T, S>
{
  private MultiEntitySelector<T> multiEntitySelector;
  private final String methodName;

  MultiEntityWhereChain(MultiEntitySelector<T> multiEntitySelector,
                        String methodName)
  {
    this.multiEntitySelector = multiEntitySelector;
    this.methodName = methodName;
  }

  /**
   * Adds a filter for the given method which can match any of the given values
   * when performing the selection in {@link MultiEntitySelector#get()} and
   * {@link MultiEntitySelector#list()}.
   */
  public final MultiEntitySelector<T> in(Collection<S> values)
  {
    return multiEntitySelector.where(methodName, new WhereInSet(values));
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by {@link MultiEntitySelector#list()} and
   * {@link MultiEntitySelector#get()}.
   */
  public final MultiEntitySelector<T> is(S value)
  {
    return multiEntitySelector.where(methodName, value);
  }
}
