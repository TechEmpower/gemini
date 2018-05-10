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
package com.techempower;

import java.io.*;
import java.net.*;
import java.util.*;

import com.techempower.helper.*;

/**
 * Contains simple constants regarding the name, client, version number,
 * etc. of the current build of the application.
 *   <p>
 * When building an application, subclass this class and provide an instance
 * of the object in your application's constructVersion() method.
 *   <p>
 * Note that for Gemini applications, the GeminiApplication object will
 * populate the deployment description (e.g., "Production", "Test", etc.)
 * from the configuration variable "DeploymentDescription". 
 */
public class Version
{
  
  //
  // Constants.
  //
  
  public static final int ENV_DEVELOPMENT = 0;
  public static final int ENV_TEST        = 1;
  public static final int ENV_PRODUCTION  = 2;
  
  //
  // Member variables.
  //

  private String      versionString;
  private String      buildDate             = null;
  private String      deploymentDescription = "Default";
  private int         environment           = ENV_DEVELOPMENT;


  //
  // Member methods.
  //

  /**
   * Constructor.  This builds the version string for later calls to 
   * getVersionString.
   */
  public Version()
  {
    this.versionString = getMajorVersion() + "." 
      + getMinorVersion() + "." 
      + getMicroVersion();
  }

  /**
   * Get the version levels.
   */
  public int getMajorVersion()  { return 1; }
  public int getMinorVersion()  { return 1; }
  public int getMicroVersion()  { return 0; }

  /**
   * Gets the two-letter product code.  The default is "GA" for "Gemini 
   * Application".  However, applications are highly encouraged to overload
   * this to provide a custom product code.
   */
  public String getProductCode()
  {
    return "GA";
  }

  /**
   * Gets the product name.
   */
  public String getProductName()
  {
    return "Example";
  }

  /**
   * Gets an abbreviated version of the product name.  By default, this returns
   * the product name unchanged.
   */
  public String getAbbreviatedProductName()
  {
    return getProductName();
  }

  /**
   * Gets the client's name.
   */
  public String getClientName()
  {
    return "Example";
  }

  /**
   * Gets the developer's name.
   */
  public String getDeveloperName()
  {
    return "TechEmpower, Inc.";
  }

  /**
   * Gets the copyright years.
   */
  public String getCopyrightYears()
  {
    return "2018";
  }

  /**
   * Gets the version as a readable string.  The format is MAJOR.MINOR.MICRO, 
   * as in "1.03.15".
   */
  public String getVersionString()
  {
    return this.versionString;
  }
  
  /**
   * Sets the version string.  This is usually called by the constructor only.
   */
  public void setVersionString(String versionString)
  {
    this.versionString = versionString;
  }
  
  /**
   * Gets a verbose version description composed of the application/product
   * name, version number, and the build date:
   *   <p>
   * [abbr-product-name] [version-string] build [build-date]
   *   <p>
   * Example Product 1.02.03 build 2009-03-27 11:00:23
   */
  public String getVerboseDescription()
  {
    return getAbbreviatedProductName() + " " + getVersionString() + " build " + getBuildDate(); 
  }

  /**
   * Gets the Java Runtime's version information as a nice String.
   */
  public String getJavaVersion()
  {
    return System.getProperty("java.version") 
      + " (" + System.getProperty("java.vendor") + ")"
      + " on " + System.getProperty("os.name")
      + " " + System.getProperty("os.version");
  }

  /**
   * @return Returns the deploymentDescription.
   */
  public String getDeploymentDescription()
  {
    return this.deploymentDescription;
  }

  /**
   * @param deploymentDescription The deploymentDescription to set.
   */
  public void setDeploymentDescription(String deploymentDescription)
  {
    this.deploymentDescription = deploymentDescription;
    
    // Parse common words to describe environments (production, test, 
    // development) and set the flags accordingly.
    int env = ENV_DEVELOPMENT;
    if (  (StringHelper.containsIgnoreCase(deploymentDescription, "Test"))
       || (StringHelper.containsIgnoreCase(deploymentDescription, "QA"))
       || (StringHelper.containsIgnoreCase(deploymentDescription, "Staging"))
       )
    {
      env = ENV_TEST;
    }
    if (StringHelper.containsIgnoreCase(deploymentDescription, "Production"))
    {
      env = ENV_PRODUCTION;
    }

    setEnvironment(env);
  }
  
