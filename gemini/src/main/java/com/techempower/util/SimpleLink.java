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

package com.techempower.util;

/**
 * A simple data structure to store a link name and URL. I use this for
 * delivering optional navigation items that need to be provided to a JSP.
 */
public class SimpleLink
{
  //
  // Variables.
  //
  
  public String linkName;
  public String url;
  public StringList classNames = new StringList(" ");

  //
  // Methods.
  //
  
  /**
   * Construct a link.
   * 
   * @param linkName The text in the hyperlink that users will see
   * @param url The url for the hyperlink to go to (href)
   */
  public SimpleLink(String linkName, String url)
  {
    this.linkName = linkName;
    this.url = url;
  }

  /**
   * Construct a link.
   * 
   * @param linkName The text in the hyperlink that users will see
   * @param url The url for the hyperlink to go to (href)
   * @param classes CSS class names (or names separated by spaces)
   */
  public SimpleLink(String linkName, String url, String... classes)
  {
    super();
    this.linkName = linkName;
    this.url = url;
    this.classNames = new StringList(" ");
    for(String className: classes) {
      this.classNames.add(className);
    }
  }
}
