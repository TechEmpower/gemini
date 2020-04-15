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
 * An interface implemented by chunks of functionality that are available
 * at a root-level path segment, such as /tasks or /users.  Conceptually
 * similar to the original Handler interface from legacy versions of Gemini.
 */
public interface PathHandler<C extends Context>
{

  /**
   * Execute any preliminary logic prior to handling a request.  This method
   * is called by the PathDispatcher prior to calling the handle method.  It
   * is not typically implemented by concrete classes but rather by abstract
   * classes situated between this interface and a concrete class.
   *   <p>
   * The return value indicates whether the request has been handled, so
   * returning false is normal because a request is not normally handled
   * by the prehandle method.  However, in some cases, the prehandle method
   * is responsible for handling a request fully (e.g., by redirecting the
   * user elsewhere), in which case returning true will indicate that the
   * request should not be dispatched to the handle method.
   * 
   * @return false normally; but return true if the request has been processed
   * by the prehandle method and should <b>not</b> be dispatched to the
   * handle method.
   */
  boolean prehandle(PathSegments segments, C context);
  
  /**
   * Handle the request.
   * 
   * @return true if the request was handled successfully (a response was
   * provided) and false otherwise; a false will effectively ask the 
   * Dispatcher's default handler to handle the request. 
   */
  boolean handle(PathSegments segments, C context);
  
  /**
   * Execute any logic after handling a request.  This is optional (that is,
   * the implementation may be empty).  It may be used to, for example, clean
   * up ThreadLocal references.
   */
  void posthandle(PathSegments segments, C context);
  
}
