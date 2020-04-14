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

package com.techempower.gemini.exceptionhandler;

import com.techempower.gemini.*;

/**
 * The definition of an ExceptionHandler, which can be added to a 
 * BasicDispatcher for the purposes of handling Java Exceptions in various 
 * ways.
 *   <p>
 * When the BasicDispatcher asks each ExceptionHandler to handle an Exception,
 * the ExceptionHandler will need to know whether or not to provide a response
 * via the HTTP request Context.
 *   <p>
 * The Context may be null, in which case a response is definitely not 
 * expected.
 *   <p>
 * If multiple ExceptionHandlers are configured within the same application
 * instance, it is the responsibility of the application to have each
 * ExceptionHandler configured to know whether a response should be provided.
 */
public interface ExceptionHandler
{
  
  /**
   * Process an Exception.
   */
  void handleException(BasicContext context, Throwable exc);
  
  /**
   * Process an Exception with a descriptive String.
   */
  void handleException(BasicContext context, Throwable exc, String description);

}
