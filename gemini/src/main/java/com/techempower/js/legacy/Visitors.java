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

import com.techempower.js.*;

/**
 * Provides useful {@link Visitor} and {@link VisitorFactory} implementations
 * for use with a {@link LegacyJavaScriptWriter}.
 *
 * <p>As an example, imagine you want the application to render {@code User}
 * objects in JSON like so:</p>
 *
 * <pre>
 *   {
 *      "id": &lt;the user's unique id&gt;,
 *      "email": &lt;the user's email address&gt;
 *   }
 * </pre>
 *
 * <p>The object responsible for translating {@code User}s to that structure is
 * a {@code VisitorFactory<User>}.  Here are a few different ways of creating
 * that visitor factory:</p>
 *
 * <ol>
 *   <li>
 *     Using a reflection-based approach.  This works when all of the properties
 *     in the output exist as getter fields on the User class.
 *     <pre>
 *   VisitorFactory&lt;User&gt; userVisitors = Visitors.forClass(
 *       User.class,
 *       "id", "getId",
 *       "email", "getUserEmail");
 *     </pre>
 *   </li>
 *   <li>
 *     Using the Visitors.map method.  This is more flexible than the
 *     reflection-based approach because not every property must exist as a
 *     getter field on the User class.  It also avoids having method names
 *     as strings in your code.
 *     <pre>
 *   VisitorFactory&lt;User&gt; userVisitors = new VisitorFactory&lt;User&gt;() {
 *     public Visitor visitor(User user)
 *     {
 *       return Visitors.map(
 *           "id", user.getId(),
 *           "email", user.getUserEmail());
 *     }
 *   };
 *     </pre>
 *   </li>
 *   <li>
 *     Converting the users into one of the standard collection types--map,
 *     array, list, etc.--and using the predefined visitor factory for that
 *     collection type.  (In this particular case, the Visitors.map approach
 *     would be less verbose and more efficient.)
 *     <pre>
 *   VisitorFactory&lt;User&gt; userVisitors = new VisitorFactory&lt;User&gt;() {
 *     public Visitor visitor(User user)
 *     {
 *       Map&lt;String, Object&gt; map = new HashMap&lt;String, Object&gt;();
 *       map.put("id", user.getId());
 *       map.put("email", user.getUserEmail());
 *       return Visitors.forMaps().visitor(map);
 *     }
 *   };
 *     </pre>
 *   </li>
 * </ol>
 */
public final class Visitors
{
  private Visitors() {}

  /**
   * Produces visitors for arrays.
   */
  public static VisitorFactory<Object> forArrays()
  {
    return ArrayVisitorFactory.INSTANCE;
  }

  /**
   * Produces visitors for enumerations.
   */
  public static VisitorFactory<Object> forEnumerations()
  {
    return EnumerationVisitorFactory.INSTANCE;
  }

  /**
   * Produces visitors for iterators.
   */
  public static VisitorFactory<Object> forIterators()
  {
    return IteratorVisitorFactory.INSTANCE;
  }

  /**
   * Produces visitors for iterables.
   */
  public static VisitorFactory<Object> forIterables()
  {
    return IterableVisitorFactory.INSTANCE;
  }

  /**
   * Produces visitors for maps.
   */
  public static VisitorFactory<Object> forMaps()
  {
    return MapVisitorFactory.INSTANCE;
  }

  /**
   * Creates a new visitor factory based on the given class, property names, and
   * method names.  The property names and method names should be interleaved.
   * Example:
   *
   * <pre>
   * VisitorFactory factory = Visitors.forClass(MyClass.class,
   *     "id", "getId", "date", "getFormattedDate");
   * </pre>
   *
   * If a visitor produced by this factory encounters exceptions while
   * invoking methods on an object, it will throw {@link JavaScriptError}.
   */
  public static <T> VisitorFactory<T> forClass(Class<T> clazz,
      String... namesAndMethods)
  {
    return new ReflectiveVisitorFactory<>(clazz, namesAndMethods);
  }

  /**
   * Returns a visitor for the provided interleaved names and values.
   *
   * <p>Example usage:</p>
   * <pre>   {@code}
   *   private static final JavaScriptWriter JS_WRITER = JavaScriptWriter
   *       .custom()
   *       .addVisitorFactory(
   *           User.class,
   *           new VisitorFactory<User>() {
   *             public Visitor visitor(User user) {
   *               return Visitors.map(
   *                   "id", user.getId(),
   *                   "email", user.getEmail());
   *             }
   *           })
   *       .build();
   *
   *   // Elsewhere in a handler method:
   *   return GeminiHelper.sendJson(
   *       context, "users", cache.list(User.class), JS_WRITER);
   *
   *   // Renders a JSON response that looks something like:
   *   // {
   *   //   "users": [
   *   //     { "id": 5, "email": "bob@example.com" },
   *   //     { "id": 7, "email": "alice@example.com" },
   *   //     ...
   *   //   ]
   *   // }
   * }</pre>
   */
  public static Visitor map(Object... namesAndValues)
  {
    if (namesAndValues == null)
    {
      throw new IllegalArgumentException(
          "Argument 'namesAndValues' must not be null.");
    }
    if (namesAndValues.length % 2 != 0)
    {
      throw new IllegalArgumentException(
          "Argument 'namesAndValues' must have an even number of elements.");
    }
    return new ArrayBackedMapVisitor(namesAndValues);
  }

  /**
   * Implementation for {@link #map(Object...)}.
   */
  private static final class ArrayBackedMapVisitor implements Visitor
  {
    private final Object[] namesAndValues;
    private int index = -2;

    ArrayBackedMapVisitor(Object[] namesAndValues)
    {
      this.namesAndValues = namesAndValues;
    }

    @Override
    public boolean hasNext()
    {
      return this.index < this.namesAndValues.length - 2;
    }

    @Override
    public boolean isArray()
    {
      return false;
    }

    @Override
    public String name()
    {
      if (this.index < 0)
      {
        throw new IllegalStateException("name() called before next()");
      }
      return String.valueOf(this.namesAndValues[this.index]);
    }

    @Override
    public void next()
    {
      if (!hasNext())
      {
        throw new IllegalStateException(
            "next() called when hasNext() was false");
      }
      this.index += 2;
    }

    @Override
    public Object value()
    {
      if (this.index < 0)
      {
        throw new IllegalStateException("value() called before next()");
      }
      return this.namesAndValues[this.index + 1];
    }
  }
}
