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
import java.util.concurrent.*;

import com.techempower.cache.*;
import com.techempower.data.*;
import com.techempower.gemini.*;
import com.techempower.gemini.messaging.*;
import com.techempower.gemini.pyxis.authorization.*;
import com.techempower.gemini.pyxis.crypto.*;
import com.techempower.gemini.pyxis.listener.*;
import com.techempower.gemini.pyxis.password.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * BasicSecurity provides basic authentication services.  Subclass this
 * class to provide additional user and user-group functionality.
 *   <p>
 * The design approach used herein is that an application's specific
 * subclass of BasicSecurity will serve as a factory for that application's
 * specific user and user groups.  This is why in Gemini v1.21, there was
 * no direct way to get a reference to the PyxisSettings object referenced
 * herein.  In Gemini v1.22, a getSettings method was added, however, to
 * allow for alternative approaches.
 *   <p>
 * Configuration options: TODO: Complete/edit the items below and consider
 * migrating more of the configuration to PyxisSettings so that the items are
 * not specific to this implementation.
 * <ul>
 * <li>BasicSecurity.HashingAlgorithm - Selects the password hashing 
 *     algorithm.  The default is "bcrypt", which is considered secure.
 *     Another option is "plaintext" which is insecure and only suitable
 *     for development environments.</li>
 * <li>BasicSecurity.CaseSensitiveUsernames - If set to yes, two users
 *     can have the same name with different case (e.g., John1 and john1).
 *     If set to no, only one of those may exist.  The default is 'yes'.</li>
 * <li>BasicSecurity.StoreUserAsID - If set to yes, the current user will 
 *     be stored in the session as an integer (the user ID) rather than 
 *     as a user object.  The default is 'no'.</li>
 * </ul>
 * The implementation of BasicSecurity assumes that the application's User
 * class is a subclass of BasicUser and not a custom implementation of
 * PyxisUser.  That is, use of BasicSecurity goes hand-in-hand with use of
 * BasicUser.
 *   <p>
 * Note that the case sensitivity for usernames implemented in BasicSecurity
 * will only work if the database is set to be case sensitive.  Using
 * CachingSecurity avoids this problem (since usernames are checked directly
 * within the User objects).
 *
 * @see PyxisSettings
 */
