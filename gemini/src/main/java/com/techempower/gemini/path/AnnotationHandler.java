/*
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
 */
package com.techempower.gemini.path;


import com.esotericsoftware.reflectasm.MethodAccess;
import com.techempower.gemini.Context;
import com.techempower.gemini.HttpRequest;
import com.techempower.gemini.path.annotation.*;
import com.techempower.helper.NumberHelper;
import com.techempower.helper.ReflectionHelper;
import com.techempower.helper.StringHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.techempower.gemini.HttpRequest.HEADER_ACCESS_CONTROL_REQUEST_METHOD;
import static com.techempower.gemini.HttpRequest.HttpMethod.*;

/**
 * Similar to MethodUriHandler, AnnotationHandler class does the same
 * strategy of creating `PathUriTree`s for each HttpRequest.Method type
 * and then inserting handler methods into the trees.
 * @param <C>
 */
class AnnotationHandler<C extends Context> {
    final String rootUri;
    final Object handler;

    private final PathUriTree getRequestHandleMethods;
    private final PathUriTree putRequestHandleMethods;
    private final PathUriTree postRequestHandleMethods;
    private final PathUriTree deleteRequestHandleMethods;
    protected final MethodAccess methodAccess;

    public AnnotationHandler(String rootUri, Object handler) {
        this.rootUri = rootUri;
        this.handler = handler;

        getRequestHandleMethods = new PathUriTree();
        putRequestHandleMethods = new PathUriTree();
        postRequestHandleMethods = new PathUriTree();
        deleteRequestHandleMethods = new PathUriTree();

        methodAccess = MethodAccess.get(handler.getClass());
        discoverAnnotatedMethods();
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

                addAnnotatedHandleMethod(psm);
            }
        }
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
     * @return Whether this handler successfully handled the given request
     */
    public boolean handle(PathSegments segments, C context) {
        return dispatchToAnnotatedMethod(segments, getAnnotatedMethod(segments, context),
                context);
    }

    /**
     * Gets the Header-appropriate string representation of the http method
     * names that this handler supports for the given path segments.
     * <p>
     * For example, if this handler has two handle methods at "/" and
     * one is GET and the other is POST, this method would return the string
     * "GET, POST" for the PathSegments "/".
     */
    public String getAccessControlAllowMethods(PathSegments segments, C context)
    {
        final StringBuilder reqMethods = new StringBuilder();
        final List<PathUriMethod> methods = new ArrayList<>();

        if(context.headers().get(HEADER_ACCESS_CONTROL_REQUEST_METHOD) != null)
        {
            final PathUriMethod put = putRequestHandleMethods.search(segments);
            if (put != null)
            {
                methods.add(put);
            }
            final PathUriMethod post = postRequestHandleMethods.search(segments);
            if (post != null)
            {
                methods.add(postRequestHandleMethods.search(segments));
            }
            final PathUriMethod delete = deleteRequestHandleMethods.search(segments);
            if (delete != null)
            {
                methods.add(deleteRequestHandleMethods.search(segments));
            }
            final PathUriMethod get = getRequestHandleMethods.search(segments);
            if (get != null)
            {
                methods.add(getRequestHandleMethods.search(segments));
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
    protected boolean dispatchToAnnotatedMethod(PathSegments segments,
                                                PathUriMethod method,
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
                final Object handlerReturnVal = methodAccess.invoke(
                        handler,
                        method.index,
                        ReflectionHelper.NO_VALUES);
                // todo: MethodUriHandler invoked methods would always return
                //       a boolean to denote whether they properly handled the
                //       request. Here, we need to determine the serialization
                //       strategy, attempt to serialize, and handle errors.
                return (Boolean)handlerReturnVal;
            }
            else
            {
                // We have already enforced that the @Path annotations have the correct
                // number of args in their declarations to match the variable count
                // in the respective URI. So, create an array of values and try to set
                // them via retrieving them as segments.
                try
                {
                    final Object handlerReturnVal = methodAccess.invoke(
                            handler,
                            method.index,
                            getVariableArguments(segments, method, context));
                    // todo
                    return (Boolean)handlerReturnVal;
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
     * methods and returning them as an argument array (in order of appearance).
     *   <p>
     * Example:
     *   <p>
     * <pre>
     * @Path("foo/{var1}/{var2}")
     * public boolean handleFoo(int var1, String var2)
     * </pre>
     *
     * The array returned for `GET /foo/123/asd` would be: [123, "asd"]
     * @param method the annotated method.
     * @return Array of corresponding values.
     */
    @SuppressWarnings({ "unchecked"})
    private Object[] getVariableArguments(PathSegments segments,
                                          PathUriMethod method,
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
                    // No reason to continue - we found all variables.
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
}
