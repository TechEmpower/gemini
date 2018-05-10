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
 * Tests for StringHelper.
 */
public class StringHelperTest {

  private static final String nullString = null;

  private List<String> list1, list2;
  private String[] array1, array2, emptyArray;
  private String sentence = "This is a sentence.";

  @Before
  public void setup() {
    emptyArray = new String[0];
    
    list1 = new ArrayList<>();
    list1.add("");
    list1.add("foo");
    list1.add("BAR");
    list1.add("  baz  ");
    list1.add("  GOO  ");
    list1.add(null);

    list2 = new ArrayList<>();
    list2.add("");
    list2.add("foo");
    list2.add("BAR");
    list2.add("baz");
    list2.add("GOO");
    list2.add("");

    array1 = new String[] { "", "foo", "BAR", "  baz  ", "  GOO  ", null };
    array2 = new String[] { "", "foo", "BAR", "baz", "GOO", "" };
  }
  
  @Test
  public void pluralize() {
    assertEquals("s", StringHelper.pluralize(2));
    assertEquals("s", StringHelper.pluralize(0));
    assertEquals("", StringHelper.pluralize(1));
    
    assertEquals("a", StringHelper.pluralize(2, "um", "a"));
    assertEquals("a", StringHelper.pluralize(0, "um", "a"));
    assertEquals("um", StringHelper.pluralize(1, "um", "a"));
  }
  
  @Test
  public void padZero() {
    assertEquals("0", StringHelper.padZero(0, 0));
    assertEquals("0", StringHelper.padZero(0, 1));
    assertEquals("1", StringHelper.padZero(1, 0));
    assertEquals("1", StringHelper.padZero(1, 1));
    assertEquals("00", StringHelper.padZero(0, 2));
    assertEquals("01", StringHelper.padZero(1, 2));
    assertEquals("-1", StringHelper.padZero(-1, 0));
    assertEquals("-1", StringHelper.padZero(-1, 1));
    assertEquals("-01", StringHelper.padZero(-1, 2));
    
    assertEquals("0123", StringHelper.padZero(123L, 4));
    assertEquals("-1234567890123456789", StringHelper.padZero(-1234567890123456789L, 0));
    
    assertEquals("00abc", StringHelper.padZero("abc", 5));
  }
  
  @Test
  public void padSpace() {
    assertEquals("0", StringHelper.padSpace(0, 0));
    assertEquals("0", StringHelper.padSpace(0, 1));
    assertEquals(" 0", StringHelper.padSpace(0, 2));
    
    assertEquals(" 123", StringHelper.padSpace(123L, 4));
    assertEquals(" 1234567890123456789", StringHelper.padSpace(1234567890123456789L, 20));
    
    assertEquals("a", StringHelper.padSpace("a", 0));
    assertEquals("  a", StringHelper.padSpace("a", 3));
  }
  
  @Test
  public void padArbitrary() {
    assertEquals("1", StringHelper.padArbitrary('a', "1", 0));
    assertEquals("aa1", StringHelper.padArbitrary('a', "1", 3));
  }
  
  @Test
  public void padArbitraryRight() {
    assertEquals("1", StringHelper.padArbitraryRight('a', "1", 0));
    assertEquals("1aa", StringHelper.padArbitraryRight('a', "1", 3));
  }
  
  @Test
  public void isNonEmpty() {
    assertTrue(StringHelper.isNonEmpty("abc"));
    assertFalse(StringHelper.isNonEmpty(""));
    assertFalse(StringHelper.isNonEmpty(nullString));
    
    assertTrue(StringHelper.isNonEmpty("abc", "xyz"));
    assertFalse(StringHelper.isNonEmpty(new String[] {}));
    assertFalse(StringHelper.isNonEmpty("", ""));
    assertFalse(StringHelper.isNonEmpty(nullString, nullString));
    assertFalse(StringHelper.isNonEmpty("", "abc"));
    assertFalse(StringHelper.isNonEmpty(nullString, "abc"));
  }
  
