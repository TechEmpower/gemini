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
package com.techempower.gemini.context;

import java.util.*;

import com.techempower.gemini.*;

/**
 * A Resin/Servlet specific implementation of the context Attachments
 * interface.
 */
public class ResinAttachments implements Attachments {

  private final Request request;
  private List<RequestFile> list;
  
  /**
   * Constructor.
   */
  public ResinAttachments(BasicContext context)
  {
    this.request = context.getRequest();
  }
  
  /**
   * Returns true if there any any files.
   */
  @Override
  public boolean any()
  {
    return (list().size() > 0);
  }

  /**
   * Returns the file uploaded in this request with the given parameter name.
   * If no file was uploaded with the given parameter name, this method returns
   * {@code null}.
   *
   * @see com.techempower.gemini.ResinRequestFile#get(Request, String)
   */
  @Override
  public RequestFile get(String name)
  {
    return ResinRequestFile.get(request, name);
  }

  /**
   * Returns all the files uploaded in this request.  If no files were uploaded,
   * this method returns an empty collection.
   *
   * @see com.techempower.gemini.ResinRequestFile#get(Request)
   */
  @Override
  public List<RequestFile> list()
  {
    if (list == null)
    {
      list = ResinRequestFile.get(request);
    }
    return list;
  }

}
