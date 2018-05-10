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

/**
 * PhoneHelper provides helper functions for working with phone numbers.
 */
public final class PhoneHelper
{

  //
  // Static variables.
  //

  //
  // Static methods.
  //

  /**
   * Converts the first 12 digits of any phone number with any punctuation to
   * XXX-XXX-XXXX format. Only use on United States phone numbers. Returns an
   * empty String if an empty String is provided.
   * 
   * @param phoneNumber the input phone number with any punctuation.
   * @return the phone number in XXX-XXX-XXXX format.
   */
  public static String formatStandard(String phoneNumber)
  {
    if (StringHelper.isEmpty(phoneNumber))
    {
      return "";
    }
    else
    {
      String numbersOnly = StringHelper.stripNonNumeric(phoneNumber);
      long numbers = 0;

      try
      {
        numbers = Long.parseLong(numbersOnly);

        while (numbers >= 10000000000L)
        {
          numbers /= 10L;
        }
      }
      catch (NumberFormatException nfexc)
      {
        // Do nothing.
      }

      StringBuilder buffer = new StringBuilder(12);
      buffer.append(StringHelper.padZero((int)(numbers / 10000000L), 3));
      numbers = numbers % 10000000;
      buffer.append('-');
      buffer.append(StringHelper.padZero((int)(numbers / 10000L), 3));
      numbers = numbers % 10000;
      buffer.append('-');
      buffer.append(StringHelper.padZero((int)numbers, 4));

      return buffer.toString();
    }
  }

  /**
   * Gets the first 12 digits of any phone number with any punctuation to
   * (XXX)XXX-XXXX format. Only use on United States phone numbers. Returns an
   * empty String if an empty String is provided.
   * 
   * @param phoneNumber the input phone number with any punctuation.
   * @return the phone number in (XXX)XXX-XXXX format.
   */
  public static String formatParen(String phoneNumber)
  {
    if (StringHelper.isEmpty(phoneNumber))
    {
      return "";
    }
    else
    {
      String numbersOnly = StringHelper.stripNonNumeric(phoneNumber);
      long numbers = 0;

      try
      {
        numbers = Long.parseLong(numbersOnly);

        while (numbers >= 10000000000L)
        {
          numbers /= 10L;
        }
      }
      catch (NumberFormatException nfexc)
      {
        // Do nothing.
      }

      StringBuilder buffer = new StringBuilder(13);
      buffer.append('(');
      buffer.append(StringHelper.padZero((int)(numbers / 10000000L), 3));
      numbers = numbers % 10000000;
      buffer.append(')');
      buffer.append(StringHelper.padZero((int)(numbers / 10000L), 3));
      numbers = numbers % 10000;
      buffer.append('-');
      buffer.append(StringHelper.padZero((int)numbers, 4));

      return buffer.toString();
    }
  }

  /**
   * Attempts to format the given phone number using a provided pattern such
   * as "(XXX)XXX-XXXX".
   * 
   * @param phoneNumber the phone number with any punctuation
   * @param formatPattern the format string where X represents a digit
   */
  public static String format(String phoneNumber, String formatPattern)
  {
    String formatted = "";

    if (StringHelper.isNonEmpty(phoneNumber)
        && StringHelper.isNonEmpty(formatPattern))
    {
      String numbersOnly = StringHelper.stripNonNumeric(phoneNumber);

      if (StringHelper.isNonEmpty(numbersOnly))
      {
        // Add zeros until we get at least 10 digits.
        numbersOnly = StringHelper.padArbitraryRight('0', numbersOnly, 10);

        int index = 0;

        // Build up the return value.
        StringBuilder toReturn = new StringBuilder(formatPattern.length());
        for (char ch : formatPattern.toCharArray())
        {
          if ((ch == 'X') && (index < numbersOnly.length()))
          {
            toReturn.append(numbersOnly.charAt(index++));
          }
          else
          {
            toReturn.append(ch);
          }
        }

        formatted = toReturn.toString();
      }
    }

    return formatted;
  }

  /**
   * Determines if a string is a valid US telephone number based on the
   * following criteria: 1) there are at least 10 digits. 2) valid characters
   * are as follows: -digits: "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"
   * -left area code separator: " ", ".", "-", "(" -right area code separator:
   * " ", ".", "-", ")" -prefix/suffix separator: " ", ".", "-" 3) separator
   * characters are required between the area code, prefix, and suffix. 4) if
   * a "1" is the first character, a separator is required between it and the
   * area code (area codes don't start with "1"). To allow for telephone
   * extensions, method will return true if it counts 10 digits and and all
   * characters prior to the 10th digit are valid. For instance:
   * "(123) 456-7890 ext.123" is valid, while "456-7890 ext.123" is not.
   */
  public static boolean isValidUS(String number)
  {
    String validLeftSeparator = " (-."; // left separator for the area code
    String validRightSeparator = " )-."; // right separator for the area code
    String validSeparator = " -."; // between the prefix and suffix

    // we should have at least 10 chars
    if ((number != null) && (number.trim().length() >= 10))
    {
      String trimmed = number.trim();
      int index = 0;

      // if the first char isn't a number, lets check if it's valid
      if (!NumberHelper.isNumber(trimmed.substring(index, index + 1)))
      {
        // look for the left area code separator
        if (!validLeftSeparator.contains(trimmed.substring(index, index + 1)))
        {
          return false;
        }

        index++;
      }
      else if (trimmed.substring(index, index + 1).equals("1"))
      {
        index++;

        // if they start with a 1, a left separator is required.
        if (!validLeftSeparator.contains(trimmed.substring(index, index + 1)))
        {
          return false;
        }

        index++;
      }

      // check the area code
      // the next 3 characters have to be numbers
      for (int i = index; i < (index + 3); i++)
      {
        if (!NumberHelper.isNumber(trimmed.substring(i, i + 1)))
        {
          return false;
        }

        // if the first number is a "1" it's invalid, area codes don't start
        // with "1"
        if (i == index && trimmed.substring(i, i + 1).equals("1"))
        {
          return false;
        }
      }

      index += 3;

      // look for the right separator
      if (!validRightSeparator.contains(trimmed.substring(index, index + 1)))
      {
        return false;
      }

      index++;

      // next we need either a number or a space
      // "(123) " or "(123)4" are ok
      if (!trimmed.substring(index, index + 1).equals(" "))
      {
        if (!NumberHelper.isNumber(trimmed.substring(index, index + 1)))
        {
          return false;
        }
      }
      else
      {
        index++; // if it is a space, lets advance.
      }

      // phone number prefix
      // the next 3 characters have to be numbers
      for (int j = index; j < (index + 3); j++)
      {
        if (!NumberHelper.isNumber(trimmed.substring(j, j + 1)))
        {
          return false;
        }
      }

      index += 3;

      // there should be a separator between the prefix and suffix
      if (!validSeparator.contains(trimmed.substring(index, index + 1)))
      {
        return false;
      }

      index++;

      // phone number suffix
      // the next 4 characters have to be numbers
      for (int k = index; k < (index + 4); k++)
      {
        if (!NumberHelper.isNumber(trimmed.substring(k, k + 1)))
        {
          return false;
        }
      }

      // if we get here, we've passed all the requirements.
      // remember we are allowing invalid characters from here to the end
      // of the string to allow for telephone extensions.
      return true;
    }
    else
    {
      return false;
    }
  } // end isValidUSTelephoneNumber

  /**
   * You may not instantiate this class.
   */
  private PhoneHelper()
  {
    // Does nothing.
  }

} // End PhoneHelper.

