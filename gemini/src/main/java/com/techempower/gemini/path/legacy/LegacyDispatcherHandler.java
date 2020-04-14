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
package com.techempower.gemini.path.legacy;

import com.techempower.gemini.*;
import com.techempower.gemini.path.*;

/**
 * Wraps a legacy Dispatcher such as the BasicDispatcher as a PathHandler
 * suitable for use as the PathDispatcher's default handler until such time
 * that all legacy handlers can be ported to PathHandlers.
 */
public class   LegacyDispatcherHandler<C extends Context>
    implements PathHandler<C>
{
  
  private final Dispatcher legacyDispatcher;
  
  /**
   * Constructor.
   */
  public LegacyDispatcherHandler(Dispatcher legacyDispatcher)
  {
    this.legacyDispatcher = legacyDispatcher;
  }

  @Override
  public boolean prehandle(PathSegments segments, C context)
  {
    // Does nothing.
    return false;
  }

  @Override
  public void posthandle(PathSegments segments, C context)
  {
    // Does nothing.
  }

  @Override
  public boolean handle(PathSegments segments, C context)
  {
    // Ask the legacy Dispatcher to dispatch on the context.
    return legacyDispatcher.dispatch(context);
  }

}
