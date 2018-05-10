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

/**
 * Raises a validation error if the user-provided value contains a specified
 * String.
 */
public class DisallowedValidator
  extends    ElementValidator
{

  //
  // Variables.
  //
  
  private final String disallowedString;
  private final boolean contains;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public DisallowedValidator(String elementName, String disallowedString, 
      boolean contains)
  {
    super(elementName);
    this.disallowedString = disallowedString.toLowerCase();
    this.contains = contains;
    message(elementName + " is not acceptable as provided.");
  }
  
  /**
   * Constructor.  Assumes a contains search.
   */
  public DisallowedValidator(String elementID, String disallowedString)
  {
    this(elementID, disallowedString, true);
  }

  @Override
  public void process(final Input input)
  {
    final String userValue = getUserValue(input);
    if (userValue.length() > 0)
    {
      if (contains)
      {
        if (userValue.toLowerCase().contains(disallowedString))
        {
          input.addError(getElementName(), message);
        }
      }
      else
      {
        if (userValue.toLowerCase().equals(disallowedString))
        {
          input.addError(getElementName(), message);
        }
      }
    }
  }
  
}
