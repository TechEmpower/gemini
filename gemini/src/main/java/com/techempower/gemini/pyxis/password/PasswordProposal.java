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

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;

/**
 * Encapsulates a password proposal for validation purposes.  A proposal is 
 * composed of a proposed password (in plaintext), a proposed username (which
 * is useful if the user does not yet exist), a user reference (if the user
 * does already exist), and a Context of the request (which may be the user
 * themselves or a system administrator; this is useful for localization
 * of validation error messages.)
 *   <p>
 * The password is required to be non-null.
 */
public class PasswordProposal
{
  
  public final String password;
  public       String hashedPassword;
  public final String username;
  public final PyxisUser user;
  public final BasicContext context;
  
  /**
   * This is an optional flag to indicate that the proposal should be accepted
   * by the Security without requiring validation by password complexity
   * requirements.  This facilitates very specific use cases such as hashing 
   * legacy passwords that do not meet current requirements.  Applications are
   * <b>strongly</b> advised to avoid setting this flag to true on any routine
   * password-change operations since this will undermine complexity 
   * requirements.
   */
  public       boolean bypassValidation = false;
  
  public PasswordProposal(String password, String username, PyxisUser user,
      BasicContext context)
  {
    Objects.requireNonNull(password, "Password may not be null.");

    this.password = password;
    this.username = username;
    this.user = user;
    this.context = context;
  }

}
