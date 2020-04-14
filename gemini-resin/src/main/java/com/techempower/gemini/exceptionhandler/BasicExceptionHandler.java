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
package com.techempower.gemini.exceptionhandler;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.gemini.*;
import com.techempower.gemini.feature.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * A basic implementation of ExceptionHandler that shows the exception
 * stack trace or an error message to the site user.
 *   <p>
 * Configurable options:
 * <ul>
 *   <li>BasicExceptionHandler.LogExceptions - Show exceptions in the log
 *       file.  Default: on</li>
 *   <li>BasicExceptionHandler.LogStackTraces - If LogExceptions is enabled,
 *       should the full stack trace also be logged?  Default: off</li>
 *   <li>BasicExceptionHandler.RevealStackTrace - Should the stack trace
 *       be revealed to site users in error displays?</li>
 *   <li>BasicExceptionHandler.UseErrorCode500 - Should a response code of
 *       500 be used?</li>
 *   <li>BasicExceptionHandler.UseJson - Should an error response be sent
 *       as a JSON data structure if the request appears to expect JSON?</li>
 *   <li>BasicExceptionHandler.ErrorTemplate - The filename of a Mustache 
 *       template to use to render error messages, relative to the Mustache
 *       root, but including the file extension.  Deliveries are "exception",
 *       "stackTrace", "description", and "reveal" (boolean) based on the 
 *       configuration above.</li>
 * </ul>
 * If UseJson is enabled, a JSON request is identified by GeminiHelper's
 * isJsonRequest method, which checks for either of the following conditions:
 * <ul>
 *   <li>Request "accepts" header includes "application/json"</li>
 *   <li>Request parameter "format" equals "json", e.g., /foo/?format=json</li>
 * </ul>
 * If the request appears to be expecting JSON, the JSON response will 
 * include:
 * <ul>
 *   <li>Error - The classname of the exception.</li>
 *   <li>ErrorDescription - the "description" String sent to the 
 *       ExceptionHandler if provided.</li>
 *   <li>ErrorMessage - the exception's "message" attribute.</li>
 *   <li>ErrorTrace - the stack trace as a string, if RevealStackTrace is
 *       enabled.</li>
 * </ul>
 */
