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
package com.techempower.gemini.input.validator;

import com.techempower.gemini.input.*;
import com.techempower.util.*;

/**
 * Validates that the length of a user-provided String is correctly bounded.
 */
public class LengthValidator
  extends    ElementValidator
{

  //
  // Variables.
  //
  
  private final IntRange lengthRange;
  private final boolean permitEmpty;

  //
  // Methods.
  //
  
  /**
   * Constructor.
   * 
   * @param elementName the form element to validate.
   * @param lengthRange a minimum and maximum length in characters.
   * @param permitEmpty if true, 0-length will be permitted as a special case
   *   even if the minimum is &gt;0.  This allows for non-required fields to be
   *   skipped if the user doesn't want to provide input. 
   */
  public LengthValidator(String elementName, IntRange lengthRange, 
      boolean permitEmpty)
  {
    super(elementName);
    this.lengthRange = lengthRange;
    this.permitEmpty = permitEmpty;
    message(elementName + " must be between " + lengthRange.min + " and " 
        + lengthRange.max + " characters in length.");
  }
  
  /**
   * Constructor.  This version assumes that empty values are not permitted
   * unless the minimumLength is 0.
   */
  public LengthValidator(String elementName, IntRange lengthRange)
  {
    this(elementName, lengthRange, false);
  }
  
  /**
   * Constructor.
   * 
   * @param elementName the form element to validate.
   * @param minLength the minimum String length to permit (E.g., 4)
   * @param maxLength the maximum String length to permit (E.g., 40)
   * @param permitEmpty if true, 0-length will be permitted as a special case
   *   even if the minimum is &gt;0.  This allows for non-required fields to be
   *   skipped if the user doesn't want to provide input. 
   */
  public LengthValidator(String elementName, int minLength, int maxLength, 
      boolean permitEmpty)
  {
    this(elementName, new IntRange(minLength, maxLength), permitEmpty);
  }
  
  /**
   * Constructor.  This version assumes that empty values are not permitted
   * unless the minimumLength is 0.
   */
  public LengthValidator(String elementName, int minLength, int maxLength)
  {
    this(elementName, minLength, maxLength, false);
  }
  
  @Override
  public void process(final Input input)
  {
    final String userValue = getUserValue(input);
    final int len = userValue.length();
    
    // If the length is zero and we permit empty values, return immediately.
    if (  (permitEmpty)
       && (len == 0)
       )
    {
      return;
    }
    
    // If the length isn't within the bounds, add a the error message.
    if (  (  (lengthRange.max > 0)
          && (len > lengthRange.max)
          )
       || (  (lengthRange.min > 0) 
          && (len < lengthRange.min)
          )
       )
    {
      input.addError(getElementName(), message);
    }
  }
  
}
