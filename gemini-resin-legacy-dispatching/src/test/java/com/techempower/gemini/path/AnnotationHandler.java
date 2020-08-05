package com.techempower.gemini.path;


import com.esotericsoftware.reflectasm.MethodAccess;
import com.techempower.gemini.Context;
import com.techempower.gemini.Request;
import com.techempower.gemini.path.annotation.*;
import com.techempower.helper.NumberHelper;
import com.techempower.helper.ReflectionHelper;
import com.techempower.helper.StringHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.techempower.gemini.Request.HEADER_ACCESS_CONTROL_REQUEST_METHOD;
import static com.techempower.gemini.Request.HttpMethod.*;

/**
 * Similar to MethodUriHandler, AnnotationHandler class does the same
 * strategy of creating `PathUriTree`s for each Request.Method type
 * and then inserting handler methods into the trees.
 * @param <C>
 */
class AnnotationHandler<C extends Context> {
  final String rootUri;
  final Object handler;

  private final AnnotationHandler.PathUriTree getRequestHandleMethods;
  private final AnnotationHandler.PathUriTree putRequestHandleMethods;
  private final AnnotationHandler.PathUriTree postRequestHandleMethods;
  private final AnnotationHandler.PathUriTree deleteRequestHandleMethods;
  protected final MethodAccess methodAccess;

  public AnnotationHandler(String rootUri, Object handler) {
    this.rootUri = rootUri;
    this.handler = handler;

    getRequestHandleMethods = new AnnotationHandler.PathUriTree();
    putRequestHandleMethods = new AnnotationHandler.PathUriTree();
    postRequestHandleMethods = new AnnotationHandler.PathUriTree();
    deleteRequestHandleMethods = new AnnotationHandler.PathUriTree();

    methodAccess = MethodAccess.get(handler.getClass());
    discoverAnnotatedMethods();
  }

