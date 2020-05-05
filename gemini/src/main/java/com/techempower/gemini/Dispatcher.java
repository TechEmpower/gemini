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

/**
 * Defines only the bare minimum methods required by a Dispatcher from the
 * point of view of the application.  How the implementation then works with
 * its Handlers (or whatever dependent objects it has) is not the concern
 * of this interface.
 *   <p>
 * A single Dispatcher instance is created by the application and then asked
 * to dispatch each HTTP request Context to something that can handle the
 * request, ultimately resulting in a response being provided.
 */
public interface Dispatcher
{
  /**
   * Returns all the registered routes associated with this dispatcher.
   * By default, returns an empty String[].
   */
  default String[] getRoutes() {
    return new String[]{};
  }

  /**
   * Dispatch a request (represented as a Context) and ensure that it is 
   * fully handled in some fashion.  The application will not take any
   * corrective measures to handle it in a failsafe fashion if the Dispatcher
   * fails; so the Dispatcher is required to have its own failsafe mechanisms.
   * However, the method returns a boolean indicating success or failure for 
   * legacy reasons.  Implementations may always return true.   
   */
  boolean dispatch(Context context);
  
  /**
   * Called by the application to notify the Dispatcher that the application
   * is done with this request.  Implementations may ignore this or use this
   * to close dependent resources or to take a performance metrics snapshot.
   */
  void dispatchComplete(Context context);
  
  /**
   * Called by the Context or any other agent responsible for rendering a
   * response to indicate that a render operation is starting.  As with
   * dispatchComplete, implementations may elect to ignore this and simply
   * return immediately.  But it may also be used to capture performance 
   * metrics.
   * 
   * @param renderingName Is any name used to identify the rendering, which
   *        would traditionally be the name of a JSP file.
   */
  void renderStarting(Context context, String renderingName);
  
  /**
   * Called by the Context or any other agent responsible for rendering a
   * response to indicate that the render is complete.  Again, implementations
   * may elect to ignore this and simply return immediately.  But it may also
   * be used to capture performance metrics.
   */
  void renderComplete(Context context);
  
  /**
   * Dispatch an Exception encountered either as part of processing a request
   * or elsewhere.  The Exception is typically dispatched to a list of
   * ExceptionHandlers, as done by BasicDispatcher, but that is not strictly 
   * required.
   *   <p>
   * The exception should not be null.
   *   <p>
   * If the context is null, the Dispatcher and anything that aids in the
   * handling process such as ExceptionHandlers should still do any logging or 
   * other processing even though no HTTP response is expected.  
   * 
   * @param context The request context may be null; indicating that no 
   *   response is required (the exception may still be logged).
   * @param description The description may be null.
   */
  void dispatchException(Context context, Throwable exception, 
      String description);
  
}
