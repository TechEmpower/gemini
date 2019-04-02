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

import java.security.*;
import java.util.*;
import java.util.regex.*;

import com.techempower.util.*;

/**
 * StringHelper provides utility functions for working with Strings.
 */
public final class StringHelper
{
  
  //
  // Static variables.
  //
  
  private static final String[] NEWLINE_FINDS = new String[]
    {
      "\n", "\r"
    };
  private static final String[] NEWLINE_REPLACES = new String[]
    {
      "", ""
    };
  /**
   * 80 space characters.
   */
  //   1234567890123456789012345678901234567890
  private static final String   SPACES =
      "                                        " +
      "                                        ";
  private static final IntRange SPACES_RANGE = IntRange.forLengthOf(SPACES);
  private static final String[] SINGLE_QUOTE_ESCAPE_FINDS    = new String[] { "'" };
  private static final String[] SINGLE_QUOTE_ESCAPE_REPLACES = new String[] { "''" };

  //
  // Static methods.
  //

  /**
   * Pluralize a string, if required.  Given an input value, this method
   * returns an empty string if no "s" is required, and a "s" if an s
   * is required.  This method is not intelligent enough to know about
   * complex plurals such as "es" and "i".
   *
   * @param value an integer input value
   *
   * @return "s" if the parameter is not 1.
   */
  public static String pluralize(int value)
  {
    if (value != 1)
    {
      return "s";
    }
    else
    {
      return "";
    }
  }

  /**
   * Pluralize a string, if required.  Similar to the simpler pluralize(int)
   * method, except that this version allows you to specify a singular
   * suffix and a plural suffix.  Depending on the numeric value, one of
   * the two suffices will be appended.
   *
   * @param value an integer input value
   * @param singularSuffix the string suffix to return if the value is
   *        singular.
   * @param pluralSuffix the string suffix to return if the value is
   *        plural.
   *
   * @return plural suffix if the parameter is not 1; singular suffix if
   *         the parameter is 1.
   */
  public static String pluralize(int value,
    String singularSuffix, String pluralSuffix)
  {
    if (value != 1)
    {
      return pluralSuffix;
    }
    else
    {
      return singularSuffix;
    }
  }

  /**
   * Pad a string representation of an integer with leading zeros.
   *
   * @param number an input number to be padded
   * @param digits the number of digits you wish to use
   *
   * @return a String representation of the number padded with zeros until
   *         the length of the string is equal to the "digits" parameter.
   */
  public static String padZero(int number, int digits)
  {
    if (number >= 0)
    {
      return padArbitrary('0', Integer.toString(number), digits);
    }
    else
    {
      return "-" + padArbitrary('0', Integer.toString(Math.abs(number)), digits);
    }
  }

  /**
   * See padZero(int, int).  This version works with longs.
   */
  public static String padZero(long number, int digits)
  {
    if (number >= 0)
    {
      return padArbitrary('0', Long.toString(number), digits);
    }
    else
    {
      return "-" + padArbitrary('0', Long.toString(Math.abs(number)), digits);
    }
  }

  /**
   * See padZero(int, int).  This version takes a String and zero pads
   * as needed.
   */
  public static String padZero(String string, int digits)
  {
    return padArbitrary('0', string, digits);
  }

  /**
   * Pad a string representation of an integer with leading spaces.
   * Does not currently handle negative numbers.
   *
   * @param number an input number to be padded
   * @param digits the number of digits you wish to use
   *
   * @return a String digits-characters in length
   */
  public static String padSpace(int number, int digits)
  {
    return padArbitrary(' ', Integer.toString(number), digits);
  }

  /**
   * See padSpace(long, int).  This version takes a long and space pads
   * as needed.
   */
  public static String padSpace(long number, int digits)
  {
    return padArbitrary(' ', Long.toString(number), digits);
  }

  /**
   * See padSpace(int, int).  This version takes a String and space pads
   * as needed.
   */
  public static String padSpace(String string, int digits)
  {
    return padArbitrary(' ', string, digits);
  }

  /**
   * Pads a String from the left with an arbitrary character.  See zeroPad
   * and spacePad above.
   *   <p>
   * Returns the string unchanged if its length equals or exceeds the target 
   * length.
   * 
   * @param pad The character to use for padding.
   * @param string The string to be padded.
   * @param length The target length after padding.
   */
  public static String padArbitrary(char pad, String string, int length)
  {
    // Return the string unchanged if its length equals or exceeds the target
    // length.
    if (string.length() >= length)
    {
      return string;
    }
    
    char[] buffer = new char[length];
    
    // Add the padding.
    int padding = length - string.length();
    int position;
    for (position = 0; position < padding; position++)
    {
      buffer[position] = pad;
    }
    
    // Copy the String's characters.
    System.arraycopy(string.toCharArray(), 0, buffer, position, string.length());
    
    return new String(buffer);
  }

  /**
   * Pads a String on the right with an arbitrary character.
   *   <p>
   * Returns the string unchanged if its length equals or exceeds the target 
   * length.
   * 
   * @param pad The character to use for padding.
   * @param string The string to be padded.
   * @param length The target length after padding.
   */
  public static String padArbitraryRight(char pad, String string, int length)
  {
    // Return the string unchanged if its length equals or exceeds the target
    // length.
    if (string.length() >= length)
    {
      return string;
    }

    char[] buffer = new char[length];
    
    // Add the padding.
    for (int position = string.length(); position < length; position++)
    {
      buffer[position] = pad;
    }
    
    // Copy the String's characters.
    System.arraycopy(string.toCharArray(), 0, buffer, 0, string.length());
    
    return new String(buffer);
  }

  /**
   * Returns a canonical representation for the string object or null if the
   * string is undefined.
   */
  public static String intern(String str)
  {
    if (str != null)
    {
      return str.intern();
    }

    return null;
  }
  
  /**
   * Returns true if the provided string is non-null and has a length greater
   * than zero.
   */
  public static boolean isNonEmpty(String inputString)
  {
    return inputString != null && inputString.length() > 0;
  }

  /**
   * Determines if all String parameters are 1) not null, and 2) not empty.
   * Returns false if given an empty array as a parameter.
   *
   * @param inputString the String to check
   */
  public static boolean isNonEmpty(String... inputString)
  {
    if (inputString == null || inputString.length == 0)
    {
      return false;
    }
    for (String string : inputString)
    {
      if(isEmpty(string))
      {
        return false;
      }
    }

    return true;
  }

  /**
   * Determines if a provided String parameter is either 1) null, or 2) empty.
   */
  public static boolean isEmpty(String inputString)
  {
    return inputString == null || inputString.length() == 0;
  }

  /**
   * Determines if all provided String parameters are either 1) null, or 2) empty.
   * Returns true if given an empty array as a parameter.
   */
  public static boolean isEmpty(String... inputString)
  {
    if (inputString == null || inputString.length == 0)
    {
      return true;
    }
    for (String string : inputString)
    {
      if (string != null && string.length() > 0)
      {
        return false;
      }
    }

    return true;
  }

  /**
   * After trimming, determines if a String parameter is 1) null, or 2) empty.
   *
   * @param inputString the String to check
   */
  public static boolean isEmptyTrimmed(String inputString)
  {
    return inputString == null 
        || StringHelper.trim(inputString).length() == 0;
  }
  
