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
import com.techempower.helper.*;

/**
 * Validates that two user-provided fields contain the same value.  The
 * principal use of this is validating that a password and a "repeat password"
 * field match.
 */
public class RepeatValidator
     extends ElementValidator
{

  //
  // Variables.
  //
  
  private final String elementName2;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public RepeatValidator(String elementName1, String elementName2)
  {
    super(elementName1);
    this.elementName2 = elementName2;
    message(elementName1 + " and " + elementName2 + " must contain the same value.");
  }
  
  @Override
  public void process(final Input input)
  {
    final String uv1 = getUserValue(input);
    final String uv2 = input.values().get(elementName2, "");
    
    // Non-zero compare-to is not a match.
    if (StringHelper.compareToNullSafe(uv1, uv2) != 0)
    {
      input.addError(getElementName(), message);
    }
  }
  
}
