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

package com.techempower.gemini.handler;

import com.techempower.gemini.*;

/**
 * This is a meaningless implementation of the Handler interface.  The 
 * BasicHandler does no useful processing; it accepts all requests
 * and outputs a simple message to the provided Context explaining that
 * it was invoked.
 *    <p>
 * This class is mostly provided as a demonstration of how to implement a
 * Handler.  It <i>can</i> be directly subclassed if you would rather
 * not implement all methods of the Handler interface.  This, however,
 * is not advised.
 *
 * @see Handler
 * @see Context
 */
public class BasicHandler
  implements Handler<BasicDispatcher,LegacyContext>
{
  //
  // Members
  //
  
  private String description = "Basic";
  
  //
  // Member methods.
  //

  /**
   * Constructor.  The Constructor does nothing in BasicHandler.
   */
  public BasicHandler()
  {
    // Constructor does nothing.
  }    

  /**
   * Gets a description of the Handler.
   */
  @Override
  public String getDescription()
  {
    return this.description;
  }

  /**
   * Gets the desired thread priority of this handler.  Default should be 
   * Handler.PRIORITY_NO_CHANGE.
   * 
   * @see java.lang.Thread
   */
  @Override
  public int getPriority()
  {
    return Handler.PRIORITY_NO_CHANGE;
  }

  /**
   * Tells the Dispatcher whether this Handler is willing to accept the
   * current request.  This is usually done by determining if the command 
   * is acceptable.  Return false if the command is unacceptable; return 
   * true if the command is acceptable and the request should be 
   * dispatched to the Handler's handleRequest method.
   */
  @Override
  public boolean acceptRequest(BasicDispatcher dispatcher, LegacyContext context, String command)
  {
    return true;
  }

  /**
   * Handle the request, regardless of whether the command is
   * acceptable.  Return false if the request could not be handled.
   *    <p>
   * The BasicHandler just displays a simple message on the Context
   * and returns true always.
   */
  @Override
  public boolean handleRequest(BasicDispatcher dispatcher, LegacyContext context, String command)
  {
    context.print("Basic handler received request.");

    return true;
  }
}   // End BasicHandler.
