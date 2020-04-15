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

import com.techempower.gemini.context.*;
import com.techempower.gemini.internationalization.GeminiResources;
import com.techempower.gemini.session.Session;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

public interface Context {

    /**
     * Gets the client's identifier, for web requests, this will be the client's
     * IP address.
     */
    String getClientId();

    /**
     * Returns the current URI of the request.
     */
    String getCurrentUri();

    /**
     * Gets the time since the start of this Context, in milliseconds.
     */
    long getDuration();

    /**
     * Set the content type of the response.
     */
    void setContentType(String contentType);

    /**
     * Sets the request and response character sets to those provided by the
     * application.
     */
    void setDefaultCharacterSets();

    /**
     * Gets a user session's locale.  If no locale it set, the default locale
     * is returned.  This is merely a pass-through convenience method for
     * calling localeManager.getLocale.
     *   <p>
     * In a locale-aware application, it is more typical to just call
     * getResources to get a reference to the GeminiResources to use for this
     * user's current request.
     */
    Locale getLocale();

    /**
     * Sets a user session's locale.  This is merely a pass-through convenience
     * method for calling localeManager.getLocale.
     */
    void setLocale(Locale locale);

    /**
     * Sets a user session's locale.  This is merely a pass-through convenience
     * method for calling localeManager.setLocale.
     *
     * @param languageID the Language ID for the Locale.
     * @param countryID the Country ID for the Locale.
     */
    void setLocale(String languageID, String countryID);

    /**
     * Gets a reference to the output stream from the response.  This is a
     * pass-through to response.getOutputStream().
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Gets the query string of the request.
     */
    String getQueryString();

    /**
     * Pass-though to request.getRealPath.
     */
    String getRealPath(String path);

    /**
     * Get the associated Request.
     */
    Request getRequest();

    /**
     * If request counting is enabled, return this request's number.
     */
    long getRequestNumber();

    /**
     * If tracking the request number is enabled, sets it. Intended to only be
     * called by InfrastructureServlet.
     */
    void setRequestNumber(long requestNumber);

    /**
     * Gets the current request's "signature;" that is, the URL including all
     * of the parameters.  This will construct a URL that will always use a
     * "GET" form even if the request itself was POSTed.
     */
    String getRequestSignature();

    /**
     * Returns the request URI.  A URI is the portion of a URI after the
     * protocol and domain.  The URL http://techempower.com/admin/users has a
     * URI of admin/users.
     */
    String getRequestUri();

    /**
     * Gets or initializes and then gets a reference to the appropriate
     * GeminiResourceBundle for this user given the Locale--or lack thereof--
     * stored in the user's session.  If your application is non-locale-
     * aware, you can optionally always return a static reference to a
     * DefaultGeminiResources object, to avoid the session lookup.
     */
    GeminiResources getResources();

    /**
     * Gets the full Secure URL to the Servlet.
     */
    String getSecureUrl();

    /**
     * Gets the name (also sometimes called the "URL") of the servlet.  Again,
     * this Context version of the method will encode session information into
     * the URL if the browser does not support cookies.
     */
    String getUrl();

    /**
     * Get a named-value interface to the Session.
     */
    SessionNamedValues session();

    /**
     * Gets the request's file attachments.
     */
    Attachments files();

    /**
     * Get a named-value interface to the Delivery map.
     */
    Delivery delivery();

    /**
     * Gets an interface for working with Cookies.
     */
    Cookies cookies();

    /**
     * Gets an interface for working with request and response Headers.
     */
    Headers headers();

    /**
     * Gets an interface for working with session Messages.
     */
    Messages messages();

    /**
     * Gets an interface for working with the request's query parameters.
     */
    Query query();

    /**
     * Gets the associated user session.
     *
     * @param create Whether to force the creation of a session. In general,
     * pass false. When you go to store a session value, it will create a
     * session at that point if necessary.
     */
    Session getSession(boolean create);

    /**
     * Gets the response writer.
     * @return
     * @throws IOException
     */
    PrintWriter getWriter() throws IOException;

    /**
     * Returns whether or not the response has been committed. This means that
     * you can no longer write to the outputstream.
     */
    boolean isCommitted();

    /**
     * Is the session new?  This method returns true if the Session was marked
     * as new by the Servlet API.
     */
    boolean isNewSession();

    /**
     * Is the request a HEAD request.
     */
    boolean isHead();

    /**
     * Is the request a GET request.
     */
    boolean isGet();

    /**
     * Is the request a POST request.
     */
    boolean isPost();

    /**
     * Is the request a PUT request.
     */
    boolean isPut();

    /**
     * Is the request a DELETE request.
     */
    boolean isDelete();

    /**
     * Is the request a TRACE request.
     */
    boolean isTrace();

    /**
     * Is the request an OPTIONS request.
     */
    boolean isOptions();

    /**
     * Is the request a CONNECT request.
     */
    boolean isConnect();

    /**
     * Is the request a PATCH request.
     */
    boolean isPatch();

    /**
     * Is a PriorRequest bound?  Determine if a PriorRequest has been
     * bound to this Context by a previous call to bindPriorRequest.
     */
    boolean isPriorRequestBound();

    /**
     * Return whether the current request is secure.
     */
    boolean isSecure();

    /**
     * Output text directly to the response via a PrintWriter.  This is not
     * intended for extensive use, but rather for quick debugging purposes.
     * If an application wants to extensively interact with the response
     * directly, use of getResponse is preferred.
     */
    void print(String text);

    /**
     * Redirect the browser to a new location using HTTP response code 302
     * (moved temporarily).  This is common after processing a POST.  Use
     * redirectPermanent if HTTP response 301 is desired.
     *
     * @param redirectDestinationUrl The destination for the browser.
     */
    boolean redirect(String redirectDestinationUrl);

    /**
     * Sends a permanent redirect (HTTP response code 301) by setting the
     * Location header and then sending error code 301.
     *
     * @param redirectDestinationUrl The destination for the browser.
     */
    boolean redirectPermanent(String redirectDestinationUrl);

    /**
     * Sets the response status code.  See the full list of response codes
     * provided by HttpServletResponse for more information.
     */
    void setStatus(int status);
}
