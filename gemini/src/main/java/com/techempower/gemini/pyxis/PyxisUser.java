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

package com.techempower.gemini.pyxis;

import java.util.*;

import com.techempower.util.*;

/**
 * PyxisUser defines the interface that must be implemented by all Pyxis
 * User objects.  BasicUser is a fairly simple starting implementation.
 *
 * @see BasicSecurity
 * @see BasicUser
 */
public interface PyxisUser
  extends        PyxisConstants,
                 Identifiable
{

  /**
   * Sets the user's username.  Returns self.
   */
  PyxisUser setUserUsername(String username);

  /**
   * Gets the userUsername.
   */
  String getUserUsername();

  /**
   * Sets the user's password.
   */
  void setUserPassword(String password);

  /**
   * Gets the userPassword.  This method is usually final to prevent 
   * overloading.
   */
  String getUserPassword();
  
  /**
   * Sets the user's last login date.  Value may be null to indicate the user
   * has never logged in.  Returns self.
   */
  PyxisUser setUserLastLogin(Date lastLogin);
  
  /**
   * Gets the user's last login date.  May return null if the user has never
   * logged in.
   */
  Date getUserLastLogin();
  
  /**
   * Sets the user's last password-change date.  Applications are not expected
   * to call this directly.  Calls to setUserPassword (for initialized User
   * objects) will set the last password-change date automatically.  Returns
   * self.
   */
  PyxisUser setUserLastPasswordChange(Date lastPasswordChange);
  
  /**
   * Gets the user's last password change date.
   */
  Date getUserLastPasswordChange();

  //
  // Pass-throughs to the standard user groups.
  //

  /**
   * Is this user an Administrator?
   */
  boolean isAdministrator();

  /**
   * Is this user in the Users group?
   */
  boolean isUser();

  /**
   * Is this user a Guest?
   */
  boolean isGuest();
  
  /**
   * Is this user a member of the provided group?
   */
  boolean isMember(PyxisUserGroup group);

  /**
   * Is this user a member of the provided group (by Group ID)?
   */
  boolean isMember(long groupID);
  
  /**
   * Is this user account enabled?  An account that is not enabled will not
   * be permitted to login.
   */
  boolean isEnabled();
 
}   // End PyxisUser.
