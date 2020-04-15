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
import com.techempower.helper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds user interface resources (in the form of macro-enabled Strings) for
 * a Locale (such as US English).
 *   <p>
 * This class should cannot be instantiated directly by client code but rather
 * is instantiated by a ResourceManager such as the FileResourceManager,
 * which in turn is created by GeminiLocaleManager.
 *   <p>
 * GeminiResources objects can have the lookup of Values cascade by setting a
 * parent reference.  When asked to resolve a Value from a Key, the local
 * map will be referenced; and if the Key is not found, the parent will be
 * asked to fulfill the request.  This can progress up the chain to the
 * "Default Resources" which are usually hard-coded as either a plain instance
 * of or a subclass of DefaultGeminiResources.
 */
public class GeminiResources
{

  /**
   * A set of macro indicators that is reused for macro expansion.
   */
  private static final String[] macroIndicators = { 
    "$S1", "$S2", "$S3", "$S4", "$S5", "$S6", "$S7", "$S8", "$S9", "$SA",
    "$SB", "$SC", "$SD", "$SE", "$SF", "$SG", "$SH", "$SI", "$SJ", "$SK", 
    "$SL", "$SM", "$SN", "$SO", "$SP", "$SQ", "$SR", "$SS", "$ST", "$SU"
  };

  //
  // Member variables.
  //

  private final Logger              log = LoggerFactory.getLogger(getClass());
  private final Locale              locale;
  private final GeminiResources     parent;
  private       Properties          properties;
  
  //
  // Member methods.
  //

  /**
   * Default constructor.
   * 
   * @param app The application reference.
   */
  protected GeminiResources(GeminiApplication app)
  {
    this(app, new Properties(), null, null);
  }

  /**
   * Constructor.
   * 
   * @param app The application reference.
   * @param properties Reference to the application's configuration.
   * @param locale the Locale to which these Resources are mapped.
   * @param parent a reference to the parent GeminiResources instance.
   */
  protected GeminiResources(GeminiApplication app, Properties properties,
      Locale locale, GeminiResources parent)
  {
    this.properties = properties;
    this.locale     = locale;
    this.parent     = parent;
  }

  /**
   * Return all the resources as a Properties object. 
   */
  public Properties getAll()
  {
    return this.properties;
  }
  
  /**
   * Returns true if the resource has a mapping with the given key.
   */
  public boolean hasKey(String key)
  {
    return this.properties.containsKey(key);
  }

  /**
   * Gets a localized string associated with the key, or the key itself if
   * no value for the given key exists.
   * 
   * @param key The localization key
   */
  public String get(String key)
  {
    // If the key doesn't exist in this resources file; add it as an empty
    // string.
    if (!hasKey(key))
    {
      if (this.parent == null)
      {
        // There are no more parents to check.  We're at the root.  So let's
        // log the issue and return just the Key as if it were the Value.
        this.log.info("Key \"{}\" does not exist in the resources.", key);
        return key;
      }
      else
      {
        return this.parent.get(key);
      }
    }
    else
    {
      String value = (String)this.properties.get(key);
      if (StringHelper.isEmptyTrimmed(value))
      {
        // The value for this key is empty, log it since this probably 
        // shouldn't be the case.
        this.log.info("Value for key \"{}\" is empty.", key);
      }
      return value;
    }
  }
  
  /**
   * Gets a resource string, replacing macros as necessary.
   */
  public String get(String key, String... macros)
  {
    String toExpand = this.get(key);
    if (macros != null)
    {
      return StringHelper.replaceSubstrings(toExpand, macroIndicators, macros);
    }
    return toExpand;
  }
  
  /**
   * Loads properties into this resource. These properties will overwrite any values with 
   * the same key.
   */
  public void loadFromProperties(Properties newProperties)
  {
    if (this.properties == null)
    {
      this.properties = newProperties;
    }
    else
    {
      for (Object objKey : newProperties.keySet())
      {
        String key = objKey.toString();
        
        if (!StringHelper.isEmptyTrimmed(newProperties.getProperty(key)))
        {
          this.properties.setProperty(key, newProperties.getProperty(key));
        }
      }
    }
  }
  
  /**
   * Gets the Local with which this GeminiResources object is associated.
   */
  public Locale getLocale()
  {
    return this.locale;
  }
  
  /**
   * Returns the parent GeminiResources
   */
  public GeminiResources getParent()
  {
    return this.parent;
  }
  
  /**
   * Gets an updater reference for this GeminiResources object.  This is used
   * to apply a set of updates in a single thread-safe event.
   */
  public Updater updater()
  {
    return new Updater();
  }
  
  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "GeminiResources [locale: " 
        + (this.locale != null ? this.locale.getDisplayName() : "--")
        + "]";
  }
  
  /**
   * A utility class allowing keys to be added to the resource map at runtime.
   * This is typically only used to update the DefaultGeminiResources subclass
   * to add resources used as classes are loaded.  For example, as custom
   * FormElements are loaded, their static initializer  
   */
  public final class Updater
  {
    private final Map<String, String> updates = new HashMap<>();
    private String prefix = "";
    
    private Updater()
    {
    }
    
    /**
     * Sets any String to prepend to subsequently-updated keys.
     */
    public Updater prefix(String newPrefix)
    {
      this.prefix = newPrefix;
      return this;
    }
    
    /**
     * Adds a key-value update.
     */
    public Updater put(String key, String value)
    {
      if (key == null)
      {
        throw new IllegalArgumentException("Key was null.");
      }
      else if (value == null)
      {
        throw new IllegalArgumentException("Value was null.");
      }
      
      this.updates.put((StringHelper.isNonEmpty(this.prefix) ? this.prefix
          : "") + key, value);
      
      return this;
    }
    
    /**
     * Commits the updates in one thread-safe event.
     */
    public Updater commit()
    {
      synchronized (GeminiResources.this) 
      {
        // Clone the existing properties so that we can do updates in a
        // thread-safe copy.
        Properties newProperties = (Properties)GeminiResources.this.properties.clone();
        
        // Update.
        for (Map.Entry<String, String> entry : this.updates.entrySet())
        {
          newProperties.put(entry.getKey(), entry.getValue());
        }
        
        // Switch the reference over.
        GeminiResources.this.properties = newProperties;
      }
      
      return this;
    }
  }
  
}   // End GeminiResources.
