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

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ResourceManager that loads resources from files.  Files 
 * are assumed to be in the format of
 *   <p> 
 * [projectname]-[languagecode]-[countrycode].resources
 *   <p>
 * and the default location is WEB-INF/internationalization.  Both countrycode
 * and languagecode are optional, and a resource file named 
 * [project].resources is assumed to be the "root" resources file.  If the 
 * root resources file is not available, the hard-coded default resources
 * provided by the GeminiLocaleManager will be used instead.
 *   <p>
 * A locale of en_US will have a parent of en. and the locale en will have a
 * parent of the root (or default) resources.
 *   <p>
 * If a file doesn't exist for a given locale, the FileResourceManager will 
 * try to look for a parent file.  if no files exist, all Locales will 
 * use the default resources provided by GeminiLocaleManager. 
 *   <p>
 * Although a Locale might inherit some resources from a parent Resources 
 * object, each Locale has a unique Resources object just the same.  Requests
 * for resources that are missing in the Locale's Resources object simply
 * pass-through to the parent, and so on.  Any changes to a Locale's Resources
 * will not effect other resources directly; changes to a parent's Resources
 * can appear to affect children, but that is only by way of the pass-through
 * mechanism.  During save operations, the unique Resources get written 
 * to their logical file, even if that file didn't exist originally.
 *   <p>
 * Configurable parameters:
 *   <ul>
 * <li>I18n.ResourceLocation - Where to find resource files.  The default is 
 *     within ${Servlet.WebInf}internationalization.</li>
 *   </ul>
 */
