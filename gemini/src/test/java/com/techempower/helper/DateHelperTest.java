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

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.techempower.util.*;

/**
 * Tests for DateHelper.
 */
public class DateHelperTest {

  Calendar c1990, c2010, c2030;
  Date d1990, d2010, d2030;
  
  String[] validDates;
  String[] invalidDates;
  
  @Before
  public void setup() {
    c1990 = Calendar.getInstance();
    c1990.set(Calendar.YEAR, 1990);
    c1990.set(Calendar.MONTH, Calendar.JANUARY);
    c1990.set(Calendar.DAY_OF_MONTH, 1);
    c1990.set(Calendar.HOUR_OF_DAY, 0);
    c1990.set(Calendar.MINUTE, 0);
    c1990.set(Calendar.SECOND, 0);
    c1990.set(Calendar.MILLISECOND, 0);
    d1990 = c1990.getTime();
    
    c2010 = (Calendar)c1990.clone();
    c2010.set(Calendar.YEAR, 2010);
    d2010 = c2010.getTime();
    
    c2030 = (Calendar)c1990.clone();
    c2030.set(Calendar.YEAR, 2030);
    d2030 = c2030.getTime();
    
    validDates = new String[] { "May 1, 1990", "12/13/2014", "2015-04-23" };
    invalidDates = new String[] { "Foo 17, 2050", "13/13/2013", "2015-00-15", "Fight!" };
  }
  
  @Test
  public void isInRange() {
    // Tests the isInRange suite.
    assertTrue(DateHelper.isInRange(c2010, c1990, c2030));
    assertFalse(DateHelper.isInRange(c2030, c1990, c2010));

    assertTrue(DateHelper.isInRange(d2010, d1990, d2030));
    assertFalse(DateHelper.isInRange(d2030, d1990, d2010));
  }
  
  @Test
  public void adjust() {
    // Tests the adjust suite.
    
    Calendar now = Calendar.getInstance();
    now.set(Calendar.MONTH, Calendar.JANUARY);

    DateHelper.adjustStartOfDay(now);
    assertEquals(0, now.get(Calendar.MILLISECOND));
    
    DateHelper.adjustEndOfDay(now);
    assertEquals(999, now.get(Calendar.MILLISECOND));
    
    DateHelper.adjustStartOfMonth(now);
    assertEquals(1, now.get(Calendar.DAY_OF_MONTH));
    
    DateHelper.adjustEndOfMonth(now);
    assertEquals(31, now.get(Calendar.DAY_OF_MONTH));
  }
  
  @Test
  public void getMonths() {
    // Tests the getStart and getEnd suite.
    
    Calendar s2020j = DateHelper.getStartOfMonth(2020, 1);
    assertEquals(2020, s2020j.get(Calendar.YEAR));
    assertEquals(Calendar.JANUARY, s2020j.get(Calendar.MONTH));
    assertEquals(1, s2020j.get(Calendar.DAY_OF_MONTH));
    assertEquals(0, s2020j.get(Calendar.MILLISECOND));
    
    s2020j = DateHelper.getEndOfMonth(2020, 1);
    assertEquals(2020, s2020j.get(Calendar.YEAR));
    assertEquals(Calendar.JANUARY, s2020j.get(Calendar.MONTH));
    assertEquals(31, s2020j.get(Calendar.DAY_OF_MONTH));
    assertEquals(999, s2020j.get(Calendar.MILLISECOND));
  }
  
  @Test
  public void isThisMonth() {
    Calendar now = Calendar.getInstance();
    assertTrue(DateHelper.isThisMonth(now));
    
    now.add(Calendar.MONTH, 1);
    assertFalse(DateHelper.isThisMonth(now));
  }
  
  @Test
  public void isToday() {
    Calendar now = Calendar.getInstance();
    assertTrue(DateHelper.isToday(now));
    
    now.add(Calendar.DAY_OF_YEAR, 1);
    assertFalse(DateHelper.isToday(now));
  }
  
  @Test
  public void pastFuture() {
    Date now = new Date();
    now.setTime(System.currentTimeMillis() - 100L);
    
    assertTrue(DateHelper.isPast(now));
    assertFalse(DateHelper.isFuture(now));
    
    now.setTime(System.currentTimeMillis() + 10000L);

    assertFalse(DateHelper.isPast(now));
    assertTrue(DateHelper.isFuture(now));
  }
  
  @Test
  public void deltaDate() {
    // TODO
  }
  
  @Test
  public void getDifference() {
    // Tests the getDifference suite.
    assertEquals(-20, DateHelper.getDifference(d2010, d1990, UtilityConstants.YEAR));
    assertEquals(20, DateHelper.getDifference(d2010, d2030, UtilityConstants.YEAR));

    assertTrue(DateHelper.getDifferenceSince(d2010, UtilityConstants.YEAR) > 0);
    assertTrue(DateHelper.getDifferenceSince(d2030, UtilityConstants.YEAR) < 0);
  }
  
  @Test
  public void getHumanDuration() {
    assertEquals("1 second",
        DateHelper.getHumanDuration(UtilityConstants.SECOND, 1));
    assertEquals(
        "1 minute, 1 second",
        DateHelper.getHumanDuration(UtilityConstants.MINUTE
            + UtilityConstants.SECOND, 2));
    assertEquals(
        "1 minute",
        DateHelper.getHumanDuration(UtilityConstants.MINUTE
            + UtilityConstants.SECOND, 1));
  }
  
  @Test
  public void isLeapYear() {
    assertTrue(DateHelper.isLeapYear(2000));
    assertFalse(DateHelper.isLeapYear(2001));
    assertTrue(DateHelper.isLeapYear(2004));
    assertFalse(DateHelper.isLeapYear(2100));
  }
  
  @Test
  public void isValid() {
    // Tests the isValid suite.
  
    for (String valid : validDates)
    {
      assertTrue(DateHelper.isValid(valid));
    }
    for (String invalid : invalidDates)
    {
      assertFalse(DateHelper.isValid(invalid));
    }
  }
  
  @Test
  public void format() {
    // Tests the format suite.
    
    // TODO
  }
  
  @Test
  public void parse() {
    // Tests the parse suite.
    
    // TODO
  }
  
  @Test
  public void getDayOfWeek() {
    assertEquals(Calendar.MONDAY, DateHelper.getDayOfWeek("MONday"));
    assertEquals(-1, DateHelper.getDayOfWeek("Nosuchday"));
    
    assertEquals("Monday", DateHelper.getDayOfWeek(Calendar.MONDAY));
    assertNull(DateHelper.getDayOfWeek(-100));
  }
  
  @Test
  public void getFirstSpecified() {
    // TODO
  }

}
