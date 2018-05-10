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

package com.techempower.gemini.jsp;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.gemini.*;

/**
 * The StringServletOutputStream passes its output to the
 * ByteArrayOutputStream provided to the constructor. This can be used
 * to get the output of a JSP as a string.
 *
 * <b>NOTE: Under Tomcat, you won't get a value unless your JSP calls
 * <pre>out.flush()</pre> at the end.</b>
 */
public class StringServletOutputStream
  extends    RedirectedServletOutputStream
{

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public StringServletOutputStream(ByteArrayOutputStream baos)
  {
    super(baos);
  }

  /**
   * Invokes a Servlet (or JSP) with a StringServletOutputStream, thereby
   * preventing any output from being seen.  This can be used to get the
   * output of a JSP without rendering it to the client.
   */
  public static String invokeToString(String toInvoke, HttpRequest context)
  {
    try
    {
      final HttpServletRequest request = context.getRawRequest();
      final HttpServletResponse response = context.getRawResponse();

      final RequestDispatcher rd = request.getRequestDispatcher(
        context.getInfrastructure().getJspDirectory() + toInvoke);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ServletOutputStream out = new StringServletOutputStream(baos);
      final HttpServletResponseWrapper responseWrapper = new RedirectedResponseWrapper(response, out);

      rd.include(request, responseWrapper);
      responseWrapper.getWriter().flush();
      responseWrapper.getWriter().close();

      return baos.toString();
    }
    catch (Exception | Error e)
    {
      // Do nothing.
    }
    
    return "";
  }

}   // End StringServletOutputStream.