  @Test
  public void isEmpty() {
    assertFalse(StringHelper.isEmpty("abc"));
    assertTrue(StringHelper.isEmpty(""));
    assertTrue(StringHelper.isEmpty(nullString));
    
    assertFalse(StringHelper.isEmpty("abc", "xyz"));
    assertTrue(StringHelper.isEmpty(new String[] {}));
    assertTrue(StringHelper.isEmpty("", ""));
    assertTrue(StringHelper.isEmpty(nullString, nullString));
    assertFalse(StringHelper.isEmpty("", "abc"));
    assertFalse(StringHelper.isEmpty(nullString, "abc"));

    assertTrue(StringHelper.isEmptyTrimmed(nullString));
    assertTrue(StringHelper.isEmptyTrimmed(""));
    assertFalse(StringHelper.isEmptyTrimmed("foo"));
    assertFalse(StringHelper.isEmptyTrimmed("foo  "));
    assertTrue(StringHelper.isEmptyTrimmed("  "));
    
    assertFalse(StringHelper.isNonEmptyTrimmed(nullString));
    assertFalse(StringHelper.isNonEmptyTrimmed(""));
    assertTrue(StringHelper.isNonEmptyTrimmed("foo"));
    assertTrue(StringHelper.isNonEmptyTrimmed("foo  "));
    assertFalse(StringHelper.isNonEmptyTrimmed("  "));
}
  
  @Test
  public void emptyDefault() {
    assertEquals("abc", StringHelper.emptyDefault("", "abc"));
    assertEquals("abc", StringHelper.emptyDefault(null, "abc"));
    assertEquals("123", StringHelper.emptyDefault("123", "abc"));
  }
  
  @Test
  public void equalsIgnoreCase() {
    assertTrue(StringHelper.equalsIgnoreCase("abc", "abc"));
    assertTrue(StringHelper.equalsIgnoreCase("abc", "ABC"));
    assertFalse(StringHelper.equalsIgnoreCase("123", "ABC"));
    assertFalse(StringHelper.equalsIgnoreCase("123", nullString));
    assertFalse(StringHelper.equalsIgnoreCase(nullString, "ABC"));
    assertFalse(StringHelper.equalsIgnoreCase(nullString, nullString));
    
    assertTrue(StringHelper.equalsIgnoreCase("FOO", array1));
    assertTrue(StringHelper.equalsIgnoreCase("foo", array1));
    assertFalse(StringHelper.equalsIgnoreCase("BAZ", array1));
    assertFalse(StringHelper.equalsIgnoreCase("goo", array1));
    assertFalse(StringHelper.equalsIgnoreCase(null, array1));
    assertFalse(StringHelper.equalsIgnoreCase("not present", array1));

    assertTrue(StringHelper.equalsIgnoreCase("FOO", list1));
    assertTrue(StringHelper.equalsIgnoreCase("foo", list1));
    assertFalse(StringHelper.equalsIgnoreCase("BAZ", list1));
    assertFalse(StringHelper.equalsIgnoreCase("goo", list1));
    assertFalse(StringHelper.equalsIgnoreCase(null, list1));
    assertFalse(StringHelper.equalsIgnoreCase("not present", list1));
    
    assertTrue(StringHelper.equalsIgnoreCaseTrim("FOO", array1));
    assertTrue(StringHelper.equalsIgnoreCaseTrim("foo", array1));
    assertTrue(StringHelper.equalsIgnoreCaseTrim("BAZ", array1));
    assertTrue(StringHelper.equalsIgnoreCaseTrim("goo", array1));
    assertFalse(StringHelper.equalsIgnoreCaseTrim(null, array1));
    assertFalse(StringHelper.equalsIgnoreCaseTrim("not present", array1));

    assertTrue(StringHelper.equalsIgnoreCaseTrim("FOO", list1));
    assertTrue(StringHelper.equalsIgnoreCaseTrim("foo", list1));
    assertTrue(StringHelper.equalsIgnoreCaseTrim("BAZ", list1));
    assertTrue(StringHelper.equalsIgnoreCaseTrim("goo", list1));
    assertFalse(StringHelper.equalsIgnoreCaseTrim(null, list1));
    assertFalse(StringHelper.equalsIgnoreCaseTrim("not present", list1));
  }

  @Test
  public void equals() {
    String ba = "ba";
    String b = "b";
    String ar = "ar";
    String r = "r";
    assertTrue(StringHelper.equals(ba + r, b + ar));
    assertFalse(StringHelper.equals(ba, null));
  }

