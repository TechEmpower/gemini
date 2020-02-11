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

import com.techempower.collection.*;
import com.techempower.gemini.*;
import com.techempower.gemini.context.*;
import com.techempower.gemini.manager.*;

/**
 * An intermediate subclass of BasicManager that provides utility 
 * functionality that mirrors that found in Path Handlers.
 *   <p>
 * This implementation requires the use of the PathDispatcher and should not
 * be used with "classic" Gemini applications that are not using 
 * PathDispatcher.  However, aside from that, this class is not related to 
 * URI Paths.
 *   <p>
 * Some of the utility functions provided by this class may appear to break
 * with the conceptual purity of Managers such as keeping request context 
 * separate from business logic.  However, the methods have been provided in 
 * case they prove expedient to development.  Many application architects are 
 * not as concerned with strict separation of concerns.  If this bothers you, 
 * you can either (a) avoid using the utility functions or (b) not use this as
 * a super-class to your application's manager classes.  No core components of
 * Gemini require that your application uses Managers at all.
 */
public class BasicPathManager<A extends GeminiApplication, C extends Context>
  extends BasicManager<A>
{
  
  /**
   * Constructor.
   */
  public BasicPathManager(A application)
  {
    super(application);
  }
  
  /**
   * Gets a reference to the current request Context.
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

}
