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

import java.util.*;

import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * Generates random text (words, sentences, paragraphs, multi-paragraph 
 * bodies), suitable for testing scenarios.  The text is just random
 * sequences of English characters, but not built with any real words.
 */
public class RandomTextGenerator 
{

  private static final IntRange DEFAULT_WORD_LENGTH = new IntRange(1, 15);
  private static final IntRange DEFAULT_SENTENCE_LENGTH = new IntRange(1, 15);
  private static final IntRange DEFAULT_PARAGRAPH_LENGTH = new IntRange(1, 7);
  private static final IntRange DEFAULT_COMMENT_LENGTH = new IntRange(1, 5);
  private static final Random RANDOM = new Random();
  
  /** This class may not be instantiated. */
  private RandomTextGenerator() { }
  
  /**
   * Returns a random integer from 0 to the provided number.
   */
  private static int r(int top)
  {
    return RANDOM.nextInt(top + 1);
  }
  
  /**
   * Returns a random integer in a specified range.
   */
  private static int r(IntRange range)
  {
    return range.min + r(range.width());
  }
  
  /**
   * Create a random word.
   */
  public static String randomWord(IntRange sizeRange)
  {
    final int size = r(sizeRange);
    return StringHelper.randomString.lowercase(size);
  }
  
  /**
   * Creates a random word between 1 and 15 characters.
   */
  public static String randomWord()
  {
    return randomWord(DEFAULT_WORD_LENGTH);
  }
  
  /**
   * Create a random sentence.
   */
  public static String randomSentence(IntRange wordCountRange)
  {
    final int words = r(wordCountRange);
    final StringList sentence = new StringList(" ");
    for (int i = 0; i < words; i++)
    {
      sentence.add(randomWord());
    }
    return StringHelper.uppercaseFirstLetter(sentence.toString());
  }
  
  /**
   * Create a random sentence between 2 and 12 words.
   */
  public static String randomSentence()
  {
    return randomSentence(DEFAULT_SENTENCE_LENGTH);
  }
  
  /**
   * Create a random paragraph.
   */
  public static String randomParagraph(IntRange sentenceCountRange)
  {
    final int sentences = r(sentenceCountRange);
    final StringList paragraph = new StringList(". ");
    for (int i = 0; i < sentences; i++)
    {
      paragraph.add(randomSentence());
    }
    return paragraph.toString() + ".";
  }
  
  /**
   * Create a random paragraph between 1 and 6 sentences in length.
   */
  public static String randomParagraph()
  {
    return randomParagraph(DEFAULT_PARAGRAPH_LENGTH);
  }
  
  /**
   * Create a random multi-paragraph body.
   */
  public static String randomBody(IntRange paragraphCountRange)
  {
    final int paragraphs = r(paragraphCountRange);
    final StringList body = new StringList("\n\n");
    for (int i = 0; i < paragraphs; i++)
    {
      body.add(randomParagraph());
    }
    return body.toString();
  }
  
  /**
   * Create a random multi-paragraph body between 1 and 3 paragraphs in 
   * length.
   */
  public static String randomBody()
  {
    return randomBody(DEFAULT_COMMENT_LENGTH);
  }

}
