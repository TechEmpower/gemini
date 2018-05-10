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

import com.techempower.util.*;

/**
 * NumberHelper provides helper functions for working with numbers and simple
 * mathematics.  Note that currency-specific helper functions are in 
 * CurrencyHelper.
 * 
 * @see CurrencyHelper
 */
public final class NumberHelper
{
  //
  // Static methods.
  //

  /**
   * Force a provided integer to be bounded by a minimum and maximum.  If the
   * number is lower than the minimum, it will be set to the minimum; if it
   * is higher than the maximum, it will be set to the maximum.
   */
  public static int boundInteger(int toBound, IntRange range)
  {
    return boundInteger(toBound, range.min, range.max);
  }
  
  /**
   * Force a provided integer to be bounded by a minimum and maximum.  If the
   * number is lower than the minimum, it will be set to the minimum; if it
   * is higher than the maximum, it will be set to the maximum.
   */
  public static int boundInteger(int toBound, int minimum, int maximum)
  {
    int result = toBound;
    if (result < minimum)
    {
      result = minimum;
    }
    if (result > maximum)
    {
      result = maximum;
    }
    
    return result;
  }
  
  /**
   * Force a provided long to be bounded by a minimum and maximum.  If the
   * number is lower than the minimum, it will be set to the minimum; if it
   * is higher than the maximum, it will be set to the maximum.
   */
  public static long boundLong(long toBound, LongRange range)
  {
    return boundLong(toBound, range.min, range.max);
  }

  /**
   * Force a provided long to be bounded by a minimum and maximum.  If the
   * number is lower than the minimum, it will be set to the minimum; if it
   * is higher than the maximum, it will be set to the maximum.
   */
  public static long boundLong(long toBound, long minimum, long maximum)
  {
    long result = toBound;
    if (result < minimum)
    {
      result = minimum;
    }
    if (result > maximum)
    {
      result = maximum;
    }
    
    return result;
  }
  
  /**
   * Parses a String representation of a standard integer into a primitive 
   * base-10 int.  In the event of a parsing problem, the default value will
   * be returned.  We avoid throwing an exception since improper String
   * representations into numbers are very common (i.e., improperly formatted
   * numbers are not "exceptional.")
   *   <p>
   * This is inspired by a similar method in Guava.
   */
  public static int parseInt(final String string, final int defaultValue) 
  {
    // If the string is null or empty, return default.
    if (StringHelper.isEmpty(string)) 
    {
      return defaultValue;
    }
    
    // Check for a sign.
    final boolean negative = string.charAt(0) == '-';
    final int length = string.length();
    
    int index = negative ? 1 : 0;

    // Only a sign?  Default value.
    if (index == length) 
    {
      return defaultValue;
    }
    
    // Compute digit by removing '0' from the character.
    int digit = string.charAt(index++) - '0';

    // Not a sign or digit?  Default value.
    if (  (digit < 0)
       || (digit > 9)
       )
    {
      return defaultValue;
    }
    
    int accumulator = -digit;
    int cap = Integer.MIN_VALUE / 10;

    while (index < length) 
    {
      digit = string.charAt(index++) - '0';
      
      // Check for impending overflow.  If so, default value.
      if (  (digit < 0)
         || (digit > 9) 
         || (accumulator < cap)
         )
      {
        return defaultValue;
      }
      
      // "Shift" the collected digits over to the left.
      accumulator *= 10;
      
      // Check for overflow.  Default value.
      if (accumulator < Integer.MIN_VALUE + digit) 
      {
        return defaultValue;
      }
      
      // Apply the digit.
      accumulator -= digit;
    }

    // Apply the sign and return.
    if (negative) 
    {
      return accumulator;
    } 
    // If the accumulator is at the minimum value but the sign is positive,
    // that's the edge overflow case (positive range is one smaller than
    // negative range).  Return default value.
    else if (accumulator == Integer.MIN_VALUE) 
    {
      return defaultValue;
    }
    else
    {
      return -accumulator;
    }
  }
    
  /**
   * A simplified version of parseInt (see above) that uses 0 as its default
   * value if the String is not an integer or empty.
   *   
   * @param string the String to parse.  If null or empty, the default value
   *   will be returned.
   */
  public static int parseInt(final String string)
  {
    return parseInt(string, 0);
  }
  
  /**
   * Variation of parseInt that will bound the resulting parsed integer by a
   * minimum and maximum.
   *   
   * @param string the String to parse.  If null or empty, the default value
   *   will be returned.
   * @param defaultValue a default int value to return is the string parameter
   *   does not parse correctly.
   * @param minimum A minimum boundary to enforce.
   * @param maximum A maximum boundary to enforce.
   */
  public static int parseInt(final String string, final int defaultValue,
      final int minimum, final int maximum)
  {
    return boundInteger(parseInt(string, defaultValue), minimum, maximum);
  }
  