  @Test
  public void containsWord() {
    assertTrue(StringHelper.containsWord(sentence, "This"));
    assertTrue(StringHelper.containsWord(sentence, "is"));
    
    assertFalse(StringHelper.containsWord(sentence, "foo"));
    assertFalse(StringHelper.containsWord(sentence, "is "));
    assertFalse(StringHelper.containsWord(sentence, " is"));
    assertFalse(StringHelper.containsWord(sentence, "isn't"));
    
    assertFalse(StringHelper.containsWord(sentence, null));
    assertFalse(StringHelper.containsWord(null, "is"));
    
    // TODO: The following tests will fail although they possibly should not.
    // Should punctuation be ignored?
    //assertFalse(StringHelper.containsWord(sentence, "sentence."));
    //assertTrue(StringHelper.containsWord("this is a sentence.", "sentence"));
  }

  @Test
  public void containsIgnoreCase() {
    assertTrue(StringHelper.containsIgnoreCase(sentence, "This"));
    assertTrue(StringHelper.containsIgnoreCase(sentence, "this"));
    assertTrue(StringHelper.containsIgnoreCase(sentence, "THIS"));
    assertFalse(StringHelper.containsIgnoreCase(sentence, "foo"));
    assertFalse(StringHelper.containsIgnoreCase(sentence, nullString));
    assertFalse(StringHelper.containsIgnoreCase(null, "this"));
  }
  
  @Test
  public void containsNullSafe() {
    assertTrue(StringHelper.containsNullSafe(sentence, "This"));
    assertTrue(StringHelper.containsNullSafe(sentence, null));
    assertFalse(StringHelper.containsNullSafe(null, "This"));
  }
  
  @Test
  public void endsWithIgnoreCase() {
    assertTrue(StringHelper.endsWithIgnoreCase(sentence, "."));
    assertTrue(StringHelper.endsWithIgnoreCase(sentence, "ce."));
    assertTrue(StringHelper.endsWithIgnoreCase(sentence, "CE."));
    assertFalse(StringHelper.endsWithIgnoreCase(sentence, "foo"));
    assertFalse(StringHelper.endsWithIgnoreCase(sentence, nullString));
    
    assertFalse(StringHelper.endsWithIgnoreCase(sentence, array1));
    assertTrue(StringHelper.endsWithIgnoreCase(sentence, new String[] { "foo", "CE." }));
  }
  
  @Test
  public void startsWithIgnoreCase() {
    assertTrue(StringHelper.startsWithIgnoreCase(sentence, "This"));
    assertTrue(StringHelper.startsWithIgnoreCase(sentence, "this"));
    assertTrue(StringHelper.startsWithIgnoreCase(sentence, "THIS"));
    assertFalse(StringHelper.startsWithIgnoreCase(sentence, "foo"));
    assertFalse(StringHelper.startsWithIgnoreCase(sentence, (String)null));
    
    assertFalse(StringHelper.startsWithIgnoreCase(sentence, array1));
    assertTrue(StringHelper.startsWithIgnoreCase(sentence, new String[] { "foo", "this" }));
  }
  
  @Test
  public void uppercaseFirstLetter() {
    assertEquals("Foo", StringHelper.uppercaseFirstLetter("foo"));
    assertEquals(" Foo", StringHelper.uppercaseFirstLetter(" foo"));
    assertEquals(" 1Foo", StringHelper.uppercaseFirstLetter(" 1Foo"));
    assertEquals("123 Foo", StringHelper.uppercaseFirstLetter("123 foo"));
    assertEquals("123 FOO", StringHelper.uppercaseFirstLetter("123 FOO"));
  }
  
  @Test
  public void uppercaseEachWord() {
    assertEquals("This Is A Sentence.", StringHelper.uppercaseEachWord(sentence));
    assertEquals("   This Is A Sentence.   ", StringHelper.uppercaseEachWord("   " + sentence + "   "));
    assertEquals("THIS IS A SENTENCE.", StringHelper.uppercaseEachWord(sentence.toUpperCase()));
  }
  
  @Test
  public void trim() {
    assertEquals("foo", StringHelper.trim("foo"));
    assertEquals("foo", StringHelper.trim("  foo  "));
    assertEquals("", StringHelper.trim((String)null));
    
    assertArrayEquals(array2, StringHelper.trim(array1));
  }
  
