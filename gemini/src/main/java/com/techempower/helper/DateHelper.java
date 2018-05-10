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

import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.techempower.text.*;
import com.techempower.util.*;

/**
 * DateHelper provides utility functionality for working with dates in
 * the general sense and the Java Date and Calendar classes specifically.  It
 * consists of code that was originally in the BasicHelper class.
 */
public final class DateHelper
{

  //
  // Static variables.
  //

  // Date formats.
  private static final List<SimpleDateFormat>      DEFAULT_DATE_FORMATTERS       = new ArrayList<>();
  public static final SynchronizedSimpleDateFormat SHORT_US_DATE_FORMAT          = new SynchronizedSimpleDateFormat("MM/dd/yy");
  public static final SynchronizedSimpleDateFormat STANDARD_US_DATE_FORMAT       = new SynchronizedSimpleDateFormat("MM/dd/yyyy");
  public static final SynchronizedSimpleDateFormat STANDARD_UK_DATE_FORMAT       = new SynchronizedSimpleDateFormat("dd MMM yyyy");
  public static final SynchronizedSimpleDateFormat STANDARD_FULL_DATE_FORMAT     = new SynchronizedSimpleDateFormat("MM/dd/yyyy HH:mm:ss");
  public static final SynchronizedSimpleDateFormat STANDARD_FULL_DATE_FORMAT_12  = new SynchronizedSimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
  public static final SynchronizedSimpleDateFormat STANDARD_FULL_DATE_FORMAT_UK  = new SynchronizedSimpleDateFormat("dd MMM yyyy hh:mm:ss aa");
  public static final SynchronizedSimpleDateFormat STANDARD_SQL_FORMAT           = new SynchronizedSimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  public static final SynchronizedSimpleDateFormat STANDARD_TECH_FORMAT          = new SynchronizedSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static final SynchronizedSimpleDateFormat STANDARD_TECH_FORMAT_DATEONLY = new SynchronizedSimpleDateFormat("yyyy-MM-dd");
  public static final SynchronizedSimpleDateFormat STANDARD_TECH_FORMAT_12       = new SynchronizedSimpleDateFormat("yyyy-MM-dd hh:mm:ss aa");
  public static final SynchronizedSimpleDateFormat STANDARD_TECH_FORMAT_WITHDAY  = new SynchronizedSimpleDateFormat("EEE yyyy-MM-dd");
  public static final SynchronizedSimpleDateFormat ISO_8601_DATE_ONLY            = new SynchronizedSimpleDateFormat("yyyy-MM-dd");
  public static final SynchronizedSimpleDateFormat ISO_8601_LOCAL                = new SynchronizedSimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
  public static final SynchronizedSimpleDateFormat ISO_8601_FULL                 = new SynchronizedSimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");
  public static final SynchronizedSimpleDateFormat ISO_8601_COMPACT              = new SynchronizedSimpleDateFormat("yyyyMMdd'T'hhmmss.SSS");
  public static final SynchronizedSimpleDateFormat STANDARD_FILENAME_FORMAT      = new SynchronizedSimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
  public static final SynchronizedSimpleDateFormat VERBOSE_TECH_FORMAT           = new SynchronizedSimpleDateFormat("EEE yyyy-MM-dd HH:mm:ss");
  public static final SynchronizedSimpleDateFormat VERBOSE_TECH_FORMAT_12        = new SynchronizedSimpleDateFormat("EEE yyyy-MM-dd hh:mm:ss aa");

  // Timezones

  /**
   * A simplified list of Time Zones (along with human-friendly descriptions
   * in English) for presenting to users.  This list includes all full-hour
   * GMT offsets and the mainstream United States zones, sorted from UTC-12
   * to UTC+14. 
   */
  private static final TimeZoneDescriptor[] SIMPLE_TIME_ZONES = new TimeZoneDescriptor[]
  {
    new TimeZoneDescriptor(0, "America/Los_Angeles", "Pacific Time (United States)"),
    new TimeZoneDescriptor(1, "America/Denver", "Mountain Time (United States)"),
    new TimeZoneDescriptor(2, "America/Chicago", "Central Time (United States)"),
    new TimeZoneDescriptor(3, "America/New_York", "Eastern Time (United States)"),
    new TimeZoneDescriptor(4, "GMT-1200", "UTC-12: Baker Island, Howland Island"),
    new TimeZoneDescriptor(5, "GMT-1100", "UTC-11: American Samoa, Samoa"),
    new TimeZoneDescriptor(6, "GMT-1000", "UTC-10: Hawaii, Papeete"),
    new TimeZoneDescriptor(7, "GMT-0900", "UTC-09: Anchorage, Fairbanks, Juneau"),
    new TimeZoneDescriptor(8, "GMT-0800", "UTC-08: California, Las Vegas, Portland, Washington state"),
    new TimeZoneDescriptor(9, "GMT-0700", "UTC-07: Arizona, Colorado"),
    new TimeZoneDescriptor(10, "GMT-0600", "UTC-06: Chicago, Dallas, Houston"),
    new TimeZoneDescriptor(11, "GMT-0500", "UTC-05: Boston, Miami, New York, Washington D.C."),
    new TimeZoneDescriptor(12, "GMT-0400", "UTC-04: Dominican Republic, Nova Scotia, Puerto Rico"),
    new TimeZoneDescriptor(13, "GMT-0300", "UTC-03: Argentina, Uruguay"),
    new TimeZoneDescriptor(14, "GMT-0200", "UTC-02: South Georgia and the South Sandwich Islands"),
    new TimeZoneDescriptor(15, "GMT-0100", "UTC-01: Azores, Cape Verde"),
    new TimeZoneDescriptor(16, "UTC", "UTC: Coordinated Universal Time"),
    new TimeZoneDescriptor(17, "GMT+0100", "UTC+01: Germany, Italy, Switzerland"),
    new TimeZoneDescriptor(18, "GMT+0200", "UTC+02: Greece, Israel, Romania, Syria"),
    new TimeZoneDescriptor(19, "GMT+0300", "UTC+03: Ethiopia, Iraq, Kenya, Saudi Arabia"),
    new TimeZoneDescriptor(20, "GMT+0400", "UTC+04: Moscow, United Arab Emirates"),
    new TimeZoneDescriptor(21, "GMT+0500", "UTC+05: Kazakhstan, Uzbekistan"),
    new TimeZoneDescriptor(22, "GMT+0600", "UTC+06: Bangladesh, Novosibirsk"),
    new TimeZoneDescriptor(23, "GMT+0700", "UTC+07: Jakarta, Thailand, Vietnam"),
    new TimeZoneDescriptor(24, "GMT+0800", "UTC+08: China, Hong Kong, Singapore"),
    new TimeZoneDescriptor(25, "GMT+0900", "UTC+09: Japan, Korea"),
    new TimeZoneDescriptor(26, "GMT+1000", "UTC+10: New South Wales, Queensland, Victoria"),
    new TimeZoneDescriptor(27, "GMT+1100", "UTC+11: Kamchatka, New Caledonia, Solomon Islands"),
    new TimeZoneDescriptor(28, "GMT+1200", "UTC+12: Fiji, New Zealand"),
    new TimeZoneDescriptor(29, "GMT+1300", "UTC+13: Tonga"),
    new TimeZoneDescriptor(30, "GMT+1400", "UTC+14: Line Islands"),
  };
  
