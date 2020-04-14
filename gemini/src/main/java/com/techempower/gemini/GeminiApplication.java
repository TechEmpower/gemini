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

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.techempower.TechEmpowerApplication;
import com.techempower.cache.EntityStore;
import com.techempower.data.ConnectorFactory;
import com.techempower.data.EntityUpdater;
import com.techempower.gemini.data.DatabaseMigrator;
import com.techempower.gemini.data.FlywayMigrator;
import com.techempower.gemini.email.EmailTransport;
import com.techempower.gemini.email.inbound.EmailDispatcher;
import com.techempower.gemini.email.outbound.EmailServicer;
import com.techempower.gemini.email.outbound.EmailTemplater;
import com.techempower.gemini.feature.BasicFeatureManager;
import com.techempower.gemini.feature.FeatureManager;
import com.techempower.gemini.internationalization.GeminiLocaleManager;
import com.techempower.gemini.lifecycle.GeminiInitializationError;
import com.techempower.gemini.lifecycle.InitConfigurationCheck;
import com.techempower.gemini.lifecycle.InitDatabaseConnectionTest;
import com.techempower.gemini.lifecycle.InitDatabaseMigrations;
import com.techempower.gemini.lifecycle.InitDisplayBanner;
import com.techempower.gemini.lifecycle.InitDisplayEnvironment;
import com.techempower.gemini.lifecycle.InitEntityStore;
import com.techempower.gemini.lifecycle.InitManageAsynchronous;
import com.techempower.gemini.lifecycle.InitPrepareForSoftKill;
import com.techempower.gemini.lifecycle.InitRegister;
import com.techempower.gemini.lifecycle.InitStartupNotification;
import com.techempower.gemini.lifecycle.InitializationTask;
import com.techempower.gemini.lifecycle.ShutdownTask;
import com.techempower.gemini.log.GeminiComponentLog;
import com.techempower.gemini.log.GeminiLog;
import com.techempower.gemini.monitor.GeminiMonitor;
import com.techempower.gemini.mustache.MustacheManager;
import com.techempower.gemini.notification.Notifier;
import com.techempower.gemini.notification.listener.EmailNotificationListener;
import com.techempower.gemini.pyxis.PyxisSecurity;
import com.techempower.gemini.session.SessionManager;
import com.techempower.gemini.simulation.SimSessionManager;
import com.techempower.helper.CollectionHelper;
import com.techempower.helper.ImageHelper;
import com.techempower.helper.JvmImageHelper;
import com.techempower.helper.StringHelper;
import com.techempower.helper.ThreadHelper;
import com.techempower.helper.ThrowableHelper;
import com.techempower.js.JacksonJavaScriptReader;
import com.techempower.js.JacksonJavaScriptWriter;
import com.techempower.js.JavaScriptReader;
import com.techempower.js.JavaScriptWriter;
import com.techempower.log.ComponentLog;
import com.techempower.log.Log;
import com.techempower.log.LogLevel;
import com.techempower.util.Chronograph;
import com.techempower.util.Configurable;
import com.techempower.util.EnhancedProperties;
import com.techempower.util.UtilityConstants;

/**
 * A repository for reaching references to application infrastructure
 * objects such as the Infrastructure, Dispatchers, and Contexts.
 *   <p>
 * As of Gemini 1.1, this class is a focal point of custom application
 * overloading.  Previously, many subclasses provided many methods
 * similar to those in this class.  The GeminiApplication subclass is
 * intended to fulfill this role as much as possible.  Of course, other
 * classes still need to provide references to this object.
 *   <p>
 * Another goal was to provide the ability to have more than a single
 * Gemini-based application running on a single servlet container.
 *    <p>
 * Reads the following configuration options from the .conf file:
 *   <ul>
 * <li>startup-notification - Yes/No - Should a notification be sent when
 *     the application starts up?
 * <li>MaxUploadSize - the maximum number of bytes allowed for a multi-part
 *     HTTP upload.
 * <li>MaxUploadInMemorySize - the maximum number of bytes to store in memory
 *     prior to saving a temporary file to disk.  This value can be the same
 *     as the MaxUploadSize.
 * <li>UploadDir - Used if the MaxUploadSize is greater than the 
 *     MaxUploadInMemorySize for storing temporary files.
 * <li>RequestCounting - Disabled by default; enable this to count all 
 *     requests, assign an ID number to each request, and attach the ID number
 *     to the current thread's name. 
 *   </ul>
 *   <p>
 * Gemini Applications start up with the following steps:
 *   1. Initialize this Servlet.
 *   2. Configure the application.
 *   3. Run initialization tasks.
 *   4. Begin accepting requests and start asynchronous resources.
 *   <p>
 * You will see these steps in InfrastructureServlet.init().
 */
