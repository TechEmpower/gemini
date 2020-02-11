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

import java.io.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The standard implementation of the ConfigurationProvider interface, this
 * loads configuration files from disk attempting a variety of locations.
 * <p>
 * It also provides a mechanism to load a configuration file from a URL,
 * presumably as hosted by a private web server. However, this function was
 * used for cluster configuration prior to the introduction of the current
 * cluster master server. Loading a configuration via URL is now considered
 * deprecated behavior.
 * <p>
 * One or more configuration files allow application behavior to be changed on
 * a deployment-environment basis. If you are using the BasicAdminHandler's
 * reconfigure functionality, all components that are configurable must be
 * capable of reconfiguring while running.
 * <p>
 * The configuration files' location can be specified by the Servlet parameter
 * named "ConfigurationFile". A comma-delimited list of filenames can be
 * provided to make use of the "precedence" mechanism. If a comma- delimited
 * list of configuration files is provided, the later files have higher
 * precedence than the earlier files. For example, the order is typically a
 * base configuration followed by an environment-specific configuration such
 * as: "Base.conf,Dev.conf"
 * <p>
 * For each of the files in the comma-delimited list, the Configurator will
 * attempt to locate the file according to the rules below.
 * <ul>
 * <li>Current directory; that is, the directory from which the application
 * was started. For example, if using Resin, this is the directory Resin was
 * started from.
 * <li>As a resource: Meaning the application will attempt to load the file
 * from within WEB-INF.
 * <li>Classpath, in order; any non-jar/non-zip classpath entry will be
 * searched for the configuration file.
 * </ul>
 * <p>
 * If the location of the configuration file is specified by the Servlet
 * parameter described above, the location can be a URL, meaning the
 * configuration file will be loaded via HTTP. A comma-delimited list of URLs
 * can also be used.
 * <p>
 * Configuration files may contain a special property named "Extends". The
 * value of this property should be the name of another configuration file.
 * Properties from the current file will be applied on top of those from the
 * specified file, similar to the "precedence" mechanism described earlier.
 * For example, <code>Dev.conf</code> could contain
 * <code>Extends = Base.conf</code>. Then, if the list of configuration
 * files specified in web.xml is simply "Dev.conf", it will behave as if
 * the list was "Base.conf,Dev.conf".
 * <p>
 * Finally, if the location of the configuration file is not specified by the
 * Servlet parameter, the constructed default name will be a single
 * configuration file named as such:
 * <p>
 * [app-short-name]-[machine-name].conf
 * <p>
 * For example:
 * <p>
 * Tempest-BLACKPARK.conf
 * <p>
 * This, combined with the "Extends" property, allows you to have baseline and
 * machine-specific configuration without specifying any machine-specific
 * settings within web.xml.
 */
