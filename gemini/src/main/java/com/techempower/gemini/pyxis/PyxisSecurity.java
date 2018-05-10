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

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.authorization.*;
import com.techempower.gemini.pyxis.crypto.*;
import com.techempower.gemini.pyxis.listener.*;
import com.techempower.gemini.pyxis.password.*;

/**
 * PyxisSecurity is a simple security provider interface for Gemini 
 * applications.  The default implementation is named BasicSecurity.
 *
 * @see BasicSecurity
 * @see PyxisSettings
 */
public interface PyxisSecurity
{
  /**
   * React to a failed login attempt.  The default functionality checks if
   * failed-attempt limiting is enabled.  If so, a count of failed attempts
   * made by the client's IP address will be tracked.
   */
  void captureFailedLoginAttempt(Context context);

  /**
   * React to a successful login attempt.  The default functionality will
   * clear any failed-attempt counting if failed-attempt limiting is enabled. 
   */
  void captureSuccessfulLoginAttempt(Context context);
  
  /**
   * Get a reference to the password hashing algorithm.
   */
  PasswordHasher getPasswordHasher();
  
  /**
   * Gets a reference to the security settings.
   */
  PyxisSettings getSettings();
  
  /**
   * Intercept a request for a secure item, and attempt to automatically login
   * the user if possible.  If the user cannot be authenticated or does not
   * meet the requirements of the optional Authorizer, the Rejector will be
   * used to reject the request.
   *   <p>
   * Note that the request <b>will be handled</b> in either case, so once an
   * authorization check is executed, the Handler that called authCheck
   * should return true even if the authorization check failed.  E.g.,
   *   <p>
   * <code><pre>
   *   @PathSegment
   *   public boolean doSomething()
   *   {
   *     if (authCheck(context()))
   *     { 
   *       return json(user());
   *     }
   *     
   *     return true;
   *   }
   * </pre></code>
   * 
   * @return true if the authorization check passed.
   * 
   * @param context the request context.
   * @param authorizer an optional Authorizer to validate that the user has
   *   sufficiency authorization/permission.
   * @param rejector an optional Rejector to reject the request of any
   *   unauthorized user; by default the force-login Rejector will be used
   *   to redirect an unauthorized user to the login view.
   */
  boolean authCheck(Context context, Authorizer authorizer, Rejector rejector);
  
  /**
   * A variation of the authorization check that does not use any special
   * authorization check (a user simply needs to be authenticated) and uses
   * the plain redirect-to-login Rejector.
   * 
   * @return true if the authorization check passed.
   * 
   * @param context the request context.
   */
  boolean authCheck(Context context);
  
  /**
   * Return the Cryptograph.
   */
  Cryptograph getCryptograph();
  
  /**
   * Return the Arbiter
   */
  PyxisAuthenticationArbiter getAuthenticationArbiter();

  /**
   * Return a Rejector that redirects the user to the login view.
   */
  Rejector getForceLoginRejector();
  
  /**
   * Get the URI of the login handler.
   */
  String getLoginUri();

  /**
   * Get the URL to direct to after login, assuming one was provided with the
   * login request as an "r" parameter.  Note that the "r" HTTP request 
   * parameter is relative and the implementation should prefix the base URI
   * to ensure an absolute URL is created.
   */
  String getPostLoginUrl(Context context);
  
  /**
   * Get the URL to direct to after login, assuming no other URL is available.
   */
  String getPostLoginUrl(PyxisUser user);
  
  /**
   * If the site is not full-SSL, should we enter SSL prior to rendering the
   * login view?  Note that most sites are now full SSL and for that type of
   * site, this setting has no effect.
   */
  boolean isRequireHttpsForm();

  /**
   * If the site is not full-SSL, should SSL be exited after login?  Note that
   * most sites are now full SSL and for that type of site, this setting has 
   * no effect.
   */
  boolean isExitHttpsPostLogin();
  
  /**
   * Does the Security implementation limit failed login attempts?
   */
  boolean isFailedAttemptLimiting();
  
