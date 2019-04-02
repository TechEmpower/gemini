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

import java.util.concurrent.*;

/**
 * The ApplicationRegistrar allows instances of applications to locate one
 * another, or for components that comprise a single application but are
 * invoked at multiple call-points to copy references.
 *   <p>
 * All GeminiApplication instances automatically get registered with the
 * ApplicationRegistrar with the application name provided by their Version
 * object.  If an environment requires multiple instances to co-operate in
 * the same virtual machine, the GeminiApplication.registerSelf() and
 * GeminiApplication.deregisterSelf() methods should be overloaded.
 */
public final class ApplicationRegistrar
{

  //
  // Static variables.
  //
  
  private static final ConcurrentMap<String,GeminiApplication> TABLE = 
      new ConcurrentHashMap<>(10);
  public static final String MAIN_APPLICATION_KEY = "default";
  
  //
  // Static methods.
  //
  
  /**
   * Registers an application using the application's name (as found in its
   * Version object) as the identifier.  Applications are registered
   * automatically by the GeminiApplication class at application start and
   * de-registered at application end.
   * 
   * @param app a reference to the application instance.
   */
  public static void register(GeminiApplication app)
  {
    register(app.getVersion().getProductName(), app);
  }
  
  /**
   * Registers an application as the main application.
   * 
   * @param app a reference to the application instance.
   */
  public static void registerMain(GeminiApplication app)
  {
    register(app);
    register(MAIN_APPLICATION_KEY, app);
  }
  
  /**
   * Registers an application.
   * 
   * @param app a reference to the application instance.
   * @param identifier a String identifier to use for lookup.
   */
  public static void register(String identifier, GeminiApplication app)
  {
    TABLE.put(identifier, app);
  }
  
  /**
   * De-registers an application.
   * 
   * @param app a reference to the application instance.
   */
  public static void deregister(GeminiApplication app)
  {
    deregister(app.getVersion().getProductName());
  }
  
  /**
   * Deregisters an application.
   * 
   * @param identifier a String identifier to use for lookup.
   */
  public static void deregister(String identifier)
  {
    TABLE.remove(identifier);
  }

  /**
   * Gets an application based on its identifier.  Returns null if no
   * such application is registered.
   * 
   * @param identifier the String identity of the application, usually
   *        the application's name from its Version object.
   */
  public static GeminiApplication lookup(String identifier)
  {
    return TABLE.get(identifier);
  }
  
  /**
   * Gets the default application, which in the vast majority of cases will
   * be the only application that is registered.
   */
  public static GeminiApplication getMain()
  {
    return lookup(MAIN_APPLICATION_KEY); 
  }
  
  /**
   * No constructor.
   */
  private ApplicationRegistrar()
  {
    // Does nothing.
  }
  
}  // End ApplicationRegistrar.
