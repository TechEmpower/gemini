package com.techempower.cache;

import java.util.function.Function;

public class MethodValue<T, S>
{
  private final Function<? super T, S> method;
  private final String methodName;
  private final Object value;

  MethodValue(Function<? super T, S> method,
              String methodName,
              Object value)
  {
    this.method = method;
    this.methodName = methodName;
    this.value = value;
  }

  /**
   * Begins a chain call to perform a filter.
   *
   * Note: The <tt>method</tt> parameter does nothing aside from specify the
   * type of the value to match to the compiler. The method name string is
   * what is actually used to determine the method to filter on.
   *
   * @param methodReference - ideally, a method reference for the method to
   *                        call. This isn't (currently) used for anything
   *                        other than static type analysis.
   * @param methodName - the name of the method to call
   * @param <T> - The type of object being filtered
   * @param <S> - The return type of the method being filtered on
   * @return a {@link MethodValueChain} that can be extended
   */
  public static <T, S> MethodValueChain<T, S> of(
      Function<? super T, S> methodReference,
      String methodName)
  {
    return new MethodValueChain<>(methodReference, methodName);
  }

  /**
   * The method being tested against
   */
  protected String getMethodName()
  {
    return methodName;
  }

  /**
   * The value being tested for
   */
  protected Object getValue()
  {
    return value;
  }
}