  // Calendar
  private static final Calendar CALENDAR_INSTANCE = Calendar.getInstance();

  // Date utility objects.
  private static long     nextDay      = 0L;
  private static int      currentYear  = 0;
  private static int      currentMonth = 0;
  private static int      currentDay   = 0;
  private static Calendar startOfYear  = null;
  private static Calendar startOfMonth = null;
  private static Calendar startOfDay   = null;
  private static Calendar endOfMonth   = null;
  private static Calendar endOfDay     = null;
  private static Calendar endOfYear    = null;
  private static TimeZone systemTimeZone = null;
  private static int      currentTimeZoneOffset = 0;
  
  // Synchronization lock object.
  private static final Object LOCK_OBJECT = new Object();

  /**
   * The default set of permissible "Date Format Strings" that are
   * used by the BasicHelper methods "isValidDate()"
   */
  private static final String[]  PERMITTED_DATE_FORMATS = {

                              "MM-dd-yy HH:mm:ss.SSS",
                              "MM-dd-yy hh:mm:ss.SSS a",
                              "MM-dd-yy hh:mm:ss.SSSa",
                              "MM-dd-yy hh:mm:ss a",
                              "MM-dd-yy hh:mm:ssa",
                              "MM-dd-yy HH:mm:ss",
                              "MM-dd-yy hh:mm a",
                              "MM-dd-yy hh:mma",
                              "MM-dd-yy HH:mm",
                              "MM-dd-yy",

                              "MM-dd-yyyy HH:mm:ss.SSS",
                              "MM-dd-yyyy hh:mm:ss.SSS a",
                              "MM-dd-yyyy hh:mm:ss.SSSa",
                              "MM-dd-yyyy hh:mm:ss a",
                              "MM-dd-yyyy hh:mm:ssa",
                              "MM-dd-yyyy HH:mm:ss",
                              "MM-dd-yyyy hh:mm a",
                              "MM-dd-yyyy hh:mma",
                              "MM-dd-yyyy HH:mm",
                              "MM-dd-yyyy",

                              "MM/dd/yy HH:mm:ss.SSS",
                              "MM/dd/yy hh:mm:ss.SSS a",
                              "MM/dd/yy hh:mm:ss.SSSa",
                              "MM/dd/yy hh:mm:ss a",
                              "MM/dd/yy hh:mm:ssa",
                              "MM/dd/yy HH:mm:ss",
                              "MM/dd/yy hh:mm a",
                              "MM/dd/yy hh:mma",
                              "MM/dd/yy HH:mm",
                              "MM/dd/yy",

                              "MM/dd/yyyy HH:mm:ss.SSS",
                              "MM/dd/yyyy hh:mm:ss.SSS a",
                              "MM/dd/yyyy hh:mm:ss.SSSa",
                              "MM/dd/yyyy hh:mm:ss a",
                              "MM/dd/yyyy hh:mm:ssa",
                              "MM/dd/yyyy HH:mm:ss",
                              "MM/dd/yyyy hh:mm a",
                              "MM/dd/yyyy hh:mma",
                              "MM/dd/yyyy HH:mm",
                              "MM/dd/yyyy",

                              "yyyy-MM-dd HH:mm:ss.SSS",
                              "yyyy-MM-dd HH:mm:ss",
                              "yyyy-MM-dd HH:mm",
                              "yyyy-MM-dd hh:mm:ss.SSS a",
                              "yyyy-MM-dd hh:mm:ss.SSSa",
                              "yyyy-MM-dd hh:mm:ss a",
                              "yyyy-MM-dd hh:mm:ssa",
                              "yyyy-MM-dd hh:mm a",
                              "yyyy-MM-dd hh:mma",
                              "yyyy-MM-dd",

                              "yyyy/MM/dd HH:mm:ss.SSS",
                              "yyyy/MM/dd HH:mm:ss",
                              "yyyy/MM/dd HH:mm",
                              "yyyy/MM/dd hh:mm:ss.SSS a",
                              "yyyy/MM/dd hh:mm:ss.SSSa",
                              "yyyy/MM/dd hh:mm:ss a",
                              "yyyy/MM/dd hh:mm:ssa",
                              "yyyy/MM/dd hh:mm a",
                              "yyyy/MM/dd hh:mma",
                              "yyyy/MM/dd",

                              "d MMM yy HH:mm:ss.SSS",
                              "d MMM yy hh:mm:ss.SSS a",
                              "d MMM yy hh:mm:ss.SSSa",
                              "d MMM yy hh:mm:ss a",
                              "d MMM yy hh:mm:ssa",
                              "d MMM yy HH:mm:ss",
                              "d MMM yy hh:mm a",
                              "d MMM yy hh:mma",
                              "d MMM yy HH:mm",
                              "d MMM yy",

                              "d MMM yyyy HH:mm:ss.SSS",
                              "d MMM yyyy hh:mm:ss.SSS a",
                              "d MMM yyyy hh:mm:ss.SSSa",
                              "d MMM yyyy hh:mm:ss a",
                              "d MMM yyyy hh:mm:ssa",
                              "d MMM yyyy HH:mm:ss",
                              "d MMM yyyy hh:mm a",
                              "d MMM yyyy HH:mm",
                              "d MMM yyyy",

                              "MMM d yy HH:mm:ss.SSS",
                              "MMM d yy hh:mm:ss.SSS a",
                              "MMM d yy hh:mm:ss.SSSa",
                              "MMM d yy hh:mm:ss a",
                              "MMM d yy hh:mm:ssa",
                              "MMM d yy HH:mm:ss",
                              "MMM d yy hh:mm a",
                              "MMM d yy hh:mma",
                              "MMM d yy HH:mm",
                              "MMM d yy",

                              "MMM d yyyy HH:mm:ss.SSS",
                              "MMM d yyyy hh:mm:ss.SSS a",
                              "MMM d yyyy hh:mm:ss.SSSa",
                              "MMM d yyyy hh:mm:ss a",
                              "MMM d yyyy hh:mm:ssa",
                              "MMM d yyyy HH:mm:ss",
                              "MMM d yyyy hh:mm a",
                              "MMM d yyyy hh:mma",
                              "MMM d yyyy HH:mm",
                              "MMM d yyyy",

                              "MMM. d yyyy HH:mm:ss.SSS",
                              "MMM. d yyyy hh:mm:ss.SSS a",
                              "MMM. d yyyy hh:mm:ss.SSSa",
                              "MMM. d yyyy hh:mm:ss a",
                              "MMM. d yyyy hh:mm:ssa",
                              "MMM. d yyyy HH:mm:ss",
                              "MMM. d yyyy hh:mm a",
                              "MMM. d yyyy hh:mma",
                              "MMM. d yyyy HH:mm",
                              "MMM. d yyyy",

                              "MMM. d, yyyy HH:mm:ss.SSS",
                              "MMM. d, yyyy hh:mm:ss.SSS a",
                              "MMM. d, yyyy hh:mm:ss.SSSa",
                              "MMM. d, yyyy hh:mm:ss a",
                              "MMM. d, yyyy hh:mm:ssa",
                              "MMM. d, yyyy HH:mm:ss",
                              "MMM. d, yyyy hh:mm a",
                              "MMM. d, yyyy hh:mma",
                              "MMM. d, yyyy HH:mm",
                              "MMM. d, yyyy",

                              "MMM d, yyyy HH:mm:ss.SSS",
                              "MMM d, yyyy hh:mm:ss.SSS a",
                              "MMM d, yyyy hh:mm:ss.SSSa",
                              "MMM d, yyyy hh:mm:ss a",
                              "MMM d, yyyy hh:mm:ssa",
                              "MMM d, yyyy HH:mm:ss",
                              "MMM d, yyyy hh:mm a",
                              "MMM d, yyyy hh:mma",
                              "MMM d, yyyy HH:mm",
                              "MMM d, yyyy",
                              
                              "yyyy-MM-dd'T'HH:mm'Z'",
                              "yyyy-MM-dd'T'HH:mm:ss'Z'",
                              "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
  };

