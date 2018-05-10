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

import com.techempower.util.*;

/**
 * PyxisUserGroup defines the interface for Pyxis user groups.  The
 * BasicUserGroup class is a base implementation of this interface.
 *
 * @see BasicSecurity
 * @see BasicUserGroup
 */
public interface PyxisUserGroup
  extends        PyxisConstants,
                 Identifiable
{

  //
  // Constants.
  //
  
  /**
   * TYPE_BUILTIN is used for the standard Pyxis user groups: Guests (ID 0); 
   * Users (ID 1); and Administrators (ID 1000).
   */
  int TYPE_BUILTIN      = 0;
  
  /**
   * TYPE_APPLICATION is intended for use by any hard-coded application-
   * specific user groups such as "Editors" or "Clients".
   */
  int TYPE_APPLICATION  = 1;
  
  /**
   * TYPE_USER_DEFINED is intended for use by any user groups that are
   * defined by system administrators using an application's user interface.
   * Generally, such a user interface would allow users to be assigned to
   * groups of any type but would only allow the creation and deletion of
   * groups of type 2 (TYPE_USER_DEFINED).
   */
  int TYPE_USER_DEFINED = 2;
  
  //
  // Mutators
  //

  /**
   * Sets the group's ID.  Some implementations may ignore this.
   */
  void setGroupID(long id);

  /**
   * Sets the group's name.  Some implementations may ignore this.
   */
  void setName(String name);
  
  /**
   * Sets the group's type.  Types are defined by the constants in this
   * interface.
   */
  void setType(int type);

  /**
   * Sets the group's description.
   */
  void setDescription(String description);

  //
  // Accessors.
  //

  /**
   * Gets the group ID.
   */
  long getGroupID();

  /**
   * Gets the group's name.
   */
  String getName();
  
  /**
   * Gets the group's type.
   */
  int getType();

  /**
   * Gets the group's description.
   */
  String getDescription();

}   // End PyxisUserGroup.