  /**
   * Is the provided user permitted to login?  By default, all enabled users
   * can login; all disabled users cannot.
   */
  boolean isLoginPermitted(PyxisUser user);
  
  /**
   * Is a login attempt permitted for the current COntext?
   */
  boolean isLoginAttemptPermitted(Context context);
  
  /**
   * Validate a proposed password for a User.  Returns a list of String error
   * messages for any password requirements not met by the proposal.  If the
   * proposal meets the requirements, the returned list will be empty.
   *   <p>
   * This function only validates that the proposal is acceptable.  It does 
   * not make any actual changes to the user.
   * 
   * @param proposal The PasswordProposal being offered by the user.
   */
  List<String> passwordValidate(PasswordProposal proposal);
  
  /**
   * Change a user's password with the provided proposal.  Runs a validation
   * check using passwordValidate first.  If any validation rules fail, the
   * change will not be made and the resulting list of errors will be 
   * returned.  If the validation rules pass, the change will be applied and
   * the returned list will be empty.
   *    <p>
   * If the password is changed, the implementation must also update the
   * user's lastPasswordChange date.
   *    <p>
   * The user record is <b>not</b> persisted by this function, however. The
   * calling code must handle persistence. 
   * 
   * @param proposal The PasswordProposal being offered by the user.
   */
  List<String> passwordChange(PasswordProposal proposal);
  
  /**
   * Tests a provided password.
   * 
   * @param user The user account being tested.
   * @param password The password provided by the user. 
   */
  boolean passwordTest(PyxisUser user, String password);
  
  /**
   * Updates the user's groups by deleting the current group memberships and
   * then inserting the new ones.
   */
  boolean updateGroupMembership(long userID, long[] userGroupID);
  
  /**
   * Adds a SecurityListener to be notified of security events.
   */
  <C extends Context> void addListener(SecurityListener<C> listener);
  
  /**
   * Removes a SecurityListener to be notified of security events.
   */
  <C extends Context> void removeListener(SecurityListener<C> listener);

  /**
   * Retrieves from the database (or a cache) a user that matches the
   * provided username and password.  Applications are highly encouraged
   * to overload this method to provide more optimum access to user records.
   * Applications are required to overload this method if a custom 
   * implementation of PyxisUser (that is, not a subclass of BasicUser) is
   * returned by constructUser.
   *    <p>
   * Usernames must be unique!
   *    <p>
   * NOTE: Returns null if constructUser is overloaded and <i>does not</i>
   * return a BasicUser subclass (rather, a different implementation of
   * PyxisUser.)
   *
   * @return a PyxisUser if found, null if not found.
   */
  PyxisUser getUser(String username, String password);

  /**
   * Retrieves from the database (or a cache) a user that matches the
   * provided username.  Applications are highly encouraged to overload this
   * method to provide more optimum access to user records.  Applications are
   * required to overload this method if a custom implementation of PyxisUser
   * (that is, not a subclass of BasicUser) is returned by constructUser.
   *    <p>
   * Usernames must be unique!
   *    <p>
   * NOTE: Returns null if constructUser is overloaded and <i>does not</i>
   * return a BasicUser subclass (rather, a different implementation of
   * PyxisUser.)
   *
   * @return a PyxisUser if found, null if not found.
   */
  PyxisUser getUser(String username);
  
  /**
   * Finds a user that matches the provided username, optionally searching
   * beyond the username field.  In BasicSecurity, the implementation first
   * attempts to find a user with a matching username and then checks for a
   * user with a matching e-mail address.
   *   <p>
   * Contrast to getUser(username) which only finds a user by the username
   * field.
   */
  PyxisUser findUser(String username);

