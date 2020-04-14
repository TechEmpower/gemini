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

package com.techempower.gemini.pyxis.handler;

import com.techempower.gemini.*;
import com.techempower.gemini.context.*;
import com.techempower.gemini.input.*;
import com.techempower.gemini.input.processor.*;
import com.techempower.gemini.input.validator.*;
import com.techempower.gemini.path.*;
import com.techempower.gemini.path.annotation.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.util.*;

/**
 * A reusable foundation for application login functionality.  This
 * LoginHandler provides integration with Pyxis-based web-application
 * security.  If your Gemini application is using Pyxis components for the
 * basis of your user model, this LoginHandler can provide basic login
 * and logout functionality.
 *   <p>
 * The mustache templates used by this and the LogoutHandler are typically
 * stored within the "auth" subdirectory of your mustache root.
 *   <p>
 * The behavior of this handler can be customized through your application's
 * configuration (properties) file.  Some behaviors can be further customized
 * only through Java subclassing.
 *   <p>
 * The following parameters are read from the application's properties file.
 * Note that the "LoginHandler." prefix can be changed to anything you'd like
 * as long as you provide an alternate properties prefix when calling this
 * object's constructor.
 *   <ul>
 * <li>LoginHandler.LoginTemplate - The mustache template file for the login
 *     page.</li>
 * <li>LoginHandler.SurplusLoginTemplate - A mustache template file to render
 *     when a user attempts to login when they already have an authenticated
 *     session.</li>
 * <li>LoginHandler.SurplusLoginMessage - A plaintext message to send to 
 *     users who attempt to login when they already have an authenticated 
 *     session.</li>
 * <li>LoginHandler.InvalidMessage - A plaintext message to send to users who
 *     attempt to login with invalid credentials.</li>
 *   </ul>
 */