  /**
   * After trimming, determines if all String parameters are 1) null, or 2) empty.
   *
   * @param inputStrings the String to check
   */
  public static boolean isEmptyTrimmed(String... inputStrings)
  {
    if (inputStrings == null || inputStrings.length == 0)
    {
      return true;
    }
    for (String inputString : inputStrings)
    {
      if (StringHelper.isNonEmptyTrimmed(inputString))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * After trimming, determines if a String parameter is 1) not null, and 2) not empty.
   *
   * @param inputString the String to check
   */
  public static boolean isNonEmptyTrimmed(String inputString)
  {
    return inputString != null 
        && StringHelper.trim(inputString).length() > 0;
  }
  
  /**
   * After trimming, determines if all String parameters are 1) not null, and 2) not empty.
   *
   * @param inputStrings the String to check
   */
  public static boolean isNonEmptyTrimmed(String... inputStrings)
  {
    if (inputStrings == null || inputStrings.length == 0)
    {
      return false;
    }
    for (String inputString : inputStrings)
    {
      if (StringHelper.isEmptyTrimmed(inputString))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the provided String unaltered if its value is non-null
   * and non-empty; otherwise, returns the default.
   *
   * @param stringToTest The String to test and return if non-empty.
   * @param defaultValue The String to return if the stringToTest is empty.
   */
  public static String emptyDefault(String stringToTest,
    String defaultValue)
  {
    if (isEmpty(stringToTest))
    {
      return defaultValue;
    }

    return stringToTest;
  }

  /**
   * Returns true if String s1 matches any string in array s2 after trimming,
   * case insensitive.
   */
  public static boolean equalsIgnoreCaseTrim(String s1, String[] s2)
  {
    return equalsIgnoreCaseTrim(s1, CollectionHelper.toList(s2));
  }

  /**
   * Returns true if String s1 matches any string in Collection s2 after 
   * trimming, case insensitive.
   */
  public static boolean equalsIgnoreCaseTrim(String s1, Collection<String> s2)
  {
    if (s1 != null && CollectionHelper.isNonEmpty(s2))
    {
      for (String aS2 : s2)
      {
        if (equalsIgnoreCaseTrim(s1, aS2))
        {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns true if s1 matches s2 after trimming, case insensitive.
   */
  public static boolean equalsIgnoreCaseTrim(String s1, String s2)
  {
    return equalsIgnoreCase(trim(s1), trim(s2));
  }

  /**
   * Returns true if s1 matches any string in array s2, case insensitive.
   */
  public static boolean equalsIgnoreCase(String s1, String[] s2)
  {
    return equalsIgnoreCase(s1, CollectionHelper.toList(s2));
  }

  /**
   * Returns true if s1 matches any string in Collection s2, case insensitive.
   */
  public static boolean equalsIgnoreCase(String s1, Collection<String> s2)
  {
    if (s1 != null && CollectionHelper.isNonEmpty(s2))
    {
      for (String aS2 : s2)
      {
        if (equalsIgnoreCase(s1, aS2))
        {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Determine if two {@link String}<tt>s</tt> are the same ignoring 
   * differences in case.  If either is <tt>null</tt> will return false.
   * 
   * @param s1 first {@link String} to compare, can be <tt>null</tt>
   * @param s2 second {@link String} to compare, can be <tt>null</tt>
   * @return true if s1 matches s2, case insensitive. If either or both are 
   * null or the Strings do not match returns false.
   */
  public static boolean equalsIgnoreCase(String s1, String s2)
  {
    return s1 != null && s2 != null && s1.equalsIgnoreCase(s2);
  }
  
  /**
   * Returns true if s1 matches s2, case sensitive.
   * @return true if s1 matches s2 , false if not a match or one or both are
   *         <tt>null</tt>
   */
  public static boolean equals(String s1, String s2)
  {
    if (s1 != null && s2 != null)
    {
      return s1.equals(s2);
    }

    return false;
  }

  /**
   * Returns true if the parameter sentence contains the parameter word.  
   * For example:
   * <pre>
   * StringHelper.containsWord("foo bar baz", "bar"); // true
   * StringHelper.containsWord("foo bar baz", "ba"); // false
   * </pre>
   * Note that the parameters obviously don't have to be English words and
   * sentences, but that is the basic use case.
   * 
   * @param sentence An arbitrary string to search.
   * @param word The word to search for.
   */
  public static boolean containsWord(String sentence, String word)
  {
    return (  (sentence != null)
           && (word != null)
           && (sentence.matches("(.*)(\\s|^)" + Pattern.quote(word) + "(\\s|$)(.*)"))
           );
  }

  /**
   * Returns true if a string contains a substring, case insensitive.
   * 
   * @param container the potential container string.
   * @param searchingFor the substring that we're interested in finding within 
   *        the container.
   */
  public static boolean containsIgnoreCase(String container, String searchingFor)
  {
    return isNonEmpty(container) && isNonEmpty(searchingFor)
        && container.toLowerCase().contains(searchingFor.toLowerCase());
  }

  /**
   * Returns true if String s1 contains any string in array s2 after 
   * trimming, case insensitive.
   */
  public static boolean containsIgnoreCase(String s1, String[] s2)
  {
    return containsIgnoreCaseTrim(s1, CollectionHelper.toList(s2));
  }

  /**
   * Returns true if String s1 contains any string in array s2 after 
   * trimming, case insensitive.
   */
  public static boolean containsIgnoreCase(String s1, Collection<String> s2)
  {
    return containsIgnoreCaseTrim(s1, s2);
  }

  /**
   * Returns true if String s1 contains any string in Collection s2 after 
   * trimming, case insensitive.
   */
  public static boolean containsIgnoreCaseTrim(String s1, Collection<String> s2)
  {
    if (s1 != null && CollectionHelper.isNonEmpty(s2))
    {
      for (String aS2 : s2)
      {
        if (containsIgnoreCaseTrim(s1, aS2))
        {
          return true;
        }
      }
    }

    return false;
  }
  
  /**
   * Returns true if s1 matches s2 after trimming, case insensitive.
   */
  public static boolean containsIgnoreCaseTrim(String s1, String s2)
  {
    return containsIgnoreCase(trim(s1), trim(s2));
  }

  /**
   * Checks if the provided substring is found in a reference string at
   * a given location.  If the reference string is not long enough,
   * returns false.
   *   <p>
   * Why not "reference.indexOf(toFind, location) == location" ?
   *   <p>
   * Just performance.  We're not interested in knowing if toFind is found
   * <b>later</b> in the String; we're only interested if it's at exactly
   * the specified location.
   * 
   * @return true if toFind is found exactly at the position specified within
   *   reference; false otherwise (including if either String is null).
   */
  public static boolean containsAt(String reference, String toFind, int location)
  {
    if (  (reference == null)
       || (toFind == null)
       )
    {
      return false;
    }
    
    if (reference.length() >= location + toFind.length())
    {
      // Iterate through and compare characters.
      for (int i = 0; i < toFind.length(); i++)
      {
        if (reference.charAt(location + i) != toFind.charAt(i))
        {
          // No match.
          return false;
        }
      }
      
      // A match.
      return true;
    }
    else
    {
      // Reference string not long enough.
      return false;
    }
  }

  /**
   * A contains check that is null-safe.  Returns false if the string to
   * search is empty; true if the search term is empty.
   *
   * @param toSearch the full string to search.
   * @param searchTerm to the word/term/etc to look for.
   */
  public static boolean containsNullSafe(String toSearch, String searchTerm)
  {
    if (isEmpty(toSearch))
    {
      return false;
    }
    return isEmpty(searchTerm) || toSearch.contains(searchTerm);
  }

  /**
   * Returns true if the provided String ends with the given suffix, ignoring 
   * case.
   *
   * @param s the String to check.
   * @param suffix the suffix to look for.
   * @return true if 's' ends with the suffix provided.
   */
  public static boolean endsWithIgnoreCase(String s, String suffix)
  {
    return (  (isNonEmpty(s))
           && (isNonEmpty(suffix))
           && (s.toLowerCase().endsWith(suffix.toLowerCase()))
           );
  }

  /**
   * Returns true if the provided String ends with one of the given suffixes,
   * ignoring case.
   *
   * @param s the String to check.
   * @param suffix an array of suffixes to look for.
   * @return true if 's' ends with any of the suffixes provided.
   */
  public static boolean endsWithIgnoreCase(String s, String[] suffix)
  {
    if (isNonEmpty(s) && CollectionHelper.isNonEmpty(suffix))
    {
      for (String aSuffix : suffix)
      {
        if (endsWithIgnoreCase(s, aSuffix))
        {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns true if the provided String starts with the given prefix,
   * ignoring case.
   */
  public static boolean startsWithIgnoreCase(String s, String prefix)
  {
    return (  (isNonEmpty(s))
           && (isNonEmpty(prefix))
           && (s.toLowerCase().startsWith(prefix.toLowerCase()))
           );
  }

  /**
   * Returns true if the provided String starts with one of the given prefixes,
   * ignoring case
   *
   * @param s String to test
   * @param prefix All possible prefixes
   * @return true if at least one of the prefixes matches ignoring case
   */
  public static boolean startsWithIgnoreCase(String s, String[] prefix)
  {
    if (isNonEmpty(s) && CollectionHelper.isNonEmpty(prefix))
    {
      for (String aPrefix : prefix)
      {
        if (startsWithIgnoreCase(s, aPrefix))
        {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns a new String with the very first letter converted to uppercase.
   */
  public static String uppercaseFirstLetter(String input)
  {
    if (input == null)
    {
      return null;
    }
    
    char[] buffer = input.toCharArray();
    boolean uppercased = false;
    for (int i = 0; i < buffer.length; i++)
    {
      if ( (!uppercased)
        && (buffer[i] >= 'a')
        && (buffer[i] <= 'z')
        )
      {
        buffer[i] = Character.toUpperCase(buffer[i]);
        break;
      }
      // If the first letter is already uppercase, let's just consider
      // the job done and continue copying characters.
      else if ( (buffer[i] >= 'A')
             && (buffer[i] <= 'Z')
             )
      {
        break;
      }
    }

    return new String(buffer);
  }

  /**
   * Returns a new String with the first letter of each word converted
   * to uppercase.  Word boundaries are any non-letter characters.
   */
  public static String uppercaseEachWord(String input)
  {
    if (input == null)
    {
      return null;
    }
    
    char[] buffer = input.toCharArray();
    boolean uppercased = false;
    for (int i = 0; i < buffer.length; i++)
    {
      if ( (!uppercased)
        && (buffer[i] >= 'a')
        && (buffer[i] <= 'z')
        )
      {
        buffer[i] = Character.toUpperCase(buffer[i]);
        uppercased = true;
      }
      else
      {
        if ( (buffer[i] >= 'A')
          && (buffer[i] <= 'Z')
          )
        {
          uppercased = true;
        }
        else if ( (buffer[i] < 'a')
               || (buffer[i] > 'z')
               )
        {
          uppercased = false;
        }
      }
    }

    return new String(buffer);
  }

  /**
   * Trims the given String. If the string is null then empty-string is returned.
   */
  public static String trim(String s)
  {
    if (s != null)
    {
      return s.trim();
    }

    return "";
  }

  /**
   * Trims the given array of Strings.  If null is given an empty array is 
   * returned.
   * 
   * @param s an array of Strings to be trimmed.
   * @return an array of equal size with the trimmed values.
   */
  public static String[] trim(String[] s)
  {
    String[] toReturn = {};

    if (CollectionHelper.isNonEmpty(s))
    {
      toReturn = new String[s.length];
      for (int i = 0; i < s.length; i++)
      {
        toReturn[i] = trim(s[i]);
      }
    }

    return toReturn;
  }

  /**
   * Returns true if the provided string is composed of all ASCII numeric
   * characters (0 through 9); returns false if any non-numeric characters
   * are found or if the input string is null.
   *
   * @param inputString An input String.
   *
   * @return true if all characters in the String are numeric.
   */
  public static boolean isAllNumeric(String inputString)
  {
    return isAll(inputString, CharacterHelper.asciiNumberMatcher);
  }

  /**
   * Returns true if the provided string is composed of all ASCII letter
   * characters (a through z; A through Z); returns false if any non-letter
   * characters are found or if the input string is null.
   *
   * @param inputString An input String.
   *
   * @return true if all characters in the String are letters.
   */
  public static boolean isAllAlpha(String inputString)
  {
    return isAll(inputString, CharacterHelper.asciiLetterMatcher);
  }

  /**
   * Returns true if the provided string is composed of all ASCII letter or
   * number characters (a through z; A through Z; 0 through 9); returns false
   * if any non-letter and non-number characters are found or if the input 
   * string is null.
   *
   * @param inputString An input String.
   *
   * @return true if all characters in the String are alphanumeric.
   */
  public static boolean isAllAlphanumeric(String inputString)
  {
    return isAll(inputString, CharacterHelper.asciiLetterOrNumberMatcher);
  }

  /**
   * A convenience version of isComposedOfArbitraryCharacters that assumes
   * that the String should be composed of alphabetic characters (both upper
   * and lowercase) and any other extra characters specified as a char[].
   */
  public static boolean isAllAlphaAndSpecialChars(String inputString, 
      char... characters)
  {
    return isAll(inputString, true, true, false, characters);
  }

  /**
   * A convenience version of isComposedOfArbitraryCharacters that assumes
   * that the String should be composed of numbers and any other extra
   * characters specified as a char[].
   */
  public static boolean isAllDigitsAndSpecialChars(String inputString, 
      char... characters)
  {
    return isAll(inputString, false, false, true, characters);
  }

  /**
   * A convenience version of isComposedOfArbitraryCharacters that assumes
   * that the String should be composed of numbers, letters (lower and upper
   * case), and any other extra characters specified as a char[].
   */
  public static boolean isAllAlphaDigitsAndSpecialChars(String inputString, 
      char... characters)
  {
    return isAll(inputString, true, true, true, characters);
  }

  /**
   * A convenience version of isComposedOfArbitraryCharacters allows that
   * the caller specify which of the standard arrays of lowercase, uppercase,
   * numeric digits should be used alongside the extra characters specified
   * as a char[].
   */
  public static boolean isAll(String inputString,
      boolean includeAlphaLowerCase,
      boolean includeAlphaUpperCase,
      boolean includeNumericDigits,
      char... characters)
  {
    // Merge the various char arrays we're using as acceptable characters.
    final char[] acceptableCharacters = CollectionHelper.arrayMerge(true,
      (includeAlphaLowerCase ? CharacterHelper.getAsciiLowercase() : null),
      (includeAlphaUpperCase ? CharacterHelper.getAsciiUppercase() : null),
      (includeNumericDigits ? CharacterHelper.getAsciiNumbers() : null),
      characters
    );

    return isAllArbitraryCharacters(inputString, acceptableCharacters);
  }
  
  /**
   * Returns true if the provided string is composed exclusively of 
   * characters that match the provided CharacterMatcher.
   *   <p>
   * Will return false if the inputString contains any character that does
   * not match the provided CharacterMatcher or if the inputString is null. 
   */
  public static boolean isAll(String inputString, 
      CharacterHelper.CharacterMatcher matcher) 
  {
    if (inputString != null)
    {
      for (int i = 0; i < inputString.length(); i++)
      {
        if (!matcher.matches(inputString.charAt(i)))
        {
          return false;
        }
      }
      
      return true;
    }
    
    return false;
  }

  /**
   * Determines if the provided String is composed entirely of characters
   * from the provided SORTED char array.  The char array must be sorted
   * since Arrays.binarySearch is used to find matching characters.
   *   <p>
   * Will return false for null input values.
   *   <p>
   * This method is effectively the opposite of isAllArbitraryExcluding.
   *
   * @param inputString An input String containing any data.
   *
   * @param acceptableCharacters A char[] of acceptable characters; if the
   *        inputString contains characters that are not in this array,
   *        false will be returned.
   *
   * @return true if all characters in the inputString are characters contained
   *         within the acceptableCharacters array.
   */
  public static boolean isAllArbitraryCharacters(String inputString,
    char... acceptableCharacters)
  {
    return isAll(inputString, 
        new CharacterHelper.ArbitraryCharacterMatcher(acceptableCharacters));
  }

  /**
   * Determines if the provided String is composed entirely of characters
   * NOT contained within the provided SORTED char array.  That is, true
   * will be returned if the inputString contains none of the characters in
   * the unacceptableCharacters array.  The char array must be sorted
   * since Arrays.binarySearch is used to find matching characters.
   *   <p>
   * Will return false for null input values.
   *   <p>
   * This method is effectively the opposite of isAllArbitraryCharacters.
   *
   * @param inputString An input String containing any data.
   *
   * @param unacceptableCharacters A char[] of unacceptable characters; if the
   *        inputString contains characters that are in this array, false will
   *        be returned.
   *
   * @return true if all characters in the inputString are characters not
   *         contained within the unacceptableCharacters array.
   */
  public static boolean isAllArbitraryExcluding(String inputString,
    char... unacceptableCharacters)
  {
    return isAll(inputString,
        new CharacterHelper.CharacterRejector(
            new CharacterHelper.ArbitraryCharacterMatcher(unacceptableCharacters)));
  }

  /**
   * Trims a String to a specified length, if the String exceeds that
   * length, otherwise leaves it alone.  "apple" truncated to 4 characters
   * becomes "appl".
   */
  public static String truncate(String input, int length)
  {
    if (input != null)
    {
      if (input.length() > length)
      {
        return input.substring(0, length);
      }
      else
      {
        return input;
      }
    }
    else
    {
      return null;
    }
  }
  
  /**
   * Trims a String to a specified length, starting at the end of the String
   * rather than the beginning.  Thus, "apple" truncated to 4 characters
   * becomes "pple" (rather than "appl" as with the plain truncate method).
   */
  public static String truncateAtEnd(String input, int length)
  {
    if (input != null)
    {
      if (input.length() > length)
      {
        return input.substring(input.length() - length, input.length());
      }
      else
      {
        return input;
      }
    }
    else
    {
      return null;
    }
  }

  /**
   * Trims a String to a specified length, if the String exceeds that
   * length, otherwise leaves it alone.  When truncation occurs, an ellipsis
   * will be added to the end.
   */
  public static String truncateEllipsis(String input, int length)
  {
    return truncateEllipsis(input, length, false);
  }

  /**
   * Trims a String to a specified length, if the String exceeds that
   * length, otherwise leaves it alone.  If findWordBoundary is true,
   * will work backwards from the target length to a space character and
   * truncate there (to avoid truncating in the middle of a word).
   */
  public static String truncateEllipsis(String input, int length,
    boolean findWordBoundary)
  {
    return truncateEllipsis(input, length, findWordBoundary, "...");
  }

  /**
   * Trims a String to a specified length, if the String exceeds that
   * length, otherwise leaves it alone.  If findWordBoundary is true,
   * will work backwards from the target length to a space character and
   * truncate there (to avoid truncating in the middle of a word). Puts the
   * specified ellipsis text at the end of the String if it is trimmed.
   */
  public static String truncateEllipsis(String input, int length,
    boolean findWordBoundary, String ellipsis)
  {
    return truncateEllipsis(input, length, findWordBoundary, true, ellipsis);
  }

  /**
   * Trims a String to a specified length, if the String exceeds that length,
   * otherwise leaves it alone. If findWordBoundary is true, will work
   * backwards (or forwards, if searchBackForWordBoundary is false) from the
   * target length to a space character and truncate there (to avoid
   * truncating in the middle of a word). Puts the specified ellipsis text at
   * the end of the String if it is trimmed.
   */
  public static String truncateEllipsis(String input, int targetLength,
    boolean findWordBoundary, boolean searchBackForWordBoundary, String ellipsis)
  {
    // Check for an empty string.
    if (isEmpty(input))
    {
      return "";
    }

    // Don't let it be null.
    String suffix = ellipsis == null ? "" : ellipsis;

    int length = targetLength;
    if (length > suffix.length())
    {
      length = length - suffix.length();
    }

    // If the input requires truncation...
    if (input.length() > length)
    {
      // If we're looking to truncate at a word boundary, look for the previous
      // or next space character, depending on searchBackForWordBoundary.
      if (findWordBoundary)
      {
        int space = searchBackForWordBoundary ? input.lastIndexOf(' ', length) 
            : input.indexOf(' ', length);
        if (space >= 1)
        {
          length = space;
        }
      }

      return input.substring(0, length) + suffix;
    }
    else
    {
      return input;
    }
  }

  /**
   * Word-wraps a String of text.  Strips newlines from input string.
   * 
   * @param input The input string to be wrapped.
   * @param width The character width of an acceptable line of text.
   * @param indentFirstLine A number of spaces to indent the first line,
   *        bounded from 0 to 80.
   * @param indentSubsequentLines A number of spaces to indent all other
   *        lines, bounded from 0 to 80.
   */
  public static String wordWrap(String input, int width,
    int indentFirstLine, int indentSubsequentLines)
  {
    return wordWrap(input, width, 0, indentFirstLine, indentSubsequentLines);
  }
  
  /**
   * Word-wraps a String of text.  Strips newlines from input string.
   * 
   * @param input The input string to be wrapped.
   * @param width The character width of an acceptable line of text.
   * @param preexistingFirstLineOffset A number of characters assumed to
   *        have been printed prior to the text being word-wrapped, causing
   *        an initial line offset that we are not aware of.
   * @param indentFirstLine A number of spaces to indent the first line,
   *        bounded from 0 to 80.
   * @param indentSubsequentLines A number of spaces to indent all other
   *        lines, bounded from 0 to 80.
   */
  public static String wordWrap(final String input, final int width,
    final int preexistingFirstLineOffset, final int indentFirstLine, 
    final int indentSubsequentLines)
  {
    if (input == null)
    {
      return null;
    }
    else
    {
      final int flOffset = NumberHelper.boundInteger(preexistingFirstLineOffset, SPACES_RANGE);
      final int flIndent = NumberHelper.boundInteger(indentFirstLine, SPACES_RANGE);
      final int slIndent = NumberHelper.boundInteger(indentSubsequentLines, SPACES_RANGE);
      
      final StringBuilder result = new StringBuilder(input.length() * 2);
      final String cleaned = replaceSubstrings(input, NEWLINE_FINDS, NEWLINE_REPLACES);

      int lineNumber = 0;
      int lineWidth = NumberHelper.boundInteger(width - flOffset, 1, width);
      
      int currentIndent = flIndent;
      int currentPosition = 0;

      while (cleaned.length() > (currentPosition + lineWidth - currentIndent))
      {
        int space = cleaned.lastIndexOf(' ', currentPosition + lineWidth - currentIndent);
        int distance = space - currentPosition;

        // Not the last line.
        if ( (space > currentPosition)
          && (distance < currentIndent + lineWidth)
          )
        {
          if (currentIndent > 0)
          {
            result.append(SPACES.substring(0, currentIndent));
          }
          result.append(cleaned.substring(currentPosition, currentPosition + distance));
          currentPosition = space + 1;
          while ((cleaned.length() > currentPosition) && (cleaned.charAt(currentPosition) == ' '))
          {
            currentPosition++;
          }
          if (cleaned.length() > currentPosition)
          {
            result.append(UtilityConstants.CRLF);
            lineNumber++;
          }
        }

        // Last line.
        else
        {
          if (currentIndent > 0)
          {
            result.append(SPACES.substring(0, currentIndent));
          }
          result.append(cleaned.substring(currentPosition, 
              currentPosition + lineWidth - currentIndent));
          currentPosition = currentPosition + lineWidth - currentIndent;
          while ( (cleaned.length() > currentPosition) 
               && (cleaned.charAt(currentPosition) == ' '))
          {
            currentPosition++;
          }
          if (cleaned.length() > currentPosition)
          {
            result.append(UtilityConstants.CRLF);
            lineNumber++;
          }
        }

        if (lineNumber == 1)
        {
          currentIndent = slIndent;
          lineWidth = width;
        }
      }

      // Handle remainder.
      if (cleaned.length() > currentPosition)
      {
        if (currentIndent > 0)
        {
          result.append(SPACES.substring(0, currentIndent));
        }
        result.append(cleaned.substring(currentPosition));
      }

      return result.toString();
    }
  }

  /**
   * Splits a String using the provided regular expression and then trims
   * each resulting string in the returned array.
   */
  public static String[] splitAndTrim(String s, String regex)
  {
    String[] toReturn = {};

    if (isNonEmpty(s))
    {
      toReturn = trim(s.split(regex));
    }

    return toReturn;
  }

  /**
   * Splits a String using the provided regular expression and then trims
   * and sets them all chars to lower case and each resulting string in 
   * the returned array.
   */
  public static String[] splitTrimAndLower(String s, String regex)
  {
    String[] toReturn = {};

    if (isNonEmpty(s))
    {
      toReturn = trim(s.toLowerCase().split(regex));
    }

    return toReturn; 
  }

  /**
   * Splits a String by line breaks and trims the resulting lines.
   */
  public static String[] splitIntoLines(String s)
  {
    return splitAndTrim(s, "\r\n|\n");
  }

  /**
   * Splits a string by whitespace.  For example:
   * <pre>
   * StringHelper.split(" foo   bar\n baz  "); // [ "foo", "bar", "baz" ]
   * </pre>
   */
  public static String[] splitIntoWords(String s)
  {
    if (s == null)
    {
      return new String[0];
    }
    
    String work = s.trim();
    
    if (work.length() == 0)
    {
      return new String[0];
    }
    
    return work.split("\\s+");
  }

  /**
   * Removes excess blank lines from a String array, "compressing" the array
   * to contain only non-empty lines separated by at most one blank line.
   *   <p>
   * If the parameter is null, null will be returned. 
   * 
   * @param lines the array of lines to compress.
   */
  public static String[] trimExcessBlankLines(String[] lines)
  {
    if (lines == null)
    {
      return null;
    }
    
    boolean prevBlank = false;
    final List<String> work = new ArrayList<>(lines.length);
    for (String line : lines)
    {
      if (line.length() == 0)
      {
        if (!prevBlank)
        {
          work.add(line);
          prevBlank = true;
        }
      }
      else
      {
        work.add(line);
        prevBlank = false;
      }
    }
    
    String[] toReturn = new String[work.size()];
    return work.toArray(toReturn);
  }

  /**
   * Parses a String representing a boolean value. If the String does not
   * represent a valid boolean value then a NumberFormatException is thrown.
   *
   * This method will accept a number of common representations for booleans:
   * true, yes, 1, y, on
   * false, no, 0, n, off
   *
   * @param boolStr The String to parse.
   * @return The parsed boolean value.
   * @throws NumberFormatException if the parameter cannot be parsed.
   */
  public static boolean parseBoolean(String boolStr)
  {
    if (  (boolStr != null)
       && (boolStr.length() < 50)  // Long strings won't be evaluated. 
       )
    {
      switch (boolStr.trim().toLowerCase())
      {
        case "true":
        case "yes":
        case "1":
        case "y":
        case "on":
          return true;
        case "false":
        case "no":
        case "0":
        case "n":
        case "off":
          return false;
      }
    }

    throw new NumberFormatException("'" + boolStr + "' is not a valid boolean value.");
  }

  /**
   * Parses a String representing a boolean value. If the String does not
   * represent a valid boolean value then the defaultValue is returned.
   *
   * This method will accept a number of common representations for booleans.
   *
   * @param boolStr The String to parse.
   * @return The parsed boolean value.
   */
  public static boolean parseBoolean(String boolStr, boolean defaultValue)
  {
    try
    {
      return parseBoolean(boolStr);
    }
    catch (NumberFormatException e)
    {
      return defaultValue;
    }
  }
  
  /**
   * Strictly parses a String representing a boolean value, accepting only
   * "true" or "false", but permitting a default value if anything else is
   * provided.
   */
  public static boolean parseBooleanStrict(String boolStr, boolean defaultValue)
  {
    if (defaultValue)
    {
      return !("false".equals(boolStr));
    }
    else
    {
      return ("true".equals(boolStr));
    }
  }

  /**
   * Strips out all ISO control characters from an input string and optionally
   * replaces them with the replaceWith parameter.
   *
   * @param inputString the input string
   * @param replaceWith an optional String used to replace the stripped 
   *        characters.
   *
   * @return a String composed of all of the non-ISO control characters in
   *         the original String, in order.
   */
  public static String stripISOControlCharacters(String inputString, 
      String replaceWith)
  {
    if (isNonEmpty(inputString))
    {
      StringBuilder result = new StringBuilder(inputString.length());

      char aChar;

      // Go through original string.
      for (int i = 0; i < inputString.length(); i++)
      {
        aChar = inputString.charAt(i);

        // We have an ISO Control character.
        if (Character.isISOControl(aChar))
        {
          // If replaceWith is provided then add it.
          if (isNonEmpty(replaceWith))
          {
            result.append(replaceWith);
          }
        }
        // Otherwise append to result string if non-ISO.
        else
        {
          result.append(aChar);
        }
      }

      return result.toString();
    }
    else
    {
      return inputString;
    }
  }

  /**
   * Strips out non-alphabetic characters from an input string.  Returns
   * null if the parameter is null.
   *
   * @param inputString the input string
   *
   * @return a String composed of all of the alphabetic characters in
   *         the original String, in order.
   */
  public static String stripNonAlphabetic(String inputString)
  {
    return stripArbitraryCharacters(inputString, 
        CharacterHelper.asciiLetterMatcher);
  }

  /**
   * Strips out non-alphanumeric characters from an input string.  Returns
   * null if the parameter is null.
   *
   * @param inputString the input string
   *
   * @return a String composed of all of the alphanumeric characters in
   *         the original String, in order.
   */
  public static String stripNonAlphanumeric(String inputString)
  {
    return stripArbitraryCharacters(inputString, 
        CharacterHelper.asciiLetterOrNumberMatcher);
  }

  /**
   * Strip all characters from an input string that are not traditional 
   * Arabic numerals (0 through 9).  Returns the input string unchanged if
   * it is null or already contains only numeric characters.
   *
   * @param inputString the input string
   *
   * @return a String composed of all of the numeric characters in
   *         the original String, in their original order.
   */
  public static String stripNonNumeric(String inputString)
  {
    return stripArbitraryCharacters(inputString, 
        CharacterHelper.asciiNumberMatcher);
  }

  /**
   * Strips arbitrary characters from a String.  The acceptableCharacters
   * array is assumed to be sorted.  You can sort the array either by calling
   *   Arrays.sort(char[])
   * or
   *   StringHelper.mergeArrays(true, char[][])
   *   <p>
   * It is okay for the acceptableCharacters array to have repeats as long
   * as it is sorted.  (E.g., 'a', 'b', 'b', 'c' is okay.)
   *   <p>
   * This method is effectively the opposite of stripUnacceptableCharacters
   * which takes an array of <b>unacceptable</b> characters.
   *
   * @param inputString the input string
   * @param acceptableCharacters an array of characters that should remain
   *        in the string; all other characters will be removed.  This array
   *        is assumed to be sorted.
   */
  public static String stripArbitraryCharacters(String inputString,
    char... acceptableCharacters)
  {
    return stripArbitraryCharacters(inputString, 
        new CharacterHelper.ArbitraryCharacterMatcher(acceptableCharacters));
  }

  /**
   * Strips arbitrary characters from a String.  The unacceptableCharacters
   * array is assumed to be sorted.  You can sort the array either by calling
   *   Arrays.sort(char[])
   * or
   *   StringHelper.mergeArrays(true, char[][])
   *   <p>
   * It is okay for the unacceptableCharacters array to have repeats as long
   * as it is sorted.  (E.g., 'a', 'b', 'b', 'c' is okay.)
   *   <p>
   * This method is effectively the opposite of stripArbitraryCharacters
   * which takes an array of <b>acceptable</b> characters.
   *
   * @param inputString the input string
   * @param unacceptableCharacters an array of characters that should be
   *        removed from the string; all other characters will be retained.
   *        This array is assumed to be sorted.
   */
  public static String stripUnacceptableCharacters(String inputString,
    char... unacceptableCharacters)
  {
    return stripArbitraryCharacters(inputString,
        new CharacterHelper.CharacterRejector(
            new CharacterHelper.ArbitraryCharacterMatcher(unacceptableCharacters)));
  }
  
  /**
   * Strip all characters that do not match a provided CharacterMatcher.
   * 
   * @param inputString the input string from which to strip characters.
   *        Will be returned as-is if the string includes no non-matching
   *        characters.
   * 
   * @param characterMatcher an implementation of CharacterMatcher that will
   *        be used to evaluate each character of the input string.
   *        
   * @return a String composed of all characters that match the provided
   *         CharacterMatcher.  If the inputString is composed exclusively of
   *         characters that match the characterMatcher, the inputString will
   *         be returned as-is.
   */
  public static String stripArbitraryCharacters(String inputString, 
      CharacterHelper.CharacterMatcher characterMatcher)
  {
    if (isNonEmpty(inputString))
    {
      int index = 0;
      StringBuilder result = null;
      
      while (index < inputString.length())
      {
        final char ch = inputString.charAt(index);
        final boolean match = characterMatcher.matches(ch);
        
        if (  (result == null)
           && (!match)
           )
        {
          // If we have not yet created a result buffer, it is because we
          // have not yet seen a character that doesn't match.  If this 
          // character is the first we've seen that does not match, we need 
          // to create a result buffer and copy all characters up until this
          // index because they are assumed to have matched.
          // We know this current character does not match, hence we 
          // subtract 1 from the maximum length.
          result = new StringBuilder(inputString.length() - 1);
          if (index > 0)
          {
            result.append(inputString.substring(0, index));
          }
        }
        else if (  (result != null)
                && (match)
                )
        {
          result.append(ch);
        }
        
        index++;
      }
      
      // If we created a result buffer, return it rendered to a String.
      // Otherwise, we'll return the input string unmodified.
      if (result != null)
      {
        return result.toString();
      }
    }
    
    return inputString;
  }

  /**
   * Strips extraneous spaces from a String, leaving only one space between
   * words.  Trims all spaces from the start and end of the String as well.
   *
   * @param inputString the input string
   *
   * @return a String with any extra spacing removed.
   */
  public static String stripExtraSpaces(String inputString)
  {
    if (isNonEmpty(inputString))
    {
      String trimmed = inputString.trim();

      StringBuilder result = new StringBuilder(trimmed.length());
      char aChar;
      for (int i = 0; i < trimmed.length(); i++)
      {
        aChar = trimmed.charAt(i);

        // If the character isn't a space, add it.
        if (aChar != ' ')
        {
          result.append(aChar);
        }

        // Otherwise, if the last added character wasn't a space, add the
        // space.
        else if ( (result.length() > 0)
               && (result.charAt(result.length() - 1) != ' ')
               )
        {
          result.append(' ');
        }
      }

      return result.toString();
    }
    else 
    {
      return inputString;
    }
  }

  /**
   * Strips doubled double-quotes (reversing the process of escapeDoubleQuotes).
   * Turns ["test ""test"""""] into ["test "test"""].
   *   <p>
   * Unlike the other strip methods, if the parameter is null, an empty String
   * will be returned.
   *
   * @param source the source String.
   */
  public static String stripDoubleDoubleQuotes(String source)
  {
    return replaceSubstrings(source, "\"\"", "\"");
  }

  /**
   * Performs a simple macro expansion on a String.  The implementation 
   * converts the Map to two parallel String arrays and calls 
   * replaceSubstrings.  For optimal performance, client code should call
   * replaceSubstrings directly.
   *
   * @param macros A map where the keys are macro-codes (e.g., "$UN")
   *   and the values specify what to replace the macro with (e.g.,
   *   "username").
   * @param text The original text with the macro-codes inside; Not all
   *   macro codes need to be present in the text (in fact, none have to
   *   be present at all).
   *
   * @return a String composed of the input text with all macros expanded.
   */
  public static String macroExpand(Map<String, ? extends Object> macros, 
      String text)
  {
    // If macros is undefined, then return the text unaltered.
    if (macros == null)
    {
      return text;
    }

    String[] find    = new String[macros.size()];
    String[] replace = new String[macros.size()];

    Object value;
    int position = 0;
    for (Map.Entry<String, ? extends Object> entry : macros.entrySet())
    {
      find[position] = entry.getKey();

      // Replace null values with empty String replacements.
      value = entry.getValue();
      replace[position++] = (value != null ? value.toString() : "");
    }

    return replaceSubstrings(text, text, find, replace);
  }

  /**
   * Escapes single quotes as double-single quotes.  This is mostly useful
   * for escaping single quotes for a database query.  Strings that contain
   * single quotes can often break database queries unless they are escaped
   * using this method.  If a null is passed in as a parameter, an empty
   * String is returned.
   *
   * @param inputString a String needing some single quotes escaped
   * @return a String with the single quotes escaped--woohoo!
   */
  public static String escapeSingleQuotes(String inputString)
  {
    return replaceSubstrings(inputString, 
        SINGLE_QUOTE_ESCAPE_FINDS, 
        SINGLE_QUOTE_ESCAPE_REPLACES);
  }

  /**
   * Escapes double quotes as double-double quotes.
   */
  public static String escapeDoubleQuotes(String inputString)
  {
    if (inputString != null)
    {
      StringBuilder newString      = new StringBuilder(inputString.length());
      int           start          = 0;
      int           quoteLocation  = inputString.indexOf('\"', start);

      // While we've found a quote, continue...
      while (quoteLocation > -1)
      {
        newString.append(inputString.substring(start, quoteLocation));
        newString.append("\"\"");
        start = quoteLocation + 1;
        quoteLocation = inputString.indexOf('\"', start);
      }
      newString.append(inputString.substring(start));

      return newString.toString();
    }

    return "";
  }

  /**
   * A replace operation that matches and replaces substrings within a
   * String as opposed to the single character replacement found in
   * java.lang.String.
   *
   * The default is to ignore case.
   */
  public static String replaceSubstrings(String source, String search,
    String replacement)
  {
    return replaceSubstrings(source, search, replacement, true);
  }

  /**
   * See replaceSubstrings(String, String, String).
   */
  public static String replaceSubstrings(String source, String search,
    String replacement, boolean ignoreCase)
  {
    if (ignoreCase)
    {
      return replaceSubstringsIgnoreCase(source, new String[] { search },
        new String[] { replacement });
    }
    else
    {
      return replaceSubstrings(source, new String[] { search },
        new String[] { replacement });
    }
  }

  /**
   * Replaces multiple substrings in parallel.  Use this version of
   * this method if your search string is identical as your source string
   * (this is most often the case).
   */
  public static String replaceSubstrings(String source, String[] find,
    String[] replace)
  {
    return replaceSubstrings(source, source, find, replace);
  }

  /**
   * Same as replaceSubstrings(String, String[], String[]) except that
   * this version ignores case.  This is accomplished by converting the
   * source string and each of the find elements to upper-case before
   * calling the actual routine.
   */
  public static String replaceSubstringsIgnoreCase(String source,
    String[] find, String[] replace)
  {
    // We can't convert the source to uppercase if it's null, so we return
    // an empty string as the full replaceSubstrings does when it sees a
    // null source.
    if (source == null)
    {
      return "";
    }
    
    String uppercaseSearch = source.toUpperCase();

    String[] upperFind = new String[find.length];
    for (int i = 0; i < find.length; i++)
    {
      upperFind[i] = find[i].toUpperCase();
    }

    return replaceSubstrings(source, uppercaseSearch, upperFind, replace);
  }

  /**
   * Replaces multiple substrings in parallel.  The source String is
   * specified separately from the String used for searching.  This
   * allows the search string to be all-uppercase, for instance.  The
   * two strings should be identical in length, otherwise an array bounds
   * exception will probably result.
   *
   * @param source the source string to copy content from
   * @param search the source string to search for find elements within
   * @param find the elements to find
   * @param replace peers to find; what to replace each with
   */
  public static String replaceSubstrings(String source, String search,
    String[] find, String[] replace)
  {
    if (source == null)
    {
      return "";
    }
    if (search == null || find == null || replace == null)
    {
      return source;
    }

    StringBuilder buffer = new StringBuilder(source.length());

    int position = 0;
    int selection;
    int foundAtPosition;
    int currentFound;

    // While something has been found in the source string, continue...
    while (position < source.length())
    {
      selection = -1;
      foundAtPosition = -1;

      // Loop through each of the find elements.
      for (int findIndex = 0;
           (findIndex < find.length) && (findIndex < replace.length);
           findIndex++)
      {
        currentFound = search.indexOf(find[findIndex], position);

        // If the currently found element is earlier in the string
        // than the last we found...
        if ( (currentFound >= 0)
          && ( (currentFound < foundAtPosition)
            || (foundAtPosition == -1)
            )
          )
        {
          // Record the selection index and where we found this selection.
          foundAtPosition = currentFound;
          selection = findIndex;
        }
      }

      // Did we find any of the find strings?
      if (selection >= 0)
      {
        buffer.append(source.substring(position, foundAtPosition));
        buffer.append(replace[selection]);
        position = foundAtPosition + find[selection].length();
      }
      else
      {
        // No?  Stop.
        buffer.append(source.substring(position));
        position = source.length();
      }
    }

    return buffer.toString();
  }

  /**
   * Replaces characters in a String.  If finds[1] is found, it will be
   * replaced with replacements[1].
   *
   * @param inputString The String to modify.
   * @param finds The characters to find.
   * @param replacements The characters to use as replacements.
   */
  public static String replaceCharacters(String inputString, char[] finds,
    char[] replacements)
  {
    char[] buffer = inputString.toCharArray();
    for (int i = 0; i < buffer.length; i++)
    {
      for (int f = 0; f < finds.length; f++)
      {
        if (buffer[i] == finds[f])
        {
          buffer[i] = replacements[f];
          break;
        }
      }
    }
    
    return new String(buffer);
  }

  public static final RandomStringGenerator randomString = 
      new RandomStringGenerator(new Random(), false);
  public static final RandomStringGenerator secureRandomString =
      new RandomStringGenerator(new SecureRandom(), true);
  
  public static class RandomStringGenerator
  {
    private static final char[] RANDOM_PATTERN_a = UtilityConstants.ASCII_LOWERCASE.toCharArray();
    private static final char[] RANDOM_PATTERN_A = (UtilityConstants.ASCII_LOWERCASE + UtilityConstants.ASCII_UPPERCASE).toCharArray();
    private static final char[] RANDOM_PATTERN_U = UtilityConstants.ASCII_UPPERCASE.toCharArray();
    private static final char[] RANDOM_PATTERN_n = (UtilityConstants.ASCII_LOWERCASE + UtilityConstants.ASCII_DIGITS).toCharArray();
    private static final char[] RANDOM_PATTERN_N = (UtilityConstants.ASCII_UPPERCASE + UtilityConstants.ASCII_DIGITS).toCharArray();
    private static final char[] RANDOM_PATTERN_d = UtilityConstants.ASCII_DIGITS.toCharArray();
    private static final char[] RANDOM_PATTERN_x = (UtilityConstants.ASCII_LOWERCASE + UtilityConstants.ASCII_UPPERCASE + UtilityConstants.ASCII_DIGITS).toCharArray();
    private static final char[] RANDOM_PATTERN_s = ("-=/\\[]()!@#$&").toCharArray();
    /** 
     * Characters suitable for passwords.  Notably, commonly mistaken characters
     * such as lowercase 'l' and '1', uppercase 'O' and '0' are omitted. 
     */
    private static final char[] RANDOM_PATTERN_p = (
          "ABCDEFGHJKLMNPQRSTUVWXYZ"
        + "abcdefghijkmnopqrstuvwxyz"
        + "23456789").toCharArray();
    
    private final Random random;
    private final boolean isSecure;
    
    private RandomStringGenerator(Random random, boolean isSecure)
    {
      this.random = random;
      this.isSecure = isSecure;
    }

    /**
     * Generates a random String composed of the characters specified in the 
     * given character array.
     */
    public String fromChars(char[] chars, int length)
    {
      char[] buffer = new char[length];
      for (int i = 0; i < length; i++)
      {
        buffer[i] = chars[random.nextInt(chars.length)];
      }
      
      return new String(buffer);
    }

    /**
     * Generates a random String composed of the characters specified in the 
     * given String.
     */
    public String fromChars(String chars, int length)
    {
      return fromChars(chars.toCharArray(), length);
    }

    /**
     * Generates a random String composed of mixed case alphanumeric characters.
     */
    public String alphanumeric(int length)
    {
      return fromChars(UtilityConstants.ASCII_DIGITS 
          + UtilityConstants.ASCII_LOWERCASE 
          + UtilityConstants.ASCII_UPPERCASE, length);
    }

    /**
     * Generates a random String composed of numeric characters.
     */
    public String numeric(int length)
    {
      return fromChars(UtilityConstants.ASCII_DIGITS, length);
    }

    /**
     * Generates a random String composed of uppercase characters.
     */
    public String uppercase(int length)
    {
      return fromChars(UtilityConstants.ASCII_UPPERCASE, length);
    }

    /**
     * Generates a random String composed of lowercase characters.
     */
    public String lowercase(int length)
    {
      return fromChars(UtilityConstants.ASCII_LOWERCASE, length);
    }

    /**
     * Generates a random String composed of characters suitable for a simple
     * password.  These are alphanumeric mixed case but omit '0' 'O' 'l' '1'.
     * Will return an UnsupportedException if the underlying random number
     * generator is not secure.
     */
    public String password(int length)
    {
      if (isSecure)
      {
        return fromChars(RANDOM_PATTERN_p, length);
      }
      else
      {
        throw new UnsupportedOperationException(
            "Random password generation is not provided by the insecure random string generator.");
      }
    }
    
    /**
     * Generates a random String composed of characters from the standard ASCII
     * set.  Includes mixed case alphanumeric and standard non-whitespace, 
     * non-special symbols.
     */
    public String alphanumericAndSymbols(int length)
    {
      return fromChars(UtilityConstants.ASCII_DIGITS 
          + UtilityConstants.ASCII_LOWERCASE 
          + UtilityConstants.ASCII_UPPERCASE 
          + UtilityConstants.ASCII_SYMBOLS, 
          length);
    }
    
    /**
     * Generates a random string using a specified pattern.  The pattern
     * can be made up of:
     *   a = lowercase letter
     *   A = lowercase or uppercase letter
     *   U = uppercase letter
     *   n = lowercase or number
     *   N = uppercase or number
     *   d = number only (digit)
     *   x = lowercase, uppercase, or number (default)
     *   s = simple symbols: - = / \ [ ] ( ) ! @ # $ &amp;
     */
    public String fromPattern(String pattern)
    {
      char[] buffer = new char[pattern.length()];
      
      for (int i = 0; i < pattern.length(); i++)
      {
        switch (pattern.charAt(i))
        {
          case ('a'): buffer[i] = (RANDOM_PATTERN_a[random.nextInt(RANDOM_PATTERN_a.length)]); break; 
          case ('A'): buffer[i] = (RANDOM_PATTERN_A[random.nextInt(RANDOM_PATTERN_A.length)]); break; 
          case ('U'): buffer[i] = (RANDOM_PATTERN_U[random.nextInt(RANDOM_PATTERN_U.length)]); break; 
          case ('n'): buffer[i] = (RANDOM_PATTERN_n[random.nextInt(RANDOM_PATTERN_n.length)]); break; 
          case ('N'): buffer[i] = (RANDOM_PATTERN_N[random.nextInt(RANDOM_PATTERN_N.length)]); break; 
          case ('d'): buffer[i] = (RANDOM_PATTERN_d[random.nextInt(RANDOM_PATTERN_d.length)]); break; 
          case ('s'): buffer[i] = (RANDOM_PATTERN_s[random.nextInt(RANDOM_PATTERN_s.length)]); break;
          case ('p'): buffer[i] = (RANDOM_PATTERN_p[random.nextInt(RANDOM_PATTERN_p.length)]); break;
          default: buffer[i] = (RANDOM_PATTERN_x[random.nextInt(RANDOM_PATTERN_x.length)]); break; 
        }
      }

      return new String(buffer);
    }
  }
  
  /**
   * Compares the two strings ignoring case and avoiding null pointer exceptions.
   * If both strings are <tt>null</tt>, this method returns 0.  Otherwise,
   * if a is <tt>null</tt>, -1 is returned. Otherwise, if b is null, 1 is returned.
   * If neither is <tt>null</tt>, the result of <tt>a.compareToIgnoreCase(b)</tt>
   * is returned.
   *
   * @param a a string
   * @param b a string
   * @return a negative integer, zero, or a positive integer as
   *    the first string is less than, equal to, or greater than
   *    the second string ignoring case
   */
  public static int compareToIgnoreCase(String a, String b)
  {
    return a == null ? b == null ? 0 : -1 : b == null ? 1 : a.compareToIgnoreCase(b);
  }
  
  /**
   * Compares two strings in a null-safe manner.  If both strings are 
   * <tt>null</tt>, this method returns 0.  Otherwise, if a is <tt>null</tt>,
   * -1 is returned. Otherwise, if b is null, 1 is returned. If neither is
   * <tt>null</tt>, the result of <tt>a.compareTo(b)</tt> is returned.
   */
  public static int compareToNullSafe(String a, String b)
  {
    return a == null ? b == null ? 0 : -1 : b == null ? 1 : a.compareTo(b);
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, Iterable<?> values)
  {
    if (values == null)
    {
      return "";
    }
    Iterator<?> iter = values.iterator();
    if (!iter.hasNext())
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(iter.next());
    while (iter.hasNext())
    {
      sb.append(separator).append(iter.next());
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  @SafeVarargs
  public static <E> String join(String separator, E... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }
  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, String... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, char... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, byte... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, short... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, int... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, long... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, float... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, double... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * Combines the values into a String.
   * 
   * @param separator a string separating each value in the result
   * @param values the values to be joined
   */
  public static String join(String separator, boolean... values)
  {
    if (values == null || values.length == 0)
    {
      return "";
    }
    StringBuilder sb = new StringBuilder().append(values[0]);
    for (int i = 1; i < values.length; i++)
    {
      sb.append(separator).append(values[i]);
    }
    return sb.toString();
  }

  /**
   * You may not instantiate this class.
   */
  private StringHelper()
  {
    // Does nothing.
  }

}  // End StringHelper.

