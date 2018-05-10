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

/**
 * Tests for CollectionHelper.
 */
public class CollectionHelperTest {

  List<String> slist1;
  List<String> slist2;
  List<Integer> ilist1;
  List<Object> emptylist;
  Collection<Object> emptyCollection;
  Collection<Object> nullCollection;
  
  Map<String, String> smap1;
  Map<Object, Object> emptyMap;
  Map<Object, Object> nullMap;
  
  String[] sarray1, sarray2, sarray3;
  int[] iarray1;
  char[] carray1, carray2, carray3;
  byte[] barray1;
  Object[] emptyarray;
  
  @Before
  public void setup() {
    slist1 = new ArrayList<>();
    slist1.add("Foo");
    slist1.add("Bar");
    slist1.add("Goo");
    slist1.add("Baz");
    
    slist2 = new ArrayList<>();
    slist2.add("0");
    slist2.add("1");
    slist2.add("2");
    slist2.add("3");
    
    ilist1 = new ArrayList<>();
    ilist1.add(0);
    ilist1.add(1);
    ilist1.add(2);
    ilist1.add(3);
    
    emptyCollection = new ArrayList<>();
    emptylist = new ArrayList<>();
    
    smap1 = new HashMap<>();
    smap1.put("f", "Foo");
    smap1.put("b", "Bar");
    smap1.put("g", "Goo");
    
    emptyMap = new HashMap<>();
    
    sarray1 = new String[] { "Foo", "Bar", "Goo", "Baz" };
    sarray2 = new String[] { "0", "1", "2", "3" };
    sarray3 = new String[] { "0", "1", "2", "3", "Bar", "Baz", "Foo", "Goo" };
    iarray1 = new int[] { 0, 1, 2, 3 };
    barray1 = new byte[] { 0, 1, 2, 3 };
    carray1 = new char[] { 'a', 'b', 'c', 'd' };
    carray2 = new char[] { 'z', 'y', 'x' };
    carray3 = new char[] { 'a', 'b', 'c', 'd', 'x', 'y', 'z' };
    emptyarray = new Object[0];
  }
  
  @Test
  public void get() {
    // Tests the suite of "get" methods.
    
    assertEquals("Foo", CollectionHelper.getFirst(slist1));
    assertEquals(Integer.valueOf(0), CollectionHelper.getFirst(ilist1));

    assertEquals("Baz", CollectionHelper.getLast(slist1));
    assertEquals(Integer.valueOf(3), CollectionHelper.getLast(ilist1));
    
    assertTrue(slist1.contains(CollectionHelper.getRandom(slist1)));
    assertTrue(ilist1.contains(CollectionHelper.getRandom(ilist1)));
  }
  
  @Test
  public void limit() {
    List<String> slimited = CollectionHelper.limit(slist1, 2);
    assertEquals(2, slimited.size());
    assertEquals(slist1.get(0), slimited.get(0));
    assertEquals(slist1.get(1), slimited.get(1));
  }
  
  @Test
  public void empty() {
    // Tests the suite of empty methods.
    
    assertFalse(CollectionHelper.isEmpty(slist1));
    assertFalse(CollectionHelper.isEmpty(ilist1));
    assertTrue(CollectionHelper.isEmpty(emptylist));
    assertTrue(CollectionHelper.isEmpty(emptyCollection));
    assertTrue(CollectionHelper.isEmpty(nullCollection));
    
    assertTrue(CollectionHelper.isNonEmpty(slist1));
    assertTrue(CollectionHelper.isNonEmpty(ilist1));
    assertFalse(CollectionHelper.isNonEmpty(emptylist));
    assertFalse(CollectionHelper.isNonEmpty(emptyCollection));
    assertFalse(CollectionHelper.isNonEmpty(nullCollection));
    
    assertFalse(CollectionHelper.isEmpty(smap1));
    assertTrue(CollectionHelper.isEmpty(emptyMap));
    assertTrue(CollectionHelper.isEmpty(nullMap));
    
    assertTrue(CollectionHelper.isNonEmpty(smap1));
    assertFalse(CollectionHelper.isNonEmpty(emptyMap));
    assertFalse(CollectionHelper.isNonEmpty(nullMap));
    
    assertFalse(CollectionHelper.isEmpty(sarray1));
    assertTrue(CollectionHelper.isEmpty(emptyarray));
    assertTrue(CollectionHelper.isEmpty((Object[])null));
    
    assertTrue(CollectionHelper.isNonEmpty(sarray1));
    assertFalse(CollectionHelper.isNonEmpty(emptyarray));
    assertFalse(CollectionHelper.isNonEmpty((Object[])null));
  }
  
