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

package com.techempower.gemini.mustache;

import java.io.*;

import com.github.mustachejava.*;
import com.techempower.gemini.*;
import com.techempower.gemini.configuration.*;
import com.techempower.util.*;

/**
 * The Resin specific implementation of {@link MustacheManager},
 * which compiles and renders Mustache templates
 */
public class ResinMustacheManager
     extends MustacheManager
{
  public ResinMustacheManager(GeminiApplication app)
  {
    super(app);
  }
  
  @Override
  public void configure(EnhancedProperties props)
  {
    super.configure(props);
    final EnhancedProperties.Focus focus = props.focus("Mustache.");
    this.mustacheDirectory = focus.get("Directory", "${Servlet.WebInf}/mustache/");
    validateMustacheDirectory();
    setupTemplateCache();
  }
  
  /**
   * Returns a mustache factory.  In the development environment, this method
   * returns a new factory on each invocation so that compiled templates are
   * not cached.  In production, this returns the same factory every time,
   * which caches templates.
   */
  @Override
  public MustacheFactory getMustacheFactory()
  {
    return (useTemplateCache && this.mustacheFactory != null
        ? this.mustacheFactory
        : new DefaultMustacheFactory(new File(this.mustacheDirectory)));
  }
  
  @Override
  public void resetTemplateCache()
  {
    mustacheFactory = new DefaultMustacheFactory(new File(mustacheDirectory));
  }
  
  /**
   * Confirm that a valid directory has been provided by the configuration.
   */
  protected void validateMustacheDirectory()
  {
    if (this.enabled)
    {
      // Confirm directory exists.
      final File directory = new File(this.mustacheDirectory);
      if (!directory.isDirectory())
      {
        throw new ConfigurationError("Mustache.Directory " + this.mustacheDirectory + " does not exist.");
      }
    }
  }
}
 