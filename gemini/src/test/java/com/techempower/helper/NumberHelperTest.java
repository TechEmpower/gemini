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

import org.junit.*;

import com.techempower.util.*;

/**
 * Tests for NumberHelper.
 */
public class NumberHelperTest {

  private static final IntRange intRange = new IntRange(1, 3);
  
  private static final long bigLong = 1000000000000L;
  
  private static final LongRange longRange = new LongRange(1, bigLong);
  
  @Test
  public void boundInteger() {
    assertEquals(1, NumberHelper.boundInteger(1, intRange));    
    assertEquals(1, NumberHelper.boundInteger(0, intRange));    
    assertEquals(3, NumberHelper.boundInteger(3, intRange));    
    assertEquals(3, NumberHelper.boundInteger(4, intRange));
    
    assertEquals(1, NumberHelper.boundInteger(1, 1, 3));
    assertEquals(1, NumberHelper.boundInteger(0, 1, 3));
    assertEquals(3, NumberHelper.boundInteger(3, 1, 3));
    assertEquals(3, NumberHelper.boundInteger(4, 1, 3));
  }
  
  @Test
  public void boundLong() {
    assertEquals(1, NumberHelper.boundLong(1, longRange));    
    assertEquals(1, NumberHelper.boundLong(0, longRange));    
    assertEquals(longRange.max, NumberHelper.boundLong(longRange.max, longRange));    
    assertEquals(longRange.max, NumberHelper.boundLong(longRange.max + 1, longRange));
    
    assertEquals(1, NumberHelper.boundLong(1, 1, 3));
    assertEquals(1, NumberHelper.boundLong(0, 1, 3));
    assertEquals(3, NumberHelper.boundLong(3, 1, 3));
    assertEquals(3, NumberHelper.boundLong(4, 1, 3));
  }
  
  @Test
  public void parseInt() {
    assertEquals(0, NumberHelper.parseInt("abc"));
    assertEquals(1, NumberHelper.parseInt("1"));
    assertEquals(1, NumberHelper.parseInt("abc", 1));
    assertEquals(-1, NumberHelper.parseInt("-1"));
    assertEquals(0, NumberHelper.parseInt("1a"));
    assertEquals(0, NumberHelper.parseInt("1 "));
    assertEquals(0, NumberHelper.parseInt(" 1"));
    assertEquals(0, NumberHelper.parseInt("a1"));
    assertEquals(2, NumberHelper.parseInt(" 1", 2));
    assertEquals(2, NumberHelper.parseInt("a1", 2));
    assertEquals(2, NumberHelper.parseInt("1", 3, 2, 4));  // bound "1" to 2-4.
    
    for (int i = 0; i < 20; i++)
    {
      assertEquals(i, NumberHelper.parseInt("" + (i)));
      assertEquals(-i, NumberHelper.parseInt("" + (-i)));
      assertEquals(Integer.MIN_VALUE + i, NumberHelper.parseInt("" + (Integer.MIN_VALUE + i)));
      assertEquals(Integer.MAX_VALUE - i, NumberHelper.parseInt("" + (Integer.MAX_VALUE - i)));
    }

    assertEquals(1, NumberHelper.parseIntPermissive("1", 0));
    assertEquals(1, NumberHelper.parseIntPermissive("1.", 0));
    assertEquals(1, NumberHelper.parseIntPermissive(" 1.", 0));
    assertEquals(2, NumberHelper.parseIntPermissive("abc", 2));
  }
  
