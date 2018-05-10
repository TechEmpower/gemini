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

import java.text.*;
import java.util.*;

import com.techempower.text.*;

/**
 * CurrencyHelper provides utility functions for working with currencies.
 */
public final class CurrencyHelper
{
  
  //
  // Static variables.
  //
  
  public static final SynchronizedDecimalFormat    STANDARD_US_CURRENCY_FORMAT   = new SynchronizedDecimalFormat("\u00A4#,##0.00;(#,##0.00)");
  public static final SynchronizedDecimalFormat    NOSYMBOL_US_CURRENCY_FORMAT   = new SynchronizedDecimalFormat("#,##0.00;(#,##0.00)");
  public static final SynchronizedDecimalFormat    NOGROUPING_US_CURRENCY_FORMAT = new SynchronizedDecimalFormat("0.00");
  public static final SynchronizedDecimalFormat    NODECIMAL_US_CURRENCY_FORMAT  = new SynchronizedDecimalFormat("\u00A4###,###");

  //
  // Static methods.
  //

  /**
   * Format given double as US currency
   *
   * @param d US dollar amount to be formatted
   */
  public static String formatUS(double d)
  {
    return NumberFormat.getCurrencyInstance(Locale.US).format(d);
  }

  /**
   * Format given double as US currency
   *
   * @param d US dollar amount to be formatted
   */
  public static String formatUSNoDecimal(double d)
  {
    return NODECIMAL_US_CURRENCY_FORMAT.format(d);
  }

  /**
   * Format given double as US currency
   *
   * @param f US dollar amount to be formatted
   */
  public static String formatUS(float f)
  {
    return NumberFormat.getCurrencyInstance(Locale.US).format(f);
  }

  /**
   * Formats an integer as US currency, e.g., "$1,234"
   */
  public static String formatUS(int number)
  {
    return NODECIMAL_US_CURRENCY_FORMAT.format(number);
  }
  
  /**
   * Formats an integer of <b>cents</b> as US currency, e.g. 123456 will be
   * rendered as "$1,234.56".
   */
  public static String formatUSCents(int cents)
  {
    return NODECIMAL_US_CURRENCY_FORMAT.format(cents / 100) + "." 
        + StringHelper.padZero(Math.abs(cents) % 100, 2);
  }

  /**
   * You may not instantiate this class.
   */
  private CurrencyHelper()
  {
    // Does nothing.
  }

}  // End CurrencyHelper.

