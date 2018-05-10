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

package com.techempower.gemini.messaging;

/**
 * The different types of messages that can be put in the session.
 */
public enum MessageType
{
  /**
   * Message to send a user indicating that an operation was successful
   */
  SUCCESS("success message",
      "background: #e6efc2; color: #264409; border-color: #c6d880;"),
  /**
   * Message to send to a user indicating that an error occurred   
   */
  ERROR("error message",
      "background: #fbe3e4; color: #8a1f11; border-color: #fbc2c4;"),
  
  /**
   * Message to send to a user as a warning
   */
  WARNING("warning message",
      "background: #fff6bf; color: #514721; border-color: #ffd324;"),
  
  /**
   * Message to send to a user
   */
  NORMAL(
      "normal message",
      "background: #d5edf8; color: #205791; border-color: #92cae4;");

  /**
   * The default CSS styling for messages
   */
  private static final String STYLE = 
      "padding: 13px; margin-bottom: 16px; border: 2px solid #ddd;";
  
  /**
   * The default CSS/HTML class name for this message type
   */
  private String className;
  
  /**
   * The default CSS styling for this message type
   */
  private String style;

  /**
   * Create a message type
   * @param className the default HTML/CSS class name
   * @param style default CSS styling for messages of this type
   */
  private MessageType(String className, String style)
  {
    this.className = className;
    this.style = style;
  }

  /**
   * Get the HTML/CSS default class name for this type
   * @return the name of the class
   */
  protected String getClassName()
  {
    return this.className;
  }

  /**
   * The CSS style rules for messages of this type to use by default
   */
  protected String getInlineStyle()
  {
    return STYLE + " " + this.style;
  }
}