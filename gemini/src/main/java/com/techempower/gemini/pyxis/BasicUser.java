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

import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * BasicUser is a fundamental User representation.  It is expected that
 * Gemini applications that use Pyxis-based authentication will subclass
 * BasicUser to provide additional user functionality.
 *    <p>
 * Note that if any field names are changed in the standard schema,
 * applications are required to overload getCustomSetMethodBindings and
 * getCustomGetMethodBindings in order to correctly bind new schema with
 * field names in this class.
 */
public class BasicUser
  implements PyxisUser, 
             Initializable
{
  //
  // Constants.
  //
  public static final IntRange USERNAME_LENGTH = new IntRange(4, 100);
  public static final IntRange PASSWORD_LENGTH = new IntRange(4, 30);
  public static final int      MAX_NAME_LENGTH = 50;
  
  public static final Comparator<BasicUser> SORT_LASTNAME_FIRSTNAME =
      new SortLastnameFirstname();
  
  static class SortLastnameFirstname 
    implements Comparator<BasicUser>
  {
    @Override
    public int compare(BasicUser u1, BasicUser u2)
    {
      int ln = StringHelper.compareToNullSafe(u1.getUserLastname(), u2.getUserLastname());
      if (ln < 0)
      {
        return -1;
      }
      else if (ln > 0)
      {
        return 1;
      }
      else
      {
        int fn = StringHelper.compareToNullSafe(u1.getUserFirstname(), u2.getUserFirstname());
        if (fn < 0)
        {
          return -1;
        }
        else if (fn > 0)
        {
          return 1;
        }
        else
        {
          return 0;
        }
      }
    }
  }
  
  //
  // Member variables.
  //

  private long    userID                 = 0;
  private String  userUsername           = "";   // 30 chars
  private String  userPassword           = "";   // 30 chars
  private String  userFullname           = "";
  private String  userFirstname          = "";   // 50 chars
  private String  userLastname           = "";   // 50 chars
  private boolean enabled                = true;
  private long[]  userGroups             = new long[0];
  private Date    userLastLogin          = null;
  private Date    userLastPasswordChange = null;

  // "Cached" quick look-up flags for the default groups.
  private transient boolean   userGroupsSet               = false;
  private transient boolean   memberAdministrators        = false;
  private transient boolean   memberUsers                 = false;
  private transient boolean   memberGuests                = false;
  private transient boolean   initialized                 = false;
  
  // BasicUser requires a BasicSecurity
  private final BasicSecurity<? extends PyxisUser, ? extends PyxisUserGroup> security;

  //
  // Member methods.
  //

  /**
   * Constructor.  Takes a reference to the PyxisSettings.
   */
  public BasicUser(BasicSecurity<? extends PyxisUser, ? extends PyxisUserGroup> security)
  {
    this.security = security;
  }
  
  /**
   * Gets the Security reference.
   */
  public BasicSecurity<? extends PyxisUser, ? extends PyxisUserGroup> getSecurity()
  {
    return this.security;
  }

  /**
   * Sets the user ID.
   */
  @Override
  public void setId(long userID)
  {
    this.userID = userID;
  }
  
  /**
   * Set the enabled flag.  Disabled users are generally not allowed to
   * log into a system.
   */
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }

  /**
   * Sets the user's username.
   */
  @Override
  public BasicUser setUserUsername(String username)
  {
    this.userUsername = this.security.sanitizeUsername(username);
    return this;
  }

  /**
   * Sets the user's password.  Applications should <b>not</b> call this 
   * directly.  Instead, use <code>security.passwordChange</code>.
   */
  @Override
  public void setUserPassword(String password)
  {
    this.userPassword = password;
  }

  /**
   * Sets the user's first name.
   */
  public BasicUser setUserFirstname(String firstname)
  {
    this.userFirstname = this.security.sanitizeFirstname(firstname);
    generateFullname();
    return this;
  }

  /**
   * Sets the user's last name.
   */
  public BasicUser setUserLastname(String lastname)
  {
    this.userLastname = this.security.sanitizeLastname(lastname);
    generateFullname();
    return this;
  }

  @Override
  public BasicUser setUserLastLogin(Date lastLogin)
  {
    if (lastLogin != null)
    {
      this.userLastLogin = new Date(lastLogin.getTime());
    }
    else
    {
      this.userLastLogin = null;
    }
    return this;
  }
  
  @Override
  public Date getUserLastLogin()
  {
    if (this.userLastLogin != null)
    {
      return new Date(this.userLastLogin.getTime());
    }
    else
    {
      return null;
    }
  }

  @Override
  public BasicUser setUserLastPasswordChange(Date lastPasswordChange)
  {
    if (lastPasswordChange != null)
    {
      this.userLastPasswordChange = new Date(lastPasswordChange.getTime());
    }
    else
    {
      this.userLastPasswordChange = null;
    }
    return this;
  }
  
  @Override
  public Date getUserLastPasswordChange()
  {
    if (this.userLastPasswordChange != null)
    {
      return new Date(this.userLastPasswordChange.getTime());
    }
    else
    {
      return null;
    }
  }

  /**
   * Gets the previously assigned user groups.
   */
  public long[] getUserGroups()
  {
    gatherUserGroups(false);
    long[] toReturn = new long[this.userGroups.length];
    System.arraycopy(this.userGroups, 0, toReturn, 0, this.userGroups.length);
    
    return toReturn;
  }

  /**
   * Sets the user's groups.
   *   <p>
   * Because this method will only update the in-memory representation of a 
   * user's groups, it is recommended that you call 
   * PyxisSecurity.updateGroupMembership rather than call this method
   * directly.
   *   <p>
   * This method is called by implementations of 
   * PyxisSecurity.updateGroupMembership.
   */
  public void setUserGroups(long[] groups)
  {
    this.userGroups = new long[groups.length];
    System.arraycopy(groups, 0, this.userGroups, 0, groups.length);

    evaluateGroups();
  }

  /**
   * Sets the user's groups.  Provide a Collection of PyxisUserGroups.
   *   <p>
   * As with setUserGroups(long[]), it is preferred that you call
   * PyxisSecurity.updateGroupMembership rather than this method.
   */
  public void setUserGroups(Collection<PyxisUserGroup> groups)
  {
    // Copy the parameters to an Array List of boxed longs.
    this.userGroups = new long[groups.size()];
    
    int i = 0;
    for (PyxisUserGroup group : groups)
    {
      this.userGroups[i++] = group.getId();
    }

    evaluateGroups();
  }
  
  /**
   * Gathers the user groups from the cache or database.
   * 
   * @param regather Should groups be gathered even if they've already been
   *        gathered for this object?
   */
  protected void gatherUserGroups(boolean regather)
  {
    if ((regather) || (!this.userGroupsSet))
    {
      long[] groups = this.security.getGroupsForUser(getId());
      setUserGroups(groups);
    }
  }

  /**
   * Returns {@code true} if the groups for this user have been set.
   */
  protected boolean isUserGroupsSet()
  {
    return this.userGroupsSet;
  }

  /**
   * Gets the user ID.
   */
  @Override
  public long getId()
  {
    return this.userID;
  }
  
  /**
   * Is the user enabled?  All users are enabled by default but can be 
   * disabled by calling setEnabled(false).
   */
  @Override
  public boolean isEnabled()
  {
    return this.enabled;
  }

  /**
   * Gets the userUsername.
   */
  @Override
  public String getUserUsername()
  {
    return this.userUsername;
  }

  /**
   * Gets the userPassword.  This method is final to prevent overloading.
   */
  @Override
  public final String getUserPassword()
  {
    return this.userPassword;
  }

  /**
   * Gets the user's first name.
   */
  public String getUserFirstname()
  {
    return this.userFirstname;
  }

  /**
   * Gets the user's last name.
   */
  public String getUserLastname()
  {
    return this.userLastname;
  }

  /**
   * Gets the user's full name.
   */
  public String getUserFullname()
  {
    return this.userFullname;
  }

  //
  // Pass-throughs to the standard user groups.
  //

  /**
   * Is this user an Administrator?
   */
  @Override
  public final boolean isAdministrator()
  {
    gatherUserGroups(false);
    return this.memberAdministrators;
  }

  /**
   * Is this user in the Users group?
   */
  @Override
  public final boolean isUser()
  {
    gatherUserGroups(false);
    return this.memberUsers;
  }

  /**
   * Is this user a Guest?
   */
  @Override
  public final boolean isGuest()
  {
    gatherUserGroups(false);
    return this.memberGuests;
  }

  //
  // Other public methods.
  //

  /**
   * Is this user a member of a specific user group?
   */
  @Override
  public final boolean isMember(PyxisUserGroup group)
  {
    gatherUserGroups(false);

    return group != null && isMember(group.getGroupID());
  }

  /**
   * Is this user a member of a specific user group?
   */
  @Override
  public final boolean isMember(long groupID)
  {
    gatherUserGroups(false);

    // Go through the user's groups...
    for (long userGroup : this.userGroups)
    {
      if (userGroup == groupID)
      {
        return true;
      }
    }

    // Group ID not found, return false.
    return false;
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "User [id: " + getId() + "; un: " + getUserUsername() + "]";
  }

  //
  // Protected methods.
  //

  /**
   * Examines the groups that the user is a part of and sets the flags
   * for the default groups appropriately.
   *   <p>
   * Calling evaluateGroups sets the "userGroupsSet" flag to true.
   */
  protected void evaluateGroups()
  {
    this.memberAdministrators = false;
    this.memberUsers          = false;
    this.memberGuests         = false;

    // Set up the standard group flags if the user is a member.
    for (long userGroup : this.userGroups)
    {
      if (userGroup == GROUP_ADMINISTRATORS)
      {
        this.memberAdministrators = true;
      }
      else if (userGroup == GROUP_USERS)
      {
        this.memberUsers = true;
      }
      else if (userGroup == GROUP_GUESTS)
      {
        this.memberGuests = true;
      }
    }
    
    this.userGroupsSet = true;
  }

  /**
   * Constructs the user's full name.  This is called automatically whenever
   * the user's first or last name is set.
   */
  protected void generateFullname()
  {
    this.userFullname = this.userFirstname + " " + this.userLastname;
  }

  //
  // Data Entity required methods.
  //

  @Override
  public void initialize()
  {
    generateFullname();
    this.initialized = true;
  }

  @Override
  public boolean isInitialized()
  {
    return this.initialized;
  }

}   // End BasicUser.