  //
  // Static initialization.
  //

  /**
   * Statically initializes the default date formatters.
   */
  static
  {
    SimpleDateFormat theFormat;

    for (int iFormatIndex = 0;
      iFormatIndex < PERMITTED_DATE_FORMATS.length;
      iFormatIndex++)
    {
      theFormat = new SimpleDateFormat(
        PERMITTED_DATE_FORMATS[iFormatIndex]
      );

      theFormat.setLenient(false);
      DEFAULT_DATE_FORMATTERS.add(iFormatIndex, theFormat);
    }
  }

  /**
   * Returns whether the given date is within the given Date range.
   * Test is inclusive of the boundary dates. If the given Date is null,
   * it is only considered to be in the range if both the start and end
   * dates are null.
   *
   * @param date The date to test
   * @param rangeStart The start of the range
   * @param rangeEnd The end of the range
   *
   * @return whether the given date is within the given Date range.
   */
  public static boolean isInRange(Date date, Date rangeStart, Date rangeEnd)
  {
    if (date == null)
    {
      return rangeStart == null && rangeEnd == null;
    }

    if (rangeStart != null && rangeStart.compareTo(date) > 0)
    {
      return false;
    }

    if (rangeEnd != null && rangeEnd.compareTo(date) < 0)
    {
      return false;
    }

    return true;
  }

  /**
   * Analogous to isDateInRange but for Calendar objects.
   */
  public static boolean isInRange(Calendar date, Calendar rangeStart,
    Calendar rangeEnd)
  {
    if (date == null)
    {
      return rangeStart == null && rangeEnd == null;
    }

    if (rangeStart != null && rangeStart.compareTo(date) > 0)
    {
      return false;
    }

    if (rangeEnd != null && rangeEnd.compareTo(date) < 0)
    {
      return false;
    }

    return true;
  }

  /**
   * Adjusts the provided Calendar objects to be the very first millisecond
   * of the provided day.
   */
  public static void adjustStartOfDay(Calendar cal)
  {
    // Set time to 00:00:00.000.
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
  }

  /**
   * Adjusts the provided Calendar objects to be the very last millisecond
   * of the provided day.
   */
  public static void adjustEndOfDay(Calendar cal)
  {
    // Zero the time, adjust forward one day, and then back one millisecond.
    adjustStartOfDay(cal);
    cal.add(Calendar.DAY_OF_YEAR, 1);
    cal.add(Calendar.MILLISECOND, -1);
  }

  /**
   * Adjusts the provided Calendar objects to be the very first millisecond
   * of the provided month.
   */
  public static void adjustStartOfMonth(Calendar cal)
  {
    // Set day to 1 and the the time to 00:00:00.000.
    cal.set(Calendar.DAY_OF_MONTH, 1);
    adjustStartOfDay(cal);
  }

  /**
   * Adjusts the provided Calendar objects to be the very last millisecond
   * of the provided month.
   */
  public static void adjustEndOfMonth(Calendar cal)
  {
    // Zero the day of the month, adjust forward one month, and then back
    // one millisecond.
    adjustStartOfMonth(cal);
    cal.add(Calendar.MONTH, 1);
    cal.add(Calendar.MILLISECOND, -1);
  }

  /**
   * Adjusts the provided Calendar objects to be the very first millisecond
   * of the provided year.
   */
  public static void adjustStartOfYear(Calendar cal)
  {
    // Set month to 0 and day to 1 and the the time to 00:00:00.000.
    cal.set(Calendar.MONTH, 0);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    adjustStartOfDay(cal);
  }

  /**
   * Adjusts the provided Calendar objects to be the very last millisecond
   * of the provided year.
   */
  public static void adjustEndOfYear(Calendar cal)
  {
    // Zero the day of the month, adjust forward one month, and then back
    // one millisecond.
    adjustStartOfYear(cal);
    cal.add(Calendar.YEAR, 1);
    cal.add(Calendar.MILLISECOND, -1);
  }

  /**
   * Gets a Calendar representing the first millisecond of the specified
   * month.
   *
   * @param year The year
   * @param month The month, using 1 for January (unlike Calendar, which uses
   * 0)
   */
  public static Calendar getStartOfMonth(int year, int month)
  {
    Calendar toReturn = DateHelper.getCalendarInstance();
    toReturn.clear();

    // Ah, silly Calendar.  Using 0 for January and 1 for the first day in
    // a month.
    toReturn.set(year, (month - 1), 1);

    return toReturn;
  }

  /**
   * Gets a Calendar representing the last millisecond of the specified
   * month.
   *
   * @param year The year
   * @param month The month, using 1 for January (unlike Calendar, which uses
   * 0)
   */
  public static Calendar getEndOfMonth(int year, int month)
  {
    Calendar toReturn = getStartOfMonth(year, month);
    toReturn.add(Calendar.MONTH, 1);
    toReturn.add(Calendar.MILLISECOND, -1);

    return toReturn;
  }

  /**
   * Gets a Calendar representing the first millisecond of the current month.
   */
  public static Calendar getStartOfMonth()
  {
    recalculateCalendarObjects();
    return copy(startOfMonth);
  }

  /**
   * Gets a Calendar representing the last millisecond of the current month.
   */
  public static Calendar getEndOfMonth()
  {
    recalculateCalendarObjects();
    return copy(endOfMonth);
  }

  /**
   * Gets a Calendar representing the first millisecond of the current year.
   */
  public static Calendar getStartOfYear()
  {
    recalculateCalendarObjects();
    return copy(startOfYear);
  }

