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
 * The FileServletOutputStream passes its output to a FileOutputStream
 * created given the filename provided to the constructor.  This can be used,
 * for example, to save the rendering of a JSP to an HTML file on the server
 * side.
 */
public class FileServletOutputStream
  extends    RedirectedServletOutputStream
{
  //
  // Member variables.
  //
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public FileServletOutputStream(String filename)
      throws IOException
  {
    super(new FileOutputStream(filename));
  }
  
  /**
   * Writes a provided JSP file to a file, using the Infrastructure's JSP
   * directory to prefix the filename.
   * 
   * @param toInvoke the name of the JSP file, which will be automatically
   *        prefixed with the Infrastructure's JSP directory. 
   * @param filename the output filename.
   * @param context the Context object associated with the request. 
   */
  public static void writeJspToFileSimple(String toInvoke, final String filename,
      Context context)
  {
    try
    {
      HttpRequest resinRequest = (HttpRequest)context.getRequest(); // safe - this is resin
      final HttpServletRequest request = resinRequest.getRawRequest();
      final HttpServletResponse response = resinRequest.getRawResponse();
      final RequestDispatcher rd = request.getRequestDispatcher(
          context.getInfrastructure().getJspDirectory() + toInvoke);
      final HttpServletResponseWrapper responseWrapper = 
          new FileHttpServletResponseWrapper(response, filename);

      rd.include(request, responseWrapper);
    }
    catch (Exception | Error e)
    {
      // Do nothing.
    }
  }

  /**
   * Writes a provided JSP file to a file.
   * 
   * @param toInvoke the name of the JSP file.  Note that this filename must
   *        be fully specified.  Use writeJspToFileSimple to write out JSP
   *        files located in your web-app's JSP directory. 
   * @param filename the output filename.
   * @param context the Context object associated with the request. 
   */
  public static void writeJspToFile(String toInvoke, final String filename,
      Context context)
  {
    try
    {
      HttpRequest resinRequest = (HttpRequest)context.getRequest(); // safe - this is resin
      final HttpServletRequest request = resinRequest.getRawRequest();
      final HttpServletResponse response = resinRequest.getRawResponse();
      final RequestDispatcher rd = request.getRequestDispatcher(toInvoke);
      final HttpServletResponseWrapper responseWrapper = 
          new FileHttpServletResponseWrapper(response, filename);

      rd.include(request, responseWrapper);
    }
    catch (Exception | Error e)
    {
      // Do nothing.
    }
  }

  /**
   * Wraps the ServletResponse with a custom version that makes an instance
   * of FileServletOutputStream.
   */
  static class FileHttpServletResponseWrapper
      extends HttpServletResponseWrapper
  {
    private final String wrapperFilename;
    private final FileServletOutputStream fsos;
    
    public FileHttpServletResponseWrapper(HttpServletResponse response,
      String filename)
      throws IOException
    {
      super(response);
      this.wrapperFilename = filename;
      this.fsos = new FileServletOutputStream(this.wrapperFilename);
    }

    @Override
    public ServletOutputStream getOutputStream()
    {
      return this.fsos;
    }

    @Override
    public PrintWriter getWriter()
    {
      return this.fsos.getWriter();
    }
  }
  
}   // End FileServletOutputStream.
