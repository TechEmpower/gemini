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
package com.techempower.gemini.annotation.intercept;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import com.techempower.gemini.*;

/**
 * POSTInterceptor is an implementation of RequirementInterceptor that checks
 * to see if the request was a POST.
 * 
 * If the request is not a POST a 405 response code is added to the response 
 * header and an JSON error is returned of the following type:
 * <pre> {
 *   "error" : "Method not allowed",
 *   "errorDescription" : "POST is expected."
 * }</pre>
 */
public class PostIntercept<D extends BasicDispatcher, C extends BasicContext>
    implements HandlerIntercept<D, C>
{

  /**
   * Checks to see if the user is logged in.
   */
  @Override
  public boolean intercept(Method m, D dispatcher, C context, String command,
      Annotation annotation)
  {
    if (!context.isPost())
    {
      // It's not a POST so intercept it!
      context.setStatus(405);
      context.headers().put("Allow", "POST");
      return true;
    }

    // It is a POST so don't intercept it
    return false;
  }

  @Override
  public boolean handleIntercept(Method m, D dispatcher, C context,
      String command, Annotation annotation)
  {
    // By here, the call to context.isPostRequest(true) has already set the
    // response code to 405 and added the "Allow: POST" header.
    Map<String, Object> error = new HashMap<>();
    error.put("error", "Method not allowed");
    error.put("errorDescription", "POST is expected.");
    return GeminiHelper.sendJson(context, error);
  }

}
