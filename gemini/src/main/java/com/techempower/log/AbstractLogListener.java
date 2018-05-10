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
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * AbstractLogListener provides overload methods for debug, log, and assert.
 * These are used by Log and LogListener implementations, so that they do
 * not have to implement the many varied ways to log information.
 *
 * @see Log
 * @see LogListener
 */
public abstract class AbstractLogListener 
  implements LogListener
{
  //
  // Members.
  //
  
  private final TechEmpowerApplication application;
  private int                    debugThreshold  = MINIMUM;
  private long                   endOfDay        = DateHelper.getEndOfDay().getTimeInMillis();
  private long                   nextSecond      = 0L;
  private String                 fullTimestamp   = null;
  private String                 briefTimestamp  = null;

  /**
   * Only create the Calendar instance once, and then call setTimeInMillis()
   * as necessary to update it. Recreating the instance uses a synchronized
   * Hashtable class variable in Calendar, causing an unnecessary lock.
   */
  private final Calendar         cal             = DateHelper.getCalendarInstance();
  
  /**
   * Constructor for AbstractLogListener.
   */
  public AbstractLogListener(TechEmpowerApplication application)
  {
    this.application = application;
  }
  
  /**
   * Gets the Application reference.
   */
  protected TechEmpowerApplication getApplication()
  {
    return this.application;
  }
    
  /**
   * The actual debug method.  Just calls log()
   */
  @Override
  public void debug(String componentCode, String debugString, int debugLevel)
  {
    log( componentCode, debugString, debugLevel );
  }

  /**
   * Debug.  Uses default component code.
   *
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   */
  @Override
  public void debug(String debugString, int debugLevel)
  {
    log(Log.DEFAULT_COMPONENT_CODE, debugString, debugLevel);
  }

  /**
   * Debug.  Uses default component code and takes an exception.
   *
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   * @param exc an exception
   */
  @Override
  public void debug(String debugString, int debugLevel, Exception exc)
  {
    log(Log.DEFAULT_COMPONENT_CODE, 
      debugString + ThrowableHelper.convertStackTraceToString(exc), 
      debugLevel);
  }

  /**
   * Debug.  Uses NORMAL priority and default component code.
   *
   * @param debugString the string to display.
   * @param exc an exception
   */
  @Override
  public void debug(String debugString, Exception exc)
  {
    log(Log.DEFAULT_COMPONENT_CODE, 
      debugString + ThrowableHelper.convertStackTraceToString(exc), 
      NORMAL);
  }

  /**
   * Debug.  Uses NORMAL priority.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param debugString the string to display.
   */
  @Override
  public void debug(String componentCode, String debugString)
  {
    log(componentCode, debugString, NORMAL);
  }

  /**
   * Debug.  Uses NORMAL priority and default component code.
   *
   * @param debugString the string to display.
   */
  @Override
  public void debug(String debugString)
  {
    log(Log.DEFAULT_COMPONENT_CODE, debugString, NORMAL);
  }
  
  /**
   * If a boolean parameter evaluates as false, the debugString will be
   * written to the console.  
   * 
   * Do not call this method directly; it is for a Log class to call.
   * 
   * Use: Log.assert(CODE, blah != null, "Blah is null.");
   *
   * @param componentCode a four-letter component code of the caller.
   * @param eval the boolean expression to evaluate.
   * @param debugString the string to display.
   * @param debugLevel the priority level.
   */
  @Override
  public void assertion(String componentCode, boolean eval,
    String debugString, int debugLevel)
  {
    // Is the priority of the item being debugged higher or equal to the
    // debugging threshold set for this Log?  If not, ignore this item.
    if (  (debugLevel >= getDebugThreshold())
       && (!eval)
       )
    {
      log(componentCode, debugString);
    }
  }

  /**
   * Provides the same functionality as another method of the same name
   * does not require the component code.  Uses NORMAL priority.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param eval the boolean expression to evaluate.
   * @param logString the string to display.
   */
  @Override
  public void assertion(String componentCode, boolean eval, String logString)
  {
    assertion(componentCode, eval, logString, NORMAL);
  }

  /**
   * Provides the same functionality as another method of the same name
   * does not require the component code.
   *
   * @param eval the boolean expression to evaluate.
   * @param logString the string to display.
   * @param debugLevel the priority level.
   */
  @Override
  public void assertion(boolean eval, String logString, int debugLevel)
  {
    assertion(Log.DEFAULT_COMPONENT_CODE, eval, logString, debugLevel);
  }


  /**
   * Provides the same functionality as another method of the same name
   * does not require the component code.  Uses NORMAL priority.
   *
   * @param eval the boolean expression to evaluate.
   * @param logString the string to display.
   */
  @Override
  public void assertion(boolean eval, String logString)
  {
    assertion(Log.DEFAULT_COMPONENT_CODE, eval, logString, NORMAL);
  }
  
  /**
   * Log.  Uses NORMAL priority.
   *
   * @param componentCode a four-letter component code of the caller.
   * @param logString the string to write into the file.
   */
  @Override
  public void log(String componentCode, String logString)
  {
    log(componentCode, logString, NORMAL);
  }

  /**
   * The actual log method.  Override this for each log listener
   */
  @Override
  public void log(String componentCode, String logString, int debugLevel)
  {
  }

  /**
   * Log.  Uses default component code.
   *
   * @param logString the string to write into the file.
   * @param debugLevel the priority level.
   */
  @Override
  public void log(String logString, int debugLevel)
  {
    log(Log.DEFAULT_COMPONENT_CODE, logString, debugLevel);
  }

  /**
   * Log.  Uses NORMAL priority and default component code.
   *
   * @param logString the string to write into the file.
   */
  @Override
  public void log(String logString)
  {
    log(Log.DEFAULT_COMPONENT_CODE, logString, NORMAL);
  }

  //
  // utility methods
  //
  
  /**
   * Generate new Timestamp strings if the time has progressed by a second.
   */
  protected void computeTimestamps()
  {
    long now = System.currentTimeMillis();
    
    // We'll treat the generation of the timestamps as an idempotent operation
    // meaning that it can occur in parallel within many threads executing
    // at the same moment, but that's probably no worse than any sort of
    // synchronization or locking alternative.
    if (now > this.nextSecond)
    {
      // Trim off milliseconds.
      long millis = now % UtilityConstants.SECOND;
      this.nextSecond = now - millis 
                            + UtilityConstants.SECOND;
      this.cal.setTimeInMillis(now);
      this.fullTimestamp = generateFullTimestamp();
      this.briefTimestamp = generateBriefTimestamp();
    }
  }
  
  /**
   * Gets the current timestamp String.
   */
  protected String getFullTimestamp()
  {
    return this.fullTimestamp;
  }
  
  /**
   * Gets the current brief timestamp String.
   */
  protected String getBriefTimestamp()
  {
    return this.briefTimestamp;
  }
  
  /**
   * Generate a timestamp.
   */
  protected String generateFullTimestamp()
  {
    StringBuilder buffer = new StringBuilder(20);

    buffer.append(this.cal.get(Calendar.YEAR));
    buffer.append('-');
    buffer.append(StringHelper.padZero(this.cal.get(Calendar.MONTH) + 1, 2));
    buffer.append('-');
    buffer.append(StringHelper.padZero(this.cal.get(Calendar.DAY_OF_MONTH), 2));
    buffer.append(' ');
    buffer.append(StringHelper.padZero(this.cal.get(Calendar.HOUR_OF_DAY), 2));
    buffer.append(':');
    buffer.append(StringHelper.padZero(this.cal.get(Calendar.MINUTE), 2));
    buffer.append(':');
    buffer.append(StringHelper.padZero(this.cal.get(Calendar.SECOND), 2));

    return buffer.toString();
  }

  /**
   * Generate a brief timestamp.
   */
  protected String generateBriefTimestamp()
  {
    StringBuilder buffer = new StringBuilder(8);

    buffer.append(StringHelper.padZero(this.cal.get(Calendar.HOUR_OF_DAY), 2));
    buffer.append(':');
    buffer.append(StringHelper.padZero(this.cal.get(Calendar.MINUTE), 2));
    buffer.append(':');
    buffer.append(StringHelper.padZero(this.cal.get(Calendar.SECOND), 2));

    return buffer.toString();
  }


  /**
   * Set the debug threshold for this log.
   */
  @Override
  public void setDebugThreshold(int d)
  {
    this.debugThreshold = d;
  }
  
  /**
   * Return the debug threshold for this log.
   */
  @Override
  public int getDebugThreshold()
  {
    return this.debugThreshold;
  }

  /**
   * Determines if we have entered a new day (indicating it's time for a new
   * log file, assuming we're writing log files.
   */
  public boolean pastEndOfDay()
  {
    if (System.currentTimeMillis() > this.endOfDay)
    {
      this.endOfDay = DateHelper.getEndOfDay().getTimeInMillis();
      return true;
    }
    return false;
  }
  
}
// End AbstractLogListener
