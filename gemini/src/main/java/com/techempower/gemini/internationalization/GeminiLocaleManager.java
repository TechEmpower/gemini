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
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * GeminiLocaleManager is the "main" class of the Gemini internationalization
 * components.
 *   <p>
 * Manages lookup and use of locale-specific user interface messages for
 * Gemini and, optionally, applications built upon Gemini.  The primary
 * intent of GeminiLocaleManager is to provide a means to localize the
 * user interface of Gemini's FormElements.  However, this manager class
 * can be used to provide localized user interface, in general, for
 * Gemini applications.
 *   <p>
 * GeminiLocaleManager utilizes the ResourceManager class to handle the 
 * underlying data-source management of GeminiResources. This allows a greater
 * flexibility for retrieval and management of GeminiResources.
 *   <p>
 * A DefaultGeminiResources class exists to provide US-English user interface
 * to applications that have no internationalization needs.
 */
public class GeminiLocaleManager
  implements Configurable
{
  
  //
  // Constants.
  //
  
  public static final String DEFAULT_LANGUAGE_CODE = "en";
  public static final String DEFAULT_COUNTRY_CODE = "US";

  public static final String COMPONENT_CODE = "loca";
  
  private static final String SESSION_LOCALE_SUFFIX = "-Locale";
  
  //
  // Static variables.
  //
  
  /**
   * Records an reference to the last-instantiated GeminiLocaleManager.  Most
   * applications will only ever instantiate one, so this is should always
   * be the right reference.
   */
  private static   GeminiLocaleManager instance;

  //
  // Member variables.
  //
  
  private final    ComponentLog      log;
  private volatile ResourceManager   resourceManager;
  private final    GeminiApplication application;
  private final    String            productName;
  private final    Locale            defaultLocale;
  private final    GeminiResources   defaultResources;

  //
  // Member methods.
  //

  /**
   * Constructor.
   * 
   * @param application The application reference.
   * @param defaultResources A set of resources to use as a fail-safe if no
   *        resources can be loaded by the ResourceManager; this is usually
   *        a subclass of DefaultGeminiResources.
   */
  public GeminiLocaleManager(GeminiApplication application, 
      GeminiResources defaultResources)
  {
    this.application   = application;
    this.log           = application.getLog(COMPONENT_CODE);
    this.productName   = application.getVersion().getAbbreviatedProductName();
    this.defaultLocale = new Locale(DEFAULT_LANGUAGE_CODE, DEFAULT_COUNTRY_CODE);
    
    if (defaultResources != null)
    {
      this.defaultResources = defaultResources; 
    }
    else
    {
      this.log.log("Instantiating built-in default (US English) resources.");
      this.defaultResources = new DefaultGeminiResources(application);
    }
    
    // Register for configuration.
    application.getConfigurator().addConfigurable(this);
    
    instance = this;
  }

  /**
   * Constructor.  This simplified version uses a plain DefaultGeminiResources
   * for the default resources.
   * 
   * @param application The application reference.
   */
  public GeminiLocaleManager(GeminiApplication application)
  {
    this(application, null);
  }

  /**
   * Gets the static reference to the GeminiLocaleManager singleton (the
   * most-recently instantiated GeminiLocaleManager, which is typically the
   * -only- such object).
   */
  public static GeminiLocaleManager getInstance()
  {
    return instance;
  }
  
  /**
   * Configures this component.  Reconfiguring the GeminiLocaleManager will
   * recreate the ResourceManager, causing all resources to be reloaded.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // We create a FileResourceManager, but in the future, we may have other
    // configurable Managers for resources (e.g., DatabaseResourceManager).
    this.resourceManager = new FileResourceManager(this.application, this, props);
  }

  /**
   * Gets the default locale for this application.
   */
  public Locale getDefaultLocale()
  {
    return this.defaultLocale;
  }
  
  /**
   * Gets a reference to the default resources.
   */
  public GeminiResources getDefaultResources()
  {
    return this.defaultResources;
  }

  /**
   * Sets a user session's locale.
   *
   * @param languageID the Language ID for the Locale.
   * @param countryID the Country ID for the Locale.
   */
  public void setLocale(BasicContext context, String languageID, String countryID)
  {
    context.session().putObject(this.productName + SESSION_LOCALE_SUFFIX, 
      new Locale(languageID, countryID));
  }

  /**
   * Sets a user session's locale.
   */
  public void setLocale(BasicContext context, Locale locale)
  {
    context.session().putObject(this.productName + SESSION_LOCALE_SUFFIX, locale);
  }

  /**
   * Gets a user session's locale.  If no locale it set, the default locale
   * is returned.
   */
  public Locale getLocale(BasicContext context)
  {
    if (context != null)
    {
      Locale toReturn = getLocaleRaw(context);

      return (toReturn != null 
          ? toReturn 
          : getDefaultLocale());
    }
    else
    {
      return getDefaultLocale();
    }
  }

  /**
   * Gets a user session's locale.  If no locale is set for the session, null
   * will be returned.
   */
  public Locale getLocaleRaw(BasicContext context)
  {
    return context.session().getObject(this.productName + SESSION_LOCALE_SUFFIX);
  }

  /**
   * Gets a GeminiResources given a Context.  If no appropriate resources are
   * available, the default resources are returned.
   */
  public GeminiResources getResources(BasicContext context)
  {
    return getResources(getLocale(context));
  }

  /**
   * Gets a GeminiResource from the ResourceManager based on the Locale.
   */
  public GeminiResources getResources(Locale locale)
  {
    return this.resourceManager.get(locale);
  }
  
  /**
   * Save the loaded resources to their underlying source.
   */
  public boolean saveResources()
  {
    return this.resourceManager.save();
  }   
    
  /**
   * Removes a resources from the table for a specific Locale.  This forces a
   * reload of the resource data.
   */
  public void resetResource(Locale locale)
  {
    this.resourceManager.reset(locale);
  }
  
  /**
   * Removes all the loaded resources.  This forces a reload of all Locales'
   * resource data.
   */
  public void resetResources()
  {
    this.resourceManager.reset();
  }

  /**
   * Gets the reference to the application.
   */
  protected GeminiApplication getApplication()
  {
    return this.application;
  }

}   // End GeminiLocaleManager.
