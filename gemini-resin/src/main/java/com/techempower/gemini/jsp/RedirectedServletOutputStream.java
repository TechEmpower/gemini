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

/**
 * The RedirectedServletOutputStream passes its output to a separate output
 * stream, typically a FileOutputStream.  See FileServletOutputStream for
 * a simple subclass for writing out a file that can be used, for example,
 * to save the rendering of a JSP to an HTML file on the server side.
 * 
 * @see FileServletOutputStream
 */
public class RedirectedServletOutputStream
  extends    ServletOutputStream
{
  //
  // Member variables.
  //
  
  private OutputStream destination;
  private PrintWriter  writer;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public RedirectedServletOutputStream(OutputStream destination)
  {
    this.destination = destination;
    this.writer = new PrintWriter(destination);
  }
  
  /**
   * Gets the writer associated with this Output stream.
   */
  public PrintWriter getWriter()
  {
    return this.writer;
  }

  /**
   * @see javax.servlet.ServletOutputStream#print(boolean)
   */
  @Override
  public void print(boolean arg0) throws IOException
  {
    this.writer.print(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#print(char)
   */
  @Override
  public void print(char arg0) throws IOException
  {
    this.writer.print(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#print(double)
   */
  @Override
  public void print(double arg0) throws IOException
  {
    this.writer.print(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#print(float)
   */
  @Override
  public void print(float arg0) throws IOException
  {
    this.writer.print(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#print(int)
   */
  @Override
  public void print(int arg0) throws IOException
  {
    this.writer.print(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#print(long)
   */
  @Override
  public void print(long arg0) throws IOException
  {
    this.writer.print(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#print(String)
   */
  @Override
  public void print(String arg0) throws IOException
  {
    this.writer.print(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#println()
   */
  @Override
  public void println() throws IOException
  {
    this.writer.println();
  }

  /**
   * @see javax.servlet.ServletOutputStream#println(boolean)
   */
  @Override
  public void println(boolean arg0) throws IOException
  {
    this.writer.println(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#println(char)
   */
  @Override
  public void println(char arg0) throws IOException
  {
    this.writer.println(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#println(double)
   */
  @Override
  public void println(double arg0) throws IOException
  {
    this.writer.println(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#println(float)
   */
  @Override
  public void println(float arg0) throws IOException
  {
    this.writer.println(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#println(int)
   */
  @Override
  public void println(int arg0) throws IOException
  {
    this.writer.println(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#println(long)
   */
  @Override
  public void println(long arg0) throws IOException
  {
    this.writer.println(arg0);
  }

  /**
   * @see javax.servlet.ServletOutputStream#println(String)
   */
  @Override
  public void println(String arg0) throws IOException
  {
    this.writer.println(arg0);
  }

  /**
   * @see java.io.OutputStream#write(byte[], int, int)
   */
  @Override
  public void write(byte[] b, int off, int len) throws IOException
  {
    this.writer.flush();
    this.destination.write(b, off, len);
  }

  /**
   * @see java.io.OutputStream#write(byte[])
   */
  @Override
  public void write(byte[] b) throws IOException
  {
    this.writer.flush();
    this.destination.write(b);
  }

  /**
   * @see java.io.OutputStream#write(int)
   */
  @Override
  public void write(int b) throws IOException
  {
    this.writer.flush();
    this.destination.write(b);
  }

  /**
   * @see java.io.OutputStream#close()
   */
  @Override
  public void close() throws IOException
  {
    this.writer.close();
    this.destination.close();
  }

  /**
   * @see java.io.OutputStream#flush()
   */
  @Override
  public void flush() throws IOException
  {
    this.writer.flush();
    this.destination.flush();
  }

}   // End RedirectedServletOutputStream.