  /**
   * Retrieves from the database (or a cache) a user that matches the
   * provided user ID.  Applications are highly encouraged to overload
   * this method to provide more optimum access to user records.
   * Applications are required to overload this method if a custom 
   * implementation of PyxisUser (that is, not a subclass of BasicUser) is
   * returned by constructUser.
   *    <p>
   * NOTE: Returns null if constructUser is overloaded and <i>does not</i>
   * return a BasicUser subclass (rather, a different implementation of
   * PyxisUser.)
   *
   * @return a PyxisUser if found, null if not found.
   */
  PyxisUser getUser(long userID);

  /**
   * Persist a user to data storage.
   */
  void saveUser(PyxisUser user);
  
  /**
   * Retrieves from the database, cache, or another source, a collection
   * of PyxisUserGroups that represents all possible user groups featured
   * by the application.
   *    <p>
   * The basic implementation of this class always hits the database and
   * always reconstructs the user groups.  It also constructs the user
   * groups as BasicUserGroup objects, thus nullifying any benefits gained
   * by implementing subclasses of BasicUserGroup.
   *    <p>
   * For that reason, applications are encouraged to overload this method 
   * in their BasicSecurity subclasses to provide BasicUserGroup subclasses
   * in the collection that is returned rather than pure BasicUserGroups.
   *    <p>
   * Depending on the application, groups may not be stored in the database.
   * An application can be designed where group definitions exist in the
   * application's .conf file.  In those cases, this method will need to be
   * overloaded.
   */
  Collection<PyxisUserGroup> getAllUserGroups();
  
  /**
   * Gets the ID numbers of the groups a user is a member of.  This method
   * replaces getGroupMembership and returns a simple long[] data structure
   * rather than using a Collection of BoxedLongs.
   */
  long[] getGroupsForUser(long userID);

  /**
   * Gets the ID numbers of the users within a specified group.
   */
  long[] getUsersInGroup(long groupID);
  
  /**
   * Adds a Login for the given userId.
   */
  void addUserLogin(long userId, Login login);
  
  /**
   * Removes a Login for the given userId.
   */
  void removeUserLogin(long userId, Login login);
  
  /**
   * Gets the Logins for a given userId.
   */
   Collection<Login> getUserLogins(long userId);

  /**
   * Gets a User Group by name, returning null if no matching User Group
   * could be found.
   * 
   * @param name The name of the User Group to return.
   */
  PyxisUserGroup getUserGroup(String name);

  /**
   * Gets a User Group by identity, returning null if no matching User Group
   * could be found.
   * 
   * @param identity The ID of the User Group to return.
   */
  PyxisUserGroup getUserGroup(long identity);

  /**
   * Takes a username and password and places a user into session if the
   * "login" is acceptable.  Returns a boolean success flag.
   * 
   * @param context the request context
   * @param username the provided username
   * @param password the provided password
   * @param save if permitted, should a cookie be sent to save the credentials
   *        on the client?
   */
  boolean login(Context context, String username, String password, boolean save);
  
  /**
   * Places a provided PyxisUser into session, effectively logging them
   * into the application.  Use this method as an alternative to
   * login(context, username, password) if you do not want Pyxis to
   * perform the authentication of a user for you.
   * 
   * @param context the request context
   * @param user the user to login.
   * @param save if permitted, should a cookie be sent to save the credentials
   *        on the client?
   */
  boolean login(Context context, PyxisUser user, boolean save);
  
  /**
   * Logout a user from the provided Context.
   */
  void logout(Context context);
  
  /**
   * Logout a user provided just the user.  This method has been added to
   * allow the tracking of "logouts" caused by session expiration, where
   * a request Context is not available.
   */
  void logout(PyxisUser user);

  /**
   * Constructs a PyxisUser object or a subclass thereof.  Applications
   * should overload this method to return an instance of a BasicUser
   * subclass.
   */
  PyxisUser constructUser();

  /**
   * Constructs a PyxisUserGroup object or a subclass thereof.  Applications
   * should overload this method to return an instance of a BasicUserGroup
   * subclass.
   */
  PyxisUserGroup constructUserGroup();

  /**
   * Gets the logged-in user from the Context's session.  Returns null
   * if no user is logged in.
   *
   * @param context the Context from which to retrieve a user.
   */
  PyxisUser getUser(Context context);

