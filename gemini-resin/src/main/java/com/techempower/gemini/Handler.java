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

package com.techempower.gemini;

import com.techempower.gemini.handler.*;

/**
 * An application's Dispatcher contains a list of Handlers that are 
 * responsible for handling specific requests.
 *    <p>
 * Handlers are used by the Dispatcher to determine what semantic action 
 * to take as a response to a user request.  The Dispatcher asks each 
 * Handler to accept an incoming request.  Each Handler is responsible 
 * for knowing what it wants to accept and what it's not interested in.
 * The Dispatcher has no logic concerning which Handlers should accept
 * a specific request.
 *    <p>
 * Each Handler will perform the necessary processing required in the
 * back-end with application-tier objects.  A set of deliveries will be
 * "passed" by the Handler to a JSP page via the Context.putDelivery 
 * method.  A JSP page can be "invoked" by calling Context.includeJSP.
 *    <p>
 * The following methods need to be implemented in order to build a 
 * Handler:
 *    <ul>
 * <li>getDescription - Provides a String description of the handler.
 * <li>acceptRequest - Analyzes a request and determines if this handler
 *     wishes to handle it.  The Handler can perform any logic necessary
 *     to determine whether it would like to accept a request.  However,
 *     the Handler's logic should be structured to be extremely quick
 *     at making <i>this decision</i>.  The acceptRequest method returns
 *     true if the Handler can handle the request and false otherwise.
 * <li>handleRequest - Handles a request.  This is called by the Dispatcher
 *     if the implementation of acceptRequest returns true.  However, 
 *     this can also be called directly by external objects to bypass 
 *     the acceptance check.
 *    </ul>
 *    <p>   
 * Handlers can also use annotations to inform the dispatcher which commands
 * the handler accepts, which methods are meant to handle that request and 
 * what constraints the request may have. The available annotations are:
 *    <ul>
 * <li>CMD(String[] command) - Informs the dispatcher which commands the 
 *     method can handle. Available at the method level.
 * <li>RequireLogin - Requires that the user be logged in before gaining
 *     access to the command. Available at both the method level and the class
 *     level.  This annotation applies to all handle methods if declared at 
 *     the class level. Also, if declared at the class level, any subclasses 
 *     will inherit this annotations, which can be used to make a 
 *     SecureHandler.
 * <li>Param(String name, String defaultValue) - Grabs a String value from the 
 *     request parameters and adds it as an argument to the method. Available
 *     at the field level or at the method level if used with the Params 
 *     annotation.
 * <li>Params(Param[]) - Signals that multiple parameters should be gotten 
 *     from the request and added as arguments to the method. Available only
 *     at the method level.
 * <li>JSP - Informs the dispatcher that upon a successful handle, include 
 *     this jsp. Available at the method level.
 *    </ul>
 *    
 * The method signature must be public and it must return a boolean. The 
 * command is no longer passed as an argument to the method and Dispatcher and
 * Context are optional.
 * 
 * Example use with Params annotation:
 *    <p>
 *    <pre><br>
 *    &#064;CMD(CMD_FOO)
 *    &#064;JSP(JSP_FOO)
 *    &#064;RequireLogin
 *    &#064;Params({
 *      &#064;Param(PARAM_1),
 *      &#064;Param(name=PARAM_2, "-1")
 *    })
 *    public boolean handleFoo(String param1, String param2)
 *    {
 *      ...
 *    }
 *    </pre>
 * 
 * Example using Param inline:
 *    <p>
 *    <pre><br>
 *    &#064;CMD(CMD_FOO)
 *    &#064;JSP(JSP_FOO)
 *    &#064;RequireLogin
 *    public boolean handleFoo(Dispatcher dispatcher, Context context,
 *      &#064;Param(PARAM_1) String param1,
 *      &#064;Param(name=PARAM_2, defaultValue="-1") String param2)
 *    {
 *      ...
 *    }
 *    </pre>
 *
 * @see BasicHandler A demonstration implementation
 * 
 * Warning: This class will soon be deprecated.
 */
public interface Handler<D extends BasicDispatcher, C extends BasicContext>
{
  //
  // Constants.
  //
  
  int PRIORITY_NO_CHANGE = -1000;
  int PRIORITY_MINIMUM = Thread.MIN_PRIORITY;
  int PRIORITY_MAXIMUM = Thread.MAX_PRIORITY;
  
  //
  // Member methods.
  //

  /**
   * Gets a description of the handler.
   */
  String getDescription();
  
  /**
   * Gets the desired thread priority of this handler.  Default should be 
   * Handler.PRIORITY_NO_CHANGE.
   * 
   * @see java.lang.Thread
   */
  int getPriority();

  /**
   * Tells the Dispatcher whether this Handler is willing to accept the
   * current request.  This is usually done by determining if the command 
   * is acceptable.  Return false if the command is unacceptable; return 
   * true if the command is acceptable and the request should be 
   * dispatched to the Handler's handleRequest method.
   */
  boolean acceptRequest(D dispatcher, C context, String command);

  /**
   * Handle the request, regardless of whether the command is acceptable.
   * Return false if the request could not be handled.
   */
  boolean handleRequest(D dispatcher, C context, String command);

}   // End Handler.