  @Test
  public void isAll() {
    // Tests the suite of isAll methods.
    
    assertTrue(StringHelper.isAllNumeric("123"));
    assertFalse(StringHelper.isAllNumeric("  123  "));
    assertFalse(StringHelper.isAllNumeric("123abc"));
    assertFalse(StringHelper.isAllNumeric(null));
    
    assertTrue(StringHelper.isAllAlpha("abc"));
    assertTrue(StringHelper.isAllAlpha("XYZ"));
    assertFalse(StringHelper.isAllAlpha("123abc"));
    assertFalse(StringHelper.isAllAlpha(null));

    assertTrue(StringHelper.isAllAlphanumeric("123"));
    assertFalse(StringHelper.isAllAlphanumeric("  123  "));
    assertTrue(StringHelper.isAllAlphanumeric("123abc"));
    assertFalse(StringHelper.isAllAlphanumeric(null));
    
    char[] special = new char[] { '-', '/', '_' };
    
    assertTrue(StringHelper.isAllAlphaAndSpecialChars("aBc", special));
    assertTrue(StringHelper.isAllAlphaAndSpecialChars("aBc-_/", special));
    assertFalse(StringHelper.isAllAlphaAndSpecialChars("aBc-_/&", special));
    assertFalse(StringHelper.isAllAlphaAndSpecialChars("aBc123", special));
    assertFalse(StringHelper.isAllAlphaAndSpecialChars(null, special));
    
    assertTrue(StringHelper.isAllDigitsAndSpecialChars("123", special));
    assertTrue(StringHelper.isAllDigitsAndSpecialChars("123-_/", special));
    assertFalse(StringHelper.isAllDigitsAndSpecialChars("123-_/&", special));
    assertFalse(StringHelper.isAllDigitsAndSpecialChars("aBc123", special));
    assertFalse(StringHelper.isAllDigitsAndSpecialChars(null, special));
    
    assertTrue(StringHelper.isAllAlphaDigitsAndSpecialChars("123", special));
    assertTrue(StringHelper.isAllAlphaDigitsAndSpecialChars("123-_/", special));
    assertFalse(StringHelper.isAllAlphaDigitsAndSpecialChars("123-_/&", special));
    assertTrue(StringHelper.isAllAlphaDigitsAndSpecialChars("aBc123", special));
    assertFalse(StringHelper.isAllAlphaDigitsAndSpecialChars(null, special));
    
    assertTrue(StringHelper.isAll("aBc123---", true, true, true, special));
    assertFalse(StringHelper.isAll("aBc123---", true, false, true, special));
    assertFalse(StringHelper.isAll(null, true, true, true, special));
    
    assertTrue(StringHelper.isAllArbitraryCharacters("--//__", special));
    assertFalse(StringHelper.isAllArbitraryCharacters("--//__?", special));
    assertFalse(StringHelper.isAllArbitraryCharacters(null, special));
    
    assertTrue(StringHelper.isAllArbitraryExcluding("abc123", special));
    assertFalse(StringHelper.isAllArbitraryExcluding("abc-123", special));
    assertFalse(StringHelper.isAllArbitraryExcluding(null, special));
  }
  
  @Test
  public void truncate() {
    // Tests the suite of truncate methods
    
    assertEquals("This", StringHelper.truncate(sentence, 4));
    assertEquals("Foo", StringHelper.truncate("Foo", 4));
    assertNull(StringHelper.truncate(null, 50));
    
    assertEquals("nce.", StringHelper.truncateAtEnd(sentence, 4));
    assertEquals("Foo", StringHelper.truncateAtEnd("Foo", 4));
    assertNull(StringHelper.truncateAtEnd(null, 50));
    
    assertEquals("This...", StringHelper.truncateEllipsis(sentence, 7, false, false, "..."));
    assertEquals("This...", StringHelper.truncateEllipsis(sentence, 8, true, true, "..."));
    assertEquals("This is...", StringHelper.truncateEllipsis(sentence, 8, true, false, "..."));
    assertEquals("", StringHelper.truncateEllipsis("", 100, true, true, "..."));
  }
  
  @Test
  public void wordWrap() {
    // TODO
  }
  
  @Test
  public void split() {
    // Tests the suite of split methods.
    
    String[] expected = new String[] { "abc", "def", "ghi" };
    
    assertArrayEquals(expected, StringHelper.splitAndTrim("abc def ghi", "\\s"));
    assertArrayEquals(expected, StringHelper.splitAndTrim(" abc X def X ghi ", "X"));
    assertArrayEquals(expected, StringHelper.splitTrimAndLower(" aBc X dEf X gHi", "x"));
    assertArrayEquals(expected, StringHelper.splitIntoLines("abc\ndef\r\nghi"));
    assertArrayEquals(expected, StringHelper.splitIntoWords("abc def ghi"));
    
    assertArrayEquals(emptyArray, StringHelper.splitIntoWords(null));
    assertArrayEquals(emptyArray, StringHelper.splitAndTrim(null, " "));
    assertArrayEquals(emptyArray, StringHelper.splitTrimAndLower(null, " "));
  }
  
