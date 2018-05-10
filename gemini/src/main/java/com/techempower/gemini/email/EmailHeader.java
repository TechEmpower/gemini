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

package com.techempower.gemini.email;

/**
 * Contains header information for emails.  Stores a single key and a 
 * single value as a pair.
 */
public class EmailHeader
{
  //
  // Member variables.
  //

  private String headerName;
  private String headerValue;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.  Email Header with the header name and
   * key set.
   */
  public EmailHeader(String name, String value)
  {
    this.headerName = name;
    this.headerValue = value;
  }
  
  /**
   * Gets the header name.
   */
  public String getHeaderName()
  {
    return this.headerName;
  }
  
  /**
   * Sets the header name.
   */
  public void setHeaderName(String name)
  {
    this.headerName = name;
  }
  
  /**
   * Gets the value.
   */
  public String getHeaderValue()
  {
    return this.headerValue;
  }
  
  /**
   * Sets the value.
   */
  public void setHeaderValue(String value)
  {
    this.headerValue = value;
  }
  
}  // End Class EmailHeader.
