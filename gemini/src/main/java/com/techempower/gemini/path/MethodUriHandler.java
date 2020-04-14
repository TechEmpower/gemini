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
import com.techempower.gemini.path.MethodUriHandler.PathUriMethod.*;
import com.techempower.gemini.path.annotation.*;
import com.techempower.helper.*;
import com.techempower.js.*;
import com.techempower.log.LogLevel;

import static com.techempower.gemini.HttpRequest.HEADER_ACCESS_CONTROL_REQUEST_METHOD;
import static com.techempower.gemini.HttpRequest.HttpMethod.*;

/**
 * Building on the BasicPathHandler, the MethodUriHandler provides easy
 * routing of requests to handler methods using the @Path annotation.
 */
public class MethodUriHandler<C extends Context>
     extends BasicPathHandler<C>  
{
  private final PathUriTree    getRequestHandleMethods;
  private final PathUriTree    putRequestHandleMethods;
  private final PathUriTree    postRequestHandleMethods;
  private final PathUriTree    deleteRequestHandleMethods;
  protected final MethodAccess methodAccess; 
  
  /**
   * Constructor.
   *
   * @param app The GeminiApplication reference.
   * @param componentCode a four-letter code for this handler's ComponentLog.
   * @param jsw A JavaScriptWriter to use when serializing objects as JSON; if
   *     null, the application's default JavaScriptWriter will be used.
   */
  public MethodUriHandler(GeminiApplication app, String componentCode, 
      JavaScriptWriter jsw)
  {
    super(app, componentCode, jsw);

    getRequestHandleMethods = new PathUriTree();
    putRequestHandleMethods = new PathUriTree();
    postRequestHandleMethods = new PathUriTree();
    deleteRequestHandleMethods = new PathUriTree();
    
    methodAccess = MethodAccess.get(getClass());    
    discoverAnnotatedMethods();
  }
  
  /**
   * Constructor.  Use the application's default JavaScriptWriter for JSON
   * serialization.
   * 
   * @param app The GeminiApplication reference.
   * @param componentCode a four-letter code for this handler's ComponentLog.
   */
  public MethodUriHandler(GeminiApplication app, String componentCode)
  {
    this(app, componentCode, null);
  }
  
  /**
   * Constructor.  Use the application's default JavaScriptWriter for JSON
   * serialization and use a default component code of "urhd".
   * 
   * @param app The GeminiApplication reference.
   */
  public MethodUriHandler(GeminiApplication app)
  {
    this(app, "urhd", null);
  }
  
  /**
   * Adds the given PathUriMethod to the appropriate list given
   * the request method type.
   */
  private void addAnnotatedHandleMethod(PathUriMethod method)
  {
    switch (method.httpMethod)
    {
      case PUT:
        this.putRequestHandleMethods.addMethod(method);
        break;
      case POST:
        this.postRequestHandleMethods.addMethod(method);
        break;
      case DELETE:
        this.deleteRequestHandleMethods.addMethod(method);
        break;
      case GET:
        this.getRequestHandleMethods.addMethod(method);
        break;
      default:
        break;
    }
  }
  
  /**
   * Discovers annotated methods at instantiation time.
   */
  private void discoverAnnotatedMethods()
  {
    final Method[] methods = getClass().getMethods();

    for (Method method : methods)
    {      
      // Set up references to methods annotated as Paths.
      final Path path = method.getAnnotation(Path.class);
      if (path != null)
      {        
        final Get get = method.getAnnotation(Get.class);
        final Put put = method.getAnnotation(Put.class);
        final Post post = method.getAnnotation(Post.class);
        final Delete delete = method.getAnnotation(Delete.class);
        // Enforce that only one http method type is on this segment.
        if ((get != null ? 1 : 0) + (put != null ? 1 : 0) + 
            (post != null ? 1 : 0) + (delete != null ? 1 : 0) > 1)
        {
          throw new IllegalArgumentException(
              "Only one request method type is allowed per @PathSegment. See " 
              + getClass().getName() + "#" + method.getName());
        }
        final PathUriMethod psm;
        // Those the @Get annotation is implied in the absence of other
        // method type annotations, this is left here to directly analyze
        // the annotated method in case the @Get annotation is updated in
        // the future to have differences between no annotations.
        if (get != null)
        {
          psm = analyzeAnnotatedMethod(path, method, GET);
        }
        else if (put != null)
        {
          psm = analyzeAnnotatedMethod(path, method, PUT);
        }
        else if (post != null)
        {
          psm = analyzeAnnotatedMethod(path, method, POST);
        }
        else if (delete != null)
        {
          psm = analyzeAnnotatedMethod(path, method, DELETE);
        }
        else
        {
          // If no http request method type annotations are present along
          // side the @PathSegment, then it is an implied GET.
          psm = analyzeAnnotatedMethod(path, method, GET);
        }
        
        this.addAnnotatedHandleMethod(psm);
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
  protected PathUriMethod analyzeAnnotatedMethod(Path path, Method method, 
      HttpRequest.HttpMethod httpMethod)
  {
    // Only allow accessible (public) methods
    if (Modifier.isPublic(method.getModifiers()))
    {
      return new PathUriMethod(
          method,
          path.value(),
          httpMethod,
          methodAccess);
    }
    else
    {
      throw new IllegalAccessError("Methods annotated with @Path must be " + 
         "public. See" + getClass().getName() + "#" + method.getName());
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
  protected PathUriMethod getAnnotatedMethod(PathSegments segments, 
      C context) 
  { 
    final PathUriTree tree;
    switch (((HttpRequest)context.getRequest()).getRequestMethod())
    {
      case PUT:
        tree = this.putRequestHandleMethods;
        break;
      case POST:
        tree = this.postRequestHandleMethods;
        break;
      case DELETE:
        tree = this.deleteRequestHandleMethods;
        break;
      case GET:
        tree = this.getRequestHandleMethods;
        break;
      default:
        // We do not want to handle this
        return null;
    }
    
    return tree.search(segments);
  }

  @Override
  protected String getAccessControlAllowMethods(PathSegments segments, C context)
  {
    final StringBuilder reqMethods = new StringBuilder();
    final List<PathUriMethod> methods = new ArrayList<>();
    
    if(context.headers().get(HEADER_ACCESS_CONTROL_REQUEST_METHOD) != null)
    {
      final PathUriMethod put = this.putRequestHandleMethods.search(segments);
      if (put != null)
      {
        methods.add(put);
      }
      final PathUriMethod post = this.postRequestHandleMethods.search(segments);
      if (post != null)
      {
        methods.add(this.postRequestHandleMethods.search(segments));
      }
      final PathUriMethod delete = this.deleteRequestHandleMethods.search(segments);
      if (delete != null)
      {
        methods.add(this.deleteRequestHandleMethods.search(segments));
      }
      final PathUriMethod get = this.getRequestHandleMethods.search(segments);
      if (get != null)
      {
        methods.add(this.getRequestHandleMethods.search(segments));
      }
      
      boolean first = true;
      for(PathUriMethod method : methods)
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
   * Dispatch the request to the appropriately annotated methods in subclasses.
   */
  protected boolean dispatchToAnnotatedMethod(PathUriMethod method, 
      C context)
  {
    // If we didn't find an associated method and have no default, we'll 
    // return false, handing the request back to the default handler.
    if (method != null && method.index >= 0)
    {
      // Set the default template to the method's name.  Handler methods can
      // override this default by calling template(name) themselves before 
      // rendering a response.
      defaultTemplate(method.method.getName());
      
      if (method.method.getParameterTypes().length == 0)
      {
        return (Boolean)methodAccess.invoke(this, method.index, 
            ReflectionHelper.NO_VALUES);
      }
      else
      {
        // We have already enforced that the @Path annotations have the correct
        // number of args in their declarations to match the variable count
        // in the respective URI. So, create an array of values and try to set
        // them via retrieving them as segments.
        try
        {
          return (Boolean)methodAccess.invoke(this, method.index,
                  this.getVariableArguments(method, context));
        }
        catch (RequestBodyException e)
        {
          log().log("Got RequestBodyException.", LogLevel.DEBUG, e);
          return this.error(e.getStatusCode(), e.getMessage());
        }
      }
    }

    return false;
  }

  /**
   * Private helper method for capturing the values of the variable annotated
   * methods and returning them as an argument array (in order or appearance).
   *   <p>
   * Example: @Path("foo/{var1}/{var2}") 
   * public boolean handleFoo(int var1, String var2)
   * 
   * The array returned for `GET /foo/123/asd` would be: [123, "asd"]
   * @param method the annotated method.
   * @return Array of corresponding values.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object[] getVariableArguments(PathUriMethod method, C context) throws RequestBodyException
  {
    final Object[] args = new Object[method.method.getParameterTypes().length];
    int argsIndex = 0;
    for (int i = 0; i < method.segments.length; i++)
    {
      if (method.segments[i].isVariable)
      {
        if (argsIndex >= args.length)
        {
          // No reason to continue - we found all are variables.
          break;
        }
        // Try to read it from the context.
        if(method.segments[i].type.isPrimitive())
        {
          // int
          if (method.segments[i].type.isAssignableFrom(int.class))
          {
            args[argsIndex] = segments().getInt(i);
          }
          // long
          else if (method.segments[i].type.isAssignableFrom(long.class))
          {
            args[argsIndex] = NumberHelper.parseLong(segments().get(i));
          }
          // boolean
          else if (method.segments[i].type.isAssignableFrom(boolean.class))
          {
            // bool variables are NOT simply whether they are present.
            // Rather, it should be a truthy value.
            args[argsIndex] = StringHelper.equalsIgnoreCase(
              segments().get(i),
              new String[]{
                "true", "yes", "1"
            });
          }
          // float
          else if (method.segments[i].type.isAssignableFrom(float.class))
          {
            args[argsIndex] = NumberHelper.parseFloat(segments().get(i), 0f);
          }
          // double
          else if (method.segments[i].type.isAssignableFrom(double.class))
          {
            args[argsIndex] = NumberHelper.parseDouble(segments().get(i), 0f);
          }
          // default
          else
          {
            // We MUST have something here, set the default to zero.
            // This is undefined behavior. If the method calls for a
            // char/byte/etc and we pass 0, it is probably unexpected.
            args[argsIndex] = 0;
          }
        }
        // String, and technically Object too.
        else if (method.segments[i].type.isAssignableFrom(String.class))
        {
          args[argsIndex] = segments().get(i);
        }
        else
        {
          int indexOfMethodToInvoke;
          Class<?> type = method.segments[i].type;
          MethodAccess methodAccess = method.segments[i].methodAccess;
          if (hasStringInputMethod(type, methodAccess, "fromString"))
          {
            indexOfMethodToInvoke = methodAccess
                .getIndex("fromString", String.class);
          }
          else if (hasStringInputMethod(type, methodAccess, "valueOf"))
          {
            indexOfMethodToInvoke = methodAccess
                .getIndex("valueOf", String.class);
          }
          else
          {
            indexOfMethodToInvoke = -1;
          }
          if (indexOfMethodToInvoke >= 0)
          {
            try
            {
              args[argsIndex] = methodAccess.invoke(null,
                  indexOfMethodToInvoke, segments().get(i));
            }
            catch (IllegalArgumentException iae)
            {
              // In the case where the developer has specified that only
              // enumerated values should be accepted as input, either
              // one of those values needs to exist in the URI, or this
              // IllegalArgumentException will be thrown. We will limp
              // on and pass a null in this case.
              args[argsIndex] = null;
            }
          }
          else
          {
            // We don't know the type, so we cannot create it.
            args[argsIndex] = null;
          }
        }
        // Bump argsIndex
        argsIndex ++;
      }
    }

    // Handle adapting and injecting the request body if configured.
    final RequestBodyParameter bodyParameter = method.bodyParameter;
    if (bodyParameter != null && argsIndex < args.length)
    {
      args[argsIndex] = bodyParameter.readBody(context);
    }

    return args;
  }

  private static boolean hasStringInputMethod(Class<?> type,
                                              MethodAccess methodAccess,
                                              String methodName) {
    String[] methodNames = methodAccess.getMethodNames();
    Class<?>[][] parameterTypes = methodAccess.getParameterTypes();
    for (int index = 0; index < methodNames.length; index++)
    {
      String foundMethodName = methodNames[index];
      Class<?>[] params = parameterTypes[index];
      if (foundMethodName.equals(methodName)
          && params.length == 1
          && params[0].equals(String.class))
      {
        try
        {
          // Only bother with the slowness of normal reflection if
          // the method passes all the other checks.
          Method method = type.getMethod(methodName, String.class);
          if (Modifier.isStatic(method.getModifiers()))
          {
            return true;
          }
        }
        catch (NoSuchMethodException e)
        {
          // Should not happen
        }
      }
    }
    return false;
  }
  
  protected static class PathUriTree
  {
    private final Node root;
    
    public PathUriTree()
    {
      root = new Node(null);
    }

    /**
     * Searches the tree for a node that best handles the given segments.
     */
    public final PathUriMethod search(PathSegments segments)
    {
      return search(this.root, segments, 0);
    }

    /**
     * Searches the given segments at the given offset with the given node
     * in the tree. If this node is a leaf node and matches the segment
     * stack perfectly, it is returned. If this node is a leaf node and
     * either a variable or a wildcard node and the segment stack has run
     * out of segments to check, return that if we have not found a true
     * match.
     */
    private PathUriMethod search(Node node, PathSegments segments, int offset)
    {
      if (node != this.root && 
          offset >= segments.getCount())
      {
        // Last possible depth; must be a leaf node
        if (node.method != null)
        {
          return node.method;
        }
        return null;
      }
      else
      {
        // Not yet at a leaf node
        PathUriMethod bestVariable = null; // Best at this depth
        PathUriMethod bestWildcard = null; // Best at this depth
        PathUriMethod toReturn     = null;
        for (Node child : node.children)
        {
          // Only walk the path that can handle the new segment.
          if (child.segment.segment.equals(segments.get(offset,"")))
          {
            // Direct hits only happen here.
            toReturn = search(child, segments, offset + 1);
          }
          else if (child.segment.isVariable)
          {
            // Variables are not necessarily leaf nodes.
            PathUriMethod temp = search(child, segments, offset + 1);
            // We may be at a variable node, but not the variable
            // path segment handler method. Don't set it in this case.
            if (temp != null)
            {
              bestVariable = temp;
            }
          }
          else if (child.segment.isWildcard)
          {
            // Wildcards are leaf nodes by design.
            bestWildcard = child.method;
          }
        }
        // By here, we are as deep as we can be.
        if (toReturn == null && bestVariable != null)
        {
          // Could not find a direct route
          toReturn = bestVariable;
        }
        else if (toReturn == null && bestWildcard != null)
        {
          toReturn = bestWildcard;
        }
        return toReturn;
      }
    }
    
    /**
     * Adds the given PathUriMethod to this tree at the
     * appropriate depth.
     */
    public final void addMethod(PathUriMethod method)
    {
      this.root.addChild(this.root, method, 0);
    }
    
    /**
     * A node in the tree of PathUriMethod.
     */
    public static class Node
    {
      private PathUriMethod method;
      private final UriSegment segment;
      private final List<Node> children;
      
      public Node(UriSegment segment)
      {
        this.segment = segment;
        this.children = new ArrayList<>();
      }
      
      @Override
      public String toString()
      {
        final StringBuilder sb = new StringBuilder()
          .append("{")
          .append("method: ")
          .append(method)
          .append(", segment: ")
          .append(segment)
          .append(", childrenCount: ")
          .append(this.children.size())
          .append("}");

        return sb.toString();
      }
      
      /**
       * Returns the immediate child node for the given segment and creates
       * if it does not exist.
       */
      private Node getChildForSegment(Node node, UriSegment[] segments, int offset)
      {
        Node toRet = null;
        for(Node child : node.children)
        {
          if (child.segment.segment.equals(segments[offset].segment))
          {
            toRet = child;
            break;
          }
        }
        if (toRet == null)
        {
          // Add a new node at this segment to return.
          toRet = new Node(segments[offset]);
          node.children.add(toRet);
        }
        return toRet;
      }
      
      /**
       * Recursively adds the given PathUriMethod to this tree at the 
       * appropriate depth.
       */
      private void addChild(Node node, PathUriMethod uriMethod, int offset)
      {
        if (uriMethod.segments.length > offset)
        {
          final Node child = this.getChildForSegment(node, uriMethod.segments, offset);
          if (uriMethod.segments.length == offset + 1)
          {
            child.method = uriMethod;
          }
          else
          {
            this.addChild(child, uriMethod, offset + 1);
          }
        }
      }
      
      /**
       * Returns the PathUriMethod for this node.
       * May be null.
       */
      public final PathUriMethod getMethod()
      {
        return this.method;
      }
    }
  }
  
  /**
   * Details of an annotated path segment method.
   */
  protected static class PathUriMethod extends BasicPathHandlerMethod
  {
    public final Method method;
    public final String uri;
    public final UriSegment[] segments;
    public final int index;
    
    public PathUriMethod(Method method, String uri, HttpRequest.HttpMethod httpMethod,
        MethodAccess methodAccess)
    {
      super(method, httpMethod);

      this.method = method;
      this.uri = uri;
      this.segments = this.parseSegments(this.uri);
      int variableCount = 0;
      final Class<?>[] classes = 
          new Class[method.getGenericParameterTypes().length];
      for (UriSegment segment : segments)
      {
        if (segment.isVariable)
        {
          classes[variableCount] = 
              (Class<?>)method.getGenericParameterTypes()[variableCount];
          segment.type = classes[variableCount];
          if (!segment.type.isPrimitive())
          {
            segment.methodAccess = MethodAccess.get(segment.type);
          }
          // Bump variableCount
          variableCount ++;
        }
      }

      // Check for and configure the method to receive a parameter for the
      // request body. If desired, it's expected that the body parameter is
      // the last one. So it's only worth checking if variableCount indicates
      // that there's room left in the classes array. If there is a mismatch
      // where there is another parameter and no @Body annotation, or there is
      // a @Body annotation and no extra parameter for it, the below checks
      // will find that and throw accordingly.
      if (variableCount < classes.length && this.bodyParameter != null)
      {
        classes[variableCount] = method.getParameterTypes()[variableCount];
        variableCount++;
      }

      if (variableCount == 0)
      {
        try
        {
          this.index = methodAccess.getIndex(method.getName(), 
              ReflectionHelper.NO_PARAMETERS);
        }
        catch(IllegalArgumentException e)
        {
          throw new IllegalArgumentException("Methods with argument "
              + "variables must have @Path annotations with matching "
              + "variable capture(s) (ex: @Path(\"{var}\"). See "
              + getClass().getName() + "#" + method.getName());
        }
      }
      else
      {
        if (classes.length == variableCount)
        {
          this.index = methodAccess.getIndex(method.getName(), classes);
        }
        else
        {
          throw new IllegalAccessError("@Path annotations with variable " 
              + "notations must have method parameters to match. See "
              + getClass().getName() + "#" + method.getName());
        }
      }
    }
    
    private UriSegment[] parseSegments(String uriToParse)
    {
      String[] segmentStrings = uriToParse.split("/");
      final UriSegment[] uriSegments = new UriSegment[segmentStrings.length];
      
      for (int i = 0; i < segmentStrings.length; i++)
      {
        uriSegments[i] = new UriSegment(segmentStrings[i]);
      }
      
      return uriSegments;
    }
    
    @Override
    public String toString()
    {
      final StringBuilder sb = new StringBuilder();
      boolean empty = true;
      for (UriSegment segment : segments)
      {
        if (!empty)
        {
          sb.append(","); 
        }
        sb.append(segment.toString());
        empty = false;
      }
      
      return "PSM [" + method.getName() + "; " + httpMethod + "; " + 
          index + "; " + sb.toString() + "]"; 
    }
    
    protected static class UriSegment
    {
      public static final String WILDCARD = "*";
      public static final String VARIABLE_PREFIX = "{";
      public static final String VARIABLE_SUFFIX = "}";
      public static final String EMPTY = "";
      
      public final boolean      isWildcard;
      public final boolean      isVariable;
      public final String       segment;
      public Class<?>           type;
      public MethodAccess       methodAccess;
      
      public UriSegment(String segment)
      {
        this.isWildcard = segment.equals(WILDCARD);
        this.isVariable = segment.startsWith(VARIABLE_PREFIX)
            && segment.endsWith(VARIABLE_SUFFIX);
        if (this.isVariable)
        {
          // Minor optimization - no reason to potentially create multiple
          // nodes all of which are variables since the inside of the variable
          // is ignored in the end. Treating the segment of all variable nodes
          // as "{}" regardless of whether the actual segment is "{var}" or 
          // "{foo}" forces all branches with variables at a given depth to
          // traverse the same sub-tree. That is, "{var}/foo" and "{var}/bar"
          // as the only two annotated methods in a handler will result in a
          // maximum of 3 comparisons instead of 4. Mode variables at same
          // depths would make this optimization felt more strongly.
          this.segment = VARIABLE_PREFIX + VARIABLE_SUFFIX;
        }
        else
        {
          this.segment = segment;
        }
      }
      
      public final String getVariableName()
      {
        if (this.isVariable)
        {
          return this.segment
            .replace(UriSegment.VARIABLE_PREFIX, UriSegment.EMPTY)
            .replace(UriSegment.VARIABLE_SUFFIX, UriSegment.EMPTY);
        }
        
        return null;
      }
      
      @Override
      public String toString()
      {
        return "{segment: '" + segment + 
            "', isVariable: " + isVariable + 
            ", isWildcard: " + isWildcard + "}";
      }
    }
  }
}
