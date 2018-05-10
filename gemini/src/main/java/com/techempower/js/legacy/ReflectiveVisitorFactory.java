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

package com.techempower.js.legacy;

import java.lang.reflect.*;
import java.util.*;

import com.techempower.js.*;

/**
 * Makes visitors for objects using reflection.
 *
 * @see LegacyJavaScriptWriter
 * @see Visitor
 * @see VisitorFactory
 */
public class ReflectiveVisitorFactory<T> implements VisitorFactory<T>
{
  private final List<String> names;
  private final List<Method> methods;

  /**
   * Creates a new visitor factory based on the given class, property names, and
   * method names.  The property names and method names should be interleaved.
   * Example:
   *
   * <pre>
   * ReflectiveVisitorFactory f = new ReflectiveVisitorFactory(MyClass.class,
   *     "id", "getId", "date", "getFormattedDate");
   * </pre>
   *
   * If a visitor produced by this factory encounters an exception while
   * invoking methods on an object, it will throw a {@link JavaScriptError}.
   */
  public ReflectiveVisitorFactory(Class<T> clazz, String... namesAndMethods)
  {
    int size = namesAndMethods.length / 2;
    List<String> newNames = new ArrayList<>(size);
    List<Method> newMethods = new ArrayList<>(size);
    for (int i = 0; i < size; i++)
    {
      newNames.add(namesAndMethods[2 * i]);
      String method = namesAndMethods[2 * i + 1];
      try
      {
        newMethods.add(clazz.getMethod(method));
      }
      catch (NoSuchMethodException e)
      {
        throw new IllegalArgumentException("NoSuchMethodException for class "
            + clazz.getName() + " and method " + method + ".", e);
      }
      catch (SecurityException e)
      {
        throw new IllegalArgumentException("SecurityException for class "
            + clazz.getName() + " and method " + method + ".", e);
      }
    }
    this.names = Collections.unmodifiableList(newNames);
    this.methods = Collections.unmodifiableList(newMethods);
  }

  @Override
  public Visitor visitor(T object)
  {
    return new ReflectiveVisitor(object);
  }

  /**
   * The Visitor implementation for reflection.
   */
  final class ReflectiveVisitor implements Visitor
  {
    private final T object;
    private int index = -1;

    private ReflectiveVisitor(T object)
    {
      this.object = object;
    }    

    @Override
    public boolean hasNext()
    {
      return this.index < ReflectiveVisitorFactory.this.names.size() - 1;
    }

    @Override
    public boolean isArray()
    {
      return false;
    }

    @Override
    public String name()
    {
      return ReflectiveVisitorFactory.this.names.get(this.index);
    }

    @Override
    public void next()
    {
      this.index++;
    }

    @Override
    public Object value()
    {
      try
      {
        return ReflectiveVisitorFactory.this.methods.get(this.index)
            .invoke(this.object);
      }
      catch (IllegalAccessException | InvocationTargetException e)
      {
        throw new JavaScriptError("ReflectiveVisitor could not access method.", e);
      }
    }
  }
}
