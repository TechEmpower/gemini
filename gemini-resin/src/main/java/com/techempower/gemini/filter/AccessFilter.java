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

package com.techempower.gemini.filter;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.authorization.*;
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * AccessFilter is a Servlet Filter that prevents unauthorized access to 
 * content.  Typically used to secure static content served by the container's
 * default Servlet, AccessFilter could also protect access to any URIs to 
 * which the filter is assigned.
 *   <p>
 * The implementation check whether a user session is active, and then
 * whether that user is a member of the correct user group, before permitting 
 * the rest of the filter chain and, ultimately, the URI's associated Servlet
 * to process the request.  If there is no user session active or the user is
 * not authorized, the request will be redirected to the login form.  The 
 * rejection behavior is configurable by overloaded the getRejector method.
 *   <p>
 * The Servlet Container's configuration (or a web.xml file) is needed to
 * configure the URI patterns for which the filter is applied.
 *   <p>
 * Beyond specifying the URI patterns to match, the following filter 
 * configuration attributes are available:
 * <ul>
 *  <li>PropsPrefix - A properties prefix to use for reading configuration
 *      from the application's configuration file.</li>
 *  <li>UserGroup - The name of the user group to which the user must be
 *      a member in order to view content.  This defaults to "Users".</li>
 * </ul>
 * Custom behavior can be implemented by overloading the class to provide
 * a custom Authorizer and Rejector (which is called if the user is not 
 * authorized). 
 */
public class AccessFilter
  extends    BasicFilter
  implements Configurable
{

  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "hAcF";
  public static final String PROPS_PREFIX   = "AccessFilter.";
  
  public static final int    ERROR_NO_ACCESS     = 1;
  public static final int    ERROR_UNKNOWN       = 2;
  
  //
  // Member variables.
  //

  private String            propsPrefix        = PROPS_PREFIX;
  private String            userGroupName      = "Users";
  private PyxisUserGroup    userGroup;

  //
  // Member methods.
  //

  /**
   * Initializes the Filter aspect of this component.
   */
  @Override
  public void init(FilterConfig config)
  {
    super.init(config);
    
    propsPrefix = getInitParameter(config, "PropsPrefix", propsPrefix);
    userGroupName = getInitParameter(config, "UserGroup", userGroupName);
  }

  @Override
  protected void initAfterAppConnection() 
  {
    final Configurator conf = app().getConfigurator();
    conf.addConfigurable(this);
    configure(conf.getLastProperties());
  }

  /**
   * Handles a filtering request.
   */
  @Override
  protected void filter(HttpServletRequest servletRequest, 
      HttpServletResponse servletResponse, 
      FilterChain chain)
          throws IOException, 
          ServletException
  {
    if (security() != null)
    {
      final ResinHttpRequest request = new ResinHttpRequest(servletRequest, servletResponse,
          getServletContext(), app());
      final BasicContext context = app().getContext(request);
      
      boolean authorized = security().authCheck(context, 
          getAuthorizer(), getRejector());
      
      if (authorized)
      {
        //debug("User permitted.  Chaining.");
        chain.doFilter(servletRequest, servletResponse);
      }
    }
    else
    {
      doError(servletRequest, servletResponse, ERROR_UNKNOWN, "");
    }
  }
  
  /**
   * Gets the Authorizer for authorizing users.
   */
  protected Authorizer getAuthorizer()
  {
    return defaultAuthorizer;
  }
  private final Authorizer defaultAuthorizer = new Authorizer() {
    @Override
    public boolean isAuthorized(PyxisUser user, BasicContext context)
    {
      return (  (user != null)
             && (  (StringHelper.isEmpty(userGroupName))
                || (user.isMember(userGroup))
                )
             );
    }
  };
  
  /**
   * Gets the Rejector for rejecting unauthorized users.
   */
  protected Rejector getRejector()
  {
    return security().getForceLoginRejector();
  }
  
  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // Note that the user group name and login handler name can also
    // be provided in the Servlet Container's (Application Server's)
    // Filter configuration.  If these attributes are set in both
    // places, the values found here in the application's .conf file
    // will take precedence.
    
    final EnhancedProperties.Focus focus = props.focus(propsPrefix);
    userGroupName = focus.get("UserGroup", userGroupName);

    //debug("User group name: " + userGroupName);
    //debug("Login handler name: " + loginHandlerName);
    
    if (StringHelper.isNonEmpty(userGroupName))
    {
      try
      {
        // Find the user group from the Application's security.
        final Collection<PyxisUserGroup> userGroups = security().getAllUserGroups();
        final Iterator<PyxisUserGroup> iter = userGroups.iterator();
  
        // Find the group.
        boolean found = false;
        while (iter.hasNext())
        {
          final PyxisUserGroup group = iter.next();
          if (group.getName().equalsIgnoreCase(userGroupName))
          {
            found = true;
            userGroup = group;
          }
        }
        
        if (!found)
        {  
          debug("User group " + userGroupName + " not found!");
        }
      }
      catch (Exception exc)
      {
        debug("Exception while finding user group " + userGroupName + ".", exc);
      }
    }
    
    // Show how we're configured.
    if (userGroup != null)
    {  
      debug("Configured [g: " + userGroup.getName() + "]");
    }
    else
    {
      debug("Configured to not require group membership.");
    }
  }
 
}  // End AccessFilter.

