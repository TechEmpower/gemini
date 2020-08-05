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
package com.techempower.gemini.path.annotation;

import java.lang.annotation.*;

/**
 * Path annotation.  Identifies a method within a PathHandler to
 * associate with a specified URI.  A method annotated as a 
 * PathSegment may accept argments if there is a variable capture
 * present in the passed value (example: "foo/{variable}" allows 
 * the annotated method to have 1 parameter of simple type such
 * as int, string, etc. Any number of variables are supported
 * (example: "foo/{var1}/{var2}") and while the variable names are
 * not used at all (technically, "foo/{}/{}" is the same as the
 * previous example) they should be considered mandatory for the
 * purposes of good self-document code (example, "foo/{userId}"
 * is much more meaningful than "foo/{}" and the former should
 * be favored).
 *   <p>
 * URIs are routed directly to a method annotated with @Path with
 * specificity trumping. That is, @Path("foo/bar") will handle
 * GET "foo/bar" before @Path("foo/{var}") and before @Path("foo/*"). 
 *   <p>
 * The method identified by this annotation will accept GET HTTP methods by
 * default, but can accept additional http request method type annotations
 * to specify which the annotated method supports (e.g. @Get or @Put).
 *   <p>
 * {@code @Path} with no arguments is synonymous with {@code @Path("")} and will handle
 * the root URI of the handler. Example /api/users =&gt; UserHandler; {@code @Path}
 * will handle `GET /api/users`.
 */
// TODO: Roll back the addition of ElementType.TYPE. Only added for testing
//  kain's stuff.
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Path
{
  // Path uri to associate.
  String value() default "";
}
