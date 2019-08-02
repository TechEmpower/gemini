package com.techempower.cache;

import com.techempower.util.Identifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author ajohnston
 */
class FieldIntersection<T extends Identifiable>
{
  private Class<T>     type;
  private Index<T>     index;
  private List<String> methodNames;
  private List<Object> values;

  FieldIntersection(Class<T> type, String methodName, Object value,
                    Object... methodNameThenValuePairs)
  {
    this.type = type;
    if (methodNameThenValuePairs.length % 2 != 0)
    {
      throw new IllegalArgumentException(
          "methodNameThenValuePairs length must be a multiple of 2");
    }

    List<MethodValuePair> methodValuePairs = new ArrayList<>();
    methodValuePairs.add(new MethodValuePair(methodName, value));
    IntStream.range(0, methodNameThenValuePairs.length / 2)
        .forEach(halfIndex -> methodValuePairs.add(new MethodValuePair(
            methodNameThenValuePairs[halfIndex * 2].toString(),
            methodNameThenValuePairs[halfIndex * 2 + 1])));
    this.index = new Index<>(type, methodValuePairs.stream()
        .map(MethodValuePair::getMethodName)
        .collect(Collectors.toList()));
    this.methodNames = methodValuePairs.stream()
        .map(MethodValuePair::getMethodName)
        .collect(Collectors.toList());
    this.values = methodValuePairs.stream()
        .map(MethodValuePair::getValue)
        .collect(Collectors.toList());
  }

  FieldIntersection(Class<T> type, List<MethodValuePair> methodValuePairs)
  {
    this.type = type;
    initMethodNamesAndValues(methodValuePairs);
    this.index = initIndex();
  }

  private void initMethodNamesAndValues(List<MethodValuePair> methodValuePairs)
  {
    int numberOfPairs = methodValuePairs.size();
    methodNames = Arrays.asList(new String[numberOfPairs]);
    values = Arrays.asList(new Object[numberOfPairs]);
    for (int i = 0; i < numberOfPairs; i++)
    {
      methodNames.set(i, methodValuePairs.get(i).getMethodName());
      values.set(i, methodValuePairs.get(i).getValue());
    }
  }

  private Index<T> initIndex()
  {
    return new Index<>(type, this.methodNames);
  }

  public String getCacheableMethodName()
  {
    return this.getIndex()
        .getCacheableMethodName();
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

  public Index<T> getIndex()
  {
    return this.index;
  }

  public static class Index<T extends Identifiable>
  {
    private final Class<T>     type;
    private final List<String> methodNames;
    private final String       cacheableMethodName;

    public Index(Class<T> type, List<String> methodNames)
    {
      this.type = type;
      this.methodNames = methodNames;
      this.cacheableMethodName = String.join("|", methodNames);
    }

    public Class<T> getType()
    {
      return this.type;
    }

    public List<String> getMethodNames()
    {
      return this.methodNames;
    }

    public String getCacheableMethodName()
    {
      return this.cacheableMethodName;
    }

    @Override
    public boolean equals(Object o)
    {
      return this == o ||
          o != null &&
              this.getClass() == o.getClass() &&
              this.getType()
                  .equals(((Index<?>)o).getType()) &&
              this.getCacheableMethodName()
                  .equals(((Index<?>)o).getCacheableMethodName());

    }

    @Override
    public int hashCode()
    {
      return this.getCacheableMethodName()
          .hashCode();
    }
  }

  static class MethodValuePair
      implements Comparable<MethodValuePair>
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

    @Override
    public int compareTo(MethodValuePair methodValuePair)
    {
      return this.methodName.compareTo(methodValuePair.methodName);
    }
  }
}
