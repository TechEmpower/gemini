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
package com.techempower.gemini.pyxis.password;

import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * Enforces a mixed-character-type complexity requirement.  That is, a
 * requirement that the password be composed with at least a specified number
 * of uppercase, lowercase, numeric, and other characters.  Note that we
 * treat <b>any</b> other character as an "other character."  The "other"
 * category includes both traditional special characters such as brackets, as
 * well as characters that are unusual for passwords such as 8-bit "ANSI"
 * characters.  Gemini applications using bcrypt or similar password hashing
 * support passwords composed of such characters.
 */
public class   PasswordComplexity
    implements PasswordRequirement
{

  public static final int DEFAULT_MIN_UPPERCASE = 1;
  public static final int DEFAULT_MIN_LOWERCASE = 1;
  public static final int DEFAULT_NUMERIC = 1;
  public static final int DEFAULT_OTHER = 1;
  
  private final int minUppercase;
  private final int minLowercase;
  private final int minNumeric;
  private final int minOther;
  private final String synopsis;
  
  public PasswordComplexity(int minUppercase, int minLowercase, 
      int minNumeric, int minOther)
  {
    this.minLowercase = minLowercase;
    this.minNumeric = minNumeric;
    this.minUppercase = minUppercase;
    this.minOther = minOther;

    final StringList rules = StringList.getPlainEnglishList();
    if (minUppercase > 0)
    {
      rules.add(minUppercase + " uppercase letter" + StringHelper.pluralize(minUppercase));
    }
    if (minLowercase > 0)
    {
      rules.add(minLowercase + " lowercase letter" + StringHelper.pluralize(minLowercase));
    }
    if (minNumeric > 0)
    {
      rules.add(minNumeric + " number" + StringHelper.pluralize(minNumeric));
    }
    if (minOther > 0)
    {
      rules.add(minOther + " other character" + StringHelper.pluralize(minOther));
    }
    
    this.synopsis = rules.toString();
  }
  
  public PasswordComplexity()
  {
    this(DEFAULT_MIN_UPPERCASE, DEFAULT_MIN_LOWERCASE, DEFAULT_NUMERIC, 
        DEFAULT_OTHER);
  }
  
  @Override
  public String validate(PasswordProposal proposal)
  {
    // Count occurrences of each type of character.
    int uc = 0, lc = 0, n = 0, o = 0;
    char c;
    for (int i = 0; i < proposal.password.length(); i++)
    {
      c = proposal.password.charAt(i);
      if ( (c >= '0') && (c <= '9') )
      {
        n++;  // numeric
      }
      else if ( (c >= 'a') && (c <= 'z') )
      {
        lc++; // lowercase
      }
      else if ( (c >= 'A') && (c <= 'Z') )
      {
        uc++; // uppercase
      }
      else
      {
        o++;  // other
      }
    }

    // Are any of the counts insufficient?
    if ( ( (this.minUppercase > 0) && (uc < this.minUppercase) )
      || ( (this.minLowercase > 0) && (lc < this.minLowercase) )
      || ( (this.minNumeric > 0) && (n < this.minNumeric) )
      || ( (this.minOther > 0) && (o < this.minOther) )
      )
    {
      return "Passwords must contain at least " + synopsis + ".";
    }
    
    return null;
  }
 
}
