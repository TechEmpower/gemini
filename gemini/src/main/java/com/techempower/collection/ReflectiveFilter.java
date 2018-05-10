/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.techempower.collection;

import java.lang.reflect.*;
import java.util.*;

import com.google.common.collect.*;
import com.techempower.helper.*;

/**
 * An ObjectFilter implementation that uses reflection to test equality with
 * the value of any field or get method return value. Can be used with either
 * straight equality or range membership to determine correctness. In this
 * implementation strict mode is defined by interpreting methods or fields
 * that are set on the filter but do not exist in the tested object as
 * failing.
 */
public class ReflectiveFilter
{
  private Map<String, Object> filteredFields;
  private Map<String, Object> filteredMethods;

  private boolean                 strict;

  /**
   * Constructor creates a filter with no pre-existing filtering and strict
   * mode set to off.
   */
  public ReflectiveFilter()
  {
    this.reset();
  }

  /**
   * Checks all set fields and methods for equality with the corresponding
   * values stored in the filter. When in strict mode, fields and methods that
   * are not found in the given object are considered fail conditions.
   */
  public boolean allow(Object o)
  {
    return this.filterFields(o) && this.filterMethods(o);
  }

  /**
   * Completely resets the filter to its initial state.
   */
  public void reset()
  {
    this.resetFields();
    this.resetMethods();
    this.setStrict(false);
  }

  /**
   * Removes all test fields from the filter.
   */
  public void resetFields()
  {
    this.filteredFields = null;
  }

  /**
   * Removes all test methods from the filter.
   */
  public void resetMethods()
  {
    this.filteredMethods = null;
  }

  /**
   * Performs all comparisons on a single value and returns whether it passes.
   * In the base implementation support for equality and range testing is
   * provided.
   * 
   * @param value The value to test
   * @param filterValue The value stored in the filter to compare with.
   * @return Whether all tests were passed
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected <V extends Object, F extends Object> boolean allow(V value, F filterValue)
  {
    // if a range was stored then test for membership,
    // otherwise test for equality
    if (filterValue instanceof Range && value instanceof Comparable
        && !((Range)filterValue).contains((Comparable<F>)value))
    {
      return false;
    }
    else if (!filterValue.equals(value))
    {
      return false;
    }

    return true;
  }

  //
  // Fields
  //

  /**
   * Gets the public member field of the given object associated with the
   * given name.
   */
  protected Field getFieldForName(String fieldName, Object o)
      throws NoSuchFieldException
  {
    return o.getClass().getField(fieldName);
  }

  /**
   * Performs equality and range membership tests on the object using the
   * fields that have been defined in the filter.
   * 
   * @param o The Object to be tested
   * @return Whether the object passed all tests
   */
  protected boolean filterFields(Object o)
  {
    if (this.filteredFields == null)
    {
      return true;
    }

    for (Iterator<String> it = this.filteredFields.keySet().iterator(); it.hasNext();)
    {
      if (!filterField(it.next(), o))
      {
        return false;
      }
    }

    return true;
  }

  /**
   * Tests a single field of the object for equality or range membership.
   * 
   * @param fieldName The name of the field to be tested
   * @param o The object to be tested
   * @return Whether the object passed the test
   */
  protected boolean filterField(String fieldName, Object o)
  {
    // There are no fields so return false if in strict mode
    if (this.filteredFields == null)
    {
      return !this.isStrict();
    }

    try
    {
      Field field = o.getClass().getField(fieldName);

      // perform the test
      if (!this.allow(field.get(o), this.filteredFields.get(fieldName)))
      {
        return false;
      }
    }
    catch (IllegalAccessException e)
    {
      // if in strict mode, the test was failed otherwise continue.
      if (this.isStrict())
      {
        return false;
      }
    }
    catch (NoSuchFieldException e)
    {
      // if in strict mode, the test was failed otherwise continue.
      if (this.isStrict())
      {
        return false;
      }
    }

    return true;
  }

  /**
   * Removes the given field from consideration.
   */
  public void removeFilteredField(String fieldName)
  {
    if (this.filteredFields != null)
    {
      this.filteredFields.remove(fieldName);
    }
  }

  /**
   * Sets the given field to be tested for equality with the given value
   */
  public void setFilteredField(String fieldName, Object value)
  {
    if (StringHelper.isEmpty(fieldName))
    {
      return;
    }

    if (this.filteredFields == null)
    {
      this.filteredFields = new HashMap<>();
    }

    this.filteredFields.put(fieldName, value);
  }

