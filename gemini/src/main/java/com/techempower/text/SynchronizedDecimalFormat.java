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

import java.text.*;

/**
 * Provides a simple thread-safe wrapper for the "format" method provided
 * by DecimalFormat.
 */
public class SynchronizedDecimalFormat
{
  //
  // Member variables.
  //
  
  private final DecimalFormat decimalFormat;

  //
  // Member methods.
  //

  /**
   * Constructor.  See SimpleDecimalFormat's constructor for more details.
   */
  public SynchronizedDecimalFormat(String numberFormat)
  {
    this.decimalFormat = new DecimalFormat(numberFormat);
  }

  /**
   * Constructor.  Uses a standard #,##0.00 format.
   */
  public SynchronizedDecimalFormat()
  {
    this("#,##0.00");
  }

  /**
   * Formats a float to a String.
   */
  public synchronized String format(float number)
  {
    return this.decimalFormat.format(number);
  }

  /**
   * Formats a long to a String.
   */
  public synchronized String format(long number)
  {
    return this.decimalFormat.format(number);
  }

  /**
   * Formats a double to a String.
   */
  public synchronized String format(double number)
  {
    return this.decimalFormat.format(number);
  }

  /**
   * Formats an int to a String.
   */
  public synchronized String format(int number)
  {
    return this.decimalFormat.format(number);
  }

} // End SynchronizedDecimalFormat.
