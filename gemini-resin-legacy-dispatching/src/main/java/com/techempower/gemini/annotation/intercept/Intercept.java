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

/**
 * Intercept is a meta annotation that allows a developer to specify their
 * custom annotation as being an Intercept, the dispatcher can then recognize
 * the custom annotation. 
 * 
 * Intercepts are used to intercept a request before it gets to the handler method. 
 * These can be used to force requirements upon commands, such as requiring the user 
 * to be logged in, or forcing the user to be a part of specific groups.
 * <pre>
 *  &#064;Intercept(CustomIntercept.class)
 *  // tells the compiler that this annotation can be used at both the class level and 
 *  method level
 *  &#064;Target({ElementType.TYPE, ElementType.METHOD)
 *  // Allows the annotation to persist between subclasses, useful for 
 *  // using &#064;RequireLogin on a parent class and then all subclasses
 *  // automatically require login as well
 *  &#064;Inherited
 *  &#064;Retention(RetentionPolicy.RUNTIME)
 *  public &#064;interface MyIntercept 
 *  {
 *    String value();
 *  }
 * </pre>
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Intercept 
{
  /**
   * Returns the class of the custom intercept to be applied to the method.
   *
   * @return the class of the custom intercept to be applied to the method
   */
  @SuppressWarnings("rawtypes")
  Class<? extends HandlerIntercept> value();
}