public class BasicSecurity<U extends PyxisUser, G extends PyxisUserGroup>
  implements PyxisSecurity,
             Configurable
{

  //
  // Constants.
  //

  public static final String COMPONENT_CODE               = "secu";
  public static final int    DEFAULT_FAILED_RESET_SECONDS = 300;  // 5 minutes.
  public static final String PROPS_PREFIX                 = "BasicSecurity.";

  //
  // Member variables.
  //

  private final GeminiApplication          application;
  private final ComponentLog               log;
  private final PyxisSettings              settings;
  private final EntityStore                store;
  private final Cryptograph                cryptograph;
  private final Class<U>                   userClass;
  private final Class<G>                   groupClass;
  private final Class<? extends EntityRelationDescriptor<U,G>> 
                                           userToGroupRelationDefinition;
  private final Class<? extends EntityRelationDescriptor<U,Login>>
                                           userToLoginRelationDefinition;
  private final PasswordRequirement[]      passwordRequirements;
  private final PyxisAuthenticationArbiter arbiter;

  private PasswordHasher passwordHasher        = null;
  private String         loginUri              = "login";
  private String         postLoginUrl          = "/";
  private boolean        requireHttpsForm      = true;
  private boolean        exitHttpsPostLogin    = false;
  private int            failedAttemptLimit    = 0;       // unlimited attempts.
  private int            failedResetSeconds    = DEFAULT_FAILED_RESET_SECONDS;
  private long           nextAutoReset         = 0L;
  private List<SecurityListener<Context>> 
                         listeners             = null;
  // unused unless failed attempt limiting is enabled.
  private Map<String,LoginAttempt> 
                         ipToAttempts          = null;

  /**
   * Constructor.
   */
  public BasicSecurity(GeminiApplication application, Class<U> userClass, 
      Class<G> groupClass, 
      Class<? extends EntityRelationDescriptor<U,G>> userToGroupRelationDefinition,
      Class<? extends EntityRelationDescriptor<U,Login>> userToLoginRelationDefinition)
  {
    this.application    = application;
    this.log            = application.getLog(COMPONENT_CODE);
    this.settings       = constructPyxisSettings(application);
    this.listeners      = new CopyOnWriteArrayList<>();
    final List<PasswordRequirement> requirements = constructPasswordRequirements();
    this.passwordRequirements = requirements.toArray(
        new PasswordRequirement[requirements.size()]);

    // Add this component to be configured by the application's Configurator.
    application.getConfigurator().addConfigurable(this);
    
    this.store = application.getStore();
    this.userClass = userClass;
    this.groupClass = groupClass;
    this.userToGroupRelationDefinition = userToGroupRelationDefinition;
    this.userToLoginRelationDefinition = userToLoginRelationDefinition;
    this.cryptograph = this.constructCryptograph();
    this.arbiter = this.constructAuthenticationArbiter();
    
    if (this.store == null)
    {
      log.log("ERROR: CachingSecurity cannot function without an application cache!", LogLevel.CRITICAL);
    }
    
    addStandardListeners();
  }
  
  /**
   * Constructor
   */
  public BasicSecurity(GeminiApplication application, Class<U> userClass,
      Class<G> groupClass,
      Class<? extends EntityRelationDescriptor<U,G>> userToGroupRelationDefinition)
  {
    this(application,userClass,groupClass,userToGroupRelationDefinition,null);
  }
  
  /**
   * Adds the standard SecurityListeners.  Subclasses can override this method
   * to customize behavior.
   */
  protected void addStandardListeners()
  {
    addListener(new LastLoginUpdater(this));
    addListener(new BasicPasswordChangeListener());
  }
  
  @Override
  public Cryptograph getCryptograph()
  {
    return this.cryptograph;
  }
  
  @Override
  public PyxisAuthenticationArbiter getAuthenticationArbiter()
  {
    return this.arbiter;
  }

  @Override
  public PyxisSettings getSettings()
  {
    return settings;
  }
  
  /**
   * Gets the ComponentLog reference.
   */
  protected ComponentLog getLog()
  {
    return log;
  }
  
  /**
   * Gets the EntityStore reference.
   */
  protected EntityStore getStore()
  {
    return store;
  }

  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    settings.configure(props);
    
    final EnhancedProperties.Focus focus = props.focus(PROPS_PREFIX);
    
    // Support "BasicSecurity.FromEmailAddress" as a legacy setting.
    loginUri              = focus.get("LoginUri", loginUri);
    postLoginUrl          = focus.get("PostLoginUrl", postLoginUrl);
    requireHttpsForm      = focus.getBoolean("RequireHTTPS", requireHttpsForm);
    exitHttpsPostLogin    = focus.getBoolean("ExitHTTPS", exitHttpsPostLogin);
    failedAttemptLimit    = focus.getInt("FailedAttemptLimit", failedAttemptLimit);
    failedResetSeconds    = focus.getInt("FailedResetSeconds", failedResetSeconds);
    
    // Strip leading slash from the login URI since we append that later.
    if (loginUri.startsWith("/"))
    {
      loginUri = loginUri.substring(1);
    }
    
    if (isFailedAttemptLimiting())
    {
      nextAutoReset = DateHelper.getEndOfDay().getTime().getTime();
    }

    final String hashingAlgorithm = focus.get("HashingAlgorithm", "bcrypt");
    switch (hashingAlgorithm)
    {
      case "plaintext": {
        passwordHasher = new PlaintextPasswordHasher();
        break;
      }
      default: {
        passwordHasher = new BCryptPasswordHasher();
        break;
      }
    }
    log.log("Using " + passwordHasher.getName() + " hashing algorithm.");
  }
  
  /**
   * Construct an Cryptograph.
   */
  protected Cryptograph constructCryptograph()
  {
    return new AesGcmNoPaddingCryptograph(this.application);
  }
  
  /**
   * Construct an AuthenticationArbiter.
   */
  protected PyxisAuthenticationArbiter constructAuthenticationArbiter()
  {
    return new SessionAuthenticationArbiter(this.application);
  }
  
  /**
   * Construct the list of the application's password requirements.  The
   * default implementation only enforces password length (minimum 8, maximum
   * 30 characters), and disallows the username to exist within the password.
   *   <p>
   * Overload this method to add optional requirements such as a
   * PasswordComplexity requirement.
   */
  protected List<PasswordRequirement> constructPasswordRequirements()
  {
    final List<PasswordRequirement> toReturn = new ArrayList<>();
    
    toReturn.add(new PasswordLength(BasicUser.PASSWORD_LENGTH));
    toReturn.add(new PasswordDisallowedUsername(false));
    
    return toReturn;
  }

  @Override
  public String getLoginUri()
  {
    return loginUri;
  }
  
  @Override
  public String getPostLoginUrl(Context context)
  {
    final PyxisUser user = getUser(context);

    // Gather the URL that was originally requested prior to displaying the
    // login view.
    String redirectUrl = context.query().get("r");
    if (StringHelper.isEmpty(redirectUrl))
    {
      // If no redirect URL is provided, use a default.
      return getPostLoginUrl(user);
    }
    
    if (redirectUrl.startsWith("/"))
    {
      // Remove the leading slash from the redirect URL.
      redirectUrl = redirectUrl.substring(1);
    }
    
    if (  (!context.isSecure())
       || (isExitHttpsPostLogin())
       )
    {
      // Redirect to the standard/insecure domain if the request was not
      // secure or if we're configured to leave SSL after login.
      redirectUrl = context.getUrl() + redirectUrl;
    }
    else
    {
      // Redirect to the secure domain in all other cases.
      redirectUrl = context.getSecureUrl() + redirectUrl;
    }
    
    return redirectUrl;
  }

  @Override
  public String getPostLoginUrl(PyxisUser user)
  {
    return postLoginUrl;
  }

  @Override
  public boolean isRequireHttpsForm()
  {
    return requireHttpsForm;
  }

  @Override
  public boolean isExitHttpsPostLogin()
  {
    return exitHttpsPostLogin;
  }

  @Override
  public boolean isFailedAttemptLimiting()
  {
    return (failedAttemptLimit > 0);
  }

  @Override  
  public boolean authCheck(Context context, Authorizer authorizer, 
      Rejector rejector)
  {
    PyxisUser user = null;
    
    if (isLoggedIn(context))
    {
      // If a user is already authenticated, let's start with that.
      // Otherwise, let's try a cookie login. 
      user = getUser(context);
    }

    // Still no user?  Or they're not authorized?
    if (  (user != null)
       && (  (authorizer == null)
          || (authorizer.isAuthorized(user, context))
          )
       )
    {
      return true;
    }
    else
    {
      if (rejector != null)
      {
        rejector.reject(context, user);
      }
      else
      {
        forceLoginRejector.reject(context, user);
      }
      
      return false;
    }
  }
  
  @Override
  public boolean authCheck(Context context)
  {
    return authCheck(context, null, null);
  }
  
  /**
   * A Rejector that directs the user to the login page.
   */
  private final Rejector forceLoginRejector = new Rejector()
  {
    @Override
    public void reject(Context context, PyxisUser user)
    {
      // Cookie login wasn't available, let's redirect the user to login.
      final String queryString = context.getQueryString();
      final String requestUri = context.getRequestUri() + (queryString != null
          ? "?" + queryString
          : "");
      final String redirectUrl = isRequireHttpsForm() 
          ? context.getSecureUrl() + getLoginUri()
          : "/" + getLoginUri();
      //log.log("redirect: " + redirectUrl + "\nlogin uri: " + getLoginUri()
      //    + "\nsec url: " + context.getSecureUrl());
      final boolean redirectWarranted = !(postLoginUrl.equals(requestUri));
          
      // If the request appears to be asking for a JSON response, let's 
      // respond with JSON.
      if (GeminiHelper.isJsonRequest(context))
      {
        final Map<String, String> map = new HashMap<>(3);
        map.put("login", redirectUrl);
        map.put("request", requestUri);
        map.put("message", "You need to be logged in to perform this action. Please login and try again.");

        context.setStatus(401);
        GeminiHelper.sendJson(context, map);
      }
      else
      {
        context.messages().put("You need to be logged in to perform this action. Please login and try again.", MessageType.ERROR);
        // Otherwise, compose a redirect that embeds the current URI as a 
        // request parameter.
        context.redirect(redirectUrl + (redirectWarranted 
            ? (StringHelper.containsNullSafe(redirectUrl, "?") ? "&" : "?")
                + "r=" + NetworkHelper.encodeUrl(requestUri)
            : ""));
      }
    }
  };
  
  @Override
  public Rejector getForceLoginRejector()
  {
    return forceLoginRejector; 
  }

  @Override
  public boolean isLoginAttemptPermitted(Context context)
  {
    if (isFailedAttemptLimiting())
    {
      final Map<String, LoginAttempt> map = getIpToAttempts();
      final LoginAttempt attempt = map.get(context.getClientId());

      // Allow for login attempts under two scenarios:
      // 1) No previous attempts
      // 2) Previous attempts have not yet resulted in a lock out
      return attempt == null || attempt.isGood();
    }
    
    // Default: Allow logins.
    return true;
  }
  
  @Override
  public boolean isLoginPermitted(PyxisUser user) 
  {
    return user.isEnabled();
  }

  @Override
  public void captureFailedLoginAttempt(Context context)
  {
    if (isFailedAttemptLimiting())
    {
      // Track a failed attempt.
      final Map<String, LoginAttempt> map = getIpToAttempts();
      LoginAttempt attempt; 
      synchronized (map)
      {
        attempt = map.get(context.getClientId());
        if (attempt == null)
        {
          attempt = new LoginAttempt();
          map.put(context.getClientId(), attempt);
        }
      }
      
      attempt.attempt();
    }
  }
  
  @Override
  public void captureSuccessfulLoginAttempt(Context context)
  {
    if (isFailedAttemptLimiting())
    {
      // Remove the entry from the IP-to-Attempts map if it exists.
      final Map<String, LoginAttempt> map = getIpToAttempts();
      map.remove(context.getClientId());
    }
  }

  /**
   * Gets a reference to the Application.
   */
  public GeminiApplication getApplication()
  {
    return application;
  }

  /**
   * Constructs the PyxisSettings object.
   */
  protected PyxisSettings constructPyxisSettings(GeminiApplication app)
  {
    return new PyxisSettings(app, PROPS_PREFIX);
  }

  @Override
  public boolean passwordTest(PyxisUser user, String password)
  {
    return (  (user != null)
           && (passwordHasher.testPassword(password, user.getUserPassword()))
           );
  }
  
  @Override
  public PyxisUser getUser(String username, String password)
  {
    // Return null if the username or password is not supplied.
    if (username == null || password == null)
    {
      return null;
    }

    final U user = getUserByUsername(username);

    if (passwordTest(user, password))
    {
      return user;
    }
    return null;
  }
  
  /**
   * Retrieves a user by e-mail address.  This method can only
   * work if the PyxisUser implementation is a subclass of BasicWebUser or
   * otherwise implements the method getUserEmail to return the email address
   * of a user.
   *   <p>
   * Returns null if no matching user is found.
   *   <p>
   * Note: This method assumes that e-mail addresses are stored lowercase.
   */
  public U getUserByEmail(String email)
  {
    if (email == null)
    {
      return null;
    }
    
    // We are assuming lowercase, case-insensitive emails.
    return store.get(userClass, "getUserEmail", sanitizeEmailAddress(email));
  }

  /**
   * Retrieves a user by e-mail address and password.  This method can only
   * work if the PyxisUser implementation is a subclass of BasicWebUser or
   * otherwise implements the method getUserEmail to return the email address
   * of a user.
   *   <p>
   * Returns null if no matching user is found.
   *   <p>
   * Note: This method assumes that e-mail addresses are stored lowercase.
   */
  public U getUserByEmail(String email, String password)
  {
    // Return null if the username or password is not supplied.
    if (email == null || password == null)
    {
      return null;
    }

    try
    {
      final U user = getUserByEmail(email);

      // Do we have a candidate?
      if (passwordTest(user, password))
      {
        return user;
      }
    }
    catch (Exception exc)
    {
      // This could come up if the implementation does not provide a
      // getUserEmail method.
      log.log("Exception while retrieving user by email address: " + exc);
    }
    return null;
  }
  
  /**
   * Get a user by username from the entity store, using specific type U.
   * Overload this as necessary to fetch users by username in a different 
   * manner. 
   */
  protected U getUserByUsername(String username)
  {
    return store.get(userClass, "getUserUsername", 
        sanitizeUsername(username));
  }

  @Override
  public PyxisUser getUser(String username)
  {
    // Return null if the username is not supplied.
    if (username == null)
    {
      return null;
    }

    return getUserByUsername(username);
  }
  
  @Override 
  public PyxisUser findUser(String username)
  {
    final PyxisUser byUsername = getUser(username);
    return (byUsername != null)
        ? byUsername
        : getUserByEmail(username);
  }

  @Override
  public PyxisUser getUser(long userID)
  {
    return store.get(userClass, userID);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<PyxisUserGroup> getAllUserGroups()
  {
    return (Collection<PyxisUserGroup>)store.list(groupClass);
  }
  
  @Override
  public PyxisUserGroup getUserGroup(String name)
  {
    return store.get(groupClass, "getName", name);   
  }

  @Override
  public PyxisUserGroup getUserGroup(long identity)
  {
    return store.get(groupClass, identity);
  }
  
  @Override
  public void addUserLogin(long userId, Login login)
  {
    store.getRelation(userToLoginRelationDefinition).add(userId, login);
  }
  
  @Override
  public void removeUserLogin(long userId, Login login)
  {
    store.getRelation(userToLoginRelationDefinition).remove(userId, login);
  }
  
  @Override
  public Collection<Login> getUserLogins(long userId)
  {
    return store.getRelation(userToLoginRelationDefinition)
        .rightValueSet(userId);
  }

  /**
   * Gets the user-to-group relation from the EntityStore.
   */
  private EntityRelation<U,G> getUserToGroupRelation()
  {
    return store.getRelation(userToGroupRelationDefinition);
  }
  
  @Override
  public long[] getGroupsForUser(long userID)
  {
    return getUserToGroupRelation().rightIDArray(userID);
  }
  
  @Override
  public long[] getUsersInGroup(long groupID)
  {
    return getUserToGroupRelation().leftIDArray(groupID);
  }

  @Override
  public boolean updateGroupMembership(long userID, long[] userGroupID)
  {
    final EntityRelation<U,G> relation = getUserToGroupRelation();
    relation.removeLeftValue(userID);
    for (long aUserGroupID : userGroupID)
    {
      relation.add(userID, aUserGroupID);
    }
    
    // Update the user's in-memory record of group membership.
    final BasicUser user = (BasicUser)getUser(userID);
    if (user != null)
    {
      user.setUserGroups(getGroupsForUser(userID));
    }
    
    return true;
  }

  @Override
  public void addUserToGroup(PyxisUser user, long groupID,
    boolean dropAll)
  {
    final EntityRelation<U,G> relation = getUserToGroupRelation();
    // Remove existing relations.
    if (dropAll)
    {
      relation.removeLeftValue(user.getId());
    }

    // Add the new relation.
    relation.add(user.getId(), groupID);
    
    // Update the user's in-memory record of group membership.
    final BasicUser bUser = (BasicUser)user;
    bUser.setUserGroups(getGroupsForUser(user.getId()));
  }

  @Override
  public void removeUserFromGroup(PyxisUser user, long groupID)
  {
    final EntityRelation<U,G> relation = getUserToGroupRelation();
    // Remove the specified group from the user's list.
    relation.remove(user.getId(), groupID);

    // Update the user's in-memory record of group membership.
    final BasicUser bUser = (BasicUser)user;
    bUser.setUserGroups(getGroupsForUser(user.getId()));
  }
  
  /**
   * ToString
   */
  @Override
  public String toString()
  {
    return "CachingSecurity [u:" 
        + (userClass != null ? userClass.hashCode() : "<not-specified>") 
        + "]";
  }

  /**
   * Saves the user's group memberships to the database.
   *
   * @param user The user
   * @return Whether the save was successful.
   */
  public boolean saveGroupMembership(BasicUser user)
  {
    return updateGroupMembership(user.getId(), user.getUserGroups());
  }

  @Override
  public void setUserMembership(PyxisUser user, long groupID, boolean member)
  {
    if (member)
    {
      addUserToGroup(user, groupID);
    }
    else
    {
      removeUserFromGroup(user, groupID);
    }
  }
  
  @Override
  public void addUserToGroup(PyxisUser user, PyxisUserGroup group)
  {
    addUserToGroup(user, group, false);
  }

  @Override
  public void addUserToGroup(PyxisUser user, long groupID)
  {
    addUserToGroup(user, groupID, false);
  }

  @Override
  public void addUserToGroup(PyxisUser user, PyxisUserGroup group, boolean dropAll)
  {
    // Bad group.
    if (group == null)
    {
      return;
    }

    addUserToGroup(user, group.getId(), dropAll);
  }

  @Override
  public void removeUserFromGroup(PyxisUser user, PyxisUserGroup group)
  {
    // Bad group.
    if (group == null)
    {
      return;
    }

    removeUserFromGroup(user, group.getId());
  }
  
  @Override
  public boolean login(Context context, String username, String password, 
      boolean save)
  {
    PyxisUser user = getUser(username, password);
    
    // If we didn't find the user via their username, and authentication via
    // email is enabled, let us try to find the user by their email address.
    if ( (user == null)
      && (settings.isEmailAuthenticationEnabled())
      )
    {
      // Send the username as a candidate email address.
      user = getUserByEmail(username, password);
    }

    return login(context, user, save);
  }

  @Override
  public boolean login(Context context, PyxisUser user, boolean save)
  {
    // Only proceed if the user is non-null and they are permitted to login.
    if (  (user != null)
       && (isLoginPermitted(user))
       )
    {      
      arbiter.login(context, user, save);
      
      // Record a successful login attempt (clearing a count of failed 
      // attempts if one exists).
      captureSuccessfulLoginAttempt(context);
      
      // Notify listeners.
      for (SecurityListener<Context> notify : listeners)
      {
        notify.loginSuccessful(context, user);
      }
      
      log.log("Logged in: " + user);

      return true;
    }
    // If the user is null or not enabled, then consider this an 
    // authentication failure.
    else
    {
      // Record a failed login attempt.
      captureFailedLoginAttempt(context);

      // Notify listeners.
      for (SecurityListener<Context> notify : listeners)
      {
        notify.loginFailed(context);
      }
    }

    return false;
  }

  @Override
  public void logout(Context context)
  {
    final PyxisUser user = getUser(context);
    
    notifyListenersLogout(user, context);

    arbiter.logout(context);
  }
  
  @Override
  public void logout(PyxisUser user)
  {
    // Without a reference context, there's not much to do except to notify
    // the listeners.    
    notifyListenersLogout(user, null);
  }
  
  /**
   * Notify the listeners about a logout.
   */
  protected void notifyListenersLogout(PyxisUser user, Context context)
  {
    if (user != null)
    {
      // Notify listeners.
      for (SecurityListener<Context> notify : listeners)
      {
        notify.logoutSuccessful(context, user);
      }
    }
  }

  @Override
  public PyxisUser getUser(Context context)
  {    
    return arbiter.getUser(context);
  }

  @Override
  public boolean isLoggedIn(Context context)
  {
    return arbiter.isLoggedIn(context);
  }

  @Override
  public PasswordHasher getPasswordHasher()
  {
    return passwordHasher;
  }

  @Override
  public List<String> passwordValidate(PasswordProposal proposal)
  {
    // Bypass validation if the proposal so requests.
    if (proposal.bypassValidation)
    {
      return Collections.emptyList();
    }
    
    final List<String> toReturn = new ArrayList<>(passwordRequirements.length);
    
    // Ask each of our password requirements to evaluate the proposal.
    for (PasswordRequirement requirement : passwordRequirements)
    {
      final String result = requirement.validate(proposal);
      if (result != null)
      {
        toReturn.add(result);
      }
    }
    
    return toReturn;
  }
  
  @Override
  public List<String> passwordChange(PasswordProposal proposal)
  {
    final List<String> validation = passwordValidate(proposal);
    if (validation.isEmpty())
    {
      proposal.hashedPassword = passwordHasher.encryptPassword(proposal.password);
      proposal.user.setUserPassword(proposal.hashedPassword);

      // Notify listeners.
      for (SecurityListener<Context> notify : listeners)
      {
        notify.passwordChanged(proposal);
      }
      
      // Note that this method does *not* save the user because all callers
      // are expected to persist the user record.
      // TODO: Change this behavior to persist here.  Remove the persistance
      // from callers.
    }
    
    return validation;
  }

  /**
   * Persists a user to the database.  
   */
  @Override
  public void saveUser(PyxisUser user)
  {
    // By default, a lazy persistence via EntityUpdater is used.  Subclasses 
    // may override this to persist the user immediately.
    application.getEntityUpdater().add(user);
  }
  
  @Override
  public void beginMasquerade(Context context, PyxisUser impersonatedUser) 
  {
    arbiter.beginMasquerade(context, impersonatedUser);
  }

  @Override
  public PyxisUser getMasqueradingUser(Context context) 
  {
    return arbiter.getMasqueradingUser(context);
  }

  @Override
  public boolean endMasquerade(Context context) 
  {
    return arbiter.endMasquerade(context);
  }

  /**
   * Gets the IP-to-Attempts map, creating it if necessary.
   */
  protected synchronized Map<String,LoginAttempt> getIpToAttempts()
  {
    // Every day, we'll reset this map to clean up orphaned entries. 
    autoResetAttemptsIfNeeded();

    if (ipToAttempts == null)
    {
      ipToAttempts = new HashMap<>();
    }
    
    return ipToAttempts;
  }
  
  /**
   * Performs an automatic clear/reset of the IP-to-attempts map on a daily
   * basis.
   */
  protected void autoResetAttemptsIfNeeded()
  {
    if (System.currentTimeMillis() > nextAutoReset)
    {
      resetLoginAttempts();
      
      // Reset at the next end of day.
      nextAutoReset = DateHelper.getEndOfDay().getTime().getTime();
    }
  }
  
  /**
   * Resets the IP-to-Attempts map.
   */
  protected synchronized void resetLoginAttempts()
  {
    ipToAttempts = null;
  }

  /**
   * A simple data structure class for tracking login attempts.
   */
  protected class LoginAttempt
  {
    private int count = 0;
    private long resetTime = 0L;
    
    public LoginAttempt()
    {
    }
    
    public void setFailState()
    {
      count = 0;
      resetTime = System.currentTimeMillis() 
          + (BasicSecurity.this.failedResetSeconds * UtilityConstants.SECOND);
    }
    
    public boolean isGood()
    {
      return (resetTime < System.currentTimeMillis());
    }
    
    public void attempt()
    {
      count++;
      if (count >= BasicSecurity.this.failedAttemptLimit)
      {
        setFailState();
      }
    }
  }

  @Override
  public PyxisUser constructUser()
  {
    return new BasicUser(this);
  }

  @Override
  public PyxisUserGroup constructUserGroup()
  {
    return new BasicUserGroup();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <C extends Context> void addListener(SecurityListener<C> listener)
  {
    listeners.add((SecurityListener<Context>)listener);
  }

  @Override
  public <C extends Context> void removeListener(SecurityListener<C> listener)
  {
    listeners.remove(listener);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Class<PyxisUser> getUserClass()
  {
    return (Class<PyxisUser>)userClass;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Class<PyxisUserGroup> getUserGroupClass()
  {
    return (Class<PyxisUserGroup>)groupClass;
  }

  @Override
  public String sanitizeUsername(String username)
  {
    Objects.requireNonNull(username, "Username may not be null.");
    // BasicSecurity assumes that usernames are lower case, and we may as
    // well trim while we're here.
    return StringHelper.trim(
        StringHelper.truncate(
            username, BasicUser.USERNAME_LENGTH.max)).toLowerCase();
  }
  
  @Override
  public String sanitizeEmailAddress(String email)
  {
    Objects.requireNonNull(email, "Email may not be null.");
    return StringHelper.trim(
        StringHelper.truncate(
            email, BasicWebUser.EMAIL_LENGTH.max)).toLowerCase();
  }

  @Override
  public String sanitizeFirstname(String firstname)
  {
    Objects.requireNonNull(firstname, "Firstname may not be null.");
    return StringHelper.truncate(firstname, BasicUser.MAX_NAME_LENGTH);
  }

  @Override
  public String sanitizeLastname(String lastname)
  {
    Objects.requireNonNull(lastname, "Lastname may not be null.");
    return StringHelper.truncate(lastname, BasicUser.MAX_NAME_LENGTH);
  }

}   // End BasicSecurity.