  /**
   * Gets a Calendar representing the last millisecond of the current year.
   */
  public static Calendar getEndOfYear()
  {
    recalculateCalendarObjects();
    return copy(endOfYear);
  }

  /**
   * Determines if a given Calendar object represents a time in the current
   * month.  Returns false if the calendar parameter is null.
   */
  public static boolean isThisMonth(Calendar calendar)
  {
    return ( (calendar != null)
          && (getStartOfMonth().compareTo(calendar) <= 0)
          && (getEndOfMonth().compareTo(calendar) >= 0)
          );
  }

  /**
   * Gets the current day in the month, starting at 1.
   */
  public static int getCurrentDay()
  {
    recalculateCalendarObjects();
    return currentDay;
  }
  
  /**
   * Gets the current month, where January is 1.
   */
  public static int getCurrentMonth()
  {
    recalculateCalendarObjects();
    return currentMonth;
  }
  
  /**
   * Gets the current year.
   */
  public static int getCurrentYear()
  {
    recalculateCalendarObjects();
    return currentYear;
  }
  
  /**
   * Gets a Calendar representing the first millisecond of the current day.
   */
  public static Calendar getStartOfDay()
  {
    recalculateCalendarObjects();
    return copy(startOfDay);
  }

  /**
   * Gets a Calendar representing the last millisecond of the current day.
   */
  public static Calendar getEndOfDay()
  {
    recalculateCalendarObjects();
    return copy(endOfDay);
  }
  
  /**
   * Gets the current system TimeZone.  For some reason, the 
   * TimeZone.getDefault() results in a synchronization.
   */
  public static TimeZone getSystemTimeZone()
  {
    recalculateCalendarObjects();
    return systemTimeZone;
  }
  
  /**
   * Gets a time zone from the SIMPLE_TIME_ZONES array (see the comments
   * on the array above); returning Pacific time if the provided ID is out
   * of range.
   */
  public static TimeZone getSimpleTimeZone(int id)
  {
    if (  (id >= 0)
       && (id < SIMPLE_TIME_ZONES.length)
       )
    {
      return SIMPLE_TIME_ZONES[id].getTimeZone();
    }
    else
    {
      return SIMPLE_TIME_ZONES[0].getTimeZone(); // Pacific time.
    }
  }
  
  /**
   * Gets a copy of the SIMPLE_TIME_ZONES array.
   */
  public static TimeZoneDescriptor[] getSimpleTimeZones()
  {
    return SIMPLE_TIME_ZONES.clone();
  }
  
  /**
   * Gets the maximum index of the SIMPLE_TIME_ZONES array.
   */
  public static int getSimpleTimeZonesMaxIndex()
  {
    return SIMPLE_TIME_ZONES.length;
  }
  
  /**
   * Gets the current offset in milliseconds from UTC, based on the system
   * TimeZone and the current time.
   */
  public static int getCurrentTimeZoneOFfset()
  {
    recalculateCalendarObjects();
    return currentTimeZoneOffset;
  }
  
  /**
   * Determines if a given Calendar object represents a time in the current
   * day.  Returns false if the calendar parameter is null.
   */
  public static boolean isToday(Calendar calendar)
  {
    return ( (calendar != null)
          && (getStartOfDay().compareTo(calendar) <= 0)
          && (getEndOfDay().compareTo(calendar) >= 0)
          );
  }

  /**
   * Calculates various Calendar and date constants once per day.
   */
  protected static void recalculateCalendarObjects()
  {
    // Get the current time in the UTC time zone.
    long currentTime = System.currentTimeMillis();

    // Determine if we need to recalculate (is it a new day?)
    if ((nextDay == 0) || (currentTime > nextDay))
    {
      synchronized (LOCK_OBJECT)
      {
        Calendar cal = DateHelper.getCalendarInstance();

        // Adjust the current time based on the current time zone.
        currentTime += cal.getTimeZone().getOffset(currentTime);

        // Round down the current time to the nearest day.
        currentTime -= (currentTime % UtilityConstants.DAY);

        // Capture current day, month, year.
        currentDay = cal.get(Calendar.DAY_OF_MONTH);
        currentMonth = cal.get(Calendar.MONTH) + 1; // Deal with 0 index.
        currentYear = cal.get(Calendar.YEAR);
        
        // Calculate the start of the current day.
        startOfDay = (Calendar)cal.clone();
        adjustStartOfDay(startOfDay);

        // Calculate the end of the current day.
        endOfDay = (Calendar)startOfDay.clone();
        adjustEndOfDay(endOfDay);

        // Calculate the start of the current month.
        startOfMonth = (Calendar)startOfDay.clone();
        adjustStartOfMonth(startOfMonth);

        // Calculate the end of the current month.
        endOfMonth = (Calendar)startOfMonth.clone();
        adjustEndOfMonth(endOfMonth);

        // Calculate the start of the current year.
        startOfYear = (Calendar)startOfMonth.clone();
        adjustStartOfYear(startOfYear);

        // Calculate the end of the current year.
        endOfYear = (Calendar)startOfYear.clone();
        adjustEndOfYear(endOfYear);

        // Get the system Timezone and current UTC offset.
        systemTimeZone = TimeZone.getDefault();
        currentTimeZoneOffset = systemTimeZone.getOffset(currentTime);
        
        // Set the next day to one day ahead of the rounded-down time.
        nextDay = currentTime + UtilityConstants.DAY;
      }
    }
  }
  
  /**
   * Copies a Date object, returning null if the parameter is null.  This
   * may seem nonsensical, but Date objects are mutable, so any set method
   * that accepts a Date or get method that returns a date should return
   * a copy rather than use the main reference itself.  Rather than write
   * a bunch of null checks every time, just call DateHelper.copy(date).
   */
  public static Date copy(Date date)
  {
    return (date != null ? (Date)date.clone() : null);
  }
  
  /**
   * Copies a Calendar object, returning null if the parameter is null.  See
   * notes on copy(Date).
   */
  public static Calendar copy(Calendar calendar) 
  {
    return (calendar != null ? (Calendar)calendar.clone() : null);
  }
  
  /**
   * Is the provided date in the past?  A null date will return false because
   * this method does not know if null is in the past or not.
   * 
   * @param date A Date to evaluate; null values will result in false being
   *        returned.
   */
  public static boolean isPast(Date date)
  {
    if (date == null)
    {
      return false;
    }
    
    long now = System.currentTimeMillis();
    return (date.getTime() < now);
  }
  
  /**
   * Is the provided date in the future?  A null date will return false 
   * because this method does not know if null is in the future or not.  As 
   * a result, this method is not precisely the opposite of isPast.
   *  
   * @param date A Date to evaluate; null values will result in false being
   *        returned.
   */
  public static boolean isFuture(Date date)
  {
    if (date == null)
    {
      return false;
    }
    
    long now = System.currentTimeMillis();
    return (date.getTime() > now);
  }
 