  /**
   * Parses a String representation of a long integer into a primitive 
   * base-10 long.  In the event of a parsing problem, the default value will
   * be returned.  We avoid throwing an exception since improper String
   * representations into numbers are very common (i.e., improperly formatted
   * numbers are not "exceptional.")
   *   <p>
   * This is inspired by a similar method in Guava.
   */
  public static long parseLong(final String string, final long defaultValue) 
  {
    // If the string is null or empty, return default.
    if (StringHelper.isEmpty(string)) 
    {
      return defaultValue;
    }
    
    // Check for a sign.
    final boolean negative = string.charAt(0) == '-';
    final int length = string.length();
    
    int index = negative ? 1 : 0;

    // Only a sign?  Default value.
    if (index == length) 
    {
      return defaultValue;
    }
    
    // Compute digit by removing '0' from the character.
    int digit = string.charAt(index++) - '0';

    // Not a sign or digit?  Default value.
    if (  (digit < 0)
       || (digit > 9)
       )
    {
      return defaultValue;
    }
    
    long accumulator = -digit;
    long cap = Long.MIN_VALUE / 10;

    while (index < length) 
    {
      digit = string.charAt(index++) - '0';
      
      // Check for impending overflow.  If so, default value.
      if (  (digit < 0)
         || (digit > 9) 
         || (accumulator < cap)
         )
      {
        return defaultValue;
      }
      
      // "Shift" the collected digits over to the left.
      accumulator *= 10;
      
      // Check for overflow.  Default value.
      if (accumulator < Long.MIN_VALUE + digit) 
      {
        return defaultValue;
      }
      
      // Apply the digit.
      accumulator -= digit;
    }

    // Apply the sign and return.
    if (negative) 
    {
      return accumulator;
    } 
    // If the accumulator is at the minimum value but the sign is positive,
    // that's the edge overflow case (positive range is one smaller than
    // negative range).  Return default value.
    else if (accumulator == Long.MIN_VALUE) 
    {
      return defaultValue;
    }
    else
    {
      return -accumulator;
    }
  }

  /**
   * A simplified version of parseLong (see above) that uses 0 as its default
   * value if the String is not a long or empty.
   *   
   * @param string the String to parse.  If null or empty, the default value
   *   will be returned.
   */
  public static long parseLong(final String string)
  {
    return parseLong(string, 0);
  }
  
  /**
   * Variation of parseLong that will bound the resulting parsed long by a
   * minimum and maximum.
   *   
   * @param string the String to parse.  If null or empty, the default value
   *   will be returned.
   * @param defaultValue a default long value to return is the string 
   *   parameter does not parse correctly.
   * @param minimum A minimum boundary to enforce.
   * @param maximum A maximum boundary to enforce.
   */
  public static long parseLong(final String string, final long defaultValue,
      final long minimum, final long maximum)
  {
    return boundLong(parseLong(string, defaultValue), minimum, maximum);
  }

  /**
   * Returns true if the given string represents a number, false otherwise.
   * This uses Integer.parseInt and relies on an exception being thrown to
   * indicate that the String is not a number.  We should eventually re-write
   * this method to not rely on an exception for a non-exceptional case.
   *   <p>
   * This method was formerly in StringHelper.
   */
  public static boolean isNumber(final String string)
  {
    try
    {
      Integer.parseInt(string);
      return true;
    }
    catch (NumberFormatException nfe)
    {
      return false;
    }
  }  // End isNumber().

  /**
   * Does it's best to make an int out of what you give it. Uses
   * Double.parseDouble() and casts the result to an int.
   * If an Exception occurs then defaultValue is returned.
   *   <p>
   * This method was formerly in StringHelper.
   */
  public static int parseIntPermissive(String numStr, int defaultValue)
  {
    if (numStr != null)
    {
      try
      {
        return (int)Double.parseDouble(numStr);
      }
      catch (NumberFormatException nfe)
      {
        // Do nothing.
      }
    }
    return defaultValue;
  }

  /**
   * A pass-through to Float.parseFloat().
   * If a NumberFormatException occurs then defaultValue is returned.
   *   <p>
   * This method was formerly in StringHelper.
   */
  public static float parseFloat(String numStr, float defaultValue)
  {
    if (numStr != null)
    {
      try
      {
        return Float.parseFloat(numStr);
      }
      catch (NumberFormatException nfe)
      {
        // Do nothing.
      }
    }
    return defaultValue;
  }
  
