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
import java.lang.reflect.*;

import com.techempower.gemini.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Response class that tosses a file. This is used with the &#064;File annotation
 * to optionally set the content type, and whether or not it should be an attachment. 
 * 
 *  &#064;CMD("get-image")
 *  &#064;File
 *  public File getImage(Context context, &#064;Param("id") int id)
 *  {
 *    File image = getImage(id);
 *    return image;
 *  }
 *  
 *  &#064;CMD("get-image")
 *  &#064;File(content-type="image/jpeg")
 *  public String getImage(Context context, &#064;Param("id") int id)
 *  {
 *    String location = getImage(id);
 *    return location;
 *  }
 */
public class FileResponse<D extends BasicDispatcher, C extends LegacyContext> 
  implements HandlerResponse<D, C>
{
  private Logger log = LoggerFactory.getLogger(getClass());
  
  public FileResponse()
  {
  }

  @Deprecated(forRemoval = true)
  public FileResponse(GeminiApplication application)
  {
  }
  
  /**
   * Sends a file as the response. The returned object from the handler method must either be a File, 
   * or a String containing the path to a file. If the returned object is something else, this method
   * will return false.
   * 
   */
  @Override
  public boolean sendResponse(Object handler, Method method,
      D dispatcher, C context, String command,
      Object returned, Annotation annotation) 
  {
    String  filename;
    boolean asAttachment = ((TossFile)annotation).asAttachment();
    String  contentType  = ((TossFile)annotation).contentType();
      
    if(returned instanceof java.io.File)
    {
      filename = ((java.io.File)returned).getAbsolutePath();
    }
    else if(returned instanceof String)
    {
      filename = (String)returned;
    }
    else
    {
      this.log.info("Returned object not of type File or String, can't include File.");
      return false;
    }
    
    return context.includeFile(filename, asAttachment, contentType);
  }

}
