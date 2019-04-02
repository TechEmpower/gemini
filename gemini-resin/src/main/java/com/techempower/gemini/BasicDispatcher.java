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

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.reflections.*;

import com.techempower.classloader.*;
import com.techempower.gemini.annotation.*;
import com.techempower.gemini.annotation.injector.*;
import com.techempower.gemini.annotation.intercept.*;
import com.techempower.gemini.annotation.response.*;
import com.techempower.gemini.exceptionhandler.*;
import com.techempower.gemini.prehandler.*;
import com.techempower.helper.*;
import com.techempower.log.*;

/**
 * The BasicDispatcher is a standard implementation of the Dispatcher
 * interface that provides basic request-dispatching functionality.  This
 * should be extended by applications to build an application-specific
 * Dispatcher instance.
 *   <p>
 * A single Dispatcher instance is created by the application and then asked
 * to dispatch each HTTP request Context to something that can handle the
 * request, ultimately resulting in a response being provided.
 *   <p>
 * In this standard implementation, the request Context is dispatched to a
 * list of Handlers, which each in turn is given the opportunity to accept and
 * handle the request.
 *   <p>
 * The standard implementation also provides a secondary "dispatching" role--
 * that is, the dispatching of Exceptions to ExceptionHandlers in much the
 * same way that HTTP request Contexts are dispatched to Handlers.  When any
 * application component encounters an Exception, it can elect to send the
 * Exception to ExceptionHandlers via the dispatcher.dispatchException method.
 */
