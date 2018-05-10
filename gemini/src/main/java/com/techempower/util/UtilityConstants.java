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

import java.util.*;

import com.techempower.helper.*;

/**
 * Provides constants that can be useful to any sort of application.  All
 * variables are static final so that they are folded into built
 * applications.
 */
public class UtilityConstants
{
  
  /**
   * The TechEmpower package name.
   */
  public static final String TECHEMPOWER_PACKAGE = "com.techempower";
  
  // ----------------------------------------------------------------------
  // Handy variables.
  // ----------------------------------------------------------------------

  /**
   * Carriage return and line feed.
   */
  public static final String  CRLF = "" + ((char)13) + ((char)10);

  /**
   * Carriage return.
   */
  public static final String  CR = "" + ((char)13);

  /**
   * Line feed.
   */
  public static final String  LF = "" + ((char)10);

  /**
   * The ASCII numeric digits, 0-9.
   */
  public static final String ASCII_DIGITS    = "0123456789";

  /**
   * The ASCII lowercase letters, a-z.
   */
  public static final String ASCII_LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

  /**
   * The ASCII uppercase letters, A-Z.
   */
  public static final String ASCII_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  /**
   * The ASCII symbol characters.
   */
  public static final String ASCII_SYMBOLS   = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

  /**
   * The ASCII space character, {@code " "}.
   */
  public static final String ASCII_SPACE     = " ";

  /**
   * Full-length days of week, starting with Sunday.
   */
  public static final List<String> DAYS_OF_WEEK = Collections.unmodifiableList(
      CollectionHelper.toList(new String[] {
          "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
          "Saturday"
      }));

  /**
   * Abbreviated day names, starting with Sun.
   */
  public static final List<String> DAYS_ABBREVIATED = Collections.unmodifiableList(
      CollectionHelper.toList(new String[] {
          "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
      }));

  /**
   * Full-length month names, starting with January.
   */
  public static final List<String> MONTH_NAMES = Collections.unmodifiableList(
      CollectionHelper.toList(new String[] {
          "January", "February", "March", "April", "May", "June", "July",
          "August", "September", "October", "November", "December"
      }));

  /**
   * Abbreviated month names, starting with Jan.
   */
  public static final List<String> MONTH_NAMES_ABBREVIATED = Collections.unmodifiableList(
      CollectionHelper.toList(new String[] {
          "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
          "Nov", "Dec"
      }));

  /**
   * A Kilobyte, in bytes.
   */
  public static final long KILOBYTE = 1024;
  
  /**
   * A megabyte, in bytes.
   */
  public static final long MEGABYTE = 1024 * KILOBYTE;
  
  /**
   * A gigabyte, in bytes.
   */
  public static final long GIGABYTE = 1024 * MEGABYTE;
  
  /**
   * A terabyte, in bytes.
   */
  public static final long TERABYTE = 1024 * GIGABYTE;
  
  /**
   * A petabyte, in bytes.
   */
  public static final long PETABYTE = 1024 * TERABYTE;
  
  /**
   * A second in milliseconds.
   */
  public static final long SECOND = 1000L;
  
  /**
   * A minute in milliseconds.
   */
  public static final long MINUTE = SECOND * 60L;
  
  /**
   * An hour in milliseconds.
   */
  public static final long HOUR = MINUTE * 60L;
  
  /**
   * A day in milliseconds.
   */
  public static final long DAY = HOUR * 24L;
  
  /**
   * A week in milliseconds.
   */
  public static final long WEEK = DAY * 7L;
  
  /**
   * A 365-day year in milliseconds.  Note that using this value will not
   * be correct for leap years!
   */
  public static final long YEAR = DAY * 365L;
  
  /**
   * The count of nanoseconds in a millisecond, one million.  This is useful
   * for converting between nanotime and System.currentTimeMillis.
   */
  public static final long NANOS_PER_MILLISECOND = 1000000L;

  /**
   * A 70-character line of equals signs to be used as a "divider" line in 
   * plain text output.
   */
  public static final String DIVIDER_DOUBLE 
    = "======================================================================";

  /**
   * A 70-character line of minuses to be used as a "divider" line in plain
   * text output.
   */
  public static final String DIVIDER_SINGLE 
    = "----------------------------------------------------------------------";
  
}   // End UtilityConstants