  @Test
  public void trimExcessBlankLines() {
    String[] test = new String[] {
        "",
        "",
        sentence,
        "",
        "",
        sentence,
        "",
        ""
    };
    
    String[] expected = new String[] {
        "",
        sentence,
        "",
        sentence,
        ""
    };
    
    assertArrayEquals(expected, StringHelper.trimExcessBlankLines(test));

    assertNull(StringHelper.trimExcessBlankLines(null));
  }
  
  @Test
  public void parseBoolean() {
    assertTrue(StringHelper.parseBoolean("true"));
    assertTrue(StringHelper.parseBoolean("TRUE"));
    assertTrue(StringHelper.parseBoolean("yes"));
    assertTrue(StringHelper.parseBoolean("YES"));
    assertTrue(StringHelper.parseBoolean("1"));
    assertTrue(StringHelper.parseBoolean("y"));
    assertTrue(StringHelper.parseBoolean("Y"));
    assertTrue(StringHelper.parseBoolean("on"));
    assertTrue(StringHelper.parseBoolean("ON"));
    
    assertFalse(StringHelper.parseBoolean("false"));
    assertFalse(StringHelper.parseBoolean("FALSE"));
    assertFalse(StringHelper.parseBoolean("no"));
    assertFalse(StringHelper.parseBoolean("NO"));
    assertFalse(StringHelper.parseBoolean("0"));
    assertFalse(StringHelper.parseBoolean("n"));
    assertFalse(StringHelper.parseBoolean("N"));
    assertFalse(StringHelper.parseBoolean("off"));
    assertFalse(StringHelper.parseBoolean("OFF"));
    
    assertTrue(StringHelper.parseBoolean("true", true));
    assertTrue(StringHelper.parseBoolean("true", false));
    assertFalse(StringHelper.parseBoolean("false", true));
    assertTrue(StringHelper.parseBoolean("eh?", true));
    assertFalse(StringHelper.parseBoolean("eh?", false));
  }
  
  @Test(expected = NumberFormatException.class)
  public void parseBooleanException() {
    @SuppressWarnings("unused")
    boolean foo = StringHelper.parseBoolean("eh?");
  }
  
  @Test
  public void strip() {
    // Tests the suite of strip methods.
    
    String withTab = "foo\tbar!";
    String withoutTab = "foo?bar!";
    String rawAlpha = "foobar";
    String withNumbers = "foo1bar2";
    
    char[] foor = new char[] { 'f', 'o', 'r' };
    
    assertEquals(withoutTab, StringHelper.stripISOControlCharacters(withTab, "?"));
    assertNull(StringHelper.stripISOControlCharacters(null, ""));
    assertEquals(rawAlpha, StringHelper.stripNonAlphanumeric(withTab));
    assertNull(StringHelper.stripNonAlphanumeric(null));
    assertEquals("12", StringHelper.stripNonNumeric(withNumbers));
    assertNull(StringHelper.stripNonNumeric(null));
    assertEquals("foor", StringHelper.stripArbitraryCharacters(rawAlpha, foor));
    assertNull(StringHelper.stripArbitraryCharacters(null, foor));
    assertEquals("ba", StringHelper.stripUnacceptableCharacters(rawAlpha, foor));
    assertNull(StringHelper.stripUnacceptableCharacters(null, foor));
    assertEquals("test test", StringHelper.stripExtraSpaces("  test    test  "));
    assertNull(StringHelper.stripExtraSpaces(null));
    
    // stripDoubleDoubleQuotes returns empty string on null parameter, unlike
    // other strip methods, because it uses replaceSubtrings.  I think this
    // is probably fine.
    assertEquals("foo", StringHelper.stripDoubleDoubleQuotes("foo"));
    assertEquals("foo\"bar", StringHelper.stripDoubleDoubleQuotes("foo\"bar"));
    assertEquals("foo\"bar", StringHelper.stripDoubleDoubleQuotes("foo\"\"bar"));
    assertEquals("", StringHelper.stripDoubleDoubleQuotes(null));
  }
  
