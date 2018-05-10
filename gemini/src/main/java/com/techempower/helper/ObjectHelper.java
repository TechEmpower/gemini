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
package com.techempower.helper;

import java.util.*;

/**
 * Provides simple utility functionality that works on basic Objects and
 * Comparables.  These functions simply don't really align with any of the
 * other Helper classes.
 */
public final class ObjectHelper
{

  /**
   * Compares the two objects avoiding null pointer exceptions.  If
   * both objects are <tt>null</tt>, this method returns 0.  Otherwise,
   * if a is null, -1 is returned. Otherwise, if b is null, 1 is returned.
   * If neither object is null, the result of <tt>a.compareTo(b)</tt> is
   * returned.
   *
   * @param <T> a class that implements <tt>Comparable&lt;? super T&gt;</tt>,
   *    meaning that instances of the class are either comparable to other
   *    instances of the class (the simple case of <tt>? = T</tt>) or to
   *    instances of some super class of <tt>T</tt>.
   * @param a a comparable object
   * @param b a comparable object
   * @return a negative integer, zero, or a positive integer as
   *    the first object is less than, equal to, or greater than
   *    the second object
   */
  public static <T extends Comparable<? super T>> int compare(T a, T b)
  {
    return a == null ? b == null ? 0 : -1 : b == null ? 1 : a.compareTo(b);
  }

  /**
   * Compares the two objects avoiding null pointer exceptions, using a
   * provided comparator. If both objects are <tt>null</tt>, this method
   * returns 0. Otherwise, if a is null, -1 is returned. Otherwise, if b is
   * null, 1 is returned. If neither object is null, the result of
   * <tt>a.compareTo(b)</tt> is returned.
   *
   * @param <T> a class that is comparable by the given comparator.
   * @param a a comparable object
   * @param b a comparable object
   * @param comparator a comparator to use
   * @return a negative integer, zero, or a positive integer as the first
   * object is less than, equal to, or greater than the second object
   */
  public static <T> int compare(T a, T b, Comparator<T> comparator)
  {
    return a == null ? b == null ? 0 : -1 : b == null ? 1 : comparator.compare(a, b);
  }

  /**
   * You may not instantiate this class.
   */
  private ObjectHelper()
  {
    // Does nothing.
  }
 
}
