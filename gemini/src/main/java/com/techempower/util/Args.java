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

/**
 * Provides utility functions for validating arguments, in most cases throwing
 * IllegalArgumentException when bounds checks are violated.
 */
public final class Args
{

  /**
   * Bound an integer argument by enforcing a minimum and maximum.
   */
  public static void intBound(int argument, String name, int minimum, int maximum)
  {
    if (  (argument < minimum)
       || (argument > maximum)
       )
    {
      throw new IllegalArgumentException(
          name + " must be between " + minimum + " and " + maximum + ".");
    }
  }
 
  /**
   * Bound an integer argument by enforcing a minimum.
   */
  public static void intMin(int argument, String name, int minimum)
  {
    if (argument < minimum)
    {
      throw new IllegalArgumentException(
          name + " must be at least " + minimum + ".");
    }
  }
 
  /**
   * Bound an integer argument by enforcing a maximum.
   */
  public static void intMax(int argument, String name, int maximum)
  {
    if (argument > maximum)
    {
      throw new IllegalArgumentException(
          name + " cannot be greater than " + maximum + ".");
    }
  }

  /**
   * Bound a long argument by enforcing a minimum and maximum.
   */
  public static void longBound(long argument, String name, long minimum, long maximum)
  {
    if (  (argument < minimum)
       || (argument > maximum)
       )
    {
      throw new IllegalArgumentException(
          name + " must be between " + minimum + " and " + maximum + ".");
    }
  }
 
  /**
   * Bound a long argument by enforcing a minimum.
   */
  public static void longMin(long argument, String name, long minimum)
  {
    if (argument < minimum)
    {
      throw new IllegalArgumentException(
          name + " must be at least " + minimum + ".");
    }
  }
 
  /**
   * Bound a long argument by enforcing a maximum.
   */
  public static void longMax(long argument, String name, long maximum)
  {
    if (argument > maximum)
    {
      throw new IllegalArgumentException(
          name + " cannot be greater than " + maximum + ".");
    }
  }
 
  /**
   * You may not instantiate this class.
   */
  private Args()
  {
    // Does nothing.
  }

}
