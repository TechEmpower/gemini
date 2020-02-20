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

import com.techempower.gemini.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * Logs request dispatches.
 */
public class DispatchLogger
  implements DispatchListener {

  private final Logger  log = LoggerFactory.getLogger(getClass());
  private final BiConsumer<Logger, String> logMethod;
  private final boolean logIpAddress;
  
  /**
   * Constructor.
   */
  public DispatchLogger(GeminiApplication app,
                        BiConsumer<Logger, String> logMethod)
  {
    this(app, logMethod, true);
  }

  /**
   * Constructor.
   */
  public DispatchLogger(GeminiApplication app,
                        BiConsumer<Logger, String> logMethod,
                        boolean logIpAddress)
  {
    this.logMethod = logMethod;
    this.logIpAddress = logIpAddress;
  }
  
  @Override
  public void dispatchStarting(Dispatcher dispatcher, Context context,
      String command) {
    if(this.logIpAddress)
    {
      logMethod.accept(log, context.getClientId() + " - " + command);
    }
    else
    {
      logMethod.accept(log, command);
    }
  }

  @Override
  public void redispatchOccurring(Dispatcher dispatcher, Context context,
      String previousCommand, String newCommand) {
    // Does nothing.
  }

  @Override
  public void dispatchComplete(Dispatcher dispatcher, Context context) {
    // Does nothing.
  }

  @Override
  public void renderStarting(Dispatcher dispatcher, String jspName) {
    // Does nothing.
  }

  @Override
  public void renderComplete(Dispatcher dispatcher, Context context) {
    // Does nothing.
  }

}