  private static final String DELTA_DATE_TOKENS = "+-hdwmy";

  /**
   * Deltas the provided Calendar object according to a simple expression
   * using the following syntax:
   *   <p>
   *   - = Prefix to indicate subtraction of time
   *   + = Optional prefix to indicate addition of time
   *   #h = A number of hours (e.g., 1h)
   *   #d = A number of days (e.g., 3d)
   *   #w = A number of weeks (e.g., 5w)
   *   #m = A number of months (e.g., 7m)
   *   #y = A number of years (e.g., 10y)
   *   <p>
   * The syntax can be chained.  E.g., -3d12h (3 days, 12 hours ago)
   *   <p>
   * Note that any value can be 0, but aside from the prefix, all values
   * should be positive.  Technically, the appearance of a + or - will change
   * to addition or subtraction, respectively, as parsing from left to right.
   * Therefore -3d+12h will yield (2 days, 12 hours ago) and +3d-12h will
   * yield (2 days, 12 hours forward).
   *   <p>
   * -1w+6d24h yields a net change of 0 since both 6 days and 24 hours will
   * be added after the first week is subtracted.
   *   <p>
   * Returns a true if a parse was successful; false otherwise
   */
  public static boolean deltaDate(Calendar input, String delta)
  {
    boolean success = false;

    // Don't do anything if the parameters are empty.
    if ( (input != null)
      && (StringHelper.isNonEmpty(delta))
      )
    {
      // Tokenize the delta string.
      StringTokenizer tokenizer = new StringTokenizer(delta, DELTA_DATE_TOKENS, true);
      String token;
      int lastAmount = 0;
      boolean addition = true;

      // Proceed through the tokens.
      while (tokenizer.hasMoreTokens())
      {
        token = tokenizer.nextToken();
        if (token.equals("+"))
        {
          addition = true;
        }
        else if (token.equals("-"))
        {
          addition = false;
        }
        else if (DELTA_DATE_TOKENS.contains(token))
        {
          int field = Calendar.DAY_OF_MONTH;
          switch (token.charAt(0))
          {
            case 'h': { field = Calendar.HOUR_OF_DAY; break; }
            case 'w': { field = Calendar.WEEK_OF_YEAR; break; }
            case 'm': { field = Calendar.MONTH; break; }
            case 'y': { field = Calendar.YEAR; break; }
            // This cannot happen, but Sonar.
            default: break;
          }
          if (addition)
          {
            input.add(field, lastAmount);
          }
          else
          {
            input.add(field, -lastAmount);
          }
          lastAmount = 0;
          success = true;
        }
        else
        {
          lastAmount = NumberHelper.parseInt(token, 0);
        }
      }
    }

    // Nothing done.
    return success;
  }
  
  /**
   * Determines the number of whole time-units difference between two dates by
   * subtracting the first date from the second.  Partial time-units will be 
   * rounded off.  The time-units are specified by providing a reference to
   * the constant for the time unit, e.g., UtilityConstants.HOUR.
   *   <p>
   * For example, assuming you could provide dates as literals:
   * getDifference([2010-01-01 00:00:00], 
   *               [2010-01-02 12:00:00], 
   *               UtilityConstants.DAY)
   * would return 1.  The additional 12 hours of difference would be rounded 
   * off.
   *   <p>
   * Returns 0 if either Date object is null.
   */
  public static int getDifference(Date date1, Date date2, long timeUnitInMillis)
  {
    if ((date1 != null) && (date2 != null))
    {
      long t1 = date1.getTime();
      long t2 = date2.getTime();
      
      return (int)((t2 - t1) / timeUnitInMillis);
    }
    else
    {
      return 0;
    }
  }
  
  /**
   * A variation of getDifference that assumes the current date is the second
   * parameter to getDifference.  This is basically a means to determine the
   * amount of time that has elapsed -since- a provided date.
   */
  public static int getDifferenceSince(Date date1, long timeUnitInMillis)
  {
    if (date1 != null)
    {
      long t1 = date1.getTime();
      long t2 = System.currentTimeMillis();
      
      return (int)((t2 - t1) / timeUnitInMillis);
    }
    else
    {
      return 0;
    }
  }

  private static final String[] HUMAN_DATE_LABELS = new String[] { "year", "week", "day", "hour", "minute", "second" };
  private static final String[] HUMAN_DATE_LABELS_COMPACT = new String[] { "y", "w", "d", "h", "m", "s" };
  private static final long[]   HUMAN_DATE_AMOUNTS = new long[] { UtilityConstants.YEAR, UtilityConstants.WEEK, UtilityConstants.DAY, UtilityConstants.HOUR, UtilityConstants.MINUTE, UtilityConstants.SECOND };

  /**
   * Renders the difference between two Date objects as a human-readable
   * amount of time (e.g., "2 days, 4 hours").
   *
   * @param date1 The first date object.
   * @param date2 The second date object.
   * @param specificity Defines how many "levels" of specificity to provide;
   *   for example 3 could yield "2 days, 4 hours, 20 minutes" where 2 would
   *   drop the minutes.  This parameter must be at least 1.
   */
  public static String getHumanDifference(Date date1, Date date2, int specificity)
  {
    // Only proceed if the parameters are not null.  Specificity must be
    // at least 1.
    if ( (date1 != null)
      && (date2 != null)
      && (specificity > 0)
      )
    {
      // Compute the difference.
      long difference = Math.abs(date1.getTime() - date2.getTime());

      return getHumanDuration(difference, specificity, false);
    }

    // Return empty string if bad parameters were provided.
    return "";
  }

  /**
   * Renders the difference between two times in milliseconds as a 
   * human-readable amount of time (e.g., "2 days, 4 hours").
   *
   * @param millis1 The first time in milliseconds.
   * @param millis2 The second time in milliseconds.
   * @param specificity Defines how many "levels" of specificity to provide;
   *   for example 3 could yield "2 days, 4 hours, 20 minutes" where 2 would
   *   drop the minutes.  This parameter must be at least 1.
   */
  public static String getHumanDifference(long millis1, long millis2, int specificity)
  {
    // Compute the difference.
    long difference = Math.abs(millis1 - millis2);

    return getHumanDuration(difference, specificity, false);
  }

  /**
   * Renders the difference between NOW and a time in milliseconds as a 
   * human-readable amount of time (e.g., "2 days, 4 hours").
   *
   * @param millis The comparison time in milliseconds (to compare versus
   *   the present time).
   * @param specificity Defines how many "levels" of specificity to provide;
   *   for example 3 could yield "2 days, 4 hours, 20 minutes" where 2 would
   *   drop the minutes.  This parameter must be at least 1.
   */
  public static String getHumanDifference(long millis, int specificity)
  {
    return getHumanDifference(millis, System.currentTimeMillis(), specificity);
  }
  
