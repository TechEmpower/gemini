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
import java.util.*;

import com.techempower.gemini.jsp.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.scheduler.*;
import com.techempower.util.*;

/**
 * Provides access to application settings (as opposed to Constants, which
 * are immutable) and various Infrastructure components.  This is a basic
 * implementation of the Infrastructure interface.  See Infrastructure for
 * more information.
 *   <p>
 * Extend this class and implement a specific Infrastructure class for
 * your application.  Overload customConfigure() to handle specific
 * configuration concerns.
 *   <p>
 * This BasicInfrastructure reads the following configuration options from
 * the application's .conf file:
 *    <ul>
 * <li>ServerName - The server's full DNS name or IP address.
 * <li>StandardDomain - The standard domain name to use when constructing
 *     the servlet URL.  This domain should include the protocol (http)
 *     and the domain name, but not the trailing slash.  This is typically
 *     "http://" plus the ServerName and defined as http://${ServerName}
 *     in the configuration file.  For example: http://www.domain.com
 * <li>SecureDomain - The secure (SSL) domain name.  This parameter can be
 *     ignored if SSL is not used within the application.  This domain should
 *     include the protocol (https) and the domain name, but not the trailing
 *     slash.  This is typically "https://" plus the ServerName and defined 
 *     as https://${ServerName} in the configuration file.  For example: 
 *     https://www.domain.com
 * <li>HTMLDirectory - Relative (relative to webroot) or absolute location
 *     of HTML, JavaScript, and CSS files.  This should always end with a
 *     trailing slash (/) but can be a relative <i>or</i> absolute URL.  For
 *     example: /images/ or http://server.domain.com/images/
 * <li>CSSDirectory - Relative (relative to webroot) or absolute location
 *     of CSS files.  This should always end with a
 *     trailing slash (/) but can be a relative <i>or</i> absolute URL.  For
 *     example: /css/ or http://server.domain.com/css/
 * <li>JavaScriptDirectory - Relative (relative to webroot) or absolute location
 *     of Javascript files.  This should always end with a
 *     trailing slash (/) but can be a relative <i>or</i> absolute URL.  For
 *     example: /js/ or http://server.domain.com/js/
 * <li>JSPDirectory - Relative (relative to application root) location of
 *     JSP files.  This should always end with a trailing slash and must be
 *     a relative URL within the directory space of the application server.
 *     For example: /jsp/ or /content/jsp/
 * <li>JSPPhysicalDirectory - The physical directory on disk that points to
 *     the same directory as JSPDirectory.  For example: F:\\content\\jsp\\
 * <li>ImageDirectory - Relative (relative to webroot) or absolute location
 *     of images.  This should always end with a trailing slash but can be
 *     a relative <i>or</i>absolute URL.  For example: /images/ or
 *     http://image-server.domain.com/application/
 * <li>StyleSheetName - An optional variable for storing the name of a single
 *     style sheet used by an application.
 * <li>ServletURL - Base URL to the servlet, as provided to the servlet
 *     container.  This must be a relative URL, from the root of the web
 *     server and within the directory space of the application server.  This
 *     URL should always start with a slash.  For example: /servlets/someapp
 * <li>AllowDirectJSPs - Should direct invocation of JSPs be permitted?  The
 *     default behavior is NO.  JSPs usually will not function correctly if
 *     invoked directly anyway.
 *   </ul>
 */