  /**
   * Adds the given PathUriMethod to the appropriate list given
   * the request method type.
   */
  private void addAnnotatedHandleMethod(AnnotationHandler.PathUriMethod method)
  {
    switch (method.httpMethod)
    {
      case PUT:
        putRequestHandleMethods.addMethod(method);
        break;
      case POST:
        postRequestHandleMethods.addMethod(method);
        break;
      case DELETE:
        deleteRequestHandleMethods.addMethod(method);
        break;
      case GET:
        getRequestHandleMethods.addMethod(method);
        break;
      default:
        break;
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
  protected AnnotationHandler.PathUriMethod analyzeAnnotatedMethod(Path path, Method method,
                                                                   Request.HttpMethod httpMethod)
  {
    // Only allow accessible (public) methods
    if (Modifier.isPublic(method.getModifiers()))
    {
      return new AnnotationHandler.PathUriMethod(
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
   * Discovers annotated methods at instantiation time.
   */
  private void discoverAnnotatedMethods()
  {
    final Method[] methods = handler.getClass().getMethods();

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
        final AnnotationHandler.PathUriMethod psm;
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

        addAnnotatedHandleMethod(psm);
      }
    }
  }

  /**
   * Determine the annotated method that should process the request.
   */
  protected AnnotationHandler.PathUriMethod getAnnotatedMethod(PathSegments segments,
                                                               C context)
  {
    final AnnotationHandler.PathUriTree tree;
    switch (((Request)context.getRequest()).getRequestMethod())
    {
      case PUT:
        tree = putRequestHandleMethods;
        break;
      case POST:
        tree = postRequestHandleMethods;
        break;
      case DELETE:
        tree = deleteRequestHandleMethods;
        break;
      case GET:
        tree = getRequestHandleMethods;
        break;
      default:
        // We do not want to handle this
        return null;
    }

    return tree.search(segments);
  }

  /**
   * Determine the annotated method that should process the request.
   */
  protected AnnotationHandler.PathUriMethod getAnnotatedMethod(PathSegments segments,
                                                               Request.HttpMethod httpMethod)
  {
    final AnnotationHandler.PathUriTree tree;
    switch (httpMethod)
    {
      case PUT:
        tree = putRequestHandleMethods;
        break;
      case POST:
        tree = postRequestHandleMethods;
        break;
      case DELETE:
        tree = deleteRequestHandleMethods;
        break;
      case GET:
        tree = getRequestHandleMethods;
        break;
      default:
        // We do not want to handle this
        return null;
    }

    return tree.search(segments);
  }

  /**
   * Locates the annotated method to call, invokes it given the path segments
   * and context.
   * @param segments The URI segments to route
   * @param context The current context
   * @return
   */
  public boolean handle(PathSegments segments, C context) {
    getAnnotatedMethod(segments, context);
    if (true) return true;
    return dispatchToAnnotatedMethod(segments, getAnnotatedMethod(segments, context),
        context);
  }

  public boolean handle(PathSegments segments, Request.HttpMethod httpMethod) {
    getAnnotatedMethod(segments, httpMethod);
    /*if (true)*/ return true;
    /*return dispatchToAnnotatedMethod(segments, getAnnotatedMethod(segments, context),
        context);*/
  }

  protected String getAccessControlAllowMethods(PathSegments segments, C context)
  {
    final StringBuilder reqMethods = new StringBuilder();
    final List<PathUriMethod> methods = new ArrayList<>();

    if(context.headers().get(HEADER_ACCESS_CONTROL_REQUEST_METHOD) != null)
    {
      final AnnotationHandler.PathUriMethod put = this.putRequestHandleMethods.search(segments);
      if (put != null)
      {
        methods.add(put);
      }
      final AnnotationHandler.PathUriMethod post = this.postRequestHandleMethods.search(segments);
      if (post != null)
      {
        methods.add(this.postRequestHandleMethods.search(segments));
      }
      final AnnotationHandler.PathUriMethod delete = this.deleteRequestHandleMethods.search(segments);
      if (delete != null)
      {
        methods.add(this.deleteRequestHandleMethods.search(segments));
      }
      final AnnotationHandler.PathUriMethod get = this.getRequestHandleMethods.search(segments);
      if (get != null)
      {
        methods.add(this.getRequestHandleMethods.search(segments));
      }

      boolean first = true;
      for(AnnotationHandler.PathUriMethod method : methods)
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
  protected boolean dispatchToAnnotatedMethod(PathSegments segments,
                                              AnnotationHandler.PathUriMethod method,
                                              C context)
  {
    // If we didn't find an associated method and have no default, we'll
    // return false, handing the request back to the default handler.
    if (method != null && method.index >= 0)
    {
      // TODO: I think defaultTemplate is going away; maybe put a check
      //       here that the method can be serialized in the annotated way.
      // Set the default template to the method's name.  Handler methods can
      // override this default by calling template(name) themselves before
      // rendering a response.
//            defaultTemplate(method.method.getName());

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
              getVariableArguments(segments, method, context));
        }
        catch (RequestBodyException e)
        {
          // todo
//                    log().log("Got RequestBodyException.", LogLevel.DEBUG, e);
//                    return this.error(e.getStatusCode(), e.getMessage());
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
  private Object[] getVariableArguments(PathSegments segments,
                                        AnnotationHandler.PathUriMethod method,
                                        C context)
      throws RequestBodyException
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
            args[argsIndex] = segments.getInt(i);
          }
          // long
          else if (method.segments[i].type.isAssignableFrom(long.class))
          {
            args[argsIndex] = NumberHelper.parseLong(segments.get(i));
          }
          // boolean
          else if (method.segments[i].type.isAssignableFrom(boolean.class))
          {
            // bool variables are NOT simply whether they are present.
            // Rather, it should be a truthy value.
            args[argsIndex] = StringHelper.equalsIgnoreCase(
                segments.get(i),
                new String[]{
                    "true", "yes", "1"
                });
          }
          // float
          else if (method.segments[i].type.isAssignableFrom(float.class))
          {
            args[argsIndex] = NumberHelper.parseFloat(segments.get(i), 0f);
          }
          // double
          else if (method.segments[i].type.isAssignableFrom(double.class))
          {
            args[argsIndex] = NumberHelper.parseDouble(segments.get(i), 0f);
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
          args[argsIndex] = segments.get(i);
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
                  indexOfMethodToInvoke, segments.get(i));
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
    final BasicPathHandler.RequestBodyParameter bodyParameter = method.bodyParameter;
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
    private final AnnotationHandler.PathUriTree.Node root;

    public PathUriTree()
    {
      root = new AnnotationHandler.PathUriTree.Node(null);
    }

    /**
     * Searches the tree for a node that best handles the given segments.
     */
    public final AnnotationHandler.PathUriMethod search(PathSegments segments)
    {
      return search(root, segments, 0);
    }

    /**
     * Searches the given segments at the given offset with the given node
     * in the tree. If this node is a leaf node and matches the segment
     * stack perfectly, it is returned. If this node is a leaf node and
     * either a variable or a wildcard node and the segment stack has run
     * out of segments to check, return that if we have not found a true
     * match.
     */
    private AnnotationHandler.PathUriMethod search(AnnotationHandler.PathUriTree.Node node, PathSegments segments, int offset)
    {
      if (node != root &&
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
        AnnotationHandler.PathUriMethod bestVariable = null; // Best at this depth
        AnnotationHandler.PathUriMethod bestWildcard = null; // Best at this depth
        AnnotationHandler.PathUriMethod toReturn     = null;
        for (AnnotationHandler.PathUriTree.Node child : node.children)
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
            AnnotationHandler.PathUriMethod temp = search(child, segments, offset + 1);
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
    public final void addMethod(AnnotationHandler.PathUriMethod method)
    {
      root.addChild(root, method, 0);
    }

    /**
     * A node in the tree of PathUriMethod.
     */
    public static class Node
    {
      private AnnotationHandler.PathUriMethod method;
      private final AnnotationHandler.PathUriMethod.UriSegment segment;
      private final List<AnnotationHandler.PathUriTree.Node> children;

      public Node(AnnotationHandler.PathUriMethod.UriSegment segment)
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
      private AnnotationHandler.PathUriTree.Node getChildForSegment(AnnotationHandler.PathUriTree.Node node, AnnotationHandler.PathUriMethod.UriSegment[] segments, int offset)
      {
        AnnotationHandler.PathUriTree.Node toRet = null;
        for(AnnotationHandler.PathUriTree.Node child : node.children)
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
          toRet = new AnnotationHandler.PathUriTree.Node(segments[offset]);
          node.children.add(toRet);
        }
        return toRet;
      }

      /**
       * Recursively adds the given PathUriMethod to this tree at the
       * appropriate depth.
       */
      private void addChild(AnnotationHandler.PathUriTree.Node node, AnnotationHandler.PathUriMethod uriMethod, int offset)
      {
        if (uriMethod.segments.length > offset)
        {
          final AnnotationHandler.PathUriTree.Node child = getChildForSegment(node, uriMethod.segments, offset);
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
      public final AnnotationHandler.PathUriMethod getMethod()
      {
        return this.method;
      }
    }
  }

  /**
   * Details of an annotated path segment method.
   */
  protected static class PathUriMethod extends BasicPathHandler.BasicPathHandlerMethod
  {
    public final Method method;
    public final String uri;
    public final AnnotationHandler.PathUriMethod.UriSegment[] segments;
    public final int index;

    public PathUriMethod(Method method, String uri, Request.HttpMethod httpMethod,
                         MethodAccess methodAccess)
    {
      super(method, httpMethod);

      this.method = method;
      this.uri = uri;
      this.segments = this.parseSegments(this.uri);
      int variableCount = 0;
      final Class<?>[] classes =
          new Class[method.getGenericParameterTypes().length];
      for (AnnotationHandler.PathUriMethod.UriSegment segment : segments)
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

    private AnnotationHandler.PathUriMethod.UriSegment[] parseSegments(String uriToParse)
    {
      String[] segmentStrings = uriToParse.split("/");
      final AnnotationHandler.PathUriMethod.UriSegment[] uriSegments = new AnnotationHandler.PathUriMethod.UriSegment[segmentStrings.length];

      for (int i = 0; i < segmentStrings.length; i++)
      {
        uriSegments[i] = new AnnotationHandler.PathUriMethod.UriSegment(segmentStrings[i]);
      }

      return uriSegments;
    }

    @Override
    public String toString()
    {
      final StringBuilder sb = new StringBuilder();
      boolean empty = true;
      for (AnnotationHandler.PathUriMethod.UriSegment segment : segments)
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
              .replace(AnnotationHandler.PathUriMethod.UriSegment.VARIABLE_PREFIX, AnnotationHandler.PathUriMethod.UriSegment.EMPTY)
              .replace(AnnotationHandler.PathUriMethod.UriSegment.VARIABLE_SUFFIX, AnnotationHandler.PathUriMethod.UriSegment.EMPTY);
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