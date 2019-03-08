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
package com.techempower.gemini.path;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.exceptionhandler.*;
import com.techempower.gemini.prehandler.*;
import com.techempower.log.*;

/**
 * A Dispatcher implementation that directs requests based on the first path
 * segment ("directory") in the URI and provides the remaining path segments 
 * as parameters to the handlers.
 *   <p>
 * Example usage:
 *   <pre>
 *   final PathDispatcher.Configuration&lt;TempestContext&gt; configuration = 
 *       new PathDispatcher.Configuration&lt;&gt;();
 *
 *   // Add handlers.
 *   configuration.add("git", new GitHandler(this))
 *                .add("task", new TaskHandler(this))
 *                .add("test", new PerformanceTest(this));
 *
 *   // Add listeners.
 *   configuration.add(getMonitor().getListener());
 *       
 *   // Add exception handlers.
 *   configuration.add(new BasicExceptionHandler(this))
 *                .add(new NotificationExceptionHandler(this));
 * 
 *   // Set a default handler.
 *   configuration.setDefault(new HomeHandler(this));
 *   
 *   // Construct and return the PathDispatcher.
 *   return new PathDispatcher<>(this, configuration);
 * </pre>
 */
public class   PathDispatcher<A extends GeminiApplication, C extends Context>
    implements Dispatcher
{

  //
  // Member variables.
  //
  
  private final A                           application;
  private final ComponentLog                log;
  private final Map<String, PathHandler<C>> handlers;
  private final PathHandler<C>              defaultHandler;
  private final PathHandler<C>              rootHandler;
  private final ExceptionHandler[]          exceptionHandlers;
  private final Prehandler[]                prehandlers;
  private final DispatchListener[]          listeners;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   * 
   * @param application The application reference.
   * @param configuration A Configuration providing references to the desired
   *     PathHandlers, ExceptionHandlers, etc.
   */
  public PathDispatcher(A application, Configuration<C> configuration)
  {
    this.application       = application;
    this.log               = application.getLog("disp");
    this.handlers          = new HashMap<>(configuration.handlers);
    this.exceptionHandlers = configuration.exceptionHandlers.toArray(
        new ExceptionHandler[configuration.exceptionHandlers.size()]);
    this.prehandlers       = configuration.prehandlers.toArray(
        new Prehandler[configuration.prehandlers.size()]);
    this.listeners         = configuration.listeners.toArray(
        new DispatchListener[configuration.listeners.size()]);
    if (configuration.defaultHandler != null)
    {
      defaultHandler = configuration.defaultHandler;
    }
    else
    {
      defaultHandler = new FourZeroFourHandler<>();
    }

    rootHandler = configuration.rootHandler;

    if (exceptionHandlers.length == 0)
    {
      throw new IllegalArgumentException("PathDispatcher must be configured with at least one ExceptionHandler.");
    }
  }
  
  /**
   * Gets a Handler given a root path segment as a key.  E.g., 
   * dispatcher.get("task")
   * 
   * @param rootPathSegment The root-level path segment; e.g., "task"
   */
  public PathHandler<C> get(String rootPathSegment)
  {
    return handlers.get(rootPathSegment);
  }
  
  /**
   * Gets a set of the root path segments that the Dispatcher handles.
   */
  public Set<String> getRootPathSegments()
  {
    return handlers.keySet();
  }
  
  /**
   * Gets the Application reference.
   */
  protected A getApplication()
  {
    return application;
  }
  
  /**
   * Gets the ComponentLog reference.
   */
  protected ComponentLog getLog()
  {
    return log;
  }
  
  /**
   * Send the request to all prehandlers.
   */
  protected boolean prehandle(C context)
  {
    final Prehandler[] thePrehandlers = prehandlers;
    for (Prehandler p : thePrehandlers)
    {
      if (p.prehandle(context))
      {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public boolean dispatch(Context plainContext)
  {
    boolean success = false;
    
    // Surround all logic with a try-catch so that we can send the request to
    // our ExceptionHandlers if anything goes wrong.
    try
    {    
      // Cast the provided Context to a C.
      @SuppressWarnings("unchecked")
      final C context = (C)plainContext;
      
      // Convert the request URI into path segments.
      final PathSegments segments = new PathSegments(context.getRequestUri());
      
      // Make these references available thread-locally.
      RequestReferences.set(context, segments);
      
      // Notify listeners.
      notifyListenersDispatchStarting(plainContext, segments.getUriFromRoot());

      // Find the associated Handler. 
      PathHandler<C> handler = null;
      
      if (segments.getCount() > 0)
      {
        handler = get(segments.get(0));

        // If we've found a Handler to use, we have consumed the first path
        // segment.
        if (handler != null)
        {
          segments.increaseOffset();
        }
      }
      // Use the root handler when the segment count is 0.
      else if (rootHandler != null)
      {
        handler = rootHandler;
      }

      // Use the default handler if nothing else was provided.
      if (handler == null)
      {
        handler = defaultHandler;
      }
      
      // Send the request to all Prehandlers.
      success = prehandle(context);
      
      // Proceed to normal Handlers if the Prehandlers did not fully handle
      // the request.
      if (!success)
      {
        try
        {
          // Do preliminary processing.  The prehandle method may fully handle
          // the request, and if it does so, it will return true.
          success = handler.prehandle(segments, context);
          
          // Proceed to the handle method if the prehandle method did not fully
          // handle the request on its own.
          if (!success)
          {
            // Dispatch.
            success = handler.handle(segments, context);
          }
        }
        finally
        {
          // Do wrap-up processing even if the request was not handled correctly.
          handler.posthandle(segments, context);
        }
      }
  
      // If the handler we selected did not successfully handle the request
      // and it's NOT the default handler, let's ask the default handler to
      // handle the request.
      if (  (!success)
         && (handler != defaultHandler)
         )
      {
        try
        {
          // Result of prehandler is ignored because the default handler is 
          // expected to handle any request.  For the default handler, we'll
          // reset the PathSegments offset to 0.
          success = defaultHandler.prehandle(segments.offset(0), context);

          if (!success)
          {
            success = defaultHandler.handle(segments, context);
          }
        }
        finally
        {
          defaultHandler.posthandle(segments, context);
        }
      }
    }
    catch (Throwable exc)
    {
      dispatchException(plainContext, exc, null);
    }
    finally
    {
      RequestReferences.remove();
    }
    
    return success;
  }

  @Override
  public void dispatchComplete(Context context)
  {
    notifyListenersDispatchComplete(context);
  }

  @Override
  public void dispatchException(Context context, Throwable exception,
      String description)
  {
    if (exception == null)
    {
      log.log("dispatchException called with a null reference.", 
          LogLevel.ALERT);
      return;
    }
    
    try
    {
      final ExceptionHandler[] theHandlers = exceptionHandlers;
      for (ExceptionHandler handler : theHandlers)
      {
        if (description != null)
        {
          handler.handleException(context, exception, description);
        }
        else
        {
          handler.handleException(context, exception);
        }
      }
    }
    catch (Exception exc)
    {
      // In the especially worrisome case that we've encountered an exception
      // while attempting to handle another exception, we'll give up on the
      // request at this point and just write the exception to the log.
      log.log("Exception encountered while processing earlier " + exception, 
          LogLevel.ALERT, exc);
    }
  }
  
  /**
   * Notify the listeners that a dispatch is starting.
   */
  protected void notifyListenersDispatchStarting(Context context, String command)
  {
    final DispatchListener[] theListeners = listeners;
    for (DispatchListener listener : theListeners)
    {
      listener.dispatchStarting(this, context, command);
    }
  }
  
  /**
   * Notify the listeners that a dispatch is complete.
   */
  protected void notifyListenersDispatchComplete(Context context)
  {
    final DispatchListener[] theListeners = listeners;
    for (DispatchListener listener : theListeners)
    {
      listener.dispatchComplete(this, context);
    }
  }
  
  /**
   * Notify the listeners that a rendering has started.
   */
  @Override
  public void renderStarting(Context context, String jspName)
  {
    final DispatchListener[] theListeners = listeners;
    for (DispatchListener listener : theListeners)
    {
      listener.renderStarting(this, jspName);
    }
  }
  
  /**
   * Notify the listeners that a JSP has been completed.
   */
  @Override
  public void renderComplete(Context context)
  {
    final DispatchListener[] theListeners = listeners;
    for (DispatchListener listener : theListeners)
    {
      listener.renderComplete(this, context);
    }
  }

  /** 
   * A class used for the initial configuration of a PathDispatcher.
   */
  public static class Configuration<C extends Context>
  {
    private       PathHandler<C> defaultHandler;
    private       PathHandler<C> rootHandler;
    private final Map<String, PathHandler<C>> handlers;
    private final List<ExceptionHandler> exceptionHandlers;
    private final List<Prehandler> prehandlers;
    private final List<DispatchListener> listeners;
    
    /**
     * Constructor.
     */
    public Configuration()
    {
      handlers = new HashMap<>();
      exceptionHandlers = new ArrayList<>();
      prehandlers = new ArrayList<>();
      listeners = new ArrayList<>();
    }
    
    /**
     * Add a Handler with a specified root path segment to this Configuration.
     */
    public Configuration<C> add(String rootPathSegment, 
        PathHandler<C> handler)
    {
      if (handler instanceof UriAware)
      {
        ((UriAware)handler).setBaseUri("/" + rootPathSegment);
      }
      
      handlers.put(rootPathSegment, handler);
      return this;
    }
    
    /**
     * Adds an ExceptionHandler.
     */
    public Configuration<C> add(ExceptionHandler exceptionHandler)
    {
      exceptionHandlers.add(exceptionHandler);
      return this;
    }
    
    /**
     * Adds a Prehandler.
     */
    public Configuration<C> add(Prehandler prehandler)
    {
      prehandlers.add(prehandler);
      return this;
    }
    
    /**
     * Adds a DispatchListener.
     */
    public Configuration<C> add(DispatchListener listener)
    {
      listeners.add(listener);
      return this;
    }
    
    /**
     * Set the default handler.
     */
    public Configuration<C> setDefault(PathHandler<C> handler)
    {
      defaultHandler = handler;
      return this;
    }

    /**
     * Set the root handler.
     */
    public Configuration<C> setRootHandler(PathHandler<C> handler)
    {
      rootHandler = handler;
      return this;
    }
  }
}
