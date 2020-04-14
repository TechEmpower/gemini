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

import com.techempower.*;
import com.techempower.cache.*;
import com.techempower.data.*;
import com.techempower.gemini.email.*;
import com.techempower.gemini.email.outbound.*;
import com.techempower.gemini.internationalization.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.session.*;
import com.techempower.log.*;
import com.techempower.scheduler.*;

/**
 * The methods provided by GeminiApplication are captured by this interface.
 * The utility of this interface is in creating Gemini services, which often
 * expect additional methods to exist in the Application subclass.  The
 * service will typically provide an interface dictating what additional
 * methods are required and then Application subclasses implement that 
 * interface in order to use the service.  E.g.,
 * 
 * <p><pre>
 * public class MyApplication
 *   extends    GeminiApplication
 *   implements OrionApplication
 * </pre><p>
 * 
 * The interesting bit is that by allowing the OrionApplication interface to
 * extend GeminiApplicationInterface, the Orion service can interact with
 * the standard GeminiApplication methods while referring to the application
 * as just an OrionApplication. 
 */
public interface GeminiApplicationInterface
{

  /**
   * Gets a reference to the Infrastructure object for this application.
   */
  BasicInfrastructure getInfrastructure();

  /**
   * Gets a reference to the Configurator object for this application.
   */
  Configurator getConfigurator();

  /**
   * Gets a reference to the Dispatcher object for this application.
   */
  Dispatcher getDispatcher();
  
  /**
   * Gemini applications are now encouraged to store a reference to a database
   * ConnectorFactory object for use by other aspects of the application.
   * Previously, these objects may have been stored within the static Helper
   * class, but this does not allow core Gemini components a convenient way to
   * access the ConnectorFactory.  By putting a reference to the connector
   * factory in the application, core Gemini components can make use of
   * database resources.
   */
  ConnectorFactory getConnectorFactory(); 

  /**
   * Gets a reference to the EmailServicer object for this application.
   */
  EmailServicer getEmailServicer();

  /**
   * Gets a reference to the EmailTransport object for this application.
   */
  EmailTransport getEmailTransport();

  /**
   * Gets a reference to the EmailTemplater object for this application.
   */
  EmailTemplater getEmailTemplater();

  /**
   * Gets a reference to the EntityStore object for this application.
   */
  EntityStore getStore();

  /**
   * Gets a reference to the GeminiLocaleManager object for this application.
   */
  GeminiLocaleManager getLocaleManager();

  /**
   * Constructs an instance of a subclass of Context, provided the
   * parameters used to construct Context objects.
   */
  Context getContext(Request request);

  /**
   * Gets the SessionManager instance for this application.
   */
  SessionManager getSessionManager();

  /**
   * Gets the ServletConfig that was provided to the Servlet when
   * its init method was called.
   */
  InitConfig getServletConfig();

  /**
   * Gets a Version object for this application.  The Version holds items
   * such as the name and version number of the application.
   */
  Version getVersion();

  /**
   * Gets a ComponentLog for a component.
   */
  ComponentLog getLog(String componentCode);

  /**
   * Gets the actual application log reference.
   */
  Log getApplicationLog();
  
  /**
   * Gets a reference to the PyxisSecurity object from an Application.
   * This method should either use lazy instantiation (that is, check to
   * see if the reference is null and then create a new reference, if
   * necessary) or overload the GeminiApplication's constructor to create
   * a new reference at application construction time.
   *   <p>
   * Note that it is also necessary for a PyxisSecurity implementation to
   * correctly ensure that it will be configured by the application's
   * Configurator.
   */
  PyxisSecurity getSecurity();
  
  /**
   * Gets the Scheduler for this application.
   */
  Scheduler getScheduler();
  
}   // End GeminiApplicationInterface.
