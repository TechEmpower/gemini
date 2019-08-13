package com.techempower.cache;

import java.util.Collection;
import java.util.function.Function;

public class MethodValueChain<T, S>
{
  private final Function<? super T, S> methodReference;
  private final String methodName;

  MethodValueChain(Function<? super T, S> methodReference,
                   String methodName)
  {
    this.methodReference = methodReference;
    this.methodName = methodName;
  }

  /**
   * Adds the given method-value pair to the list to use when filtering the
   * objects retrieved by the given entity selector's <code>list</code> and
   * <code>get()</code> methods.
   */
  public final MethodValue<T, S> is(S value)
  {
    return new MethodValue<>(methodReference, methodName, value);
  }

  /**
   * Adds a filter for the given method which can match any of the given values
   * when performing the selection in the given entity selector's
   * <code>list</code> and <code>get()</code> methods.
   */
  public final MethodValue<T, S> in(Collection<S> values)
  {
    return new MethodValue<T, S>(methodReference, methodName,
        new WhereInSet(values));
  }
}
