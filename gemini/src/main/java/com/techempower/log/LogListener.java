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


/**
 * Provides debug and logging functionality to the BasicLog component.
 * Each LogListener receives log, debug, and assertion methods from BasicLog.  
 * What happens to these messages is up to the LogListener 
 * implementation; obvious choices are writing to a file, to the console, etc.
 * <p>
 * To create a LogListener, implement this class, and extend 
 * AbstractLogListener.  
 * <p>
 * The log, assert, and debug methods of LogListeners are not called directly;
 * instead, LogListeners are installed into an implementation of Log, 
 * (such as BasicLog)  Other components then call BasicLog, which in turn 
 * calls each of the installed listeners.
 * <p> 
 * See Log and BasicLog for more details.
 */
public interface LogListener
  extends LogLevel
{

  /**
   * Gets the name of this listener.
   */
  String getName();
  
  /**
   * Log a string to the log listener. Takes a 
   * priority level and <i>does not</i> log this item if the priority level
   * is less than the debug level threshold.
   * This is effectively the same as debug; an alias for backwards 
   * compatibility.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param logString the string to write into the file.
   * @param debugLevel the priority level.
   */
  void log(String componentCode, String logString, int debugLevel);

  /**
   * Alias for log(String componentCode, String logString, int debugLevel);
   * Uses NORMAL priority.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param logString the string to write into the file.
   */
  void log(String componentCode, String logString);
  
  /**
   * Alias for log(String componentCode, String logString, int debugLevel);
   * Uses default component code.
   *
   * @param logString the string to write into the file.
   * @param debugLevel the priority level.
   */
  void log(String logString, int debugLevel);
  
  /**
   * Alias for log(String componentCode, String logString, int debugLevel);
   * Uses NORMAL priority and default component code.
   * 
   * @param logString the string to write into the file.
   */
  void log(String logString);
  
  /**
   * Log a string to the log listener. Takes a 
   * priority level and <i>does not</i> log this item if the priority level
   * is less than the debug level threshold.
   * This is effectively the same as log; an alias for backwards 
   * compatibility.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   */
  void debug(String componentCode, String debugString, 
    int debugLevel);
  
  /**
   * Alias for debug(String componentCode, String logString, int debugLevel);
   * Uses default component code.
   *
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   */
  void debug(String debugString, int debugLevel);
  
  /**
   * Alias for debug(String componentCode, String logString, int debugLevel);
   * Uses default component code and takes in an exception.
   *
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   * @param exc an exception
   */
  void debug(String debugString, int debugLevel, Exception exc);
  
  /**
   * Alias for debug(String componentCode, String logString, int debugLevel);
   * Uses default debug level and component code, and takes in an exception.
   *
   * @param debugString the string to display.
   * @param exc an exception
   */
  void debug(String debugString, Exception exc);
  
  /**
   * Alias for debug(String componentCode, String logString, int debugLevel);
   * Uses default debug level.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param debugString the string to display.
   */
  void debug(String componentCode, String debugString);
  
  /**
   * Alias for debug(String componentCode, String logString, int debugLevel);
   * Uses default debug level and component code.
   *
   * @param debugString the string to display.
   */
  void debug(String debugString);
  
  /**
   * Log a string to the log listener. 
   * If a boolean parameter evaluates as false, the debugString will be
   * displayed.  Use: Log.assert(CODE, blah != null, "Blah is null.");;
   * 
   * Takes a  priority level and <i>does not</i> log this item if the 
   * priority level is less than the debug level threshold.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param eval the boolean expression to evaluate.
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   */
  void assertion(String componentCode, boolean eval,
    String debugString, int debugLevel);
  
  /**
   * Provides the same functionality as another method of the same name
   * does not require the component code.  Uses NORMAL priority.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param eval the boolean expression to evaluate.
   * @param logString the string to display.
   */
  void assertion(String componentCode, boolean eval, String logString);
  
  /**
   * Provides the same functionality as another method of the same name
   * does not require the component code.
   *
   * @param eval the boolean expression to evaluate.
   * @param logString the string to display.
   * @param debugLevel the priority level.
   */
  void assertion(boolean eval, String logString, int debugLevel);
  
  /**
   * Provides the same functionality as another method of the same name
   * does not require the component code.  Uses NORMAL priority.
   *
   * @param eval the boolean expression to evaluate.
   * @param logString the string to display.
   */
  void assertion(boolean eval, String logString);

  /**
   * Sets the debug threshold for this LogListener.
   */
  void setDebugThreshold(int debugThreshold);
  
  /**
   * Gets the debug threshold for this LogListener.
   */
  int getDebugThreshold();

  /**
   * If true, a severity prefix will be placed at the very front of each logged
   * message.
   */
  boolean isSeverityPrefixEnabled();

  /**
   * Sets whether or not a severity prefix will be included. The prefix will be
   * placed at the very front of each logged message.
   *
   * @param severityPrefixEnabled whether or not the severity prefix will be
   *                              included
   */
  void setSeverityPrefixEnabled(boolean severityPrefixEnabled);

}  // end LogListener.
