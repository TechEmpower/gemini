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

package com.techempower.gemini.email;

import java.io.*;

/**
 * Encapsulates the information related to an email-attached file.  The
 * EmailPackage class has a collection of attachments composed of instances
 * of this class.
 */
public class EmailAttachment
{
  //
  // Member variables.
  //

  private File      file        = null;    //  null attachment by default.
  private String    name        = null;    //  null attachmentName by default.
  private Object    object      = null;    //  null attachmentObject by default.
  
  //
  // Member methods.
  //

  /**
   * Standard constructor.  Email Attachment with a file attachment name 
   * specified.
   */
  public EmailAttachment(File file, String name)
  {
    setFile(file);
    setName(name);
  }
  
  /**
   * Simpler constructor.  Email Attachment that assigns the file name to the
   * attachment name.
   */
  public EmailAttachment(File file)
  {
    setFile(file);
    setName(file.getName());
  }

  /**
   * Constructor to define an arbitrary object as an attachment.
   */
  public EmailAttachment(Object obj, String name)
  {
    this.object = obj;
    setName(name);
  }

  /**
   * Gets the object attachment.
   */
  public Object getObjectAttachment()
  {
    return this.object;
  }

  /**
   * Returns the Object attachment as an InputStream if it is an InputStream,
   * otherwise returns null.
   */
  public InputStream getInputStream()
  {
    if (this.object instanceof InputStream)
    {
      return (InputStream)this.object;
    }
    else
    {
      return null;
    }
  }

  /**
   * Sets the full file path of the file to be attached.
   */
  public void setFile(File attachment)
  {
    if ( (attachment != null) && (attachment.isFile()) )
    {
      this.file = attachment;
    }
  }

  /**
   * Gets the full file path of the attached file.
   */
  public File getFile()
  {
    return this.file;
  }
  
  /**
   * Sets the name of the attached file as it will appear in the email.
   */
  protected void setName(String attachmentName)
  {
    this.name = attachmentName;
  }
  
  /**
   * Gets the name of the EmailAttachment
   */
  public String getName()
  {
    return this.name;
  }

  /**
   * Standard Java toString.
   */
  @Override
  public String toString()
  {
    return "EmailAttachment [file name:" + getFile().getName() 
      + "; attachment name:" + getName()
      + "]";
  }

}   // End EmailAttachment.
