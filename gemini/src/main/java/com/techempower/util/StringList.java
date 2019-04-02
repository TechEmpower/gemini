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

package com.techempower.util;

import java.io.*;
import java.util.*;

import com.techempower.helper.*;

/**
 * Simple class to build up String lists, wherein a separator (say, a comma)
 * exists between each element, but not before the first element and not
 * after the last element.
 *   <p>
 * This structure is not internally thread-safe (much like StringBuilder),
 * so if thread safety is required, synchronization should be applied 
 * externally.
 */
public class StringList
  implements Serializable
{
  private static final long serialVersionUID = -2672100195017838740L;

  private final StringBuilder value = new StringBuilder();

  private final String   separator;
  private final String   prefix;
  private final String   suffix;
  private final String   finalSeparator;
  
  private int            lastInsertionPoint = 0;
  private int            size = 0;

  /**
   * Constructor.  By default, the separator will be a comma (without any
   * spaces).
   */
  public StringList()
  {
    this.separator = ",";
    this.prefix = "";
    this.suffix = "";
    this.finalSeparator = null;
  }

  /**
   * Constructor that takes a separator.
   *
   * @param separator the string to insert between list elements.
   */
  public StringList(String separator)
  {
    this.separator = separator;
    this.prefix = "";
    this.suffix = "";
    this.finalSeparator = null;
  }
  
  /**
   * Constructor that takes a separator and a prefix &amp; suffix to wrap each
   * added value.
   *
   * @param separator the string to insert between list elements.
   * @param prefix A string to prepend to each list element.
   * @param suffix A string to append to each list element.
   */
  public StringList(String separator, String prefix, String suffix)
  {
    this.separator = separator;
    this.suffix = suffix;
    this.prefix = prefix;
    this.finalSeparator = null;
  }

  /**
   * Constructor that takes a separator, a prefix &amp; suffix to wrap each
   * added value, and a unique final separator.  This is useful for the
   * common English practice  of adding an "and" before the last element of
   * a list, such as "X, Y, and Z".
   *   <p>
   * Note that since the last element can change at any time, using a final
   * separator means that any call to toString will necessarily construct
   * a new temporary rendering of the current resulting list including the
   * final separator.
   *
   * @param separator the string to insert between list elements.
   * @param prefix A string to prepend to each list element.
   * @param suffix A string to append to each list element.
   * @param finalSeparator a special final separator to use prior to the
   *   last element.
   */
  public StringList(String separator, String prefix, String suffix, 
    String finalSeparator)
  {
    this.separator = separator;
    this.suffix = suffix;
    this.prefix = prefix;
    this.finalSeparator = finalSeparator;
  }
  
  /**
   * Gets a StringList for use in generating a plain-English list where items
   * are separated by commas and concluded with an "and" prior to the final
   * element. 
   */
  public static StringList getPlainEnglishList()
  {
    return new StringList(", ", "", "", " and ");
  }
  
  /**
   * Gets a StringList for use in generating an Oxford-comma English list 
   * where items are separated by commas and concluded with an "and" prior 
   * to the final element. 
   */
  public static StringList getOxfordEnglishList()
  {
    return new StringList(", ", "", "", ", and ");
  }
  
  /**
   * Gets a StringList for use in generating a plain-English list where items
   * are separated by semi-colons and concluded with an "and" prior to the 
   * final element. 
   */
  public static StringList getSemicolonEnglishList()
  {
    return new StringList("; ", "", "", " and ");
  }
  
  /**
   * Gets a StringList for use in generating an Oxford-semicolon English list
   * where items are separated by semi-colons and concluded with an "and" 
   * prior to the final element. 
   */
  public static StringList getOxfordSemicolonEnglishList()
  {
    return new StringList("; ", "", "", "; and ");
  }
  
  /**
   * Add a list item.  If this is not the first item, a separator is added
   * before concatenating the element, toAdd.
   *
   * @param toAdd the element to add to the String list.
   */
  public StringList add(String toAdd)
  {
    // Add the separator if we're past the first element.
    if (size > 0)
    {
      value.append(separator);
    }
    
    size++;
    
    // Track where we last inserted a value.
    lastInsertionPoint = value.length() - separator.length();
    
    value.append(prefix);
    value.append(toAdd);
    value.append(suffix);
    return this;
  }
  
  /**
   * Adds an integer item.
   */
  public StringList add(int toAdd)
  {
    add(Integer.toString(toAdd));
    return this;
  }
  
  /**
   * Adds a long item.
   */
  public StringList add(long toAdd)
  {
    add(Long.toString(toAdd));
    return this;
  }
  
  /**
   * Adds the element if the value is non-empty.
   * 
   * @see #add(String)
   * @param toAdd the element to add to the String list if its non empty.
   */
  public StringList addNonEmpty(String toAdd)
  {
    if (StringHelper.isNonEmpty(toAdd))
    {
      add(toAdd);
    }
    return this;
  }
  
  /**
   * Adds all of the Strings provided as an array as list elements.
   */
  public StringList addAll(String[] toAdd)
  {
    if (CollectionHelper.isNonEmpty(toAdd))
    {
      for (String stringToAdd : toAdd)
      {
        add(stringToAdd);
      }
    }
    return this;
  }

  /**
   * Adds all of the Strings provided as a Collection as list elements.
   */
  public StringList addAll(Collection<String> toAdd)
  {
    if (CollectionHelper.isNonEmpty(toAdd))
    {
      for (String stringToAdd : toAdd)
      {
        add(stringToAdd);
      }
    }
    return this;
  }
  
  /**
   * Returns the length of the StringList in characters.  This is different than
   * {@link #size()}, which returns the number of strings in the list.
   */
  public int length()
  {
    return value.length()
        + (finalSeparator != null 
            ? finalSeparator.length() - separator.length() 
            : 0);
  }

  /**
   * Get the list as a complete list.
   */
  @Override
  public String toString()
  {
    if ( (finalSeparator != null)
      && (lastInsertionPoint > 0)
      )
    {
      final StringBuilder toReturn = new StringBuilder(value);
      toReturn.delete(lastInsertionPoint, lastInsertionPoint + separator.length());
      toReturn.insert(lastInsertionPoint, finalSeparator);
      return toReturn.toString();
    }
    else
    {
      return value.toString();
    }
  }
  
  /**
   * Returns the number of strings in the list.  This is different than
   * {@link #length()}, which returns the number of characters in the joined
   * list.
   */
  public int size()
  {
    return size;
  }
  
  /**
   * Returns {@code true} if no strings have been added to the list.
   */
  public boolean isEmpty()
  {
    return (size == 0);
  }

}   // End StringList.
