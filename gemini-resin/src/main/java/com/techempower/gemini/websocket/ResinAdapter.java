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

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.websocket.*;

/**
 * Adapts the WebSocket implementation from Caucho Resin 4.x to our simplified 
 * and generalized library.  Note that Resin 4.29+ is recommended for this
 * to function properly.
 */
public class   ResinAdapter
    implements WebsocketAdapter
{

  public ResinAdapter()
  {
    // Do nothing.
  }
  
  @Override
  public void promoteToWebsocket(WebsocketProcessor processor,
      HttpServletRequest request)
          throws ServletException
  {
    // Create the socket adapter.
    final SocketAdapter socketAdapter = new SocketAdapter(processor);
    
    // Switch the request to a Websocket request.
    final WebSocketServletRequest wsReq = (WebSocketServletRequest)request;
    
    try
    {
      wsReq.startWebSocket(socketAdapter);
    }
    catch (IOException ioexc)
    {
      throw new ServletException("Could not start Websocket.", ioexc);
    }
  }
  
  //
  // Classes.
  //

  private class  SocketAdapter
      implements WebSocketListener
  {
    private final WebsocketProcessor processor;
  
    public SocketAdapter(WebsocketProcessor processor)
    {
      this.processor = processor;
    }
    
    @Override
    public void onClose(WebSocketContext wsc) throws IOException
    {
      processor.teardown();
    }
  
    @Override
    public void onDisconnect(WebSocketContext wsc) throws IOException
    {
      processor.teardown();
    }
  
    @Override
    public void onReadBinary(WebSocketContext wsc, InputStream is)
        throws IOException
    {
      processor.readBinary(is);
    }
  
    @Override
    public void onReadText(WebSocketContext wsc, Reader reader)
        throws IOException
    {
      processor.readText(reader);
    }
  
    @Override
    public void onStart(WebSocketContext wsc) throws IOException
    {
      processor.setContext(new ContextAdapter(wsc));
      processor.startup();
    }
  
    @Override
    public void onTimeout(WebSocketContext wsc) throws IOException
    {
      processor.teardown();
    }
  }
  
  public class ContextAdapter
    implements WebsocketContext
  {
    private WebSocketContext wsc;
    
    public ContextAdapter(WebSocketContext wsc)
    {
      this.wsc = wsc;
    }

    @Override
    public void close()
    {
      wsc.close();
    }

    @Override
    public void sendText(String toSend)
        throws IOException
    {
      try (PrintWriter writer = wsc.startTextMessage())
      {
        writer.println(toSend);
      }
      finally
      {
        // Do nothing.
      }
    }

    @Override
    public void setTimeout(long milliseconds)
    {
      wsc.setTimeout(milliseconds);
    }
  }

}
