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

import java.lang.reflect.*;
import java.util.*;

import com.esotericsoftware.reflectasm.*;
import com.techempower.gemini.*;
import com.techempower.gemini.Request.*;
import com.techempower.gemini.path.annotation.*;
import com.techempower.helper.*;
import com.techempower.js.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Building on the BasicPathHandler, the MethodPathHandler provides easy
 * routing of requests to handler methods using the <code>@PathSegment</code> and
 * <code>@PathDefault</code> annotations.
 *   <p>
 * Zero or more methods may be annotated with <code>@PathSegment</code>, <code>@PathDefault</code>,
 * and <code>@PathRoot</code>. Each of these may be additionally annotated with <code>@Get</code>,
 * <code>@Put</code>, <code>@Post</code>, or <code>@Delete</code> to indicate which http request method types
 * the route while handle.
 */
public class MethodSegmentHandler<C extends Context>
     extends BasicPathHandler<C>  
{
  private final Map<String, PathSegmentMethod> getRequestHandleMethods;
  private final Map<String, PathSegmentMethod> putRequestHandleMethods;
  private final Map<String, PathSegmentMethod> postRequestHandleMethods;
  private final Map<String, PathSegmentMethod> deleteRequestHandleMethods;
  protected final MethodAccess                 methodAccess;
  private PathSegmentMethod                    defaultGetMethod;
  private PathSegmentMethod                    defaultPostMethod;
  private PathSegmentMethod                    defaultPutMethod;
  private PathSegmentMethod                    defaultDeleteMethod;
  private Logger                               log = LoggerFactory.getLogger(getClass());
  
  /**
   * Constructor.
   * 
   * @param app The GeminiApplication reference.
   * @param jsw A JavaScriptWriter to use when serializing objects as JSON; if
   *     null, the application's default JavaScriptWriter will be used.
   */
  public MethodSegmentHandler(GeminiApplication app, JavaScriptWriter jsw)
  {
    super(app, jsw);

    getRequestHandleMethods = new HashMap<>();
    putRequestHandleMethods = new HashMap<>();
    postRequestHandleMethods = new HashMap<>();
    deleteRequestHandleMethods = new HashMap<>();
    methodAccess = MethodAccess.get(getClass());
    discoverAnnotatedMethods();
  }
  
  /**
   * Constructor.  Use the application's default JavaScriptWriter for JSON
   * serialization.
   * 
   * @param app The GeminiApplication reference.
   */
  public MethodSegmentHandler(GeminiApplication app)
  {
    this(app, null);
  }
  
  /**
   * Adds the given PathSegmentMethod to the appropriate segment map given
   * the segment string and request method type.
   */
  private void addAnnotatedHandleMethod(String segment, 
      PathSegmentMethod method)
  {
    final String className = getClass().getSimpleName();
    final String duplicateError = className + ": Segment \""
        + segment + "\" is assigned to multiple methods. "
        + "Each segment must map to a single method per HTTP verb.";

    log.trace("{}: {} {} -> #{}", className, method.httpMethod, segment,
        method.name);
    
    switch (method.httpMethod)
    {
      case PUT:
        if (putRequestHandleMethods.containsKey(segment))
        {
          throw new IllegalArgumentException(duplicateError);
        }
        putRequestHandleMethods.put(segment, method);
        break;
      case POST:
        if (postRequestHandleMethods.containsKey(segment))
        {
          throw new IllegalArgumentException(duplicateError);
        }
        postRequestHandleMethods.put(segment, method);
        break;
      case DELETE:
        if (deleteRequestHandleMethods.containsKey(segment))
        {
          throw new IllegalArgumentException(duplicateError);
        }
        deleteRequestHandleMethods.put(segment, method);
        break;
      case GET:
        // fall through
      default:
        if (getRequestHandleMethods.containsKey(segment))
        {
          throw new IllegalArgumentException(duplicateError);
        }
        getRequestHandleMethods.put(segment, method);
        break;
    }
  }
  
  /**
   * Adds the given PathSegmentMethod based on whether or not there are
   * segment arguments present.
   */
  private void addAnnotatedSegment(PathSegment ps, Method method, 
      PathSegmentMethod psm)
  {
    final String[] segments = ps.value();
    if (  (segments.length == 0)
       || (  (segments.length == 1)
          && (StringHelper.isEmpty(segments[0]))
          )
       )
    {
      this.addAnnotatedHandleMethod(method.getName(), psm);
    }
    else
    {
      for (String segment : segments)
      {
        if (StringHelper.isNonEmpty(segment)) 
        {
          this.addAnnotatedHandleMethod(segment, psm);
        }
        else
        {
          // Mapping an empty segment to a psm indicates the root path.
          // There is support for this via @PathRoot and that should be
          // used to support this behavior.
          throw new IllegalArgumentException(
              "Empty path segment not permitted in @PathSegment parameter" + 
              " list. See " + getClass().getName() + "#" + method.getName());
        }
      }
    }
  }
  
  /**
   * Discovers annotated methods at instantiation time.
   * @return The default, if present, PathSegmentMethod
   */
  private void discoverAnnotatedMethods()
  {
    final Method[] methods = getClass().getMethods();

    for (final Method method : methods)
    {
      // Set up references to methods annotated as PathSegments.  A method 
      // named "test" annotated as a PathSegment will be available at /test.
      // If the annotation has a parameter, the String or list of Strings
      // will be used as URI mappings instead of the method name.
      final PathSegment ps = method.getAnnotation(PathSegment.class);
      if (ps != null)
      {
        final Get get = method.getAnnotation(Get.class);
        final Put put = method.getAnnotation(Put.class);
        final Post post = method.getAnnotation(Post.class);
        final Delete delete = method.getAnnotation(Delete.class);
        
        // Those the @Get annotation is implied in the absence of other
        // method type annotations, this is left here to directly analyze
        // the annotated method in case the @Get annotation is updated in
        // the future to have differences between no annotations.
        if (get != null)
        {
          this.addAnnotatedSegment(ps, method,
              analyzeAnnotatedMethod(method, HttpMethod.GET));
        }
        if (put != null)
        {
          this.addAnnotatedSegment(ps, method,
              analyzeAnnotatedMethod(method, HttpMethod.PUT));
        }
        if (post != null)
        {
          this.addAnnotatedSegment(ps, method,
              analyzeAnnotatedMethod(method, HttpMethod.POST));
        }
        if (delete != null)
        {
          this.addAnnotatedSegment(ps, method,
              analyzeAnnotatedMethod(method, HttpMethod.DELETE));
        }
        if (get == null && put == null && post == null && delete == null)
        {
          // If no http request method type annotations are present along
          // side the @PathSegment, then it is an implied GET.
          this.addAnnotatedSegment(ps, method,
              analyzeAnnotatedMethod(method, HttpMethod.GET));
        }
      }
      
      // If a method is annotated as the single and optional PathRoot, it will
      // be called for any request that does not have an additional segment
      // defined (the root for the handler's base URI).
      final PathRoot pr = method.getAnnotation(PathRoot.class);
      if (pr != null)
      {
        final Get get = method.getAnnotation(Get.class);
        final Put put = method.getAnnotation(Put.class);
        final Post post = method.getAnnotation(Post.class);
        final Delete delete = method.getAnnotation(Delete.class);

        if (get != null)
        {
          // Mapping an empty segment to a psm indicates the root path.
          this.addAnnotatedHandleMethod("", 
              analyzeAnnotatedMethod(method, HttpMethod.GET));
        }
        if (put != null)
        {
          this.addAnnotatedHandleMethod("", 
              analyzeAnnotatedMethod(method, HttpMethod.PUT));
        }
        if (post != null)
        {
          this.addAnnotatedHandleMethod("", 
              analyzeAnnotatedMethod(method, HttpMethod.POST));
        }
        if (delete != null)
        {
          this.addAnnotatedHandleMethod("", 
              analyzeAnnotatedMethod(method, HttpMethod.DELETE));
        }
        if (get == null && put == null && post == null && delete == null)
        {
          // If no http request method type annotations are present along
          // side the @PathSegment, then it is an implied GET.
          this.addAnnotatedHandleMethod("", 
              analyzeAnnotatedMethod(method, HttpMethod.GET));
        }
      }

      // If a method is annotated as the single and optional PathDefault, it 
      // will be called for any request that doesn't match one of the other 
      // PathSegments or PathRoot. By design, PathRoot supercedes PathDefault
      // for a given http request method.
      final PathDefault pd = method.getAnnotation(PathDefault.class);
      if (pd != null)
      {
        final Get get = method.getAnnotation(Get.class);
        final Put put = method.getAnnotation(Put.class);
        final Post post = method.getAnnotation(Post.class);
        final Delete delete = method.getAnnotation(Delete.class);

        if (get != null)
        {
          this.defaultGetMethod = analyzeAnnotatedMethod(method, HttpMethod.GET);
        }
        if (put != null)
        {
          this.defaultPutMethod = analyzeAnnotatedMethod(method, HttpMethod.PUT);
        }
        if (post != null)
        {
          this.defaultPostMethod = analyzeAnnotatedMethod(method, HttpMethod.POST);
        }
        if (delete != null)
        {
          this.defaultDeleteMethod = analyzeAnnotatedMethod(method, HttpMethod.DELETE);
        }
        if (get == null && put == null && post == null && delete == null)
        {
          // If no http request method type annotations are present along
          // side the @PathDefault, then it is an implied GET.
          this.defaultGetMethod = analyzeAnnotatedMethod(method, HttpMethod.GET);
        }
      }
    }
  }
  
  /**
   * Analyze an annotated method and return its index if it's suitable for
   * accepting requests.
   * 
   * @param method The annotated handler method.
   * @param httpMethod The http method name (e.g. "GET"). Null
   * implies that all http methods are supported.
   * @return The PathSegmentMethod for the given handler method. 
   */
  protected PathSegmentMethod analyzeAnnotatedMethod(Method method, 
      HttpMethod httpMethod)
  {
    // Only allow accessible (public) methods
    if (Modifier.isPublic(method.getModifiers()))
    {
      return new PathSegmentMethod(method, httpMethod, methodAccess);
    }
    else
    {
      throw new IllegalAccessError("Method " + method.getName() + 
          " must be public to be annotated as a @PathSegment or @PathDefault.");
    }
  }
  
  /**
   * Provides the default behavior of fanning out to methods annotated with
   * the PathSegment annotation.
   */
  @Override
  public boolean handle(PathSegments segments, C context)
  {
    return dispatchToAnnotatedMethod(getAnnotatedMethod(segments, context), 
        context);
  }
  
  /**
   * Determine the annotated method that should process the request.
   */
  protected PathSegmentMethod getAnnotatedMethod(PathSegments segments, 
      C context) 
  {
    if (context.getRequestMethod() == null) {
      return null;
    }
    // If there is no segment or parameters provided, try to route to
    // the @PathRoot for the given http request method type.
    if (StringHelper.isEmpty(segments.getUriBelowOffset()))
    {
      final PathSegmentMethod root;
      switch (context.getRequest().getRequestMethod())
      {
        case PUT:
          root = this.putRequestHandleMethods.get("");
          break;
        case POST:
          root = this.postRequestHandleMethods.get("");
          break;
        case DELETE:
          root = this.deleteRequestHandleMethods.get("");
          break;
        case GET:
          // Fall through
        default:
          root = this.getRequestHandleMethods.get("");
          break;
      }
      if (root != null)
      {
        return root;
      }
    }
    
    // Lookup the method associated with the current path segment and use the
    // default method if there is no association.
    final PathSegmentMethod method;
    switch (context.getRequest().getRequestMethod())
    {
      case PUT:
        method = this.putRequestHandleMethods.get(segments.get(0, ""));
        break;
      case POST:
        method = this.postRequestHandleMethods.get(segments.get(0, ""));
        break;
      case DELETE:
        method = this.deleteRequestHandleMethods.get(segments.get(0, ""));
        break;
      case GET:
        // Fall through
      default:
        method = this.getRequestHandleMethods.get(segments.get(0, ""));
        break;
    }

    if (method != null)
    {
      return method;
    }
    
    // At this point, we have not returned the desired method. This means that
    // we are routed to the correct handler by the dispatch segment, but that
    // there does not exist a @PathSegment for the given path segment portion
    // of the URI. Return default (maybe null).
    switch (context.getRequest().getRequestMethod())
    {
      case PUT:
        return this.defaultPutMethod;
      case POST:
        return this.defaultPostMethod;
      case DELETE:
        return this.defaultDeleteMethod;
      case GET:
        // Fall through
      default:
        return this.defaultGetMethod;
    }
  }
  
  /**
   * Dispatch the request to the appropriately annotated methods in subclasses.
   */
  protected boolean dispatchToAnnotatedMethod(PathSegmentMethod method, 
      C context)
  {
    // If we didn't find an associated method and have no default, we'll 
    // return false, handing the request back to the default handler.
    if (method != null && method.index >= 0)
    {
      // Set the default template to the method's name.  Handler methods can
      // override this default by calling template(name) themselves before 
      // rendering a response.
      defaultTemplate(method.name);

      try
      {
        return (Boolean)methodAccess.invoke(this, method.index,
            this.getVariableArguments(method, context));
      }
      catch (RequestBodyException e)
      {
        log.debug("Got RequestBodyException.", e);
        return this.error(e.getStatusCode(), e.getMessage());
      }
    }

    return false;
  }

  private Object[] getVariableArguments(PathSegmentMethod method, C context)
      throws RequestBodyException
  {
    final boolean contextParameter = method.contextParameter;
    final RequestBodyParameter bodyParameter = method.bodyParameter;

    if (contextParameter || bodyParameter != null)
    {
      final Object[] args = new Object[(contextParameter ? 1 : 0)
              + (bodyParameter != null ? 1 : 0)];

      if (contextParameter)
      {
        // The context parameter is always the first (or only) parameter.
        args[0] = context;
      }

      if (bodyParameter != null)
      {
        // The body parameter is always the last (or only) parameter.
        args[args.length - 1] = bodyParameter.readBody(context);
      }

      return args;
    }

    return ReflectionHelper.NO_VALUES;
  }

  @Override
  protected String getAccessControlAllowMethods(PathSegments segments, C context)
  {
    final StringBuilder reqMethods = new StringBuilder();
    final List<PathSegmentMethod> methods = new ArrayList<>();
    
    if(context.headers().get(Request.HEADER_ACCESS_CONTROL_REQUEST_METHOD) != null)
    {
      PathSegmentMethod put = this.putRequestHandleMethods.get(segments.get(0, ""));
      if (put != null)
      {
        methods.add(put);
      }
      PathSegmentMethod post = this.postRequestHandleMethods.get(segments.get(0, ""));
      if (post != null)
      {
        methods.add(post);
      }
      PathSegmentMethod delete = this.deleteRequestHandleMethods.get(segments.get(0, ""));
      if (delete != null)
      {
        methods.add(delete);
      }
      PathSegmentMethod get = this.getRequestHandleMethods.get(segments.get(0, ""));
      if (get != null)
      {
        methods.add(get);
      }
      
      boolean first = true;
      for(PathSegmentMethod method : methods)
      {
        if(!first)
        {
          reqMethods.append(", ");
        }
        else
        {
          first = false;
        }
        reqMethods.append(method.httpMethod);
      }
    }
    
    return reqMethods.toString();
  }
  
  /**
   * Details of an annotated path segment method.
   */
  protected static class PathSegmentMethod extends BasicPathHandlerMethod
  {
    public final String name;      // For debugging purposes only.
    public final int index;
    public final boolean contextParameter;
    
    public PathSegmentMethod(Method method, HttpMethod httpMethod,
        MethodAccess methodAccess)
    {
      super(method, httpMethod);

      this.name = method.getName();

      final Class<?>[] parameterTypes = method.getParameterTypes();
      this.index = methodAccess.getIndex(method.getName(), parameterTypes);

      if (parameterTypes.length == 0)
      {
        // No arguments, which means no context or body parameter. If this
        // incorrectly has a @Body parameter then the base class will have
        // caught that by now.
        this.contextParameter = false;
      }
      else if (parameterTypes.length == 1)
      {
        // One parameter could be either a context or body parameter.
        // Check the parameter type to find out.
        // TODO should this be something like parameterTypes[0].isAssignableFrom(Context.class)?
        //      If so, make sure to adjust the below checks as well.
        if (parameterTypes[0] == Context.class)
        {
          this.contextParameter = true;
        }
        else if (this.bodyParameter != null)
        {
          this.contextParameter = false;
        }
        else
        {
          throw new IllegalArgumentException("Handler method is configured "
                  + "incorrectly. It accepts 1 parameter but it is not the context, "
                  + "and is not annotated with @Body. See " + getClass().getName() + "#"
                  + method.getName());
        }
      }
      else if (parameterTypes.length == 2)
      {
        if (parameterTypes[0] != Context.class || this.bodyParameter == null)
        {
          throw new IllegalArgumentException("Handler method is configured "
                  + "incorrectly. The first parameter must be the context and "
                  + "the method must be annotated with @Body in order "
                  + "for the second parameter to accept the adapted request body. "
                  + "See " + getClass().getName() + "#" + method.getName());
        }
        else
        {
          this.contextParameter = true;
        }
      }
      else
      {
        throw new IllegalArgumentException("Handler method must accept 2 or fewer "
                + "parameters. See " + getClass().getName() + "#" + method.getName());
      }
    }
    
    @Override
    public String toString()
    {
      return "PSM [" + name + "; " + httpMethod + "; " + index + "]"; 
    }
  }

}
