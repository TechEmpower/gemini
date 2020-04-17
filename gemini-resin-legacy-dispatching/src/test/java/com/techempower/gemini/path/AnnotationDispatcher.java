/*******************************************************************************
 * Copyright (c) 2020, TechEmpower, Inc.
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

import com.techempower.classloader.PackageClassLoader;
import com.techempower.gemini.*;
import com.techempower.gemini.configuration.ConfigurationError;
import com.techempower.gemini.exceptionhandler.ExceptionHandler;
import com.techempower.gemini.path.annotation.Path;
import com.techempower.gemini.prehandler.Prehandler;
import com.techempower.helper.NetworkHelper;
import com.techempower.helper.StringHelper;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.techempower.gemini.Request.*;
import static com.techempower.gemini.Request.HEADER_ACCESS_CONTROL_EXPOSED_HEADERS;
import static com.techempower.gemini.Request.HttpMethod.*;

public class AnnotationDispatcher<C extends Context> implements Dispatcher {

  //
  // Member variables.
  //

  //private final GeminiApplication               app;
  private final Map<String, AnnotationHandler>  handlers;
  private final ExceptionHandler[]              exceptionHandlers;
  private final Prehandler[]                    prehandlers;
  private final DispatchListener[]              listeners;

  private ExecutorService  preinitializationTasks = Executors.newSingleThreadExecutor();
  private Reflections      reflections            = null;

  public AnnotationDispatcher(/*GeminiApplication application*/)
  {
    //app = application;
    handlers = new HashMap<>();
    exceptionHandlers = new ExceptionHandler[]{};
    prehandlers       = new Prehandler[]{};
    listeners         = new DispatchListener[]{};

    /*if (exceptionHandlers.length == 0)
    {
      throw new IllegalArgumentException("PathDispatcher must be configured with at least one ExceptionHandler.");
    }*/

    //startReflectionsThread();
  }

  /*private void startReflectionsThread()
  {
    // Start constructing Reflections on a new thread since it takes a
    // bit of time.
    preinitializationTasks.submit(new Runnable() {
      @Override
      public void run() {
        try
        {
          reflections = PackageClassLoader.getReflectionClassLoader(app);
        }
        catch (Exception exc)
        {
          // todo
//                    log.log("Exception while instantiating Reflections component.", exc);
        }
      }
    });
  }*/

  /*public void initialize() {
    // Wait for pre-initialization tasks to complete.
    try
    {
//            log.log("Completing preinitialization tasks.");
      preinitializationTasks.shutdown();
//            log.log("Awaiting termination of preinitialization tasks.");
      preinitializationTasks.awaitTermination(5L, TimeUnit.MINUTES);
//            log.log("Preinitialization tasks complete.");
//            log.log("Reflections component: " + reflections);
    }
    catch (InterruptedException iexc)
    {
//            log.log("Preinitialization interrupted.", iexc);
    }

    // Throw an exception if Reflections is not ready.
    if (reflections == null)
    {
      throw new ConfigurationError("Reflections not ready; application cannot start.");
    }

    //register();
  }*/

  /*private void register() {
//        log.log("Registering annotated entities, relations, and type adapters.");
    try {
      final ExecutorService service = Executors.newFixedThreadPool(1);

      // @Path-annotated classes.
      service.submit(new Runnable() {
        @Override
        public void run() {
          for (Class<?> clazz : reflections.getTypesAnnotatedWith(Path.class)) {
            final Path annotation = clazz.getAnnotation(Path.class);

            try {
              handlers.put(annotation.value(),
                  new AnnotationHandler(annotation.value(),
                      clazz.getDeclaredConstructor().newInstance()));
            }
            catch (NoSuchMethodException nsme) {
              // todo
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
              // todo
            }
          }
        }
      });

      try
      {
        service.shutdown();
        service.awaitTermination(1L, TimeUnit.HOURS);
      }
      catch (InterruptedException iexc)
      {
//                log.log("Unable to register all entities in 1 hour!", LogLevel.CRITICAL);
      }

//            log.log("Done registering annotated items.");
    }
    catch (ReflectionsException e)
    {
      throw new RuntimeException("Warn: problem registering class with reflection", e);
    }
  }*/

  public void register(Object resource) {
    Class<?> clazz = resource.getClass();
    Path annotation = clazz.getAnnotation(Path.class);
    try
    {
      handlers.put(annotation.value(),
          new AnnotationHandler(annotation.value(),
              clazz.getDeclaredConstructor().newInstance()));
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
    {
      throw new RuntimeException(e);
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

    // Returning false indicates we did not fully handle this request and
    // processing should continue to the handle method.
    return false;
  }

  @Override
  public boolean dispatch(Context plainContext) {
    throw new UnsupportedOperationException();
  }

  public boolean dispatch(/*Context plainContext*/Request.HttpMethod httpMethod, String uri) {
    boolean success = false;

    // Surround all logic with a try-catch so that we can send the request to
    // our ExceptionHandlers if anything goes wrong.
    try
    {
      // Cast the provided Context to a C.
      //@SuppressWarnings("unchecked")
      //final C context = (C)plainContext;

      // Convert the request URI into path segments.
      final PathSegments segments = new PathSegments(uri);

      // Any request with an Origin header will be handled by the app directly,
      // however there are some headers we need to set up to add support for
      // cross-origin requests.
      /*if(context.headers().get(HEADER_ORIGIN) != null)
      {
        //addCorsHeaders(context);

        if(((Request)context.getRequest()).getRequestMethod() == OPTIONS)
        {
          //addPreflightCorsHeaders(segments, context);
          // Returning true indicates we did fully handle this request and
          // processing should not continue.
          return true;
        }
      }*/

      // Make these references available thread-locally.
      //RequestReferences.set(context, segments);

      // Notify listeners.
      //notifyListenersDispatchStarting(plainContext, segments.getUriFromRoot());

      // Find the associated Handler.
      AnnotationHandler<C> handler = null;

      if (segments.getCount() > 0)
      {
        handler = this.handlers.get(segments.get(0));

        // If we've found a Handler to use, we have consumed the first path
        // segment.
        if (handler != null)
        {
          segments.increaseOffset();
        }
      }
      /**
       * todo: We no longer have the notion of a 'rootHandler'.
       *       This can be accomplished by having a POJO annotated with
       *       `@Path("/")` to denote the root uri and a single method
       *       annotated with `@Path()` to handle the root request.
       */
      // Use the root handler when the segment count is 0.
//            else if (rootHandler != null)
//            {
//                handler = rootHandler;
//            }

      /**
       * todo: We no longer have the notion of a 'defaultHandler'.
       *       This can be accomplished by having a POJO annotated with
       *       `@Path("*")` to denote the wildcard uri and a single
       *       method annotated with `@Path("*")` to handle any request
       *       routed there.
       */
      // Use the default handler if nothing else was provided.
//            if (handler == null)
//            {
//                // The HTTP method for the request is not listed in the HTTPMethod enum,
//                // so we are unable to handle the request and simply return a 501.
//                if (((Request)plainContext.getRequest()).getRequestMethod() == null)
//                {
//                    handler =  notImplementedHandler;
//                }
//                else
//                {
//                    handler = defaultHandler;
//                }
//            }

      // TODO: I don't know how I want to handle `prehandle` yet.
      success = false; // this means we didn't prehandle
      // Send the request to all Prehandlers.
//            success = prehandle(context);

      // Proceed to normal Handlers if the Prehandlers did not fully handle
      // the request.
      if (!success)
      {
        try
        {
          // Proceed to the handle method if the prehandle method did not fully
          // handle the request on its own.
          success = handler.handle(segments, httpMethod);
        }
        finally
        {
          // todo: I'm not sure how to do `posthandle` yet.
          // Do wrap-up processing even if the request was not handled correctly.
//                    handler.posthandle(segments, context);
        }
      }

      /**
       * TODO: again, we don't have a `defaultHandler` anymore except by
       *       routing to a POJO annotated with `@Path("*")` and a method
       *       annotated with `@Path("*")`.
       */
      // If the handler we selected did not successfully handle the request
      // and it's NOT the default handler, let's ask the default handler to
      // handle the request.
//            if (  (!success)
//                    && (handler != defaultHandler)
//            )
//            {
//                try
//                {
//                    // Result of prehandler is ignored because the default handler is
//                    // expected to handle any request.  For the default handler, we'll
//                    // reset the PathSegments offset to 0.
//                    success = defaultHandler.prehandle(segments.offset(0), context);
//
//                    if (!success)
//                    {
//                        defaultHandler.handle(segments, context);
//                    }
//                }
//                finally
//                {
//                    defaultHandler.posthandle(segments, context);
//                }
//            }
    }
    catch (Throwable exc)
    {
      throw new RuntimeException(exc);
      //dispatchException(plainContext, exc, null);
    }
    finally
    {
      //RequestReferences.remove();
    }

    return success;
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

  @Override
  public void dispatchComplete(Context context) {
    notifyListenersDispatchComplete(context);
  }

  @Override
  public void renderStarting(Context context, String renderingName) {
    // Intentionally left blank
  }

  @Override
  public void renderComplete(Context context) {
    // Intentionally left blank
  }

  @Override
  public void dispatchException(Context context, Throwable exception, String description) {
    if (exception == null)
    {
//            log.log("dispatchException called with a null reference.",
//                    LogLevel.ALERT);
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
//            log.log("Exception encountered while processing earlier " + exception,
//                    LogLevel.ALERT, exc);
    }
  }

  /**
   * Gets the Header-appropriate string representation of the http method
   * names that this handler supports for the given path segments.
   * <p>
   * For example, if this handler has two handle methods at "/" and
   * one is GET and the other is POST, this method would return the string
   * "GET, POST" for the PathSegments "/".
   * <p>
   * By default, this method returns "GET, POST", but subclasses should
   * override for more accurate return values.
   */
  protected String getAccessControlAllowMethods(PathSegments segments,
                                                C context)
  {
    // todo: map of routes-to-handler-tuples that expresses something like
    //       /foo/bar -> { class, method, HttpMethod }
    //       for lookup here.
    // todo: this is also probably wrong in BasicPathHandler
    return HttpMethod.GET + ", " + HttpMethod.POST;
  }


  /**
   * Adds the standard headers required for CORS support in all requests
   * regardless of being preflight.
   * @see <a href="
   * https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Origin">
   * Access-Control-Allow-Origin</a>
   * @see <a href="
   * https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Requests_with_credentials">
   * Access-Control-Allow-Credentials</a>
   */
  /*private void addCorsHeaders(C context)
  {
    // Applications may configure whitelisted origins to which cross-origin
    // requests are allowed.
    if(NetworkHelper.isWebUrl(context.headers().get(HEADER_ORIGIN)) &&
        app.getSecurity().getSettings().getAccessControlAllowedOrigins()
            .contains(context.headers().get(HEADER_ORIGIN).toLowerCase()))
    {
      // If the server specifies an origin host rather than wildcard, then it
      // must also include Origin in the Vary response header.
      context.headers().put(HEADER_VARY, HEADER_ORIGIN);
      context.headers().put(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
          context.headers().get(HEADER_ORIGIN));
      // Applications may configure the ability to allow credentials on CORS
      // requests, but only for domain-specified requests. Wildcards cannot
      // allow credentials.
      if(app.getSecurity().getSettings().accessControlAllowCredentials())
      {
        context.headers().put(
            HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
      }
    }
    // Applications may also configure wildcard origins to be whitelisted for
    // cross-origin requests, effectively making the application an open API.
    else if(app.getSecurity().getSettings().getAccessControlAllowedOrigins()
        .contains(HEADER_WILDCARD))
    {
      context.headers().put(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
          HEADER_WILDCARD);
    }
    // Applications may configure whitelisted headers which browsers may
    // access on cross origin requests.
    if(!app.getSecurity().getSettings().getAccessControlExposedHeaders().isEmpty())
    {
      boolean first = true;
      final StringBuilder exposed = new StringBuilder();
      for(final String header : app.getSecurity().getSettings()
          .getAccessControlExposedHeaders())
      {
        if(!first)
        {
          exposed.append(", ");
        }
        exposed.append(header);
        first = false;
      }
      context.headers().put(HEADER_ACCESS_CONTROL_EXPOSED_HEADERS,
          exposed.toString());
    }
  }*/

  /**
   * Adds the headers required for CORS support for preflight OPTIONS requests.
   * @see <a href="
   * https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Preflighted_requests">
   * Preflighted requests</a>
   */
  /*private void addPreflightCorsHeaders(PathSegments segments, C context)
  {
    // Applications may configure whitelisted headers which may be sent to
    // the application on cross origin requests.
    if (StringHelper.isNonEmpty(context.headers().get(
        HEADER_ACCESS_CONTROL_REQUEST_HEADERS)))
    {
      final String[] headers = StringHelper.splitAndTrim(
          context.headers().get(
              HEADER_ACCESS_CONTROL_REQUEST_HEADERS), ",");
      boolean first = true;
      final StringBuilder allowed = new StringBuilder();
      for(final String header : headers)
      {
        if(app.getSecurity().getSettings()
            .getAccessControlAllowedHeaders().contains(header.toLowerCase()))
        {
          if(!first)
          {
            allowed.append(", ");
          }
          allowed.append(header);
          first = false;
        }
      }

      context.headers().put(HEADER_ACCESS_CONTROL_ALLOW_HEADERS,
          allowed.toString());
    }

    final String methods = getAccessControlAllowMethods(segments, context);
    if(StringHelper.isNonEmpty(methods))
    {
      context.headers().put(HEADER_ACCESS_CONTROL_ALLOW_METHOD, methods);
    }

    if(((Request)context.getRequest()).getRequestMethod() == HttpMethod.OPTIONS)
    {
      context.headers().put(HEADER_ACCESS_CONTROL_MAX_AGE,
          app.getSecurity().getSettings().getAccessControlMaxAge() + "");
    }
  }*/
}