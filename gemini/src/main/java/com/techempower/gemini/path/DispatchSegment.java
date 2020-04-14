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

import java.util.*;

import com.techempower.gemini.*;

/**
 * A PathHandler that consumes a path segment and then dispatches to a set of
 * "child" PathHandlers.  This is used to allow the construction of a full
 * virtual directory structure.
 *   <p>
 * In the example below, TestHandler will be available at /foo/[bar]/test,
 * where [bar] indicates an argument named "bar" will be captured.
 *   <p><pre>
 * config.add("foo", new DispatchSegment&lt;MyContext&gt;()
 *     .arg("bar", new DispatchSegment&lt;MyContext&gt;()
 *         .add("test", new TestHandler(...)
 *     )
 * );
 *   </pre><p>
 * In other words, given the URI /foo/cat/test, from within TestHandler, a
 * call to args().get("bar") would return "cat".
 */
public class   DispatchSegment<C extends BasicContext>
    implements PathHandler<C>,
               UriAware
{
  
  protected final Map<String, MappedHandler<C>> handlers;
  protected final String capturedArgumentName;
  protected final MappedHandler<C> defaultHandler;
  protected       String baseUri;
  
  /**
   * Constructor.
   */
  public DispatchSegment()
  {
    this.handlers = new HashMap<>(1);
    this.defaultHandler = null;
    this.capturedArgumentName = null;
  }
  
  /**
   * Copy-constructor used by the add() method.
   * 
   * @param handlers A map of handlers to use.
   * @param defaultHandler The optional default Handler to use if the current
   *     segment of a request does not match one of the named handlers.
   */
  protected DispatchSegment(Map<String, MappedHandler<C>> handlers, 
      MappedHandler<C> defaultHandler)
  {
    this.handlers = new HashMap<>(handlers.size() + 1);
    this.handlers.putAll(handlers);
    this.defaultHandler = defaultHandler;
    this.capturedArgumentName = null;
  }
  
  /**
   * Copy-constructor used by the arg() method.
   * 
   * @param argumentName the name of the argument (the captured path segment
   *     as a named value).
   * @param nextHandler the next handler in the segment chain; required.
   */
  protected DispatchSegment(String argumentName, MappedHandler<C> nextHandler)
  {
    this.handlers = null;
    this.capturedArgumentName = argumentName;
    this.defaultHandler = nextHandler;
  }
  
  @Override
  public void setBaseUri(String uri)
  {
    this.baseUri = uri;
    
    // Fan out the notification to the child segments.
    if (handlers != null)
    {
      for (Map.Entry<String, DispatchSegment.MappedHandler<C>> entry
          : handlers.entrySet())
      {
        final MappedHandler<C> mh = entry.getValue();
        if (mh.handler instanceof UriAware)
        {
          final UriAware uriAware = (UriAware)mh.handler;
          uriAware.setBaseUri(baseUri + "/" + entry.getKey());
        }
      }
    }
    
    // Fan out to the default handler as well.
    if (  (defaultHandler != null)
       && (defaultHandler.handler instanceof UriAware)
       )
    {
      final UriAware uriAware = (UriAware)defaultHandler.handler;
      uriAware.setBaseUri(baseUri);
    }
  }
  
  /**
   * Adds a Handler.  This is done by cloning this object so that the 
   * resulting "handlers" map data structure is immutable.
   * 
   * @param pathSegment the named path segment to use for routing to the
   *     provided PathHandler.
   * @param handler The handler that should receive requests when the provided
   *     pathSegment matches the request.
   * @param followingArgumentNames an optional array of additional names for
   *     any arguments that follow the current segment in the URI. 
   */
  public DispatchSegment<C> add(String pathSegment, PathHandler<C> handler, 
      String... followingArgumentNames)
  {
    DispatchSegment<C> toReturn = new DispatchSegment<>(handlers, 
        defaultHandler);
    toReturn.handlers.put(pathSegment, 
        new MappedHandler<>(handler, followingArgumentNames));
    return toReturn;
  }
  
  /**
   * Sets this DispatchSegment to capture a named path segment (an "argument")
   * and always dispatch to the provided next handler.  arg() and add() 
   * methods cannot be mixed within a single DispatchSegment; they are
   * mutually exclusive and the last used will win.
   * 
   * @param argumentName The name to assign to the path segment, creating an 
   *     argument.
   * @param nextHandler the handler that always receives the matching request.
   */
  public DispatchSegment<C> arg(String argumentName, PathHandler<C> nextHandler)
  {
    return new DispatchSegment<>(argumentName,
        new MappedHandler<>(nextHandler, null));
  }
  
  /**
   * Sets the default Handler.  This is done by cloning this object.
   * 
   * @param defaultHandler The handler that will receive any requests 
   *     that do not match any of the mapped handlers.
   * @param followingArgumentNames an optional array of additional names for
   *     any arguments that follow the current segment in the URI. 
   */
  public DispatchSegment<C> setDefault(PathHandler<C> defaultHandler, 
      String... followingArgumentNames)
  {
    return new DispatchSegment<>(this.handlers, 
        new MappedHandler<>(defaultHandler, followingArgumentNames));
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
    final String segment = segments.get(0);
    final MappedHandler<C> mappedHandler = handlers != null 
        ? handlers.get(segment)
        : null;
    final MappedHandler<C> handlerToUse = mappedHandler != null 
        ? mappedHandler 
        : defaultHandler;

    // Is there a mapped handler or a default handler we can use?
    if (handlerToUse != null)
    {
      final PathHandler<C> handler = handlerToUse.handler;
      
      // Do we capture this segment as an argument?
      if (capturedArgumentName != null)
      {
        segments.assignName(0, capturedArgumentName);
      }
      
      // Capture additional segments if we've got those too.
      if (handlerToUse.followingArgumentNames != null)
      {
        int index = 1;
        for (String name : handlerToUse.followingArgumentNames) 
        {
          segments.assignName(index++, name);
        }
      }
      
      // "Consume" the current segment and increase the offset only if the
      // default handler was not selected.
      if (  (mappedHandler != null)
         || (capturedArgumentName != null)
         )
      {
        segments.increaseOffset();
      }
      
      // Prehandle, handle, and posthandle.
      try
      {
        return handler.prehandle(segments, context) || handler.handle(
            segments, context);
      }
      finally
      {
        try
        {
          handler.posthandle(segments, context);
        }
        finally
        {
          if (  (mappedHandler != null)
             || (capturedArgumentName != null)
             )
          {
            segments.decreaseOffset();
          }
        }
      }
    }

    // No handler available, we can't handle this request.
    return false;
  }
  
  /**
   * A simple structure of a PathHandler and an optional String array of 
   * following argument names.
   */
  static class MappedHandler<C extends BasicContext>
  {
    final PathHandler<C> handler;
    final String[] followingArgumentNames;
    
    MappedHandler(PathHandler<C> hdlr, String[] argumentNames)
    {
      handler = hdlr;
      followingArgumentNames = argumentNames;      
    }
  }

}
