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
import com.techempower.gemini.path.*;
import com.techempower.gemini.path.annotation.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.util.*;

/**
 * Allows users to logout (sign-out) from the web application, thereby ending
 * their authenticated session.
 */
public class LogoutHandler<C extends BasicContext>
  extends    MethodSegmentHandler<C>
  implements Configurable
{
  
  //
  // Constants.
  //

  public static final String DEFAULT_PROPS_PREFIX    = "LogoutHandler.";
  public static final String DEFAULT_TEMPLATE_PATH   = "/auth/";
  
  public static final String DEFAULT_LOGOUT_TEMPLATE = "logout";
  public static final String DEFAULT_NOUSER_TEMPLATE = "logout-nouser";
  
  public static final String DEFAULT_LOGOUT_MESSAGE = "Successfully logged out.";
  public static final String DEFAULT_NOUSER_MESSAGE = "No user is logged in.";
   
  //
  // Member variables.
  //

  private final PyxisSecurity     security;
  private final String            propsPrefix;
  private       String            logoutTemplate = DEFAULT_LOGOUT_TEMPLATE;
  private       String            logoutMessage  = DEFAULT_LOGOUT_MESSAGE;
  private       String            noUserTemplate = DEFAULT_NOUSER_TEMPLATE;
  private       String            noUserMessage  = DEFAULT_NOUSER_MESSAGE;
  private       boolean           httpGetPermitted = true;

  //
  // Member methods.
  //
  
  /**
   * Constructor.
   * 
   * @param app the Application constructing and using this Handler.
   * @param propsPrefix the property name prefix to use when reading
   *        configuration properties; the default is "LoginHandler.".
   */
  public LogoutHandler(GeminiApplication app, String propsPrefix)
  {
    // Copy parameters.
    super(app);
    this.propsPrefix = propsPrefix != null ? propsPrefix : DEFAULT_PROPS_PREFIX; 

    // Add this as a configurable component.
    app().getConfigurator().addConfigurable(this);

    // Get a reference to the application's security manager.
    this.security = app().getSecurity();
  }

  /**
   * Constructor.  Assumes the default properties prefix of "LogoutHandler."
   * (trailing period included.)
   * 
   * @param application the Application constructing and using this Handler.
   */
  public LogoutHandler(GeminiApplication application)
  {
    this(application, null);
  }
  
  @Override
  public void configure(EnhancedProperties props)
  {
    EnhancedProperties.Focus focus = PyxisHandlerHelper.getFocus(props, this.propsPrefix);

    logoutTemplate = focus.get("LogoutTemplate", DEFAULT_LOGOUT_TEMPLATE);
    logoutMessage = focus.get("LogoutMessage", DEFAULT_LOGOUT_MESSAGE);
    noUserTemplate = focus.get("NoUserTemplate", DEFAULT_NOUSER_TEMPLATE);
    noUserMessage = focus.get("NoUserMessage", DEFAULT_NOUSER_MESSAGE);
    httpGetPermitted = focus.getBoolean("AllowLogoutViaGet", true);
    setBaseTemplatePath(focus.get("TemplateRelativePath", DEFAULT_TEMPLATE_PATH));
  }
  
  /**
   * Gets the Security reference.
   */
  protected PyxisSecurity getSecurity()
  {
    return this.security;
  }
  
  /**
   * Handle a logout request.
   */
  @PathDefault
  @Get
  public boolean getLogout(BasicContext context)
  {
    if (httpGetPermitted)
    {
      return logout(context);
    }
    return badHttpMethod("Logout must be POSTed.");
  }
  
  /**
   * Handle a logout request.
   */
  @PathDefault
  @Post
  public boolean postLogout(BasicContext context)
  {
    return logout(context);
  }
  
  private boolean logout(BasicContext context)
  {
    // Check to see if anyone is logged in...
    if (getSecurity().isLoggedIn(context))
    {
      getSecurity().logout(context);

      return handlePostLogout();
    }
    else
    {
      // If no one is logged in, do not respond to the logout command.
      return handleLogoutNoUser();
    }
  }
  
  /**
   * Handles a logout request where no user is presently logged in.  The
   * Default behavior of LoginHandler is to render a special template to
   * inform the site visitor that they have attempted to logout but they
   * had no active session.
   */
  protected boolean handleLogoutNoUser()
  {
    context().delivery().put(GeminiConstants.GEMINI_MESSAGE, noUserMessage);
    template(noUserTemplate);
    return render();
  }
  
  /**
   * After a user has been logged out the default behavior is rendering a
   * simple "You have been logged out" view.
   */
  protected boolean handlePostLogout()
  {
    context().delivery().put(GeminiConstants.GEMINI_MESSAGE, logoutMessage);
    template(logoutTemplate);
    return render();
  }

}
