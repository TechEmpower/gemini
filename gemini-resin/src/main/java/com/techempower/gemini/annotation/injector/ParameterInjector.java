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
package com.techempower.gemini.annotation.injector;

import java.lang.annotation.*;

import com.techempower.gemini.*;

/**
 * An interface for describing how to inject an object into a handler method.
 * 
 * This class is typed for Dispatcher and Context, but not for Annotation, this was done 
 * because annotations cannot subclass themselves, which would make it difficult to 
 * create a custom annotation by extending classes of type ParameterInjector.
 */
public interface ParameterInjector<D extends BasicDispatcher, C extends Context> 
{
  /**
   * Extracts an argument from a web request, given an annotation that can
   * determine the parameter's name and default value.
   *
   * @param dispatcher the request dispatcher
   * @param context the current context
   * @param annotation the annotation that provides the parameter's name and
   *                   default value
   * @param c the type of the result to be returned
   * @return the request parameter value, as determined by the annotation and
   *         class
   */
  public Object getArgument(D dispatcher, C context, Annotation annotation, Class<?> c);
}
