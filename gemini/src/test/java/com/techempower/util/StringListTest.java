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

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Tests for StringList.
 */
public class StringListTest {

  String[] array;

  @Before
  public void setup() {
    array = new String[] { "this", "is", "a", "test" };
  }
  
  @Test
  public void basic() {
    StringList list = new StringList();
    assertEquals(list.length(), 0);
    assertEquals(list.size(), 0);
    assertTrue(list.isEmpty());

    list.add("hello");
    assertEquals(list.length(), 5);
    assertEquals(list.size(), 1);
    assertFalse(list.isEmpty());
    
    list.add("world");
    assertEquals(list.length(), 11);
    assertEquals(list.size(), 2);
    assertFalse(list.isEmpty());
    
    assertEquals("hello,world", list.toString());
  }
  
  @Test
  public void prefixSuffix() {
    StringList list = new StringList(",", "[", "]");
    assertEquals(list.length(), 0);
    assertEquals(list.size(), 0);
    assertTrue(list.isEmpty());

    list.add("hello");
    assertEquals(list.length(), 7);
    assertEquals(list.size(), 1);
    assertFalse(list.isEmpty());
    
    list.add("world");
    assertEquals(list.length(), 15);
    assertEquals(list.size(), 2);
    assertFalse(list.isEmpty());
    
    assertEquals("[hello],[world]", list.toString());
  }
  
  @Test
  public void array() {
    StringList list = new StringList();
    list.addAll(array);
    assertEquals(array.length, list.size());
    assertEquals(11 + 3, list.length());
    assertEquals("this,is,a,test", list.toString());
  }
  
  @Test
  public void englishList() {
    StringList list = StringList.getPlainEnglishList();
    String correct = "this, is, a and test";
    list.addAll(array);
    assertEquals(correct.length(), list.length());
    assertEquals(correct, list.toString());

    list = StringList.getOxfordEnglishList();
    correct = "this, is, a, and test";
    list.addAll(array);
    assertEquals(correct.length(), list.length());
    assertEquals(correct, list.toString());

    list = StringList.getSemicolonEnglishList();
    correct = "this; is; a and test";
    list.addAll(array);
    assertEquals(correct.length(), list.length());
    assertEquals(correct, list.toString());

    list = StringList.getOxfordSemicolonEnglishList();
    correct = "this; is; a; and test";
    list.addAll(array);
    assertEquals(correct.length(), list.length());
    assertEquals(correct, list.toString());
  }
  
}
