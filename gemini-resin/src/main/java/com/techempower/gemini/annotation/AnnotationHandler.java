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
package com.techempower.gemini.annotation;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import com.esotericsoftware.reflectasm.*;
import com.techempower.gemini.*;
import com.techempower.gemini.annotation.injector.*;
import com.techempower.gemini.annotation.intercept.*;
import com.techempower.gemini.annotation.response.*;
import com.techempower.log.*;

/**
 * An implementation of Handler the wraps the necessary reflection to convert methods
 * annotated with &#064;CMD into Handlers. Each &#064;CMD annotation is converted into 
 * a single instance of this class. The dispatcher is responsible for finding these 
 * annotations and instantiating AnnotationHandler objects as necessary. Because &#064;CMD
 * may refer to more than 1 command, this class can handle multiple commands as well. 
 * <p>
 * While the Dispatcher is responsible for finding the &#064;CMD and &#064;Default annotations, 
 * this class is responsible for dealing with other annotations that alter the request. These other 
 * annotations include:
 * &#064;Role
 * &#064;Require
 * &#064;Intercept
 * &#064;Injector
 * &#064;Response
 * <p>
 * This class attempts to cache as much as possible to avoid unnecessary reflection
 * calls during runtime. This increases space requirements, but substantially reduces the 
 * overhead of reflection.
 * <p>
 * The current set of annotations take a request through a very specific pipeline. Every request
 * in Gemini has a command associated with it. The &#064;CMD annotation is responsible for telling the 
 * dispatcher that a method is responsible for handling one or more commands. Request without the command 
 * parameter are generally sent to the default handler. This looks like
 * <p><pre>
 *   public class FooHandler
 *   {
 *     &#064;CMD("foo")
 *     public boolean handleFoo(Dispatcher dispatcher, Context context)
 *     {
 *       ...
 *     }
 *     
 *     &#064;Default
 *     public boolean handleDefault(Dispatcher dispatcher, Context context)
 *     {
 *       ...
 *     }
 *   }
 * </pre>
 * <p>
 * Methods annotated with &#064;CMD become handler methods. These methods can always declare their project 
 * specific the Dispatcher and Context in their method signature, but the use of &#064;Injector annotations
 * can be used to inject specific types of parameters into the method. Some examples of &#064;Injector
 * annotations are &#064;Param and &#064;Entity and can be  used like this.
 * <p><pre>
 *  public class FooHandler
 *  {
 *    // This method takes advantage of the &#064;Param annotation and &#064;Entity annotation to inject objects
 *    // into the method based on values from the request.
 *    &#064;CMD("foo")
 *    public boolean handleFoo(Context context, &#064;Param("is-bar") boolean isBar, &#064;Entity("fooid") Foo foo)
 *    {
 *      ..
 *    }
 *  }
 * </pre> 
 * &#064;Param can handle multiple data types.
 * 
 * Handler methods are also responsible for specifying the response. In most cases a method will want to use a JSP
 * for it's final response. This can be achieved through the &#064;JSP annotation. Because this is the most common 
 * response type, by default, if no response type is implicitly stated, the default behavior is to include a jsp
 * called {classname}/{command}.jsp (classname will be stripped of the string 'handler' and both classname and 
 * command will be lowercase). Other response annotations exist as well, and may actually be influenced by the returned
 * value of the method. It's important to note that if your method sets the response on it's own, the response 
 * annotation will be ignored. Below are some examples.
 * <p><pre>
 *  public class FooHandler
 *  {
 *    
 *    // This method doesn't explicitly use a response annotation, but because of the default behavior,
 *    // the jsp at foo/view-foos.jsp will be included
 *    &#064;CMD("view-foos")
 *    public void handleViewFoos(Context context)
 *    {
 *      context.putDelivery("foos", cache.getFoos());
 *    }
 *    
 *    // This method explicitly defines the &#064;JSP annotation to state what the response should be 
 *    // after the method completes
 *    &#064;CMD("add-foo")
 *    &#064;JSP("foo/edit-foo.jsp")
 *    public boolean addFoo(Context context)
 *    {
 *      // Add new foo if form is submitted
 *    }
 *    
 *    // This method will use the default behavior 
 *    &#064;CMD("edit-foo")
 *    public boolean addFoo(Context context)
 *    {
 *      // when there's a successful update, we want to take them to the view-foo command
 *      // this will take precedence of any response annotation OR the default behavior
 *      if(success)
 *      {
 *        return context.sendHTTPRedirectCaught(context.getEscapedServletCmdURL("view-foos"));
 *      }
 *      else
 *      {
 *        // this will trigger the default jsp
 *        return true;
 *      }
 *    }
 *  }
 * </pre> 
 * It's sometimes necessary to intercept commands, this is usually because a request has
 * some requirement the must be met before it can be handled. The most common example of
 * this is secure pages that require a user to be logged in. This is where intercept 
 * commands come in, when specified, a request will always check with the intercept before 
 * continuing on to the handle method. Available intercepts are:
 * <p>
 * &#064;RequireLogin
 * &#064;POST
 * &#064;Maintenance
 * <p>
 * Intercept annotations can be specified at both the class level and the method level, 
 * intercepts at the class level have higher precedence than method level intercepts.
 * <p>
 * There's another intercept annotation that meant to be used with custom intercepts, 
 * &#064;Require. This annotation takes a list of classes of type Intercept.
 * <p>
 * Here are some examples:
 * <pre>
 *  &#064;RequireLogin
 *  public class SecureHandler {}
 *  
 *  public class FooHandler
 *       extends SecureHandler
 *  {
 *    // Because FooHandler implements SecureHandler, ever handler method in this class
 *    // will require login.
 *    &#064;CMD("edit-foo")
 *    public boolean editFoo()
 *    {
 *    
 *    }
 *  }
 *  
 *  public class BarHandler
 *  {
 *    // This handler method requires the request to be a POST
 *    &#064;CMD("edit-bar")
 *    &#064;RequirePOST
 *    public boolean editBar()
 *    {
 *    
 *    }
 *    
 *    // This method uses the &#064;Require annotation to use the custom intercept
 *    &#064;CMD("find-bar")
 *    &#064;Require(MyIntercept.class)
 *    public boolean findBar()
 *    {
 *    
 *    }
 *  }
 * </pre> 
 * The last type of annotation that this class recognizes is the &#064;Role annotation. Roles can be attributed 
 * to handle methods, injectors, intercepts, and responses. Once one of these has a role, they can be retrieved 
 * from the dispatcher based on the role name. This is useful if you need dynamically alter an object at runtime.
 * The maintenance intercept uses this concept, maintenance mode is a flag that can be turned on and off at runtime,
 * so it's necessary for the intercept object to be discoverable so that it can be updated.
 */
