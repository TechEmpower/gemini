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
import java.util.Map.Entry;

import com.techempower.gemini.context.*;
import com.techempower.gemini.session.*;
import com.techempower.helper.*;
import com.techempower.log.*;

/**
 * Provides legacy applications with now-outdated Context functionality and
 * behaviors such as support for JSP and request "commands" (in the form of
 * a URL parameter named "cmd").
 *   <p>
 * Applications are strongly encouraged to move to Mustache and PathDispatcher,
 * but the effort to do so is non-trivial.  Until such a migration is complete,
 * application-specific Context subclasses should extend LegacyContext.  Once
 * the migration to Mustache and PathDispatcher is complete, the inheritance
 * should be changed to Context.
 */
public abstract class LegacyContext
     extends Context
{
  public  static final String PARAM_DO_NOT_USE_CACHED_RESPONSE = "no-cached";
  public  static final String PARAM_FORCE_CACHING              = "force-caching";

  private String                      command;                        // Dispatcher command.
  private String                      referencedRender    = null;     // Can be used for debugging purposes.
  private int                         dispatches          = 0;        // Number of dispatcher invocations.
  private String                      priorURL;

  /**
   * Constructor.
   */
  public LegacyContext(Request request, GeminiApplication application)
  {
    super(application, request);
  }

  /**
   * Binds a prior URL object found in the session, if found.  If
   * not found, this call does nothing.
   *
   * @param sessionValueName The name of the Session variable storing
   *        a prior URL.
   * @param removeFromSession Should the prior URL be removed
   *        from the session once used?
   */
  public void bindPriorURL(String sessionValueName,
                           boolean removeFromSession)
  {
    final String urlFromSession = session().get(sessionValueName);

    if (urlFromSession != null)
    {
      setPriorURL(urlFromSession);
      if (removeFromSession)
      {
        session().remove(sessionValueName);
      }
    }
  }

  /**
   * Gets the prior URL.  See setPriorURL.
   */
  public String getPriorURL()
  {
    return this.priorURL;
  }

  /**
   * Binds a prior URL to this Context.  This is only useful if the request
   * that resulted in this Context being created was an atypical request
   * for a Gemini application (e.g., one that did not have a command and
   * would otherwise appear to be a typical URL, e.g.,
   * www.domain.com/home.html).  This will be used by the LoginHandler
   * to redirect the user after login.
   */
  public void setPriorURL(String url)
  {
    this.priorURL = url;
  }

  /**
   * Sets the dispatcher command.  Normally, this is not called directly
   * unless a redispatch is occurring.  During the initial dispatching
   * event, a call to gatherCommand should be made to gather the command
   * from the URL.
   */
  public void setCommand(String command)
  {
    this.command = command;
  }

  /**
   * Gets the dispatcher command.
   */
  public String getCommand()
  {
    return this.command;
  }

  /**
   * Sets the referenced JSP.  Generally, this is not called directly, but
   * is invoked automatically by Context when a call to includeJSP or
   * forwardJSP is made.
   *
   * @param pageName the name of the JSP file.
   */
  public void setReferencedRender(String pageName)
  {
    this.referencedRender = pageName;
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
  public boolean render(String pageName)
  {
    return this.render(pageName, false);
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
  public boolean render(String pageName, boolean fullyQualified)
  {
    return this.render(pageName, fullyQualified, null);
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
   * @param fullyQualified it's usually preferable to call render with a relative path
   *        and have the render function use the default configuration to get the basepath.
   *        But sometimes it's necessary to give a fully qualified path that differs from the
   *        configured basepath, in this case, use this method with fullyQualified true.
   *
   * @return true if no exception is caught; false if an exception is
   *         caught.
   */
  public boolean render(String pageName, boolean fullyQualified, String contentType)
  {

    // Notify the Dispatch listeners that a JSP page is starting.
    getDispatcher().renderStarting(this, pageName);

    if (this.referencedRender == null)
    {
      // Only set the referencedJSP variable and the content type
      // of the response if we haven't already invoked a JSP.
      this.referencedRender = pageName;
    }

    try
    {
      if (contentType != null)
      {
        this.setContentType(contentType);
      }

      ((HttpRequest)getRequest()).render(pageName, fullyQualified);
    }
    catch (Exception exc)
    {
      this.processRenderException(exc, pageName);
    }
    finally
    {
      // Notify the Dispatch listeners that a JSP page is complete.
      getDispatcher().renderComplete(this);
    }

    return true;
  }

  /**
   * Sets the "Expires" response header easily.  Just provide a time-out
   * period in seconds.  This can be useful with caching application
   * servers in scenarios where the contents of an invoked page are not
   * specific to a user and their content can be cached for a while.
   */
  public void setExpiration(int secondsFromNow)
  {
    headers().expires(secondsFromNow);
  }

  /**
   * Sets the request parameter, or overrides the parameter if it already exists
   */
  public void putRequestValue(String name, String value)
  {
    getRequest().putParameter(name, value);
  }

  /**
   * Get the parameter names.
   */
  public Enumeration<String> getParameterNames()
  {
    return getRequest().getParameterNames();
  }

  /**
   * Returns whether disk caching is being used or not.
   */
  public boolean useDiskResponseCaching()
  {
    boolean toReturn = !getInfrastructure().usesCachedToDiskResponses();
    final String value = this.query().get(PARAM_DO_NOT_USE_CACHED_RESPONSE);
    if (value != null)
    {
      toReturn = Boolean.valueOf(value);
    }

    return !toReturn;
  }

  /**
   * Tells the request that it has been rewritten
   */
  public void setRewritten(boolean rewritten)
  {
    ((HttpRequest)getRequest()).setRewritten(rewritten);
  }

  /**
   * Sends a client-side HTTP redirection to the browser to direct to the
   * secure domain prefix and dispatcher.
   *
   * @param cmd a new dispatcher command to be processed by the Dispatcher
   *        when the request arrives at the secure domain.
   *
   * @return true if no IOException is encountered.
   */
  public boolean sendSecureCMDRedirect(String cmd)
  {
    return this.redirect(getInfrastructure().getSecureDomain()
        + getInfrastructure().getCmdUrl(cmd));
  }

  /**
   * Outputs an Exception's (Throwable's) stack trace to the PrintWriter.
   * This should only be used for debugging.
   */
  public void printException(Throwable throwable)
  {
    try
    {
      throwable.printStackTrace(getRequest().getWriter());
    }
    catch (IOException ioexc)
    {
      getLog().log("Exception while attempting to print exception to Context:\n" + ioexc);
    }
  }

  /**
   * Writes a file to the response.  This method should be using similar to
   * "includeJSP" - at the end of a handler method.
   *
   * @param file The file on disk.
   * @param fileName The file name to present with the response.
   * @param asAttachment Whether to include the file as an attachment.  If this
   *                     is true, most browsers will prompt the user with a
   *                     Save dialog.
   * @return {@code true} unless an IOException occurs while writing the file
   *         to the response
   */
  public boolean includeFile(File file, String fileName, boolean asAttachment)
  {
    return includeFile(file, fileName, asAttachment, null);
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
    return ((HttpRequest)getRequest()).includeFile(file, fileName, asAttachment, contentType);
  }

  /**
   * Writes a file to the response.  This method should be using similar to
   * "includeJSP" - at the end of a handler method.
   *
   * @param fileName The name of the file on disk.
   * @param asAttachment Whether to include the file as an attachment.  If this
   *                     is true, most browsers will prompt the user with a
   *                     Save dialog.
   * @return {@code true} unless an IOException occurs while writing the file
   *         to the response.
   */
  public boolean includeFile(String fileName, boolean asAttachment)
  {
    File file = new File(fileName);
    return includeFile(file, fileName, asAttachment, null);
  }

  /**
   * Writes a file to the response.  This method should be using similar to
   * "includeJSP" - at the end of a handler method.
   *
   * @param fileName The name of the file on disk.
   * @param asAttachment Whether to include the file as an attachment.  If this
   *                     is true, most browsers will prompt the user with a
   *                     Save dialog.
   * @param contentType (optional) Forcibly sets the content type to the type
   *        indicated in the string. Pass null or empty string to determine
   *        content-type by file.
   * @return {@code true} unless an IOException occurs while writing the file
   *         to the response.
   */
  public boolean includeFile(String fileName, boolean asAttachment, String contentType)
  {
    File file = new File(fileName);
    return includeFile(file, fileName, asAttachment, contentType);
  }

  /**
   * Gets the dispatcher invocation count.
   */
  public int getDispatches()
  {
    return this.dispatches;
  }

  /**
   * Increments the dispatcher invocation count.
   */
  public void incrementDispatches()
  {
    this.dispatches++;
  }

  /**
   * Gets a command URL for the Servlet using the standard (non-secure) prefix.
   */
  public String getStandardCmdURL(String commandString)
  {
    return getRequest().encodeURL(getInfrastructure().getStandardCmdUrl(commandString));
  }

  /**
   * Generates an secure URL to the servlet with a command and
   * a single boolean parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   */
  public String getStandardCmdURL(String commandString, String paramKey, boolean paramValue)
  {
    return getStandardCmdURL(commandString, paramKey, Boolean.toString(paramValue));
  }

  /**
   * Generates an absolute URL to the servlet with a command and
   * a single integer parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   */
  public String getStandardCmdURL(String commandString, String paramKey, int paramValue)
  {
    return getStandardCmdURL(commandString, paramKey, Integer.toString(paramValue));
  }

  /**
   * Generates an absolute URL to the servlet with a command and
   * a single parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   */
  public String getStandardCmdURL(String commandString, String paramKey, String paramValue)
  {
    return getStandardCmdURL(commandString, new String[] { paramKey }, new String[] { paramValue });
  }

  /**
   * Generates an absolute URL to the servlet with a command and
   * parameters specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKeys An array of additional param keys associated with the paramValues.
   * @param paramValues An array of param values associated with the paramKeys.
   */
  public String getStandardCmdURL(String commandString, String[] paramKeys,
      String[] paramValues)
  {
    return getInfrastructure().getStandardDomain() + this.getCmdURL(commandString, paramKeys, paramValues);
  }

  /**
   * Gets a command URL for the Servlet using the secure prefix.
   *
   */
  public String getSecureCmdURL(String commandString)
  {
    return getRequest().encodeURL(getInfrastructure().getSecureCmdUrl(commandString));
  }

  /**
   * Generates an absolute secure URL to the servlet with a command and
   * a single boolean parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   *
   */
  public String getSecureCmdURL(String commandString, String paramKey, boolean paramValue)
  {
    return getSecureCmdURL(commandString, paramKey, Boolean.toString(paramValue));
  }

  /**
   * Generates an absolute secure URL to the servlet with a command and
   * a single integer parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   *
   */
  public String getSecureCmdURL(String commandString, String paramKey, int paramValue)
  {
    return getSecureCmdURL(commandString, paramKey, Integer.toString(paramValue));
  }

  /**
   * Generates an absolute secure URL to the servlet with a command and
   * a single parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   */
  public String getSecureCmdURL(String commandString, String paramKey, String paramValue)
  {
    return getSecureCmdURL(commandString, new String[] { paramKey }, new String[] { paramValue });
  }

  /**
   * Generates an absolute secure URL to the servlet with a command and
   * parameters specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKeys An array of additional param keys associated with the paramValues.
   * @param paramValues An array of param values associated with the paramKeys.
   */
  public String getSecureCmdURL(String commandString, String[] paramKeys,
      String[] paramValues)
  {
    return getInfrastructure().getSecureDomain() + this.getCmdURL(commandString, paramKeys, paramValues);
  }

  /**
   * Generates an anchor tag with a servlet-command URL.  Again, this
   * Context version of the method will encode session information into the
   * URL if the browser does not support cookies.
   */
  public String getCmdAnchor(String commandString)
  {
    return "<a href=\"" + getCmdURL(commandString) + "\">";
  }

  /**
   * Generates a simple relative URL to the servlet with a command specified.
   * This version differs from the version in BasicInfrastructure in that
   * browsers that do not enable cookies will be able to take advantage of
   * sessions by encoding the session ID into the URL generated.
   */
  public String getCmdURL(String commandString)
  {
    return getRequest().encodeURL(getInfrastructure().getCmdUrl(commandString));
  }

  /**
   * Generates a simple relative URL to the servlet with a command and
   * a single boolean parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   */
  public String getCmdURL(String commandString, String paramKey, boolean paramValue)
  {
    return getCmdURL(commandString, paramKey, Boolean.toString(paramValue));
  }

  /**
   * Generates a simple relative URL to the servlet with a command and
   * a single integer parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   */
  public String getCmdURL(String commandString, String paramKey, int paramValue)
  {
    return getCmdURL(commandString, paramKey, Integer.toString(paramValue));
  }

  /**
   * Generates a simple relative URL to the servlet with a command and
   * a single long parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   */
  public String getCmdURL(String commandString, String paramKey, long paramValue)
  {
    return getCmdURL(commandString, paramKey, Long.toString(paramValue));
  }

  /**
   * Generates a simple relative URL to the servlet with a command and
   * a single parameter specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKey The name of the single parameter.
   * @param paramValue The value of the single parameter.
   */
  public String getCmdURL(String commandString, String paramKey, String paramValue)
  {
    return getCmdURL(commandString, new String[] { paramKey }, new String[] { paramValue });
  }

  /**
   * Generates a simple relative URL to the servlet with a command and
   * parameters specified.
   *
   * @param commandString A Gemini command string.
   * @param paramKeys An array of additional param keys associated with the paramValues.
   * @param paramValues An array of param values associated with the paramKeys.
   */
  public String getCmdURL(String commandString, String[] paramKeys,
      String[] paramValues)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getInfrastructure().getCmdUrl(commandString));
    if (sb.length() > 0)
    {
      sb.append(sb.indexOf("?") >= 0 ? "&" : "?");
    }

    sb.append(NetworkHelper.getQueryString(paramKeys, paramValues));

    return getRequest().encodeURL(sb.toString());
  }

  /**
   * Gets the current request signature even if the request was a POST by
   * using getAllRequestParametersString.
   */
  public String getRequestSignatureFull()
  {
    return getRequest().getRequestURL() + getAllRequestParametersString();
  }

  /**
   * 1) Cache the text of the response for future use.
   * 2) Include JSP like normal.
   */
  public boolean cacheThenRender(String page)
  {
    return this.cacheThenRender(page, null);
  }

  /**
   * 1) Cache the text of the response for future use.
   * 2) Include JSP like normal.
   */
  public boolean cacheThenRender(String page, String[] expectedParams)
  {
    final String responseText = ((HttpRequest)getRequest()).renderToString(page);
    String key;
    if (expectedParams != null)
    {
      key = this.getAllRequestParametersKey(expectedParams);
    }
    else
    {
      key = this.getAllRequestParametersKey();
    }
    getApplication().getStore().setCachedResponse(key, responseText);
    getLog().log("Caching response for key: " + key + ", referrer: " + headers().referrer(), LogLevel.DEBUG);
    return this.respondWithText(responseText);
  }

  /**
   * Returns whether this request should force caching or not.
   */
  public boolean forceCaching()
  {
    boolean notCacheResponse = false;
    boolean forceCache = false;

    String value = query().get(PARAM_DO_NOT_USE_CACHED_RESPONSE);
    if (value != null)
    {
      notCacheResponse = Boolean.valueOf(value);
    }

    value = query().get(PARAM_FORCE_CACHING);
    if (value != null)
    {
      forceCache = Boolean.valueOf(value);
    }
    return notCacheResponse && forceCache;
  }

  /**
   * If we have a cached response for the parameters of this Context,
   * then just respond with that.
   */
  public boolean respondFromCache()
  {
    return this.respondFromCache(null);
  }

  /**
   * If we have a cached response for the parameters of this Context,
   * then just respond with that.
   */
  public boolean respondFromCache(String[] expectedParams)
  {
    String cachedResponse = getApplication().getStore().getCachedResponse(this.getAllRequestParametersKey(expectedParams));
    if (cachedResponse != null)
    {
      getLog().log("Using cached response.", LogLevel.DEBUG);
      return this.respondWithText(cachedResponse);
    }
    return false;
  }

  /**
   * Respond with the given text.
   */
  protected boolean respondWithText(String responseText)
  {
    if (responseText != null)
    {
      OutputStreamWriter osw = null;
      try
      {
        osw = new OutputStreamWriter(this.getOutputStream());
        osw.write(responseText);
        return true;
      }
      catch (Exception exc)
      {
        getLog().log("::respondWithText caught exception ", exc);
      }
      finally
      {
        if (osw != null)
        {
          try
          {
            osw.close();
          }
          catch (IOException exc)
          {
            // Do nothing.
          }
        }
      }
    }
    return false;
  }

  /**
   * Calls ServletResponse.encodeURL().  This is used for tracking sessions
   * when cookies are disabled.  Note that supporting sessions without cookies
   * is extremely challenging to get right.  Every single URL the user may
   * navigate to needs to embed the session identifier, as provided by this
   * method.
   *   <p>
   * Most Gemini web applications do not support sessions without cookies.
   *
   * @param originalURL the original un-encoded URL.
   */
  public String encodeURL(String originalURL)
  {
    return getRequest().encodeURL(originalURL);
  }

  /**
   * Gets the request headers as a List of Strings, suitable for display or
   * debugging purposes.
   *
   * @return List of request headers.
   */
  public List<String> getAllRequestHeadersForDisplay()
  {
    return this.getAllRequestHeadersForDisplay(" = ");
  }

  /**
   * Gets the request headers as a List of Strings, suitable for display or
   * debugging purposes.
   *
   * @param separator A string to separate the key and value.
   *
   * @return List of request headers.
   */
  public List<String> getAllRequestHeadersForDisplay(String separator)
  {
    final List<String> headerStrings = new ArrayList<>();
    final Enumeration<String> enumeration = getRequest().getHeaderNames();
    while (enumeration.hasMoreElements())
    {
      String name = enumeration.nextElement();
      headerStrings.add(name + separator + getRequest().getHeader(name));
    }
    return headerStrings;
  }

  /**
   * Gets all of the request parameters as a Map.
   */
  public Map<String,String> getAllRequestParameters()
  {
    final Map<String,String> map = new HashMap<>();
    final Enumeration<String> enumeration = getParameterNames();
    while (enumeration.hasMoreElements())
    {
      String name = enumeration.nextElement();
      map.put(name, getRequest().getParameter(name));
    }
    return map;
  }

  /**
   * Gets the request parameters as a List of Strings, suitable for display or
   * debugging purposes.
   *
   * @return List of request parameters.
   */
  public List<String> getAllRequestParametersForDisplay()
  {
    final List<String> parmStrings = new ArrayList<>();
    final Enumeration<String> enumeration = getParameterNames();
    while (enumeration.hasMoreElements())
    {
      String name = enumeration.nextElement();
      parmStrings.add(name + " = " + getRequest().getParameter(name));
    }
    return parmStrings;
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
    if(expectedParams == null)
    {
      for(Entry<String,String> e : params.entrySet())
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
        for(Entry<String,String> e : params.entrySet())
        {
          if(e.getKey().startsWith(s))
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
   * Gets all of the request parameters as a URL-type String.
   */
  public String getAllRequestParametersString()
  {
    final StringBuilder buffer = new StringBuilder();
    Enumeration<String> enumeration = getParameterNames();
    while (enumeration.hasMoreElements())
    {
      if (buffer.length() > 0)
      {
        buffer.append("&");
      }

      String name = enumeration.nextElement();
      buffer.append(name)
            .append("=")
            .append(NetworkHelper.encodeUrl(getRequest().getParameter(name)));
    }

    return buffer.toString();
  }

  /**
   * Gets the session attributes as a List of Strings, suitable for display or
   * debugging purposes.
   *
   * @return List of String representations of name-to-value pairs.
   */
  public List<String> getAllSessionAttributesForDisplay()
  {
    final List<String> attrStrings = new ArrayList<>();

    final Session currentSession = getSession(false);
    if (currentSession != null) // Nothing to do if there is no session yet.
    {
      final Enumeration<String> enumeration = currentSession.getAttributeNames();
      while (enumeration.hasMoreElements())
      {
        String name = enumeration.nextElement();
        attrStrings.add(name + " = " + currentSession.getAttribute(name));
      }
    }
    return attrStrings;
  }

  /**
   * Gets the referenced page name.  Set whenever includeJSP or forwardJSP is
   * called.  This can be used for debugging purposes to determine the name
   * of the JSP file.
   *
   * @return the filename, as a String, or "none" if no JSP has been invoked
   *         during this Context.
   */
  public String getReferencedRender()
  {
    //log.debug("ReferencedJSP = " + referencedJSP);
    if (this.referencedRender != null)
    {
      return this.referencedRender;
    }
    else
    {
      return "none";
    }
  }

  /**
   * Gets the request's context path.
   */
  public String getContextPath()
  {
    return ((HttpRequest)this.request).getContextPath();
  }

  @Override
  public Attachments files()
  {
    if (files == null)
    {
      files = new ResinAttachments(this);
    }
    return files;
  }
}