public class FileResourceManager 
     extends ResourceManager
{

  //
  // Constants.
  //
  
  public static final String           FILE_EXTENSION = ".resources";
  public static final String           DEFAULT_RESOURCE_LOCATION = 
      "${" + Configurator.PROP_WEBINF + "}"
      + File.separatorChar
      + "internationalization" 
      + File.separatorChar;
  
  //
  // Members.
  //
  
  private String                       resourceLocation;
  private Map<Locale, GeminiResources> loadedResources = new HashMap<>();
  private volatile GeminiResources     rootResources;
  private          Logger              log = LoggerFactory.getLogger(getClass());
  
  //
  // Methods.
  //
  
  /**
   * Default Constructor.  This is typically called when the application is
   * configured.
   * 
   * @param application the Application reference.
   * @param localeManager the GeminiLocalManager reference.
   * @param props the application's configuration.
   */
  public FileResourceManager(GeminiApplication application, 
      GeminiLocaleManager localeManager, 
      EnhancedProperties props)
  {
    super(application, localeManager, props);
    this.resourceLocation = props.get("I18n.ResourcesLocation",
        DEFAULT_RESOURCE_LOCATION);
    log.info("Resources location: {}", this.resourceLocation);
  }

  /**
   * Returns the GeminiResource for a specific Locale.
   */
  @Override
  public GeminiResources get(Locale locale) 
  {
    // If the Locale is null, or language and country are empty, 
    // we'll just return the root resources.
    if (locale == null || StringHelper.isEmpty(locale.getLanguage()))
    {
      return getRootResources();
    }
    
    // Look in our map for any resources associated with this Locale.
    GeminiResources resources = this.loadedResources.get(locale);
    
    // If the resources for this Locale aren't found, let's load them.
    if (resources == null)
    {
      resources = load(locale);
    }
    
    return resources;
  }
  
  /**
   * Load resource files for a specified Locale, and wire up the parent
   * reference appropriately.
   */
  protected synchronized GeminiResources load(Locale locale)
  {
    // Find a suitable parent.  We'll either use the root as a parent or, if 
    // available, the country-only Locale as a parent.
    GeminiResources parentResources = getRootResources();
    GeminiResources toReturn;
    
    if (  (locale != null)
       && (locale.getCountry() != null)
       )
    {
      Locale parentLocale = new Locale(locale.getLanguage());
      
      // Only proceed up the chain if we have, in fact, found a different
      // locale than the one we were provided.
      if (!parentLocale.equals(locale))
      {
        // By calling "get" here, we'll either get the country-only resources
        // or the root resources if no country-only resources exist. 
        parentResources = get(parentLocale);
      }
    }
    
    // Attempt to load the properties file for the requested Locale.
    Properties props = loadProperties(getResourceFilename(locale));

    // If we were successful at loading the Properties, let's build a 
    // GeminiResources object from them.
    if (props != null)
    {
      toReturn = new GeminiResources(getApplication(), props, locale, parentResources);
    }
    else
    {
      // No luck, we'll just return the parent resources.
      toReturn = parentResources;
    }
    
    // Clone the existing map.
    Map<Locale, GeminiResources> newMap = 
        new HashMap<>(this.loadedResources);
    
    // Add the resource to the cloned map.
    newMap.put(locale, toReturn);
    
    // Update the reference.
    this.loadedResources = newMap;
    
    return toReturn;
  }
  
  /**
   * Returns the file system location for a Resources file associated with
   * the provided Locale.  This method only returns the name of the location
   * and does not verify that any such file exists.
   */
  protected String getResourceFilename(Locale locale)
  {
    String filename;
    if (  (locale == null)
       || (StringHelper.isEmpty(locale.getLanguage()))
       )
    {
      // This is the special case for the application's root resources file
      //(with no language or country code).
      filename = getApplication().getVersion().getAbbreviatedProductName()
          + FILE_EXTENSION;
    }
    else if (  (StringHelper.isNonEmpty(locale.getLanguage()))
            && (StringHelper.isNonEmpty(locale.getCountry()))
            )
    {
      // The Locale has both language and country defined.
      filename = getApplication().getVersion().getAbbreviatedProductName()
          + "-" + locale.getLanguage() + "-" + locale.getCountry()
          + FILE_EXTENSION;
    }
    else
    {
      // The Local defines only a language.
      filename = getApplication().getVersion().getAbbreviatedProductName()
          + "-" + locale.getLanguage() + FILE_EXTENSION;
    }
    
    return this.resourceLocation + filename;
  }
    
  /**
   * Loads a resource from a given file path. Returns null if the file doesn't exist,
   * or if it can't be read.
   */
  protected Properties loadProperties(String filename)
  {
    // Try to load the requested resource file using a traditional file 
    // input stream.
    File resourceFile = new File(filename);
    
    if (!resourceFile.exists())
    {
      // The resources file doesn't exist.
      log.info("Resources file not found: \"{}\"", filename);
      return null;
    }

    try (
        InputStream is = new FileInputStream(resourceFile);
        // Try to load the requested resources file from this directory.
        Reader in = new InputStreamReader(is, StandardCharsets.UTF_8)
        )
    {
      // Load the contents of the file.
      Properties props = new Properties();
      props.load(in);

      log.info("Loaded resources from \"{}\".", filename);
      
      return props;
    }
    catch (IOException ioexception)
    {
      // there was a problem reading the input, we'll log it.
      log.error("Exception while reading resource file: \"{}\"",
          filename, ioexception);
    }

    return null;
  }

  /**
   * Saves every resource to it's underlying source. This can create new 
   * files if originally there was no file for the Locale. So if the only 
   * file that exists is [project].resource, and the locale fr_FR and en_US 
   * are both mapped to this resource, new files called 
   * [project]-fr-FR.resource and [project]-en-US.resources will be created. 
   * The reason for this is that it's possible someone has made a change 
   * to the values for a specific locale, we want to make sure those changes
   * persist.
   */
  @Override
  public boolean save()
  {
    boolean saved = save(null, getRootResources());
    for (Map.Entry<Locale, GeminiResources> entry : this.loadedResources.entrySet())
    {
      if (!save(entry.getKey(), entry.getValue()))
      {
        saved = false;
      }
    }
    return saved;
  }
  
  /**
   * Saves a single resource to disk.
   */
  @Override
  public boolean save(GeminiResources resources) 
  {
    return save(resources.getLocale(), resources);
  }

  /**
   * Saves the resource for the given locale to disk.
   */
  @Override
  public boolean save(Locale locale) 
  {
    if (!this.loadedResources.containsKey(locale))
    {
      return false;
    }
    
    return save(locale, this.loadedResources.get(locale));
  }
  
  /**
   * saves a given resource to file. Doesn't update mappings
   */
  protected boolean save(Locale locale, GeminiResources resource)
  {
    // make sure directory exists
    File dir = new File(this.resourceLocation);
    if (!dir.exists())
    {
      dir.mkdir();
    }

    // Get the file path to save to based on the locale
    String filename = getResourceFilename(locale);
    try (
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(filename), StandardCharsets.UTF_8))
        )
    {
      // Tell the Properties object of the resource to write itself.
      resource.getAll().store(writer, null);

      return true;
    }
    catch (IOException e)
    {
      log.error("::save - threw exception while trying to write to file.", e);
    }

    // We've run into an error while trying to save
    return false;
  }

  /**
   * Tries to gather default resources from the "root" resources file,
   * [project].resources.  If the file doesn't exist, the default resources
   * provided by the LocaleManager will be used.
   */
  protected GeminiResources getRootResources()
  {
    // Yes, rootResources is volatile.
    if (this.rootResources == null)
    {
      synchronized (this)
      {
        if (this.rootResources == null)
        {
          GeminiResources newRootResources;
          
          // Attempt to load the properties file for the requested Locale.
          Properties props = loadProperties(getResourceFilename(null));

          // If we were successful at loading the Properties, let's build a 
          // GeminiResources object from them.
          if (props != null)
          {
            newRootResources = new GeminiResources(getApplication(), props,
                null, getLocaleManager().getDefaultResources());
          }
          else
          {
            newRootResources = getLocaleManager().getDefaultResources();
          }
          
          // Set the reference.
          this.rootResources = newRootResources;
        }
      }
    }
    
    return this.rootResources;
  }

  /**
   * Clears all resource mappings, this will force them all to be reloaded.
   */
  @Override
  public void reset() 
  {
    this.loadedResources.clear();
  }

  /**
   * Clears the mapping for a specific Locale, this will force that resource to be
   * reloaded.
   */
  @Override
  public void reset(Locale locale) 
  {
    this.loadedResources.remove(locale);
  }
  
}