  @Test
  public void replaceSubstrings() {
    String source = "Th$IS $IS a $SENTENCE";
    Map<String, Object> macros = new HashMap<>();
    macros.put("$IS", "is");
    macros.put("$SENTENCE", "sentence.");
    macros.put("$UNUSED", "unused");
    
    String[] finds = new String[] { "$IS", "$SENTENCE", "$UNUSED" };
    String[] replaces = new String[] { "is", "sentence.", "unused" };
    
    char[] charFinds = new char[] { 'T', 's' };
    char[] charReplaces = new char[] { 'Z', 'z' };
    
    assertEquals(sentence, StringHelper.macroExpand(macros, source));
    assertEquals(sentence, StringHelper.replaceSubstrings(source, finds, replaces));
    assertEquals(sentence.toLowerCase(), StringHelper.replaceSubstringsIgnoreCase(source.toLowerCase(), finds, replaces));
    
    assertEquals("Zhiz iz a zentence.", StringHelper.replaceCharacters(sentence, charFinds, charReplaces));
  }
  
  @Test
  public void escape() {
    // Tests the suite of escape methods.
    
    assertEquals("\'\'Test \'\'\'\' Foo\'\'", StringHelper.escapeSingleQuotes("\'Test \'\' Foo\'"));
    assertEquals("Test \" foo", StringHelper.escapeSingleQuotes("Test \" foo"));
    assertEquals("", StringHelper.escapeSingleQuotes(null));

    assertEquals("\"\"Test \"\"\"\" Foo\"\"", StringHelper.escapeDoubleQuotes("\"Test \"\" Foo\""));
    assertEquals("Test \' foo", StringHelper.escapeDoubleQuotes("Test \' foo"));
    assertEquals("", StringHelper.escapeDoubleQuotes(null));
  }
  
  @Test
  public void random() {
    // Tests the suite of random methods.
    
    char[] chars = new char[] { 'a', 'b', 'c' };
    
    assertTrue(StringHelper.isAllArbitraryCharacters(
        StringHelper.randomString.fromChars(chars, 100), chars));
    assertTrue(StringHelper.isAllAlphanumeric(StringHelper.randomString.alphanumeric(100)));
    assertTrue(StringHelper.isAllNumeric(StringHelper.randomString.numeric(100)));
    
    assertEquals(StringHelper.randomString.fromChars(chars, 100).length(), 100);
    assertEquals(StringHelper.randomString.alphanumeric(100).length(), 100);
    assertEquals(StringHelper.randomString.numeric(100).length(), 100);
    
    char[] notForPasswords = new char[] { '0', 'O', 'l', '1' };
    assertTrue(StringHelper.isAllArbitraryExcluding(
        StringHelper.secureRandomString.password(1000), notForPasswords));
  }
  
  @Test
  public void compareTo() {
    // Tests the suite of compareTo methods.
    
    assertEquals(0, StringHelper.compareToIgnoreCase("APPLE", "apple"));
    assertTrue(StringHelper.compareToIgnoreCase("apple", "PEAR") < 0);
    assertTrue(StringHelper.compareToIgnoreCase("PEAR", "apple") > 0);
    
    assertEquals(0, StringHelper.compareToNullSafe("apple", "apple"));
    assertEquals(0, StringHelper.compareToNullSafe(null, null));
    assertTrue(StringHelper.compareToNullSafe("apple", null) > 0);
    assertTrue(StringHelper.compareToNullSafe(null, "apple") < 0);
  }
  
  @Test
  public void join() {
    // Tests the suite of join methods.
    
    String expected = ",foo,BAR,baz,GOO,";
    
    assertEquals(expected, StringHelper.join(",", list2));
    assertEquals(expected, StringHelper.join(",", array2));
    
    expected = "a,b,c";
    assertEquals(expected, StringHelper.join(",", new char[] { 'a', 'b', 'c' }));
    
    expected = "1,2,3";
    assertEquals(expected, StringHelper.join(",", new byte[] { 1, 2, 3 }));
    assertEquals(expected, StringHelper.join(",", new short[] { 1, 2, 3 }));
    assertEquals(expected, StringHelper.join(",", new int[] { 1, 2, 3 }));
    assertEquals(expected, StringHelper.join(",", new long[] { 1, 2, 3 }));
    
    expected = "1.0,2.0,3.0";
    assertEquals(expected, StringHelper.join(",", new float[] { 1, 2, 3 }));
    assertEquals(expected, StringHelper.join(",", new double[] { 1, 2, 3 }));
    
    expected = "true,false,true";
    assertEquals(expected, StringHelper.join(",", new boolean[] { true, false, true }));
  }

}
