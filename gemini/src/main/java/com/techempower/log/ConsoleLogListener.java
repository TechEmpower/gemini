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

import com.techempower.*;

/**
 * A basic implementation of LogListener.  This implementation writes each log
 * entry to the console.  
 *   <p>
 * BasicLog starts with three LogListeners; FileLogListener, 
 * GraphicLogListener, and ConsoleLogListener (this one).  
 *   <p>
 * There is no need to create and register an instance of this class with
 * BasicLog unless you want ANOTHER ConsoleLogListener, which is very
 * unlikely.
 */
public class ConsoleLogListener
     extends AbstractLogListener
{

  /**
   * Constructor.
   *
   * LogListener objects are typically constructed only by BasicLog, but can
   * be constructed elsewhere for specific needs.
   *
   * @param application a reference to the application using the Log.
   * @param threshold The minimum log level that this listener will process.
   */
  public ConsoleLogListener(TechEmpowerApplication application, 
                            int threshold)
  {
    super(application);
    setDebugThreshold(threshold);
  }

  /**
   * Returns the name of this listener.
   */
  @Override
  public String getName()
  {
    return "Console Log";
  }

  /**
   * Display debug information to the console. 
   *   <p>
   * Takes a priority level and <i>does not</i> display this item if the 
   * priority level is less than the debug level threshold.
   * 
   * Do not call this method directly; it is for a Log class to call.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   */
  @Override
  public void log(String componentCode, String debugString, 
    int debugLevel)
  {
    // Is the priority of the item being debugged higher or equal to the
    // debugging threshold set for this Log?  If not, ignore this item.
    if (debugLevel >= getDebugThreshold())
    {
      debugToConsole(componentCode, debugString, debugLevel);
    }
  }

  //
  // Protected member methods.
  //
  
  /**
   * Displays debug information to the console.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param debugString the string to display.
   */
  protected void debugToConsole(String componentCode, String debugString)
  {
    debugToConsole(componentCode, debugString, NORMAL);
  }

  /**
   * Displays debug information to the console.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param debugString the string to display.
   * @param debugLevel -- debug level of message
   */
  protected void debugToConsole(String componentCode, String debugString, int debugLevel)
  {
    // Update the time in the calendar.
    computeTimestamps();

    StringBuilder buffer = new StringBuilder(120);
    buffer.append(getApplication().getVersion().getProductCode());
    buffer.append(' ');
    buffer.append(getBriefTimestamp());
    buffer.append(' ');
    buffer.append(componentCode);
    buffer.append(": ");
    buffer.append(debugString);

    System.out.println(buffer.toString());
  }

}  // End ConsoleLogListener.
