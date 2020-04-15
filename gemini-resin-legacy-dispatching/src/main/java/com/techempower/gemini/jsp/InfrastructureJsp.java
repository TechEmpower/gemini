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

package com.techempower.gemini.jsp;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

import com.techempower.gemini.*;
import com.techempower.helper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides all JSP pages within the web site with a common foundation.
 *   <p>
 * This foundation provides the following checks: 
 * <ul>
 * <li> Disallow direct JSP invocation, if selected by configuration.
 * <li> Use Configurator to configure system, if necessary, when a JSP
 *      is invoked directly.
 * </ul>
 *   <p>
 * Applications should subclass the related BasicJsp class to create a
 * foundation JSP tailored to the specific application.  Specifically,
 * it is important for testing purposes to have a JSP invoke the correct
 * Configurator if JSPs are to be invoked directly during testing.
 *   <p>
 * All JSPs in the project should contain as the first line an extends
 * directive.  For instance, on MyApplication, the following line is at the
 * top of all JSPs:
 *   <p>
 * <pre><br>
 *  &lt;%@ page extends="com.example.MyApplicationJSP" %@&gt;
 * </pre>
 *   <p>
 * Once the JSP subclasses the application-specific JSP infrastructure
 * class, the JSP should get a reference to the Context in order to
 * retrieve deliveries.  The Context object is delivered to the JSP as
 * a request attribute.  See the example below:
 *    <p>
 * <pre><br>
 *   &lt;%
 *     Context        context    = (Context)request.getAttribute("Context");
 *     Form           form       = context.getDelivery("Form");
 *     FormValidation validation = context.getDelivery("FormValidation");
 *   %&gt;
 * </pre>
 */