  /**
   * Sets the given field to be tested for range membership using the given
   * boundary values. Boundaries, as well as the field, must be Comparable.
   */
  public <T extends Comparable<T>> void setFilteredField(String fieldName,
      T rangeStart, T rangeEnd)
  {
    this.setFilteredField(fieldName, newRange(rangeStart, rangeEnd));
  }

  /**
   * Sets the given field to be tested for range membership using the given
   * range. The field must be Comparable.
   */
  public <T extends Comparable<T>> void setFilteredField(String fieldName,
      Range<T> range)
  {
    this.setFilteredField(fieldName, (Object) range);
  }

  //
  // Methods
  //

  /**
   * Gets the member method of the given object associated with the given
   * name.
   */
  protected Method getMethodForName(String methodName, Object o)
      throws NoSuchMethodException
  {
    return o.getClass().getMethod(methodName, (Class[])null);
  }

  /**
   * Performs equality and range membership tests on the object using the get
   * methods that have been defined in the filter.
   * 
   * @param o The Object to be tested
   * @return Whether the object passed all tests
   */
  protected boolean filterMethods(Object o)
  {
    if (this.filteredMethods == null)
    {
      return true;
    }

    for (Iterator<String> it = this.filteredMethods.keySet().iterator(); it.hasNext();)
    {
      if (!filterMethod(it.next(), o))
      {
        return false;
      }
    }

    return true;
  }

  /**
   * Tests a single get method of the object for equality or range membership.
   * 
   * @param methodName The name of the method to be tested
   * @param o The object to be tested
   * @return Whether the object passed the test
   */
  protected boolean filterMethod(String methodName, Object o)
  {
    // There are no methods so return false if in strict mode
    if (this.filteredMethods == null)
    {
      return !this.isStrict();
    }

    try
    {
      Method method = this.getMethodForName(methodName, o);

      // perform the test
      if (!this.allow(method.invoke(o, new Object[0]),
          this.filteredMethods.get(methodName)))
      {
        return false;
      }
    }
    catch (InvocationTargetException e)
    {
      if (this.isStrict())
      {
        return false;
      }
    }
    catch (IllegalAccessException e)
    {
      if (this.isStrict())
      {
        return false;
      }
    }
    catch (NoSuchMethodException e)
    {
      if (this.isStrict())
      {
        return false;
      }
    }

    return true;
  }

  /**
   * Removes the given get method from consideration.
   */
  public void removeFilteredMethod(String methodName)
  {
    if (this.filteredMethods != null)
    {
      this.filteredMethods.remove(methodName);
    }
  }

  /**
   * Sets the given get method to be tested for equality with the given value
   */
  public void setFilteredMethod(String methodName, Object value)
  {
    if (StringHelper.isEmpty(methodName))
    {
      return;
    }

    if (this.filteredMethods == null)
    {
      this.filteredMethods = new HashMap<>();
    }

    this.filteredMethods.put(methodName, value);
  }

  /**
   * Sets the given get method to be tested for range membership using the
   * given boundary values. Boundaries, as well as the return value of the
   * method, must be Comparable.
   */
  public <T extends Comparable<T>> void setFilteredMethod(String methodName,
      T rangeStart, T rangeEnd)
  {
    this.setFilteredMethod(methodName, newRange(rangeStart, rangeEnd));
  }

  /**
   * Sets the given get method to be tested for range membership using the
   * given range. The return value of the method must be Comparable.
   */
  public <T extends Comparable<T>> void setFilteredMethod(String methodName,
      Range<T> range)
  {
    this.setFilteredMethod(methodName, (Object) range);
  }

  //
  // Strict Mode
  //

  /**
   * Returns whether the filter is in strict mode. Strict mode requires that
   * all fields and methods defined in the filter must exist in the tested
   * object or it will fail.
   */
  public boolean isStrict()
  {
    return this.strict;
  }

  /**
   * Sets whether the filter is in strict mode. Strict mode requires that all
   * fields and methods defined in the filter must exist in the tested object
   * or it will fail.
   */
  public void setStrict(boolean strict)
  {
    this.strict = strict;
  }

  /**
   * Returns a range between the given start and end points.  This mimics the
   * behavior of the now-deleted "ComparableRange" class that existed in a
   * previous version of Gemini.
   */
  private static <T extends Comparable<? super T>> Range<T> newRange(T start, T end)
  {
    if (start == null)
    {
      if (end == null)
      {
        return Range.all();
      }
      return Range.atLeast(start);
    }
    if (end == null)
    {
      return Range.atMost(end);
    }
    return Range.closed(start, end);
  }
}
