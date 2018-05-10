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
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.js.*;
import com.techempower.util.*;

/**
 * A simplified implementation of WebsocketProcessor.
 */
public abstract class BasicWebsocketProcessor
    implements        WebsocketProcessor
{
  
  private final JavaScriptWriter  jsw;
  private       WebsocketContext  context;

  //
  // Member methods.
  //
  
  /**
   * Constructor.
   * 
   * @param application The application reference.
   * @param jsw A JavaScript writer for sending JSON to the client.
   */
  public BasicWebsocketProcessor(GeminiApplication application, JavaScriptWriter jsw)
  {
    this.jsw = (jsw != null ? jsw : application.getJavaScriptWriter());
  }
  
  /**
   * Constructor.
   */
  public BasicWebsocketProcessor(GeminiApplication application)
  {
    this(application, null);
  }
  
  /**
   * Gets the WebsocketContext.
   */
  @Override
  public WebsocketContext getContext()
  {
    return this.context;
  }

  /**
   * Sets the WebsocketContext.
   */
  @Override
  public void setContext(WebsocketContext context)
  {
    this.context = context;
  }
  
  /**
   * Start the Processor.  Called when the interface's onStart method is
   * called.
   */
  @Override
  public void startup()
  {
    getContext().setTimeout(getTimeout());
  }
  
  /**
   * Tear down the Processor.  Called after the socket has been closed (for
   * any reason).  Override to provide custom shutdown code.
   */
  @Override
  public void teardown()
  {    
  }
  
  /**
   * Returns a timeout duration in milliseconds.  The default is 2 minutes.
   */
  public long getTimeout()
  {
    return 2 * UtilityConstants.MINUTE;
  }

  /**
   * Receive binary data from the client.
   */
  @Override
  public void readBinary(InputStream is) throws IOException
  {
    // In this base class, we just consume anything found in the InputStream
    // and discard it.  Subclasses should definitely overload this method if
    // they intend to deal with binary data.
    byte[] discardBuffer = new byte[(int)(64 * UtilityConstants.KILOBYTE)];
    while (is.available() > 0)
    {
      is.read(discardBuffer);
    }
  }

  /**
   * Receive text data from the client.
   */
  @Override
  public void readText(Reader reader) throws IOException
  {
    // In this base class, we just consume anything found on the reader,
    // convert it to a String and then call readString(String).
    char[] buffer = new char[(int)(4 * UtilityConstants.KILOBYTE)];
    StringBuilder result = new StringBuilder();
    int read;
    do 
    {
      read = reader.read(buffer);
      if (read >= 1)
      {
        result.append(buffer, 0, read);
      }
    } while (read >= 1);
    
    readString(result.toString());
  }
  
  /**
   * Receives a String from the client.
   */
  public void readString(String string)
  {
    // Does nothing in this base class.
  }
  
  /**
   * Sends a named message (as JSON) to the client.  Creates a one-entry map
   * with the message type name as the key.
   */
  public void sendJson(String messageType, Object messageData)
  {
    final Map<String, Object> toSend = new HashMap<>(1);
    toSend.put(messageType, messageData);
    sendJson(toSend);
  }
  
  /**
   * Sends a JSON map to the client.
   */
  public void sendJson(Map<String, Object> toSend)
  {
    sendText(jsw.write(toSend));
  }
  
  /**
   * Sends text to the client.
   */
  @Override
  public void sendText(String text)
  {
    // We can't send anything unless there's a Context.
    if (getContext() != null)
    {
      try
      {
        getContext().sendText(text);
      }
      catch (IOException ioexc)
      {
        // Close the socket if we get an exception while writing.
        close();
      }
    }
  }
  
  /**
   * Close the socket.
   */
  public void close()
  {
    if (getContext() != null)
    {
      getContext().close();
    }
  }

}
