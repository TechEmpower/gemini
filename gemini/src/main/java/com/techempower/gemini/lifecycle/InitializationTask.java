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

package com.techempower.gemini.lifecycle;

import com.techempower.gemini.*;

/**
 * An initialization task is anything that needs to be initialized before
 * and application can be considered "ready" to start processing requests.
 * This is a fairly simple interface that allows the application to ask the 
 * Task to initialize.
 *   <p>   
 * An InitializationTask implementation may also implement Asynchronous.  The
 * difference being that the application will call Asynchronous.begin() after
 * the application has started.  By also implementing Asynchronous, however,
 * an implementation class can also be notified when the application is
 * shutting down.
 */
public interface InitializationTask
{

  /**
   * Initialize this component as part of the application's initialization
   * process.  Optionally throws an unchecked exception (typically a
   * GeminiInitializationError) if the task is unable to complete.  See
   * additional details in comments above.
   *   <p>
   * If an Exception is thrown during taskInitialize, the application will
   * delay for some time and then re-try calling taskInitialize.  This may
   * occur multiple times until the application has made a configurable number
   * of attempts.  If all attempts fail, the application will consider its
   * startup process to have failed.
   * 
   * @param application A reference to the application.
   */
  void taskInitialize(GeminiApplication application);
  
}
