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
 * Provides utility functionality for validating and dealing with credit
 * card numbers.  This is not an e-commerce component in the sense that it
 * does not run any credit card transactions.  Rather, it simply provides
 * utility functions for dealing with credit card numbers such as determining
 * the type of a card from the number alone.
 */
public final class CreditCardHelper
{

  //
  // Constants.
  //
  
  public static final int TYPE_UNKNOWN    = 0;
  public static final int TYPE_VISA       = 1;
  public static final int TYPE_MASTERCARD = 2;
  public static final int TYPE_DISCOVER   = 3;
  public static final int TYPE_AMEX       = 4;
  
  private static final String[] TYPE_NAMES = { "Unknown", "Visa", 
    "Mastercard", "Discover", "American Express" };
  
  private static final String[] TYPE_NAMES_ABBR = { "U", "V", "M", "D", "A" };
  private static final int[]    TYPE_VALUES = { 0, 1, 2, 3, 4 };
  private static final String[] TYPE_VALUES_STRING = 
    CollectionHelper.toStringArray(TYPE_VALUES);
  
  public static final int VALIDATION_OK = 0;
  public static final int VALIDATION_UNKNOWN_CARD_TYPE = 1;
  public static final int VALIDATION_INCORRECT_LENGTH = 2;
  public static final int VALIDATION_LUHN_FAILED = 3;
  
  //
  // Methods.
  //
  
  /**
   * Determines the type of a Credit Card by its number.
   *   <p>
   * The identification of card types from numbers is based on the information
   * available at Wikipedia:
   *   http://en.wikipedia.org/wiki/Credit_card_number
   *   
   * @param number The credit card number to evaluate.
   * @param checkLength Confirm that the length of the number is correct for
   *        the type identified by the prefix.
   */
  public static int getType(String number, boolean checkLength)
  {
    return getType(toIntArray(number), checkLength);
  }
  
  /**
   * Determines the type of a Credit Card by its number.  This method assumes
   * that length should be checked.
   */
  public static int getType(String number)
  {
    return getType(number, true);
  }
  
  /**
   * Determines the type of a Credit Card by its number.
   *   <p>
   * The identification of card types from numbers is based on the information
   * available at Wikipedia:
   *   http://en.wikipedia.org/wiki/Credit_card_number
   *   
   * @param number The credit card number to evaluate.
   * @param checkLength Confirm that the length of the number is correct for
   *        the type identified by the prefix.
   */
  public static int getType(byte[] number, boolean checkLength)
  {
    if (number.length > 4)
    {
      if (number[0] == 4)
      {
        // Visa can be 16 or 13 digits.
        if (!checkLength || number.length == 16 || number.length == 13)
        {
          return TYPE_VISA;
        }
      }
      else if ( ( number[0] == 5 )
             && ( ( number[1] == 1 )
               || ( number[1] == 2 )                
               || ( number[1] == 3 )                
               || ( number[1] == 4 )                
               || ( number[1] == 5 )
               )
             )
      {
        // Mastercard is 16 digits.
        if (!checkLength || number.length == 16)
        {
          return TYPE_MASTERCARD;
        }
      }
      else if ( ( number[0] == 3 )
             && ( ( number[1] == 4 )
               || ( number[1] == 7 )
          )
        )
      {
        // American Express is 15 digits.
        if (!checkLength || number.length == 15)
        {
          return TYPE_AMEX;
        }
      }
      else if ( (number[0] == 6) 
             && (number[1] == 0)
             && (number[2] == 1)
             && (number[3] == 1)
             )
      {
        // Discover is 16 digits.
        if (!checkLength || number.length == 16)
        {
          return TYPE_DISCOVER;
        }
      }
    }
    
    // Default.
    return TYPE_UNKNOWN;
  }
  
  /**
   * Validates a credit card number.  This does a Luhn algorithm check to
   * make sure that the number passes basic correctness checks.  An e-commerce
   * provider would need to actually attempt a transaction to know if the card
   * is, in fact, valid.
   * 
   * @param numberAsString The credit card number to evaluate.
   * @return an int from the VALIDATION set of constants.
   */
  public static int validate(String numberAsString)
  {
    byte[] number = toIntArray(numberAsString); 
    return validate(number);
  }
  