  /**
   * Renders a number of milliseconds as a human-readable amount of time
   * (e.g., "2 days, 4 hours").
   *
   * @param milliseconds the number of milliseconds.
   * @param specificity Defines how many "levels" of specificity to provide;
   *   for example 3 could yield "2 days, 4 hours, 20 minutes" where 2 would
   *   drop the minutes.  This parameter must be at least 1.
   */
  public static String getHumanDuration(long milliseconds, int specificity)
  {
    return getHumanDuration(milliseconds, specificity, false);
  }
  
  /**
   * Renders a number of milliseconds as a human-readable amount of time
   * (e.g., "2 days, 4 hours").
   *
   * @param milliseconds the number of milliseconds.
   * @param specificity Defines how many "levels" of specificity to provide;
   *   for example 3 could yield "2 days, 4 hours, 20 minutes" where 2 would
   *   drop the minutes.  This parameter must be at least 1.
   * @param compact If true, use compact notation (e.g., "d" for days"); if 
   *   false, use normal notation (e.g., "days").
   */
  public static String getHumanDuration(long milliseconds, int specificity,
      boolean compact)
  {
    long ms = milliseconds;
    StringList toReturn = new StringList(", ");

    int level = 0;

    // Handle the less than 1 second special case.
    if (ms < UtilityConstants.SECOND)
    {
      return compact
          ? "<1s"
          : "Less than 1 second";
    }
    // Handle the general case.
    else
    {
      for (int i = 0; i < HUMAN_DATE_LABELS.length; i++)
      {
        int count = (int)(ms / HUMAN_DATE_AMOUNTS[i]);
        if (count > 0)
        {
          if (compact)
          {
            toReturn.add(count + HUMAN_DATE_LABELS_COMPACT[i]);
          }
          else
          {
            toReturn.add(count + " " + HUMAN_DATE_LABELS[i] + StringHelper.pluralize(count));
          }
          level++;
          if (level == specificity)
          {
            return toReturn.toString();
          }

          ms -= count * HUMAN_DATE_AMOUNTS[i];
        }
      }
    }

    return toReturn.toString();
  }

  /**
   * Parses a date using both parseComplexDate (first) and deltaDate (second),
   * using an optionally provided input Calendar that will be provided to
   * deltaDate.  If that optional parameter is null, the deltaDate function
   * will use the current date instead.
   *
   * @param input An optional Calendar to use as the starting time/date.
   * @param delta A string in the format described in deltaDate above.
   * @param timeZone the TimeZone to use or null for system default.
   */
  public static Calendar parseDelta(Calendar input, String delta,
    TimeZone timeZone)
  {
    // Try parsing as a normal date.
    Calendar toReturn = parse(delta, timeZone);

    // It's not a normal date.  Try parsing as a deltaDate.
    if (toReturn == null)
    {
      // If there is no input date, use right now.
      Calendar start = (input != null ? (Calendar)input.clone() : DateHelper.getCalendarInstance()); 
      
      // If deltaDate returns true, it succeeded in parsing (at least
      // partially).
      if (deltaDate(start, delta))
      {
        toReturn = start;
      }
    }

    return toReturn;
  }

  /**
   * Determines if a specified year (as an integer) is a leap year.  Leap
   * years are years that are divisible by 4, with the following exceptions:
   *    <ol>
   * <li>Years divisible by 100 are not leap years, <i>except that,</i>
   * <li>Years divisible by 400 <i>are</i> leap years.
   *    </ol>
   */
  public static boolean isLeapYear(int year)
  {
    return ( ((year % 4 == 0) && (year % ONE_CENTURY != 0))
          || (year % FOUR_CENTURIES == 0)
          );
  }
  
  private static final int ONE_CENTURY = 100;
  private static final int FOUR_CENTURIES = 400;

  /**
   * Gets a clone of the Default Date Formatters list.
   */
  public static List<SimpleDateFormat> getDefaultDateFormatters()
  {
    return new ArrayList<>(DEFAULT_DATE_FORMATTERS);
  }
  
  /**
   * isValidDate() returns a boolean if the date passed in the String
   * parameter represents a valid date according to the validation rules
   * defined by the set of SimpleDateFormat objects in UtilityConstants.
   * DEFAULT_DATE_FORMATTERS
   *
   * If only the date is provided, Gemini's default set of "Date Format
   * Strings" provided in UtilityConstants are applied to validate the
   * String value.
   *
   * Alternatively, the caller has the option of passing his own
   * application-defined ArrayList of SimpleDateFormat objects in order
   * to customize the validation rules being applied to the string holding
   * the date to be validated.
   *
   * This method returns false if the "date" parameter is null. If the
   * "dateFormatters" parameter is null, a default values will be used in
   * its place.
   *
   * A caller can have returned to it the index of the SimpleDateFormat
   * object whose pattern matches the date being parsed by calling the most
   * complex form of "isValidDate()" and providing a non-null short int
   * as fourth parameter.
   *
   * Following the call to "isValidDate()" this int will store -1 if the
   * method returns false or some non-negative value corresponding to the
   * index of the SimpleDateFormat object whose pattern successfully
   * matched the date value.
   */
  public static boolean isValid(String date)
  {
    return isValid(date, DEFAULT_DATE_FORMATTERS);
  }

  /**
   * See isValidDate(String).
   */
  public static boolean isValid(String date, List<SimpleDateFormat> dateFormatters)
  {
    return isValid(date, dateFormatters, null);
  }

  /**
   * See isValidDate(String).
   */
  public static boolean isValid(String date, List<SimpleDateFormat> dateFormatters,
    AtomicInteger indexOfValidFormatter)
  {
    // This is an unrecoverable error state
    if (date == null)
    {
      return false;
    }

    // If "dateFormatters" is empty then use the default set of
    // SimpleDateFormat objects
    final List<SimpleDateFormat> formatters = CollectionHelper.isEmpty(dateFormatters)
        ? DEFAULT_DATE_FORMATTERS
        : dateFormatters;

    // Attempt to parse the submitted string using the different
    // parsing patterns we're supporting.

    SimpleDateFormat dateFormat;

    for (int iIndex = 0; iIndex < formatters.size(); iIndex++)
    {
      dateFormat = formatters.get(iIndex);

      synchronized (dateFormat)
      {
        if (dateFormat.parse(date, new ParsePosition(0)) != null)
        {
          if (indexOfValidFormatter != null)
          {
            indexOfValidFormatter.set(iIndex);
          }
          return true;
        }
      }
    }
    if (indexOfValidFormatter != null)
    {
      indexOfValidFormatter.set(-1);
    }

    return false;
  }

  /**
   * getCalendarAsString() provides users of the com.techempower package
   * with a method of quickly converting a Calendar object into a string
   * with a limited degree of control over customizing the appearance of
   * that String.
   *
   * @param theCalendar the Calendar object to be formatted into a String
   * @param dateStyle (valid values are DateFormat.SHORT,
   *        DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL)
   * @param timeStyle (valid values are DateFormat.SHORT,
   *        DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL)
   * @return a String representing the Date stored in the Calendar object
   *
   */
  public static String format(Calendar theCalendar,
    int dateStyle, int timeStyle)
  {
    return format(theCalendar, dateStyle, timeStyle, "", "");
  }