public class BasicDispatcher
  implements Dispatcher
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE         = "disp"; // Four-letter component ID
  public static final int    REDISPATCH_LIMIT       = 5;      // Limit the number of redispatches (for endless-loop suppression).
  public static final int    REDISPATCH_ABORT_LIMIT = 15;     // If we reach 15 redispatches, abort the request entirely.
  public static final String INTERNAL_ERROR_PAGE    = "internal-error-handler";

  //
  // Member variables.
  //

  protected final ComponentLog       log;
  protected final GeminiApplication  application;

  protected final CopyOnWriteArrayList<Handler<? extends BasicDispatcher,? extends Context>>
    dispatchHandlers  = new CopyOnWriteArrayList<>();
  protected final CopyOnWriteArrayList<ExceptionHandler>
    exceptionHandlers = new CopyOnWriteArrayList<>();
  protected final ConcurrentHashMap<String,Handler<? extends BasicDispatcher, ? extends Context>>
    roledHandlers     = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<Prehandler>
    prehandlers       = new CopyOnWriteArrayList<>();
  private CopyOnWriteArrayList<DispatchListener>
    listeners;
  protected Handler<BasicDispatcher, ? extends Context>
    defaultHandler;

  // A simple cache of commands that are registered using the annotations
  protected ConcurrentHashMap<String, Method>
    registeredCommands = new ConcurrentHashMap<>();

  // Annotation object caches for intercepts, injectors and responses
  private final CopyOnWriteArrayList<HandlerIntercept<? extends BasicDispatcher,? extends Context>>
    intercepts        = new CopyOnWriteArrayList<>();
  private final ConcurrentHashMap<String, HandlerIntercept<? extends BasicDispatcher,? extends Context>>
    roledIntercepts   = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<ParameterInjector<? extends BasicDispatcher,? extends Context>>
    injectors                  = new CopyOnWriteArrayList<>();
  private final ConcurrentHashMap<String, ParameterInjector<? extends BasicDispatcher,? extends Context>>
    roledInjectors    = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<HandlerResponse<? extends BasicDispatcher,? extends Context>>
    responses         = new CopyOnWriteArrayList<>();
  private final ConcurrentHashMap<String, HandlerResponse<? extends BasicDispatcher,? extends Context>>
    roledResponses    = new ConcurrentHashMap<>();

  //
  // Member methods.
  //

  /**
   * Constructor.
   *
   * @param application the GeminiApplication reference.
   */
  public BasicDispatcher(GeminiApplication application)
  {
    this.application = application;
    this.log = application.getLog(COMPONENT_CODE);
    installHandlers();
  }

  /**
   * Insert handlers into the dispatchHandlers vector.
   *    <p>
   * In the basic Dispatcher, this method is empty.
   */
  protected void installHandlers()
  {
    // Add the various handlers, IN ORDER, to the dispatch handler vector.
    // Order is important!  Handlers closer to the top of will get an earlier
    // crack at handling requests.

    // ... None are added in the default dispatcher ...
    // Example:
    //    addHandler("login", loginHandler);
    //    addHandler(myOtherHandler);
    //    addHandlers("com.myproject.handlers");

    // Set the default handler.

    // ... By default, a BasicHandler is used.
    // Example:
    //    setDefaultHandler(homeHandler);
  }

  /**
   * Gets the ComponentLog.
   */
  public ComponentLog getLog()
  {
    return this.log;
  }

  /**
   * Adds a DispatchListener.
   */
  public void addListener(DispatchListener listener)
  {
    if (this.listeners == null)
    {
      this.listeners = new CopyOnWriteArrayList<>();
    }
    this.listeners.add(listener);
  }

  public void addIntercept(HandlerIntercept<? extends BasicDispatcher,? extends Context> intercept)
  {
    this.intercepts.add(intercept);
  }

  public void addIntercept(String role, HandlerIntercept<? extends BasicDispatcher,? extends Context> intercept)
  {
    this.roledIntercepts.put(role, intercept);
    addIntercept(intercept);
  }

  public void addInjector(ParameterInjector<? extends BasicDispatcher,? extends Context> injector)
  {
    this.injectors.add(injector);
  }

  public void addInjector(String role, ParameterInjector<? extends BasicDispatcher,? extends Context> injector)
  {
    this.roledInjectors.put(role, injector);
    addInjector(injector);
  }

  public void addResponse(HandlerResponse<? extends BasicDispatcher,? extends Context> response)
  {
    this.responses.add(response);
  }

  public void addResponse(String role, HandlerResponse<? extends BasicDispatcher,? extends Context> response)
  {
    this.roledResponses.put(role, response);
    addResponse(response);
  }

  /**
   * Adds a prehandler to the array of ready ones. They will be executed in order.
   *
   * @param pHandler the Prehandler to be added
   */
  protected void addPrehandler(Prehandler pHandler)
  {
    if (pHandler == null)
    {
      throw new IllegalArgumentException("Prehandler may not be null.");
    }
    this.prehandlers.add(pHandler);
  }

  /**
   * The method that runs all the prehandlers prior to checking handlers.
   */
  protected boolean prehandle(Context context)
  {
    // Tiny optimization to get references to the Prehandlers more quickly.
    final List<Prehandler> prehandlerList = this.prehandlers;
    for (Prehandler p : prehandlerList)
    {
      if (p.prehandle(context))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Sets the default handler.  This is usually set to something like a
   * LoginHandler.
   */
  @SuppressWarnings("unchecked")
  public void setDefaultHandler(Handler<? extends BasicDispatcher,? extends Context> handler)
  {
    this.defaultHandler = (Handler<BasicDispatcher,? extends Context>)handler;
  }

  /**
   * Gets an array of Strings representing the handlers loaded.
   */
  public String[] getHandlerDescriptions()
  {
    String[] descriptions = new String[this.dispatchHandlers.size()];

    Handler<? extends BasicDispatcher,? extends Context> handler;
    for (int i = 0; i < descriptions.length; i++)
    {
      handler = this.dispatchHandlers.get(i);
      if (handler == null)
      {
        this.log.log("Handler " + i + " is null!");
      }
      else
      {
        descriptions[i] = handler.getDescription();
      }
    }

    return descriptions;
  }

  /**
   * Displays the handler descriptions to the console.
   */
  public void displayHandlerDescriptions()
  {
    String[] descriptions = getHandlerDescriptions();
    for (int i = 0; i < descriptions.length; i++)
    {
      this.log.log(i + ": " + descriptions[i]);
    }
  }

  /**
   * Adds an ExceptionHandler.
   */
  public void addExceptionHandler(ExceptionHandler excHandler)
  {
    this.exceptionHandlers.add(excHandler);
  }

  /**
   * This method logs the dispatch to the log.  The default is in the form
   * shown below:
   *   <p>
   * AP 12:01:50 disp: 100.200.150.250; dispatching: home
   *   <p>
   * Applications may overload this method to display more interesting
   * things such as:
   *   <p>
   * AP 12:01:50 disp: 100.200.150.250; (u1:username); dispatching: home
   *   <p>
   * Applications can also overload this method to not output anything at
   * all.
   */
  protected void displayDispatch(Context context, String command)
  {
    this.log.log(context.getClientId() + "; dispatching: " + command);
  }

  /**
   * Sets the thread priority and swallows any Exceptions.
   */
  protected static void setThreadPriority(int priority)
  {
    Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
  }

  /**
   * Gets this Dispatcher's GeminiApplication reference.
   *
   * @return GeminiApplication the reference in use by this Dispatcher.
   */
  public GeminiApplication getApplication()
  {
    return this.application;
  }

  /**
   * Gets the Dispatcher command parameter name.  Normally, this is "cmd",
   * an abbreviation of "command".  However, applications may overload this
   * method to change the name of the URL parameter used to determine the
   * command.  For instance, an application could change the parameter name
   * to "handler", making the following URL possible:
   *    <p>
   *  {@code http://www.somedomain.com/app/?handler=home&screen=4}
   */
  public String getCommandParameterName()
  {
    return GeminiConstants.CMD;
  }

  /**
   * Gets the default command to use when initializing a new Dispatcher.
   *   <p>
   * Overload this method to return a command other than CMD_HOME from
   * GeminiConstants.
   *
   * @return String the default command string.
   */
  public String getDefaultCommand()
  {
    // Overload this method.

    return GeminiConstants.CMD_HOME;
  }

  /**
   * Gets the default handler.
   */
  public Handler<? extends BasicDispatcher,? extends Context> getDefaultHandler()
  {
    return this.defaultHandler;
  }

  /**
   * Returns a reference to the Dispatcher's Vector of Handlers
   */
  public List<Handler<? extends BasicDispatcher,? extends Context>> getDispatchHandlers()
  {
    return this.dispatchHandlers;
  }

  /**
   * Returns a Handler based on its arbitrary Role name.
   *
   * @param role the role name used when adding the Handler to this
   *        Dispatcher.
   *
   * @return Handler The Handler matching the role name, or null if no match.
   */
  public Handler<? extends BasicDispatcher,? extends Context> getRoledHandler(String role)
  {
    return this.roledHandlers.get(role);
  }

  /**
   * Returns the specified handler by its String description.  Generally, this
   * is less reliable than requested a Roled Handler.
   *
   * @see BasicDispatcher#addHandler(String, Handler)
   */
  public Handler<? extends BasicDispatcher,? extends Context> getDispatchHandler(String description)
  {
    Handler<? extends BasicDispatcher,? extends Context> handler;
    for (int i = 0; i < this.dispatchHandlers.size(); i++)
    {
      handler = this.dispatchHandlers.get(i);
      if (handler != null && handler.getDescription().equals(description))
      {
        return handler;
      }
    }
    return null;
  }

  public HandlerIntercept<? extends BasicDispatcher,? extends Context> getIntercept(Class<?> clazz)
  {
    for (HandlerIntercept<? extends BasicDispatcher,? extends Context> intercept : this.intercepts)
    {
      if(intercept.getClass() == clazz)
      {
        return intercept;
      }
    }

    return null;
  }

  public HandlerIntercept<? extends BasicDispatcher,? extends Context> getRoledIntercept(String role)
  {
    return this.roledIntercepts.get(role);
  }

  public ParameterInjector<? extends BasicDispatcher,? extends Context> getInjector(Class<?> clazz)
  {
    for (ParameterInjector<? extends BasicDispatcher,? extends Context> injector : this.injectors)
    {
      if(injector.getClass() == clazz)
      {
        return injector;
      }
    }

    return null;
  }

  public ParameterInjector<? extends BasicDispatcher,? extends Context> getRoledInjector(String role)
  {
    return this.roledInjectors.get(role);
  }

  public HandlerResponse<? extends BasicDispatcher,? extends Context> getResponse(Class<?> clazz)
  {
    for (HandlerResponse<? extends BasicDispatcher,? extends Context> response : this.responses)
    {
      if(response.getClass() == clazz)
      {
        return response;
      }
    }

    return null;
  }

  public HandlerResponse<? extends BasicDispatcher,? extends Context> getRoledResponse(String role)
  {
    return this.roledResponses.get(role);
  }

  /**
   * Gets the command to use when a Dispatcher hits the redispatch limit.
   * Hitting the redispatch limit is not a normal condition and is usually
   * a sign of an error in the JSP code.  However, in order to gracefully
   * trap the condition, a specific command will be Dispatched in the
   * event.
   *   <p>
   * Overload this method to return a command other than
   * CMD_REDISPATCH_LIMIT_HIT from GeminiConstants.
   *
   * @return String the redispatch limit command string.
   */
  public String getRedispatchLimitCommand()
  {
    // Overload this method.

    return GeminiConstants.CMD_REDISPATCH_LIMIT_HIT;
  }

  /**
   * Returns the list of registered commands along with their associated method.
   * This list should be considered incomplete since it only captures commands
   * registered via annotations.
   *
   * @return known commands
   */
  public Map<String, Method> getRegisteredCommands()
  {
    return new HashMap<>(this.registeredCommands);
  }

  /**
   * Gets a String to return to the user in the event of hitting the
   * redispatch "abort" limit (which should only occur if there's a problem
   * with the redispatch limit Handler.
   */
  public String getRedispatchAbortMessage()
  {
    return "<html><head><title>Error</title></head><body><h2>Error</h2>"
     + "<p>Sorry, we cannot process your request at this time.  Please try again in a few moments.</p>"
     + "</body></html>";
  }

  /**
   * Gets the name of an error-handler JSP page.  If no error-handler is
   * provided, the servlet will provide a default error trap that reports
   * the error to the Context.
   *    <p>
   * Note, the exception that is caught will be given to the JSP page
   * as a delivery named "Exception".
   *    <p>
   * Returning null will imply use of the internal default error trap.
   *   <p>
   * Note: This method has no function if an ExceptionHandler is being used.
   * It is only used by the internal exception-handling logic.
   */
  protected String getErrorHandlerPage()
  {
    // Optionally overload this method.

    // Example:
    // return "error-handler.jsp";
    return null;
  }

  /**
   * When the handleException method handles an exception, it may be useful
   * to have a stack trace displayed on the standard-out line for
   * development.  This is useful when an application is not specifying its
   * own error handler JSP page.
   *   <p>
   * Overload this method to return true if a stack trace is desired on
   * standard out in the event of an exception caught by the Servlet.
   *   <p>
   * Note: This method has no function if an ExceptionHandler is being used.
   * It is only used by the internal exception-handling logic.
   */
  protected boolean showExceptionTraces()
  {
    return false;
  }

  /**
   * Dispatches an Exception to the list of ExceptionHandlers.  The dispatch
   * model here is very simple.  Each ExceptionHandler is merely provided
   * a reference to the Context and the Exception.
   *   <p>
   * Since multiple ExceptionHandlers may be capable of providing a response
   * to the request context, it is the responsibility of the application and
   * the ExceptionHandlers that it uses to be configured so that only a single
   * response is provided.
   *
   * @param context The current Context, which may be null if no response is
   *   expected.
   * @param exception The exception encountered.
   * @param description A String describing the current situation; can be
   *   null.
   */
  protected void dispatchExceptionToHandlers(Context context, Throwable exception,
    String description)
  {
    try
    {
      if (  (this.exceptionHandlers != null)
         && (this.exceptionHandlers.size() > 0)
         )
      {
        for (ExceptionHandler handler : this.exceptionHandlers)
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
      else
      {
        this.log.log("Cannot dispatch exception; no ExceptionHandlers have been specified.");
      }
    }
    catch (Exception exc)
    {
      // It's a bummer to have an exception thrown while processing another
      // one.  In that case, let's hope we can log something.
      this.log.log("Exception while processing earlier exception: " + exc);
    }
  }

  /**
   * Called by the InfrastructureServlet to notify the Dispatcher that its
   * work with the provided Context is complete.
   */
  @Override
  public void dispatchComplete(Context context)
  {
    notifyListenersDispatchComplete(context);
  }

  /**
   * Notify the listeners that a dispatch is starting.
   */
  protected void notifyListenersDispatchStarting(Context context, String command)
  {
    final List<DispatchListener> theListeners = this.listeners;
    if (theListeners != null)
    {
      for (DispatchListener theListener : theListeners)
      {
        theListener.dispatchStarting(this, context, command);
      }
    }
  }

  /**
   * Notify the listeners that a re-dispatch is occurring.
   */
  protected void notifyListenersRedispatch(Context context, String previousCommand, String newCommand)
  {
    final List<DispatchListener> theListeners = this.listeners;
    if (theListeners != null)
    {
      for (DispatchListener theListener : theListeners)
      {
        theListener.redispatchOccurring(this, context, previousCommand,
            newCommand);
      }
    }
  }

  /**
   * Notify the listeners that a dispatch is complete.
   */
  protected void notifyListenersDispatchComplete(Context context)
  {
    final List<DispatchListener> theListeners = this.listeners;
    if (theListeners != null)
    {
      for (DispatchListener theListener : theListeners)
      {
        theListener.dispatchComplete(this, context);
      }
    }
  }

  /**
   * Notify the listeners that a rendering has started.
   */
  @Override
  public void renderStarting(Context context, String jspName)
  {
    final List<DispatchListener> theListeners = this.listeners;
    if (this.listeners != null)
    {
      for (DispatchListener theListener : theListeners)
      {
        theListener.renderStarting(this, jspName);
      }
    }
  }

  /**
   * Notify the listeners that a JSP has been completed.
   */
  @Override
  public void renderComplete(Context context)
  {
    final List<DispatchListener> theListeners = this.listeners;
    if (this.listeners != null)
    {
      for (DispatchListener theListener : theListeners)
      {
        theListener.renderComplete(this, context);
      }
    }
  }


  /**
   * Inserts a Handler into the dispatchHandlers vector and the roledHandlers
   * hashtable.  A Roled Handler is a Handler that fulfills a specific role
   * and needs to be retrievable by other Handlers by role name.  For example,
   * a LoginHandler class may be inserted with the role name "login" so that
   * it can be retrieved by other Handlers.
   *
   * @param role Arbitrary role name, such as "login".
   * @param handler the Handler to add to the handlers vector.
   */
  public void addHandler(String role, Handler<? extends Dispatcher,? extends Context> handler)
  {
    addHandler(handler);
    this.roledHandlers.put(role, handler);
    addHandler((Object)handler);
  }

  /**
   * Inserts a Handler into the dispatchHandlers vector.  This method exists
   * so Dispatcher doesn't have to overloaded for each new application.
   *
   * @param handler A Handler to add to the Dispatch Handlers list.
   */
  public void addHandler(Handler<? extends Dispatcher,? extends Context> handler)
  {
    this.dispatchHandlers.add(handler);
    addHandler((Object)handler);
  }

  /**
   * Inserts a Handler that uses annotations into the annotationHandler map.
   *
   * @param handler An object that defines methods using the &#064;CMD annotation
   */
  public void addHandler(Object handler)
  {
    Class<?> c = handler.getClass();
    Method[] methods = c.getMethods();

    // loop through the methods looking for any @CMD annotations
    for (Method method : methods)
    {
      // only allow accessible (public) methods
      if (!Modifier.isPublic(method.getModifiers()))
      {
        this.log.log("Error: Trying to use a non-public method as an annotated handler: " + c.getSimpleName() + "." + method.getName());
        continue;
      }

      AnnotationHandler annotatedHandler = null;

      // search for the CMD annotation for this Method
      CMD cmd = method.getAnnotation(CMD.class);
      if (cmd != null)
      {
        String[] commands = cmd.value();
        // create an annotatedHandler for this method
        // annotationHandlers are wrapper classes for single instances of
        // @CMD annotated methods. They take care of
        // any java reflection calls for the dispatcher
        annotatedHandler = new AnnotationHandler(this.application, this, handler, method, commands);

        this.dispatchHandlers.add(annotatedHandler);

        // Add the commands from this annotation to our local cache, which we use for reporting purposes
        if (commands != null && commands.length > 0)
        {
          for(String command : commands)
          {
            this.registeredCommands.put(command, method);
          }
        }
      }

      // search for the URL annotation for this Method
      URL url = method.getAnnotation(URL.class);
      if (url != null)
      {
        String[] urls = url.value();
        // We need to turn the regular expression representation of this url into a command we can handle
        // in a unique way.
        for(String rule : urls)
        {

          annotatedHandler = new AnnotationHandler(this.application, this, handler, method, new String[]{ rule });

          this.dispatchHandlers.add(annotatedHandler);

          this.registeredCommands.put(rule, method);
        }
      }

      // search for the default annotation to set the default command
      // may not be associated with an actual @CMD annotation
      Default defaultAnnotation = method.getAnnotation(Default.class);
      if (defaultAnnotation != null)
      {
        if (annotatedHandler != null)
        {
          // @Default was attached to a method already annotated with @CMD
          // so we've already set up the annotationHandler
          this.defaultHandler = annotatedHandler;
        }
        else
        {
          this.defaultHandler = new AnnotationHandler(this.application, this, handler, method, null);
        }
      }

      // Search for the @Role annotation that describes a role for this method that
      // can be used to lookup this method directly, must be used with either @CMD or @Default
      Role role = method.getAnnotation(Role.class);
      if (role != null && annotatedHandler != null)
      {
        this.roledHandlers.put(role.value(), annotatedHandler);
      }
    }
  }

  /**
   * Inserts a Handler into the dispatchHandlers vector at the specified index.
   */
  public void addHandler(Handler<? extends Dispatcher,? extends Context> handler, int index)
  {
    this.dispatchHandlers.add(index, handler);
    addHandler((Object)handler);
  }

  /**
   * Searches through the selected package and it's subpackages to look for annotated handlers.
   * While order of the handler addition is not guaranteed, you can assume that they will be
   * ordered by package name, then class name.
   *
   * IMPORTANT: For classes to be added by this method, they must be public and have either
   * a constructor with no arguments, or a constructor with a single argument of type
   * GeminiApplication. Classes cannot be interfaces or abstract.
   *
   * This operation can be time consuming, but will only occur once at startup.
   *
   * @param packageName - full name of package to search, e.g. com.techempower.gemini.handlers.
   */
  public void addHandlers(String packageName)
  {
    try
    {
      Reflections reflection = PackageClassLoader
          .getReflectionClassLoader(packageName);

      reflection.getMethodsAnnotatedWith(CMD.class);

      // get a list of all classes in this package and it's subpackages
      for (Class<?> clazz : reflection.getSubTypesOf(Handler.class))
      {
        // do some preprocessing to shrink the number of lookups.
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())
            || !Modifier.isPublic(clazz.getModifiers()))
        {
          continue;
        }

        // look through the methods of this class and see if any methods contain the
        // @CMD annotation, if not, skip this class
        @SuppressWarnings("unchecked")
        Set<Method> cmdMethods = ReflectionUtils.getAllMethods(clazz, ReflectionUtils.withAnnotation(CMD.class));
        boolean isAnnotated = cmdMethods.size() > 0;

        if (isAnnotated)
        {
          // we've found an annotated class, we need to generate an object, then add it to
          // the list of handlers.
          Object obj = null;
          @SuppressWarnings({ "unchecked", "rawtypes" })
          Set<Constructor> constructors = ReflectionUtils.getAllConstructors(
              clazz, ReflectionUtils.withParametersAssignableTo(GeminiApplication.class),
              ReflectionUtils.withParametersCount(1));
          try
          {
            for(@SuppressWarnings("rawtypes") Constructor constructor : constructors)
            {
              obj = constructor.newInstance(this.application);
            }
          }
          catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e)
          {
            this.log.log("Warning: Class " + clazz.getName() + " could not be instantiated.", e);
          }

          // no constructor with GeminiApplication, try the default constructor
          if (obj == null)
          {
            try
            {
              obj = clazz.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e)
            {
              this.log.log("Warning: Class " + clazz.getName() + " could not be instantiated.", e);
            }
          }

          // we didn't find a valid constructor, log this
          if (obj == null)
          {
            this.log.log("Warning: class " + clazz + " is an annotated handler without a valid constructor, so it has not been added as a handler.");
          }
          else
          {
            // we've got out handler object, now add it to our list
            addHandler(obj);
          }
        }
      }
    }
    catch (ReflectionsException e)
    {
      this.log.log("Warning: exception with reflection in " + packageName + ", aborting addHandlers", e);
    }
  }

  /**
   * Gathers the dispatch command from the Request and sets it within the
   * Context.
   */
  protected String gatherCommand(Context context)
  {
    String toReturn = context.query().get(getCommandParameterName(), getDefaultCommand());
    ((LegacyContext)context).setCommand(toReturn);
    return toReturn;
  }

  /**
   * The dispatching process.  This takes the command from the request
   * and delivers it, along with the Context, to each handler in turn.  If
   * a handler is interested in acting on the request, it does so and
   * returns true.  If either the handler is not interested in the request or
   * fails to handle it, the handler returns false.
   *    <p>
   * If no handlers accept the request, a default handler is forced to
   * handle it.
   *
   * @param rawContext The context of the request as constructed by the
   *        servlet.
   */
  @Override
  @SuppressWarnings("unchecked")
  public boolean dispatch(Context rawContext)
  {
    LegacyContext context = (LegacyContext)rawContext;
    // Only proceed if the context is provided.
    if (context != null)
    {
      // Ask the Context to gather the command if necessary.
      String command = context.getCommand();
      if (command == null)
      {
        command = gatherCommand(context);
      }

      // Notify listeners.
      notifyListenersDispatchStarting(context, command);

      try
      {
        Handler<? super BasicDispatcher,? super Context> handler;

        // Check if a command is provided.
        if (command != null)
        {
          displayDispatch(context, command);

          // If a Prehandler handles the request fully, return true
          // immediately, having not dispatched to the normal Handlers.
          if (this.prehandle(context))
          {
            return true;
          }

          // If there wasn't an annotated handler for this request, or if it
          // returned false, we go through the list of traditional handlers.
          // We copy the dispatchHandlers reference list for a tiny
          // optimization.
          final List<Handler<? extends BasicDispatcher,? extends Context>> handlers = this.dispatchHandlers;
          for (int i = 0; i < handlers.size(); i++)
          {
            handler = (Handler<? super BasicDispatcher,? super Context>)handlers.get(i);

            boolean accept = handler.acceptRequest(this, context, command);

            // If a handler accepts a request, then we're done.  Return.
            if (accept)
            {
              int originalPriority = Handler.PRIORITY_NO_CHANGE;

              if (handler.getPriority() != Handler.PRIORITY_NO_CHANGE)
              {
                originalPriority = Thread.currentThread().getPriority();

                if (originalPriority != handler.getPriority())
                {
                  setThreadPriority(handler.getPriority());
                }
                else
                {
                  originalPriority = Handler.PRIORITY_NO_CHANGE;
                }
              }

              boolean successfullyHandled = false;
              try
              {
                successfullyHandled = handler.handleRequest(this, context, command);
              }
              finally
              {
                if (originalPriority != Handler.PRIORITY_NO_CHANGE)
                {
                  setThreadPriority(originalPriority);
                }
              }

              // If the request was handled, return.
              if (successfullyHandled)
              {
                return true;
              }
            }
          }

          this.log.log("No handler for: " + command);
        }
        else
        {
          // Command was null.
          this.log.log(context.getClientId() + "; dispatching with no command specified.");
        }

        // If nothing handled the request, go to the default handler.
        if (command == null)
        {
          command = getDefaultCommand();
        }

        Handler<? super BasicDispatcher,? super Context> castDefaultHandler = (Handler<? super BasicDispatcher,? super Context>)this.defaultHandler;
        if (castDefaultHandler.handleRequest(this, rawContext, command))
        {
          return true;
        }
        else
        {
          // Default handler returned false on handleRequest?  That really
          // should not happen!
          this.log.log("Default handler's handleRequest method returned false!");
          return false;
        }
      }
      catch (Throwable exc)
      {
        // If any exception comes up while we're doing this back-end
        // processing, direct the context to an error page.
        dispatchException(context, exc, null);

        // Dispatch was "successful" in the sense that it was handled by
        // the ExceptionHandler.
        return true;
      }
    }

    // Null context.
    else
    {
      this.log.log("Error: dispatch() called with a null Context.");
      return false;
    }
  }

  /**
   * Redispatch from this Dispatcher.  Redispatches are useful when a request
   * has been successfully processed by a handler that does not wish to
   * provide a presentation--leaving that to a different handler.  This is
   * often the case when forms are submitted.  A form can be submitted to the
   * same handler, in effect, until the form's contents are correctly
   * validated.  At that time, the handler can process the form contents and
   * then direct the user to a different page.  This is best handled server-
   * side, using a redispatch.
   *    <p>
   * Example use:
   *    <p>
   * <pre><br>
   *   public void handleRequest(...)
   *   {
   *     ...
   *     Form form = buildForm(context);
   *     // Check if the form has been submitted (contents have changed).
   *     if (!form.isUnchanged())
   *     {
   *       // Check if the form's contents are valid.
   *       FormValidation formValidation = form.validate();
   *       if (formValidation.isGood())
   *       {
   *         // Submit to database.
   *         ...
   *         // Redispatch to thank-you page.
   *         dispatcher.redispatch(Constants.CMD_TESTIMONIAL_THANKS);
   *       }
   *     }
   *   }
   * </pre>
   *
   * @param rawContext a reference to the Context
   * @param command the new command to use for redispatching
   */
  public boolean redispatch(Context rawContext, String command)
  {
    LegacyContext context = (LegacyContext)rawContext;

    final String previousCommand = context.getCommand();
    context.incrementDispatches();
    final int redispatchCount = context.getDispatches();

    this.log.log("Redispatching: " + command + " (" + redispatchCount + ")");

    if (redispatchCount <= REDISPATCH_LIMIT)
    {
      context.setCommand(command);
    }
    else if (redispatchCount > REDISPATCH_ABORT_LIMIT)
    {
      try
      {
        this.log.log("Dispatcher redispatched " + redispatchCount + " times.");
        this.log.log("Redispatch abort limit hit!  Sending basic error message to user.");
        context.print(getRedispatchAbortMessage());
      }
      // Catch exceptions and errors.  This method must return true.
      catch (Exception | Error e)
      {
        // Do nothing.
      }
      return true;
    }
    else
    {
      this.log.log("Dispatcher redispatched " + redispatchCount + " times.");
      this.log.log("This is likely the cause of a dispatch loop; redirecting to "
          + getRedispatchLimitCommand() + ".");
      context.setCommand(getRedispatchLimitCommand());
    }

    notifyListenersRedispatch(context, previousCommand, command);

    return dispatch(context);
  }

  /**
   * Dispatches exceptions.  The standard behavior is that the
   * ExceptionHandlers registered with this Dispatcher will be notified of the
   * Exception and asked to handle it.  However, if no ExceptionHandlers have
   * been registered, the getErrorHandlerPage method will be called to
   * determine the name of a JSP to display to users.  If no such error page
   * has been specified either, a default error message will be rendered.
   *
   * @param context The request context may be null; indicating that no
   *   response is required (the exception may still be logged).
   * @param description The description may be null.
   */
  @Override
  public void dispatchException(Context context, Throwable exception,
      String description)
  {
    if (  (exception != null)
       && (this.exceptionHandlers.size() > 0)
       )
    {
      // Use the ExceptionHandler model.
      dispatchExceptionToHandlers(context, exception, description);
    }
    else
    {
      // User the legacy internal exception-handling model.
      dispatchExceptionDefault(context, exception, description);
    }
  }

  /**
   * A legacy built-in means of handling Exceptions that predates the
   * ExceptionHandler model.  Will use a minimalist plain HTML Exception
   * report or a custom JSP (if specified by getErrorHandlerPage).
   *
   * @param rawContext The request context may be null; indicating that no
   *   response is required (the exception may still be logged).
   * @param description The description may be null.
   */
  protected void dispatchExceptionDefault(Context rawContext, Throwable exception,
      String description)
  {
    String localDescription = description;
    // Set a default description value.
    if (localDescription == null)
    {
      localDescription = "No detail available.";
    }

    // Log the exception to the log file for further evaluation.

    this.log.log("InfrastructureServlet caught exception:\n" + exception);

    if (exception != null)
    {
      if (showExceptionTraces())
      {
        //exception.printStackTrace();
        this.log.log(ThrowableHelper.getStackTrace(exception));
      }
  
      if (exception.getCause() != null)
      {
        this.log.log("Cause of the exception: ", exception.getCause());
        if (showExceptionTraces())
        {
          //exception.printStackTrace();
          this.log.log(ThrowableHelper.getStackTrace(exception.getCause()));
        }
      }
    }

    // Send a response if the Context is non-null.
    if (rawContext != null)
    {
      LegacyContext context = (LegacyContext)rawContext;

      String errorHandler = getErrorHandlerPage();

      // See if we caught an exception that was -previously- caught.  In that
      // event, the invoked JSP would be either the errorHandler or the
      // key string "internal-error-handler".  If the exception was previously
      // caught, there's nothing we can do.  We'll just return immediately.
      if (  (!context.getReferencedRender().equals(errorHandler))
          && (!context.getReferencedRender().equals(INTERNAL_ERROR_PAGE))
          )
      {
        // Invoke the error-handler JSP if there is one.  However, we should
        // not invoke the error-handler JSP if it just caused an exception!
        if (errorHandler != null)
        {
          rawContext.delivery().putObject("Exception", exception);
          rawContext.delivery().put("Description", localDescription);
          context.setReferencedRender(errorHandler);
          context.render(errorHandler);
        }
        else
        {
          // An internal error handler page that displays a developer-friendly
          // (but end-user unfriendly) error message.
          context.setReferencedRender(INTERNAL_ERROR_PAGE);
          rawContext.print("<html>");
          rawContext.print("<head><title>Internal error</title>");
          rawContext.print("<style>");
          rawContext.print("body { background-color: white; color: black; }");
          rawContext.print("p { font-family: Arial, Helvetica, Sans-serif; font-size: 12px; }");
          rawContext.print("h2 { font-family: Arial, Helvetica, Sans-serif; font-size: 14px; font-weight: bold; }");
          rawContext.print("pre { font-size: 9px; }");
          rawContext.print("</style>");
          rawContext.print("</head>");
          rawContext.print("<body>");
          rawContext.print("<h2>Internal error</h2>");
          rawContext.print("<p>An exception was caught by the application infrastructure:</p>");
          rawContext.print("<p>" + localDescription + "</p>");
          rawContext.print("<p><pre>");
          if (exception != null)
          {
            context.printException(exception);
          }
          else
          {
            rawContext.print("No exception provided.");
          }
          rawContext.print("");
          rawContext.print("</pre></p>");
          if (  (exception != null)
             && (exception.getCause() != null)
             )
          {
            rawContext.print("<p>Root cause:</p>");
            rawContext.print("<p><pre>");
            context.printException(exception.getCause());
            rawContext.print("");
            rawContext.print("</pre></p>");
          }
          else
          {
            rawContext.print("<p>No root cause provided.</p>");
          }

          rawContext.print("</body>");
          rawContext.print("</html>");
        }
      }
      else
      {
        this.log.log("Exception received from error handler.  Not processing.");
      }
    }
  }


}   // End Dispatcher.
