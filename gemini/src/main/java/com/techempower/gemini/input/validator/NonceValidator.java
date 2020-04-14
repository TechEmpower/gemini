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

import java.security.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.context.*;
import com.techempower.gemini.input.*;
import com.techempower.helper.*;

/**
 * Provides a singular session-backed nonce as a countermeasure against
 * cross-site request forgery (CSRF).  This class acts both as a validator
 * for input (to confirm the proper nonce value was submitted as part of
 * a request) and also a manager for the session-stored nonce.  It provides
 * a static method (getNonce) to retrieve the nonce for a Context, creating
 * and storing a new one if the associated session does not yet have one.
 */
public class NonceValidator
     extends ElementValidator 
{
  
  private static final String SO_NONCE = "GeminiSessionNonce";
  private static final Random RANDOM = new SecureRandom();

  /**
   * Constructor.
   */
  public NonceValidator(String elementName)
  {
    super(elementName);
    message("Please use the form provided.");
  }
  
  /**
   * Constructor that assumes the element name is "nonce".
   */
  public NonceValidator()
  {
    this("nonce");
  }
  
  @Override
  public void process(Input input) 
  {
    final long userValue = NumberHelper.parseLong(getUserValue(input));
    if (!checkNonce(input.context(), userValue))
    {
      input.addError(getElementName(), getMessage());
    }
  }
  
  /**
   * Checks a user-provided nonce valud.
   * 
   * @param candidate The user-provided candidate nonce to check against the
   *        session's valid nonce.
   */
  public static boolean checkNonce(BasicContext context, long candidate)
  {
    return (getNonce(context) == candidate);
  }
  
  /**
   * Get the nonce stored in the provided Context's session.  If the session
   * does not yet have a nonce, a new one will be created, stored, and
   * then returned.  That is, for the scope of a given Context's session, 
   * this method will always return the same nonce value.
   */
  public static long getNonce(BasicContext context)
  {
    final SessionNamedValues session = context.session();
    if (session.has(SO_NONCE))
    {
      return session.getLong(SO_NONCE);
    }
    else
    {
      final long nonce = RANDOM.nextLong();
      session.put(SO_NONCE, nonce);
      return nonce;
    }
  }

}
