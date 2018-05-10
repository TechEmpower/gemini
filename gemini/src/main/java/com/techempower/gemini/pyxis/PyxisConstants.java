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

/**
 * Stores utility constants for the Pyxis authentication services of 
 * Gemini.
 *
 * @see BasicSecurity
 */
public interface PyxisConstants
{

  // Pyxis prefix.
  String  PYXIS_PREFIX                  = "Pyxis.";
  String  SHORT_PYXIS_PREFIX            = "px";

  // Pyxis user session name.
  String  SESSION_USER                  = PYXIS_PREFIX + "User";
  String  SESSION_CLOSE_INDICATOR       = PYXIS_PREFIX + "SessionCloseIndicator";
  String  SESSION_COOKIE_LOGIN          = PYXIS_PREFIX + "UsedCookieLogin";
  String  SESSION_EXPIRATION_WARNED     = PYXIS_PREFIX + "PasswordExpirationWarned";
  String  SESSION_IMPERSONATED_USER     = PYXIS_PREFIX + "ImpersonatedUser";

  // Users table information.
  String  USERS_IDENTITY                = "UserID";
  String  USERS_USERNAME                = "UserUsername";
  String  USERS_PASSWORD                = "UserPassword";
  String  USERS_EMAIL                   = "UserEmail";

  // Groups table information.
  String  GROUPS_IDENTITY               = "GroupID";
  String  GROUPS_NAME                   = "GroupName";
  String  GROUPS_DESCRIPTION            = "GroupDescription";

  // IDs for Predefined Groups
  int     GROUP_ADMINISTRATORS          = 1000;
  int     GROUP_GUESTS                  = 0;
  int     GROUP_USERS                   = 1;

}   // End Constants.
