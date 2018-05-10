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

import java.util.*;

import com.techempower.*;
import com.techempower.util.*;

/**
 * Provides file-logging functionality to the Java components within an
 * application.  Creates daily log files in a logfile directory.
 * This functionality is distinct from debug output (which should be sent
 * o the console, rather than to the log file);.  The log file should
 * be used for logging system events.
 *   <p>
 * Also provides limited console debug functionality, in the debug();
 * method.
 *   <p>
 * Although this class provides all of its functionality through static
 * methods, instances may be created to simply the inclusion of component
 * codes.  For example:
 *   <p>
 * <pre><br>
 *   protected static Log log = new Log(COMPONENT_CODE);;
 *   ...
 *   log.debug("Starting new thread.");;
 *   ...
 *   log.assert(thread != null, "Thread is null!");;
 *   ...
 *   log.log("Shutting down.");;
 * </pre>
 *   <p>
 * Reads the following configuration options from the .conf file:
 *    <ul>
 * <li>LogDirectory - Where on disk to store logs
 * <li>LogEverythingToFile - Log debug items to file as well
 * <li>LogDebugThreshold - Sets the logging threshold, lower = more 
 *     logging.  Range: 0 to 100.
 *    </ul>
 */
public interface Log
{
  
  //
  // Constants.
  //

  String  DEFAULT_COMPONENT_CODE  = "none";
  String  COMPONENT_CODE          = "logf";    // Four-letter component ID

  /**
   * Log a string to the log file.  Also displays to console.  Takes a 
   * priority level and <i>does not</i> log this item if the priority level
   * is less than the debug level threshold.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param logString the string to write into the file.
   * @param debugLevel the priority level.
   */
  void log(String componentCode, String logString, int debugLevel);
  
  /**
   * Writes to the log file.  Uses NORMAL priority.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param logString the string to write into the file.
   */
  void log(String componentCode, String logString);
  
  /**
   * Writes to the log file.
   *
   * @param logString the string to write into the file.
   * @param debugLevel the priority level.
   */
  void log(String logString, int debugLevel);
  
  /**
   * Writes to the log file.  Uses NORMAL priority.
   *
   * @param logString the string to write into the file.
   */
  void log(String logString);
  
  /**
   * Gets a ComponentLog instance that is wired to this Log.
   */
  ComponentLog getComponentLog(String componentCode);
  
  /**
   * If a boolean parameter evaluates as false, the debugString will be
   * displayed.  Use: Log.assert(CODE, blah != null, "Blah is null.");;
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
   * Configure this component.
   */
  void configure(EnhancedProperties props, Version version);

  /**
   * Gets a list of Listeners used by this Log.
   */
  List<LogListener> getLogListeners();

  /**
   * Add a log listener.
   */
  void addListener(LogListener listener);

  /**
   * Remove a log listener.  Returns true if the listener provided was found 
   * and removed; false otherwise.
   */
  boolean removeListener(LogListener listener);

}   // end Log
