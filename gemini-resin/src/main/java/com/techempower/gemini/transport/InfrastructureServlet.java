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
package com.techempower.gemini.transport;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.*;
import com.techempower.gemini.*;
import com.techempower.log.*;

/**
 * Gemini's foundation servlet.  As part of building a Gemini web
 * application, this base servlet should be extended to get some basic 
 * functionality.  Provides default functionality for init(), destroy(),
 * doGet(), doPost(), getServletName(), and getServletInto().
 *    <p>
 * Typical Gemini applications will leave the handleRequest() method
 * as-is to use a Dispatcher.  However, you may overload handleRequest() 
 * for processing requests by alternative means.
 *    <p>
 * The InfrastructureServlet combined with a Dispatcher subclass and
 * one or more Handlers creates the "Controller" portion of a "Model View
 * Controller" (MVC) web-based application.
 *    <p>
 * A typical Gemini application has the following MVC requirements:
 *
 *    <ul>
 * <li> Business logic, including validation and processing of forms
 *      is done in the back-end layer, principally by Handlers.
 * <li> There is one instance of the Servlet class.  This Servlet combined
 *      with the Dispatcher and Handlers, forms the "Controller".
 * <li> The Dispatcher is responsible for handing the request off to Handlers
 *      to process or "handle" the request.
 * <li> The Handler classes create and manipulate objects that are stored
 *      within Application, Request, or Session scope.  These objects
 *      are the "Model".
 * <li> The handler invokes JSP pages to produce the "View".
 *    </ul>
 *
 * The InfrastructureServlet, being the first point of execution for the
 * web application, is the entity responsible for constructing the Gemini 
 * Application and for invoking the Configurator when the application instance
 * is ready.
 *
 * @see com.techempower.gemini.BasicDispatcher
 * @see com.techempower.gemini.Handler
 */
public abstract class InfrastructureServlet
  extends    HttpServlet
{
  private static final long   serialVersionUID   = 8642344935L;
  
  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "svlt";    // Four-letter component ID

  //
  // Member variables.
  //

  private final GeminiApplication application = getApplication();
  private final ComponentLog      log         = this.application.getLog(COMPONENT_CODE);
  private final Version           version     = this.application.getVersion();
  
  //
  // Public member methods.
  //

  /**
   * Handles the init call.  Note that as of Gemini v1.23, it is no longer
   * recommended to use this init() method as a place to start asynchronous
   * resources.  See GeminiApplication.addAsynchronous() for the new
   * recommended approach.
   *   <p>
   * Gemini Applications start up with the following steps:
   *   1. Initialize this Servlet.
   *   2. Ask each InitializationTask to initialize, which includes running
   *      the Configurator.
   *   3. Evaluate the configuration state (pause here until good).
   *   4. Post-initialize, which sets up the data cache.
   *   5. Begin accepting requests and start asynchronous resources.
   */
  @Override
  public void init()
  {
    InitConfig config;
    try
    {
      config = HttpInitConfig.createHttpInitConfig(getServletConfig(),
          getServletContext());
    }
    catch (IllegalStateException e)
    {
      log.log("Defaulting config to null for mocking servlet. Exception: " + e);
      config = HttpInitConfig.createHttpInitConfig(null, null);
    }
    
    // Initialize the application.
    this.application.initialize(config);
  }

  /**
   * Gets a GeminiApplication object for this application.  Subclass 
   * the GeminiApplication class to build a custom class for your 
   * application.
   */
  public abstract GeminiApplication getApplication();

  /**
   * Handles the destroy call.  Note that as of Gemini v1.23, it is no longer
   * recommended to use this destroy() method to stop asynchronous resources.
   * See GeminiApplication.addAsynchronous() for the new recommended approach.
   */
  @Override
  public void destroy()
  {
    super.destroy();

    // Stop the application and all dependencies.
    this.application.end();
  }
  
  /**
   * Services all incoming requests to the application server. By default,
   * this method calls doRequest below. This allows an application to process 
   * all request methods in essentially the same way. If this behavior is 
   * not desired, overload.
   * <p>
   * Note: This does not route requests to the servlet doXXX methods based on 
   * the method of the request.
   */
  @Override
  public void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    final ResinHttpRequest httpRequest = new ResinHttpRequest(request, response,
        this.getServletContext(), this.application);
    application.doRequest(httpRequest);
  }

  /**
   * Does nothing.
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected final void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    // Intentionally left empty.
  }

  /**
   * Does nothing.
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected final void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    // Intentionally left empty.
  }

  /**
   * Does nothing.
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected final void doPut(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    // Intentionally left empty.
  }

  /**
   * Does nothing.
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    // Intentionally left empty.
  }

  /**
   * Does nothing.
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected final void doOptions(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    // Intentionally left empty.
  }

  /**
   * Does nothing.
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected final void doHead(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException
  {
    // Intentionally left empty.
  }

  /**
   * Does nothing.
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected final void doTrace(HttpServletRequest arg0, HttpServletResponse arg1)
      throws ServletException, IOException
  {
    // Intentionally left empty.
  }


  /**
   * Returns this servlet's name.  Overload this method to return a more
   * descriptive name.  By default, returns the product name and version.
   */
  @Override
  public String getServletName()
  {
    return this.version.getAbbreviatedProductName() + " Servlet version " + this.version.getVersionString();
  }

  /**
   * Returns information about this servlet.  Overload this method to 
   * return more a descriptive string.  By default, returns product name,
   * version, and copyright info.
   */
  @Override
  public String getServletInfo()
  {
    return getServletName() 
      + " - Copyright (c) " + this.version.getCopyrightYears() 
      + " " + this.version.getClientName()
      + " - developed by " + this.version.getDeveloperName() + ".";
  }

  //
  // Protected methods.
  //
  
  /**
   * Displays the Servlet init parameters.
   */
  protected void displayInitParameters()
  {
    Enumeration<String> enumeration = getServletConfig().getInitParameterNames();
    
    if (enumeration.hasMoreElements())
    {
      this.log.log("Servlet initialization parameters:");
      while (enumeration.hasMoreElements())
      {
        String name = enumeration.nextElement();
        this.log.log(name + ": " + getServletConfig().getInitParameter(name));      
      }
    }
    else
    {
      this.log.log("No Servlet initialization parameters set.");
    }

    ServletContext servletContext = getServletConfig().getServletContext();
    enumeration = servletContext.getInitParameterNames();
    
    if (enumeration.hasMoreElements())
    {
      this.log.log("ServletContext initialization parameters:");
      while (enumeration.hasMoreElements())
      {
        String name = enumeration.nextElement();
        this.log.log(name + ": " + servletContext.getInitParameter(name));      
      }
    }
    else
    {
      this.log.log("No ServletContext initialization parameters set.");
    }
  }

}   // End InfrastructureServlet.
