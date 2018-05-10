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
package com.techempower.gemini.path;

import java.util.*;

import com.techempower.cache.*;
import com.techempower.collection.*;
import com.techempower.gemini.*;
import com.techempower.gemini.Request.*;
import com.techempower.gemini.context.*;
import com.techempower.gemini.input.*;
import com.techempower.gemini.mustache.*;
import com.techempower.helper.*;
import com.techempower.js.*;
import com.techempower.log.*;

/**
 * A basic implementation of PathHandler that provides a small suite of 
 * utility and convenience functions.  BasicPathHandler makes the request 
 * Context and PathSegments variables available as ThreadLocals so that they
 * are not necessarily passed around via method calls.
 *   <p>
 * Responses may be rendered as JSON or as HTML using Mustache templates.
 */
public abstract class BasicPathHandler<C extends Context>
    implements PathHandler<C>,
               UriAware
{

  private final GeminiApplication          application;
  private final EntityStore                store;
  private final JavaScriptWriter           javaScriptWriter;
  private final MustacheManager            mustacheManager;
  private final ComponentLog               log;
  private       String                     baseUri;
  private       String                     baseTemplatePath;
  protected String responseContentType = "text/html; charset=UTF-8";

  /**
   * Constructor.
   * 
   * @param app The GeminiApplication reference.
   * @param componentCode a four-letter code for this handler's ComponentLog.
   * @param jsw A JavaScriptWriter to use when serializing objects as JSON; if
   *     null, the application's default JavaScriptWriter will be used.
   */
  public BasicPathHandler(GeminiApplication app, String componentCode, 
      JavaScriptWriter jsw)
  {
    application = app;
    mustacheManager = app.getMustacheManager();
    javaScriptWriter = jsw != null ? jsw : app.getJavaScriptWriter();
    store = app.getStore();
    log = app.getLog(componentCode);
  }

  /**
   * Constructor.  Use the application's default JavaScriptWriter for JSON
   * serialization.
   * 
   * @param app The GeminiApplication reference.
   * @param componentCode a four-letter code for this handler's ComponentLog.
   */
  public BasicPathHandler(GeminiApplication app, String componentCode)
  {
    this(app, componentCode, null);
  }
  
  /**
   * Constructor.  Use the application's default JavaScriptWriter for JSON
   * serialization and use a default component code of "hdlr".
   * 
   * @param app The GeminiApplication reference.
   */
  public BasicPathHandler(GeminiApplication app)
  {
    this(app, "hdlr", null);
  }
  
  @Override
  public boolean prehandle(PathSegments segments, C context)
  {
    // Any request with an Origin header will be handled by the app directly,
    // however there are some headers we need to set up to add support for
    // cross-origin requests.
    if(context.headers().get(Request.HEADER_ORIGIN) != null)
    {
      addCorsHeaders(segments, context);
      
      if(context.getRequestMethod() == HttpMethod.OPTIONS)
      {
        addPreflightCorsHeaders(segments, context);
        // Returning true indicates we did fully handle this request and
        // processing should not continue.
        return true;
      }
    }
    
    // Returning false indicates we did not fully handle this request and
    // processing should continue to the handle method.
    return false;
  }
  
  /**
   * Adds the standard headers required for CORS support in all requests
   * regardless of being preflight.
   * @see <a href="
   * https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Origin">
   * Access-Control-Allow-Origin</a>
   * @see <a href="
   * https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Requests_with_credentials">
   * Access-Control-Allow-Credentials</a>
   */
  private void addCorsHeaders(PathSegments segments, C context)
  {
    // Applications may configure whitelisted origins to which cross-origin
    // requests are allowed.
    if(NetworkHelper.isWebUrl(context.headers().get(Request.HEADER_ORIGIN)) &&
       app().getSecurity().getSettings().getAccessControlAllowedOrigins()
          .contains(context.headers().get(Request.HEADER_ORIGIN).toLowerCase()))
    {
      // If the server specifies an origin host rather than wildcard, then it
      // must also include Origin in the Vary response header.
      context.headers().put(Request.HEADER_VARY, Request.HEADER_ORIGIN);
      context.headers().put(Request.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
          context.headers().get(Request.HEADER_ORIGIN));
      // Applications may configure the ability to allow credentials on CORS
      // requests, but only for domain-specified requests. Wildcards cannot
      // allow credentials.
      if(app().getSecurity().getSettings().accessControlAllowCredentials())
      {
        context.headers().put(
            Request.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
      }
    }
    // Applications may also configure wildcard origins to be whitelisted for
    // cross-origin requests, effectively making the application an open API.
    else if(app().getSecurity().getSettings().getAccessControlAllowedOrigins()
          .contains(Request.HEADER_WILDCARD))
    {
      context.headers().put(Request.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
          Request.HEADER_WILDCARD);
    }
    // Applications may configure whitelisted headers which browsers may
    // access on cross origin requests.
    if(!app().getSecurity().getSettings().getAccessControlExposedHeaders().isEmpty())
    {
      boolean first = true;
      final StringBuilder exposed = new StringBuilder();
      for(final String header : app().getSecurity().getSettings()
          .getAccessControlExposedHeaders())
      {
        if(!first)
        {
          exposed.append(", ");
        }
        exposed.append(header);
        first = false;
      }
      context.headers().put(Request.HEADER_ACCESS_CONTROL_EXPOSED_HEADERS, 
          exposed.toString());
    }
  }
  
  /**
   * Adds the headers required for CORS support for preflight OPTIONS requests.
   * @see <a href="
   * https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Preflighted_requests">
   * Preflighted requests</a>
   */
  private void addPreflightCorsHeaders(PathSegments segments, C context)
  {
    // Applications may configure whitelisted headers which may be sent to
    // the application on cross origin requests.
    if (StringHelper.isNonEmpty(context.headers().get(
        Request.HEADER_ACCESS_CONTROL_REQUEST_HEADERS)))
    {
      final String[] headers = StringHelper.splitAndTrim(
          context.headers().get(
              Request.HEADER_ACCESS_CONTROL_REQUEST_HEADERS), ",");
      boolean first = true;
      final StringBuilder allowed = new StringBuilder();
      for(final String header : headers)
      {
        if(app().getSecurity().getSettings()
            .getAccessControlAllowedHeaders().contains(header.toLowerCase()))
        {
          if(!first)
          {
            allowed.append(", ");
          }
          allowed.append(header);
          first = false;
        }
      }
      
      context.headers().put(Request.HEADER_ACCESS_CONTROL_ALLOW_HEADERS, 
          allowed.toString());
    }
    
    final String methods = getAccessControlAllowMethods(segments, context);
    if(StringHelper.isNonEmpty(methods))
    {
      context.headers().put(Request.HEADER_ACCESS_CONTROL_ALLOW_METHOD, methods);
    }

    if(context.getRequest().getRequestMethod() == HttpMethod.OPTIONS)
    {
      context.headers().put(Request.HEADER_ACCESS_CONTROL_MAX_AGE, 
          app().getSecurity().getSettings().getAccessControlMaxAge() + "");
    }
  }
  
  @Override
  public void posthandle(PathSegments segments, C context)
  {
    // Does nothing in this base class.
  }

  @Override
  public abstract boolean handle(PathSegments segments, C context);

  /**
   * Gets the Header-appropriate string representation of the http method
   * names that this handler supports for the given path segments.
   * <p>
   * For example, if this handler has two handle methods at "/" and
   * one is GET and the other is POST, this method would return the string
   * "GET, POST" for the PathSegments "/".
   * <p>
   * By default, this method returns "GET, POST", but subclasses should 
   * override for more accurate return values.
   */
  protected String getAccessControlAllowMethods(PathSegments segments,
      C context)
  {
    return HttpMethod.GET + ", " + HttpMethod.POST;
  }
  
  @Override
  public void setBaseUri(String uri)
  {
    this.baseUri = uri;
    
    // Set the base template path to equivalent to the base URI if it has
    // not yet been specified.
    if (this.baseTemplatePath == null)
    {
      setBaseTemplatePath(uri);
    }
  }
  
  /**
   * Get the base URI of this Handler, as deployed to a PathDispatcher. The
   * base URI does not end with a slash.
   */
  public String getBaseUri()
  {
    return this.baseUri;
  }
  
  /**
   * Set the base template path.
   */
  protected BasicPathHandler<C> setBaseTemplatePath(String basePath)
  {
    this.baseTemplatePath = basePath.endsWith("/")
        ? basePath
        : basePath + "/";
    return this;
  }
  
  /**
   * Gets the base template path for this Handler, which is by default the
   * same as the base URI.  But subclasses can overload this as needed to
   * customize template directory organization.
   */
  protected String getBaseTemplatePath()
  {
    return baseTemplatePath == null 
        ? "/" 
        : baseTemplatePath;
  }

  /**
   * Gets the current request Context.
   */
  @SuppressWarnings("unchecked")
  protected C context()
  {
    return (C)RequestReferences.get().context;
  }
  
  /**
   * Gets the current PathSegments. 
   */
  protected PathSegments segments()
  {
    return RequestReferences.get().segments;
  }
  
  /**
   * Gets the current named segments (or "arguments").
   */
  protected NamedValues args()
  {
    return segments().getArguments();
  }
  
  /**
   * Gets the request's Query.
   */
  protected Query query()
  {
    return context().query();
  }
  
  /**
   * Gets the session's Messages.
   */
  protected Messages messages()
  {
    return context().messages();
  }
  
  /**
   * Gets the request's Session.
   */
  protected SessionNamedValues session()
  {
    return context().session();
  }
  
  /**
   * Gets the request's Delivery.
   */
  protected Delivery delivery()
  {
    return context().delivery();
  }
  
  /**
   * Gets the request's Cookies.
   */
  protected Cookies cookies()
  {
    return context().cookies();
  }
  
  /**
   * Gets the request and response Headers.
   */
  protected Headers headers()
  {
    return context().headers();
  }
  
  /**
   * Gets the request's file attachments.
   */
  protected Attachments files()
  {
    return context().files();
  }
  
  /**
   * Gets the filename of the template to use for rendering.  If no specific
   * template has been specified, this may return a default template filename
   * or null.
   */
  protected String template()
  {
    return RequestReferences.get().getTemplate();
  }
  
  /**
   * Sets a specific template filename to use for rendering.  This will
   * override any default template that has been specified by the framework.
   */
  protected void template(String name)
  {
    RequestReferences.get().template = name;
  }
  
  /**
   * Sets a default template filename to use for rendering if no specific
   * template is specified by calling the template(name) method above.  This
   * method is typically called by the framework only.  Application code
   * should call template(name) for most use-cases.
   */
  protected void defaultTemplate(String name)
  {
    RequestReferences.get().defaultTemplate = name;
  }
  
  /**
   * Gets the ComponentLog reference.
   */
  protected ComponentLog log()
  {
    return log;
  }
  
  /**
   * Gets the GeminiApplication reference.
   */
  protected GeminiApplication app()
  {
    return application;
  }
  
  /**
   * Gets the EntityStore reference.
   */
  protected EntityStore store()
  {
    return store;
  }
  
  /**
   * Gets the JavaScriptWriter reference.
   */
  protected JavaScriptWriter jsw()
  {
    return javaScriptWriter;
  }
  
  /**
   * Write something to the log.  This is just a convenience alias for
   * log.debug.
   */
  protected void l(String toLog) 
  {
    log.log(toLog);
  }
  
  /**
   * Write something to the log.  This is just a convenience alias for
   * log.debug.
   */
  protected void l(String toLog, int debugLevel) 
  {
    log.log(toLog, debugLevel);
  }
  
  /**
   * Write something to the log.  This is just a convenience alias for
   * log.debug.
   */
  protected void l(String toLog, int debugLevel, Throwable throwable) 
  {
    log.log(toLog, debugLevel, throwable);
  }
  
  /**
   * Send a basic message as a response (Mustache template or JSON depending
   * on the request characteristics and availability of a template).
   */
  protected boolean message(String message)
  {
    final Map<String, Object> response = new HashMap<>(1 
        + delivery().size());
    delivery().copyTo(response);
    response.put(GeminiConstants.GEMINI_MESSAGE, message);

    defaultTemplate("/common/message");
    return render(response);
  }
  
  /**
   * Send an error message as a response (Mustache template or JSON depending
   * on the request characteristics and availability of a template).
   * 
   * @param httpStatusCode An HTTP response code.  See HttpServletResponse.
   * @param errorMessage A message to name as "error" in a response map.
   */
  protected boolean error(int httpStatusCode, String errorMessage)
  {
    final Map<String, Object> response = new HashMap<>(1 
        + delivery().size());
    delivery().copyTo(response);
    response.put(GeminiConstants.GEMINI_ERROR, errorMessage);
    
    context().setStatus(httpStatusCode);
    defaultTemplate("/common/error");
    return render(response);
  }
  
  /**
   * Renders a validation error.
   * 
   * @param result The result of validation, which includes element-specific
   *        and general error messages.
   * @param generalErrorMessage A general error message to return at the root
   *        of the standard Gemini error object graph.
   */
  public boolean validationFailure(Input result, String generalErrorMessage)
  {
    delivery().validation(result);
    return error(result.getStatusCode(), generalErrorMessage);
  }
  
  /**
   * Renders a validation error.
   * 
   * @param result The result of validation, which includes element-specific
   *        and general error messages.
   */
  public boolean validationFailure(Input result)
  {
    return validationFailure(result, "validation-failure");
  }
  
  /**
   * Sends an error message and the HTTP response code METHOD_NOT_ALLOWED,
   * which is suitable when a request uses the GET method but POST is 
   * expected.
   * 
   * @param errorMessage A message to name as "error" in a response map.
   */
  protected boolean badHttpMethod(String errorMessage)
  {
    return error(405, errorMessage);
  }
  
  /**
   * Sends an error message and the HTTP response code BAD_REQUEST.
   * 
   * @param errorMessage A message to name as "error" in a response map.
   */
  protected boolean badRequest(String errorMessage)
  {
    return error(400, errorMessage);
  }
  
  /**
   * Sends an error message and the HTTP response code UNAUTHORIZED.
   * 
   * @param errorMessage A message to name as "error" in a response map.
   */
  protected boolean unauthorized(String errorMessage)
  {
    return error(401, errorMessage);
  }
  
  /**
   * Sends an error message and the HTTP response code NOT_FOUND.
   * 
   * @param errorMessage A message to name as "error" in a response map.
   */
  protected boolean notFound(String errorMessage)
  {
    return error(404, errorMessage);
  }
  
  /**
   * Sends an error message and the HTTP response code SERVICE_UNAVAILABLE.
   * 
   * @param errorMessage A message to name as "error" in a response map.
   */
  protected boolean unavailable(String errorMessage)
  {
    return error(503, errorMessage);
  }
  
  /**
   * Redirect to another URL using a temporary redirect.
   */
  protected boolean redirect(String url)
  {
    return context().redirect(url);
  }
  
  /**
   * Redirect to another URI relative to the current offset URI.  See
   * PathSegments.getUriBelowOffset for more detail.
   * 
   * @param uri A relative URI such as "/foo" or "/bar/baz".  If the leading
   *        slash is omitted, a leading slash will be added automatically
   *        since the URI below the current offset does not end with a slash. 
   */
  protected boolean redirectRelative(String uri)
  {
    boolean initialSlash = ((uri.length() > 0) && (uri.charAt(0) == '/'));
    return context().redirect(segments().getUriBelowOffset()
        + (initialSlash ? "" : "/")
        + uri);
  }
  
  /**
   * Render an object as a response.  If the request accepts JSON or if there
   * is no template specified, the object parameter will be serialized as JSON
   * and provided directly.  If a template is specified and the request is
   * not expecting JSON, the template is used to provide a response using
   * server-side composition.
   *   <p>
   * <b>Important:</b> Be mindful of the JSON serialization of all deliveries
   * that will occur if a request arrives that expects a JSON response.  It
   * is conceivable that the serialization would expose information that is
   * not rendered in the corresponding Mustache template.  Although the
   * corresponding template may not render any given attribute in the 
   * deliveries, a JSON serialization will use the provided JavaScriptWriter.
   * If you want to avoid this automatic serialization to JSON, use the
   * mustache() method instead.
   */
  protected boolean render(Object object)
  {
    final String template = template();
    
    // If json-accepting request or there is no template, send JSON.
    if (  (StringHelper.isEmpty(template))
       || (mustacheManager == null)
       || (GeminiHelper.isJsonRequest(context()))
       )
    {
      return json(object);
    }
    
    return mustache(template, object);
  }
  
  /**
   * Render the Context's deliveries as a response.  Follows the same
   * semantics as render(object) otherwise.
   */
  protected boolean render()
  {
    return render(delivery().asMap());
  }
  
  /**
   * Send a response as JSON, regardless of the request headers.
   */
  protected boolean json(Object object)
  { 
    return GeminiHelper.sendJson(context(), object, javaScriptWriter);
  }
  
  /**
   * Send a response as JSON, regardless of the request headers.
   */
  protected boolean json()
  { 
    return json(delivery().asMap());
  }
  
  /**
   * Send a response as plaintext, regardless of the request headers or a
   * pre-existing template selection.
   */
  protected boolean text(String text)
  {
    return GeminiHelper.sendPlaintext(context(), text);
  }

  /**
   * Send a response as HTML, composed by Mustache.  Uses the template
   * defined by any previous call to template(String), which may be the
   * default template specified by the handler.
   */
  protected boolean mustache()
  {
    return mustache(template());
  }
  
  /**
   * Send a response as HTML, composed by Mustache.  If available, the
   * deliveries captured by the Context will be provided as the template's
   * rendering context.
   * 
   * @param template The filename (less the ".mustache" suffix) of the 
   *     Mustache template to use.  Mustache templates are found in 
   *     WEB-INF/mustache.
   */
  protected boolean mustache(String template)
  {
    return mustache(template, context().delivery().asMap());
  }
  
  /**
   * Send a response as HTML, composed by Mustache.
   * 
   * @param template The filename (less the ".mustache" suffix) of the 
   *     Mustache template to use.  Mustache templates are found in
   *     WEB-INF/mustache.
   * @param object The template's scope objects--that is, the data to make 
   *     available for rendering within the template. 
   */
  protected boolean mustache(String template, Object object)
  {
    // If the template begins at root, ignore the base template path; 
    // otherwise, prepend the base template path.
    final String filename = 
        ( (template.startsWith("/") || baseTemplatePath == null) 
            ? template
            : baseTemplatePath + template)
        + MustacheManager.DEFAULT_MUSTACHE_EXTENSION;
    final Context context = context();

    application.getDispatcher().renderStarting(context, template);
    try
    {
      if(!context.isContentTypeSet())
      {
        context.setContentType(responseContentType);
      }
      return mustacheManager.render(filename, context, object);
    }
    finally
    {
      application.getDispatcher().renderComplete(context);
    }
  }

}
