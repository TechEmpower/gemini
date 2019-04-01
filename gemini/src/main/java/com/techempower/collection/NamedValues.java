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

import java.util.*;

/**
 * Data is often encoded as a map of String names to String values, such as 
 * HTTP requests or the contents of a properties file.  This interface defines
 * simple utility methods that may be exposed by components that work with 
 * underlying String-to-String maps: methods for fetching values from the map
 * as Strings, ints, long integers, and so on.
 *   <p>
 * This interface exposes no mutator methods, thereby assuming the underlying
 * data structure may be immutable.  However, a subinterface named
 * MutableNamedValues extends this interface with optional mutators.
 *   <p>
 * The BasicNamedValues implementation uses a backing {@code Map<String,String>} .
 */
public interface NamedValues
{

  /**
   * Is a value associated with a name?
   */
  boolean has(String name);
  
  /**
   * Gets a set of all available names (keys).
   */
  Set<String> names();
  
  /**
   * Gets a value by name, returning null if it's not found.
   */
  String get(String name);
  
  /**
   * Gets a value by name, returning a provided default value if the named
   * value is not found.
   */
  String get(String name, String defaultValue);
  
  /**
   * Gets a value by name as an integer, returning 0 if the named value is
   * not found.
   */
  int getInt(String name);
  
  /**
   * Gets a value by name as an integer, returning a provided default value
   * if the named value is not found. 
   */
  int getInt(String name, int defaultValue);
  
  /**
   * Gets a value by name as an integer, returning a provided default value
   * if the named value is not found.  The returned value will be bounded by
   * the provided minimum and maximum values.
   */
  int getInt(String name, int defaultValue, int minimum, int maximum);
  
  /**
   * Gets a value by name as a long integer, returning 0L if the named value
   * is not found.
   */
  long getLong(String name);
  
  /**
   * Gets a value by name as a long integer, returning a provided default 
   * value if the named value is not found. 
   */
  long getLong(String name, long defaultValue);
  
  /**
   * Gets a value by name as a long integer, returning a provided default 
   * value if the named value is not found.  The returned value will be 
   * bounded by the provided minimum and maximum values.
   */
  long getLong(String name, long defaultValue, long minimum, long maximum);
  
  /**
   * Gets a boolean value by name, returning false if the named value is not
   * found.
   */
  boolean getBoolean(String name);
  
  /**
   * Gets a boolean value by name, returning the default value if the named
   * value is not found or cannot be parsed.
   */
  boolean getBoolean(String name, boolean defaultValue);
  
}