  /**
   * Is a user logged in?
   */
  boolean isLoggedIn(Context context);

  /**
   * Sets a user's membership within a group according to the provided
   * "member" boolean parameter.  Calls addUserToGroup or removeUserFromGroup
   * accordingly.
   */
  void setUserMembership(PyxisUser user, long groupID, boolean member);

  /**
   * Adds the given user to the given group.
   *
   * @param user The user
   * @param group The group to add the user to
   */
  void addUserToGroup(PyxisUser user, PyxisUserGroup group);

  /**
   * Adds the given user to the given group.
   *
   * @param user The user
   * @param groupID The ID of the group to add the user to.
   */
  void addUserToGroup(PyxisUser user, long groupID);

  /**
   * Adds the given user to the given group.
   * If dropAll is true then all pre-existing groups will be removed first.
   *
   * @param user The user
   * @param group The group to add the user to
   * @param dropAll Whether to remove all pre-existing memberships prior to adding the new one.
   */
  void addUserToGroup(PyxisUser user, PyxisUserGroup group, boolean dropAll);

  /**
   * Adds the given user to the given group.
   * If dropAll is true then all pre-existing groups will be removed first.
   *
   * @param user The user.
   * @param groupID The ID of the group to add the user to.
   * @param dropAll Whether to remove all pre-existing memberships prior to adding the new one.
   */
  void addUserToGroup(PyxisUser user, long groupID, boolean dropAll);

  /**
   * Removes the given user from the given group.
   *
   * @param user The user
   * @param group The group to remove the user from
   */
  void removeUserFromGroup(PyxisUser user, PyxisUserGroup group);

  /**
   * Removes the given user from the given group.
   *
   * @param user The user
   * @param groupID The ID of the group to remove the user from
   */
  void removeUserFromGroup(PyxisUser user, long groupID);
  
  /**
   * Get the class of the application's User objects. 
   */
  Class<PyxisUser> getUserClass();
  
  /**
   * Get the class of the application's UserGroup objects.
   */
  Class<PyxisUserGroup> getUserGroupClass();
  
  /**
   * Begin masquerading as another user.  For hopefully-obvious reasons,
   * implementations should limit this functionality to administrators.  Once
   * in a masquerade, calls to getUser() should return the impersonated user,
   * rather than the administrator, until endMasquerade is called.
   *   <p>
   * Logout will end the masquerade and logout the system administrator.
   */
  void beginMasquerade(Context context, PyxisUser impersonatedUser);
  
  /**
   * Gets the masquerading user, if a masquerade is active.  If there is
   * no active masquerade, returns null.
   */
  PyxisUser getMasqueradingUser(Context context);
  
  /**
   * Ends masquerading, returning to a default state where calls to getUser
   * will return the administrator themselves.  Returns true if an existing
   * masquerade has been ended; returns false if nothing was done.
   */
  boolean endMasquerade(Context context);
  
  /**
   * Sanitizes the given username based on the rules of this security.  The
   * default implementation converts the provided username to all-lowercase
   * and enforces a maximum length.
   * 
   * @param username The username to sanitize
   * @return The sanitized username
   */
  String sanitizeUsername(String username);
  
  /**
   * Sanitizes the given e-mail address based on the rules of this security.
   * The default implementation converts the provided address to all-lowercase
   * and enforces a maximum length.
   */
  String sanitizeEmailAddress(String address);
  
  /**
   * Sanitize the given firstname based on the rules of this security.  The
   * default implementation enforces a maximum length.
   * 
   * @param firstname The firstname to sanitize
   * @return The sanitized firstname
   */
  String sanitizeFirstname(String firstname);
  
  /**
   * Sanitize the given lastname based on the rules of this security.  The
   * default implementation enforces a maximum length.
   * 
   * @param lastname The lastname to sanitize
   * @return The sanitized lastname
   */
  String sanitizeLastname(String lastname);

}  // End PyxisSecurity.
