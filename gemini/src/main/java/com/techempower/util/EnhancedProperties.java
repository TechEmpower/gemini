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

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.Map.Entry;

import com.techempower.*;
import com.techempower.collection.*;
import com.techempower.helper.*;

/**
 * Similar to the standard Java Properties class but tuned for configuration
 * files containing named strings (key-value mappings where the values are
 * strings).  The value strings can then be parsed by convenience methods
 * into integers, longs, and booleans.
 *   <p>
 * EnhancedProperties supports simple macro-expansion using ${PropName}
 * syntax.
 *   <p>
 * A focused view limited to only property names that begin with a specified
 * prefix can be created using the focus() method.
 */
public class EnhancedProperties
  implements MutableNamedValues 
{
  //
  // Constants.
  //

  public static final String MACRO_START = "${";
  public static final String MACRO_END   = "}";

  //
  // Member variables.
  //

  private final transient TechEmpowerApplication application;
  private final Properties backing;

  //
  // Member methods.
  //

  /**
   * Constructor.  Takes in a reference to a TechEmpower application.
   */
  public EnhancedProperties(TechEmpowerApplication application)
  {
    this.application = application;
    this.backing = new Properties();
  }

  /**
   * Basic constructor.
   */
  public EnhancedProperties()
  {
    this(null);
  }

  /**
   * Gets the TechEmpowerApplication reference.
   */
  public TechEmpowerApplication getApplication()
  {
    return this.application;
  }
  
  /**
   * Gets the backing Properties reference.  This is not recommended but is
   * enabled for legacy applications.
   * 
   * @deprecated This should not be used in new applications.
   */
  @Deprecated
  public Properties getBackingProperties()
  {
    return backing;
  }

  /**
   * Store an arbitrary object.
   */
  private EnhancedProperties putObject(String name, Object value)
  {
    backing.put(name, value);
    return this;
  }

  @Override
  public EnhancedProperties put(String name, String value)
  {
    return putObject(name, value);
  }

  @Override
  public EnhancedProperties put(String name, int value)
  {
    return putObject(name, Integer.toString(value));
  }

  @Override
  public EnhancedProperties put(String name, long value)
  {
    return putObject(name, Long.toString(value));
  }

  @Override
  public EnhancedProperties put(String name, boolean value)
  {
    return putObject(name, Boolean.toString(value));
  }

  @Override
  public EnhancedProperties remove(String name)
  {
    backing.remove(name);
    return this;
  }

  @Override
  public EnhancedProperties clear()
  {
    backing.clear();
    return this;
  }
  
  @Override
  public String get(String name)
  {
    return getBackingProperty(name);
  }
  
  @Override
  public String get(String name, String defaultValue)
  {
    return getBackingProperty(name, defaultValue);
  }
  
  @Override
  public Set<String> names()
  {
    final Set<Object> backingKeys = backing.keySet();
    final Set<String> toReturn = new HashSet<>(backingKeys.size());
    for (Object key : backingKeys)
    {
      toReturn.add(key.toString());
    }
    return toReturn;
  }

  @Override
  public boolean has(String name)
  {
    return (backing.getProperty(name) != null);
  }
  
  @Override
  public long getLong(String name)
  {
    return getLong(name, 0L);
  }
  
  @Override
  public long getLong(String name, long defaultValue)
  {
    long toReturn = defaultValue;

    try
    {
      String value = getBackingProperty(name);
      if (value != null)
      {
        toReturn = Long.parseLong(value.trim());
      }
    }
    catch (NumberFormatException exc)
    {
      // Do nothing.
    }

    return toReturn;
  }

  @Override
  public long getLong(String name, long defaultValue, long minimum, long maximum)
  {
    long toReturn = defaultValue;

    try
    {
      String value = getBackingProperty(name);
      if (value != null)
      {
        toReturn = NumberHelper.boundLong(Long.parseLong(value.trim()), minimum, maximum);
      }
    }
    catch (NumberFormatException exc)
    {
      // Do nothing.
    }

    return toReturn;
  }

  /**
   * Gets an enumerated ("enum") property.
   */
  public <T extends Enum<T>> T getEnum(String name, Class<T> clazz, T defaultValue)
  {
    T toReturn = defaultValue;
    
    String value = getBackingProperty(name);
    if (value != null)
    {
      toReturn = Enum.valueOf(clazz, value.trim().toUpperCase());
    }
    
    return toReturn;
  }

  @Override
  public int getInt(String name)
  {
    return getInt(name, 0);
  }
  
  @Override
  public int getInt(String name, int defaultValue)
  {
    String value = getBackingProperty(name);
    if (StringHelper.isNonEmpty(value))
    {
      return NumberHelper.parseInt(value.trim(), defaultValue);
    }
    else
    {
      return defaultValue;
    }
  }
  
  @Override
  public int getInt(String name, int defaultValue, int minimum, int maximum)
  {
    return NumberHelper.boundInteger(getInt(name, defaultValue), minimum, maximum);
  }

  /**
   * Get a double property.  Defaults to 0.0D.
   */
  public double getDouble(String name)
  {
    return getDouble(name, 0.0D);
  }

  /**
   * Gets a double property.  Returns the default if no property is found.
   */
  public double getDouble(String name, double defaultValue)
  {
    double toReturn = defaultValue;

    try
    {
      String value = getBackingProperty(name);
      if (value != null)
      {
        toReturn = Double.parseDouble(value.trim());
      }
    }
    catch (NumberFormatException exc)
    {
      // Do nothing.
    }

    return toReturn;
  }

  /**
   * Get a float property.  Defaults to 0.0F.
   */
  public float getFloat(String name)
  {
    return getFloat(name, 0.0F);
  }

  /**
   * Gets a float property.  Returns the default if no property is found.
   */
  public float getFloat(String name, float defaultValue)
  {
    float toReturn = defaultValue;

    try
    {
      String value = getBackingProperty(name);
      if (value != null)
      {
        toReturn = Float.parseFloat(value.trim());
      }
    }
    catch (NumberFormatException exc)
    {
      // Do nothing.
    }

    return toReturn;
  }

  @Override
  public boolean getBoolean(String name)
  {
    return getBoolean(name, false);
  }
  
  @Override
  public boolean getBoolean(String name, boolean defaultValue)
  {
    return StringHelper.parseBoolean(getBackingProperty(name), defaultValue);
  }

  /**
   * Converts a comma-delimited value into a String array.
   */
  public String[] getArray(String name, String[] defaultArray)
  {
    String[] toReturn;

    String prop = this.getBackingProperty(name);
    if (StringHelper.isNonEmpty(prop))
    {
      toReturn = prop.split("\\s*,\\s*");
    }
    else
    {
      toReturn = defaultArray;
    }

    return toReturn;
  }
  
  /**
   * getArrayProperty with no default value.
   */
  public String[] getArray(String name)
  {
    return getArray(name, new String[0]);
  }

  /**
   * Overrides the base getProperty method to provide recursive expansion of
   * values based on other values within the same EnhancedProperties.  This
   * allows for the properties file to be used with ${variable} insertion
   * syntax.  For example:
   *   <p>
   * ApplicationRoot = c:\\development\\This Application\\
   * DocumentRoot = ${ApplicationRoot}Document Root\\
   */
  public String getBackingProperty(String name)
  {
    return macroExpand(backing.getProperty(name));
  }
  
  /**
   * Overrides the base getProperty(key, defaultValue) method to provide
   * recursive expansion of values, even within the defaultValue if present.
   */
  public String getBackingProperty(String key, String defaultValue)
  {
    return macroExpand(backing.getProperty(key, defaultValue));
  }

  /**
   * Expands macros in values.
   */
  private String macroExpand(String value)
  {
    String expandedValue = value;
    
    if (expandedValue != null)
    {
      int endLocation;
      String macroName, macroValue;

      // Find the macro beginning marker, ${
      int markerLocation = expandedValue.indexOf(MACRO_START);
      while (markerLocation >= 0)
      {
        // Find the macro ending marker, }
        endLocation = expandedValue.indexOf(MACRO_END, markerLocation);
        if (endLocation >= 0)
        {
          // Get the name to lookup.
          macroName = expandedValue.substring(markerLocation + MACRO_START.length(), endLocation);

          // Look up the value for the name.
          macroValue = getBackingProperty(macroName, "");

          // Put the value into our base value.
          expandedValue = expandedValue.substring(0, markerLocation) + macroValue + expandedValue.substring(endLocation + 1);
        }

        // Move on to see if there are more macros.
        markerLocation = expandedValue.indexOf(MACRO_START);
      }
    }
    
    return expandedValue;
  }

  /**
   * Populate the properties with the parameters from the given command string.
   *
   * @param s the url command string.
   */
  public void loadFromUrlCommandString(String s)
  {
    try
    {
      String s1 = URLDecoder.decode(s, StandardCharsets.UTF_8.name());
      for (StringTokenizer stringtokenizer = new StringTokenizer(s1, "?&"); stringtokenizer.hasMoreElements();)
      {
        String s2 = stringtokenizer.nextToken();
        int i = s2.indexOf('=');
        if (i > 0)
        {
          if (i == s2.length() - 1)
          {
            put(s2.substring(0, i), "");
          }
          else
          {
            put(s2.substring(0, i), s2.substring(i + 1));
          }
        }
        else if (i == -1)
        {
          put(s2, "");
        }
      }
    }
    catch (UnsupportedEncodingException uee)
    {
    }
  }
  
  /**
   * Provides a proxy reference that narrows the view to properties whose
   * names begin with the provided prefix, focusing attention on only those.
   * However, other properties are retained, so variable expansion still works
   * as expected.
   */
  public Focus focus(String prefix)
  {
    return new Focus(prefix);
  }
  
  /**
   * Load from an InputStream.
   */
  public EnhancedProperties load(InputStream stream)
    throws IOException
  {
    backing.load(stream);
    return this;
  }
  
  /**
   * Store to an OutputStream.
   */
  public EnhancedProperties store(OutputStream stream)
    throws IOException
  {
    backing.store(stream, "");
    return this;
  }

  /**
   * Returns a subset of the properties that start with the given prefix.
   * <p>
   * Note that extracting properties is not compatible with the macro-
   * expansion functionality. Once a set of Properties are extracted, macro-
   * expansion may not be able to find the values to use for macros in the
   * filtered subset of properties.
   * <p>
   * In light of the stripping of variables (covered in the paragraph above),
   * I consider this method very dangerous. Avoid extracting properties so
   * that variables work as expected by users of configuration files. For most
   * use cases, the "focus" method is preferred.
   *
   * @param prefix The prefix to filter by.
   * @return The resulting properties that matched the prefix.
   */
  public EnhancedProperties extractProperties(String prefix)
  {
    return this.extractProperties(prefix, false);
  }

  /**
   * Returns a subset of the properties that start with the given prefix.
   * <p>
   * Note that extracting properties is not compatible with the macro-
   * expansion functionality. Once a set of Properties are extracted, macro-
   * expansion may not be able to find the values to use for macros in the
   * filtered subset of properties.
   * <p>
   * In light of the stripping of variables (covered in the paragraph above),
   * I consider this method very dangerous. Avoid extracting properties so
   * that variables work as expected by users of configuration files. For most
   * use cases, the "focus" method is preferred.
   *
   * @param prefix The prefix to filter by.
   * @param strip Should the prefix be stripped of the resulting keys?
   * @return The resulting properties that matched the prefix.
   */
  public EnhancedProperties extractProperties(String prefix, boolean strip)
  {
    EnhancedProperties extractedProps = new EnhancedProperties();

    for (Entry<Object, Object> entry : backing.entrySet())
    {
      if (entry.getKey() instanceof String
          && entry.getValue() instanceof String)
      {
        String key = (String)entry.getKey();
        String value = (String)entry.getValue();

        if (StringHelper.startsWithIgnoreCase(key, prefix)
            && key.length() > prefix.length())
        {
          if (strip)
          {
            key = key.substring(prefix.length());
          }

          extractedProps.put(key, value);
        }
      }
    }

    return extractedProps;
  }

  /**
   * Adds the properties in props.
   *
   * @param props The properties to merge with.
   */
  public void mergeProperties(EnhancedProperties props)
  {
    mergeProperties(props, "");
  }

  /**
   * Adds the properties in props.
   *
   * @param props The properties to merge with.
   * @param prefix The prefix to append onto the keys.
   */
  public void mergeProperties(EnhancedProperties props, String prefix)
  {
    for (Entry<Object, Object> entry : backing.entrySet())
    {
      if (entry.getKey() instanceof String
          && entry.getValue() instanceof String)
      {
        String key = (String)entry.getKey();
        String value = (String)entry.getValue();

        put(prefix + key, value);
      }
    }
  }
  
  /**
   * Gets the number of properties.
   */
  public int size()
  {
    return backing.size();
  }
  
  /**
   * Adds all properties from another EnhancedProperties. 
   */
  public EnhancedProperties putAll(EnhancedProperties other)
  {
    backing.putAll(other.backing);
    return this;
  }
  
  /**
   * Adds all properties from a regular Properties.
   */
  public EnhancedProperties putAll(Properties other)
  {
    backing.putAll(other);
    return this;
  }
  
  //
  // Inner classes.
  //
  
  /**
   * A simple proxy class for getting properties qualified by a provided
   * prefix.
   */
  public class Focus
  {
    private final String prefix;
    
    /**
     * Constructor.
     */
    private Focus(String prefix)
    {
      this.prefix = prefix;
    }
    
    /**
     * Further focus.
     */
    public Focus focus(String furtherFocusingPrefix)
    {
      return new Focus(prefix + furtherFocusingPrefix);
    }
    
    /**
     * Translates a property name to its fully-qualified version.
     */
    public String name(String name)
    {
      return this.prefix + name;
    }
    
    /**
     * Is a property defined?
     */
    public boolean has(String name)
    {
      return EnhancedProperties.this.has(name(name));
    }
    
    /**
     * Gets a property.
     */
    public String get(String name)
    {
      return EnhancedProperties.this.get(name(name));
    }
    
    /**
     * Gets a property.
     */
    public String get(String name, String defaultValue)
    {
      return EnhancedProperties.this.get(name(name), defaultValue);
    }
    
    /**
     * Gets a boolean property.
     */
    public boolean getBoolean(String name, boolean defaultValue)
    {
      return EnhancedProperties.this.getBoolean(name(name), defaultValue);
    }
    
    /**
     * Gets an integer property.
     */
    public int getInt(String name, int defaultValue)
    {
      return EnhancedProperties.this.getInt(name(name), defaultValue);
    }
    
    /**
     * Gets an integer property.
     */
    public int getInt(String name, int defaultValue, int minimum, int maximum)
    {
      return EnhancedProperties.this.getInt(name(name), defaultValue, minimum, maximum);
    }
    
    /**
     * Gets a Long property.
     */
    public long getLong(String name, long defaultValue)
    {
      return EnhancedProperties.this.getLong(name(name), defaultValue);
    }

    /**
     * Gets a Long property.
     */
    public long getLong(String name, long defaultValue, long minimum, long maximum)
    {
      return EnhancedProperties.this.getLong(name(name), defaultValue, minimum, maximum);
    }

    /**
     * Gets an enum property.
     */
    public <T extends Enum<T>> T getEnum(String name, Class<T> clazz, T defaultValue)
    {
      return EnhancedProperties.this.getEnum(name(name), clazz, defaultValue);
    }
  }

}  // End EnhancedProperties
