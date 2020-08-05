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

/**
 * The Param annotation is used to specify how to look up the parameters value from the 
 * request. Most primitive types, String and File are allowed, other types will always return 
 * null (or the primitive default if the type is a primitive)
 * 
 * Supports default values for String types only. For default values of other type, use the type specific 
 * &#064;Param annotation, such as &#064;IntParam
 * <pre>
 *  // example of using &#064;Param for both an int and String parameters.
 *  &#064;CMD("foo")
 *  public boolean handleFoo(&#064;Param("fooID") int fooID, &#064;Param(value="type", defaultValue="foo") String foo)
 *  {
 *
 *  }
 * </pre>
 */
@Injector(ParamInjector.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Param 
{
  /**
   * Returns the name of the request parameter corresponding to this
   * {@code Param}.
   *
   * @return the name of the request parameter
   */
  String value();

  /**
   * Returns the default value of this {@code Param}, in case no value was
   * provided in the request.  If not defined, this defaults to the empty
   * string.
   *
   * @return the default value of this {@code Param}
   */
  String defaultValue() default "";
}
