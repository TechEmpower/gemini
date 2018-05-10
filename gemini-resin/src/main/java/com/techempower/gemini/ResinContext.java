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

import com.techempower.gemini.context.*;

/**
 * A wrapper for a Request. While in most cases, a Request represents an
 * HTTP request, and therefore a Context is a wrapper over an HTTP request,
 * that is not always the case. Requests may arrive via other means, such as
 * via a simulation or via some other transport.
 *   <p> 
 * A Context instance is an interface to these items:
 * <ul>
 *   <li> Request Parameters, which are in the form of a key-value map.
 *   <li> Session Attributes, which are also in the form of a key-value map.
 *   <li> Cookie Handling (Basically any information that has been saved to a
 *        client, and is provided to the application on each request; when
 *        the request is an HTTP request, this represents conventional HTTP
 *        Cookies).
 *   <li> Functionality to render a response to the client.
 * </ul>
 * Wrapping all of these into a single object makes passing a reference
 * between simpler than alternatives (such as HttpServletResponse and
 * HttpServletRequest) and less rigidly coupled to a specific request
 * implementation.
 */
public class ResinContext extends Context {
  
  /**
   * Standard constructor.
   *
   * @param request the Request object received by the servlet.
   * @param application The Gemini Application that is creating this Context.
   */
  public ResinContext(Request request, GeminiApplication application)
  {
    super(application, request);
  }

  /**
   * Gets the request's context path.
   */
  public String getContextPath()
  {
    return ((HttpRequest)this.request).getContextPath();
  }
  
  /**
   * Get an interface for working with attached files.
   */
  @Override
  public Attachments files()
  {
    if (files == null)
    {
      files = new ResinAttachments(this);
    }
    return files;
  }

}   // End Context
