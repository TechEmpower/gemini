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

/**
 * Once an HTTP request has been successfully promoted to a WebSocket by
 * the WebSocketServlet and a protocol-matched WebSocketHandler, a
 * WebsocketProcessor is then responsible for processing inbound and outbound
 * communication over the Websocket for the socket's life-span.
 */
public interface WebsocketProcessor
{

  /**
   * Gets the WebSocketContext.
   */
  WebsocketContext getContext();

  /**
   * Sets the WebSocketContext.
   */
  void setContext(WebsocketContext context);
  
  /**
   * Start the Processor.  Called when the interface's onStart method is
   * called.
   */
  void startup();
  
  /**
   * Tear down the Processor.  Called when any of the interface's termination
   * methods are called.
   */
  void teardown();
  
  /**
   * Receive binary data from the client.
   */
  void readBinary(InputStream is) 
      throws IOException;
  
  /**
   * Receive text data from the client.
   */
  void readText(Reader reader)
      throws IOException;
  
  /**
   * Sends text to the client.
   */
  void sendText(String text);
  
}
