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


import java.util.ArrayList;
import java.util.List;

public class PathUriTree
{
    private final PathUriTree.Node root;

    public PathUriTree()
    {
        root = new PathUriTree.Node(null);
    }

    /**
     * Searches the tree for a node that best handles the given segments.
     */
    public final PathUriMethod search(PathSegments segments)
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
    private PathUriMethod search(PathUriTree.Node node, PathSegments segments, int offset)
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
            PathUriMethod bestVariable = null; // Best at this depth
            PathUriMethod bestWildcard = null; // Best at this depth
            PathUriMethod toReturn     = null;
            for (PathUriTree.Node child : node.children)
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
        root.addChild(root, method, 0);
    }

    /**
     * A node in the tree of PathUriMethod.
     */
    public static class Node
    {
        private PathUriMethod method;
        private final PathUriMethod.UriSegment segment;
        private final List<PathUriTree.Node> children;

        public Node(PathUriMethod.UriSegment segment)
        {
            this.segment = segment;
            this.children = new ArrayList<>();
        }

        @Override
        public String toString()
        {
            return "{" +
                    "method: " +
                    method +
                    ", segment: " +
                    segment +
                    ", childrenCount: " +
                    this.children.size() +
                    "}";
        }

        /**
         * Returns the immediate child node for the given segment and creates
         * if it does not exist.
         */
        private PathUriTree.Node getChildForSegment(PathUriTree.Node node, PathUriMethod.UriSegment[] segments, int offset)
        {
            PathUriTree.Node toRet = null;
            for(PathUriTree.Node child : node.children)
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
                toRet = new PathUriTree.Node(segments[offset]);
                node.children.add(toRet);
            }
            return toRet;
        }

        /**
         * Recursively adds the given PathUriMethod to this tree at the
         * appropriate depth.
         */
        private void addChild(PathUriTree.Node node, PathUriMethod uriMethod, int offset)
        {
            if (uriMethod.segments.length > offset)
            {
                final PathUriTree.Node child = getChildForSegment(node, uriMethod.segments, offset);
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
