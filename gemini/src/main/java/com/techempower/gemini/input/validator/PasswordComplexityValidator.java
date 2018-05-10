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

import java.util.*;

import com.techempower.gemini.input.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.password.*;
import com.techempower.helper.*;

/**
 * Provides validation logic to enforce password-complexity constraints in a
 * user-registration or user-edit context.  Uses the validation rules
 * enforced by the Security.
 *   <p>
 * Allows empty input to pass validation to facilitate building user 
 * interfaces that allow users to <b>optionally</b> change their passwords
 * (electing to leave them unchanged by leaving the new-password field empty).
 * However, because of this, it is important to note that in a user 
 * registration flow, this validator should be paired with a RequiredValidator
 * to ensure that at least something is input into the field.  
 */
public class PasswordComplexityValidator
     extends ElementValidator 
{
  
  private final PyxisSecurity security;
  private final String usernameElementName;

  /**
   * Constructor.
   * 
   * @param security A reference to the application's security.
   */
  public PasswordComplexityValidator(String elementName, PyxisSecurity security)
  {
    super(elementName);
    this.security = security;
    this.usernameElementName = null;
  }

  /**
   * Constructor.
   * 
   * @param security A reference to the application's security.
   * @param usernameElementName If no user is in scope during validation, the
   *        user-provided value in the field identified by usernameElementName
   *        will be used as the user's proposed username. 
   */
  public PasswordComplexityValidator(String elementName, PyxisSecurity security,
      String usernameElementName)
  {
    super(elementName);
    this.security = security;
    this.usernameElementName = usernameElementName;
  }

  @Override
  public void process(Input input) 
  {
    final String userValue = getUserValue(input);
    if (userValue.length() > 0)
    {
      final PyxisUser user = security.getUser(input.context());
      final String username;
      if (user != null)
      {
        username = user.getUserUsername();
      }
      else if (usernameElementName != null)
      {
        username = input.values().get(usernameElementName);
      }
      else
      {
        username = null;
      }
      
      final PasswordProposal proposal = new PasswordProposal(
          userValue,
          username,
          user,
          input.context()
          );
      final List<String> errors = security.passwordValidate(proposal);
      
      if (errors.size() > 0)
      {
        input.addError(getElementName(), StringHelper.join(" ", errors));
      }
    }
  }

}
