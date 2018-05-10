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

import javax.servlet.*;

/**
 * A wrapper around a ServletConfig and a ServletContext.
 *
 * @see InitConfig
 * @see ServletConfig
 * @see ServletContext
 */
public class HttpInitConfig
  implements InitConfig
{
  private final ServletConfig config;
  private final ServletContext context;

  /**
   * Creates a new HTTP init config based on the given servlet config and
   * context.
   *
   * @param config the servlet config
   * @param context the servlet context
   */
  private HttpInitConfig(ServletConfig config, ServletContext context)
  {
    this.config = config;
    this.context = context;
  }

  /**
   * Returns a new HTTP init config based on the given servlet config and
   * context.
   *
   * @param config the servlet config
   * @param context the servlet context
   * @return a new HTTP init config based on the given servlet config and
   *         context
   */
  public static HttpInitConfig createHttpInitConfig(ServletConfig config, 
      ServletContext context)
  {
    return new HttpInitConfig(config, context);
  }

  @Override
  public String getInitParameter(String name)
  {
    if (this.config == null)
    {
      return null;
    }
    return this.config.getInitParameter(name);
  }

  @Override
  public InputStream getResourceAsStream(String filename)
  {
    if (this.config == null)
    {
      return null;
    }
    if (this.config.getServletContext() != null)
    {
      return this.config.getServletContext().getResourceAsStream(filename);
    }
    
    return null;
  }

  @Override
  public Enumeration<String> getInitParameterNames()
  {
    if (this.config == null)
    {
      return null;
    }
    return this.config.getInitParameterNames();
  }

  @Override
  public Object getAttribute(String name)
  {
    if (this.config == null)
    {
      return null;
    }
    return this.config.getServletContext().getAttribute(name);
  }

  @Override
  public Enumeration<String> getAttributeNames()
  {
    if (this.config == null)
    {
      return null;
    }
    return this.config.getServletContext().getAttributeNames();
  }

  @Override
  public String getRealPath(String path)
  {
    if (this.config == null)
    {
      return null;
    }
    return this.config.getServletContext().getRealPath(path);
  }

  @Override
  public String getMimeType(String file)
  {
    if (this.config == null)
    {
      return null;
    }
    return this.config.getServletContext().getMimeType(file);
  }

  @Override
  public String getServerInfo()
  {
    if (this.config == null)
    {
      return null;
    }
    return this.config.getServletContext().getServerInfo();
  }

  @Override
  public <T extends EventListener> void addListener(T listener)
  {
    if (this.context != null)
    {
      this.context.addListener(listener);
    }
  }
  
}  // End HttpInitConfig.