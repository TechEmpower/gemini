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

package com.techempower.reflect;

import java.lang.reflect.*;
import java.util.*;

import com.techempower.cache.*;

/**
 * Converts values from one type to another.  Instances of this class can be
 * registered with a {@link EntityStore} in order to support non-standard
 * field types for data entities.  See
 * {@link EntityStore#register(TypeAdapter)}.
 *
 * @param <F> The adapter converts values <em>from</em> this type.
 * @param <T> The adapter converts values <em>to</em> this type.
 */
public abstract class TypeAdapter<F, T>
{
  private final Type type;

  /**
   * Constructs a new type adapter.  The "from" type is extracted from the first
   * generic parameter of the subclass which (probably implicitly) invoked this
   * constructor.
   *
   * @throws RuntimeException if the subclass did not have a generic signature
   */
  protected TypeAdapter()
  {
    Type superclass = getClass().getGenericSuperclass();
    if (!(superclass instanceof ParameterizedType))
    {
      throw new RuntimeException("Missing type parameter.");
    }
    this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
  }

  /**
   * Writes the given input value as its corresponding output value.
   */
  public abstract T write(F value);

  /**
   * Reads the given output value as its corresponding input value.
   */
  public abstract F read(T value);

  /**
   * Returns the type of values to which this adapter applies.
   */
  public Type getType()
  {
    return this.type;
  }

  /**
   * Returns {@code true} if this adapter applies to values of the given type.
   */
  public boolean appliesToType(Type typeToCheck)
  {
    Objects.requireNonNull(typeToCheck);
    return this.type.equals(typeToCheck);
  }

  /**
   * Returns {@code true} if this adapter applies to values stored in the given
   * field.
   */
  public boolean appliesToField(Field field)
  {
    Objects.requireNonNull(field);
    return appliesToType(field.getGenericType());
  }

  /**
   * Returns {@code true} if this adapter applies to values returned by the
   * given get method.
   */
  public boolean appliesToGetMethod(Method getMethod)
  {
    Objects.requireNonNull(getMethod);
    return appliesToType(getMethod.getGenericReturnType());
  }

  /**
   * Returns {@code true} if this adapter applies to values passed in to the
   * given set method.
   */
  public boolean appliesToSetMethod(Method setMethod)
  {
    Objects.requireNonNull(setMethod);
    return appliesToType(setMethod.getGenericParameterTypes()[0]);
  }
}
