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

import java.util.*;

import org.junit.*;

/**
 * Tests for EnhancedProperties.
 */
public class EnhancedPropertiesTest {
  
  EnhancedProperties props1;
  Properties starting;

  @Before
  public void setup() {
    props1 = new EnhancedProperties();

    // Create a Properties object to populate some starting items.
    starting = new Properties();
    starting.put("one", "1");
    starting.put("two", "2");
    starting.put("onelong", "1");
    starting.put("onefloat", "1.0");
    starting.put("true", "true");
    starting.put("on", "on");
    starting.put("yes", "yes");
    starting.put("false", "false");
    starting.put("off", "off");
    starting.put("no", "no");
    starting.put("hello", "world");
    starting.put("array", "hello,world");
    starting.put("expanded", "hello ${hello}");
    starting.put("prefixed.one", "1");

    props1.putAll(starting);
  }
  
  @Test
  public void size() {
    EnhancedProperties test = new EnhancedProperties();
    test.putAll(starting);
    assertEquals(starting.size(), test.size());
  }
  
  @Test
  public void has() {
    assertTrue(props1.has("one"));
    assertFalse(props1.has("missing-item"));
  }
  
  @Test
  public void get() {
    assertEquals("world", props1.get("hello"));
    assertEquals("test", props1.get("missing-item", "test"));
  }
  
  @Test
  public void getInt() {
    assertEquals(1, props1.getInt("one"));
    assertEquals(2, props1.getInt("two"));
    assertEquals(3, props1.getInt("missing-item", 3));
  }
  
  @Test
  public void getLong() {
    assertEquals(1L, props1.getLong("onelong"));
    assertEquals(2L, props1.getLong("missing-item", 2L));
  }
  
  @Test
  public void getFloat() {
    assertEquals(1.0f, props1.getFloat("onefloat"), 0.01f);
    assertEquals(2.0f, props1.getFloat("missing-item", 2.0f), 0.01f);
  }
  
  @Test
  public void getBoolean() {
    assertTrue(props1.getBoolean("true"));
    assertTrue(props1.getBoolean("on"));
    assertTrue(props1.getBoolean("yes"));
    assertTrue(props1.getBoolean("missing-item", true));
    assertFalse(props1.getBoolean("false"));
    assertFalse(props1.getBoolean("off"));
    assertFalse(props1.getBoolean("no"));
    assertFalse(props1.getBoolean("missing-item"));
  }
  
  @Test
  public void getArray() {
    String[] expected = new String[] { "hello","world" };
    String[] empty = new String[0];
    assertArrayEquals(expected, props1.getArray("array"));
    assertArrayEquals(empty, props1.getArray("missing-item"));
    assertArrayEquals(expected, props1.getArray("missing-item", expected));
  }
  
  @Test
  public void macroExpansion() {
    assertEquals("hello world", props1.get("expanded"));
    assertEquals("hello world", props1.get("missing-item", "hello ${hello}"));
  }
  
  @Test
  public void focus() {
    EnhancedProperties.Focus focus = props1.focus("prefixed.");
    assertTrue(focus.has("one"));
    assertEquals(1, focus.getInt("one", 0));
    assertFalse(focus.has("missing-item"));
    assertNull(focus.get("missing-item"));
  }
  
  @Test
  public void put() {
    EnhancedProperties test = new EnhancedProperties();
    test.put("true", true);
    assertTrue(test.getBoolean("true"));
    test.put("false", false);
    assertFalse(test.getBoolean("false", true));
    test.put("one", 1);
    assertEquals(1, test.getInt("one"));
    test.put("onelong", 1L);
    assertEquals(1L, test.getLong("onelong"));
    test.put("hello", "world");
    assertEquals("world", test.get("hello"));
  }
  
  @Test
  public void remove() {
    EnhancedProperties test = new EnhancedProperties();
    test.put("hello", "world");
    assertEquals("world", test.get("hello"));
    test.remove("hello");
    assertNull(test.get("hello"));
  }

}
