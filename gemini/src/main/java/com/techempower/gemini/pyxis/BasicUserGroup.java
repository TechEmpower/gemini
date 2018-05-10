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

import com.techempower.helper.StringHelper;
import com.techempower.util.PersistenceAware;


/**
 * BasicUserGroup is a fundamental User Group representation.  It is 
 * expected that Gemini applications that use Pyxis-based authentication
 * may subclass BasicUserGroup to provide additional application-specific
 * user groups.
 *    <p>
 * Note that if any field names are changed in the standard schema,
 * applications are required to overload getCustomVariableBindings or
 * getCustomMethodBindings in order to correctly bind new schema with
 * field names in this class.
 *
 * @see BasicSecurity
 */
public class BasicUserGroup
  implements PyxisUserGroup,
        PersistenceAware
{

  //
  // Constants.
  //

  public static final int MAX_NAME_LENGTH = 50;
  public static final int MIN_NAME_LENGTH = 1;
  public static final int MAX_DESCRIPTION_LENGTH = 500;

  //
  // Member variables.
  //
  private long          id;
  private int           type       = PyxisUserGroup.TYPE_APPLICATION;
  private String        name;          // 50 chars
  private String        description;   // 500 chars
  private boolean       persisted;

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public BasicUserGroup()
  {
  }

  //
  // Mutators
  //

  /**
   * Sets the group's ID.
   */
  @Override
  public void setGroupID(long id)
  {
    this.setId(id);
  }

  /**
   * Sets the group's name.
   */
  @Override
  public void setName(String name)
  {
    this.name = StringHelper.truncate(name, MAX_NAME_LENGTH);
  }

  /**
   * Sets the group's description.
   */
  @Override
  public void setDescription(String description)
  {
    this.description = StringHelper.truncate(description, MAX_DESCRIPTION_LENGTH);
  }

  @Override
  public void setType(int type)
  {
    // Disallow the setting of type to TYPE_BUILTIN.  That type is reserved
    // only for Guests (ID 0), Users (ID 1), and Administrators (ID 1000).
    if (type == PyxisUserGroup.TYPE_BUILTIN)
    {
      this.type = PyxisUserGroup.TYPE_APPLICATION;
    }
    else
    {
      this.type = type;
    }
  }

  //
  // Accessors.
  //

  /**
   * Returns the identity of this user group.
   *
   * @return int identity number of the entity.
   */
  @Override
  public long getId()
  {
    return this.id;
  }

  /**
   * Sets the identity of this user group.
   */
  @Override
  public void setId(long identity)
  {
    this.id = identity;
  }

  @Override
  public int getType()
  {
    return this.type;
  }

  /**
   * Gets the group ID.
   */
  @Override
  public long getGroupID()
  {
    return this.getId();
  }

  /**
   * Gets the group's name.
   */
  @Override
  public String getName()
  {
    return this.name;
  }

  /**
   * Gets the group's description.
   */
  @Override
  public String getDescription()
  {
    return this.description;
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "BasicUserGroup [" + getId() + "; " + getName() + "]";
  }

  //
  // Data Entity required methods.
  //

  @Override
  public boolean isPersisted() {
    return this.persisted;
  }

  @Override
  public void setPersisted(boolean persisted) {
    this.persisted = persisted;
  }

}