public class BasicInfrastructure
  implements Infrastructure
{
  //
  // Constants.
  //

  public static final String URL_DIR_SEPARATOR           = "/";                 // Directory separator.
  public static final String DEFAULT_URL_PREFIX          = "";                  // Default is nothing.
  public static final String DEFAULT_HTML_DIR            = "/";                 // Default
  public static final String DEFAULT_CSS_DIR             = "/";                 // Default
  public static final String DEFAULT_JAVASCRIPT_DIR      = "/";                 // Default
  public static final String DEFAULT_JSP_DIR             = "/jsp/";             // Default
  public static final String DEFAULT_JSP_PHYSICAL_DIR    = "/jsp/";             // Default
  public static final String DEFAULT_IMAGES_DIR          = "/images/";          // Default
  public static final String DEFAULT_STYLE_DIR           = "/html/";            // Default
  public static final String DEFAULT_CACHED_RESPONSE_DIR = "/cachedResponses/"; // Default
  public static final String DEFAULT_SERVLET_NAME        = "/";                 // Default
  public static final String DEFAULT_IDENTITY            = "Gemini";            // Default

  //
  // Member variables.
  //

  private final GeminiApplication application;
  private final ComponentLog      log;
  
  private String            serverName              = DEFAULT_URL_PREFIX;
  private String            standardDomain          = DEFAULT_URL_PREFIX;
  private String            secureDomain            = DEFAULT_URL_PREFIX;
  private String            htmlFileDirectory       = DEFAULT_HTML_DIR;
  private String            cssFileDirectory        = DEFAULT_CSS_DIR;
  private String            jsFileDirectory         = DEFAULT_JAVASCRIPT_DIR;
  private String            logFileDirectory        = "";
  private String            jspFileDirectory        = DEFAULT_JSP_DIR;
  private String            jspPhysicalDirectory    = DEFAULT_JSP_PHYSICAL_DIR;
  private String            imageFileDirectory      = DEFAULT_IMAGES_DIR;
  private String            cachedResponseDirectory = DEFAULT_CACHED_RESPONSE_DIR;

  private String            secureHtmlFileDirectory     = null;
  private String            secureCssFileDirectory      = null;
  private String            secureJsFileDirectory       = null;
  private String            secureImageFileDirectory    = null;

  private String            servletName              = DEFAULT_SERVLET_NAME;
  private String            urlDirectoryPrefix       = DEFAULT_URL_PREFIX;
  private boolean           allowDirectJSPs          = false;
  private boolean           useURLDirectoryPrefix    = false;
  private boolean           started                  = false;
  private boolean           useCachedToDiskResponses = false;
  private String            identity                 = DEFAULT_IDENTITY;
  private ScriptsAndSheets  applicationSas;
  
  //
  // Member methods.
  //

  /**
   * Constructor.  Sets up a reference to the Application and constructs
   * a ComponentLog.
   */
  public BasicInfrastructure(GeminiApplication application)
  {
    // Copy parameters.
    this.application = application;

    // Set up log reference.
    this.log = application.getLog(COMPONENT_CODE);
    
    // Set the Infrastructure as an Asynchronous resource.
    application.addAsynchronous(this);

    application.getConfigurator().addConfigurable(this);

    // The Scheduler is not part of Gemini proper and is therefore it does
    // not register itself as a Configurable.  Infrastructure handles that
    // for the Scheduler.
    final Scheduler scheduler = application.getScheduler();
    if (scheduler != null)
    {
      application.getConfigurator().addConfigurable(scheduler);
    }
  }
  
  /**
   * Configure this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // Configure the properties of the Basic Infrastructure.
    this.serverName               = props.get("ServerName", this.serverName);
    this.standardDomain           = props.get("StandardDomain", this.standardDomain);
    this.secureDomain             = props.get("SecureDomain", this.secureDomain);
    this.htmlFileDirectory        = props.get("HTMLDirectory", this.htmlFileDirectory);
    this.cssFileDirectory         = props.get("CSSDirectory", this.cssFileDirectory);
    this.jsFileDirectory          = props.get("JavaScriptDirectory", this.jsFileDirectory);
    this.jspFileDirectory         = props.get("JSPDirectory", this.jspFileDirectory);
    this.jspPhysicalDirectory     = props.get("JSPPhysicalDirectory", this.jspPhysicalDirectory);
    this.imageFileDirectory       = props.get("ImageDirectory", this.imageFileDirectory);
    this.cachedResponseDirectory  = props.get("CachedResponsesDirectory", this.cachedResponseDirectory);

    this.secureHtmlFileDirectory  = props.get("SecureHTMLDirectory");
    this.secureCssFileDirectory   = props.get("SecureCSSDirectory");
    this.secureJsFileDirectory    = props.get("SecureJavaScriptDirectory");
    this.secureImageFileDirectory = props.get("SecureImageDirectory");

    if (props.get("StyleSheetName") != null)
    {
      this.log.log("StyleSheetName is deprecated!", LogLevel.ALERT);
    }
    this.servletName              = props.get("ServletURL", this.servletName);
    this.allowDirectJSPs          = props.getBoolean("AllowDirectJSPs", false);
    this.useCachedToDiskResponses = props.getBoolean("UseDiskResponseCaching", false);
    this.logFileDirectory         = props.get("Log.File.LogDirectory", this.logFileDirectory);
    this.identity                 = props.get("Identity", this.identity);

    // Clean up any missing trailing slashes.
    if (!this.htmlFileDirectory.endsWith("/"))
    {
      this.log.log("HTMLDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.htmlFileDirectory = this.htmlFileDirectory + '/';
    }
    if (!this.cssFileDirectory.endsWith("/"))
    {
      this.log.log("CSSDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.cssFileDirectory = this.cssFileDirectory + '/';
    }
    if (!this.jsFileDirectory.endsWith("/"))
    {
      this.log.log("JavaScriptDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.jsFileDirectory = this.jsFileDirectory + '/';
    }
    if (!this.imageFileDirectory.endsWith("/"))
    {
      this.log.log("ImageDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.imageFileDirectory = this.imageFileDirectory + '/';
    }
    if (!this.jspFileDirectory.endsWith("/"))
    {
      this.log.log("JSPDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.jspFileDirectory = this.jspFileDirectory + '/';
    }
    if (!this.jspPhysicalDirectory.endsWith(File.separator))
    {
      this.log.log("JSPPhysicalDirectory should end with a trailing slash or backslash.", LogLevel.ALERT);
      this.jspPhysicalDirectory = this.jspPhysicalDirectory + File.separator;
    }

    // Clean up any missing trailing slashes.
    if (this.secureHtmlFileDirectory != null && !this.secureHtmlFileDirectory.endsWith("/"))
    {
      this.log.log("HTMLDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.secureHtmlFileDirectory = this.secureHtmlFileDirectory + '/';
    }
    if (this.secureCssFileDirectory != null && !this.secureCssFileDirectory.endsWith("/"))
    {
      this.log.log("CSSDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.secureCssFileDirectory = this.secureCssFileDirectory + '/';
    }
    if (this.secureJsFileDirectory != null && !this.secureJsFileDirectory.endsWith("/"))
    {
      this.log.log("JavaScriptDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.secureJsFileDirectory = this.secureJsFileDirectory + '/';
    }
    if (this.secureImageFileDirectory != null && !this.secureImageFileDirectory.endsWith("/"))
    {
      this.log.log("ImageDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.secureImageFileDirectory = this.secureImageFileDirectory + '/';
    }
    if (this.cachedResponseDirectory != null && !this.cachedResponseDirectory.endsWith("/"))
    {
      this.log.log("CachedResponsesDirectory should end with a trailing slash.", LogLevel.ALERT);
      this.cachedResponseDirectory = this.cachedResponseDirectory + '/';
    }
    
    if(this.useCachedToDiskResponses)
    {
      File cachedToDiskDir = new File(this.cachedResponseDirectory);
      if(cachedToDiskDir.exists() || cachedToDiskDir.mkdirs())
      {
        this.log.log("CachedToDisk directory: " + this.cachedResponseDirectory);
      }
      else
      {
        this.log.log("CachedToDisk directory could not be created (tried " + this.cachedResponseDirectory + ")");
      }
    }

    // Construct the ScriptsAndSheets reference for application-scope 
    // JavaScript and CSS style-sheet dependencies.
    ScriptsAndSheets newSas = new ScriptsAndSheets(this.application);
    configureSas(newSas);
    this.applicationSas = newSas; 
    
    // Do any custom configuration.
    customConfigure(props);
  }
  
  /**
   * Gets the Infrastructure's ComponentLog.
   */
  protected ComponentLog getLog()
  {
    return this.log;
  }
  
  /**
   * Configures the ScriptsAndSheets reference for application-scope 
   * external JavaScript and CSS style-sheet dependencies.
   *   <p>
   * Applications should overload this method to define application-scope
   * scripts and sheets.
   */
  protected void configureSas(ScriptsAndSheets appSas)
  {
    // Does nothing in this base class.
  }

  /**
   * Configure this component.  Subclasses should overload this
   * method to do any further configuration.
   * 
   * @param props The configuration properties.
   */
  public void customConfigure(EnhancedProperties props)
  {
    // Overload this method.
  }

  /**
   * Starts this asynchronous resource.
   */
  @Override
  public synchronized void begin()
  {
    if (!this.started)
    {
      start();
      this.started = true;
    }
  }

  /**
   * Stops this asynchronous resource.
   */
  @Override
  public synchronized void end()
  {
    if (this.started)
    {
      shutdown();
      this.started = false;
    }
  }

  /**
   * Called by the "begin" method to provide any custom start-up logic.
   * This will only be called if the Infrastructure is not already in a
   * "started" state.
   */
  public void start()
  {
    // Does nothing in this base class.
  }

  /**
   * Called by the "end" method to provide any custom shutdown logic. This
   * will only be called if the Infrastructure is not already in a "stopped"
   * state.
   */
  public void shutdown()
  {
    // Does nothing in this base class.
  }
  
  /**
   * Gets a reference to the application.
   */
  protected GeminiApplication getApplication()
  {
    return this.application;
  }
  
  /**
   * Gets the Application-scope ScriptsAndSheets reference used for managing
   * application-scope external JavaScript and CSS style-sheet dependencies.
   */
  public ScriptsAndSheets getSas()
  {
    return this.applicationSas;
  }

  /**
   * Gets the Server's name or IP address.  E.g., www.domain.com
   */
  @Override
  public String getServerName()
  {
    return this.serverName;
  }

  /**
   * Gets the standard domain prefix.  This will normally be in
   * the form "http://www.domain.com", without the trailing /.
   */
  @Override
  public String getStandardDomain()
  {
    return this.standardDomain;
  }

  /**
   * Gets the standard domain prefix, pulled from the Context provided.
   * This will normally be in the form "http://www.domain.com", without
   * the trailing /.
   */
  @Override
  public String getStandardDomain(Context context)
  {
    return "http://" + context.headers().host();
  }

  /**
   * Gets the secure (SSL) domain prefix.  This will normally be in
   * the form "https://www.domain.com", without the trailing /.
   */
  @Override
  public String getSecureDomain()
  {
    return this.secureDomain;
  }

  /**
   * Gets the JSP directory.
   */
  @Override
  public String getJspDirectory()
  {
    return this.jspFileDirectory;
  }

  /**
   * Gets the JSP physical directory.
   */
  @Override
  public String getJspPhysicalDirectory()
  {
    return this.jspPhysicalDirectory;
  }

  /**
   * Gets the HTML directory. Will attempt to derive the context in order
   * to determine if the secure directory should be returned.
   */
  @Override
  public String getHtmlDirectory()
  {
    if (this.secureHtmlFileDirectory != null)
    {
      return this.getHtmlDirectory(Context.get());
    }

    return this.htmlFileDirectory;
  }

  /**
   * Gets the HTML directory. Will use the given context in order
   * to determine if the secure directory should be returned.
   */
  @Override
  public String getHtmlDirectory(Context context)
  {
    if (this.secureHtmlFileDirectory != null && context != null && context.getRequest().isSecure())
    {
      return this.secureHtmlFileDirectory;
    }

    return this.htmlFileDirectory;
  }

  /**
   * Gets the CSS directory. Will attempt to derive the context in order
   * to determine if the secure directory should be returned.
   */
  @Override
  public String getCssDirectory()
  {
    if (this.secureCssFileDirectory != null)
    {
      return this.getCssDirectory(Context.get());
    }

    return this.cssFileDirectory;
  }

  /**
   * Gets the CSS directory. Will use the given context in order
   * to determine if the secure directory should be returned.
   */
  @Override
  public String getCssDirectory(Context context)
  {
    if (this.secureCssFileDirectory != null && context != null && context.getRequest().isSecure())
    {
      return this.secureCssFileDirectory;
    }

    return this.cssFileDirectory;
  }

  /**
   * Gets the JavaScript directory. Will attempt to derive the context in order
   * to determine if the secure directory should be returned.
   */
  @Override
  public String getJavaScriptDirectory()
  {
    if (this.secureJsFileDirectory != null)
    {
      return this.getJavaScriptDirectory(Context.get());
    }

    return this.jsFileDirectory;
  }

  /**
   * Gets the JavaScript directory. Will use the given context in order
   * to determine if the secure directory should be returned.
   */
  @Override
  public String getJavaScriptDirectory(Context context)
  {
    if (this.secureJsFileDirectory != null && context != null && context.getRequest().isSecure())
    {
      return this.secureJsFileDirectory;
    }

    return this.jsFileDirectory;
  }

  /**
   * Gets the images directory.  When specified in the application's .conf
   * file, the directory should have a trailing slash included. Will attempt
   * to derive the context in order to determine if the secure directory should
   * be returned.
   */
  @Override
  public String getImageDirectory()
  {
    if (this.secureImageFileDirectory != null)
    {
      return this.getImageDirectory(Context.get());
    }

    return this.imageFileDirectory;
  }

  /**
   * Gets the images directory.  When specified in the application's .conf
   * file, the directory should have a trailing slash included. Will use the
   * given context in order to determine if the secure directory should
   * be returned.
   */
  @Override
  public String getImageDirectory(Context context)
  {
    if (this.secureImageFileDirectory != null && context != null && context.getRequest().isSecure())
    {
      return this.secureImageFileDirectory;
    }

    return this.imageFileDirectory;
  }
  
  /**
   * Gets the Cached Response Directory
   */
  public String getCachedResponseDirectory()
  {
    return this.cachedResponseDirectory;
  }
  
  /**
   * Gets whether we are using disk caching or not.
   */
  public boolean usesCachedToDiskResponses()
  {
    return this.useCachedToDiskResponses;
  }

  /**
   * Gets a URL to a specific image, using the "default" images directory.
   */
  @Override
  public String getImageUrl(String imageFilename)
  {
    return getImageDirectory() + imageFilename;
  }

  /**
   * Gets a URL to a specific image, given a Locale.  The directory structure
   * specified by Gemini for localized applications is:
   *    <p>
   * Locale provided: [image-directory]\[locale-directory]
   * Default locale:  [image-directory]
   *    <p>
   * For image "photo.jpg", a localized version for en-US would be stored as
   * "[image-directory]\en-US\photo.jpg".  A default version could also be
   * stored as "[image-directory]photo.jpg" for situations where there is no
   * Locale defined.
   *
   * @param imageFilename the filename of the image, eg, 'photo.jpg'.
   * @param locale the locale for which to locate the image.
   */
  @Override
  public String getImageUrl(String imageFilename, Locale locale)
  {
    return getImageDirectory()
      + locale.getLanguage()
      + "-" + locale.getCountry()
      + URL_DIR_SEPARATOR
      + imageFilename;
  }

  /**
   * Gets a URL to a specific image, given a user's Context.  See the notes
   * for getImageURL(imageFilename, locale) above for more information about
   * locales and image directories.
   *   <p>
   * If the user's Context has no session set, the default locale will be
   * used (that is, the image directory without a locale directory appended).
   *
   * @param imageFilename the filename of the image, eg, 'photo.jpg'.
   * @param context a user's request Context reference.
   */
  @Override
  public String getImageUrl(String imageFilename, Context context)
  {
    Locale locale = this.application.getLocaleManager().getLocaleRaw(context);
    if (locale != null)
    {
      return getImageUrl(imageFilename, locale);
    }
    else
    {
      return getImageUrl(imageFilename);
    }
  }

  /**
   * Returns the HTML <link> tag that should be embedded in an HTML page
   * in order to import the application .CSS file for use in that HTML page.
   *
   * @param styleSheetFilename the filename (not path) of the stylesheet.
   */
  @Override
  public String getStyleLink(String styleSheetFilename)
  {
    return "<link rel=\"STYLESHEET\" type=\"text/css\" href=\""
      + getCssDirectory() + styleSheetFilename + "\" />";
  }

  /**
   * Returns the HTML <link> tag that should be embedded in an HTML page
   * in order to import the application .CSS file for use in that HTML page.
   *
   * @param styleSheetFilename the filename (not path) of the stylesheet.
   * @param media the optional media for this stylesheet (e.g., "screen" or
   *        "print")
   */
  @Override
  public String getStyleLink(String styleSheetFilename, String media)
  {
    return "<link rel=\"STYLESHEET\" type=\"text/css\" "
      + (StringHelper.isNonEmpty(media) ? "media=\"" + media + "\" " : "")
      + "href=\"" + getCssDirectory() + styleSheetFilename + "\" />";
  }
  
  /**
   * Gets the name of the servlet.  This provides a means to make links
   * back to the servlet.
   */
  @Override
  public String getName()
  {
    return this.servletName;
  }

  /**
   * Gets the name (also sometimes called the "URL") of the servlet.
   */
  @Override
  public String getUrl()
  {
    return getName();
  }

  /**
   * Generates an absolute URL to the servlet.
   */
  @Override
  public String getStandardUrl()
  {
    return getStandardDomain() + getUrl();
  }

  /**
   * Generates an absolute URL to the servlet,
   * using the secure (SSL) domain name.
   */
  @Override
  public String getSecureUrl()
  {
    return getSecureDomain() + getUrl();
  }

  /**
   * Generates a simple relative URL to the servlet with a command
   * specified.
   */
  @Override
  public String getCmdUrl(String command)
  {
    return this.servletName + '?' + GeminiConstants.CMD + '=' + command;
  }

  /**
   * Generates an anchor tag with a servlet-command URL.
   */
  @Override
  public String getCmdAnchor(String command)
  {
    return "<a href=\"" + getCmdUrl(command) + "\">";
  }

  /**
   * Generates an absolute URL to the servlet with a command specified,
   * using the standard domain name.
   */
  @Override
  public String getStandardCmdUrl(String command)
  {
    return getStandardDomain() + getCmdUrl(command);
  }

  /**
   * Generates an absolute URL to the servlet with the command specified,
   * using the domain pulled from the Context specified.
   *  <p>
   * NOTE: This does NOT use getStandardDomain().
   */
  @Override
  public String getStandardCmdUrl(String command, Context context)
  {
    return getStandardDomain(context) + getCmdUrl(command);
  }

  /**
   * Generates an absolute URL to the servlet with a command specified,
   * using the secure (SSL) domain name.
   */
  @Override
  public String getSecureCmdUrl(String command)
  {
    return getSecureDomain() + getCmdUrl(command);
  }

  /**
   * Returns a reference to the String value currently assigned for the
   * "urlDirectoryPrefix" member variable of BasicInfrastructure.
   */
  @Override
  public String getURLDirectoryPrefix()
  {
    return this.urlDirectoryPrefix;
  }

  /**
   * Determines if JSPs are allowed to be invoked directly by URL.
   */
  @Override
  public boolean canInvokeJSPsDirectly()
  {
    return this.allowDirectJSPs;
  }

  /**
   * Gets a reference to the flag variable that indicates whether this application
   * should use the urlDirectoryPrefix string when making calls to Content.includeJSP(...)
   */
  @Override
  public boolean useURLDirectoryPrefix()
  {
    return this.useURLDirectoryPrefix;
  }

  /**
   * @return Returns the logFileDirectory.
   */
  public String getLogFileDirectory()
  {
    return this.logFileDirectory;
  }

  /**
   * Returns the identity of the Gemini instance. Useful for distinguishing between
   * different deployments of the same application ie Development, Staging, Production.
   *   <p>
   * [Please note that this functionality is redundant to that provided
   * by the DeploymentDescription functionality in com.techempower.Version]
   *
   * @see com.techempower.Version
   *
   * @return The identity of the deployment
   */
  @Override
  public String identify()
  {
    return this.identity;
  }

  /**
   * Gets the html directory for secure connections.
   * If none is defined, will default to standard directory.
   */
  public String getSecureHtmlDirectory()
  {
    if (this.secureHtmlFileDirectory == null)
    {
      return this.htmlFileDirectory;
    }

    return this.secureHtmlFileDirectory;
  }

  /**
   * Gets the css directory for secure connections.
   * If none is defined, will default to standard directory.
   */
  public String getSecureCssDirectory()
  {
    if (this.secureCssFileDirectory == null)
    {
      return this.cssFileDirectory;
    }

    return this.secureCssFileDirectory;
  }

  /**
   * Gets the js directory for secure connections.
   * If none is defined, will default to standard directory.
   */
  public String getSecureJsDirectory()
  {
    if (this.secureJsFileDirectory == null)
    {
      return this.jsFileDirectory;
    }

    return this.secureJsFileDirectory;
  }

  /**
   * Gets the image directory for secure connections.
   * If none is defined, will default to standard directory.
   */
  public String getSecureImageDirectory()
  {
    if (this.secureImageFileDirectory == null)
    {
      return this.imageFileDirectory;
    }

    return this.secureImageFileDirectory;
  }

}   // End BasicInfrastructure