  /**
   * See getCalendarAsString(Calendar, int, int)
   */
  public static String format(Calendar theCalendar,
    int dateStyle, int timeStyle, String alternateDateSeparator,
    String dateTimeSpacer)
  {
    String stringValue;

    if (dateTimeSpacer.equalsIgnoreCase(""))
    {
      stringValue = (DateFormat.getDateTimeInstance(dateStyle,
        timeStyle)).format(theCalendar.getTime());
    }
    else
    {
      stringValue = DateFormat.getDateInstance(dateStyle).format(theCalendar.getTime())
        + dateTimeSpacer
        + DateFormat.getTimeInstance(timeStyle).format(theCalendar.getTime());
    }

    if (!alternateDateSeparator.equalsIgnoreCase(""))
    {
      StringHelper.replaceSubstrings(stringValue, new String[] { "." },
        new String[] { alternateDateSeparator });
    }

    return stringValue;
  }

  /**
   * Formats a Date object to a String using the simple standard US format
   * DD/MM/YYYY.
   */
  public static String formatUS(Date date)
  {
    return formatUS(date, null);
  }

  /**
   * Formats a Date object to a String using the simple standard US format
   * DD/MM/YYYY with an optional default.
   */
  public static String formatUS(Date date, String defaultValue)
  {
    String value = defaultValue;

    if (date != null)
    {
      value = STANDARD_US_DATE_FORMAT.format(date);
    }

    return value;
  }

  /**
   * Formats a Date object to a String using the simple standard UK format
   * DD MMM YYY.
   */
  public static String formatUK(Date date)
  {
    return formatUK(date, null);
  }

  /**
   * Formats a Date object to a String using the simple standard UK format
   * DD MMM YYY with an optional default.
   */
  public static String formatUK(Date date, String defaultValue)
  {
    String value = defaultValue;

    if( date != null )
    {
      value = STANDARD_UK_DATE_FORMAT.format(date);
    }

    return value;
  }

  /**
   * Format a Date object to a String using the full date format.
   */
  public static String format(Date date)
  {
    return format(date, null);
  }

  /**
   * Format a Date object to a String using the full date format with an
   * optional default.
   */
  public static String format(Date date, String defaultValue)
  {
    String value = defaultValue;

    if( date != null )
    {
      value = STANDARD_FULL_DATE_FORMAT.format(date);
    }

    return value;
  }

  /**
   * parseComplexDate() returns a Calendar object initialized to the value
   * specified in the String parameter "date".  By default, the date value
   * contained in the string is validated using the default set of
   * SimpleDateFormat objects contained in UtilityConstants.
   * DEFAULT_DATE_FORMATTERS.
   *
   * Alternatively, the caller has the option of passing his own
   * application-defined ArrayList of SimpleDateFormat objects in order to
   * customize the validation rules being applied to the string holding
   * the date to be validated.
   *
   * This method returns <null> if the "date" parameter is null. If the
   * "dateFormatters" parameter is null, a default value will be used in
   * its place.
   *
   * A caller can have returned to it the index of the SimpleDateFormat
   * object whose pattern matches the date being parsed by calling the
   * most complex form of "parseComplexDate()" and providing a non-null
   * short int as fourth parameter.
   *
   * Following the call to "isValidDate()" this int will store -1 if the
   * method returns false or some non-negative value corresponding to the
   * index of the SimpleDateFormat object whose pattern successfully matched
   * the date value.
   */
  public static Calendar parse(String date)
  {
    return parse(date, DEFAULT_DATE_FORMATTERS);
  }

  /**
   * See parseComplexDate(String).
   *
   * @param timeZone The TimeZone in which the given date string should be 
   * evaluated.  If null, the default TimeZone is used.
   */
  public static Calendar parse(String date, TimeZone timeZone)
  {
    return parse(date, DEFAULT_DATE_FORMATTERS, null, timeZone);
  }

  /**
   * See parseComplexDate(String).
   */
  public static Calendar parse(String date, List<SimpleDateFormat> dateFormatters)
  {
    return parse(date, dateFormatters, null);
  }

  /**
   * See parseComplexDate(String).
   */
  public static Calendar parse(
    String date,
    List<SimpleDateFormat> dateFormatters,
    AtomicInteger indexOfValidFormatter)
  {
    return parse(
      date,
      dateFormatters,
      indexOfValidFormatter,
      null);
  }

  /**
   * See parseComplexDate(String).
   *
   * @param timeZone The TimeZone in which the given date string should be
   * evaluated.  If null, the default TimeZone is used.
   */
  public static Calendar parse(
    String date,
    List<SimpleDateFormat> dateFormatters,
    AtomicInteger indexOfValidFormatter,
    TimeZone timeZone)
  {
    // This is an unrecoverable error state
    if (date == null)
    {
      return null;
    }

    // If "dateFormatters" is empty then use the default set of
    // SimpleDateFormat objects
    final List<SimpleDateFormat> formatters = CollectionHelper.isEmpty(dateFormatters)
        ? DEFAULT_DATE_FORMATTERS
        : dateFormatters;

    Calendar dateToReturn = null;
    AtomicInteger parseIndex = new AtomicInteger(-1);

    // If the date is valid, then return a Calendar object initialized with the
    // date represented by the String parameter named "date"

    if (isValid(date, formatters, parseIndex))
    {
      dateToReturn = DateHelper.getCalendarInstance();
      dateToReturn.clear();

      SimpleDateFormat formatter = formatters.get(parseIndex.get());

      // If a TimeZone is provided then make a copy of the SimpleDateFormat
      // and set its TimeZone.
      if (timeZone != null)
      {
        dateToReturn.setTimeZone(timeZone);

        formatter = new SimpleDateFormat( formatter.toPattern() );
        formatter.setTimeZone(timeZone);
      }

      synchronized (formatter)
      {
        dateToReturn.setTime(formatter.parse(date, new ParsePosition(0)));
      }
    }

    if (indexOfValidFormatter != null)
    {
      indexOfValidFormatter.set(parseIndex.get());
    }

    return dateToReturn;
  }

  /**
   * This method returns a Calendar given a String.  This is a very simple
   * date parser and only works with simplistic American date formats.  For
   * more robust date parsing, use a SimpleDateFormat or DateFormat object
   * from the standard Java API.  Parses dates with 4- or 2-digit years;
   * in the case of a 2-digit year, treats values greater than 50 as 1900
   * and values of 50 or lesser as 2000.
   *    <p>
   * Returns null if the input is improper.
   *
   * @param date A very simple American-only String representation of a
   * date.
   */
  public static Calendar parseSimple(String date)
  {
    // Only proceed if we're given a non-null String.
    if (date != null)
    {
      // trim date of any spaces
      String work = date.trim();

      // Dates can be separated by -, /, or .
      StringTokenizer tokenizer = new StringTokenizer(work, "-/.");
      String strMonth = tokenizer.nextToken();
      String strDay   = tokenizer.nextToken();
      String strYear  = tokenizer.nextToken();

      int month = Integer.parseInt(strMonth) - 1;
      int day   = Integer.parseInt(strDay);
      int year  = Integer.parseInt(strYear);

      // 2-digit years greater than 50 will be in 1900, below are in 2000.
      if (year < 100)
      {
        if (year > 50)
        {
          year += 1900;
        }
        else
        {
          year += 2000;
        }
      }

      Calendar cal = DateHelper.getCalendarInstance();
      cal.clear();
      cal.set(year, month, day);

      return cal;
    }

    return null;
  }

