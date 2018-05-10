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
import com.techempower.util.*;

/**
 * Validates that the user provided string is an integer within the provided
 * minimum and maximum.
 */
public class LongValidator
  extends    ElementValidator
{
  
  //
  // Variables.
  //
  
  private final long minimum;
  private final long maximum;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public LongValidator(String elementName, long minimum, long maximum)
  {
    super(elementName);
    this.minimum = minimum;
    this.maximum = maximum;
    message(elementName + " must be an integer between " + minimum + " and " + maximum + ".");
  }
  
  /**
   * Constructor.
   */
  public LongValidator(String elementName, LongRange range)
  {
    this(elementName, range.min, range.max);
  }
  
  @Override
  public void process(final Input input)
  {
    final String userValue = getUserValue(input);
    final long intValue = NumberHelper.parseLong(userValue);
    if (  (intValue < minimum)
       || (intValue > maximum)
       )
    {
      input.addError(getElementName(), message);
    }
  }

  /**
   * @return the minimum
   */
  public long getMinimum()
  {
    return minimum;
  }

  /**
   * @return the maximum
   */
  public long getMaximum()
  {
    return maximum;
  }
    
}
