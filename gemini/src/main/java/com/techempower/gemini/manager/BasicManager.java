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
package com.techempower.gemini.manager;

import com.techempower.cache.*;
import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * Provides baseline functionality for Manager classes.  The most common
 * design pattern for Managers is one per major chunk of application 
 * functionality.  A single instance of each type of Manager is created and
 * made available through the Application object, allowing Managers to be used
 * by Handlers and other Managers as needed.
 *   <p>
 * A Manager is typically responsible for some subsection of an application's
 * functionality such as managing a shopping cart, articles, or integration
 * with a credit card processor. 
 *   <p>
 * It is common for applications to inherit from an intermediate subclass of
 * BasicManager such as BasicPathManager.
 * 
 * @see com.techempower.gemini.path.BasicPathManager
 */
public class BasicManager<A extends GeminiApplication>
  implements Configurable 
{

  private final A            application;
  private final ComponentLog log;

  /**
   * Constructor.
   */
  public BasicManager(A application, String componentCode)
  {
    this.application = application;
    this.log = application.getLog(componentCode);
    application.getConfigurator().addConfigurable(this);
  }

  /**
   * Gets a reference to the application.
   */
  protected A app()
  {
    return application;
  }
  
  /**
   * Gets a reference to the log.
   */
  protected ComponentLog log()
  {
    return log;
  }
  
  /**
   * Gets a reference to the EntityStore.
   */
  protected EntityStore store()
  {
    return application.getStore();
  }
  
  /**
   * Gets the application's Security.
   */
  protected PyxisSecurity security()
  {
    return application.getSecurity(); 
  }
  
  /**
   * Write something to the log.  This is just a convenience alias for
   * log.debug.
   */
  protected void l(String toLog) 
  {
    log.log(toLog);
  }
  
  /**
   * Write something to the log.  This is just a convenience alias for
   * log.debug.
   */
  protected void l(String toLog, int debugLevel) 
  {
    log.log(toLog, debugLevel);
  }
  
  /**
   * Write something to the log.  This is just a convenience alias for
   * log.debug.
   */
  protected void l(String toLog, Throwable throwable) 
  {
    log.log(toLog, LogLevel.ALERT, throwable);
  }
  
  /**
   * Write something to the log.  This is just a convenience alias for
   * log.debug.
   */
  protected void l(String toLog, int debugLevel, Throwable throwable) 
  {
    log.log(toLog, debugLevel, throwable);
  }
  
  @Override
  public void configure(EnhancedProperties props) 
  {
    // Does nothing in this base class.
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName();
  }

}
