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
package com.techempower.gemini;

import java.io.*;
import java.util.*;

import com.techempower.gemini.session.*;

/**
 * The Request interface is meant to be a servlet independent view of the traditional request/response 
 * model. 
 *
 * A request is responsible for providing access to:
 * <ul>
 *  <li>Parameters
 *  <li>Headers
 *  <li>Cookies
 *  <li>Sessions
 * </ul>
 * 
 * While the names seem to imply an HTTP specific nature, the implementation does not need to adhere
 * to the exact HTTP implementation of these items.
 * 
 * The request will also be responsible for handling rendering. In a servlet request, this is often 
 * times a jsp. But the request will also need to handle file attachments, printing directly to the 
 * response stream, as well as other forms.
 */
public interface Request
{
  public enum HttpMethod
  {
    GET,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    TRACE,
    CONNECT,
    PATCH,
    HEAD;
  }

  
  public static final String 
    // Http Header names
    HEADER_AUTHORIZATION                    = "Authorization",
    HEADER_ORIGIN                           = "Origin",
    HEADER_VARY                             = "Vary",
    HEADER_WILDCARD                         = "*",
    HEADER_ACCESS_CONTROL_ALLOW_ORIGIN      = "Access-Control-Allow-Origin",
    HEADER_ACCESS_CONTROL_REQUEST_METHOD    = "Access-Control-Request-Method",
    HEADER_ACCESS_CONTROL_ALLOW_METHOD      = "Access-Control-Allow-Methods",
    HEADER_ACCESS_CONTROL_EXPOSED_HEADERS   = "Access-Control-Exposed-Headers",
    HEADER_ACCESS_CONTROL_REQUEST_HEADERS   = "Access-Control-Request-Headers",
    HEADER_ACCESS_CONTROL_ALLOW_HEADERS     = "Access-Control-Allow-Headers",
    HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials",
    HEADER_ACCESS_CONTROL_MAX_AGE           = "Access-Control-Max-Age";
  
  /**
   * Sets the request and response character sets to those provided by the
   * application.
   */
  void setCharacterEncoding(String encoding) throws UnsupportedEncodingException;

  /**
   * Returns the character encoding associated with the request.
   */
  String getRequestCharacterEncoding();

  /**
   * Returns all the Header names for this request
   */
  Enumeration<String> getHeaderNames();
  
  /**
   * Returns that value for the specified header.
   * 
   * Null if the header isn't present.
   */
  String getHeader(String name);
  
  /**
   * Returns all parameter names for this request
   */
  Enumeration<String> getParameterNames();
  
  /**
   * Returns the parameter value or null if if doesn't exist.
   */
  String getParameter(String name);
  
  /**
   * Sets the request parameter, or overrides the parameter if it already exists
   */
  void putParameter(String name, String value);
  
  /**
   * Removes the parameter from this request.
   */
  void removeParameter(String name);
  
  /**
   * Removes all request parameters from this request
   */
  void removeAllRequestValues();
  
  /**
   * Returns an array of values for a given request parameter
   */
  String[] getParameterValues(String name);
  
  /**
   * Calls ServletResponse.encodeURL().  This is used for tracking sessions
   * when cookies are disabled.  EVERY SINGLE URL used in the web site
   * must be encoded for this to work.
   *
   * @param url the original un-encoded URL.
   */
  String encodeURL(String url);
  
  /**
   * Output text directly to the response via a PrintWriter.  This is not
   * intended for extensive use, but rather for quick debugging purposes.
   * If an application wants to extensively interact with the response
   * directly, use of getResponse is preferred.
   */
  void print(String text) throws IOException;
  
  /**
   * Returns the PrintWriter for the Request that can be directly written to
   */
  PrintWriter getWriter() throws IOException;
  
  /**
   * Gets the current request's "signature;" that is, the URL including all
   * of the parameters.  This will construct a URL that will always use a
   * "GET" form even if the request itself was POSTed.
   */
  String getRequestSignature();
  
  String getRealPath(String path);
  
  /**
   * Returns the base url for this request (minus and request parameters that 
   * may have been sent with the request.
   */
  StringBuffer getRequestURL();
  
  /**
   * Returns the request uri. this differs from the url in that
   * the url http://some-domain.com/admin/users would have a uri 
   * of admin/users
   */
  String getRequestURI();

  /**
   * Gets a cookie from the request by name.
   *
   * @param name the cookie name
   *
   * @return A cookie object or null if no such cookie is found
   */
  <C extends Cookie> C getCookie(String name);
  
  /**
   * Sends a cookie in the response.  A default cookie lifetime of 30 days is
   * used.  The Servlet's path is used as a default path setting.
   */
  void setCookie(String name, String value, String domain, String path, 
      int age, boolean secure);
  
  /**
   * Deletes a cookie from the user's browser.  This is achieved by setting
   * the cookie's lifetime to 0.  Uses the Servlet's path as the cookie's 
   * path.
   *
   * @param name the cookie's name
   */
  void deleteCookie(String name, String path);
  
  /**
   * Gets the client's IP address as a String.
   */
  String getClientId();
  
  /**
   * Gets the request's method as a String.
   */
  HttpMethod getRequestMethod();

  /**
   * Gets the input stream for the request body.
   */
  InputStream getInputStream() throws IOException;
  
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
   * Sets a response header.  This is a pass-through to response.setHeader.
   *
   * @param headerName the header name (e.g., "Content-disposition")
   * @param value the header value (e.g., "attachment; filename=blah.txt")
   */
  void setResponseHeader(String headerName, String value);
  
  /**
   * Gets a reference to the output stream from the response.  This is a
   * pass-through to response.getOutputStream().
   */
  OutputStream getOutputStream() throws IOException;

  /**
   * Gets the response's content type.
   *
   */
  String getRequestContentType();

  /**
   * Sets the response's content type.
   *
   * @param contentType the content type String to provide in the response.
   */
  void setContentType(String contentType);
  
  /**
   * Sets the "Expires" response header easily.  Just provide a time-out
   * period in seconds.  This can be useful with caching application
   * servers in scenarios where the contents of an invoked page are not
   * specific to a user and their content can be cached for a while.
   *    <p>
   * NOTE: Using the setExpiration method to take advantage of application-
   * server caching will cripple the ability of Gemini to write useful
   * request logs.  Requests for cached pages will <i>not</i> be caught
   * by the Context.
   */
  void setExpiration(int secondsFromNow);
  
  /**
   * Returns the current URI of the request.
   */
  String getCurrentURI();
  
  /**
   * Return whether the current request is secure.
   */
  boolean isSecure();
  
  /**
   * Returns whether or not the response has been committed. This means that you can no longer 
   * write to the outputstream. 
   */
  boolean isCommitted();
  
  /**
   * Gets the query string of the request.
   */
  String getQueryString();
  
  /**
   * Get the associated user session.
   * @param create Whether to force the creation of a session. In general,
   * pass false. When you go to store a session value, it will create a
   * session at that point if necessary.
   */
  Session getSession(boolean create);
  
  /**
   * Sets an attribute for this request
   */
  void setAttribute(String name, Object o);
  
  /**
   * Gets an attribute of this request
   */
  Object getAttribute(String name);
  
  /**
   * Provides access to the Infrastructure.
   */
  Infrastructure getInfrastructure();
  
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
   * Is the request a OPTIONS request.
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
   * Sets the response status code.  See the full list of response codes
   * provided by HttpServletResponse for more information.
   */
  void setStatus(int status);
}
