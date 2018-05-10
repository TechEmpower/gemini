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

package com.techempower.js.legacy;

/**
 * An iterator-like object that traverses objects encountered by 
 * JavaScriptWriter.  The flow of calls to a visitor is:
 *
 * <ol>
 * <li>{@code isArray()} - if this returns {@code true}, then always skip step 4</li>
 * <li>{@code hasNext()} - if this returns {@code false}, then stop</li>
 * <li>{@code next()}</li>
 * <li>{@code name()}</li>
 * <li>{@code value()}</li>
 * <li>go to step 2</li>
 * </ol>
 *
 * @see LegacyJavaScriptWriter
 * @see VisitorFactory
 */
public interface Visitor
{
  /**
   * Returns {@code true} if there are more nodes to visit in the object.
   */
  boolean hasNext();

  /**
   * Returns {@code true} if the object should be treated as an array.
   */
  boolean isArray();

  /**
   * Returns the name of the current node.  Only applies to non-array visitors.
   */
  String name();

  /**
   * Advances to the next node.
   */
  void next();

  /**
   * Returns the value of the current node.
   */
  Object value();
}
