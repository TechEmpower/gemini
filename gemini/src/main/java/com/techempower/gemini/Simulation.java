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

import com.techempower.gemini.log.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.simulation.*;

/**
 * Simulation class that simulates an "entry point" into an application. Another way
 * to put it is this class simulates the InfrastructureServlet class, which configures
 * a gemini application and handles requests.
 * 
 * This Simulation class is meant to be used to test out requests without requiring
 * a server. Example usages would be like this
 */
public abstract class Simulation
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "simu";    // Four-letter component ID

  //
  // Member variables.
  //
  
  private final GeminiApplication application;
  
  private final String            docroot;
  
  public Simulation()
  {
    this.application = getApplication();
    this.docroot = getDocroot();
    
    init();
  }
  
  /**
   * Handles the init call.  Note that as of Gemini v1.23, it is no longer
   * recommended to use this init() method as a place to start asynchronous
   * resources.  See GeminiApplication.addAsynchronous() for the new
   * recommended approach.
   */
  public void init()
  {
    InitConfig config = null;
    if (getServletConfig() != null)
    {
      config = getServletConfig();
    }
    
    // Initialize the application.
    this.application.initialize(config);
  }

  /**
   * Create a Simulation initialization configuration (which is an 
   * approximation of ServletConfig from the Servlet API). 
   */
  public InitConfig getServletConfig()
  {
    return new SimConfig(this.docroot);
  }

  public Context postRequest(SimClient client, String url, Map<String, String> parameters)
  {
    if (this.application.isRunning())
    {
      SimRequest request = new PostSimRequest(this, url, parameters, client, this.application);
        
      final Context context = this.application.getContext(request);
      
      handleRequest(context);
      
      return context;
    }
    else
    {
      if (this.application.getState() == GeminiApplication.OperationalState.NEW)
      {
        Map<String, Object> toReturn = new HashMap<>();
        toReturn.put("Error", "Application not yet initialized.");
        return null;
      }
      else
      {
        Map<String, Object> toReturn = new HashMap<>();
        toReturn.put("Error", "Application not running.");
        return null;
      }
    }
  }
  
  public Context getRequest(SimClient client, String url, Map<String, String> parameters)
  {
    if (this.application.isRunning())
    {
      SimRequest request = new GetSimRequest(this, url, parameters, client, this.application);
        
      final Context context = this.application.getContext(request);
      
      handleRequest(context);
      
      return context;
    }
    else
    {
      if (this.application.getState() == GeminiApplication.OperationalState.NEW)
      {
        Map<String, Object> toReturn = new HashMap<>();
        toReturn.put("Error", "Application not yet initialized.");
        return null;
      }
      else
      {
        Map<String, Object> toReturn = new HashMap<>();
        toReturn.put("Error", "Application not running.");
        return null;
      }
    }
  }
  
  /**
   * Assuming the application is running and is in a good state, this method
   * will be called to handle the request.
   *   <p>
   * Overload this method if you do not want to use the dispatcher.
   */
  public void handleRequest(Context context)
  {
    // Identify the current thread if we are counting requests.
    String threadName = null;
    if (this.application.isRequestCounting())
    {
      threadName = Thread.currentThread().getName();
      final long requestNumber = this.application.incrementRequestCount();
      Thread.currentThread().setName(threadName + " (Request " + requestNumber + ")");
      context.setRequestNumber(requestNumber);
    }
    
    try
    {
      // Set the Context information to be displayed with every log message.
      ContextLogInfo.setContextInformation(context);
  
      this.application.getDispatcher().dispatch(context);
    }
    finally
    {
      // Notify the Dispatcher that we're done with this request.
      this.application.getDispatcher().dispatchComplete(context);
      
      // Clear the Context info now that this Thread is done handling the request.
      ContextLogInfo.clearContextInformation();
      
      // The current Context's usage is now complete, dissociate it with
      // the current thread.
      BasicContext.complete();
      
      // Remove the request number from the thread's name.
      if ( (this.application.isRequestCounting())
        && (threadName != null)
        )
      {
        Thread.currentThread().setName(threadName);
      }
    }
  }
  
  /**
   * Helper method that will automatically login the specified user, returns false
   * if the username doesn't exist
   */
  public boolean login(SimClient client, String username)
  {
    // setup
    SimRequest request = new PostSimRequest(this, "", null, client, this.application); 
    final Context context = this.application.getContext(request);
    
    BasicUser user = null;
    
    Collection<? extends BasicUser> users = this.application.getStore().list(getUserClass());
    for(BasicUser u : users)
    {
      if(u.getUserUsername().equals(username))
      {
        user = u;
        break;
      }
    }
    
    if(user != null)
    {
      this.application.getSecurity().login(context, user, false);
      return true;
    }
    
    return false;
  }
  
  public void destroy()
  {
    this.application.end();
  }
  
  public abstract GeminiApplication getApplication();
  
  protected abstract String getDocroot();
  
  protected abstract Class<? extends BasicUser> getUserClass();
}
