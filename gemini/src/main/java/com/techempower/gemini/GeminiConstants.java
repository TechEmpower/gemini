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
import java.util.jar.*;

import com.techempower.text.*;
import com.techempower.util.*;

/**
 * This class stores the core constants used by the the Gemini
 * infrastructure.
 *   <p>
 * These constants should be accessed by using this class directly, for 
 * example: GeminiConstants.CMD.
 */
public final class GeminiConstants
  extends UtilityConstants
{
  // 
  // Private Constructor
  //
  private GeminiConstants(){}
  
  //
  // Gemini-specific constants.
  //

  public static final String GEMINI_VERSION = findGeminiVersion();
  public static final String GEMINI_NAME    = "gemini";

  //
  // Dispatcher commands.
  //

  public static final String  CMD                           = "cmd";
  public static final String  REFERRER_CMD                  = "referrer-cmd";

  // Home commands.
  public static final String  CMD_HOME                      = "home";

  // Default commands.
  public static final String  CMD_DEFAULT                   = CMD_HOME;
  public static final String  CMD_REDISPATCH_LIMIT_HIT      = CMD_HOME;

  //
  // Cookie information.
  //

  public static final int     DEFAULT_COOKIE_MAX_AGE        = 2592000;   // 30 days

  //
  // Date formatters.
  //

  /**
   * Standard Gemini date and time format.
   */
  public static final SynchronizedSimpleDateFormat GEMINI_DATE_FORMAT      = 
      new SynchronizedSimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /**
   * Standard Gemini date-only format.
   */
  public static final SynchronizedSimpleDateFormat GEMINI_DATE_ONLY_FORMAT = 
      new SynchronizedSimpleDateFormat("yyyy-MM-dd");

  /**
   * Standard Gemini time-only format.
   */
  public static final SynchronizedSimpleDateFormat GEMINI_TIME_ONLY_FORMAT = 
      new SynchronizedSimpleDateFormat("HH:mm:ss");

  //
  // Some content types.
  //
  
  public static final String   CONTENT_TYPE_JSON       = "application/json";
  public static final String   CONTENT_TYPE_HTML       = "text/html";
  public static final String   CONTENT_TYPE_TEXT       = "text/plain";
  public static final String   CONTENT_TYPE_MULTIPART  = "mixed/multipart";
  
  //
  // Constant names for objects that are placed into Sessions.
  //

  public static final String   SESSION_ID_NAME         = "Gemini-Session-ID";
  public static final String   SESSION_REQUEST_STACK   = "Gemini-Request-Stack";
  
  /**
   * Tracks the initial request's referrer (using the HTTP "referer" 
   * misspelling) header setting.
   */
  public static final String   SESSION_REFERER         = "Gemini-Referer";

  //
  // Constant names for objects that are added to as deliveries.
  //
  
  public static final String   GEMINI_FORM             = "form";
  public static final String   GEMINI_FORM_VALIDATION  = "validation";
  public static final String   GEMINI_MESSAGE          = "message";
  public static final String   GEMINI_STATUS           = "status";
  public static final String   GEMINI_ERROR            = "error";
  public static final String   GEMINI_ERROR_CODE       = "errorCode";
  public static final String   GEMINI_ERROR_NAME       = "errorName";
  
  //
  // Names of Properties for application configuration.
  //
  
  public static final String   PROP_DEPLOYMENT_DESCRIPTION = "DeploymentDescription";
  public static final String   PROP_INSTANCE_NUMBER        = "ApplicationInstanceNumber";
  public static final String   PROP_SPAWNED_INSTANCE       = "SpawnedInstance";
  
  //
  // Mime types.
  //
  
  public static final String   MULTIPART_ENCODING_TYPE = "multipart/form-data";
  
  //
  // Upload Constants.
  //
  
  public static final int      DEFAULT_MAX_UPLOAD_SIZE           = 104857600; // 100 MB
  public static final int      DEFAULT_MAX_UPLOAD_IN_MEMORY_SIZE = 10485760;  // 10 MB
  
  //
  // Servlet attribute parameter names.
  //
  
  public static final String   CONFIGURATION_FILE         = "ConfigurationFile";

  /**
   * Find the Gemini version by looking at the manifest generated by the
   * build tool.  If the runtime is not from a build tool, we assume the
   * version is "development".
   */
  private static String findGeminiVersion()
  {
    try
    {
      final Enumeration<URL> resources = GeminiApplication.class
          .getClassLoader().getResources("META-INF/MANIFEST.MF");

      // Iterate through all of the returned manifest.mfs
      while (resources.hasMoreElements())
      {
        final java.net.URL url = resources.nextElement();
        final Manifest manifest = new Manifest(url.openStream());
        try
        {
          final Attributes a = manifest.getMainAttributes();
          if (a != null)
          {
            for (Object o : a.keySet())
            {
              // If the manifest we found contains the version of Gemini, then 
              // grab it.
              if (o.toString().equalsIgnoreCase("geminiVersion"))
              {
                return a.get(o).toString();
              }
            }
          }
        }
        catch (Exception exc)
        {
          // Catching any possible exception because if an exception does get 
          // thrown, it is *impossible* to debug as the application container
          // just dies with no stack trace.  We'll return "development" in
          // this case.
        }
      }
    }
    catch (IOException ioexc)
    {
      // Assume "development".
    }
    return "development";
  }

}   // End GeminiConstants.