  @Test
  public void parseLong() {
    assertEquals(0, NumberHelper.parseLong("abc"));
    assertEquals(1, NumberHelper.parseLong("1"));
    assertEquals(1, NumberHelper.parseLong("abc", 1));
    assertEquals(-1, NumberHelper.parseLong("-1"));
    assertEquals(0, NumberHelper.parseLong("1a"));
    assertEquals(0, NumberHelper.parseLong("1 "));
    assertEquals(0, NumberHelper.parseLong(" 1"));
    assertEquals(0, NumberHelper.parseLong("a1"));
    assertEquals(2, NumberHelper.parseLong(" 1", 2));
    assertEquals(2, NumberHelper.parseLong("a1", 2));
    assertEquals(2, NumberHelper.parseLong("1", 3, 2, 4));  // bound "1" to 2-4.
    
    assertEquals(bigLong, NumberHelper.parseLong("" + bigLong));
    assertEquals(bigLong, NumberHelper.parseLong("" + (bigLong + 1), 0, 0, bigLong));

    for (int i = 0; i < 20; i++)
    {
      assertEquals(i, NumberHelper.parseLong("" + (i)));
      assertEquals(-i, NumberHelper.parseLong("" + (-i)));
      assertEquals(Long.MIN_VALUE + i, NumberHelper.parseLong("" + (Long.MIN_VALUE + i)));
      assertEquals(Long.MAX_VALUE - i, NumberHelper.parseLong("" + (Long.MAX_VALUE - i)));
    }
  }
  
  @Test
  public void parseFloat() {
    assertEquals(1.0f, NumberHelper.parseFloat("1", 0.0f), 0.01f);
    assertEquals(1.0f, NumberHelper.parseFloat("1.", 0.0f), 0.01f);
    assertEquals(1.0f, NumberHelper.parseFloat("1.0", 0.0f), 0.01f);
    assertEquals(1.0f, NumberHelper.parseFloat(" 1.0", 0.0f), 0.01f);
    assertEquals(2.0f, NumberHelper.parseFloat("abc", 2.0f), 0.01f);
  }
  
  @Test
  public void isNumber() {
    assertTrue(NumberHelper.isNumber("-1"));
    assertTrue(NumberHelper.isNumber("0"));
    assertTrue(NumberHelper.isNumber("1"));
    assertFalse(NumberHelper.isNumber(""));
    assertFalse(NumberHelper.isNumber("abc"));
    assertFalse(NumberHelper.isNumber("1abc"));
  }
  
  @Test
  public void round() {
    assertEquals(1.0d, NumberHelper.round(1.01d, 1), 0.0d);
    assertEquals(1.0d, NumberHelper.round(0.99d, 1), 0.0d);
    assertNotEquals(1.0d, NumberHelper.round(1.51d, 1), 0.0d);
    assertNotEquals(1.0d, NumberHelper.round(0.49d, 1), 0.0d);
    assertEquals(1.0f, NumberHelper.round(1.2f, 0), 0.01f);
    assertEquals(1.2f, NumberHelper.round(1.22f, 1), 0.001f);
    assertEquals(-1.0f, NumberHelper.round(-1.2f, 0), 0.01f);
    assertEquals(-1.2f, NumberHelper.round(-1.22f, 1), 0.001f);
  }

  @Test
  public void paging() {
    // Tests the paging suite of methods.

    assertEquals(0, NumberHelper.getPageStartOffset(0, 10, 100));
    assertEquals(0, NumberHelper.getPageStartOffset(1, 10, 100));
    assertEquals(10, NumberHelper.getPageStartOffset(2, 10, 100));
    assertEquals(0, NumberHelper.getPageStartOffset(20, 10, 100));
    assertEquals(190, NumberHelper.getPageStartOffset(20, 10));
    
    // TODO: Not really sure of the utility of getPageEndOffset since it's
    // not returning the end (inclusive) but 1 greater than the end.
    assertEquals(10, NumberHelper.getPageEndOffset(0, 10, 100));
    assertEquals(10, NumberHelper.getPageEndOffset(1, 10, 100));
    assertEquals(20, NumberHelper.getPageEndOffset(2, 10, 100));
    
    assertEquals(0, NumberHelper.getPageCount(0, 100));
    assertEquals(1, NumberHelper.getPageCount(100, 100));
    assertEquals(10, NumberHelper.getPageCount(100, 10));
    assertEquals(11, NumberHelper.getPageCount(101, 10));
  }

}
