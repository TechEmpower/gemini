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

import java.lang.management.*;
import java.util.*;

import com.techempower.gemini.monitor.*;
import com.techempower.log.*;
import com.techempower.thread.*;
import com.techempower.util.*;

/**
 * The main class (and thread) for CPU usage percentage monitoring, an 
 * optional Gemini Monitor sub-component.
 */
public class PercentageMonitorThread
  extends    EndableThread
{
  
  //
  // Constants.
  
  private static final long SLEEP_PERIOD = 15 * UtilityConstants.SECOND;
  
  //
  
  //
  // Variables.
  //
  
  private final GeminiMonitor monitor;
  private final ComponentLog  log;
  private final ThreadMXBean  jmx;

  private long thisInterval = 0L;
  private long nextInterval = 0L;
  
  private PercentageInterval current;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public PercentageMonitorThread(GeminiMonitor monitor)
  {
    super("Gemini Monitor CPU Percentage (" + monitor.getApplication().getVersion().getProductName() + ")", (int)UtilityConstants.SECOND);
    
    this.monitor = monitor;
    this.log = monitor.getApplication().getLog("cpup");
    this.jmx = ManagementFactory.getThreadMXBean();
  }
  
  /**
   * Standard run method.
   */
  @Override
  public void run()
  {
    log.log("Gemini Monitor CPU percentage thread started.");
    while (checkPause())
    {
      // Don't do anything if CPU percentage monitoring is disabled.
      if (monitor.isCpuPercentageEnabled())
      {
        synchronized (this)
        {
          pushInterval();
          
          // Evaluate the interval.
          final String evaluation = monitor.evaluatePercentage(current);
          if (evaluation != null)
          {
            current.setEvaluation(evaluation);
          }
          
          // Determine the time for the next interval.
          nextInterval = nextInterval + UtilityConstants.SECOND;
          if (thisInterval > nextInterval)
          {
            nextInterval = thisInterval + UtilityConstants.SECOND;
          }
        }
        
        // Sleep until the next interval.
        simpleSleep((int)(nextInterval - thisInterval));
      }
      // If we're not enabled, sleep for 15 seconds.
      else
      {
        simpleSleep(SLEEP_PERIOD);
      }
    }
    log.log("Gemini Monitor CPU percentage thread stopped.");
  }
  
  /**
   * Gets a List of PercentageSamples from the current interval.
   */
  public synchronized List<PercentageSample> getCurrent()
  {
    return current.getSamples();
  }
  
  /**
   * Pushes to the next interval.
   */
  public void pushInterval()
  {
    final long now = System.currentTimeMillis();
    long delta = UtilityConstants.SECOND;
    if (thisInterval > 0)
    {
      delta = now - thisInterval;
    }
    thisInterval = now;
    
    final Thread[] threads = monitor.getThreadArray();
    final PercentageInterval newInterval = new PercentageInterval(
        threads, current, jmx, delta);
    current = newInterval;
  }

}
