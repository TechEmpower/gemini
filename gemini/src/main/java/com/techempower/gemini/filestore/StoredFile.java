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
package com.techempower.gemini.filestore;

import com.techempower.helper.*;
import com.techempower.js.legacy.*;
import com.techempower.util.*;

/**
 * A simple object representing a stored file.
 */
public class StoredFile
  implements JavaScriptObject
{
  
  //
  // Constants.
  //
  
  public static final short TYPE_PLAIN             = 0;
  public static final short TYPE_IMAGE             = 1;
  public static final short TYPE_TEXT              = 2;  // Text or document file
  public static final short TYPE_BINARY            = 3;
  
  private static final String[] TYPE_NAMES = { "Unknown file type", "Image", 
    "Text or document", "Binary"
  };
  private static final int[] TYPE_VALUES = new int[] { 0, 1, 2, 3 };
    
  //
  // Member variables.
  //
  
  private String filename;
  private String url;
  private long   size;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public StoredFile(String filename, String url, long size)
  {
    setFilename(filename);
    setUrl(url);
    setSize(size);
  }

  /**
   * @return Returns the filename.
   */
  public String getFilename()
  {
    return this.filename;
  }
  
  /**
   * Tries to determine the rough type (image, text/document, binary) of
   * the file by its extension.
   */
  public int getRoughType()
  {
    if (getFilename() != null)
    {
      switch (FileHelper.getExtension(getFilename().toLowerCase()))
      {
        case "jpg":
        case "jpeg":
        case "gif":
        case "png":
          return TYPE_IMAGE;
        case "txt":
        case "log":
        case "doc":
        case "xls":
        case "docx":
        case "xlsx":
        case "ppt":
        case "sql":
        case "html":
        case "js":
        case "xml":
          return TYPE_TEXT;
        case "zip":
        case "dat":
        case "rar":
        case "7z":
          return TYPE_BINARY;
      }
    }
    
    // If we don't know what it is, return type 0.
    return TYPE_PLAIN;
  }
  
  /**
   * Returns the file's extension as lowercase.
   */
  public String getExtension()
  {
    return FileHelper.getExtension(getFilename().toLowerCase());
  }

  /**
   * @param filename The filename to set.
   */
  public void setFilename(String filename)
  {
    this.filename = filename;
  }

  /**
   * @return Returns the size.
   */
  public long getSize()
  {
    return this.size;
  }

  /**
   * @param size The size to set.
   */
  public void setSize(long size)
  {
    this.size = size;
  }
  
  /**
   * Gets the size in kilobytes.
   */
  public int getSizeKiB()
  {
    return (int)(Math.max((getSize() / UtilityConstants.KILOBYTE), 1));
  }

  /**
   * @return Returns the url.
   */
  public String getUrl()
  {
    return this.url;
  }

  /**
   * @param url The url to set.
   */
  public void setUrl(String url)
  {
    this.url = url;
  }
  
  /**
   * To string.
   */
  @Override
  public String toString()
  {
    return "File [" + getFilename() + "; " + getSize() + "b]";
  }
  
  @Override
  public VisitorFactory<StoredFile> getJsVisitorFactory()
  {
    return JSVF;
  }
  private static final VisitorFactory<StoredFile> JSVF = 
      new ReflectiveVisitorFactory<>(
        StoredFile.class,
        "filename", "getFilename",
        "url", "getUrl",
        "size", "getSize",
        "type", "getRoughType"
      );

  /**
   * Gets the Type names.
   */
  public static String[] getTypeNames()
  {
    return TYPE_NAMES.clone(); 
  }
  
  /**
   * Gets the type values. 
   */
  public static int[] getTypeValues()
  {
    return TYPE_VALUES.clone();
  }
  
}  // End StoredFile.
