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
package com.techempower.gemini.simulation;

import java.io.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.session.*;

/**
 * A simulated web request.
 */
public abstract class SimRequest
    implements Request
{
  
  private final GeminiApplication     application;
  private final BasicInfrastructure   infrastructure;
  private String                      url;
  private final String                uri;
  private final String                requestSignature;
  private String                      queryString;
  private Hashtable<String, String>   parameters;
  private final List<SimCookie>       cookies             = new ArrayList<>();
  private String                      redirectURL;
  private boolean                     redirected          = false;
  private String                      renderedJSP;
  private String                      includedFile;

  /**
   * Constructs a new simulated web request with the given parameters.
   *
   * @param simulation the simulation
   * @param url the url of the request
   * @param parameters the query string parameters of the request
   * @param client the simulation client
   * @param application the application
   */
  public SimRequest(Simulation simulation, String url, 
      Map<String, String> parameters, SimClient client, 
      GeminiApplication application)
  {
    this.application     = application;
    this.infrastructure  = this.application.getInfrastructure();
    //this.simulation      = simulation;
    //this.clientID        = String.valueOf(client.getId()); 
    
    // the path can optionally look like /?cmd=login&username=admin
    // so we need to process the path in this case.
    if(parameters == null)
    {
      this.parameters = new Hashtable<>();
    }
    else
    {
      this.parameters = new Hashtable<>(parameters);
    }
    
    this.requestSignature = url;
    
    String[] urlSplit = url.split("\\?");
    if(urlSplit.length > 1)
    {
      this.queryString = urlSplit[1];
      String[] params = urlSplit[1].split("&");
      for (String param : params)
      {
        String[] keyValuePair = param.split("=");
        this.parameters.put(keyValuePair[0], keyValuePair[1]);
      }
    }
    
    this.uri = this.url = urlSplit[0];
  }
  
  @Override
  public void setCharacterEncoding(String encoding)
      throws UnsupportedEncodingException
  {
  }

  @Override
  public String getRequestCharacterEncoding()
  {
    return null;
  }

  @Override
  public Enumeration<String> getHeaderNames()
  {
    
    return null;
  }

  @Override
  public String getHeader(String name)
  {
    
    return null;
  }

  @Override
  public Enumeration<String> getParameterNames()
  {
    return this.parameters.keys();
  }

  @Override
  public String getParameter(String name)
  {
    return this.parameters.get(name);
  }
  
  /**
   * Sets the request parameter, or overrides the parameter if it already exists
   */
  @Override
  public void putParameter(String name, String value)
  {
    if(value != null)
    {
      this.parameters.put(name, value);
    }
  }

  @Override
  public void removeParameter(String name)
  {
    
  }

  @Override
  public void removeAllRequestValues()
  {
    
  }

  @Override
  public String[] getParameterValues(String name)
  {
    
    return null;
  }

  @Override
  public String encodeURL(String urlToEncode)
  {
    return urlToEncode;
  }

  @Override
  public void print(String text) throws IOException
  {
  }

  @Override
  public PrintWriter getWriter() throws IOException
  {
    
    return null;
  }

  @Override
  public String getRequestSignature()
  {
    return this.requestSignature;
  }

  @Override
  public String getRealPath(String path)
  {
    return path;
  }
  
  @Override
  public StringBuffer getRequestURL()
  {
    return new StringBuffer(this.url);
  }
  
  /**
   * Returns the request uri. this differs from the url in that
   * the url http://some-domain.com/admin/users would have a uri 
   * of admin/users
   * 
   * For sim requests, we assume the url and uri are identical
   */
  @Override
  public String getRequestURI()
  {
    return this.uri;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <C extends Cookie> C getCookie(String name)
  {
    for(SimCookie cookie : this.cookies)
    {
      if(cookie.getName().equals(name))
      {
        return (C)cookie;
      }
    }
    
    return null;
  }

  @Override
  public void setCookie(String name, String value, String domain,
      String path, int age, boolean secure)
  {
    SimCookie cookie = new SimCookie(name, value, path, age, secure);
    this.cookies.add(cookie);
  }

  @Override
  public void deleteCookie(String name, String path)
  {
    Iterator<SimCookie> iter = this.cookies.iterator();
    while(iter.hasNext())
    {
      SimCookie cookie = iter.next();
      if(cookie.getName().equals(name) && cookie.getPath().equals(path))
      {
        iter.remove();
        break;
      }
    }
  }

  @Override
  public String getClientId()
  {
    
    return null;
  }

  @Override
  public InputStream getInputStream()
  {
    return null;
  }

  @Override
  public boolean redirect(String redirectDestinationUrl)
  {
    this.redirectURL = redirectDestinationUrl;
    this.redirected  = true;
    return true;
  }

  @Override
  public boolean redirectPermanent(String redirectDestinationUrl)
  {
    
    return false;
  }

  @Override
  public void setResponseHeader(String headerName, String value)
  {
  }

  @Override
  public OutputStream getOutputStream() throws IOException
  {
    
    return null;
  }
  
  @Override
  public Infrastructure getInfrastructure()
  {
    return this.infrastructure;
  }

  @Override
  public void setContentType(String contentType)
  {
  }

  @Override
  public void setExpiration(int secondsFromNow)
  {
  }
  
  @Override
  public String getCurrentURI()
  {
    
    return null;
  }
  
  @Override
  public boolean isSecure()
  {
    
    return false;
  }
  
  @Override
  public boolean isCommitted()
  {
    
    return false;
  }
  
  @Override
  public String getQueryString()
  {
    return this.queryString;
  }
  
  // TODO: make this support multiple sessions
  @Override
  public Session getSession(boolean create)
  {
    return this.application.getSimSessionManager().getSession(this, create);
  }
  
  @Override
  public void setAttribute(String name, Object o)
  {
  }
  
  /**
   * Gets an attribute for this request
   */
  @Override
  public Object getAttribute(String name)
  {
    return null;
  }

  public String getRedirectURL()
  {
    return this.redirectURL;
  }
  
  public boolean redirected()
  {
    return this.redirected;
  }

  public String getRenderedJSP()
  {
    return this.renderedJSP;
  }

  public String getIncludedFile()
  {
    return this.includedFile;
  }
  
  @Override
  public void setStatus(int status)
  {
    
  }

  @Override
  public String getRequestContentType()
  {
    return "demotext";
  }

  @Override
  public boolean isPost()
  {
    return false;
  }

  @Override
  public boolean isHead()
  {
    return false;
  }

  @Override
  public boolean isGet()
  {
    return false;
  }

  @Override
  public boolean isPut()
  {
    return false;
  }

  @Override
  public boolean isDelete()
  {
    return false;
  }

  @Override
  public boolean isTrace()
  {
    return false;
  }

  @Override
  public boolean isOptions()
  {
    return false;
  }

  @Override
  public boolean isConnect()
  {
    return false;
  }

  @Override
  public boolean isPatch()
  {
    return false;
  }
}
