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

/**
 * A simple HttpServletResponseWrapper that allows response output to be
 * directed to an arbitrary OutputStream.
 */
public class RedirectedResponseWrapper 
     extends HttpServletResponseWrapper
{
  private ServletOutputStream outputStream;
  private PrintWriter writer;

  public RedirectedResponseWrapper(HttpServletResponse response, 
      ServletOutputStream outputStream)
  {
    super(response);
    this.outputStream = outputStream;
    this.writer = null;
  }

  @Override
  public ServletOutputStream getOutputStream()
  {
    return this.outputStream;
  }

  @Override
  public PrintWriter getWriter()
  {
    if (this.writer == null)
    {
      this.writer = new PrintWriter(getOutputStream());
    }

    return this.writer;
  }

  @Override
  public void flushBuffer()
  {
    if (this.writer != null)
    {
      try
      {
        this.writer.flush();
      }
      catch (Exception e)
      {
        // DO NOTHING
      }
    }
  }
}
