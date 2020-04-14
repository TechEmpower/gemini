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

package com.techempower.gemini.monitor;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.annotation.*;
import com.techempower.gemini.*;

/**
 * Represents a Dispatcher command that has been monitored/observed by the
 * Gemini Monitor component.  This object will contain statistical information
 * about the behavior of previous requests for the specific command in
 * question.
 *   <p>
 * For example, an object of this class would be instantiated by the Monitor
 * for the command "login" which would keep track of statistics for all
 * "login" requests.
 */
public class MonitoredCommand
{
  
  //
  // Constants.
  //
  
  public static final int PAST_INTERVALS = 100;

  //
  // Member variables.
  //
  
  private final GeminiMonitor           monitor;
  private final String                  command;
  private final AtomicInteger           requests = new AtomicInteger(0);
  private final AtomicInteger           currentLoad = new AtomicInteger(0);

  private MonitorSample           lastSample;
  private MonitorInterval[]       recentHistory;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public MonitoredCommand(GeminiMonitor monitor, String command)
  {
    this.monitor = monitor;
    this.command = command;
    this.recentHistory = new MonitorInterval[PAST_INTERVALS];
  }
  
  /**
   * Process a sample.
   */
  public void process(MonitorSample sample, BasicContext context)
  {
    this.lastSample = sample;
    this.requests.incrementAndGet();
    
    if (this.recentHistory[0] == null)
    {
      this.recentHistory[0] = new MonitorInterval(this.monitor.getPerfIntervalStart(), this.monitor.getPerfIntervalEnd());
    }
    
    this.recentHistory[0].process(sample, context);
  }
  
  /**
   * Adjust the current load.
   */
  public void adjustLoad(int adjustment)
  {
    this.currentLoad.addAndGet(adjustment);
  }
    
  /**
   * Gets the current load.
   */
  @JsonProperty("currentload")
  public int getCurrentLoad()
  {
    return this.currentLoad.get();
  }
  
  /**
   * Gets the last sample.
   */
  @JsonProperty("last")
  public MonitorSample getLastSample()
  {
    return this.lastSample;
  }
  
  /**
   * Gets the current Interval.
   */
  @JsonProperty("ci")
  public MonitorInterval getCurrentInterval()
  {
    return this.recentHistory[0];
  }
  
  /**
   * Gets the total number of requests processed.
   */
  @JsonProperty("count")
  public int getRequestCount()
  {
    return this.requests.get();
  }
  
  /**
   * Gets the command.
   */
  @JsonProperty("command")
  public String getCommand()
  {
    return this.command;
  }
  
  /**
   * Pushes the recent history further along the time-line.  Intervals that
   * do not have any recorded requests for this command will remain null as
   * a space-saving measure.
   */
  public void push()
  {
    // Move everything to the right one space.
    for (int i = this.recentHistory.length - 1; i > 0; i--)
    {
      this.recentHistory[i] = this.recentHistory[i - 1];
    }
    // Nullify the left-most/zero position
    this.recentHistory[0] = null;
  }

  /**
   * Gets the historical array.
   */
  @JsonIgnore
  public MonitorInterval[] getHistory()
  {
    return this.recentHistory;
  }
  
  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "MC [" + this.command + "]";
  }
  
  public static final Comparator<MonitoredCommand> BY_COMMAND = new Comparator<MonitoredCommand>() {
    @Override
    public int compare(MonitoredCommand o1, MonitoredCommand o2) {
      if  (  (o1 != null)
          && (o1.getCommand() != null)
          && (o2 != null)
          && (o2.getCommand() != null)
          )
       {
         return o1.getCommand().compareTo(o2.getCommand());
       }
       
       // Null values?  Cannot determine comparison.
       return 0;
    }
  };
}
