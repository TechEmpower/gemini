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
import com.techempower.helper.ReflectionHelper;

import java.lang.reflect.Method;

/**
 * Details of an annotated path segment method.
 */
public class PathUriMethod extends BasicPathHandler.BasicPathHandlerMethod
{
    public final Method method;
    public final String uri;
    public final PathUriMethod.UriSegment[] segments;
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
        for (PathUriMethod.UriSegment segment : segments)
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

        // Check for an configure the method to receive a context if the
        // method signature calls for one.
        if (variableCount < classes.length && Context.class.isAssignableFrom(
                method.getParameterTypes()[variableCount]))
        {
            // todo
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

    private PathUriMethod.UriSegment[] parseSegments(String uriToParse)
    {
        String[] segmentStrings = uriToParse.split("/");
        final PathUriMethod.UriSegment[] uriSegments = new PathUriMethod.UriSegment[segmentStrings.length];

        for (int i = 0; i < segmentStrings.length; i++)
        {
            uriSegments[i] = new PathUriMethod.UriSegment(segmentStrings[i]);
        }

        return uriSegments;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        boolean empty = true;
        for (PathUriMethod.UriSegment segment : segments)
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
                        .replace(PathUriMethod.UriSegment.VARIABLE_PREFIX, PathUriMethod.UriSegment.EMPTY)
                        .replace(PathUriMethod.UriSegment.VARIABLE_SUFFIX, PathUriMethod.UriSegment.EMPTY);
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