public final class AnnotationHandler 
  implements Handler<BasicDispatcher,LegacyContext>
{
  private GeminiApplication                       application;
  private ComponentLog                            log;
  
  // Handler object associated with the handle method
  // this is required to invoke the method
  private Object                                  handleObject;            
  
  // Handle method to invoke
  private Method                                  method;
  
  // ReflectASM method invoker
  private MethodAccess                            access;

  // The index of the method within the class, used when invoking the method through ReflectASM
  private int                                     methodIndex;
  
  // The return type of the method
  private Class<?>                                returnType;
  
  // commands that this handler will accept
  private String[]                                commands;
  
  // Ordered list of intercepts that will be run before method execution
  private List<HandlerIntercept<BasicDispatcher, BasicContext>>
                                                    intercepts           = new ArrayList<>();
  
  // List of annotations associated with intercepts. The annotation at position i in this list
  // directly corresponds to the intercept at position i in intercepts
  private List<Annotation>                        interceptAnnotations = new ArrayList<>();
  
  // Ordered list of parameters based on the method signature.
  private List<Parameter>                         parameters           = new ArrayList<>();
  
  // The response of the request, if no response is specified, @JSP is used as the default
  private HandlerResponse<BasicDispatcher, BasicContext>     responseIntercept;
  private Annotation                              responseAnnotation;
  
  public AnnotationHandler(GeminiApplication application, BasicDispatcher dispatcher, 
      Object handleObject, Method method, String[] commands)
  {
    this.application  = application;
    this.log          = application.getLog("hAnn");
    this.handleObject = handleObject;
    this.method       = method;
    this.commands     = commands;
    configureHandler(dispatcher);
  }
  
  /**
   * Collect all known annotations and cache their behavior
   * so that the call to handleRequest is free of reflection.
   */
  private void configureHandler(BasicDispatcher dispatcher)
  {
    // Set return type of the handler method, this is mostly boolean
    this.returnType = this.method.getReturnType();
    
    // set some reflection variables that we'll use when invoking the method through reflect asm
    this.access      = MethodAccess.get(this.handleObject.getClass());
    this.methodIndex = this.access.getIndex(this.method.getName());
    
    // Go through the argument list for the method and configure the parameters 
    // based on the supplied annotations
    setParameters(dispatcher);
    
    // Gather all intercepts. These can be set at the method level or the class level.
    // Class intercepts have higher priority than method intercepts, the reason for this is that 
    // a common paradigm is to create a Secure class annotated with @RequireLogin, and have 
    // an extending class use @RequireAdmin at the method level, and in this set up, it's 
    // important for @RequireLogin to have precedence.     
    setInterceptAnnotations(dispatcher, this.method.getDeclaringClass().getAnnotations());
    setInterceptAnnotations(dispatcher, this.method.getAnnotations());
    
    // Look for a response annotation. If none is specified, we use the @JSP as the default
    setResponseAnnotation(dispatcher, this.method.getAnnotations());
  }
  
  /**
   * Returns true if the given command was contained in the list of commands from the 
   * original &#064;CMD annotation. No other special checking is done here. Intercepts are
   * invoked in the handleRequest method.
   */
  @Override
  public boolean acceptRequest(BasicDispatcher dispatcher, LegacyContext context, String command) 
  {
    if (this.commands != null && this.commands.length > 0)
    {
      for (String cmd : this.commands)
      {
        if (cmd.equalsIgnoreCase(command))
        {
          return true;
        }
      }
    }
    
    return false;
  }
  
  /**
   * Handles the request pipeline:
   *  1. Allow declared intercepts to intercept the request
   *  2. If the request was not intercepted, invoke the handle method with parameter injection
   *  3. Handle the response if the handler method did not take care of it already.
   * 
   * This method will return true is it handled the request, it should only be the rare case that this method would 
   * return false.
   * 
   * At this point, the only piece of reflection code should be the invoke method on the handler method.
   */
  @Override
  public boolean handleRequest(BasicDispatcher dispatcher, LegacyContext context, String command)
  {
    // current number of dispatches, used to determine if the method
    // calls redispatch.
    int currentDispatches = context.getDispatches();
    
    // check intercepts first to determine if the request should be intercepted
    int counter = 0;
    for (HandlerIntercept<BasicDispatcher, BasicContext> intercept : this.intercepts)
    {
      if (intercept.intercept(this.method, dispatcher, context, command,
          this.interceptAnnotations.get(counter)))
      {
        // this intercept has requested to intercept the request, so we're
        // handing off the request to it
        return intercept.handleIntercept(this.method, dispatcher, context,
            command, this.interceptAnnotations.get(counter));
      }
      counter++;
    }
    
    // The request wasn't intercepted, continuing on with the defined handler method
    Object returnValue;
    try
    {
      returnValue = this.access.invoke(this.handleObject, this.methodIndex, prepareArgs(dispatcher, context));
      //returnValue = this.method.invoke(this.handleObject, prepareArgs(dispatcher, context));
    } 
    catch (IllegalArgumentException e) 
    {
      // The argument list that we provided to the method was wrong in some way,
      // this usually indicates that the prepareArgs method incorrectly returned a null to a
      // primitive type.
      this.log.log("Error: Invoking method (" + this.method.getName() + ") with incorrect arguments.");
      throw new AnnotationException(e);
    }
    catch (Exception e)
    {
      // the handle method has thrown an exception, we'll bubble that up to the dispatcher to handle.
      throw new AnnotationException(e);
    }
    
    // We need to make sure the handler method did not handle the response on it's own before 
    // moving on. If the handler method already took care of the response, then we don't want to
    // send it again, and we'll just return out of this method.
    // A handler method can handle the response by either including a jsp (or sending some other mime type), 
    // sending a redirect or redispatching. 
    if (context.getDispatches() != currentDispatches 
        || context.isCommitted() || this.responseIntercept == null)
    {
      // The handle method already dealt with the response, so we want to return from here.
      // if The return type of the handle method is boolean, we'll return it, otherwise
      // we'll just return true.
      if(this.returnType.equals(boolean.class))
      {
        return (Boolean)returnValue;
      }
      else
      {
        return true;
      }
    }
    else
    {
      // the handler method did not send the response, so we'll send it using the provided response annotation.
      return this.responseIntercept.sendResponse(this.handleObject, this.method, dispatcher, context, command, returnValue, this.responseAnnotation);
    }
  }
  
  /**
   * Prepares the argument array to be included with the method invocation.
   */
  private Object[] prepareArgs(BasicDispatcher dispatcher, BasicContext context)
  {
    // the args array to be returned
    Object[] args = new Object[this.parameters.size()];
    
    // loop through the parameters and request it's object
    int counter = 0;
    for (Parameter param : this.parameters)
    {
      args[counter++] = param.getObject(dispatcher, context);
    }
    
    return args;
  }
  
  /**
   * Responsible for creating an object of Type HandlerIntercept and adding it to the 
   * intercept cache.
   */
  private void addIntercept(BasicDispatcher dispatcher, Class<? extends HandlerIntercept<BasicDispatcher, BasicContext>> clazz)
  {
    addIntercept(dispatcher, clazz, null);
  }
  
  /**
   * Responsible for creating an object of Type HandlerIntercept and adding it to the 
   * intercept cache. Will also add it to the map of intercept to annotation.
   */
  @SuppressWarnings("unchecked")
  private void addIntercept(BasicDispatcher dispatcher, Class<? extends HandlerIntercept<BasicDispatcher, BasicContext>> clazz, Annotation annotation)
  {
    // check to see if the dispatcher already has a cached version of this intercept
    // if so we don't need to instantiate a new one
    HandlerIntercept<BasicDispatcher, BasicContext> intercept = (HandlerIntercept<BasicDispatcher, BasicContext>)dispatcher.getIntercept(clazz);
    if (intercept == null)
    {
      // We allow intercepts to declare a constructor with the single argument of type
      // GeminiApplication (typed accordingly)
      for(Constructor<?> constructor : clazz.getDeclaredConstructors())
      {
        if (constructor.getParameterTypes().length == 1 
            && GeminiApplication.class.isAssignableFrom(constructor.getParameterTypes()[0]))
        {
          try 
          {
            intercept = (HandlerIntercept<BasicDispatcher, BasicContext>)constructor.newInstance(this.application);
            break;
          } 
          catch (IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) 
          {
            this.log.log("Error: Intercept (" + clazz.getName() + ") could not be created with GeminiApplication, using default constructor.");
          } 
        }
      }
      
      // no constructor for GeminiApplication, or we ran into an error trying to use that constructor. Using default
      if (intercept == null)
      {
        try
        {
          intercept = clazz.getConstructor().newInstance();
        }
        catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) 
        {
          this.log.log("Error: Intercept (" + clazz.getName() + ") has not defined a default constructor.");
        } 
        catch (IllegalAccessException e) 
        {
          this.log.log("Error: Intercept (" + clazz.getName() + ") is not a public class.");
        }
      }
      
      // add it to the dispatcher cache
      if (intercept != null)
      {
        // check for the @Role annotation
        if (clazz.isAnnotationPresent(Role.class))
        {
          dispatcher.addIntercept(clazz.getAnnotation(Role.class).value(), intercept);
        }
        else
        {
          // No role
          dispatcher.addIntercept(intercept);
        }
      }
    }
    
    // add it to our lists
    if(intercept != null)
    {
      this.intercepts.add(intercept);
      this.interceptAnnotations.add(annotation);
    }
  }
  
  /**
   * Given an array of annotations, this method will look for any annotations of the type
   * Intercept and add them to the list of intercepts.
   * 
   * Intercepts will be added in the order they were found, so the array of annotations
   * should be ordered the way you want it before handing it off.
   */
  @SuppressWarnings("unchecked")
  private void setInterceptAnnotations(BasicDispatcher dispatcher, Annotation[] annotations)
  {
    for (Annotation annotation : annotations)
    {
      // Check for @Require, this is a special annotation that bundles custom intercept
      // classes together. We need to extract the classes from the annotation and add them
      // to our cache with null annotations.
      if (annotation.annotationType().equals(Require.class))
      {
        for (Class<? extends HandlerIntercept<BasicDispatcher, BasicContext>> intercept :
          (Class<? extends HandlerIntercept<BasicDispatcher, BasicContext>>[])((Require)annotation).value())
        {
          
          addIntercept(dispatcher, intercept);
        }
      }
      
      // check to see if this annotation is of type Intercept
      Intercept interceptAnnotation = annotation.annotationType().getAnnotation(Intercept.class);
      if(interceptAnnotation != null)
      {
        addIntercept(dispatcher, (Class<HandlerIntercept<BasicDispatcher, BasicContext>>)interceptAnnotation.value(), annotation);
      }
    }
  }
  
  /**
   * Creates the parameter list that defines this method.
   */
  @SuppressWarnings("unchecked")
  private void setParameters(BasicDispatcher dispatcher)
  {
    // Array of all the arguments
    Class<?>[] argumentTypes = this.method.getParameterTypes();
    
    // parameter annotation table, each row is an array of annotations for
    // the argument at that position.
    Annotation[][] paramAnnotations = this.method.getParameterAnnotations();
    
    int count = 0;
    for (Class<?> c : argumentTypes)
    {
      // Context and Dispatcher parameters are special types that don't need an 
      // annotation
      if (BasicDispatcher.class.isAssignableFrom(c))//.isAssignableFrom(Dispatcher.class))
      {
        this.parameters.add(new Parameter(BasicDispatcher.class, null, null));
      }
      else if (BasicContext.class.isAssignableFrom(c))//c.isAssignableFrom(Context.class))
      {
        this.parameters.add(new Parameter(BasicContext.class, null, null));
      }
      else
      {
        // search for injector annotations   
        boolean foundAnnotation = false;
        for(Annotation annotation : paramAnnotations[count])
        {
          if (annotation != null && annotation.annotationType().isAnnotationPresent(Injector.class))
          {
            Injector injector = annotation.annotationType().getAnnotation(Injector.class);
            Class<? extends ParameterInjector<BasicDispatcher, BasicContext>> clazz =
              (Class<? extends ParameterInjector<BasicDispatcher, BasicContext>>)injector.value();
            
            // check the dispatcher cache first
            ParameterInjector<BasicDispatcher, BasicContext> paramInjector =
              (ParameterInjector<BasicDispatcher, BasicContext>)dispatcher.getInjector(clazz);
            if (paramInjector == null)
            {
              // We allow injectors to declare a constructor with the single argument of type
              // GeminiApplication (typed accordingly)
              for(Constructor<?> constructor : clazz.getDeclaredConstructors())
              {
                if (constructor.getParameterTypes().length == 1 
                    && GeminiApplication.class.isAssignableFrom(constructor.getParameterTypes()[0]))
                {
                  try 
                  {
                    paramInjector = (ParameterInjector<BasicDispatcher, BasicContext>)constructor.newInstance(this.application);
                    break;
                  } 
                  catch (IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) 
                  {
                    this.log.log("Error: Injector (" + clazz.getName() + ") could not be created with GeminiApplication, using default constructor.");
                  } 
                }
              }
              
              // no constructor for GeminiApplication, using default
              if (paramInjector == null)
              {
                try
                {
                  paramInjector = clazz.getConstructor().newInstance();
                }
                catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) 
                {
                  this.log.log("Error: Injector (" + clazz.getName() + ") has not defined a default constructor.");
                } 
                catch (IllegalAccessException e) 
                {
                  this.log.log("Error: Injector (" + clazz.getName() + ") is not a public class.");
                }
              }
              
              // add to the dispatcher cache
              if (paramInjector != null)
              {
                // check for the @Role annotation
                if(paramInjector.getClass().isAnnotationPresent(Role.class))
                {
                  dispatcher.addInjector(clazz.getAnnotation(Role.class).value(), paramInjector);
                }
                else
                {
                  // No role
                  dispatcher.addInjector(paramInjector);
                }
              }
            }
          
            // add it to our list and map
            if (paramInjector != null)
            {
              this.parameters.add(new Parameter(c, paramInjector, annotation));
              foundAnnotation = true;
              break;
            }
          }
        }
        
        // user hasn't specified an annotation for this parameter
        if (!foundAnnotation)
        {
          // there's no injector annotation for this method parameter, we can't omit the argument, so
          // we'll include a param object that will return null for this object
          this.parameters.add(new Parameter(c, null, null));
        }
      }
      count++;
    }
  }
  
  /**
   * Given an array of annotations, this method will look for any annotations of the type
   * Response. Only the first instance will be set as the response. If none is provide
   * we use the default &#064;JSP annotation
   */
  @SuppressWarnings("unchecked")
  private void setResponseAnnotation(BasicDispatcher dispatcher, Annotation[] annotations)
  {
    for (Annotation annotation : annotations)
    {
      // check to see if this annotation is of type GeminiResponse
      Response theResponseAnnotation = annotation.annotationType().getAnnotation(Response.class);
      if (theResponseAnnotation != null)
      {
        Class<? extends HandlerResponse<BasicDispatcher, BasicContext>> clazz =
          (Class<? extends HandlerResponse<BasicDispatcher, BasicContext>>)theResponseAnnotation.value();
        HandlerResponse<BasicDispatcher, BasicContext> response =
          (HandlerResponse<BasicDispatcher, BasicContext>)dispatcher.getResponse(clazz);
        
        // this response object isn't in the dispatcher cache yet, create it
        if (response == null)
        {
          // We allow responses to declare a constructor with the single argument of type
          // GeminiApplication (typed accordingly)
          for (Constructor<?> constructor : clazz.getDeclaredConstructors())
          {
            if (constructor.getParameterTypes().length == 1 
                && GeminiApplication.class.isAssignableFrom(constructor.getParameterTypes()[0]))
            {
              try 
              {
                response = (HandlerResponse<BasicDispatcher, BasicContext>)constructor.newInstance(this.application);
                break;
              } 
              catch (IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) 
              {
                this.log.log("Error: Response (" + clazz.getName() + ") could not be created with GeminiApplication, using default constructor.");
              } 
            }
          }
        
          // no constructor for GeminiApplication, using default
          if (response == null)
          {
            try
            {
              response = clazz.getConstructor().newInstance();
            }
            catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) 
            {
              this.log.log("Error: Response (" + clazz.getName() + ") has not defined a default constructor.");
            } 
            catch (IllegalAccessException e) 
            {
              this.log.log("Error: Response (" + clazz.getName() + ") is not a public class.");
            }
          }
          
          // add it the dispatcher cache
          if(response != null)
          {
            // check for the @Role annotation
            if (clazz.isAnnotationPresent(Role.class))
            {
              dispatcher.addResponse(clazz.getAnnotation(Role.class).value(), response);
            }
            else
            {
              // No role
              dispatcher.addResponse(response);
            }
          }
        }
      
        // add it to our list and map
        if (response != null)
        {
          this.responseIntercept = response;
          this.responseAnnotation = annotation;
        }
      }
    }
    
    // didn't find a response annotation, adding default jsp response
    // pf - for now, we're disabling default JSP inclusion because we can't reliably tell if
    // the response has already been started.
    /*
    if (this.responseIntercept == null)
    {
      HandlerResponse<BasicDispatcher,Context> response = 
        (HandlerResponse<BasicDispatcher,Context>)dispatcher.getResponse(JspResponse.class);
      if(response == null)
      {
        // create an instance of the response
        response = new JspResponse<BasicDispatcher,Context>();
        dispatcher.addResponse(response);
      }
      
      // add it to our list and map, there's no annotation associated with this
      // response, so we set it to null
      this.responseIntercept = response;
      this.responseAnnotation = null;
    }
    */
  }
  
  //
  // Unnecessary Handler methods
  //
  @Override
  public String getDescription() 
  {
    return null;
  }

  @Override
  public int getPriority() 
  {
    return 0;
  }
  
  /**
   * Class for defining a single parameter from the method signature. Annotated handler methods can use injector annotations
   * to specify how the object for this parameter should be constructed. This class facilitates the retrieval of the object
   * that will be passed along with the method invocation.
   */
  private class Parameter
  {
    private final Class<?>                              parameterType;
    private final Annotation                            injectorAnnotation;
    private final ParameterInjector<BasicDispatcher, BasicContext> injector;
    
    public Parameter(Class<?> parameterType, ParameterInjector<BasicDispatcher, BasicContext> injector, Annotation injectorAnnotation)
    {
      this.parameterType         = parameterType;
      this.injectorAnnotation    = injectorAnnotation;
      this.injector              = injector;
    }
    
    /**
     * Return a valid object for this parameter using the associated injector and annotation
     */
    public Object getObject(BasicDispatcher dispatcher, BasicContext context)
    {
      // Dispatcher and Context are special cases that can be returned without
      // requiring a separate annotation
      if (BasicDispatcher.class.isAssignableFrom(this.parameterType))
      {
        return dispatcher;
      }
      else if (BasicContext.class.isAssignableFrom(this.parameterType))
      {
        return context;
      }
      else
      {
        // If the injector isn't null, use it to get the object
        if (this.injectorAnnotation != null && this.injector != null)
        {
          Object toReturn = this.injector.getArgument(dispatcher, context,
              this.injectorAnnotation, this.parameterType);
          
          if (toReturn == null)
          {
            // if a method parameter is a primitive type, null cannot be used as it's value
            // it must be a value of that type of primitive.
            if (this.parameterType.isPrimitive())
            {
              return getDefaultPrimitiveValue(this.parameterType);
            }
          }
          
          return toReturn;
        }
        else
        {
          // if a method parameter is a primitive type, null cannot be used as it's value
          // it must be a value of that type of primitive.
          if (this.parameterType.isPrimitive())
          {
            return getDefaultPrimitiveValue(this.parameterType);
          }
          
          return null;
        }
      }
    }
    
    /**
     * Returns a primitive object for the specified class.
     */
    private Object getDefaultPrimitiveValue(Class<?> c)
    {
      // INT
      if (c.equals(int.class))
      {
        return 0;
      }

      // LONG
      if (c.equals(long.class))
      {
        return 0L;
      }

      // SHORT
      if (c.equals(short.class))
      {
        return 0;
      }

      // BYTE
      if (c.equals(byte.class))
      {
        return 0;
      }

      // FLOAT
      if (c.equals(float.class))
      {
        return 0.0f;
      }

      // DOUBLE
      if (c.equals(double.class))
      {
        return 0.0d;
      }

      // CHAR
      if (c.equals(char.class))
      {
        return '\u0000';
      }

      // BOOLEAN
      if (c.equals(boolean.class))
      {
        return false;
      }

      // Doesn't match a primitive, so it's ok to return null
      return null;
    }
  }
  
  /**
   * Special unchecked exception that allows exceptions occurring within the execution of the 
   * handler method to be bubbled up to the dispatcher, where it can be correctly handled.
   */
  private class AnnotationException
    extends RuntimeException
  {
    /**
     * Auto-generated serialVersionUID
     */
    private static final long serialVersionUID = 5908268019924291717L;

    /**
     * Creates an unchecked exception based on the provided cause. All methods redirect to the 
     * causes methods.
     * 
     * @param cause - The original exception that was thrown
     */
    public AnnotationException(Throwable cause)
    {
      super(cause);
    }
    
    /**
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() 
    {
      if (getCause() == null)
      {
        return super.getMessage();
      }
      
      return getCause().getMessage();
    }

    /**
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() 
    {
      if (getCause() == null)
      {
        return super.getLocalizedMessage();
      }
      
      return getCause().getLocalizedMessage();
    }

    /**
     * @see java.lang.Throwable#printStackTrace()
     */
    @Override
    public void printStackTrace() 
    {
      if (getCause() == null)
      {
        super.printStackTrace();
      }
      
      getCause().printStackTrace();
    }

    /**
     * @see java.lang.Throwable#printStackTrace(PrintStream)
     */
    @Override
    public void printStackTrace(PrintStream s) 
    {
      if (getCause() == null)
      {
        super.printStackTrace(s);
      }
      
      getCause().printStackTrace(s);
    }

    /**
     * @see java.lang.Throwable#printStackTrace(PrintWriter)
     */
    @Override
    public void printStackTrace(PrintWriter s) 
    {
      if (getCause() == null)
      {
        super.printStackTrace(s);
      }
      
      getCause().printStackTrace(s);
    }

    /**
     * @see java.lang.Throwable#getStackTrace()
     */
    @Override
    public StackTraceElement[] getStackTrace() 
    {
      if (getCause() == null)
      {
        return super.getStackTrace();
      }
      
      return getCause().getStackTrace();
    }
    
    /**
     * Uses the toString method of the causing exception.
     */
    @Override
    public String toString()
    {
      if (getCause() == null)
      {
        return super.toString();
      }
      
      return getCause().toString();
    }
  }
}
