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

import java.io.*;
import java.util.*;

import com.techempower.js.*;

/**
 * Writes objects in JavaScript notation.  The mapping from Java types to
 * JavaScript types is as follows, in order of precedence:
 *
 * <ol>
 * <li>null -&gt; null</li>
 * <li>boolean -&gt; boolean</li>
 * <li>byte, short, int, long, float, double -&gt; number</li>
 * <li>String, char -&gt; string</li>
 * <li>Array (Object[], int[][], etc.) -&gt; array</li>
 * <li>custom* -&gt; string, object, or array</li>
 * <li>Map -&gt; object</li>
 * <li>Iterable, Iterator, Enumeration -&gt; array</li>
 * <li>everything else -&gt; string</li>
 * </ol>
 *
 * <p>[*] The rendering of all objects other than null, primitives, strings,
 * and arrays may be customized.  See
 * {@link Builder#addVisitorFactory(Class, VisitorFactory)} and
 * {@link Builder#renderAsStrings(Class)}.</p>
 *
 * <p>Instances of this class are immutable.</p>
 *
 * @see Visitors
 */
public class LegacyJavaScriptWriter
  implements JavaScriptWriter
{
  //
  // Public static utilities.
  //

  /**
   * Returns the standard, default JavaScriptWriter with no custom behavior.
   */
  public static LegacyJavaScriptWriter standard()
  {
    return STANDARD;
  }

  /**
   * Returns a builder for a JavaScriptWriter with custom behavior for certain
   * classes.  See {@link Builder#addVisitorFactory(Class, VisitorFactory)}.
   */
  public static Builder custom()
  {
    return new Builder();
  }

  /**
   * A utility for creating JavaScriptWriter instances with custom visitor
   * factories.
   *
   * <p>Instances of this class are not thread-safe, but the JavaScriptWriter
   * instances returned by {@link #build()} are immutable and thread-safe.</p>
   */
  public static class Builder
  {
    private static final Set<Class<?>> UNCUSTOMIZABLE_TYPES
        = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            boolean.class, byte.class, int.class, short.class,
            long.class, float.class, double.class, void.class,
            Boolean.class, Byte.class, Integer.class, Short.class,
            Long.class, Float.class, Double.class, Void.class,
            String.class)));

    /**
     * Ensures that the rendering of the given type can be customized.
     *
     * @param type The type whose rendering is to be customized.
     * @throws IllegalArgumentException if the rendering of the given type
     *         cannot be customized.
     */
    private static void requireCustomizableType(Class<?> type)
    {
      if (type.isArray() || UNCUSTOMIZABLE_TYPES.contains(type))
      {
        throw new IllegalArgumentException(
            "Rendering of type \"" + type + "\" cannot be customized.");
      }
    }

    private final LinkedHashMap<Class<?>, VisitorFactory<?>> visitorFactories
        = new LinkedHashMap<>();

    private Builder() {}

    /**
     * Adds a visitor factory for the given class.
     *
     * @param type The class to associate with a visitor factory.
     * @param visitorFactory The visitor factory to be used for the class.
     * @return A reference to this builder.
     */
    public <T> Builder addVisitorFactory(Class<? extends T> type,
        VisitorFactory<? extends T> visitorFactory)
    {
      requireNonNull(type, "type");
      requireNonNull(visitorFactory, "visitorFactory");
      requireCustomizableType(type);
      this.visitorFactories.put(type, visitorFactory);
      return this;
    }

    /**
     * Indicates that instances of the given class should be rendered as strings.
     *
     * <p>This method is not useful in most cases.  You can probably ignore it.</p>
     *
     * <p>It is not necessary to call this method unless you want instances of
     * {@code type} to render as strings and (a) a visitor factory has already
     * been assigned for some supertype of {@code type}, or (b) {@code type} is a
     * map, iterable, iterator, or enumeration.</p>
     *
     * @param type The class to be treated as strings.
     * @return A reference to this builder.
     */
    public Builder renderAsStrings(Class<?> type)
    {
      requireNonNull(type, "type");
      requireCustomizableType(type);
      this.visitorFactories.put(type, NullVisitorFactory.INSTANCE);
      return this;
    }

    /**
     * Returns a JavaScriptWriter whose custom visitor factories are specified
     * by this builder.
     */
    public LegacyJavaScriptWriter build()
    {
      return (this.visitorFactories.isEmpty())
          ? STANDARD
          : new LegacyJavaScriptWriter(this.visitorFactories);
    }
  }

  //
  // Private static utilities.
  //

  /**
   * The minimum depth at which the writer will being to look for circular
   * references in the objects being written.
   */
  private static final int CIRCULAR_REFERENCE_MIN_DEPTH = 10;

  /**
   * The singleton instance returned by {@link #standard()}.
   */
  private static final LegacyJavaScriptWriter STANDARD = new LegacyJavaScriptWriter(
      Collections.<Class<?>, VisitorFactory<?>>emptyMap());

  /**
   * A special VisitorFactory implementation that simply indicates that no
   * VisitorFactory should be used for a given class, and so instances of
   * that class should be rendered as strings.
   */
  private enum NullVisitorFactory implements VisitorFactory<Object>
  {
    INSTANCE;

    @Override
    public Visitor visitor(Object object)
    {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Ensures that the given argument is not null.
   *
   * @param argument The argument that may not be null.
   * @param name The name of the argument (for debugging).
   * @throws IllegalArgumentException if {@code argument} is {@code null}.
   */
  private static void requireNonNull(Object argument, String name)
  {
    if (argument == null)
    {
      throw new IllegalArgumentException(
          "Argument \"" + name + "\" may not be null.");
    }
  }

  /**
   * Renders the object as a string, escaped for JavaScript and surrounded by
   * double quotes.
   *
   * @param object The object to be rendered as a string.
   * @param out The target for the rendered string.
   * @throws IOException If an I/O error occurs.
   */
  private static void stringify(Object object, Appendable out) throws IOException
  {
    String text = object.toString();
    out.append('"');
    for (int i = 0, length = text.length(); i < length; i++)
    {
      char c = text.charAt(i);
      switch (c)
      {
        case '\\':
          out.append("\\\\");
          break;
        // These would be caught by the default case because they are control
        // characters, but we prefer this shorter escape syntax to the more
        // verbose unicode syntax.
        case '\t':
          out.append("\\t");
          break;
        case '\b':
          out.append("\\b");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\f':
          out.append("\\f");
          break;
        // These must be escaped for safety within HTML code.
        case '<':
        case '>':
        case '&':
        case '=':
        // The quotes are escaped for safety with HTML, e.g. onclick="here" and
        // onclick='here'.  The \" and \' escape sequences are not recognized in
        // HTML code.
        case '\'':
        case '"':
        // These are the line separator and paragraph separator characters,
        // which JavaScript treats as breaking whitespace, which can mess up
        // the eval function.
        case '\u2028':
        case '\u2029':
          out.append(String.format("\\u%04x", (int) c));
          break;
        default:
          // From RFC 4627:
          //
          // "All Unicode characters may be placed within the quotation marks
          // except for the characters that must be escaped:  quotation mark,
          // reverse solidus, and the control characters (U+0000 through
          // U+001F)."
          //
          // That's where this character range comes from.
          if (c >= '\u0000' && c <= '\u001f')
          {
            out.append(String.format("\\u%04x", (int) c));
          }
          else
          {
            out.append(c);
          }
          break;
      }
    }
    out.append('"');
  }

  //
  // Public non-static utilities.
  //

  /**
   * Writes the object in JavaScript notation.
   *
   * @param object The object to be written in JavaScript notation.
   * @return The object in JavaScript notation.
   * @throws IllegalArgumentException If traversing the object would have
   *                                  resulted in an infinite loop because of a
   *                                  circular reference.
   */
  @Override
  public String write(Object object)
  {
    StringBuilder out = new StringBuilder();
    try
    {
      write(object, out);
    }
    catch (IOException e)
    {
      // StringBuilder does not throw IOExceptions.
      throw new AssertionError(e);
    }
    return out.toString();
  }

  /**
   * Writes the object to the output in JavaScript notation.
   *
   * @param object The object to be written in JavaScript notation.
   * @param out The target for the object in JavaScript notation.
   * @throws IllegalArgumentException If traversing the object would have
   *                                  resulted in an infinite loop because of a
   *                                  circular reference.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void write(Object object, Appendable out) throws IOException
  {
    requireNonNull(out, "out");
    write(object, out, 0, null);
  }

  //
  // Private non-static utilities.
  //

  private final Map<Class<?>, VisitorFactory<?>> visitorFactories;

  // For optimal performance in iterating over the entries of the above map.
  private final List<Map.Entry<Class<?>, VisitorFactory<?>>> visitorFactoryEntries;

  /**
   * Creates a new JavaScriptWriter with the given custom visitor factories.
   * Default visitor factories for some common types such as Map and Iterable
   * will be assigned if they are not already defined in {@code visitorFactories}.
   */
  private LegacyJavaScriptWriter(Map<Class<?>, VisitorFactory<?>> visitorFactories)
  {
    LinkedHashMap<Class<?>, VisitorFactory<?>> vf
        = new LinkedHashMap<>(visitorFactories);
    if (!vf.containsKey(Map.class))
    {
      vf.put(Map.class, Visitors.forMaps());
    }
    if (!vf.containsKey(Iterable.class))
    {
      vf.put(Iterable.class, Visitors.forIterables());
    }
    if (!vf.containsKey(Iterator.class))
    {
      vf.put(Iterator.class, Visitors.forIterators());
    }
    if (!vf.containsKey(Enumeration.class))
    {
      vf.put(Enumeration.class, Visitors.forEnumerations());
    }
    this.visitorFactories = Collections.unmodifiableMap(vf);
    this.visitorFactoryEntries = Collections.unmodifiableList(
        new ArrayList<>(
            this.visitorFactories.entrySet()));
  }

  /**
   * Returns the visitor factory to use for the given object, or
   * {@link NullVisitorFactory#INSTANCE} if this object should be rendered as a
   * string.
   */
  @SuppressWarnings("unchecked") // See the note on generics below.
  private <T> VisitorFactory<T> getVisitorFactory(T object)
  {
    /*
     * Note on generics:
     *
     * We've lost the knowledge of the generic type of the visitor factories at
     * this point, but we know that we have the correct types for every object
     * because that was ensured by the generics of builder.addVisitorFactory.
     * So the unchecked casts below are safe.
     */

    if (object.getClass().isArray())
    {
      return (VisitorFactory<T>) Visitors.forArrays();
    }

    // Give higher precedence to a VisitorFactory that has been attached 
    // to this JSW, superseding a JavaScriptObject-provided VF.
    VisitorFactory<?> visitorFactory = this.visitorFactories.get(object.getClass());
    if (visitorFactory != null)
    {
      return (VisitorFactory<T>) visitorFactory;
    }

    // If the object implements the JavaScriptObject interface, it will have a
    // default VisitorFactory implementation available for us to use here.
    if (object instanceof JavaScriptObject)
    {
      return (VisitorFactory<T>) ((JavaScriptObject) object).getJsVisitorFactory();
    }

    // Check for customizations of this object's supertypes.
    for (Map.Entry<Class<?>, VisitorFactory<?>> entry : this.visitorFactoryEntries)
    {
      if (entry.getKey().isInstance(object))
      {
        return (VisitorFactory<T>)entry.getValue();
      }
    }

    // Render this object as a string.
    return (VisitorFactory<T>) NullVisitorFactory.INSTANCE;
  }

  /**
   * Writes the object to the output in JavaScript notation.
   *
   * @param object The object to be written in JavaScript notation.
   * @param out The target for the object in JavaScript notation.
   * @param depth How many object levels deep we are currently in the hierarchy.
   * @param parents The parent objects of the current object, or {@code null} if
   *                the writer hasn't begun keeping track of parent objects.
   * @throws IllegalArgumentException If traversing the object would have
   *                                  resulted in an infinite loop because of a
   *                                  circular reference.
   * @throws IOException If an I/O error occurs.
   */
  private void write(Object object, Appendable out, int depth,
      Set<Object> parents) throws IOException
  {
    if (object == null
        || object instanceof Byte
        || object instanceof Short
        || object instanceof Integer
        || object instanceof Long
        || object instanceof Float
        || object instanceof Double
        || object instanceof Boolean)
    {
      // These types have the same string representation in JavaScript.
      out.append(String.valueOf(object));
      return;
    }

    if (object instanceof String
        || object instanceof Character)
    {
      stringify(object, out);
      return;
    }

    VisitorFactory<Object> visitorFactory = getVisitorFactory(object);
    if (visitorFactory == NullVisitorFactory.INSTANCE)
    {
      stringify(object, out);
      return;
    }
    
    Set<Object> localParents = parents;

    // Prevent infinite recursion caused by circular references.
    if (depth > CIRCULAR_REFERENCE_MIN_DEPTH)
    {
      if (localParents == null)
      {
      	localParents = Collections.newSetFromMap(
            new IdentityHashMap<Object, Boolean>());
      }
      if (!localParents.add(object))
      {
        throw new IllegalArgumentException(
            "Detected circular reference from object: " + object);
      }
    }

    Visitor visitor = visitorFactory.visitor(object);
    if (visitor.isArray())
    {
      out.append('[');
      if (!visitor.hasNext())
      {
        out.append(']');
        if (localParents != null)
        {
        	localParents.remove(object);
        }
        return;
      }
      visitor.next();
      write(visitor.value(), out, depth + 1, localParents);
      while (visitor.hasNext())
      {
        visitor.next();
        out.append(',');
        write(visitor.value(), out, depth + 1, localParents);
      }
      out.append(']');
      if (localParents != null)
      {
      	localParents.remove(object);
      }
      return;
    }
    else
    {
      out.append('{');
      if (!visitor.hasNext())
      {
        out.append('}');
        if (localParents != null)
        {
        	localParents.remove(object);
        }
        return;
      }
      visitor.next();
      stringify(visitor.name(), out);
      out.append(':');
      write(visitor.value(), out, depth + 1, localParents);
      while (visitor.hasNext())
      {
        visitor.next();
        out.append(',');
        stringify(visitor.name(), out);
        out.append(':');
        write(visitor.value(), out, depth + 1, localParents);
      }
      out.append('}');
      if (localParents != null)
      {
      	localParents.remove(object);
      }
      return;
    }
  }
}
