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
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.Map.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.gemini.jsp.*;
import com.techempower.gemini.session.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Request that's specific to HttpServlet. This request is a
 * wrapper for HttpServletRequest and HttpServletResponse. 
 * 
 * The default rendering for this request is JSP.
 */
public class ResinHttpRequest
  implements HttpRequest
{

  //
  // Member variables.
  //
  
  private final GeminiApplication  application;
  private final Logger             log = LoggerFactory.getLogger(getClass());
  private final HttpServletRequest request;
  private final HttpServletResponse   response;
  private final ServletContext        servletContext;
  private final BasicInfrastructure   infrastructure;
  
  private boolean                     jspHasBeenIncluded  = false; // have we called includeJSP at least once?
  private final HttpMethod            method;

  private boolean                     rewritten = false;
  private Map<String, List<String>>   rewrittenParameters = null;
  
  //
  // Member methods.
  //
  
  /**
   * Standard constructor
   */
  public ResinHttpRequest(HttpServletRequest request, HttpServletResponse response,
      ServletContext servletContext, GeminiApplication application)
  {
    this.application     = application;
    this.request         = request;
    this.response        = response;
    this.servletContext  = servletContext;
    this.infrastructure  = this.application.getInfrastructure();
    
    // Set the method of the request.
    HttpMethod httpMethod;
    try {
      httpMethod = HttpMethod.valueOf(request.getMethod());
    } catch (IllegalArgumentException e) {
      log.info("Unsupported HTTP method: {}", request.getMethod());
      httpMethod = null;
    }
    method = httpMethod;

    // Set the default response data type.  By default this will be
    // "text/html;charset=utf-8".
    response.setContentType(this.application.getDefaultResponseType());

    // Attempt to force x-www-form-urlencoded data from a PUT to be
    // included in the parameters of this request.
    this.forceFormDataToParametersFromPutRequest();
  }
  
  /**
   * Checks to see this request is a PUT, if there is form data present
   * in the request body stream, and, if so, parses the keys and values
   * and adds them as rewritten parameters for this request.
   *  <p>
   * Note: this may be useless for non-servlet implementations of
   * Gemini, as the non-servlet specifications may already do this
   * by default.
   *  <p>
   * Note: this may also be deprecated at some point as the servlet
   * spec may be updated to include this rational behavior by default
   * in a future version: https://java.net/jira/browse/SERVLET_SPEC-58
   */
  private void forceFormDataToParametersFromPutRequest()
  {
    // Attempt to add in override values from the body of a PUT.
    if (this.method == HttpMethod.PUT &&
        this.request.getContentType() != null &&
        this.request.getContentType().toLowerCase()
          .contains("application/x-www-form-urlencoded"))
    {
      // Note: after this reader is opened and consumed, it cannot be reopened
      try (BufferedReader reader = this.request.getReader())
      {
        // The entire parameter line (e.g. "test=value&test2=value2")
        final String parameterLine = reader.readLine();
        if (parameterLine != null)
        {
          final String[] parameters = parameterLine.split("&");
          rewrittenParameters = new HashMap<>(parameters.length);
          // As key-value strings (e.g. "test=value")
          for (String parameter : parameters)
          {
            final String[] keyValue = parameter.split("=");
            if (keyValue.length == 2)
            {
              // Note: The World Wide Web Consortium Recommendation states that 
              // UTF-8 should be used. Not doing so may introduce 
              // incompatibilities.
              final String key = URLDecoder.decode(keyValue[0], 
                  StringHelper.emptyDefault(
                      this.request.getCharacterEncoding(),
                      StandardCharsets.UTF_8.name()));
              final String value = URLDecoder.decode(keyValue[1], 
                  StringHelper.emptyDefault(
                      this.request.getCharacterEncoding(),
                      StandardCharsets.UTF_8.name()));
              final List<String> values;
              if (rewrittenParameters.get(key) != null)
              {
                values = rewrittenParameters.get(key);
              }
              else
              {
                values = new ArrayList<>(1);
              }
              values.add(value);
              
              rewrittenParameters.put(key, values);
            }
          }
          this.rewritten = true;
        }
      }
      catch (IOException ioe)
      {
        // This cannot really happen in practice.
        this.log.info("Exception thrown trying to request request stream", ioe);
      }
    }
  }

  /**
   * Returns a reference to the application.
   */
  protected GeminiApplication getApplication()
  {
    return this.application;
  }
  
  /**
   * Returns the raw HttpServletRequest for this request.
   */
  public HttpServletRequest getRawRequest()
  {
    return this.request;
  }
  
  /**
   * Returns the servlet context for this request (not to be confused with 
   * the com.techempower.gemini.Context
   */
  public ServletContext getServletContext()
  {
    return this.servletContext;
  }
  
  /**
   * Returns the raw HttpServletResponse for this request.
   */
  public HttpServletResponse getRawResponse()
  {
    return this.response;
  }
  
  /**
   * Sets the request and response character sets to those provided by the
   * application.
   */
  @Override
  public void setCharacterEncoding(String encoding)
      throws UnsupportedEncodingException
  {
    this.response.setCharacterEncoding(encoding);
    this.request.setCharacterEncoding(encoding);
  }

  @Override
  public String getRequestCharacterEncoding()
  {
    return this.request.getCharacterEncoding();
  }

  /**
   * Returns all the Header names for this request
   */
  @Override
  public Enumeration<String> getHeaderNames()
  {
    return this.request.getHeaderNames();
  }

  /**
   * Returns that value for the specified header.
   * 
   * Null if the header isn't present.
   */
  @Override
  public String getHeader(String name)
  {
    return this.request.getHeader(name);
  }

  /**
   * Returns all parameter names for this request
   */
  @Override
  public Enumeration<String> getParameterNames()
  {
    if (this.rewritten)
    {
    	Set<String> allParams = new HashSet<>();
    	allParams.addAll(this.rewrittenParameters.keySet());
    	for(Enumeration<String> name = this.request.getParameterNames(); name.hasMoreElements(); )
    	{
    		allParams.add(name.nextElement());
    	}
      return Collections.enumeration(allParams);
    }
    else
    {
      return this.request.getParameterNames();
    }
  }

  /**
   * Returns the parameter value or null if if doesn't exist.
   */
  @Override
  public String getParameter(String name)
  {
    if (this.rewritten)
    {
      List<String> values = this.rewrittenParameters.get(name);
      if (values == null || values.isEmpty())
      {
        return this.request.getParameter(name);
      }
      return values.get(0);
    }
    else
    {
      return this.request.getParameter(name);
    }
  }
  
  /**
   * Sets the request parameter, or overrides the parameter if it already exists
   */
  @Override
  public void putParameter(String name, String value)
  {
    if (this.rewrittenParameters == null)
    {
      this.rewrittenParameters = new HashMap<>();
    }
    if (this.rewrittenParameters.get(name) == null)
    {
      this.rewrittenParameters.put(name, new ArrayList<String>());
    }
    this.rewrittenParameters.get(name).add(value);
  }
  
  /**
   * Removes the parameter from this request.
   */
  @Override
  public void removeParameter(String name)
  {
    if (this.rewrittenParameters != null)
    {
      this.rewrittenParameters.remove(name);
    }
  }
  
  /**
   * Returns an array of values for a given request parameter
   */
  @Override
  public String[] getParameterValues(String name)
  {
    if (this.rewritten)
    {
      List<String> values = this.rewrittenParameters.get(name);
      if (values == null)
      {
        return this.request.getParameterValues(name);
      }
      return values.toArray(new String[values.size()]);
    }
    return this.request.getParameterValues(name);
  }
  
  /**
   * Gets all of the request parameters as a Map.
   */
  public Map<String,String> getAllRequestParameters()
  {
    HashMap<String,String> map = new HashMap<>();
    Enumeration<String> enumeration = getParameterNames();
    while (enumeration.hasMoreElements())
    {
      String name = enumeration.nextElement();
      map.put(name, this.request.getParameter(name));
    }
    return map;
  }
  
  /**
   * Produces a string usable as a key in a Map, in case you want to cache
   * a response.
   */
  public String getAllRequestParametersKey()
  {
    return getAllRequestParametersKey(null);
  }
  
  /**
   * Produces a string usable as a key in a Map, in case you want to cache
   * a response.
   */
  public String getAllRequestParametersKey(String[] expectedParams)
  {
    SortedMap<String, String> params = new TreeMap<>(getAllRequestParameters());
    StringBuilder sb = new StringBuilder();
    if (expectedParams == null)
    {
      for (Entry<String,String> e : params.entrySet())
      {
        sb.append(e.getKey());
        sb.append("=");
        sb.append(e.getValue());
        sb.append("|");
      }
    }
    else
    {
      for (String s : expectedParams)
      {
        for (Entry<String,String> e : params.entrySet())
        {
          if( e.getKey().startsWith(s))
          {
            sb.append(e.getKey());
            sb.append("=");
            sb.append(e.getValue());
            sb.append("|");
          }
        }
      }
    }
    return sb.toString();
  }
  
  /**
   * Removes all request parameters from this request
   */
  @Override
  public void removeAllRequestValues()
  {
    if (this.rewrittenParameters != null)
    {
      this.rewrittenParameters.clear();
    }
  }
  
  /**
   * Calls ServletResponse.encodeURL().  This is used for tracking sessions
   * when cookies are disabled.  EVERY SINGLE URL used in the web site
   * must be encoded for this to work.
   *
   * @param url the original un-encoded URL.
   */
  @Override
  public String encodeURL(String url)
  {
    return this.response.encodeURL(url);
  }
  
  /**
   * Output text directly to the response via a PrintWriter.  This is not
   * intended for extensive use, but rather for quick debugging purposes.
   * If an application wants to extensively interact with the response
   * directly, use of getResponse is preferred.
   */
  @Override
  public void print(String text) throws IOException
  {
    this.response.getWriter().println(text);
  }
  
  /**
   * Returns the PrintWriter for the Request that can be directly written to
   */
  @Override
  public PrintWriter getWriter() throws IOException
  {
    return this.response.getWriter();
  }
  
  /**
   * Gets the current request's "signature;" that is, the URL including all
   * of the parameters.  This will construct a URL that will always use a
   * "GET" form even if the request itself was POSTed.
   */
  @Override
  public String getRequestSignature()
  {
    // Check to see if the request was forwarded.  If so, then we want to 
    // use the forwarded URI and query string to construct our URL, because 
    // that will tell us what was actually requested by the user.
    
    Object forwardURI = request.getAttribute("javax.servlet.forward.request_uri");
    if (forwardURI == null)
    {
      // The request was not forwarded, so simply concatenate the request URL 
      // and the query string (if present).
      
      String queryString = request.getQueryString();
      if (StringHelper.isNonEmpty(queryString))
      {
        return request.getRequestURL() + "?" + queryString;
      }
      else
      {
        return request.getRequestURL().toString();
      }
    }
    
    // The request was forwarded.  Build the full URL manually.
    
    StringBuilder sb = new StringBuilder();
    sb.append(request.getScheme());
    sb.append("://");
    sb.append(request.getServerName());
    int port = request.getServerPort();
    
    if (port > 0
        && port != 80
        && port != 443)
    {
      sb.append(":");
      sb.append(port);
    }
    
    sb.append(forwardURI);
    
    Object forwardQueryString = request.getAttribute("javax.servlet.forward.query_string");
    if (forwardQueryString != null)
    {
      sb.append("?");
      sb.append(forwardQueryString);
    }
    
    return sb.toString();
  }

  @Override
  public String getRealPath(String path)
  {
    return servletContext.getRealPath(path);
  }
  
  public String getContextPath()
  {
    return request.getContextPath();
  }
  
  /**
   * Returns the base url for this request (minus and request parameters that 
   * may have been sent with the request.
   */
  @Override
  public StringBuffer getRequestURL()
  {
    return request.getRequestURL();
  }
  
  /**
   * Returns the request uri. this differs from the url in that
   * the url http://some-domain.com/admin/users would have a uri 
   * of admin/users
   */
  @Override
  public String getRequestURI()
  {
    return request.getRequestURI();
  }
  
  /**
   * Gets a cookie from the request by name.
   *
   * @param name the cookie name
   *
   * @return A cookie object or null if no such cookie is found
   */
  @Override
  @SuppressWarnings("unchecked")
  public <C extends Cookie> C getCookie(String name)
  {
    C   returnedCookie = null;
    javax.servlet.http.Cookie[] theCookies = request.getCookies();

    if (theCookies != null)
    {
      for (int i = 0; i < theCookies.length; i++)
      {
        if (theCookies[i].getName().equals(name))
        {
          returnedCookie = (C)new HttpCookie(theCookies[i]);
          break;
        }
      }
    }

    return returnedCookie;
  }
  
  /**
   * Sends a cookie to the response.  Use an empty String for "path" to not 
   * have a path parameter set for the cookie.
   *
   * @param name the cookie's name
   * @param value the cookie's value
   * @param domain the domain name for the cookie.
   * @param path the server path to which the cookie applies.
   * @param age the cookie's lifetime
   * @param secure the cookie should only be used for HTTPS/SSL requests.
   */
  @Override
  public void setCookie(String name, String value, String domain, String path,
      int age, boolean secure)
  {
    //log.debug("Set a new cookie to value " + value);
    final javax.servlet.http.Cookie targetCookie = 
        new javax.servlet.http.Cookie(name, value);
    if (StringHelper.isNonEmpty(domain))
    {
      targetCookie.setDomain(domain);
    }
    if (StringHelper.isNonEmpty(path))
    {
      targetCookie.setPath(path);
    }
    targetCookie.setSecure(secure);
    //if (secure) { this.log.debug("Cookie " + name + " set as secure.", LogLevel.DEBUG); }
    targetCookie.setMaxAge(age);
    response.addCookie(targetCookie);
  }
  
  /**
   * Deletes a cookie from the user's browser.  This is achieved by setting 
   * the cookie's lifetime to 0.  Send empty path String to not specify a 
   * path for the cookie.
   *
   * @param name the cookie's name
   * @param path the server path to which the cookie applies.
   */
  @Override
  public void deleteCookie(String name, String path)
  {
    javax.servlet.http.Cookie deleteCookie = new javax.servlet.http.Cookie(name,"");
    if (StringHelper.isNonEmpty(path))
    {
      deleteCookie.setPath(path);
    }
    deleteCookie.setMaxAge(0);            // Set to expire immediately.
    response.addCookie(deleteCookie);
  }
  
  /**
   * Gets the client's IP address as a String.
   */
  @Override
  public String getClientId()
  {
    return request.getRemoteAddr();
  }

  /**
   * Gets the request's method as a String.
   */
  @Override
  public HttpMethod getRequestMethod()
  {
    return method;
  }

  /**
   * Redirect the browser to a new location using HTTP response code 302
   * (moved temporarily).  This is common after processing a POST.  Use
   * redirectPermanent if HTTP response 301 is desired.
   * 
   * @param redirectDestinationUrl The destination for the browser.
   */
  @Override
  public boolean redirect(String redirectDestinationUrl)
  {
    try
    {
      response.sendRedirect(redirectDestinationUrl);
      return true;
    }
    catch (IOException ioexc)
    {
      return false;
    }
  }
  
  /**
   * Sends a permanent redirect (HTTP response code 301) by setting the
   * Location header and then sending error code 301.
   * 
   * @param redirectDestinationUrl The destination for the browser.
   */
  @Override
  public boolean redirectPermanent(String redirectDestinationUrl)
  {
    try
    {
      setResponseHeader("Location", redirectDestinationUrl);
      this.response.sendError(HttpServletResponse.SC_MOVED_PERMANENTLY);
      return true;
    }
    catch (Exception exc)
    {
      return false;
    }
  }
  
  /**
   * Sets a response header.  This is a pass-through to response.setHeader.
   *
   * @param headerName the header name (e.g., "Content-disposition")
   * @param value the header value (e.g., "attachment; filename=blah.txt")
   */
  @Override
  public void setResponseHeader(String headerName, String value)
  {
    this.response.setHeader(headerName, value);
  }
  
  /**
   * Gets a reference to the output stream from the response.  This is a
   * pass-through to response.getOutputStream().
   */
  @Override
  public OutputStream getOutputStream()
    throws IOException
  {
    return this.response.getOutputStream();
  }
  
  /**
   * Gets a reference to the output stream from the response.  This is a
   * pass-through to response.getOutputStream().
   * @throws IOException 
   */
  @Override
  public InputStream getInputStream() 
    throws IOException
  {
    return this.request.getInputStream();
  }

  @Override public String getRequestContentType()
  {
    return this.request.getContentType();
  }

  /**
   * Sets the response's content type.
   *
   * @param contentType the content type String to provide in the response.
   */
  @Override
  public void setContentType(String contentType)
  {
    this.response.setContentType(contentType);
  }
  
  /**
   * (9) Uses a RequestDispatcher to include a JSP file.  Before invoking a JSP,
   *     this method logs the invocation in the RequestLog using an optional
   *     custom JSP filename to write into the log (if null, the real JSP filename
   *     will be used).  This method also sets the content type as provided.
   *
   * @param pageName the filename of the JSP to invoke, relative to the
   *        JSP root directory specified in the configuration file.
   *
   * @return true if no exception is caught; false if an exception is
   *         caught.
   */
  public boolean render(String pageName, boolean fullyQualified) throws Exception
  {
    return forwardToJsp(pageName, fullyQualified);
  }
  
  public String renderToString(String path)
  {
    return StringServletOutputStream.invokeToString(path, this);
  }

  /**
   * Fulfills the "render" method's contract by forwarding to a JSP.
   *
   * @param pageName the filename of the JSP to invoke, relative to the
   *        JSP root directory specified in the configuration file.
   *
   * @return true if no exception is caught; false if an exception is
   *         caught.
   * @throws IOException 
   * @throws ServletException 
   */
  protected boolean forwardToJsp(String pageName, boolean fullyQualified) 
      throws ServletException, IOException
  {
    String path = "";
    
    if (fullyQualified)
    {
      path = pageName;
    }
    else
    {
      path = this.infrastructure.getJspDirectory() + pageName;
    }
    
    this.servletContext.getRequestDispatcher(path).forward(
        this.request, this.response);
    this.jspHasBeenIncluded = true;
    
    return true;
  }
  
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
  @Override
  public void setExpiration(int secondsFromNow)
  {
    this.response.setDateHeader("Expires", System.currentTimeMillis()
      + (secondsFromNow * UtilityConstants.SECOND));
  }
  
  /**
   * Returns the current URI of the request.
   */
  @Override
  public String getCurrentURI()
  {
    return this.request.getRequestURI();
  }
  
  /**
   * Return whether the current request is secure.
   */
  @Override
  public boolean isSecure()
  {
    return this.request.isSecure();
  }
  
  /**
   * Has includeJSP been called at least once?
   */
  public boolean hasJspBeenIncluded()
  {
    return this.jspHasBeenIncluded;
  }
  
  /**
   * Returns whether or not the response has been committed. This means that you can no longer 
   * write to the outputstream. 
   */
  @Override
  public boolean isCommitted()
  {
    return this.response.isCommitted();
  }
  
  /**
   * Sends an error to the client.
   */
  public void sendError(int error) throws IOException
  {
    this.response.sendError(error);
  }
  
  /**
   * Sends an error to the client.
   */
  public void sendError(int error, String message) throws IOException
  {
    this.response.sendError(error, message);
  }
  
  /**
   * Gets the query string of the request.
   */
  @Override
  public String getQueryString()
  {
    return this.request.getQueryString();
  }
  
  /**
   * Get the associated user session.
   * @param create Whether to force the creation of a session. In general,
   * pass false. When you go to store a session value, it will create a
   * session at that point if necessary.
   */
  @Override
  public Session getSession(boolean create)
  {
    return this.application.getSessionManager().getSession(
        this, create);
  }
  
  /**
   * Sets an attribute for this request
   */
  @Override
  public void setAttribute(String name, Object o)
  {
    this.request.setAttribute(name, o);
  }
  
  /**
   * Gets an attribute for this request
   */
  @Override
  public Object getAttribute(String name)
  {
    return this.request.getAttribute(name);
  }

  /**
   * Provides access to the Infrastructure.
   */
  @Override
  public Infrastructure getInfrastructure()
  {
    return this.infrastructure;
  }
  
  @Override
  public boolean isHead()
  {
    return method == HttpMethod.HEAD;
  }

  @Override
  public boolean isGet()
  {
    return method == HttpMethod.GET;
  }

  @Override
  public boolean isPost()
  {
    return method == HttpMethod.POST;
  }

  @Override
  public boolean isPut()
  {
    return method == HttpMethod.PUT;
  }

  @Override
  public boolean isDelete()
  {
    return method == HttpMethod.DELETE;
  }

  @Override
  public boolean isTrace()
  {
    return method == HttpMethod.TRACE;
  }

  @Override
  public boolean isOptions()
  {
    return method == HttpMethod.OPTIONS;
  }

  @Override
  public boolean isConnect()
  {
    return method == HttpMethod.CONNECT;
  }

  @Override
  public boolean isPatch()
  {
    return method == HttpMethod.PATCH;
  }
  
  /**
   * Tells the request that it has been rewritten
   */
  public void setRewritten(boolean rewritten)
  {
    this.rewritten = rewritten;
    if (rewritten && this.rewrittenParameters == null)
    {
      this.rewrittenParameters = new HashMap<>();
    }
  }
  
  /**
   * Writes a file to the response.  This method should be using similar to 
   * "includeJSP" - at the end of a handler method.
   * 
   * @param file The file on disk.
   * @param fileName The file name to present with the response; does not have
   *                 to match the real filename of the "file" parameter.
   * @param asAttachment Whether to include the file as an attachment.  If this 
   *                     is true, most browsers will prompt the user with a 
   *                     Save dialog.
   * @param contentType (optional) Forcibly sets the content type to the type
   *        indicated in the string. Pass null or empty string to determine
   *        content-type by file.
   * @return {@code true} unless an IOException occurs while writing the file
   *         to the response
   */
  public boolean includeFile(File file, String fileName, boolean asAttachment,
      String contentType)
  {
    // Flags whether or not we were able to find out the file's content type.
    boolean setContentType = false;
    
    // Set up the file attachment.
    InitConfig servletConfig = this.application.getServletConfig();
    if (servletConfig != null)
    {
      if(StringHelper.isNonEmpty(contentType))
      {
        setContentType(contentType);
        setContentType = true;
      }
      else
      {
        String mimeType = servletConfig.getMimeType(fileName);
        if (mimeType != null)
        {
          setContentType(mimeType);
          setContentType = true;
        }
      }
    }
    
    // If we could not determine the content type, set it to octet-stream.
    // At least the user will be able to save the file.
    if (!setContentType)
    {
      setContentType("application/octet-stream;name=\""
          + fileName + "\"");
    }
    
    if (asAttachment)
    {
      setResponseHeader("Content-disposition",
          "attachment; filename=\""  + fileName + "\"");
    }
    
    byte[] buffer = new byte[4096];
    try (
        FileInputStream fis = new FileInputStream(file);
        OutputStream os = getOutputStream();
        )
    {
      // Write out the file.
      int read;
      while (fis.available() > 0)
      {
        read = fis.read(buffer);
        os.write(buffer, 0, read);
      }
    }
    catch (IOException e)
    {
      this.log.info("IOException while including file.", e);
      return false;
    }

    return true;
  }
  
  /**
   * Sets the response status code.  See the full list of response codes
   * provided by HttpServletResponse for more information.
   */
  @Override
  public void setStatus(int status)
  {
    this.response.setStatus(status);
  }
}
