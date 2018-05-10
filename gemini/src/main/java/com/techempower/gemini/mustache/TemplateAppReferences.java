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

import java.util.*;

import com.techempower.*;
import com.techempower.gemini.*;
import com.techempower.util.*;

/**
 * As part of the TemplateReferences structure delivered to Mustache 
 * templates, this portion is a small set of references to application-scope
 * objects and flags such as the deployment environment of the application 
 * (e.g., dev, test, or prod). 
 */
public class TemplateAppReferences
  implements Configurable
{

  //
  // Member variables.
  //

  /** Reference to the application. */
  public final GeminiApplication application;
  
  /** A set of flags characterizing the deployment environment. */
  public final Map<Object, Object> env;
  
  /** A set of asset directories/paths. */
  public final Map<Object, String> path;

  /**
   * Constructor.
   */
  public TemplateAppReferences(GeminiApplication application) 
  {
    this.application = application;
    
    // Make an empty map as a placeholder until configuration is done.
    this.env = new HashMap<>();
    this.path = new HashMap<>();
  }
  
  /**
   * Sets the deployment context flag set.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    final Version version = this.application.getVersion();
    
    // Copy the environment flags from the Version object.
    env.clear();
    env.putAll(version.getEnvironmentFlags());
    env.put("name", version.getProductName());
    env.put("descriptor", version.getDeploymentDescription());
    env.put("version", version.getVersionString());
    
    // Add asset directories/paths from the infrastructure.
    path.clear();
    Infrastructure inf = this.application.getInfrastructure();
    path.put("img", inf.getImageDirectory());
    path.put("css", inf.getCssDirectory());
    path.put("js", inf.getJavaScriptDirectory());
    path.put("server", inf.getServerName());
    path.put("relative", inf.getUrl());
    path.put("absolute", inf.getStandardDomain());
    path.put("secure", inf.getSecureDomain());    // "secure root"
  }
  
}
