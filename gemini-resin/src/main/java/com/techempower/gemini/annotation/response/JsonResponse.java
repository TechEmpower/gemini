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
package com.techempower.gemini.annotation.response;

import java.lang.annotation.*;
import java.lang.reflect.*;

import com.techempower.gemini.*;

/**
 * A response class that takes a returned object and sends it back as JSON.
 * A Map object will use GeminiHelper.sendJsonMap, any other object will use 
 * GeminiHelper.sendSingleJsonObject.
 * 
 * Currently there's not way to supply this class with a custom JavaScriptWriter,
 * so advanced rendering is not available, if you need to use a custom JavaScriptWriter
 * please do so manually in the handler method, but you may want to include the 
 * &#064;JSON annotation as documentation to others that your handler method should
 * be sending JSON.
 */
public class JsonResponse<D extends BasicDispatcher, C extends Context>
  implements HandlerResponse<D, C> 
{
 
  public JsonResponse(GeminiApplication application)
  {
    // Does nothing.
  }
  
  /**
   * Sends the returned object as JSON.
   */
  @Override
  public boolean sendResponse(Object handler, Method method,
      D dispatcher, C context, String command,
      Object returned, Annotation annotation) 
  {
    return GeminiHelper.sendJson(context, returned);
  }
}