public class FileConfigurationProvider
    implements ConfigurationProvider
{
  private final Logger log = LoggerFactory.getLogger(Configurator.COMPONENT_CODE);

  /**
   * Constructor.
   */
  public FileConfigurationProvider()
  {
    // Does nothing.
  }

  //
  // Methods.
  //

  /**
   * Attempt to load a configuration file from disk.
   */
  @Override
  public boolean load(GeminiApplication application,
                      EnhancedProperties props)
  {
    try
    {
      // Get the configuration filename(s).
      String filenames = getConfigFilenames(application);
      StringTokenizer tokenizer = new StringTokenizer(filenames, ",");
      while (tokenizer.hasMoreTokens())
      {
        // Trim off extra spaces.
        String filename = tokenizer.nextToken().trim();
        loadPropertiesFromFile(application, props, filename);
      }

      // Look for extended configuration properties.
      loadExtendedProperties(application, props);

      // Did we load anything?
      return (props.size() > 0);
    }
    catch (IOException ioexc)
    {
      log.error("IOException while loading configuration files: ", ioexc);
    }

    // If we get here, we did not load properties successfully.
    return false;
  }

  /**
   * Modifies the given properties from the given configuration file.
   */
  protected void loadPropertiesFromFile(GeminiApplication application,
      EnhancedProperties props, String filename)
      throws IOException
  {
    boolean loaded = false;
    // Try getting the configuration file as a resource stream via the
    // classloader.
    if (!loaded)
    {
      try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(
          filename))
      {
        if (is != null)
        {
          // Get the configuration file from the classloader.
          props.load(is);
          loaded = true;
        }
      }
    }

    // Try getting the configuration file as a resource stream via the
    // classloader under configuration.
    if (!loaded)
    {
      try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(
          "configuration/" + filename))
      {
        if (is != null)
        {
          // Get the configuration file from the classloader.
          props.load(is);
          loaded = true;
        }
      }
    }

    // Try getting the configuration file as a resource stream via the
    // application server. For this mode, we assume that the file is
    // stored within the WEB-INF directory.
    if (!loaded)
    {
      // Try the "WEB-INF/configuration" directory first, and if that is
      // not successful, try the root of WEB-INF.
      try (InputStream is = application.getResourceAsStream("configuration/"
          + filename))
      {
        if (is != null)
        {
          props.load(is);
          loaded = true;
        }
      }

      if (!loaded)
      {
        try (InputStream is = application.getResourceAsStream(filename))
        {
          if (is != null)
          {
            props.load(is);
            loaded = true;
          }
        }
      }
    }

    // Oops, not in the classloader and not available as an application
    // resource. Let's try using the filename as if it's an absolute path.
    if (!loaded)
    {
      // Do not allow an exception to be thrown if the file doesn't exist.
      // We are permissive of missing files because we're attempting to find
      // the location.
      final File file = new File(filename);
      if (file.exists())
      {
        try (InputStream is = new FileInputStream(file))
        {
          props.load(is);
          loaded = true;
        }
      }
    }

    // Now Let's try using the filename under the configuration folder
    if (!loaded)
    {
      final File file = new File("configuration/" + filename);
      if (file.exists())
      {
        try (InputStream is = new FileInputStream(file))
        {
          props.load(is);
          loaded = true;
        }
      }
    }

    // Perhaps it is not an absolute path? Let's try making it one.
    if (!loaded)
    {
      // Do not allow an exception to be thrown if the file doesn't exist;
      // We are permissive of missing files because we're attempting to find
      // the location.
      final File file = new File(new File(".").getCanonicalPath()
          + File.separator + filename);
      if (file.exists())
      {
        try (InputStream is = new FileInputStream(file))
        {
          props.load(is);
          loaded = true;
        }
      }
    }

    if (loaded)
    {
      log.info("Read {}", filename);
    }
  }

  /**
   * Modifies the given properties based on its "Extends" property, which is
   * used to specify a configuration filename that these properties extend.
   */
  protected void loadExtendedProperties(GeminiApplication application,
      EnhancedProperties props)
      throws IOException
  {
    for (String extendedFilename : props.getArray("Extends"))
    {
      EnhancedProperties extendedProps = new EnhancedProperties(application);
      loadPropertiesFromFile(application, extendedProps,
          extendedFilename);

      // Recursively load the extended properties.
      loadExtendedProperties(application, extendedProps);

      // The extended properties should have be overwritten by these
      // properties. Apply these properties on top of the extended
      // ones, then put that result back into these properties.
      extendedProps.putAll(props);
      props.clear();
      props.putAll(extendedProps);
    }
  }

  /**
   * Gets the name of the configuration file(s).
   */
  protected String getConfigFilenames(GeminiApplication application)
  {
    String filenames;

    if (application.getServletConfigParameter(GeminiConstants.CONFIGURATION_FILE) != null)
    {
      // Use configuration file names specified in the servlet configuration.
      filenames = application.getServletConfigParameter(GeminiConstants.CONFIGURATION_FILE);
      log.info("Using specified configuration file(s): {}", filenames);
    }
    else if (System.getProperty(GeminiConstants.CONFIGURATION_FILE) != null)
    {
      // Use configuration file names specified in a system property.
      filenames = System.getProperty(GeminiConstants.CONFIGURATION_FILE);
      log.info("Using specified configuration file(s): {}", filenames);
    }
    else if (System.getenv(GeminiConstants.CONFIGURATION_FILE) != null)
    {
      // Use configuration file names specified in an environment variable
      filenames = System.getenv(GeminiConstants.CONFIGURATION_FILE);
      log.info("Using specified configuration file(s): {}", filenames);
    }
    else
    {
      // Attempt to load a machine-specific configuration file.
      //
      // Format: <machinename>.conf
      // Example: BLACKPARK.conf

      String machineName = Configurator.getMachineName();

      // Load <baseConfigurationFile>.conf first, then <machinename>.conf to allow
      // the machine-specific file to override a base configuration.
      filenames = GeminiConstants.BASE_CONFIGURATION_FILE + Configurator.CONFIGURATION_FILENAME_EXT
          + "," + machineName + Configurator.CONFIGURATION_FILENAME_EXT;
      log.info("Using default configuration file(s): {}", filenames);
    }

    return filenames;
  }

}