public abstract class InfrastructureJsp
  implements          HttpJspPage
{

  //
  // Private variables.
  //

  private          ServletConfig     config;
  private volatile GeminiApplication application;
  private          Logger            log = LoggerFactory.getLogger(getClass());
  private          ScriptsAndSheets  sas;

  //
  // Public methods.
  //

  /**
   * Sets the application reference.  This is typically called as soon as
   * an external reference to the Application becomes available.
   */
  protected synchronized void setApplication(GeminiApplication application)
  {
    // Only bother with the steps below if the application reference is
    // new to us (which will happen only once at application start-up time).
    if (this.application != application)
    {
      this.sas = new ScriptsAndSheets(application);
      this.application = application;
      initSas();
    }
  }
  
  /**
   * Gets the GeminiApplication reference.
   */
  protected GeminiApplication getApplication()
  {
    return this.application;
  }
  
  /**
   * Gets the page-scope ScriptsAndSheets reference.
   */
  public ScriptsAndSheets getSas()
  {
    return this.sas;
  }
  
  /**
   * Initialize the servlet (in this case, the JSP).
   */
  @Override
  public void init(ServletConfig configuration)
    throws ServletException
  {
    this.config = configuration;
    jspInit();
  }

  /**
   * Initialize the JSP.
   */
  @Override
  public void jspInit()
  {
    // Do nothing.
  }
  
  /**
   * Initialize page-scope JavaScript and CSS style-sheet references in the 
   * "sas" variable.
   */
  protected void initSas()
  {
    // Does nothing in the base class.
  }

  /**
   * Get a reference to the servlet config.
   */
  @Override
  public ServletConfig getServletConfig()
  {
    return this.config;
  }

  /**
   * This is not final so it can be overridden by a more precise
   * method.
   */
  @Override
  public String getServletInfo()
  {
    return "Infrastructure JSP";
  }

  /**
   * Destroy this servlet (in this case, the JSP).
   */
  @Override
  public void destroy()
  {
    jspDestroy();
  }

  /**
   * Unload anything necessary.  To be overloaded if necessary.
   */
  @Override
  public void jspDestroy()
  {
    // Do nothing.
  }

  /**
   * Renders untrusted Strings into HTML.  This should be used for 
   * essentially all String deliveries.  By "untrusted" we mean 
   * Strings that were provided by a user at some prior time.  
   * Typical non-malicious input will appear normally.
   */
  public static String render(String text)
  {
    return NetworkHelper.render(text);
  }
  
  /**
   * Renders the full URL path to the given CSS file.  Note that typically
   * an application will add CSS dependencies to a ScriptsAndSheets object
   * such as "this.sas" (page-scope) or "vars.sas" (request-scope) rather
   * than render references directly using this method.
   */
  public static String renderCssPath(Context context, String filename)
  {
    return render(context.getInfrastructure().getCssDirectory(context) + filename);
  }

  /**
   * Renders the full URL path to the given HTML file.  
   */
  public static String renderHtmlPath(Context context, String filename)
  {
    return render(context.getInfrastructure().getHtmlDirectory(context) + filename);
  }

  /**
   * Renders the full URL path to the given image file.  
   */
  public static String renderImagePath(Context context, String filename)
  {
    return render(context.getInfrastructure().getImageDirectory(context) + filename);
  }

  /**
   * Renders the full URL path to the given JavaScript file.  Note that 
   * typically an application will add JS dependencies to a ScriptsAndSheets
   * object such as "this.sas" (page-scope) or "vars.sas" (request-scope)
   * rather than render references directly using this method.  
   */
  public static String renderJavaScriptPath(Context context, String filename)
  {
    return render(context.getInfrastructure().getJavaScriptDirectory(context) + filename);
  }

  /**
   * Renders any object (such as a String or an array of integers) to 
   * JavaScript using the Application's default JavaScriptWriter.
   */
  public String renderJavaScript(Object object)
  {
    return getApplication().getJavaScriptWriter().write(object);
  }
  
  /**
   * Renders a list of Script tags to reference external JavaScript files
   * as cataloged by the application, this JSP instance, and optionally
   * the request itself.  The order of rendering is application-scope, then
   * page-scope, then request-scope.
   * 
   * @param context the current request context.
   * @param requestScopeSas if non-null, this is a list of scripts and sheets
   *        that was produced for the particulars of this request.
   */
  public String renderScripts(Context context, ScriptsAndSheets requestScopeSas)
  {
    List<String> suppressed = this.sas.getSuppressed(null);
    suppressed = (requestScopeSas != null ? requestScopeSas.getSuppressed(suppressed) : suppressed);
    
    return
      // Application-scope scripts.
      context.getApplication().getInfrastructure().getSas().renderScripts(context, suppressed)
      // Page-scope scripts.
      + this.sas.renderScripts(context, suppressed)
      // Request-scope scripts.
      + (requestScopeSas != null ? requestScopeSas.renderScripts(context, suppressed) : "");
  }
  
  /**
   * A simplified version of the renderScripts method that assumes there
   * are no request-scope scripts.  That is, only application-scope and page-
   * scope scripts will be rendered.
   *   <p>
   * This is the common use-case since request-scope scripts and sheets
   * are quite rare.
   */
  public String renderScripts(Context context)
  {
    return renderScripts(context, null);
  }
  
  /**
   * Renders a list of Link tags to reference external CSS Style-sheet files
   * as cataloged by the application, this JSP instance, and optionally
   * the request itself.  The order of rendering is application-scope, then
   * page-scope, then request-scope.
   * 
   * @param context the current request context.
   * @param requestScopeSas if non-null, this is a list of scripts and sheets
   *        that was produced for the particulars of this request.
   */
  public String renderSheets(Context context, ScriptsAndSheets requestScopeSas)
  {
    List<String> suppressed = this.sas.getSuppressedSheets(null);
    suppressed = (requestScopeSas != null ? requestScopeSas.getSuppressedSheets(suppressed) : suppressed);

    return
      // Application-scope style-sheets.
      context.getApplication().getInfrastructure().getSas().renderSheets(context, suppressed)
      // Page-scope style-sheets.
      + this.sas.renderSheets(context, suppressed)
      // Request-scope style-sheets.
      + (requestScopeSas != null ? requestScopeSas.renderSheets(context, suppressed) : "");
  }
  
  /**
   * A simplified version of the renderSheets method that assumes there
   * are no request-scope sheets.  That is, only application-scope and page-
   * scope sheets will be rendered.
   *   <p>
   * This is the common use-case since request-scope scripts and sheets
   * are quite rare.
   */
  public String renderSheets(Context context)
  {
    return renderSheets(context, null);
  }
  
  /**
   * Renders the favicon's link rel="shortcut icon" tag based on the following
   * order of precedence: request scope, page scope, application scope.  It's
   * a good idea to have at least an application-default shortcut icon 
   * specified in your infrastructure's sas variable. 
   * 
   * @param context the current request context.
   * @param requestScopeSas if non-null, this is a list of scripts and sheets
   *        that was produced for the particulars of this request.
   */
  public String renderFavicon(Context context, ScriptsAndSheets requestScopeSas)
  {
    String fav = "";
    
    // Try a request-scope SAS if provided.
    if (requestScopeSas != null)
    {
      fav = requestScopeSas.renderFavicon(context);
      if (StringHelper.isNonEmpty(fav))
      {
        return fav;
      }
    }
    
    // Try page-scope SAS.
    fav = this.sas.renderFavicon(context);
    if (StringHelper.isNonEmpty(fav))
    {
      return fav;
    }
    
    // Finally try application-scope SAS.  Empty String will be returned
    // if the application-scope SAS does not define a favicon.
    fav = context.getApplication().getInfrastructure().getSas().renderFavicon(context);
    return fav;
  }
  
  /**
   * A simplified version of the renderFavicon method that assumes there
   * is no request-scope favicon.  That is, only application-scope and page-
   * scope favicons will be considered.
   *   <p>
   * This is the common use-case since request-scope favicons are quite rare.
   * 
   * @param context the current request context.
   */
  public String renderFavicon(Context context)
  {
    return renderFavicon(context, null);
  }

  /**
   * Utility debugging method.
   */
  protected void debug(String debugString)
  {
    log.debug("{}{}", logPrefix(), debugString);
  }
  
  /**
   * Gets the name of the current JSP file.
   */
  protected String getCurrentPageName(LegacyContext context)
  {
    if ("none".equals(context.getReferencedRender()))
    {
      return this.getServletConfig().getServletName();
    }
    else
    {
      return context.getReferencedRender();
    }
  }

  /**
   * Abstract method to be provided by the JSP processor in the subclass.
   * Must be defined in subclass.
   */
  @Override
  abstract public void _jspService(HttpServletRequest request,
    HttpServletResponse response)
    throws ServletException, IOException;

  private String logPrefix() {
    final LegacyContext context = (LegacyContext)Context.get();
    final String pageName = context != null
        ? getCurrentPageName(context) : "no Context";
    return "[" + pageName + "] ";
  }

}   // End InfrastructureJSP