  /**
   * A pass-through to Double.parseDouble().
   * If a NumberFormatException occurs then defaultValue is returned. 
   */
  public static double parseDouble(String numStr, double defaultValue)
  {
    if (numStr != null)
    {
      try
      {
        return Double.parseDouble(numStr);
      }
      catch (NumberFormatException nfe)
      {
        // Do nothing.
      }
    }
    return defaultValue;
  }

  /**
   * A simple method to round a double to x number of decimal places.
   */
  public static double round(double value, int decimalPlaces)
  {
    if (decimalPlaces < 0)
    {
      throw new IllegalArgumentException(
          "decimalPlaces can not be less than 0: " + decimalPlaces);
    }

    return java.math.BigDecimal.valueOf(value).setScale(decimalPlaces, 
        java.math.BigDecimal.ROUND_HALF_UP).doubleValue();
  }

  /**
   * A helper method that translates a page number into a zero-based index.
   * Returns the index offset for the start of the given page into a full list
   * using the given pageSize. This version does not care about the overall size.
   *
   * @param page A page number starting at 1
   * @param pageSize The size of a page
   *
   * @return The zero based starting offset index to use
   */
  public static int getPageStartOffset(int page, int pageSize)
  {
    return getPageStartOffset(page, pageSize, -1);
  }

  /**
   * A helper method that translates a page number into a zero-based index.
   * Returns the index offset for the start of the given page into a full list
   * of size given by maximum using the given pageSize.
   *
   * @param page A page number starting at 1
   * @param pageSize The size of a page
   * @param maximum The total count of items in the list
   *
   * @return The zero based starting offset index to use
   */
  public static int getPageStartOffset(int page, int pageSize, int maximum)
  {
    int boundedPage = boundInteger(page, 1, Integer.MAX_VALUE);

    int offset = (boundedPage - 1) * pageSize;

    if (maximum > 0 && offset > maximum)
    {
      offset = 0;
    }

    return offset;
  }

  /**
   * A helper method that translates a page number into a zero-based index.
   * Returns the index offset for the end of the given page into a full list
   * of size given by maximum using the given pageSize.
   *   <p>
   * The end index will be 1 greater than the last element to include in a 
   * page.
   *
   * @param page A page number starting at 1
   * @param pageSize The size of a page
   * @param maximum The total count of items in the list
   *
   * @return The zero based ending offset index to use
   */
  public static int getPageEndOffset(int page, int pageSize, int maximum)
  {
    int offset = getPageStartOffset(page, pageSize, maximum) + pageSize;

    if (offset > maximum)
    {
      offset = maximum;
    }

    return offset;
  }
  
  /**
   * Gets the number of pages given a count of items.
   */
  public static int getPageCount(int items, int pageSize)
  {
    return (items / pageSize) + (items % pageSize > 0 ? 1 : 0);
  }

  /**
   * Gets the median of the unsorted list of integers. If {@code values}
   * is null or empty, null is returned.
   *
   * @return A Double representation of the median calculated using the
   * provided list. Null if unable to calculate.
   */
  public static Double getMedian(List<Integer> values)
  {
    if (CollectionHelper.isNonEmpty(values))
    {
      Collections.sort(values);
      if (values.size() % 2 == 1)
      {
        // Middle value
        // Subtract one due to zero-based indexing
        return Double.valueOf(values.get(((values.size() + 1) / 2) - 1));
      }
      else
      {
        // Average of the 2 sharing the middle
        // Subtract one due to zero-based indexing
        int value1 = values.get((values.size() / 2) - 1);
        int value2 = values.get(((values.size() / 2) + 1) - 1);
        return (value1 + value2) / 2.0;
      }
    }

    return null;
  }

  /**
   * To address FindBugs rule FE_FLOATING_POINT_EQUALITY. Because floating
   * point calculations may involve rounding, calculated float and double
   * values may not be accurate. Instead we compare for equality within the
   * range of: ( Math.abs(a - b) < 0.0000001d ).
   *
   * @param a value to compare
   * @param b value to compare
   * @return whether a and b are close enough to be considered equal
   */
  public static boolean almostEquals(double a, double b)
  {
    return almostEquals(a, b, 0.0000001d);
  }

  /**
   * To address FindBugs rule FE_FLOATING_POINT_EQUALITY. Because floating
   * point calculations may involve rounding, calculated float and double
   * values may not be accurate. Instead we compare for equality within the
   * range of: ( Math.abs(a - b) < epsilon ).
   *
   * @param a value to compare
   * @param b value to compare
   * @param epsilon how close a and b need to be to be considered equal
   * @return whether a and b are close enough to be considered equal
   */
  public static boolean almostEquals(double a, double b, double epsilon)
  {
    return Math.abs(a - b) < epsilon;
  }

  /**
   * You may not instantiate this class.
   */
  private NumberHelper()
  {
    // Does nothing.
  }

}  // End NumberHelper.