public class LoginHandler<C extends BasicContext>
  extends    MethodSegmentHandler<C>
  implements Configurable
{
  
  //
  // Constants.
  //

  public static final String COMPONENT_CODE           = "hLog";
  public static final String DEFAULT_PROPS_PREFIX     = "LoginHandler.";
  public static final String DEFAULT_TEMPLATE_PATH    = "/auth/";
  public static final String SO_PRIOR_URL             = "PriorURL";
  public static final String SO_TEMP_PRIOR_REQUEST    = "TemporaryPriorRequest";
  public static final String DEFAULT_LOGIN_TEMPLATE   = "login";
  public static final String DEFAULT_SURPLUS_TEMPLATE = "login-surplus";
  public static final String DEFAULT_INVALID_MESSAGE  = "Invalid login. Please try again.";
  public static final String DEFAULT_SURPLUS_LOGIN_MESSAGE = "You are already logged in.";
  public static final String DEFAULT_LOGIN_MESSAGE    = "You are now logged in."; 
  
  //
  // Member variables.
  //

  private final String           propsPrefix;
  private final PyxisSecurity    security;
  
  private String            loginTemplate         = DEFAULT_LOGIN_TEMPLATE;
  private String            invalidMessage        = DEFAULT_INVALID_MESSAGE;
  private String            surplusLoginTemplate  = DEFAULT_SURPLUS_TEMPLATE;
  private String            surplusLoginMessage   = DEFAULT_SURPLUS_LOGIN_MESSAGE;
  private String            loginSuccessMessage   = DEFAULT_LOGIN_MESSAGE;
  //private boolean           capturePosts          = false;
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   * 
   * @param application the Application constructing and using this Handler.
   * @param propsPrefix the property name prefix to use when reading
   *        configuration properties; the default is "LoginHandler.".
   */
  public LoginHandler(GeminiApplication application, String propsPrefix)
  {
    super(application);
    this.propsPrefix = propsPrefix != null ? propsPrefix : DEFAULT_PROPS_PREFIX; 
    
    // Add this as a configurable component.
    app().getConfigurator().addConfigurable(this);

    // Get a reference to the application's security manager.
    this.security = app().getSecurity();
  }
  
  /**
   * Constructor.  Assumes the default properties prefix of "LoginHandler."
   * (trailing period included.)
   * 
   * @param application the Application constructing and using this Handler.
   */
  public LoginHandler(GeminiApplication application)
  {
    this(application, null);
  }
  
  /**
   * Handle a login page request.
   */
  @PathDefault
  @Get
  public boolean getLogin(BasicContext context)
  {
    // If the user is already logged in respond accordingly.
    if (this.security.isLoggedIn(context))
    {
      return handleSurplusLogin();
    }

    template(loginTemplate);
    
    return render();
  }
  
  
  /**
   * Handle a login request.
   */
  @PathDefault
  @Post
  public boolean login(BasicContext context)
  {
    // If the user is already logged in respond accordingly.
    if (security.isLoggedIn(context))
    {
      return handleSurplusLogin();
    }
    
    final Input input = getLoginValidatorSet(context).process(context);
    if (input.passed())
    {
      final Query values = input.values();
      final String username = values.get("lhuser");
      final String password = values.get("lhpass");
      values.put("lhpass", "");
      final boolean saveCookie = values.has("lhremember");

      // Determine if the current IP address can login right now. 
      boolean success = security.isLoginAttemptPermitted(context);
      
      // If the IP address is permitted to attempt a login, let's proceed.
      if (success)
      {
        success = security.login(context, username, password, saveCookie);
      }
      else
      {
        l("Too many attempts from " + context.getClientId() + "; blocked temporarily.");
      }

      if (success)
      {
        // Login successful.
        return handlePostLogin(context);
      }
      else
      {
        // Bad login.
        return handleInvalidLogin();
      }
    }
    
    template(loginTemplate);
    return validationFailure(input);
  }
  
  /**
   * Provide the form validation rules.  Standard validation rules are
   * provided by default, but custom rules can be provided by overloading this
   * method.
   */
  protected ValidatorSet getLoginValidatorSet(BasicContext context)
  {
    return standardLoginForm;
  }
  
  /**
   * Hard-coded default form validation rules. 
   */
  private final ValidatorSet standardLoginForm = new ValidatorSet(
      new Lowercase("lhuser"), 
      new LengthValidator("lhuser", BasicUser.USERNAME_LENGTH, false)
          .message("Please provide a valid username."),
      new LengthValidator("lhpass", BasicUser.PASSWORD_LENGTH, false)
          .message("Please provide a valid password.")
      );
  
  /**
   * Configures the LoginHandler.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    EnhancedProperties.Focus focus = PyxisHandlerHelper.getFocus(props, this.propsPrefix);

    this.loginTemplate        = focus.get("LoginTemplate", DEFAULT_LOGIN_TEMPLATE);
    this.surplusLoginTemplate = focus.get("SurplusLoginTemplate", DEFAULT_SURPLUS_TEMPLATE);
    this.surplusLoginMessage  = focus.get("SurplusLoginMessage", DEFAULT_SURPLUS_LOGIN_MESSAGE);
    this.invalidMessage       = focus.get("InvalidMessage", DEFAULT_INVALID_MESSAGE);
    this.loginSuccessMessage  = focus.get("LoginSuccessMessage", DEFAULT_LOGIN_MESSAGE);
    setBaseTemplatePath(focus.get("TemplateRelativePath", DEFAULT_TEMPLATE_PATH));
  }
  
  /**
   * After a user has logged in, the LoginHandler will direct the user to
   * the post-login page or the stored page that was requested prior to
   * login.
   * 
   * @param context the request context of the user who has just logged-in.
   */
  protected boolean handlePostLogin(BasicContext context)
  {
    final String redirectUrl = security.getPostLoginUrl(context);
    l("Redirecting: " + redirectUrl);

    // If the request appears to be asking for a JSON response, and we're
    // configured to respond with JSON, let's do so.
    if (GeminiHelper.isJsonRequest(context))
    {
      return postLoginJson(context, redirectUrl);
    }
    else
    {
      return postLoginTraditional(context, redirectUrl);
    }
  }
  
  /**
   * Handle a post-login JSON response.
   */
  protected boolean postLoginJson(BasicContext context, String redirectUrl)
  {
    delivery()
        .put("status", "ok")
        .put("redirect", redirectUrl);
        
    return message(loginSuccessMessage);
  }
  
  /**
   * Handle a post-login traditional response.
   */
  protected boolean postLoginTraditional(BasicContext context, String redirectUrl)
  {
    return context.redirect(redirectUrl);
  }
  
  /**
   * Handles an invalid login attempt.
   */
  protected boolean handleInvalidLogin()
  {
    template(loginTemplate);
    delivery().message(invalidMessage);
    return unauthorized("authentication-failed");
  }
  
  /**
   * Handles an attempt to login while already logged in.
   */
  protected boolean handleSurplusLogin()
  {
    template(surplusLoginTemplate);
    delivery().message(surplusLoginMessage);
    return badRequest("already-authenticated");
  }

}   // End LoginHandler.