  /**
   * Validates a credit card number.  This does a Luhn algorithm check to
   * make sure that the number passes basic correctness checks.  An e-commerce
   * provider would need to actually attempt a transaction to know if the card
   * is, in fact, valid.
   * 
   * @param number The credit card number to evaluate.
   * @return an int from the VALIDATION set of constants.
   */
  public static int validate(byte[] number)
  {
    // First, see if the prefix suggests a known type.
    int type = getType(number, false);
    
    if (type > TYPE_UNKNOWN)
    {
      // Second, check if the length matches the expected length.
      int lengthCheck = getType(number, true);
      //System.out.println("Type: " + lengthCheck);
      if (lengthCheck > TYPE_UNKNOWN)
      {
        // Run the Luhn algorithm to make sure the number appears valid.
        int runningTotal = 0;
        for (int i = number.length - 2; i >= 0; i -= 2)
        {
          int doubled = number[i] * 2;
          
          if (doubled < 10)
          {
            runningTotal += doubled;
          }
          else
          {
            runningTotal += (1 + doubled % 10);
          }
        }
        
        for (int i = number.length - 1; i >= 0; i -= 2)
        {
          runningTotal += number[i];
        }
        
        if (runningTotal % 10 == 0)
        {
          return VALIDATION_OK;
        }
        else
        {
          return VALIDATION_LUHN_FAILED;
        }
      }
      else
      {
        return VALIDATION_INCORRECT_LENGTH;
      }
    }
    else
    {
      return VALIDATION_UNKNOWN_CARD_TYPE;
    }
  }
  
  /**
   * Converts a String of numbers, such as a credit card number, to a byte 
   * array of byte-sized integers.  Non-numeric characters such as spaces
   * or hyphens will be stripped.
   */
  @SuppressWarnings("cast")
  public static byte[] toIntArray(String number)
  {
    if (StringHelper.isNonEmpty(number))
    {
      // Strip non-numeric characters.
      String numeric = StringHelper.stripNonNumeric(number);
      
      byte[] digits = new byte[numeric.length()];
      int digit;
      
      for (int i = 0; i < digits.length; i++)
      {
        digit = (int)(numeric.charAt(i)) - (int)'0';
        digits[i] = (byte)digit;
      }
      
      return digits;
    }
    else
    {
      return new byte[0];
    }
  }
  
  /**
   * Obscures a credit card number with a provided character (typically 'X')
   * and a number of visible digits at the end (typically 4).
   * 
   * @param number The input credit card number.
   * @param obscuringCharacter The character to use for obscuring (typically 
   *        'X')
   * @param startDigitsToShow the number of digits to show at the start 
   *        (typically 1)
   * @param endDigitsToShow the number of digits to show at the end 
   *        (typically 4)
   */
  public static String obscure(String number, char obscuringCharacter, 
    int startDigitsToShow, int endDigitsToShow)
  {
    StringBuilder toReturn = new StringBuilder(number.length());
    
    for (int i = 0; i < number.length(); i++)
    {
      // If we're passed the first character and we're before the last 4
      // characters then append an "X".
      if (i >= startDigitsToShow && i < number.length() - endDigitsToShow)
      {
        toReturn.append(obscuringCharacter);
      }
      else
      {
        toReturn.append(number.charAt(i));
      }
    }

    return toReturn.toString();
  }
  
  /**
   * Obscures a credit card number using the typical approach of showing
   * the first digit and last four digits, replacing all other digits with
   * 'X'.
   */
  public static String obscure(String number)
  {
    return obscure(number, 'X', 1, 4);
  }
  
  /**
   * Produces a human-readable obscuring of a card rendered as 
   * "[type] ending with [last-four-digits".  For example, "Visa ending
   * with 1234".
   *   <p>
   * This method assumes that the credit card is a valid type and has been
   * validated.  Otherwise, it may return a curious result such as
   * "Unknown ending with abc1".
   */
  public static String obscureFriendly(String number)
  {
    if (number.length() > 4)
    {
      int type = getType(number);
      String remainder = number.substring(number.length() - 4);
      
      return TYPE_NAMES[type] + " ending with " + remainder;
    }
    
    // Fail-safe default.
    return "Unknown";
  }

  /**
   * Gets the names of credit card types supported.
   */
  public static String[] getTypeNames()
  {
    return TYPE_NAMES.clone();
  }

  /**
   * Gets abbreviated names of the credit card types supported.
   */
  public static String[] getTypeNamesAbbr()
  {
    return TYPE_NAMES_ABBR.clone();
  }

  /**
   * Gets integer IDs for the types of credit cards supported.
   */
  public static int[] getTypeValues()
  {
    return TYPE_VALUES.clone();
  }

  /**
   * Gets integer IDs for the types of credit cards supported as Strings.
   */
  public static String[] getTypeValuesString()
  {
    return TYPE_VALUES_STRING.clone();
  }

  /**
   * You may not instantiate this class.
   */
  private CreditCardHelper()
  {
    // Does nothing.
  }
  
}
