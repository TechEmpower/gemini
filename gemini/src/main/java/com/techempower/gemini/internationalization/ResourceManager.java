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

package com.techempower.gemini.internationalization;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interface for loading, persisting, and quickly retrieving cached 
 * GeminiResources.  A ResourceManager is expected to fully manage a cache
 * of loaded GeminiResources mapped from Locales.  This is typically as
 * simple as maintaining a {@code Map<Locale, GeminiResources>}.
 */
public abstract class ResourceManager 
{
  
  //
  // Members.
  //
  
  private final GeminiApplication   application;
  private final GeminiLocaleManager localeManager;
  protected static final String     COMPONENT_CODE = "rMgr";
  private final Logger              log = LoggerFactory.getLogger(COMPONENT_CODE);
  
  //
  // Methods.
  //
  
  /**
   * Constructor.  This is typically called when the application is
   * configured.
   * 
   * @param application the Application reference.
   * @param localeManager the GeminiLocalManager reference.
   * @param props the application's configuration.
   */
  public ResourceManager(GeminiApplication application, 
      GeminiLocaleManager localeManager, EnhancedProperties props)
  {
    this.application = application;
    this.localeManager = localeManager;
  }
  
  /**
   * Gets the application reference.
   */
  protected GeminiApplication getApplication()
  {
    return this.application;
  }
  
  /**
   * Gets the LocaleManager reference. 
   */
  protected GeminiLocaleManager getLocaleManager()
  {
    return this.localeManager;
  }
  
  /**
   * Returns a GeminiResource object for the given Locale. 
   */
  public abstract GeminiResources get(Locale locale);
  
  /**
   * Saves changes to all resources. For a File Resource, this would save all
   * changes to disk.
   */
  public abstract boolean save();
  
  /**
   * Saves changes to a specific GeminiResource.
   */
  public abstract boolean save(GeminiResources resource);
  
  /**
   * Saves changes for a specific Locale
   */
  public abstract boolean save(Locale locale);
  
  /**
   * Resets references to all loaded resources, meaning that resources will
   * be re-loaded on demand as they are needed.
   */
  public abstract void reset();
  
  /**
   * Resets a specific reference to a Locale's loaded resources, meaning that
   * that Locale's resources will be re-loaded on demand as needed.
   */
  public abstract void reset(Locale locale);

}
