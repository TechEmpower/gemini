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

import com.techempower.gemini.monitor.*;
import com.techempower.gemini.mustache.*;
import com.techempower.gemini.session.*;

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
 * <li>CommandHistorySize - The maximum number of RequestSignatures to store
 *     in a a CommandHistory that is built for each user's session.  Note
 *     that unless your application uses context.putIntoCommandHistory, this
 *     value is meaningless and can be ignored.
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
 *   3. Evaluate the configuration state (pause here until good).
 *   4. Post-initialize, which sets up the data cache.
 *   5. Begin accepting requests and start asynchronous resources.
 *   <p>
 * You will see these steps in InfrastructureServlet.init().
 */
public abstract class ResinGeminiApplication
  extends    GeminiApplication
{

  /**
   * Overload: Constructs a Dispatcher reference.  Overload to return a custom
   * object -or- to construct a basic Dispatcher and call addHandler as necessary
   * to add custom Handlers to the Dispatcher.
   *   <p>
   * For example,
   *   <p><pre>
   * return new MyApplicationDispatcher(this); </pre></p>
   * -or-
   *   <p><pre>
   * BasicDispatcher toReturn = new BasicDispatcher(this);
   * toReturn.addHandler(new HomeHandler(this));
   * toReturn.addHandler(new AdministrationHandler(this));
   * return toReturn; </pre></p>
   */
  @Override
  protected Dispatcher constructDispatcher()
  {
    return new BasicDispatcher(this);
  }

  /**
   * Overload: Constructs an HttpSessionManager reference.  Overload to return a
   * custom object.  It is not likely that a application would need to subclass
   * HttpSessionManager.
   */
  @Override
  protected SessionManager constructSessionManager()
  {
    return new HttpSessionManager(this);
  }

  /**
   * Constructs a GeminiMonitor reference.
   */
  @Override
  protected GeminiMonitor constructMonitor()
  {
    return new ResinGeminiMonitor(this);
  }

  /**
   * Overload: Constructs an instance of a subclass of Context, provided the
   * parameters used to construct Context objects.  Note that it is NO
   * LONGER necessary to overload this method if your application is not using
   * a special subclass of Context.
   */
  @Override
  public Context getContext(Request request)
  {
    return new ResinContext(request, this);
  }

  @Override
  protected MustacheManager constructMustacheManager()
  {
    return new ResinMustacheManager(this);
  }

}  // End GeminiApplication.
