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

package com.techempower.gemini;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.techempower.*;
import com.techempower.gemini.configuration.*;
import com.techempower.gemini.lifecycle.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * Provides an application-wide configuration facility. Application components
 * implement the Configurable interface and then add themselves to the
 * Configurator via the addConfigurable method. The application is configured
 * at application start time and may be re-configured on-the-fly by a system
 * administrator.
 * <p>
 * Application components implementing the Configurable interface will receive
 * a call to their configure(EnhancedProperties) method, allowing them to read
 * any configuration properties. Components should support run-time
 * re-configuration gracefully. In many cases, that means immediately changing
 * their behavior based on the new configuration; in some cases that may not
 * be possible and the most graceful behavior is to simply keep the component
 * running in the same manner as application start-time.
 * <p>
 * Configurator is an application InitializationTask, meaning it is given the
 * opportunity to load configuration and then remediate if the load fails.
 */
public class Configurator
  implements InitializationTask
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE             = "conf";    // Four-letter component ID
  public static final String CONFIGURATION_FILENAME_EXT = ".conf";
  
  // Special property names.
  public static final String PROP_WEBINF                = "Servlet.WebInf";
  public static final String PROP_CONTEXTNAME           = "Servlet.ContextName";
  public static final String PROP_DEPLOYMENTROOT        = "Servlet.DeploymentRoot";
  public static final String PROP_DOCROOT               = "Servlet.Docroot"; // alias for DeploymentRoot. 
  public static final String PROP_MACHINENAME           = "Servlet.MachineName";
  public static final String PROP_APPROOT               = "Servlet.ApplicationRoot";
  public static final String PROP_APPROOT_LASTDIR       = "Servlet.ApplicationRoot.LastDir";
  public static final String PROP_SERVLETPARAM_PREFIX   = "Servlet.Parameter.";
  public static final String PROP_SERVLETATTRIB_PREFIX  = "Servlet.Attribute.";
  
  public static final String PROP_ENVIRONMENT_PREFIX    = "Environment.";

  //
  // Member variables.
  //

  private final List<Configurable> configurableComponents = new CopyOnWriteArrayList<>();
  private final GeminiApplication  application;
  private final ComponentLog       log;
  private final List<ConfigurationProvider> providers = new ArrayList<>(1);
  
  private boolean              configured  = false;
  private EnhancedProperties   lastProps   = null;

  //
  // Protected methods.
  //

  /**
   * Constructor.
   */
  protected Configurator(GeminiApplication application)
  {
    this.application = application;
    this.log         = application.getLog(COMPONENT_CODE);
    
    addStandardProviders();
    
    // Add ourselves as an application initialization task.
    application.getLifecycle().addInitializationTask(this);
  }
  
  /**
   * Gets the Application reference.
   */
  protected GeminiApplication getApplication()
  {
    return application;
  }
  
  /**
   * Gets the Configurator's ComponentLog reference.
   */
  protected ComponentLog getLog()
  {
    return log;
  }
  
  /**
   * Called by the constructor to add standard ConfigurationProviders.  By
   * default, the Configurator will add the ClusterConfigurationProvider
   * and FileConfigurationProvider.  An application-specific Configurator
   * may overload this method to use different providers.
   */
  protected void addStandardProviders()
  {
    addProvider(new FileConfigurationProvider());
  }

  /**
   * Add a ConfigurationProvider.  Order is important, as providers added
   * earlier will be given an earlier opportunity to provide a suitable
   * configuration for the application.  Once a suitable configuration is
   * provided, the remaining Providers are not queried.
   */
  protected void addProvider(ConfigurationProvider provider)
  {
    providers.add(provider);
  }
  
  //
  // Member methods.
  //
  
  @Override
  public void taskInitialize(GeminiApplication app)
  {
    try
    {
      configureIfNecessary();
      
      if (!configured)
      {
        throw new GeminiInitializationError("Unable to read configuration.");
      }
    }
    catch (Exception exc)
    {
      throw new GeminiInitializationError("Configuration failed.", exc);
    }
  }

  /**
   * Returns the name of the machine running this application, in uppercase.
   *    <p>
   * The machine's name is read from the COMPUTERNAME environment variable, 
   * which is a default variable on Windows and presumably can be easily set 
   * on Linux.
   *    <p>
   * If the COMPUTERNAME environment variable was not present, it will 
   * attempt to find the name of the local host.  This should work by default 
   * on both Windows and Linux.
   *    <p>
   * This method is exposed as a public method so that it can be used by 
   * implementations of ConfigurationProvider. 
   */
  public static String getMachineName()
  {
    // This works on Windows.
    String machineName = System.getenv("COMPUTERNAME");
    
    if (machineName == null)
    {
      try
      {
        // This works on Windows and Linux.
        machineName = java.net.InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException e)
      {
        e.printStackTrace();
      }
    }
    
    if (machineName != null)
    {
      // For consistency between Windows and Linux.
      machineName = machineName.toUpperCase();
    }
    
    return machineName;
  }

  /**
   * Configure the application by loading configuration from the Providers.
   * If none of the ConfigurationProviders can provide a suitable 
   * configuration, this method will return false.  Otherwise, it will 
   * return true.
   *   <p>
   * ConfigurationError may be thrown by any configurable components but the
   * Configurator does not trap those directly.
   */
  public boolean configure()
  {
    // Get a reference to the version.
    final Version version = application.getVersion();

    log.log("Configuring " + version.getAbbreviatedProductName() + ".");

    // Get configuration properties from a provider.
    final EnhancedProperties props = getConfigurationFromProviders();
    
    // Configure if we have received properties.
    if (  (props != null)
       && (props.size() > 0)
       )
    {
      // Add special-purpose configuration properties that are specific
      // to the application's deployment and can be discovered 
      // automatically, such as the location of the WEB-INF directory.
      addStandardProperties(props, application);
      
      // Do the configuration.
      configureWithProps(props, version);
      
      // Set the configured flag.
      configured = true;

      // Copy reference to last known Props.
      lastProps = props;
      
      // Log the completion.
      log.log("Configuration complete.");
      
      // Configuration successful.
      return true;
    }
    else
    {
      log.log("Configuration failed; unable to read configuration file(s).", LogLevel.CRITICAL);
    }
    
    // Not able to configure.
    return false;
  }
  
  /**
   * Configure the provided Configurable on demand with the last-read
   * configuration.  This is functionally identical to calling:
   *   <p>
   * configure(application.getConfigurator().getLastProperties());
   *   <p>
   * from within your Configurable's code.  So instead you can call:
   *   <p>
   * application.getConfigurator().configure(this); 
   */
  public void configure(Configurable configurable)
  {
    configurable.configure(getLastProperties());
  }
  
  /**
   * Creates an EnhancedProperties object pre-populated with some environment
   * variables suitable for providing to the ConfigurationProviders.
   */
  protected EnhancedProperties constructProperties()
  {
    // Create a new properties object for each provider to use.
    final EnhancedProperties props = new EnhancedProperties(application);

    // Add system environment variables with the "Environment." prefix.
    for (Map.Entry<String, String> entry : System.getenv().entrySet())
    {
      props.put(PROP_ENVIRONMENT_PREFIX + entry.getKey(), entry.getValue());
    }
    
    return props;
  }
  
  /**
   * Asks each of the ConfigurationProviders to provide configuration in turn.
   */
  protected EnhancedProperties getConfigurationFromProviders()
  {
    // Ask each provider to load the properties.
    for (ConfigurationProvider provider : providers)
    {
      // Create a new properties object for each provider to use.
      EnhancedProperties props = constructProperties();
      
      boolean loaded = provider.load(application, log, props);
      if (loaded)
      {
        return props;
      }
    }
    
    return null;
  }
  
  /**
   * Adds a set of standard properties to an EnhancedProperties list.  This
   * operation is intended to provide access to deployment-specific 
   * automatically-discovered information such as the location of the
   * WEB-INF directory.
   */
  public void addStandardProperties(EnhancedProperties props, GeminiApplication app)
  {
    final InitConfig config = app.getServletConfig();
    if (config != null)
    {
      // Add initialization parameters with the prefix "ServletParameter."
      Enumeration<String> enumeration = config.getInitParameterNames();
      while (enumeration != null && enumeration.hasMoreElements())
      {
        final String name = enumeration.nextElement();
        props.put(PROP_SERVLETPARAM_PREFIX + name, config.getInitParameter(name));
      }

      // Add server attributes with the prefix "ServletAttribute."
      enumeration = config.getAttributeNames();
      while (enumeration != null && enumeration.hasMoreElements())
      {
        final String name = enumeration.nextElement();
        props.put(PROP_SERVLETATTRIB_PREFIX + name, config.getAttribute(name).toString());
      }

      // Add the context name, deployment root, and WEB-INF locations in the
      // physical file system.
      //props.put(PROP_CONTEXTNAME, config.getServletContextName());
      final String rootRealPath = config.getRealPath("/");
      if (rootRealPath != null)
      {
        props.put(PROP_DEPLOYMENTROOT, rootRealPath);
        // Servlet.Docroot is a convenience alias for Servlet.DeploymentRoot.
        props.put(PROP_DOCROOT, rootRealPath);
      }
      final String webInfRealPath = config.getRealPath("/WEB-INF"); 
      if (webInfRealPath != null)
      {
        props.put(PROP_WEBINF, webInfRealPath);
      }

      // Discover the parent of the deployment root and add that as
      // Servlet.ApplicationRoot.
      try
      {
        final File deployRoot = new File(config.getRealPath("/"));
        String appRootPath = deployRoot.getPath();
        if (!appRootPath.endsWith(File.separator))
        {
          appRootPath = appRootPath + File.separator;
        }
        props.put(PROP_APPROOT, appRootPath);

        final File appRoot = new File(appRootPath);
        props.put(PROP_APPROOT_LASTDIR, appRoot.getName());
      }
      catch (Exception exc)
      {
        // In the event of an exception, use the DeploymentRoot as the
        // Servlet.ApplicationRoot.
        if (config.getRealPath("/") != null)
        {
          String appRootPath = config.getRealPath("/");
          if(!appRootPath.endsWith(File.separator))
          {
            log.log("Servlet.ApplicationRoot did not end with file separator. Adding.", LogLevel.NOTICE);
            appRootPath = appRootPath + File.separator;
          }
          props.put(PROP_APPROOT, appRootPath);
        }
      }
    }

    // Add the machine name, if it could be discovered.
    final String machineName = getMachineName();
    if (machineName != null)
    {
      props.put(PROP_MACHINENAME, machineName);
    }
  }
  
  /**
   * Gets a reference to the last known properties.
   */
  public EnhancedProperties getLastProperties()
  {
    return lastProps;
  }

  /**
   * Only configure if not yet configured.  Returns true if the application
   * is configured (either having been configured previously or as a result
   * of this method call).
   */
  protected boolean configureIfNecessary()
  {
    return isConfigured() || configure();
  }

  /**
   * Is the application configured?  Has the Configurator successfully loaded
   * a configuration at least once?
   */
  public boolean isConfigured()
  {
    return (configured);
  }

  /**
   * Use a Properties object to configure the application.
   */
  protected void configureWithProps(EnhancedProperties props, Version version)
  {
    // Configure the log and the application first.

    // Configure log files.
    application.getApplicationLog().configure(props, version);

    // Configure the application itself.
    application.configure(props);

    // Configure the Configurable components.
    configureConfigurables(props);

    // Do custom configuration.
    customConfiguration(props);
  }
  
  /**
   * Adds a Configurable component to the configurable components list.  Note
   * that the order of Configurable components in this list is not meaningful.
   * When the list of Configurables is being configured, the order of 
   * execution is non-deterministic and spread across several threads.
   *    <p>
   * If custom configuration order is needed, overload the configureWithProps
   * method and call configure() methods manually.
   */
  public void addConfigurable(Configurable configurable)
  {
    configurableComponents.add(configurable);
  }

  /**
   * Removes a Configurable component from the configurable components 
   * list.
   */
  public void removeConfigurable(Configurable configurable)
  {
    configurableComponents.remove(configurable);
  }

  /**
   * Configures each of the components in the Configurables list.
   */
  protected void configureConfigurables(final EnhancedProperties props)
  {
    final Chronograph chrono = new Chronograph("configuration");
    final int size = configurableComponents.size();
    final int workers = NumberHelper.boundInteger(
        Runtime.getRuntime().availableProcessors(), 1, 8);

    log.log("Configuring " + size + " Configurable" 
        + StringHelper.pluralize(size) + " using " + workers + " worker"
        + StringHelper.pluralize(workers) + ". " 
        + chrono, LogLevel.DEBUG);
    
    final CountDownLatch latch = new CountDownLatch(workers);
    final ConcurrentLinkedQueue<Configurable> queue = 
        new ConcurrentLinkedQueue<>(configurableComponents);
    
    for (int i = 0; i < workers; i++)
    {
      final int workerId = i + 1;
      ThreadHelper.submit(() -> {
        final Chronograph cwc = new Chronograph("CW" + workerId);
        Configurable configurable;
        do {
          configurable = queue.poll();
          if (configurable != null)
          {
            log.log("CW" + workerId + " configuring " 
                + configurable + ".", LogLevel.DEBUG);
            configurable.configure(props);
          }
        }
        while (configurable != null);
        latch.countDown();
        log.log("CW" + workerId + " done. " + cwc, LogLevel.DEBUG);
      });
    }
    
    try
    {
      latch.await();
    }
    catch (InterruptedException iexc) { }
    
    log.log("Done configuring Configurables. " + chrono, LogLevel.DEBUG);
  }
  
  /**
   * Perform any custom configuration for this application.  Specific
   * applications should overload this method to provide configuration
   * to custom components.
   *    <p>
   * Note that any subclass of Configurator must configure the application
   * infrastructure.  If there is no application infrastructure, you must
   * configure the BasicInfrastructure!
   */
  protected void customConfiguration(EnhancedProperties props)
  {
    // Optionally overload this method.
  }

}   // End Configurator.
