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

/**
 * CharacterHelper provides utility functions for working with characters.
 */
public class CharacterHelper 
{

  /**
   * Gets all lower-case letters of the ASCII/English alphabet.
   */
  public static final char[] getAsciiLowercase() {
    return new char[] { 
        'a','b','c','d','e','f',
        'g','h','i','j','k','l',
        'm','n','o','p','q','r',
        's','t','u','v','w','x',
        'y','z'
    };
  }
  
  /**
   * Gets all upper-case letters of the ASCII/English alphabet.
   */
  public static final char[] getAsciiUppercase() {
    return new char[] {
        'A','B','C','D','E','F',
        'G','H','I','J','K','L',
        'M','N','O','P','Q','R',
        'S','T','U','V','W','X',
        'Y','Z'
    };
  }

  /**
   * Gets all ASCII/Arabic numeric digit characters.
   */
  public static final char[] getAsciiNumbers() {
    return new char[] {
        '0','1','2','3','4','5','6','7','8','9'
    };
  }

  /**
   * Is a character a traditional ASCII/English uppercase or lowercase letter?
   */
  public static boolean isAsciiLetter(char ch)
  {
    return (  ((ch >= 'a') && (ch <= 'z'))
           || ((ch >= 'A') && (ch <= 'Z'))
           );
  }

  /**
   * Is a character a traditional ASCII/Arabic number?
   */
  public static boolean isAsciiNumber(char ch)
  {
    return ((ch >= '0') && (ch <= '9'));
  }
  
  /**
   * Is a character a traditional ASCII uppercase letter, lowercase letter,
   * or number?
   */
  public static boolean isAsciiLetterOrNumber(char ch)
  {
    return (  ((ch >= 'a') && (ch <= 'z'))
           || ((ch >= 'A') && (ch <= 'Z'))
           || ((ch >= '0') && (ch <= '9'))
           );
  }
  
  /**
   * A simple interface for matching characters.
   */
  public static interface CharacterMatcher {
    boolean matches(char ch);
  }
  
  /**
   * A character matcher for an arbitrary array of characters, where the array
   * is in sorted order.
   */
  public static class ArbitraryCharacterMatcher implements CharacterMatcher {
    private final char[] acceptable;
    
    /**
     * Construct an ArbitraryCharacterMatcher from a <b>sorted</b> array 
     * of characters. 
     */
    public ArbitraryCharacterMatcher(char... acceptableCharacters) {
      acceptable = acceptableCharacters;
    }
    
    /**
     * Construct an ArbitraryCharacterMatcher from an array of characters.
     * 
     * @param acceptableCharacters An array of characters that is normally
     *        already sorted. If the array is not yet sorted, be sure to 
     *        set the <b>sortArray</b> parameter to true.
     * @param sortArray set to true if this constructor should sort the
     *        acceptableCharacters array for you.
     */
    public ArbitraryCharacterMatcher(boolean sortArray, char... acceptableCharacters) {
      acceptable = new char[acceptableCharacters.length];
      System.arraycopy(acceptableCharacters, 0, acceptable, 0, acceptable.length);
      if (sortArray) {
        Arrays.sort(acceptable);
      }
    }
    
    @Override
    public boolean matches(char ch) {
      return (Arrays.binarySearch(acceptable, ch) >= 0);
    }
  }
  
  /**
   * An inverse character matcher; returns true if a character does not match,
   * and false if it does.
   */
  public static class CharacterRejector implements CharacterMatcher {
    private final CharacterMatcher next;
    public CharacterRejector(CharacterMatcher next) {
      this.next = next;
    }
    
    @Override
    public boolean matches(char ch) {
      return !next.matches(ch);
    }
  }
  
  /**
   * A character matcher for traditional ASCII/English letters. 
   */
  public static final CharacterMatcher asciiLetterMatcher = new CharacterMatcher() {
    @Override
    public boolean matches(char ch) {
      return isAsciiLetter(ch);
    }
  };
  
  /**
   * A character matcher for traditional ASCII/Arabic numbers.
   */
  public static final CharacterMatcher asciiNumberMatcher = new CharacterMatcher() {
    @Override
    public boolean matches(char ch) {
      return isAsciiNumber(ch);
    }
  };
  
  /**
   * A character matcher for traditional ASCII letters and numbers.
   */
  public static final CharacterMatcher asciiLetterOrNumberMatcher = new CharacterMatcher() {
    @Override
    public boolean matches(char ch) {
      return isAsciiLetterOrNumber(ch);
    }
  }; 
  
  /**
   * You may not instantiate this class.
   */
  private CharacterHelper() { }
  
}