  @Test
  public void arrayContains() {
    // Tests the suite of "arrayContains" and "arrayIndexOf" methods.
    
    assertEquals(0, CollectionHelper.arrayIndexOf(sarray1, "Foo"));
    assertEquals(1, CollectionHelper.arrayIndexOf(sarray1, "Bar"));
    assertEquals(-1, CollectionHelper.arrayIndexOf(sarray1, "Notfound"));
    
    assertTrue(CollectionHelper.arrayContains(sarray1, "Foo"));
    assertFalse(CollectionHelper.arrayContains(sarray1, "Notfound"));
    
    assertEquals(0, CollectionHelper.arrayIndexOf(iarray1, 0));
    assertEquals(1, CollectionHelper.arrayIndexOf(iarray1, 1));
    assertEquals(-1, CollectionHelper.arrayIndexOf(iarray1, 100));
    
    assertTrue(CollectionHelper.arrayContains(iarray1, 1));
    assertFalse(CollectionHelper.arrayContains(iarray1, 100));
    
    assertEquals(0, CollectionHelper.arrayIndexOf(carray1, 'a'));
    assertEquals(1, CollectionHelper.arrayIndexOf(carray1, 'b'));
    assertEquals(-1, CollectionHelper.arrayIndexOf(carray1, 'z'));
    
    assertTrue(CollectionHelper.arrayContains(carray1, 'a'));
    assertFalse(CollectionHelper.arrayContains(carray1, 'z'));

    assertEquals(0, CollectionHelper.arrayIndexOfIgnoreCase(sarray1, "FOO"));
    assertEquals(1, CollectionHelper.arrayIndexOfIgnoreCase(sarray1, "bar"));
    assertEquals(-1, CollectionHelper.arrayIndexOfIgnoreCase(sarray1, "Notfound"));
  }
  
  @Test
  public void toStringSuite() {
    // Tests the toString suite.
    
    // TODO: Reconcile these with StringHelper.join ?
    
    assertEquals("Foo Bar Goo Baz", CollectionHelper.toString(sarray1, " "));
    assertEquals("Foo Bar Goo Baz", CollectionHelper.toString(slist1, " "));
    assertEquals("-Foo -Bar -Goo -Baz", CollectionHelper.toString(slist1, " ", "-"));
    
    assertEquals("0,1,2,3", CollectionHelper.toString(iarray1));
    assertEquals("0,1,2,3", CollectionHelper.toString(barray1));
  }
  
  @Test
  public void toMapFromInterleaved() {
    Map<String, String> result = CollectionHelper.toMapFromInterleaved(sarray1);
    assertEquals("Bar", result.get("Foo"));
    assertEquals("Baz", result.get("Goo"));
  }
  
  @Test
  public void toList() {
    // Tests the toList suite.
    
    assertArrayEquals(sarray1, CollectionHelper.toList(sarray1).toArray());
    assertArrayEquals(sarray1, CollectionHelper.toList(slist1).toArray());
    assertArrayEquals(sarray1, CollectionHelper.toList(slist1.iterator()).toArray());
    
    List<Integer> result = CollectionHelper.toList(iarray1);
    assertEquals(0, result.get(0).intValue());
    assertEquals(1, result.get(1).intValue());
  }
  
  @Test
  public void toSet() {
    // Tests the toSet suite.
    
    Set<String> result = CollectionHelper.toSet(sarray1);
    assertTrue(result.contains("Foo"));
    assertTrue(result.contains("Goo"));
    assertFalse(result.contains("Notfound"));

    result = CollectionHelper.toSet(slist1);
    assertTrue(result.contains("Foo"));
    assertTrue(result.contains("Goo"));
    assertFalse(result.contains("Notfound"));
  }
  
  @Test
  public void toIntArray() {
    // Tests the toIntArray suite.
    
    assertArrayEquals(iarray1, CollectionHelper.toIntArray(sarray2));
    assertArrayEquals(iarray1, CollectionHelper.toIntArray("0,1,2,3", ","));
    assertArrayEquals(iarray1, CollectionHelper.toIntArray("Foo,1,2,3", ","));
    assertArrayEquals(iarray1, CollectionHelper.toIntArray(ilist1));
    assertArrayEquals(iarray1, CollectionHelper.toIntArrayFromStrings(slist2));
  }
  
  @Test
  public void toStringArray() {
    // Tests the toStringArray suite.
    
    assertArrayEquals(sarray1, CollectionHelper.toStringArray(slist1));
    assertArrayEquals(sarray2, CollectionHelper.toStringArray(iarray1));
    assertArrayEquals(sarray2, CollectionHelper.toStringArray(barray1));
  }
  
  @Test
  public void arrayMerge() {
    // Tests the arrayMerge suite.
    
    assertArrayEquals(sarray3, CollectionHelper.arrayMerge(true, sarray1, sarray2));
    assertArrayEquals(sarray3, CollectionHelper.arrayMerge(true, sarray2, sarray1));
    assertArrayEquals(carray3, CollectionHelper.arrayMerge(true, carray1, carray2));
    assertArrayEquals(carray3, CollectionHelper.arrayMerge(true, carray2, carray1));
  }
  
  @Test
  public void getFilledArray() {
    // Tests the filledArray suite.
    
    assertArrayEquals(new String[] { "Foo", "Foo" }, CollectionHelper.getFilledArray("Foo", 2));
    assertArrayEquals(new int[] { 1, 1 }, CollectionHelper.getFilledArray(1, 2));
  }
  
  @Test
  public void getReducedMap() {
    Map<String, String> result = CollectionHelper.getReducedMap(
        smap1, "f", "Fookey", "g", "Gookey");
    
    assertEquals("Foo", result.get("Fookey"));
    assertEquals("Goo", result.get("Gookey"));
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void getReducedMapException() {
    CollectionHelper.getReducedMap(smap1, "bad");
  }
  
  @Test
  public void getIntersection() {
    assertTrue(CollectionHelper.isEmpty(CollectionHelper.getIntersection(slist1, null)));
    assertTrue(CollectionHelper.isEmpty(CollectionHelper.getIntersection(null, slist1)));
    
    Collection<String> result = CollectionHelper.getIntersection(slist1, CollectionHelper.toList(new String[] { "Foo" } ));
    assertEquals(1, result.size());
    assertEquals("Foo", result.iterator().next());
  }

}
