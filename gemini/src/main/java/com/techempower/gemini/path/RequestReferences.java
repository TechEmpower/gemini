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

import com.techempower.gemini.*;

/**
 * Provides ThreadLocal access to key references that exist for the lifetime
 * of a request.
 */
public class RequestReferences
{
  
  private final static ThreadLocal<RequestReferences> MAP = new ThreadLocal<>();

  /**
   * The current request context.
   */
  public final Context context;
  
  /**
   * The request URI segments.
   */
  public final PathSegments segments;
  
  /**
   * The specified template, which is optional.  Typical behavior is for
   * rendering to use the defaultTemplate if template is null.
   */
  public String template;
  
  /**
   * A default template to use if template is null.  This is typically not
   * set by application code, but rather by the framework itself.
   */
  public String defaultTemplate;
  
  /**
   * Gets the template, using the defaultTemplate if the specific template is
   * null.  Note that defaultTemplate may also be null, so it is possible for
   * this method to return null.
   */
  public String getTemplate()
  {
    return (template != null ? template : defaultTemplate);
  }
  
  private RequestReferences(Context context, PathSegments segments)
  {
    this.context = context;
    this.segments = segments;
  }
  
  /**
   * Gets the current thread's references.
   */
  public static RequestReferences get()
  {
    return MAP.get();
  }

  /**
   * Sets the current thread's references.
   */
  public static RequestReferences set(Context context, PathSegments segments)
  {
    final RequestReferences refs = new RequestReferences(context, segments); 
    MAP.set(refs);
    return refs;
  }
  
  /**
   * Clears the current thread's references.
   */
  public static void remove()
  {
    MAP.remove();
  }
  
}
