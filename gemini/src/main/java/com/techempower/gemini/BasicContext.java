/*******************************************************************************
 * Copyright (c) 2020, TechEmpower, Inc.
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
package com.techempower.gemini;

import java.io.*;
import java.util.*;

import com.google.common.io.*;
import com.techempower.gemini.Request.*;
import com.techempower.gemini.context.*;
import com.techempower.gemini.internationalization.*;
import com.techempower.gemini.session.*;
import com.techempower.log.*;
import com.techempower.security.*;

public abstract class BasicContext implements Context
{
  public static final String        COMPONENT_CODE = "ctxt";
  public static final String        SO_CONSUMABLE_REQUEST = 
      "_consumable_prior_request";
  protected static final ThreadLocal<Context>
                                    CONTEXTS_BY_THREAD = new ThreadLocal<>();
  private static final String       SO_TOKEN_PROVIDER = "_token_provider";
  private static final BaseEncoding TOKEN_ENCODING = 
      BaseEncoding.base32().omitPadding();
  
  protected final GeminiApplication   application;
  protected final ComponentLog        log;
  protected final Dispatcher          dispatcher;
  protected final BasicInfrastructure infrastructure;
  protected final long                processingStart;
  protected final Request             request;
  protected final Request             priorRequest;
  protected final SessionNamedValues  sessionNamedValues;
  protected final Messages            messages;
  protected Query                     query;
  protected Attachments               files;
  private long                        requestNumber = 0L;
  private GeminiResources             resources;
  private Session                     session;
  private Delivery                    delivery;
  private Cookies                     cookies;
  private Headers                     headers;
  private boolean                     contentTypeSet;

  public BasicContext(GeminiApplication application, Request request)
  {
    this.processingStart = System.currentTimeMillis();
    this.application     = application;
    this.infrastructure  = application.getInfrastructure();
    this.request         = request;
    this.dispatcher      = application.getDispatcher();

    // If there is a session already, get it. If the request does not yet have
    // a session, don't create one until a value is stored.
    getSession(false);

    this.sessionNamedValues = new SessionNamedValues(this);
    this.messages        = new Messages(this);
    this.log             = application.getLog(COMPONENT_CODE);

    // Register this Context to the current thread.
    CONTEXTS_BY_THREAD.set(this);

    // Sets this Context object as an attribute of the request.
    this.request.setAttribute("Context", this);

    // Bind a consumable prior request if available in the session.
    if (session().has(SO_CONSUMABLE_REQUEST))
    {
      this.priorRequest = session().getObject(SO_CONSUMABLE_REQUEST);
      this.query = new Query(priorRequest);
      session().remove(SO_CONSUMABLE_REQUEST);
    }
    else
    {
      this.priorRequest = null;
      this.query = new Query(request);
    }

    // Set the request and response character sets.
    setDefaultCharacterSets();
  }

  /**
   * Indicates that the current thread's Context will no longer be used.
   * Dissociate it with the current thread.
   *   <p>
   * See the static get() method for some important notes about this method.
   */
  public static void complete()
  {
    CONTEXTS_BY_THREAD.set(null);
  }

  /**
   * Gets the current thread's Context object.
   *   <p>
   * If your application uses Gemini's legacy support for JSP as a view
   * (rather than Mustache), note that this method and Context.complete()
   * should only be used within the Java back-end of your application (that
   * is, Handlers, Managers, and the like).  This should not be called within
   * JSP pages.  JSP pages are executed as Servlets in their own right, and
   * are therefore when the request is forwarded to a JSP, it is subject to
   * assignment to a different thread by the Servlet container.  If that
   * occurs, Context.get() will behave unpredictably.
   *   <p>
   * If you are using JSP, you should consider the call to context.includeJSP
   * as potentially the last point of execution for the current thread.
   *   <p>
   * All ThreadLocal variables used elsewhere within your application will
   * be subject to the same constraint.
   */
  public static Context get()
  {
    return CONTEXTS_BY_THREAD.get();
  }

  /**
   * Returns a reference to the application.
   */
  public GeminiApplication getApplication()
  {
    return this.application;
  }

  /**
   * Returns a reference to the ComponentLog.
   */
  protected ComponentLog getLog()
  {
    return this.log;
  }

  /**
   * Gets the client's identifier, for web requests, this will be the client's
   * IP address.
   */
  public String getClientId()
  {
    return this.request.getClientId();
  }

  /**
   * Returns the current URI of the request.
   */
  public String getCurrentUri()
  {
    return this.request.getCurrentURI();
  }

  /**
   * Gets the dispatcher that dispatched this context.
   */
  public Dispatcher getDispatcher()
  {
    return this.dispatcher;
  }

  /**
   * Gets the time since the start of this Context, in milliseconds.
   */
  public long getDuration()
  {
    return (System.currentTimeMillis() - this.processingStart);
  }

  /**
   * Get a reference to the application's Infrastructure.
   */
  public BasicInfrastructure getInfrastructure()
  {
    return this.infrastructure;
  }

  /**
   * Gets a user session's locale.  If no locale it set, the default locale
   * is returned.  This is merely a pass-through convenience method for
   * calling localeManager.getLocale.
   *   <p>
   * In a locale-aware application, it is more typical to just call
   * getResources to get a reference to the GeminiResources to use for this
   * user's current request.
   */
  public Locale getLocale()
  {
    return this.application.getLocaleManager().getLocale(this);
  }

  /**
   * Gets a reference to the output stream from the response.  This is a
   * pass-through to response.getOutputStream().
   */
  public OutputStream getOutputStream()
      throws IOException
  {
    return this.request.getOutputStream();
  }

  /**
   * Gets the query string of the request.
   */
  public String getQueryString()
  {
    return this.request.getQueryString();
  }

  /**
   * Pass-though to request.getRealPath.
   */
  public String getRealPath(String path)
  {
    return this.request.getRealPath(path);
  }

  /**
   * Get the associated Request.
   */
  public Request getRequest()
  {
    return this.request;
  }

  /**
   * If request counting is enabled, return this request's number.
   */
  public long getRequestNumber()
  {
    return this.requestNumber;
  }

  /**
   * Gets the current request's "signature;" that is, the URL including all
   * of the parameters.  This will construct a URL that will always use a
   * "GET" form even if the request itself was POSTed.
   */
  public String getRequestSignature()
  {
    return this.request.getRequestSignature();
  }

  /**
   * Returns the request URI.  A URI is the portion of a URI after the
   * protocol and domain.  The URL http://techempower.com/admin/users has a
   * URI of admin/users.
   */
  public String getRequestUri()
  {
    return this.request.getRequestURI();
  }

  /**
   * Gets or initializes and then gets a reference to the appropriate
   * GeminiResourceBundle for this user given the Locale--or lack thereof--
   * stored in the user's session.  If your application is non-locale-
   * aware, you can optionally always return a static reference to a
   * DefaultGeminiResources object, to avoid the session lookup.
   */
  public GeminiResources getResources()
  {
    if (this.resources == null)
    {
      this.resources = this.application.getLocaleManager().getResources(this);
    }

    return this.resources;
  }

  /**
   * Gets the full Secure URL to the Servlet.
   */
  public String getSecureUrl()
  {
    return this.request.encodeURL(this.infrastructure.getSecureUrl());
  }

  /**
   * Gets the name (also sometimes called the "URL") of the servlet.  Again,
   * this Context version of the method will encode session information into
   * the URL if the browser does not support cookies.
   */
  public String getUrl()
  {
    return this.request.encodeURL(this.infrastructure.getName());
  }

  /**
   * Get a named-value interface to the Session.
   */
  public SessionNamedValues session()
  {
    return sessionNamedValues;
  }

  public abstract Attachments files();

  /**
   * Get a named-value interface to the Delivery map.
   */
  public Delivery delivery()
  {
    if (delivery == null)
    {
      delivery = new Delivery();
    }
    return delivery;
  }

  /**
   * Gets an interface for working with Cookies.
   */
  public Cookies cookies()
  {
    if (cookies == null)
    {
      cookies = new Cookies(this);
    }
    return cookies;
  }

  /**
   * Gets an interface for working with request and response Headers.
   */
  public Headers headers()
  {
    if (headers == null)
    {
      headers = new Headers(this);
    }
    return headers;
  }

  /**
   * Gets an interface for working with session Messages.
   */
  public Messages messages()
  {
    return messages;
  }

  /**
   * Gets an interface for working with the request's query parameters.
   */
  public Query query()
  {
    return query;
  }

  /**
   * Get the associated user session.
   *
   * @param create Whether to force the creation of a session. In general,
   * pass false. When you go to store a session value, it will create a
   * session at that point if necessary.
   */
  public Session getSession(boolean create)
  {
    if (this.session == null)
    {
      // Creates the session object from the Request object
      this.session = this.request.getSession(create);
    }
    return this.session;
  }

  /**
   * Gets the full standard (non-secure) URL to the Servlet.
   */
  public String getStandardUrl()
  {
    return this.request.encodeURL(this.infrastructure.getStandardUrl());
  }

  /**
   * Gets the start time of this Context--or, more strictly defined--the
   * time at which this Context was constructed, in System time milliseconds.
   */
  public long getStartTime()
  {
    return this.processingStart;
  }

  /**
   * Returns a new token.  This may invalidate an old token.
   */
  public String getToken()
  {
    return TOKEN_ENCODING.encode(getTokenProvider().next());
  }

  /**
   * Returns the session's token provider.
   */
  protected TokenProvider getTokenProvider()
  {
    TokenProvider provider = session().getObject(SO_TOKEN_PROVIDER);
    if (provider == null)
    {
      provider = new TokenProvider(100, 16);
      session().putObject(SO_TOKEN_PROVIDER, provider);
    }
    return provider;
  }

  public PrintWriter getWriter() throws IOException
  {
    return this.request.getWriter();
  }

  /**
   * Returns whether or not the response has been committed. This means that
   * you can no longer write to the outputstream.
   */
  public boolean isCommitted()
  {
    return this.request.isCommitted();
  }

  /**
   * Is the session new?  This method returns true if the Session was marked
   * as new by the Servlet API.
   */
  public boolean isNewSession()
  {
    Session currentSession = getSession(false);
    return (currentSession == null)
        ? true
        : currentSession.isNew();
  }

  /**
   * Is the request a HEAD request.
   */
  public boolean isHead()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isHead();
    }
    else
    {
      return this.request.isHead();
    }
  }

  /**
   * Is the request a GET request.
   */
  public boolean isGet()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isGet();
    }
    else
    {
      return this.request.isGet();
    }
  }

  /**
   * Is the request a POST request.
   */
  public boolean isPost()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isPost();
    }
    else
    {
      return this.request.isPost();
    }
  }

  /**
   * Is the request a PUT request.
   */
  public boolean isPut()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isPut();
    }
    else
    {
      return this.request.isPut();
    }
  }

  /**
   * Is the request a DELETE request.
   */
  public boolean isDelete()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isDelete();
    }
    else
    {
      return this.request.isDelete();
    }
  }

  /**
   * Is the request a TRACE request.
   */
  public boolean isTrace()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isTrace();
    }
    else
    {
      return this.request.isTrace();
    }
  }

  /**
   * Is the request an OPTIONS request.
   */
  public boolean isOptions()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isOptions();
    }
    else
    {
      return this.request.isOptions();
    }
  }

  /**
   * Is the request a CONNECT request.
   */
  public boolean isConnect()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isConnect();
    }
    else
    {
      return this.request.isConnect();
    }
  }

  /**
   * Is the request a PATCH request.
   */
  public boolean isPatch()
  {
    if (isPriorRequestBound())
    {
      return this.priorRequest.isPatch();
    }
    else
    {
      return this.request.isPatch();
    }
  }

  /**
   * Is a PriorRequest bound?  Determine if a PriorRequest has been
   * bound to this Context by a previous call to bindPriorRequest.
   */
  public boolean isPriorRequestBound()
  {
    return (this.priorRequest != null);
  }

  /**
   * Return whether the current request is secure.
   */
  public boolean isSecure()
  {
    return this.request.isSecure();
  }

  /**
   * Output text directly to the response via a PrintWriter.  This is not
   * intended for extensive use, but rather for quick debugging purposes.
   * If an application wants to extensively interact with the response
   * directly, use of getResponse is preferred.
   */
  public void print(String text)
  {
    try
    {
      this.request.print(text);
    }
    catch (IOException ioexc)
    {
      this.log.log("IOException on print().");
    }
  }

  /**
   * Similar to below but specifies a basic description.
   */
  protected void processRenderException(Exception exc, String pageName)
  {
    this.processRenderException(exc, pageName, "Including " + pageName);
  }

  /**
   * Ask the Dispatcher to process an exception that was encountered while
   * attempting to render a JSP.  The Dispatcher will in turn dispatch
   * the exception to its list of ExceptionHandlers.
   */
  protected void processRenderException(Exception exc, String pageName, String description)
  {
    this.log.log("Exception while including " + pageName + ": " + exc);

    this.dispatcher.dispatchException(this, exc, description);
  }

  /**
   * Redirect the browser to a new location using HTTP response code 302
   * (moved temporarily).  This is common after processing a POST.  Use
   * redirectPermanent if HTTP response 301 is desired.
   *
   * @param redirectDestinationUrl The destination for the browser.
   */
  public boolean redirect(String redirectDestinationUrl)
  {
    return this.request.redirect(redirectDestinationUrl);
  }

  /**
   * Sends a permanent redirect (HTTP response code 301) by setting the
   * Location header and then sending error code 301.
   *
   * @param redirectDestinationUrl The destination for the browser.
   */
  public boolean redirectPermanent(String redirectDestinationUrl)
  {
    return this.request.redirectPermanent(redirectDestinationUrl);
  }

  /**
   * Set the content type of the response.
   */
  public void setContentType(String contentType)
  {
    this.contentTypeSet = true;
    this.request.setContentType(contentType);
  }

  /**
   * Sets the request and response character sets to those provided by the
   * application.
   */
  public void setDefaultCharacterSets()
  {
    // Set the request character set, if it has been specified in the
    // application's configuration.
    try
    {
      this.request.setCharacterEncoding(this.application.getDefaultRequestCharset().displayName());
    }
    catch (UnsupportedEncodingException ueexc)
    {
      // Do nothing.
    }
  }

  /**
   * Sets a user session's locale.  This is merely a pass-through convenience
   * method for calling localeManager.getLocale.
   */
  public void setLocale(Locale locale)
  {
    this.application.getLocaleManager().setLocale(this, locale);
  }

  /**
   * Sets a user session's locale.  This is merely a pass-through convenience
   * method for calling localeManager.setLocale.
   *
   * @param languageID the Language ID for the Locale.
   * @param countryID the Country ID for the Locale.
   */
  public void setLocale(String languageID, String countryID)
  {
    this.application.getLocaleManager().setLocale(this, languageID, countryID);
  }

  /**
   * If tracking the request number is enabled, sets it. Intended to only be
   * called by InfrastructureServlet.
   */
  public void setRequestNumber(long requestNumber)
  {
    this.requestNumber = requestNumber;
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "Context [" + getClientId() + "]";
  }

  /**
   * Returns {@code true} if the request value is a valid token.  If it
   * is, only the first call to {@code validateToken(name)} will return
   * {@code true}.
   *
   * @param name the name of the request value
   * @return {@code true} if the request value is a valid token
   */
  public boolean validateToken(String name)
  {
    final String token = query().get(name);
    return token != null && getTokenProvider().validate(TOKEN_ENCODING.decode(token));
  }

  /**
   * Sets the response status code.  See the full list of response codes
   * provided by HttpServletResponse for more information.
   */
  public void setStatus(int status)
  {
    this.request.setStatus(status);
  }

  /**
   * Pass through method to get the request's content type from the wrapped request.
   * @return The content type of the request.
   */
  public String getRequestContentType()
  {
    return this.request.getRequestContentType();
  }

  public boolean isContentTypeSet()
  {
    return contentTypeSet;
  }
}
