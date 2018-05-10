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
 * Validates a submitted form element.
 */
public abstract class ElementValidator
           implements Validator
{
  
  //
  // Variables.
  //
  
  private final String elementName;
  protected String message;
  
  //
  // Methods.
  //

  /**
   * Constructor.
   */
  public ElementValidator(String elementName)
  {
    this.elementName = elementName;
  }
  
  /**
   * Sets the validation error message.
   */
  public ElementValidator message(String newMessage)
  {
    this.message = newMessage;
    return this;
  }
  
  /**
   * Performs a validation of the element.  Returns null if there is no
   * validation error; a non-null String message otherwise.
   */
  @Override
  public abstract void process(final Input input);
  
  /**
   * Gets the user-provided value in the specified field.  Will not return
   * null--if the request does not provide this value, empty String will be
   * returned.
   */
  protected String getUserValue(final Input input)
  {
    return input.values().get(elementName, "");
  }
  
  /**
   * Gets the Element's name.
   */
  public String getElementName()
  {
    return elementName;
  }
  
  /**
   * Gets the validation message.
   */
  public String getMessage()
  {
    return message;
  }

}