  /**
   * This method gets the day representation from the Calendar object
   * for a string representing a day of the week.
   *
   * @return Calendar's day constant or -1 in the event of an error.
   */
  public static int getDayOfWeek(String day)
  {
    if (day != null)
    {
      if (day.equalsIgnoreCase("monday"))
      {
        return Calendar.MONDAY;
      }
      else if (day.equalsIgnoreCase("tuesday"))
      {
        return Calendar.TUESDAY;
      }
      else if (day.equalsIgnoreCase("wednesday"))
      {
        return Calendar.WEDNESDAY;
      }
      else if (day.equalsIgnoreCase("thursday"))
      {
        return Calendar.THURSDAY;
      }
      else if (day.equalsIgnoreCase("friday"))
      {
        return Calendar.FRIDAY;
      }
      else if (day.equalsIgnoreCase("saturday"))
      {
        return Calendar.SATURDAY;
      }
      else if (day.equalsIgnoreCase("sunday"))
      {
        return Calendar.SUNDAY;
      }
    }

    return -1;
  }

  /**
   * This method gets the String representation for a day given the
   * Calendar class's constants for days of the week.
   */
  public static String getDayOfWeek(int day)
  {
    switch (day)
    {
      case Calendar.MONDAY:    return "Monday";
      case Calendar.TUESDAY:   return "Tuesday";
      case Calendar.WEDNESDAY: return "Wednesday";
      case Calendar.THURSDAY:  return "Thursday";
      case Calendar.FRIDAY:    return "Friday";
      case Calendar.SATURDAY:  return "Saturday";
      case Calendar.SUNDAY:    return "Sunday";
      default:                 return null;
    }
  }

  /**
   * Determines if the provided Calendar object represents a week day.
   *
   * @return true if the Calendar is a weekday.
   */
  public static boolean isWeekday(Calendar cal)
  {
    return !(isWeekend(cal));
  }

  /**
   * Determines if the provided Calendar object represents a weekend day.
   *
   * @return true if the Calendar is a weekend day.
   */
  public static boolean isWeekend(Calendar cal)
  {
    int day = cal.get(Calendar.DAY_OF_WEEK);
    return ( (day == Calendar.SATURDAY)
          || (day == Calendar.SUNDAY)
          );
  }

  /**
   * Given the desired day of the week, returns the first occurrence of that
   * day in the current month.
   * 
   * @param dayOfWeek One of Calendar.MONDAY, Calendar.TUESDAY, etc.
   */
  public static Date getFirstSpecifiedDayOfWeekInMonth(int dayOfWeek)
  {
    return getFirstSpecifiedDayOfWeekInMonth(dayOfWeek, new Date());
  }

  /**
   * Given the desired day of the week, returns the first occurrence of that
   * day in the given month.
   * 
   * @param dayOfWeek One of Calendar.MONDAY, Calendar.TUESDAY, etc.
   * @param date The date to work from.
   * @return The date of the first occurrence of dayOfWeek in the given month.
   */
  public static Date getFirstSpecifiedDayOfWeekInMonth(int dayOfWeek, Date date)
  {
    return getFirstSpecifiedDayOfWeekInSpecifiedPeriod(Calendar.DAY_OF_MONTH, dayOfWeek, date);
  }

  /**
   * Given the desired day of the week, returns the first occurrence of that
   * day in the current month.
   * 
   * @param dayOfWeek One of Calendar.MONDAY, Calendar.TUESDAY, etc.
   */
  public static Date getFirstSpecifiedDayOfWeekInYear(int dayOfWeek)
  {
    return getFirstSpecifiedDayOfWeekInYear(dayOfWeek, new Date());
  }

  /**
   * Given the desired day of the week, returns the first occurrence of that
   * day in the given month.
   * 
   * @param dayOfWeek One of Calendar.MONDAY, Calendar.TUESDAY, etc.
   * @param date The date to work from.
   * @return The date of the first occurrence of dayOfWeek in the given month.
   */
  public static Date getFirstSpecifiedDayOfWeekInYear(int dayOfWeek, Date date)
  {
    return getFirstSpecifiedDayOfWeekInSpecifiedPeriod(Calendar.DAY_OF_YEAR, dayOfWeek, date);
  }

  /**
   * Given the desired day of the week, returns the first occurrence of that
   * day in the given month.
   * 
   * @param dayOfMonthOrYear One of Calendar.DAY_OF_MONTH or Calendar.DAY_OF_YEAR.
   * @param dayOfWeek One of Calendar.MONDAY, Calendar.TUESDAY, etc.
   * @param date The date to work from.
   * @return The date of the first occurrence of dayOfWeek in the given month or year.
   */
  public static Date getFirstSpecifiedDayOfWeekInSpecifiedPeriod(int dayOfMonthOrYear, int dayOfWeek, Date date)
  {
    Calendar newCal = DateHelper.getCalendarInstance();
    newCal.setTime(date);
    newCal.set(dayOfMonthOrYear, 1);

    int currentDayOfWeek = newCal.get(Calendar.DAY_OF_WEEK);
    int offset = dayOfWeek - currentDayOfWeek;
    if (currentDayOfWeek > dayOfWeek)
    {
      offset += 7; // for when we're already past the target day
    }
    newCal.add(dayOfMonthOrYear, offset);
    return newCal.getTime();
  }

  /**
   * Safe to use in any place you would normally call Calendar.getInstance().
   * <p>
   * Recreating a Calendar instance uses a synchronized Hashtable class
   * variable in Calendar, causing an unnecessary lock. Instead, call this
   * function any time you need a new Calendar and you'll get one cloned from
   * a single instance created at class initialization time.
   * <p>
   * Note: If you need an instance for a different locale or time zone from
   * the default, you'll want to use something different.
   */
  public static Calendar getCalendarInstance()
  {
    return getCalendarInstance(System.currentTimeMillis());
  }
  
  /**
   * Gets a new calendar object initialized with the provided date/time in
   * milliseconds.  See getCalendarInstance() above for more information.
   */
  public static Calendar getCalendarInstance(long datetime)
  {
    Calendar cal = (Calendar)CALENDAR_INSTANCE.clone();
    cal.setTimeInMillis(datetime);
    return cal;
  }

  /**
   * You may not instantiate this class.
   */
  private DateHelper()
  {
    // Does nothing.
  }

}  // End DateHelper.
