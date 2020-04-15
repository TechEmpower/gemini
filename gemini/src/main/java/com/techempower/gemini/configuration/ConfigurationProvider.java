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
package com.techempower.gemini.configuration;

import com.techempower.gemini.*;
import com.techempower.util.*;

/**
 * Describes a component that can provide an EnhancedProperties configuration
 * for use by the application.  The standard implementation is a file system
 * provider that reads configuration file(s) from disk.  An alternate
 * implementation reads the configuration from the cluster master.
 *   <p>
 * The interface provides for an order of precedence for configuration
 * providers.  This allows you to specify, for example, that you prefer to
 * load configuration from the cluster master, assuming the Servlet
 * deployment specifies the necessary information, and failing that, from
 * the file system.
 */
public interface ConfigurationProvider
{

  /**
   * Load configuration into the provided EnhancedProperties object using 
   * whatever approach this provider implements.  Return false if no suitable
   * configuration can be found/loaded; return true if configuration was 
   * found and successfully loaded.
   *   <p>
   * No changes made to the provided EnhancedProperties will be retained if
   * the provider returns false.
   */
  boolean load(GeminiApplication application, EnhancedProperties props);
  
}
