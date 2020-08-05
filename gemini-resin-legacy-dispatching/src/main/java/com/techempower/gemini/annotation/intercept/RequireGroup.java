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
 * An Intercept annotations that checks for the required groups, the annotation takes a 
 * list of group ids to check against. If the user is not in a group,
 * then the user will be redirected to the jsp specified. By default this is unauthorized.jsp.
 * 
 * This annotation should be used with &#064;RequireLogin, although future versions of this annotation
 * may inherit the RequireLogin functionality, that is not the case as of this writing.
 * 
 * <pre>
 * &#064;CMD("foo")
 * &#064;RequireGroup(anyOf={1, 2})
 * public boolean handleFoo(MyContext context)
 * {
 *   // this method requires users to be in group 1 and 2 
 * }
 * </pre>
 */
@Inherited
@Intercept(GroupIntercept.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireGroup 
{
  /**
   * Specifies that only a user mapped to <b>all of the groups</b> listed will 
   * meet the group requirement. 
   */
  int[] allOf() default {};
  /**
   * Specifies that a user mapped to <b>any of the groups</b> listed will meet 
   * the group requirement. 
   */
  int[] anyOf() default {};
  /**
   * Specifies the path to the jsp to render in the case where the group 
   * requirement is not met.<br>
   * <br>
   * By default, "unauthorized.jsp" (relative to the JSP Document Root) will be
   * returned.
   */
  String jsp() default "unauthorized.jsp";
  /**
   * Specifies an optional message string to be passed as a delivery to the
   * rendered jsp.<br>
   * <br>
   * Supplying a message is tantamount to the following:<br>
   * <pre>context.putDelivery("Message", msg())</pre>
   */
  String msg() default "";
}
