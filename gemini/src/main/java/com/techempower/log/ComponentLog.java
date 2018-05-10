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

package com.techempower.log;

import com.techempower.helper.*;

/**
 * ComponentLog provides a means to easily attach component codes to output
 * that is being sent to a Log implementation.  In Gemini 1.0,
 * the Log class handled multiple components by using instances to track the
 * individual components using the log.  This left application-level scope
 * to Log's static members.  In Gemini 1.1, in order to allow for multiple
 * applications to coexist, the ComponentLog is now used to provide the
 * component-level instancing.
 *   <p>
 * Generally, the ComponentLog constructor is not called directly, but rather
 * through calls to GeminiApplication.getLog(), which provides an appropriate
 * reference to the application's Log instance.
 *   <p>
 * See com.techempower.Log for the specifications of the debug levels.
 *
 * @see Log
 */
public class ComponentLog
{

  //
  // Member variables.
  //

  private final Log    internalLog;
  private final String componentCode;

  //
  // Member methods.
  //

  /**
   * Constructor.  Generally not invoked directly, but rather through
   * GeminiApplication.getLog().
   */
  public ComponentLog(Log log, String componentCode)
  {
    this.internalLog   = log;
    this.componentCode = componentCode;
  }

  /**
   * Logs the provided string as the provided log level.
   *
   * @param logString the text to be written.
   * @param debugLevel the priority level of this text.
   */
  public void log(String logString, int debugLevel)
  {
    this.internalLog.log(this.componentCode, logString, debugLevel);
  }

  /**
   * Logs the provided string at the NORMAL level.
   *
   * @param logString the text to be written.
   */
  public void log(String logString)
  {
    this.internalLog.log(this.componentCode, logString);
  }
  
  /**
   * Logs a throwable (exception) at the provided log level.
   *
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   * @param exception an exception
   */
  public void log(String debugString, int debugLevel, Throwable exception)
  {
    log(debugString + ' ' + ThrowableHelper.convertStackTraceToString(exception), 
      debugLevel);
  }

  /**
   * Logs an throwable (exception) at the NORMAL level.
   *
   * @param debugString the string to display.
   * @param exception an exception
   */
  public void log(String debugString, Throwable exception)
  {
    log(debugString + ' ' + ThrowableHelper.convertStackTraceToString(exception), 
      LogLevel.NORMAL);
  }

  /**
   * Asserts an expression.
   *
   * @param evalExpression the expression to evaluate.
   * @param debugString the text to be written.
   * @param debugLevel the priority level of this text.
   */
  public void assertion(boolean evalExpression, String debugString,
    int debugLevel)
  {
    this.internalLog.assertion(this.componentCode, evalExpression, debugString, debugLevel);
  }

  /**
   * Asserts an expression.
   *
   * @param evalExpression the expression to evaluate.
   * @param debugString the text to be written.
   */
  public void assertion(boolean evalExpression, String debugString)
  {
    this.internalLog.assertion(this.componentCode, evalExpression, debugString);
  }

  /**
   * Gets the reference to the application's Log object.
   */
  public Log getApplicationLog()
  {
    return this.internalLog;
  }
  
  /**
   * Return the component code.
   */
  public String getComponentCode()
  {
    return this.componentCode;
  }

}   // End ComponentLog.
