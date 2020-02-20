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

import java.util.*;

import com.techempower.asynchronous.*;
import com.techempower.util.*;

/**
 * The Infrastructure is Gemini's mechanism for abstracting issues pertaining
 * to application roll-out and deployment.  The location of various web-app
 * resources such as images, style-sheets, etc. can be handled by the
 * Infrastructure.  This allows JSPs and Handler components the freedom of
 * not needing to hard-code these locations.
 *   <p>
 * Most typically, the BasicInfrastructure is used as a foundation for
 * building an application-specific Infrastructure.
 */
public interface Infrastructure
  extends        Asynchronous,
                 Configurable
{

  //
  // Methods.
  //

  /**
   * Gets the server's DNS name or IP address.  E.g., www.domain.com
   */
  String getServerName();
  
  /**
   * Gets the standard domain prefix.  This will normally be in
   * the form "http://www.domain.com", without the trailing /.
   */
  String getStandardDomain();

  /**
   * Gets the standard domain prefix, pulled from the Context provided.
   * This will normally be in the form "http://www.domain.com", without
   * the trailing /.
   */
  String getStandardDomain(Context context);

  /**
   * Gets the secure (SSL) domain prefix.  This will normally be in
   * the form "https://www.domain.com", without the trailing /.
   */
  String getSecureDomain();

  /**
   * Gets the JSP directory.
   */
  String getJspDirectory();

  /**
   * Gets the JSP physical directory.
   */
  String getJspPhysicalDirectory();

  /**
   * Gets the HTML directory.
   */
  String getHtmlDirectory();
  
  /**
   * Gets the HTML directory.
   */
  <C extends Context> String getHtmlDirectory(C context);

  /**
   * Gets the images directory.  When specified in the application's .conf
   * file, the directory should have a trailing slash included.
   */
  String getImageDirectory();

  /**
   * Gets a URL to a specific image, using the "default" images directory.
   */
  String getImageUrl(String imageFilename);
  
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
  String getImageUrl(String imageFilename, Locale locale);

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
  <C extends Context> String getImageUrl(String imageFilename, C context);
  
  /**
   * Returns the HTML {@code <link>} tag that should be embedded in an HTML page
   * in order to import the application .CSS file for use in that HTML page.
   *
   * @param styleSheetFilename the filename (not path) of the stylesheet.
   */
  String getStyleLink(String styleSheetFilename);

  /**
   * Returns the HTML {@code <link>} tag that should be embedded in an HTML page
   * in order to import the application .CSS file for use in that HTML page.
   *
   * @param styleSheetFilename the filename (not path) of the stylesheet.
   * @param media the optional media for this stylesheet (e.g., "screen" or
   *        "print")
   */
  String getStyleLink(String styleSheetFilename, String media);
  
  /**
   * Gets the name of the servlet.  This provides a means to make links
   * back to the servlet.
   */
  String getName();

  /**
   * Gets the name (also sometimes called the "URL") of the servlet.
   */
  String getUrl();

  /**
   * Generates an absolute URL to the servlet.
   */
  String getStandardUrl();

  /**
   * Generates an absolute URL to the servlet,
   * using the secure (SSL) domain name.
   */
  String getSecureUrl();

  /**
   * Generates a simple relative URL to the servlet with a command
   * specified.
   */
  String getCmdUrl(String command);

  /**
   * Generates an anchor tag with a servlet-command URL.
   */
  String getCmdAnchor(String command);

  /**
   * Generates an absolute URL to the servlet with a command specified,
   * using the standard domain name.
   */
  String getStandardCmdUrl(String command);

  /**
   * Generates an absolute URL to the servlet with the command specified,
   * using the domain pulled from the Context specified.
   *  <p>
   * NOTE: This does NOT use getStandardDomain().
   */
  <C extends Context> String getStandardCmdUrl(String command, C context);

  /**
   * Generates an absolute URL to the servlet with a command specified,
   * using the secure (SSL) domain name.
   */
  String getSecureCmdUrl(String command);

  /**
   * Returns a reference to the String value currently assigned for the
   * "urlDirectoryPrefix" member variable of BasicInfrastructure.
   */
  String getURLDirectoryPrefix();

  /**
   * Determines if JSPs are allowed to be invoked directly by URL.
   */
  boolean canInvokeJSPsDirectly();

  /**
   * Gets a reference to the flag variable that indicates whether this
   * application should use the urlDirectoryPrefix string when making calls
   * to Content.includeJSP(...)
   */
  boolean useURLDirectoryPrefix();

  /**
   * Returns the identity of the Gemini instance. Useful for distinguishing between
   * different deployments of the same application ie Development, Staging, Production.
   *
   * @return The identity of the deployment
   */
  String identify();

  /**
   * Gets the CSS directory. Will attempt to derive the context in order
   * to determine if the secure directory should be returned.
   */
  String getCssDirectory();

  /**
   * Gets the CSS directory. Will use the given context in order
   * to determine if the secure directory should be returned.
   */
  String getCssDirectory(Context context);

  /**
   * Gets the JavaScript directory. Will attempt to derive the context in order
   * to determine if the secure directory should be returned.
   */
  String getJavaScriptDirectory();

  /**
   * Gets the JavaScript directory. Will use the given context in order
   * to determine if the secure directory should be returned.
   */
  <C extends Context> String getJavaScriptDirectory(C context);

  /**
   * Gets the images directory.  When specified in the application's .conf
   * file, the directory should have a trailing slash included. Will use the
   * given context in order to determine if the secure directory should
   * be returned.
   */
  <C extends Context> String getImageDirectory(C context);
  
}   // End Infrastructure interface.