public abstract class GeminiApplication
  extends    TechEmpowerApplication
  implements GeminiApplicationInterface,
             Configurable
{
  
  //
  // Constants.
  //
  
  public static final String FEATURE_STARTUP_NOTE = "startup-notification";
  public static final Chronograph STARTUP_CHRONOGRAPH = new Chronograph("Startup chronograph"); 

  //
  // Initialization/running states.
  //
  
  /**
   * The operational state of the application, where NEW means a new instance
   * that has not yet started servicing requests, RUNNING means an instance
   * that is servicing requests, STOPPED means an instance that was servicing
   * requests that has since shut down, and FAILED means an instance that
   * tried to initialize but failed to begin servicing requests. 
   */
  public enum OperationalState { NEW, RUNNING, STOPPED, FAILED }
  private OperationalState state = OperationalState.NEW;
    
  //
  // Member variables.
  //

  private final ComponentLog               log;
  private       InitConfig                 initConfig;
  private final BasicInfrastructure        infrastructure;
  private final Configurator               configurator;
  private final SessionManager             sessionManager;
  private final SimSessionManager          simSessionManager;
  private final Dispatcher                 dispatcher;
  private final ConnectorFactory           connectorFactory;
  private final DatabaseMigrator           databaseMigrator;
  private final EmailServicer              emailServicer;
  private final EmailTemplater             emailTemplater;
  private final EmailTransport             emailTransport;
  private final ImageHelper                imageHelper;
  private final EntityStore                entityStore;
  private final GeminiLocaleManager        localeManager;
  private final EntityUpdater              entityUpdater;
  private final GeminiMonitor              monitor;
  private final PyxisSecurity              security;
  private final FeatureManager             featureManager;
  private final Notifier                   notifier;
  private final JavaScriptWriter           standardJsw;
  private final JavaScriptReader           standardJsr;
  private final MustacheManager            mustacheManager;
  private final Lifecycle                  lifecycle;
  private       RequestListener[]          listeners       = new RequestListener[0];

  private String                     administratorEmail    = "";
  
  // Simple request monitoring.
  private boolean                    requestCounting       = false;
  private final AtomicLong           requestNumber         = new AtomicLong(0L);

  private Chronograph                chronograph; 

  // Character encoding preferences.
  private String                     defaultResponseType = "text/html;charset=utf-8";
  private Charset                    defaultResponseCharset = null;
  private Charset                    defaultRequestCharset = null;
  
  // Instance number, used for distributed applications.
  private int                        instanceNumber        = 0;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public GeminiApplication()
  {
    super();

    // Construct a ComponentLog.
    try
    {
      this.log = getLog("gemi");
    }
    catch (Exception exc)
    {
      throw new GeminiInstantiationError("Failed to instantiate log.", exc);
    }

    try
    {
      // The following methods are available for overloading by the 
      // application.  Many applications will provide custom subclasses of 
      // the standard Gemini components (e.g., a custom EntityStore returned
      // by constructEntityStore).  The order is important since items
      // constructed earlier will be available to those constructed later.
      this.lifecycle            = constructLifecycle();
      this.configurator         = constructConfigurator();
      this.standardJsw          = constructJavaScriptWriter();
      this.standardJsr          = constructJavaScriptReader();
      this.featureManager       = constructFeatureManager();
      this.connectorFactory     = constructConnectorFactory();
      this.databaseMigrator     = constructDatabaseMigrator();
      this.notifier             = constructNotifier();
      this.monitor              = constructMonitor();
      this.infrastructure       = constructInfrastructure();
      this.entityStore          = constructEntityStore();
      this.security             = constructSecurity();
      this.emailTemplater       = constructEmailTemplater();
      this.emailTransport       = constructEmailTransport();
      this.imageHelper          = constructImageHelper();
      this.emailServicer        = constructEmailServicer();
      this.mustacheManager      = constructMustacheManager();
      this.dispatcher           = constructDispatcher();
      this.sessionManager       = constructSessionManager();
      this.simSessionManager    = constructSimSessionManager();
      this.localeManager        = constructLocaleManager();
      this.entityUpdater        = constructEntityUpdater();
  
      final InitDisplayBanner banner = new InitDisplayBanner(); 
      lifecycle.addInitializationTask(banner);
      lifecycle.addShutdownTask(banner);
      
      // Add the start-up notification feature.
      getFeatureManager().add(FEATURE_STARTUP_NOTE, "Send application start-up notification", true);

      // Add some configurables that do not add themselves.
      configurator.addConfigurable(connectorFactory);
      configurator.addConfigurable(entityStore);
      
      // Add the default initialization tasks.
      lifecycle.addLifecycleTasks();
    }
    catch (Exception exc)
    {
      setState(OperationalState.FAILED);
      log.log("Failed to instantiate application.", LogLevel.CRITICAL, exc);
      throw new GeminiInstantiationError("Failed to instantiate application.", exc);
    }
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    if (props.get("StartupMailRecipients") != null)
    {
      log.log("Property \"StartupMailRecipients\" is deprecated.");
    }
    if (props.get("StartupMailAuthor") != null)
    {
      log.log("Property \"StartupMailAuthor\" is deprecated.");
    }

    administratorEmail = props.get("AdministratorEmail", administratorEmail);
    requestCounting    = props.getBoolean("RequestCounting", requestCounting);


    // Read the deployment description (e.g., Production, Test, Development)
    // and store this into the Version object.
    getVersion().setDeploymentDescription(
        props.get(GeminiConstants.PROP_DEPLOYMENT_DESCRIPTION, "Unspecified Deployment"),
        Configurator.getMachineName());
    
    configureCharacterSets(props);

    // Get the instance number for this particular instance of the application.
    instanceNumber = props.getInt(GeminiConstants.PROP_INSTANCE_NUMBER, instanceNumber);
    instanceNumber = props.getInt(GeminiConstants.PROP_SPAWNED_INSTANCE, instanceNumber);

    // Attach the monitor listener
    if ((monitor != null) && (monitor.getListener() != null))
    {
      addRequestListener(monitor.getListener());
    }
  }
  
  /**
   * Configure the default character sets.
   */
  protected void configureCharacterSets(EnhancedProperties props)
  {
    // This legacy configuration parameter is used first.
    String charsetName = props.get("DefaultCharacterSet");
    
    final EnhancedProperties.Focus focus = props.focus("Encoding.");
    charsetName = focus.get("Charset", charsetName);  // Allow the request and response to be configured using a single parameter.
    final String responseCharsetName = focus.get("ResponseCharset", charsetName);
    if (StringHelper.isNonEmpty(responseCharsetName))
    {
      try
      {
        defaultResponseCharset = Charset.forName(responseCharsetName);
      }
      catch (Exception exc)
      {
        log.log("No matching character set for name " + responseCharsetName, exc);
        defaultResponseCharset = null;
      }
    }
    
    final String requestCharsetName = focus.get("RequestCharset", charsetName);
    if (StringHelper.isNonEmpty(requestCharsetName))
    {
      try
      {
        defaultRequestCharset = Charset.forName(requestCharsetName);
      }
      catch (Exception exc)
      {
        log.log("No matching character set for name " + requestCharsetName, exc);
        defaultRequestCharset = null;
      }
    }
    
    // Use UTF-8 response type by default.
    defaultResponseType = focus.get("ResponseType", defaultResponseType);
  }

  /**
   * Initialize the application in preparation for processing requests.  This
   * is typically called by the InfrastructureServlet when the Servlet 
   * container calls InfrastructureServlet.init. 
   *   <p>
   * Gemini Applications start up with the following steps:
   *   1. Collect configuration information from the execution environment,
   *      such as from the Servlet container.
   *   2. Ask each InitializationTask to initialize, which includes running
   *      the Configurator.
   *   3. Begin accepting requests and start asynchronous resources.
   *   
   * @param config an InitConfig object providing either a ServletConfiguration
   *   or an approximation of the same (@see InitConfig).
   */
  public void initialize(InitConfig config)
  {
    // Copy configuration information from the execution environment
    // (e.g., Servlet container or simulation).
    initConfig = config;
    
    lifecycle.start();
  }

  /**
   * Stops the Gemini Application.  Stops asynchronous resources.
   * Deregisters this application with the ApplicationRegistrar.
   */
  public void end()
  {
    setState(OperationalState.STOPPED);

    getLifecycle().runShutdownTasks();
  }
  
  /**
   * Returns the running flag, indicating that requests can be serviced by
   * the application.  The running flag will be false if the application has
   * been ended (end method above) or was never started correctly, such as
   * in the case of a failed configuration.
   */
  public boolean isRunning()
  {
    return (getState() == OperationalState.RUNNING);
  }
  
  /**
   * Gets the state of the application.
   */
  public OperationalState getState()
  {
    return state;
  }
  
  /**
   * Sets the operational state of the application.
   */
  protected void setState(OperationalState state)
  {
    this.state = state;

    if (state == OperationalState.RUNNING)
    {
      // Capture the application start time.
      chronograph = new Chronograph(getVersion().getProductName());
    }
  }
  
  /**
   * Should inbound requests be counted and assigned ID numbers?
   */
  public boolean isRequestCounting()
  {
    return requestCounting;
  }

  /**
   * Gets the total up-time (so far if still running, or until stopped
   * if stopped) in milliseconds.  Returns 0L if the application has not
   * been started.
   */
  public long getUptime()
  {
    if (chronograph != null)
    {
      return chronograph.elapsed();
    }
    else
    {
      return 0L;
    }
  }
  
  /**
   * Add an initialization task.
   * 
   * @deprecated Use getLifecycle().addInitializationTask instead.
   */
  @Deprecated
  public void addInitializationTask(InitializationTask task)
  {
    getLifecycle().addInitializationTask(task);
  }
  
  /**
   * Add a shutdown task.
   * 
   * @deprecated Use getLifecycle().addShutdownTask instead.
   */
  @Deprecated
  public void addShutdownTask(ShutdownTask task)
  {
    getLifecycle().addShutdownTask(task);
  }

  /**
   * Adds a RequestListener.  This method is not thread-safe, but we expect
   * that listeners are attached very infrequently and most likely only at
   * application start-up.
   */
  public void addRequestListener(RequestListener listener)
  {
    // Only proceed if we're adding a RequestListener we're not already
    // aware of.
    if ((listeners.length == 0)
        || (!CollectionHelper.arrayContains(listeners, listener)))
    {
      // Create space for the listener in a new array and then replace
      // the current array.
      RequestListener[] temp = new RequestListener[listeners.length + 1];
      System.arraycopy(listeners, 0, temp, 0, listeners.length);
      temp[temp.length - 1] = listener;
      listeners = temp;
    }
  }

  /**
   * Gets the application instance number for this particular running instance
   * of the application.  This is used by the "distributed" sub-package 
   * although it is not required that an application use the distributed
   * functionality.
   *   <p>
   * The configuration file can specify an instance number for an application
   * by setting the "ApplicationInstanceNumber".
   *   <p>
   * An application instance number of 0 (the default value) indicates that
   * this instance is either the master instance -or- that this application
   * is not distributed at all.
   */
  public int getApplicationInstanceNumber()
  {
    return instanceNumber;
  }
  
  /**
   * Construct a Lifecycle management object for this application.  The
   * default implementation is a member inner class, but that may be
   * subclassed for customization. 
   */
  protected Lifecycle constructLifecycle()
  {
    return new Lifecycle(this);
  }
  
  /**
   * Gets a reference to the Lifecycle
   */
  public Lifecycle getLifecycle()
  {
    return lifecycle;
  }

  /**
   * Construct an Infrastructure implementation.
   */
  protected BasicInfrastructure constructInfrastructure()
  {
    return new BasicInfrastructure(this);
  }

  @Override
  protected Log constructLog()
  {
    return new GeminiLog(this);
  }

  /**
   * Construct a Configurator.
   */
  protected Configurator constructConfigurator()
  {
    return new Configurator(this);
  }

  /**
   * Construct the application's request Dispatcher.
   */
  protected abstract Dispatcher constructDispatcher();

  /**
   * Construct a database ConnectorFactory reference. E.g., BasicConnectorFactory
   * from the gemini-jdbc package or HikariCPConnectorFactory from
   * gemini-hikaricp.
   */
  protected abstract ConnectorFactory constructConnectorFactory();

  /**
   * Get a reference to the DatabaseMigrator implementation.
   */
  public DatabaseMigrator getDatabaseMigrator()
  {
    return databaseMigrator;
  }

  /**
   * Construct a DatabaseMigrator to use in the InitDatabaseMigrations
   * initialization task. By default Flyway is used. Overload to provide another
   * implementation.
   */
  protected DatabaseMigrator constructDatabaseMigrator()
  {
    return new FlywayMigrator(this);
  }

  /**
   * Construct a Notifier for use by the application.  By default, a standard
   * Notifier is constructed with an EmailNotificationListener attached.  
   * Overload to customize listeners.
   */
  protected Notifier constructNotifier()
  {
    final Notifier toReturn = new Notifier(this);
    toReturn.addListener(new EmailNotificationListener(this));
    return toReturn;
  }

  /**
   * Construct and configure a JavaScriptWriter suitable for application-wide
   * general usage.
   */
  protected JavaScriptWriter constructJavaScriptWriter()
  {
    return new JacksonJavaScriptWriter();
  }

  /**
   * Construct and configure a JavaScriptReader suitable for application-wide
   * general usage.
   */
  protected JavaScriptReader constructJavaScriptReader()
  {
    return new JacksonJavaScriptReader();
  }

  /**
   * Construct a MustacheManager for interfacing with the Java Mustache 
   * template library. 
   */
  protected abstract MustacheManager constructMustacheManager();
  
  /**
   * Constructs an HttpSessionManager.
   */
  protected abstract SessionManager constructSessionManager();

  /**
   * Constructs a SimSessionManager.
   */
  protected SimSessionManager constructSimSessionManager()
  {
    return new SimSessionManager(this);
  }

  /**
   * Constructs an EmailServicer.  Applications do not typically overload 
   * this method.
   *   <p>
   * Note: it is acceptable to return null if no e-mail servicing is
   * required.
   */
  protected EmailServicer constructEmailServicer()
  {
    return new EmailServicer(this);
  }

  /**
   * Constructs an EmailTransport.  Applications do not typically overload 
   * this method.
   *   <p>
   * Note: it is acceptable to return null if no e-mail transport is
   * required.
   */
  protected EmailTransport constructEmailTransport()
  {
    return new EmailTransport(this);
  }

  /**
   * Constructs an EmailTemplater.
   *   <p>
   * Note: it is acceptable to return null if no e-mail templating is
   * required.  The default implementation does exactly that.
   */
  protected EmailTemplater constructEmailTemplater()
  {
    // Normally, an application would return a custom templater, like below:
    //return new MyApplicationEmailTemplater(this);

    return null;
  }

  /**
   * Construct an EntityStore.
   *   <p>
   * Note: it is acceptable to return null if no database connectivity is 
   * used.
   */
  protected EntityStore constructEntityStore()
  {
    return new EntityStore(this, getConnectorFactory());
  }
  
  /**
   * Construct a PyxisSecurity implementation.
   *   <p>
   * Note: it is acceptable to return null if no user authentication is used.
   */
  protected PyxisSecurity constructSecurity()
  {
    return null;
  }
  
  /**
   * Construct a FeatureManager.  By default, a BasicFeatureManager is
   * constructed, but an application may optionally use a custom 
   * implementation. 
   */
  protected FeatureManager constructFeatureManager()
  {
    return new BasicFeatureManager(this);
  }

  /**
   * Construct a GeminiLocaleManager object for managing locales in an 
   * internationalized application.  Do not overload this method if
   * locale-awareness is not important to your application.
   */
  protected GeminiLocaleManager constructLocaleManager()
  {
    return new GeminiLocaleManager(this);
  }
  
  /**
   * Construct an EntityUpdater.
   */
  protected EntityUpdater constructEntityUpdater()
  {
    return new EntityUpdater(this, getConnectorFactory());
  }
  
  /**
   * Constructs a GeminiMonitor reference.
   */
  protected abstract GeminiMonitor constructMonitor();

  /**
   * Construct an ImageHelper. Returns a JvnImageHelper by default but can
   * be overridden to return an ImageMagickHelper.
   */
  protected ImageHelper constructImageHelper()
  {
    return new JvmImageHelper(this);
  }
  
  /**
   * Construct a Context for a request.  Some applications overload this
   * to provide a custom subclass of Context.
   */
  @Override
  public abstract Context getContext(Request request);

  /**
   * Gets the application's Infrastructure.
   */
  @Override
  public BasicInfrastructure getInfrastructure()
  {
    return infrastructure;
  }
  
  /**
   * Gets the FeatureManager.
   */
  public FeatureManager getFeatureManager()
  {
    return featureManager;
  }
  
  /**
   * Gets the MustacheManager.
   */
  public MustacheManager getMustacheManager()
  {
    return mustacheManager;
  }
  
  /**
   * Gets the Notifier.
   */
  public Notifier getNotifier()
  {
    return notifier;
  }
  
  /**
   * Gets the application's standard JavaScriptWriter.  Note that an 
   * application may use additional JavaScriptWriters for other contexts.  
   * The application's main JavaScriptWriter will be used as the default.
   */
  public JavaScriptWriter getJavaScriptWriter()
  {
    return standardJsw;
  }

  /**
   * Gets the application's standard JavaScriptReader.  Note that an
   * application may use additional JavaScriptReaders for other contexts.
   * The application's main JavaScriptReader will be used as the default,
   * for example when parsing a JSON request body.
   */
  public JavaScriptReader getJavaScriptReader()
  {
    return standardJsr;
  }

  /**
   * Gets the Configurator.
   */
  @Override
  public Configurator getConfigurator()
  {
    return configurator;
  }

  /**
   * Gets the request Dispatcher.
   */
  @Override
  public Dispatcher getDispatcher()
  {
    return dispatcher;
  }

  /**
   * Gets the application's PyxisSecurity implementation.
   */
  @Override
  public PyxisSecurity getSecurity()
  {
    return security;
  }
  
  /**
   * Gets the database connector factory.
   */
  @Override
  public ConnectorFactory getConnectorFactory()
  {
    return connectorFactory;
  }

  /**
   * Gets the Email Dispatcher.  Returns null if this application does not
   * use an EmailDispatcher.
   */
  public EmailDispatcher getEmailDispatcher()
  {
    return null;
  }
  
  /**
   * Gets the Gemini Monitor.  Returns null if this application does not use
   * a Gemini Monitor.
   */
  public GeminiMonitor getMonitor()
  {
    return monitor;
  }

  /**
   * Gets the EmailServicer.
   */
  @Override
  public EmailServicer getEmailServicer()
  {
    return emailServicer;
  }

  /**
   * Gets the EmailTransport.
   */
  @Override
  public EmailTransport getEmailTransport()
  {
    return emailTransport;
  }

  /**
   * Gets the EmailTemplater.
   */
  @Override
  public EmailTemplater getEmailTemplater()
  {
    return emailTemplater;
  }

  /**
   * Gets the EntityStore.
   */
  @Override
  public EntityStore getStore()
  {
    return entityStore;
  }

  /**
   * Gets the default response type.  Set by Encoding.ResponseType.  The
   * standard default is "text/html;charset=utf-8".
   */
  public String getDefaultResponseType()
  {
    return defaultResponseType;
  }

  /**
   * Is a default response character set specified?
   */
  public boolean isDefaultResponseCharsetSpecified()
  {
    return (defaultResponseCharset != null);
  }
  
  /**
   * Gets the default response character set.  The system's default character
   * set will be returned if no character set has been specified in the
   * application's configuration file.
   */
  public Charset getDefaultResponseCharset()
  {
    return defaultResponseCharset == null 
        ? Charset.defaultCharset() 
        : defaultResponseCharset;
  }

  /**
   * Is a default request character set specified?
   */
  public boolean isDefaultRequestCharsetSpecified()
  {
    return (defaultRequestCharset != null);
  }
  
  /**
   * Gets the default request character set.  The system's default character
   * set will be returned if no character set has been specified in the
   * application's configuration file.
   */
  public Charset getDefaultRequestCharset()
  {
    return defaultRequestCharset == null 
        ? Charset.defaultCharset() 
        : defaultRequestCharset;
  }

  /**
   * Gets the GeminiLocaleManager.
   */
  @Override
  public GeminiLocaleManager getLocaleManager()
  {
    return localeManager;
  }

  /**
   * Gets the SessionManager.
   */
  @Override
  public SessionManager getSessionManager()
  {
    return sessionManager;
  }

  /**
   * Gets the SimSessionManager.
   */
  public SimSessionManager getSimSessionManager()
  {
    return simSessionManager;
  }

  /**
   * Gets the ServletConfig that was provided to the Servlet when its init()
   * method was called.
   */
  @Override
  public InitConfig getServletConfig()
  {
    return initConfig;
  }

  /**
   * Gets a ServletConfig parameter based on its name.  This is a
   * convenience pass-through method to ServletConfig.getInitParameter.
   */
  public String getServletConfigParameter(String name)
  {
    if (getServletConfig() != null)
    {
      return getServletConfig().getInitParameter(name);
    }
    return null;
  }
  
  /**
   * Opens an InputStream to a web application resource.  Returns null if
   * the file cannot be found.
   */
  public InputStream getResourceAsStream(String filename)
  {
    if (getServletConfig() != null)
    {
      return getServletConfig().getResourceAsStream("WEB-INF/" + filename);
    }
    else
    {
      return null;
    }
  }
  
  /**
   * Gets the administrator email.
   */
  public String getAdministratorEmail()
  {
    return administratorEmail;
  }

  /**
   * Gets the number of requests received by the application since starting.
   */
  public long getRequestNumber()
  {
    return requestNumber.get();
  }
  
  /**
   * Gets the Entity Updater.
   */
  public EntityUpdater getEntityUpdater()
  {
    return entityUpdater;
  }

  /**
   * Returns a reference to the default Image Helper.  Depending
   * on the "UseImageMagick" value in the configuration, it will return
   * an ImageMagick helper or a pure JVM helper.
   */
  public ImageHelper getImageHelper()
  {
    return imageHelper;
  }
  
  @Override
  public String toString()
  {
    return getVersion().getVerboseDescription() + " (instance " + hashCode() + ")";
  }
  
  /**
   * Increments the request and returns the current request number.
   */
  public long incrementRequestCount()
  {
    return requestNumber.incrementAndGet();
  }

  // Methods that deal with an incoming request

  /**
   * Processes a request.
   *   <p>
   * This method checks that the application is running and if it is, creates
   * a Context for the request and handles it by calling handleRequest.  If
   * the application is not running or is in a bad state, an error message is
   * sent in response to the request.
   *   <p>
   * Listeners are notified of the request's start and completion by this
   * method.
   */
  public final void doRequest(Request httpRequest) throws IOException
  {
    final Context context = getContext(httpRequest);

    if (isRunning())
    {
      // Notify the listeners that we're starting a request.
      for (RequestListener listener : listeners)
      {
        try
        {
          listener.requestStarting(context);
        }
        catch (Exception exc)
        {
          // Do nothing; the request should still be processed.
        }
      }

      // Handle the request in a try block so that we can be sure to
      // notify listeners of the request completing even if the request
      // handling somehow results in an exception.
      try
      {
        handleRequest(context);
      }
      finally
      {
        // Notify the listeners that we're completing a request.
        for (RequestListener listener : listeners)
        {
          try
          {
            listener.requestCompleting(context);
          }
          catch (Exception exc)
          {
            // Do nothing, but the remainder of the listeners should still be
            // notified.
          }
        }
      }
    }
    else
    {
      if (getState() == GeminiApplication.OperationalState.NEW)
      {
        handleError(context, "Application not yet initialized.");
      }
      else
      {
        handleError(context, "Application not running.");
      }
    }
  }

  /**
   * Assuming the application is running and is in a good state, this method
   * will be called to handle the request.
   *   <p>
   * Overload this method if you do not want to use the dispatcher.
   */
  protected void handleRequest(Context context)
  {
    // Identify the current thread if we are counting requests.
    String threadName = null;
    if (isRequestCounting())
    {
      threadName = Thread.currentThread().getName();
      final long currentRequestNumber = incrementRequestCount();
      Thread.currentThread().setName(
          threadName + " (Request " + currentRequestNumber + ")");
      context.setRequestNumber(currentRequestNumber);
    }

    try
    {
      // Set the Context information to be displayed with every log message.
      GeminiComponentLog.setContextInformation(context);

      getDispatcher().dispatch(context);
    }
    finally
    {
      // Notify the Dispatcher that we're done with this request.
      getDispatcher().dispatchComplete(context);

      // Clear the Context info now that this Thread is done handling the request.
      GeminiComponentLog.clearContextInformation();

      // The current Context's usage is now complete, dissociate it with
      // the current thread.
      BasicContext.complete();

      // Remove the request number from the thread's name.
      if ( (isRequestCounting()) && (threadName != null) )
      {
        Thread.currentThread().setName(threadName);
      }
    }
  }

  /**
   * Renders a simple error message indicating that the site is not available.
   */
  private void handleError(Context context, String error) throws IOException
  {
    context.setContentType("text/html");
    final Writer writer = context.getWriter();
    writer.write("<html>");
    writer.write("<head><title>Temporarily Unavailable</title>");
    writer.write("<style>");
    writer.write("body { background-color: white; color: black; }");
    writer.write("p { font-family: Arial, Helvetica, Sans-serif; font-size: 12px; }");
    writer.write("h2 { font-family: Arial, Helvetica, Sans-serif; font-size: 14px; font-weight: bold; }");
    writer.write("pre { font-size: 9px; }");
    writer.write("</style>");
    writer.write("</head>");
    writer.write("<body>");
    writer.write("<h2>Temporarily Unavailable</h2>" + UtilityConstants.CRLF);
    writer.write("<p>This web site is temporarily unavailable due to maintenance work.  Please check back later.</p>"
        + UtilityConstants.CRLF);
    writer.write("<!-- " + error + " -->" + UtilityConstants.CRLF);
    writer.write("</body></html>");
  }

  /**
   * Default implementation of the application Lifecycle object.
   */
  public class Lifecycle
  {
    public static final int MAX_INITIALIZATION_ATTEMPTS = 10;
    public static final int SECONDS_BETWEEN_ATTEMPTS = 10;
    
    // Tasks to run during application initialization and shutdown.
    private final GeminiApplication        app;
    private final List<InitializationTask> initializationTasks   = new ArrayList<>();
    private final List<ShutdownTask>       shutdownTasks         = new ArrayList<>();
    
    private int initializationAttempts = 0; 
    
    public Lifecycle(GeminiApplication app)
    {
      this.app = app;
    }
    
    /**
     * Add an initialization task.
     */
    public void addInitializationTask(InitializationTask task)
    {
      initializationTasks.add(task);
    }
    
    /**
     * Add a shutdown task.
     */
    public void addShutdownTask(ShutdownTask task)
    {
      shutdownTasks.add(task);
    }
    
    /**
     * Add standard initialization and shutdown tasks.  If overloaded to specify
     * custom tasks, a call to super.addLifecycleTasks is strongly recommended.
     */
    protected void addLifecycleTasks()
    {
      final InitRegister register = new InitRegister();
      final InitManageAsynchronous asynchronous = new InitManageAsynchronous(); 
      
      addInitializationTask(new InitDisplayEnvironment());
      addInitializationTask(new InitPrepareForSoftKill());
      addInitializationTask(new InitConfigurationCheck());    
      addInitializationTask(new InitDatabaseConnectionTest(app));
      addInitializationTask(new InitDatabaseMigrations(app));
      addInitializationTask(new InitEntityStore());
      addInitializationTask(register);
      addInitializationTask(new InitStartupNotification());
      addInitializationTask(asynchronous);
      
      addShutdownTask(asynchronous);
      addShutdownTask(register);
    }
    
    /**
     * Start the application.
     */
    public void start()
    {
      // Increase the initialization attempts.
      initializationAttempts++;
      
      log.log("Starting initialization attempt " + initializationAttempts 
          + " of " + MAX_INITIALIZATION_ATTEMPTS + ".");
      
      // Attempt initialization.
      if (!attemptInitializationTasks())
      {
        if (initializationAttempts <= MAX_INITIALIZATION_ATTEMPTS)
        {
          // If initialization fails, start a remediation.
          log.log("Failed to initialize on attempt " + initializationAttempts 
              + ". Retrying in " + SECONDS_BETWEEN_ATTEMPTS + " seconds.");
          ThreadHelper.schedule(() -> start(), SECONDS_BETWEEN_ATTEMPTS, TimeUnit.SECONDS);
        }
        else
        {
          log.log("All " + MAX_INITIALIZATION_ATTEMPTS + " initialization attempts have failed.");
          log.log("Running shutdown tasks and halting.");
          
          setState(OperationalState.FAILED);
          
          runShutdownTasks();
        }
      }
    }

    /**
     * Attempts to run the InitializationTasks.  In the event of an error, log
     * the error and return false.
     */
    protected boolean attemptInitializationTasks()
    {
      try
      {
        // Run initialization tasks.
        final Iterator<InitializationTask> iter = initializationTasks.iterator();
        while (iter.hasNext())
        {
          final InitializationTask task = iter.next();
          task.taskInitialize(app);
          
          // Remove the task from the list once it's successful.
          iter.remove();
        }
        
        // If initialization tasks are complete, run some cleanup.
        initializationTasksComplete();
        
        return true;
      }
      catch (GeminiInitializationError error)
      {
        log.log("Exception thrown during initialization.", error);
        return false;
      }
    }
    
    /**
     * Once all initialization tasks are complete, do some final cleanup and
     * switch the state to RUNNING. 
     */
    protected void initializationTasksComplete()
    {
      // Step 3: Set the state to running.
      setState(OperationalState.RUNNING);
      
      log.log(getVersion().getProductName() + " started.");
      log.log(STARTUP_CHRONOGRAPH + ".");
    }
    
    /**
     * Runs the shutdown tasks.
     */
    protected void runShutdownTasks()
    {
      for (ShutdownTask task : shutdownTasks)
      {
        try
        {
          task.taskShutdown(app);
        }
        catch (Exception exc)
        {
          // In the event a shutdown tasks throws an exception, we will report
          // the exception to stderr (because the log may be closed) and then
          // proceed to the next shutdown task.
          System.err.println("Exception while executing shutdown task " 
              + task.getClass().getSimpleName() + ":\n" 
              + ThrowableHelper.convertStackTraceToString(exc));
        }
      }
    }
  }

}  // End GeminiApplication.
