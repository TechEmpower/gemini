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


/**
 * A Response annotation used to state what JSP should be included
 * after successful completion of a handler method. A string can be 
 * provided to the Annotation to explicitly state which jsp should be included,
 * or it can be left blank to signal that the default jsp location should be used,
 * which is {classname}/{command}.jsp (classname will have "Handler" removed from 
 * it's name, and both classname and command will be lowercase.
 * 
 *  public class FooHandler
 *  { 
 *    // The JSP view-foo.jsp will be included. 
 *    &#064;CMD("foo")
 *    &#064;JSP("view-foo.jsp")
 *    public boolean viewFoo()
 *    {
 *      return true;
 *    }
 *    
 *    // The JSP foo/edit-foo.jsp will be included. 
 *    &#064;CMD("edit-foo")
 *    &#064;JSP()
 *    public boolean editFoo()
 *    {
 *      return true;
 *    }
 *  }
 */
@Response(JspResponse.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JSP 
{
  /**
   * Returns the name of the JSP to be rendered upon the completion of this
   * handler method.
   *
   * @return the name of the JSP to be rendered upon the completion of this
   *         handler method
   */
  String value() default "";
}
