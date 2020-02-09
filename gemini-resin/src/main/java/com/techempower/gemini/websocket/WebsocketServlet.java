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
package com.techempower.gemini.websocket;

import java.io.*;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.gemini.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple foundation for WebSocket support in Gemini applications.
 */
public class WebsocketServlet
     extends HttpServlet
{

  //
  // Constants.
  //
  
  private static final long serialVersionUID = 4902296554565036185L;

	public static final String COMPONENT_CODE = "wssv";    // Four-letter component ID
	public static final String PROTOCOL_HEADER = "Sec-WebSocket-Protocol";
  
  //
  // Member variables.
  //
  
  private final Logger           log = LoggerFactory.getLogger(COMPONENT_CODE);
  private final WebsocketAdapter adapter;
  private final ConcurrentHashMap<String, WebsocketHandler> listeners;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  protected WebsocketServlet(WebsocketAdapter adapter)
  {
    this.adapter = adapter;
    this.listeners = new ConcurrentHashMap<>();
  }
  
  @Deprecated(forRemoval = true)
  protected WebsocketServlet(GeminiApplication application,
                             WebsocketAdapter adapter)
  {
    this(adapter);
  }
  
  /**
   * Add a WebsocketHandler by mapping to a protocol name.
   */
  public WebsocketServlet addHandler(String protocol, WebsocketHandler handler)
  {
    this.listeners.put(protocol, handler);
    return this;
  }
  
  /**
   * Servlet Initialization.
   */
  @Override
  public void init()
      throws ServletException
  {
    super.init();
  }
  
  /**
   * Process a Websocket request; reject non-Websocket requests.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException
  {
    final String protocol = request.getHeader(PROTOCOL_HEADER);
    final WebsocketHandler handler = this.listeners.get(protocol);
    
    if (handler != null)
    {
      // Chrome requires that the same protocol is specified in the response
      // headers.
      response.addHeader(PROTOCOL_HEADER, protocol);
      final WebsocketProcessor processor = handler.handleRequest(request, response);

      // If the request has been accepted by the handler, a processor will
      // be returned and we can stop our work.
      if (processor != null)
      {
        this.adapter.promoteToWebsocket(processor, request);
        return;
      }
    }
    else
    {
      this.log.debug("Bad WebSocket protocol: {}", protocol);
    }
    
    // If we get here, response with a "forbidden" code.
    sendForbidden(response);
  }
  
  /**
   * Sends a forbidden response.
   */
  private void sendForbidden(HttpServletResponse response)
  {
    try
    {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    catch (IllegalStateException | IOException exc)
    {
      // Don't care.
    }
  }
  
}
