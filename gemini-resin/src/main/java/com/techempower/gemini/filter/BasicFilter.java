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

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.helper.*;
import com.techempower.log.*;

/**
 * A foundation for simple filters.
 */
public class BasicFilter
  implements Filter
{
  
  //
  // Member variables.
  //

  private ServletContext    servletContext;
  private GeminiApplication app;
  private ComponentLog      log;
  private boolean           debugEnabled;
  private String            filterName;
  private String            errorPage          = "/access-error.jsp";

  //
  // Member methods.
  //

  /**
   * Initializes the Filter aspect of this component.
   */
  @Override
  public void init(FilterConfig config)
  {
    servletContext = config.getServletContext();
    debugEnabled = getInitParameter(config, "DebugEnabled", false);
    errorPage = getInitParameter(config, "ErrorPage", errorPage);
    filterName = config.getFilterName();
  }
  
  /**
   * Finds the GeminiApplication that is using this filter.
   */
  private void findApplication()
  {
    if (app == null)
    {
      app = ApplicationRegistrar.getMain();
      if (app != null)
      {
        log = app.getLog(StringHelper.truncate(getClass().getSimpleName(), 4));
        debug(app.getVersion().getProductName() +  " app and " + filterName 
            + " connected.");
        initAfterAppConnection();
      }
    }
  }
  
  /**
   * Initialization after the application is connected.  This is where a
   * subclass could configure via the application.
   */
  protected void initAfterAppConnection()
  {
    // Does nothing here.
  }
  
  /**
   * Gets the application.
   */
  protected GeminiApplication app()
  {
    return app;
  }
  
  /**
   * Gets the application's security.
   */
  protected PyxisSecurity security()
  {
    return (app != null)
        ? app.getSecurity() 
        : null;
  }
  
  /**
   * Gets the error page.
   */
  protected String getErrorPage()
  {
    return errorPage;
  }
  
  /**
   * @return the servletContext reference.
   */
  public ServletContext getServletContext()
  {
    return servletContext;
  }

  /**
   * Gets a String initialization parameter from the FilterConfig.
   */
  protected String getInitParameter(FilterConfig config, String name, 
                                    String defaultValue)
  {
    if (config.getInitParameter(name) != null)
    {
      return config.getInitParameter(name);
    }
    else
    {
      return defaultValue;
    }
  }
  
  /**
   * Gets a long initialization parameter from the FilterConfig.
   */
  protected long getInitParameter(FilterConfig config, String name, 
                                  long defaultValue)
  {
    if (config.getInitParameter(name) != null)
    {
      return NumberHelper.parseLong(config.getInitParameter(name), 
          defaultValue);
    }

    return defaultValue;
  }
  
  /**
   * Gets a boolean initialization parameter from the FilterConfig.
   * True can be provided as "yes" or "true" and false can be provided
   * as "no" or "false".
   */
  protected boolean getInitParameter(FilterConfig config, String name, 
                                  boolean defaultValue)
  {
    return StringHelper.parseBoolean(config.getInitParameter(name), 
        defaultValue);
  }
  
  /**
   * Destroys the Filter aspect of this component.
   */
  @Override
  public void destroy()
  {
    app = null;
    log = null;
  }
  
  /**
   * Handles a filtering request.  This version can be ignored in most cases.
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, 
                       FilterChain chain)
    throws IOException, 
           ServletException
  {
    doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
  }
  
  /**
   * Handles a filtering request.  This version can be implemented in 
   * subclasses that only deal with HTTP requests.
   */
  public void doFilter(HttpServletRequest request, HttpServletResponse response, 
                       FilterChain chain)
    throws IOException, 
           ServletException
  {
    findApplication();
    filter(request, response, chain);
  }
  
  /**
   * Overload this class to do custom work.
   */
  protected void filter(HttpServletRequest request, HttpServletResponse response, 
                       FilterChain chain)
    throws IOException, 
           ServletException
  {
    chain.doFilter(request, response);
  }
  
  /**
   * Dispatches to an error page (specific to this Filter).
   */
  protected void doError(ServletRequest request, ServletResponse response,
                         int errorType, String errorText)
  {
    try
    {
      request.setAttribute("ErrorType", Integer.valueOf(errorType));
      request.setAttribute("ErrorText", errorText);
      
      request.getRequestDispatcher(errorPage)
          .forward(request, response);
    }
    catch (IOException |ServletException e)
    {
      // Do nothing.
    }
  }

  /**
   * Writes a debug statement to System.out.
   */
  protected void debug(String text)
  {
    debug(text, LogLevel.NORMAL);
  }

  /**
   * Writes a debug statement to System.out.
   */
  protected void debug(String text, Throwable t)
  {
    debug(text, LogLevel.NORMAL, t);
  }

  /**
   * Writes a debug statement to System.out.
   */
  protected void debug(String text, int level)
  {
    if (log != null)
    {
      log.log(text, level);
    }
    else if (debugEnabled)
    {
      System.out.println(filterName + ": " + text);
    }
  }

  /**
   * Writes a debug statement to System.out.
   */
  protected void debug(String text, int level, Throwable t)
  {
    if (log != null)
    {
      log.log(text, level, t);
    }
    else if (debugEnabled)
    {
      System.out.println(filterName + ": " + text + "\n" + t.toString());
    }
  }
  
}  // End SimpliedBasicFilter.

