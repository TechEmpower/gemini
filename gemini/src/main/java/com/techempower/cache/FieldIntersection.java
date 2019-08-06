package com.techempower.cache;

import com.techempower.util.Identifiable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents one or more method-value pairs for use in
 * the multi-value selector methods of {@link MethodValueCache}.
 *
 * @param <T> The type of entities managed by this object.
 */
class FieldIntersection<T extends Identifiable>
{
  private final Class<T>     type;
  private final List<String> methodNames;
  private final List<Object> values;

  FieldIntersection(Class<T> type, String methodName, Object value,
                    Object... methodNameThenValuePairs)
  {
    this.type = type;
    if (methodNameThenValuePairs.length % 2 != 0)
    {
      throw new IllegalArgumentException(
          "methodNameThenValuePairs length must be a multiple of 2");
    }

    Iterator<Object> it = Arrays.asList(methodNameThenValuePairs).iterator();
    int numberOfPairs = methodNameThenValuePairs.length / 2 + 1;
    methodNames = new ArrayList<>(numberOfPairs);
    values = new ArrayList<>(numberOfPairs);
    methodNames.add(methodName);
    values.add(value);
    while (it.hasNext())
    {
      methodNames.add(it.next().toString());
      values.add(it.next());
    }
  }

  FieldIntersection(Class<T> type, List<String> methods, List<Object> values)
  {
    this.type = type;
    if (methods.size() != values.size())
    {
      throw new IllegalArgumentException(
          "must have same number of methods and values");
    }
    this.methodNames = methods;
    this.values = values;
  }

  public List<String> getMethodNames()
  {
    return this.methodNames;
  }

  public List<Object> getValues()
  {
    return this.values;
  }

  public Class<T> getType()
  {
    return this.type;
  }

  static class MethodValuePair
  {
    private final String methodName;
    private final Object value;

    MethodValuePair(String methodName, Object value)
    {
      this.methodName = methodName;
      this.value = value;
    }

    public String getMethodName()
    {
      return this.methodName;
    }

    public Object getValue()
    {
      return this.value;
    }
  }
}
