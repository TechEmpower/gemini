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

package com.techempower.gemini.monitor.cpupercentage;

import static com.techempower.util.UtilityConstants.*;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.monitor.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * A simple PercentageEvaluator that detects if any given thread has utilized
 * a great portion of CPU time for several seconds in a row.  The percentage
 * threshold and number of seconds are configurable in the constructor.
 */
public class   BasicSpikeDetector
    implements PercentageEvaluator,
               Configurable
{
  
  //
  // Constants
  //
  
  public static final String CONFIGURATION_PREFIX = "BasicSpikeDetector.";
  public static final int DEFAULT_INTERVAL_BEFORE_EXCEPTIONAL = 5;
  public static final int DEFAULT_PERCENTAGE_THRESHOLD = 90;
  
  //
  // Member variables.
  //
  
  private final ComponentLog      log;
  private final Set<String>       ignored;
  private final Set<String>       ignoredStackItems;
  private final List<List<Long>>  overThreshold;
  
  private final int               intervalsBeforeExceptional;
  private final int               percentageThreshold;
  private boolean                 enabled = true;
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public BasicSpikeDetector(GeminiApplication application, 
      int intervalsBeforeExceptional, int percentageThreshold)
  {
    this.log = application.getLog("Spke");
    this.intervalsBeforeExceptional = intervalsBeforeExceptional;
    this.percentageThreshold = percentageThreshold;
    this.ignored = new HashSet<>();
    this.ignoredStackItems = new HashSet<>();
  
    // We store one more sample than the number of seconds to determine an
    // exceptional state.  This is because we will only raise an exceptional
    // state when the utilization history looks like 011111 (0 = lower than
    // threshold; 1 = over threshold).  This prevents repeatedly raising an
    // exceptional state for the same thread at each interval.
    this.overThreshold = new ArrayList<>(intervalsBeforeExceptional + 1);
    
    // Fill up the array.
    for (int i = 0; i < (intervalsBeforeExceptional + 1); i++)
    {
      this.overThreshold.add(new ArrayList<Long>(0));
    }
    
    application.getConfigurator().addConfigurable(this);
  }
    
  
  /**
   * Simpler constructor that uses the default 5-second/90% threshold.
   */
  public BasicSpikeDetector(GeminiApplication application)
  {
    this(application, DEFAULT_INTERVAL_BEFORE_EXCEPTIONAL,
        DEFAULT_PERCENTAGE_THRESHOLD);
  }
  
  @Override
  public String isExceptional(PercentageInterval interval, GeminiMonitor monitor)
  {
    // If we're not enabled, just ignore everything.
    if (!this.enabled)
    {
      return null;
    }

    StringBuilder toReturn = null;

    // Remove the first entry from the list, pushing the rest down.
    this.overThreshold.remove(0);
    
    // Count how many threads are over the CPU utilization threshold.
    int count = 0;
    List<PercentageSample> samples = interval.getSamples();
    for (PercentageSample sample : samples)
    {
      if (sample.getUsage() > this.percentageThreshold)
      {
        count++;
      }
    }
    
    // Build the most recent entry for the list.
    List<Long> entry = new ArrayList<>(count);
    for (PercentageSample sample : samples)
    {
      if (sample.getUsage() > this.percentageThreshold)
      {
        // Don't process threads that we've been told to ignore.
        if (!this.ignored.contains(sample.getName()))
        {
          // Add this thread to the most recent entry.
          entry.add(sample.getId());
          
          // Has this thread exceeded the threshold for the requisite number
          // of intervals to be exceptional?  Note that the earliest interval
          // should show this thread at below the threshold.  This is done to
          // prevent repeatedly triggering exceptional status for the same
          // thread/event.
          if (isThreadExceptional(sample.getId()))
          {
            String stackTrace = capture(sample.getId());
            
            // Check the list of ignored stack items.  If the stack trace
            // contains any of the ignored items, we'll ignore this thread
            // exception.  Otherwise, let's proceed.
            if (!containsIgnoredStackItems(stackTrace))
            {
              this.log.log("Exceptional thread detected: " + sample.getId());
              if (toReturn == null)
              {
                toReturn = new StringBuilder();
              }
              toReturn.append(stackTrace);
              toReturn.append(CRLF);
            }
          }
        }
      }   
    }
    
    // Add the newly constructed entry to the list.
    this.overThreshold.add(entry);
    
    // Do we have any exceptional threads?
    if (toReturn == null)
    {
      return null;
    }
    else
    {
      return toReturn.toString();
    }
  }
  
  /**
   * Returns true if the provided thread ID is found in the overThreshold
   * List in a 01111... pattern.  See comments elsewhere explaining that
   * leading zero. 
   */
  protected boolean isThreadExceptional(long id)
  {
    List<Long> list = this.overThreshold.get(0);
    
    // Null?
    if (list == null)
    {
      return false;
    }
    
    // First interval contains the ID, so return false.
    if (list.contains(id))
    {
      return false;
    }
    else
    {
      // Check the remaining intervals.
      for (int i = 1; i < this.overThreshold.size(); i++)
      {
        list = this.overThreshold.get(i);
        if (  (list == null)
           || (!list.contains(id))
           )
        {
          return false;
        }
      }
    }
    
    return true;
  }
  
  /**
   * Captures some trace information about a thread for an exceptional state.
   */
  protected String capture(long threadId)
  {
    StringBuilder toReturn = new StringBuilder();
    toReturn.append("Thread ")
            .append(threadId)
            .append(" over ")
            .append(this.percentageThreshold)
            .append("% CPU usage for ")
            .append(this.intervalsBeforeExceptional)
            .append(" intervals (seconds)")
            .append(CRLF);

    Thread thread = ThreadHelper.getThread(threadId);
    if (thread != null)
    {
      toReturn.append("  ")
              .append(thread.getName())
              .append(CRLF);
      toReturn.append("  Stack trace:")
              .append(CRLF);
      StackTraceElement[] trace = thread.getStackTrace();
      for (StackTraceElement aTrace : trace)
      {
        toReturn.append("  ")
            .append(aTrace.toString())
            .append(CRLF);
      }
    }
    else
    {
      toReturn.append("  Thread ")
              .append(threadId)
              .append(" not available for capture.")
              .append(CRLF);
    }
    
    return toReturn.toString();
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    EnhancedProperties.Focus focus = props.focus(CONFIGURATION_PREFIX);
    
    // In case you just want to turn this off.
    this.enabled = focus.getBoolean("Enabled", true);

    // Read in ignored thread names.
    int index = 1;
    String toIgnore;
    do
    {
      toIgnore = focus.get("Ignore" + index);
      if (toIgnore != null)
      {
        addIgnoredThread(toIgnore);
      }
      index++;
    } while (toIgnore != null);
    
    if (this.ignored.size() > 0)
    {
      this.log.log(this.ignored.size() + " thread(s) ignored for alerts.");
    }
    
    // Read in ignored stack trace items.
    index = 1;
    do
    {
      toIgnore = focus.get("IgnoreStack" + index);
      if (toIgnore != null)
      {
        addIgnoredStackItem(toIgnore);
      }
      index++;
    } while (toIgnore != null);
    
    if (this.ignoredStackItems.size() > 0)
    {
      this.log.log(this.ignoredStackItems.size() + " stack item(s) ignored for alerts.");
    }
  }
  
  /**
   * Adds a thread name to ignore for alerting purposes.
   */
  public void addIgnoredThread(String threadName)
  {
    this.ignored.add(threadName);
  }
  
  /**
   * Adds a String which indicates a stack trace should be ignored.  For
   * example, to ignore any threads whose stack trace currently includes
   * "SnowballProgram" in any method name, add "SnowballProgram" using this
   * method (or via the configuration file's BasicSpikeDetector.IgnoreStack
   * items. 
   */
  public void addIgnoredStackItem(String stackItem)
  {
    this.ignoredStackItems.add(stackItem);
  }
  
  /**
   * Should the provided stack trace be ignored (that is, does the stack trace
   * contain any of the to-be-ignored stack item strings?
   */
  protected boolean containsIgnoredStackItems(String stackTrace)
  {
    for (String ignoredItem : this.ignoredStackItems)
    {
      if (stackTrace.contains(ignoredItem))
      {
        return true;
      }
    }
    
    return false;
  }

  @Override
  public String getEvaluatorName()
  {
    return "SpikeDetector";
  }

}
