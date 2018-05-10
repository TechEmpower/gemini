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
 * A response annotation that will toss the returned file along with the response.
 * The return type of your handler method must either be a File or a string containing the
 * path of the file you want to toss. 
 * 
 * You may specify whether this should be an attachment (this defaults to true) and what 
 * the content type should be, if you don't specify the content type, an attempt will be
 * made to figure it out from the file itself.
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
 *  &#064;File
 *  public String getImage(Context context, &#064;Param("id") int id)
 *  {
 *    String location = getImage(id);
 *    return location;
 *  }
 */
@Response(FileResponse.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TossFile 
{
  /**
   * Returns {@code true} if the file should included as an attachment on the
   * response.  This value defaults to {@code true}.
   *
   * @return {@code true} if the file should included as an attachment on the
   *         response
   */
  boolean asAttachment() default true;

  /**
   * Returns the content type of the file in the response.  THis value defaults
   * to the empty string.
   *
   * @return the content type of the file in the response
   */
  String contentType() default "";
}