public class BasicExceptionHandler
  implements ExceptionHandler,
             Feature,
             Configurable
{

  //
  // Constants
  //

  public static final String COMPONENT_CODE = "bvEH";

  //
  // Member variables.
  //

  private final GeminiApplication application;
  private final FeatureManager    fm;
  private final ComponentLog      log;
  
  private boolean           displayExceptionsInLog = true;
  private boolean           displayStackTracesInLog = false;
  private boolean           revealStackTrace = true;
  private boolean           useErrorCode500 = false;
  private boolean           useJson = true;
  private String            errorTemplate = null;

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public BasicExceptionHandler(GeminiApplication app)
  {
    this.application = app;
    this.log = app.getLog(COMPONENT_CODE);
    this.fm = app.getFeatureManager();
    app.getConfigurator().addConfigurable(this);
    
    // Add this Feature.
    this.fm.add("exc-basic", "Basic Exception Handler");
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    EnhancedProperties.Focus focus = props.focus("BasicExceptionHandler.");
    
    displayExceptionsInLog = focus.getBoolean("LogExceptions", true);
    displayStackTracesInLog = focus.getBoolean("LogStackTraces", false);
    revealStackTrace = focus.getBoolean("RevealStackTrace", revealStackTrace);
    useErrorCode500 = focus.getBoolean("UseErrorCode500", useErrorCode500);
    useJson = focus.getBoolean("UseJson", useJson);
    errorTemplate = focus.get("ErrorPage", null);
    if (errorTemplate != null)
    {
      log.log(focus.name("ErrorPage") + " is deprecated; use " 
          + focus.name("ErrorTemplate") + " instead.");
    }
    errorTemplate = focus.get("ErrorTemplate", errorTemplate);
  }

  /**
   * @return the revealStackTrace
   */
  public boolean isRevealStackTrace()
  {
    return revealStackTrace;
  }

  /**
   * @param revealStackTrace the revealStackTrace to set
   */
  public void setRevealStackTrace(boolean revealStackTrace)
  {
    this.revealStackTrace = revealStackTrace;
  }

  @Override
  public void handleException(BasicContext context, Throwable exc)
  {
    handleException(context, exc, null);
  }

  @Override
  public void handleException(BasicContext context, Throwable exception, String description)
  {
    // Is this Feature enabled?
    if (fm.on("exc-basic"))
    {
      // Log the exception to the log file for further evaluation.
      if (exception instanceof ServletException)
      {
        final ServletException servletException = (ServletException)exception;
        logException(context, servletException.getRootCause(), description);
      }
      else
      {
        logException(context, exception, description);
      }

      // Only provide a response by including an error page if the context
      // is not null.
      if (context != null)
      {
        includeErrorPage(context, exception, description);
      }
    }
  }

  /**
   * Write the exception to the log.
   * 
   * @param context the request context.
   * @param exception the exception that was thrown and is being logged.
   * @param description any descriptive text.
   */
  protected void logException(BasicContext context, Throwable exception, String description)
  {
    if (displayExceptionsInLog)
    {
      if (StringHelper.isNonEmpty(description))
      {
        log.log("Exception (" + description + "):\n" + exception);
      }
      else
      {
        log.log("Exception:\n" + exception);
      }
    }

    if (displayStackTracesInLog)
    {
      log.log("Stack trace: " + ThrowableHelper.getStackTrace(exception));
    }
  }

  /**
   * Render the configured error page, or the plain default if no error
   * page has been configured.
   */
  protected void includeErrorPage(BasicContext context, Throwable exception, String description)
  {
    // If the request appears to be asking for a JSON response, and we're
    // configured to respond with JSON, let's do so.
    if (  (useJson)
       && (GeminiHelper.isJsonRequest(context))
       )
    {
      Map<String, String> map = null;
      
      // Add the stack trace if we're configured to do so.  Generally, this
      // is not enabled in production environments.
      if (isRevealStackTrace())
      {
        map = new HashMap<>();
        map.put("errorTrace", ThrowableHelper.convertStackTraceToString(exception));
      }

      GeminiHelper.sendJsonError(
          context,
          exception.getClass().getSimpleName(),
          "exception",
          exception.getMessage(),
          description,
          map);
    }
    // Leave the error page up to the application server (e.g. Resin).
    else if (useErrorCode500)
    {
      context.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    // Use the custom ErrorPage if specified.
    else if (  (StringHelper.isNonEmpty(errorTemplate))
            && (!context.delivery().has("BasicExceptionHandler.handled"))
            )
    {
      context.delivery().put("BasicExceptionHandler.handled", true);
      
      Map<String, Object> map = new HashMap<>(4);
      map.put("exception", exception.toString());
      map.put("stackTrace", ThrowableHelper.getStackTrace(exception));
      map.put("description", (description != null ? description : "No detail available."));
      map.put("reveal", revealStackTrace);

      context.setStatus(500);
      
      try
      {
        application.getMustacheManager().render(errorTemplate, context, 
            map);
      }
      catch (Exception exc)
      {
        // If there is an exception when including the error page, let's show
        // the default.
        outputDefaultErrorPage(context, exception, description);
      }
    }
    // Print a default error page.
    else
    {
      outputDefaultErrorPage(context, exception, description);
    }
  }

  /**
   * Render a plain default error page.
   */
  protected void outputDefaultErrorPage(BasicContext context, Throwable exception,
                                        String description)
  {
    if (isRevealStackTrace())
    {
      // An internal error handler page that displays a developer-friendly
      // (but end-user unfriendly) error message.
      context.print("<html>");
      context.print("<head><title>Internal error</title>");
      context.print("<style>");
      context.print("body { background-color: white; color: black; }");
      context.print("p { font-family: Arial, Helvetica, Sans-serif; font-size: 12px; }");
      context.print("h2 { font-family: Arial, Helvetica, Sans-serif; font-size: 14px; font-weight: bold; }");
      context.print("pre { font-size: 9px; }");
      context.print("</style>");
      context.print("</head>");
      context.print("<body>");
      context.print("<!-- BasicExceptionHandler -->");
      context.print("<h2>Internal error</h2>");
      context.print("<p>An exception was caught by the application infrastructure:</p>");
      if (StringHelper.isNonEmpty(description))
      {
        context.print("<p>" + description + "</p>");
      }
      context.print("<p><pre>");
      context.print(ThrowableHelper.convertStackTraceToString(exception));
      context.print("");
      context.print("</pre></p>");
      if (exception instanceof ServletException)
      {
        ServletException servletException = (ServletException)exception;
        if (servletException.getRootCause() != null)
        {
          context.print("<p>Root cause:</p>");
          context.print("<p><pre>");
          context.print(ThrowableHelper.convertStackTraceToString(servletException.getRootCause()));
          context.print("");
          context.print("</pre></p>");
        }
        else
        {
          context.print("<p>No root cause provided.</p>");
        }
      }
      context.print("</body>");
      context.print("</html>");
    }
    else
    {
      context.print("<html><head>");
      context.print("<title>Server Error</title>");
      context.print("<style>");
      context.print("body { background-color: white; color: black; }");
      context.print("p, div { color: white; font-family: Tahoma, Verdana, Arial, Helvetica, Sans-serif; font-size: 14px; }");
      context.print(".container { border: 8px solid #777777; width: 350px; }");
      context.print(".banner { background: #D06060; color: white; font-weight: bold; padding: 4px }");
      context.print(".text { background: #777777; padding: 4px; }");
      context.print("</style>");
      context.print("</head><body>");
      context.print("<!-- BasicExceptionHandler -->");
      context.print("<div class=\"container\">");
      context.print("<div class=\"banner\">Please bear with us...</div>");
      context.print("<div class=\"text\">We're sorry, our web site is not able to process your request correctly at this time.  Please try again at a later time.  If this situation persists, please get in touch with our customer service or technical support staff.</div>");
      context.print("</div>");
      context.print("<!--");
      if (StringHelper.isNonEmpty(description))
      {
        context.print(UtilityConstants.CRLF + description + UtilityConstants.CRLF);
      }
      context.print(ThrowableHelper.convertStackTraceToString(exception));
      if (exception instanceof ServletException)
      {
        ServletException servletException = (ServletException)exception;
        context.print("Root cause:");
        context.print(ThrowableHelper.convertStackTraceToString(servletException.getRootCause()));
      }
      context.print("-->");
      context.print("</body></html>");
    }
  }

}  // End BasicExceptionHandler
