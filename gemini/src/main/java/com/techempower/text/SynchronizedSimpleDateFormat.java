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

package com.techempower.text;

import java.text.*;
import java.util.*;

/**
 * Extends SimpleDateFormat and provides thread-safe access to the format()
 * and parse() methods.
 *   <p>
 * This class is preferred to the similar (now deprecated) DateFormatter
 * class.
 */
public class SynchronizedSimpleDateFormat
     extends SimpleDateFormat
{
  //
  // Member variables.
  //

  //
  // Member methods.
  //

  private static final long serialVersionUID = -3607754024638244107L;

  /**
   * Constructor.  See SimpleDateFormat's constructor for more details.
   */
  public SynchronizedSimpleDateFormat(String format)
  {
    super(format);
  }

  /**
   * Constructor.  Uses a standard yyyy-MM-dd HH:mm:ss.SSS format.
   */
  public SynchronizedSimpleDateFormat()
  {
    this("yyyy-MM-dd HH:mm:ss.SSS");
  }

  /**
   * Provides a simple way to call format and have a default value returned
   * if the Date being provided is null.
   * 
   * @param date The date to format
   * @param defaultValueWhenNull What to return if the date is null.
   */
  public String format(Date date, String defaultValueWhenNull)
  {
    // Do a null check.
    if (date == null)
    {
      return defaultValueWhenNull;
    }
    
    return format(date);
  }
  
  /**
   * Format a Date using the underlying SimpleDateFormat code.  Note that
   * this method is thread-safe, unlike the normal SimpleDateFormat.format
   * method.
   *
   * @param tz The TimeZone into which the given Date should be converted and formatted.
   */
  public String format(Date date, TimeZone tz)
  {
    synchronized (this)
    {
      // Save the currently set TimeZone.
      TimeZone origTimeZone = this.getTimeZone();
      // Use the default TimeZone if this one is null (should never happen).
      if (origTimeZone == null)
      {
        origTimeZone = TimeZone.getDefault();
      }

      // If a TimeZone is set, then use it.
      if (tz != null)
      {
        this.setTimeZone(tz);
      }

      // Call the super-class version.
      String toReturn = super.format(date);

      // If a TimeZone was set, then switch back to the original.
      if (tz != null)
      {
        this.setTimeZone(origTimeZone);
      }

      return toReturn;
    }
  }

  /**
   * Override format() so that all calls first go to the synchronized version containing
   * the TimeZone param.
   */
  @Override
  public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos)
  {
    return this.format(date, toAppendTo, pos, null);
  }

  /**
   * Format a Date using the underlying SimpleDateFormat code.  Note that
   * this method is thread-safe, unlike the normal SimpleDateFormat.format
   * method.
   *
   * @param tz The TimeZone into which the given Date should be converted and formatted.
   */
  public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos, TimeZone tz)
  {
    synchronized (this)
    {
      // Save the currently set TimeZone.
      TimeZone origTimeZone = this.getTimeZone();
      // Use the default TimeZone if this one is null (should never happen).
      if (origTimeZone == null)
      {
        origTimeZone = TimeZone.getDefault();
      }

      // If a TimeZone is set, then use it.
      if (tz != null)
      {
        this.setTimeZone(tz);
      }

      // Call the super-class version.
      StringBuffer toReturn = super.format(date, toAppendTo, pos);

      // If a TimeZone was set, then switch back to the original.
      if (tz != null)
      {
        this.setTimeZone(origTimeZone);
      }

      return toReturn;
    }
  }

  /**
   * Parses a date string using the underlying SimpleDateFormat code.  Note that
   * this method is thread-safe, unlike the normal SimpleDateFormat.parse
   * method.
   *
   * @param tz The TimeZone which the given source string should be interpreted in.
   */
  public Date parse(String source, TimeZone tz)
    throws ParseException
  {
    //
    // Adapted from method
    // public Date parse(String source) throws ParseException
    // of java.text.DateFormat.java.
    //

    ParsePosition pos = new ParsePosition(0);

    // Call the overloaded version that contains the TimeZone.
    Date result = this.parse(source, pos, tz);

    if (pos.getIndex() == 0)
    {
      throw new ParseException("Unparseable date: \"" + source + "\"", pos.getErrorIndex());
    }

    return result;
  }
  
  /**
   * Parses a date string using the underlying SimpleDateFormat code.  Note that
   * this method is thread-safe, unlike the normal SimpleDateFormat.parse
   * method.
   *
   */
  @Override
  public Date parse(String source)
    throws ParseException
  {
    return this.parse(source, new ParsePosition(0));
  }

  /**
   * Override parse() so that all calls first go to the synchronized version containing
   * the TimeZone param.
   *
   * Parses a date string using the underlying SimpleDateFormat code.  Note that
   * this method is thread-safe, unlike the normal SimpleDateFormat.parse
   * method.
   */
  @Override
  public Date parse(String source, ParsePosition pos)
  {
    return this.parse(source, pos, null);
  }

  /**
   * Parses a date string using the underlying SimpleDateFormat code.  Note that
   * this method is thread-safe, unlike the normal SimpleDateFormat.parse
   * method.
   *
   * @param tz The TimeZone which the given source string should be interpreted in.
   */
  public Date parse(String source, ParsePosition pos, TimeZone tz)
  {
    synchronized (this)
    {
      // Save the currently set TimeZone.
      TimeZone origTimeZone = this.getTimeZone();
      // Use the default TimeZone if this one is null (should never happen).
      if (origTimeZone == null)
      {
        origTimeZone = TimeZone.getDefault();
      }

      // If a TimeZone is set, then use it.
      if (tz != null)
      {
        this.setTimeZone(tz);
      }

      // Call the super-class version.
      Date toReturn = super.parse(source, pos);

      // If a TimeZone was set, then switch back to the original.
      if (tz != null)
      {
        this.setTimeZone(origTimeZone);
      }

      return toReturn;
    }
  }

  @Override
  public String toString()
  {
    return super.toPattern();
  }

} // End SynchronizedSimpleDateFormat.