  /**
   * Gets the application name and the deployment description, if available.
   * This returns the format of "MyApplication (Production/Some-box)" or
   * "MyApplication (Development/Some-other-box)".
   */
  public String getNameAndDeployment()
  {
    return getProductName() 
        + (StringHelper.isNonEmpty(getDeploymentDescription()) ? " (" + getDeploymentDescription() + ")" : ""); 
  }

  /**
   * Sets the environment type (See constants defined in this class).
   * 
   * @param environmentType either ENV_DEVELOPMENT, ENV_TEST, or 
   *        ENV_PRODUCTION.
   */
  public void setEnvironment(int environmentType)
  {
    if (  (environmentType >= ENV_DEVELOPMENT)
       && (environmentType <= ENV_PRODUCTION)
       )
    {
      this.environment = environmentType;
    }
  }
  
  /**
   * Gets the environment type.
   */
  public int getEnvironment()
  {
    return this.environment;
  }
  
  /**
   * Creates and returns a set of deployment environment flags.  These flags 
   * are provided in this seemingly odd form in order to make them available 
   * for use in Mustache templates as conditional checks.
   *   <p>
   * This method constructs a new map on each call, so it is safe for callers
   * to append more values to the map.
   */
  public Map<Object, Boolean> getEnvironmentFlags()
  {
    final Map<Object, Boolean> toReturn = new HashMap<>();
    
    if (isProduction())
    {
      // Add some flags indicating a Production deployment environment.
      toReturn.put("production", Boolean.TRUE);
      toReturn.put("prod", Boolean.TRUE);
    }
    if (isTest())
    {
      // Add some flags indicating a Test/staging deployment environment.
      toReturn.put("test", Boolean.TRUE);
      toReturn.put("staging", Boolean.TRUE);
    }
    if (isDevelopment())
    {
      // Add some flags indicating a Development deployment environment.
      toReturn.put("development", Boolean.TRUE);
      toReturn.put("dev", Boolean.TRUE);
    }
    if (isTest() || isProduction())
    {
      // Add a special flag indicating either a Production or Test deployment
      // environment.
      toReturn.put("testOrProduction", Boolean.TRUE);
      toReturn.put("testOrProd", Boolean.TRUE);
    }
    if (isDevelopment() || isTest())
    {
      toReturn.put("developmentOrTest", Boolean.TRUE);
      toReturn.put("devOrTest", Boolean.TRUE);
    }
    
    return toReturn;
  }

  /**
   * Returns true if this application is in the DEVELOPMENT environment.
   */
  public boolean isDevelopment()
  {
    return this.environment == ENV_DEVELOPMENT;
  }

  /**
   * Returns true if this application is in the TEST environment.
   */
  public boolean isTest()
  {
    return this.environment == ENV_TEST;
  }

  /**
   * Returns true if this application is in the PRODUCTION environment.
   */
  public boolean isProduction()
  {
    return this.environment == ENV_PRODUCTION;
  }

  /**
   * @return the buildDate.  By default, this will return the file modified
   * date of the application's subclass of the Version class.  E.g.,
   * the "SomeappVersion.class" file.  Because this will only be useful if
   * the subclass of Version is recompiled, it is recommended that a full
   * re-build is done before a deployment.  Alternatively, a tool such as
   * "touch" can be used to update the last modified date of the file at
   * build time.
   */
  public String getBuildDate()
  {
    if (this.buildDate == null)
    {
      this.buildDate = "Unknown";
      try
      {
        String name = this.getClass().getCanonicalName();
        URL location = this.getClass().getClassLoader().getResource(
          name.replace('.', '/') + ".class");
        File source = new File(location.toURI());
        
        if (source.exists())
        {
          this.buildDate = DateHelper.STANDARD_TECH_FORMAT.format(
            new Date(source.lastModified()));
        }
      }
      catch (URISyntaxException use) 
      {
        // Do nothing.
      }
      catch (IllegalArgumentException iae)
      {
        // This will happen if using a jar and the .class file can't be found.
        // Do nothing.
      }
    }

    return this.buildDate;
  }

  /**
   * @param buildDate the buildDate to set.  This is a String to allow the 
   * build date to take any form desired.  Setting the build date manually
   * is optional (see default implementation of getBuildDate).
   */
  public void setBuildDate(String buildDate)
  {
    this.buildDate = buildDate;
  }

}   // End Version
