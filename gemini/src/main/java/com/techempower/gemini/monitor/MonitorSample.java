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

import java.lang.management.*;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.techempower.helper.*;

/**
 * A simple data structure to track metrics collected during the scope of
 * fulfilling a request within a Gemini application.  For example, the number
 * of Database queries executed, the duration of time elapsed prior to
 * directing to a JSP, and the duration of the JSP rendering.
 */
public class MonitorSample
{

  //
  // Constants.
  //
  
  public static final long NANOS_PER_MILLI = 1000000L;
  
  //
  // Member variables
  //
  
  private final long startTime;
  private final long threadID;

  private long    queryStart;
  private long    specialStart;
  private long    cpuTimeAtStart;
  private int     dispatches = 0;
  private int     queries    = 0;
  private int     logicTime  = 0;
  private int     renderTime = 0;
  private int     queryExceptions = 0;
  private int     queryTime  = 0;
  private int     specialTime = 0;
  private int     totalCpuTime = 0;
  private int     requestLoad = 1;
  private long    requestNumber = 0L;
  private String  command;
  private String  requestSignature;
  
  private static final ThreadLocal<MonitorSample> CURRENT = new ThreadLocal<MonitorSample>()
  {
    @Override
    protected MonitorSample initialValue()
    {
      return new MonitorSample();
    }
  };
  
  //
  // Static methods.
  //
  
  /**
   * Gets the MonitorSample for the current thread of execution.
   */
  public static MonitorSample get()
  {
    return CURRENT.get();
  }
  
  /**
   * Marks the MonitorSample for the current thread of execution as complete.
   */
  public static void complete()
  {
    CURRENT.remove();
  }
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.  Assumes present time is the start time.
   */
  public MonitorSample()
  {
    this.startTime = System.currentTimeMillis();
    this.threadID = Thread.currentThread().getId();
  }
  
  /**
   * Sets the CPU time at start.  The CPU time provided should be in 
   * nanoseconds, as provided by JMX.
   */
  public void setCpuTimeAtStart(long cpuTime)
  {
    this.cpuTimeAtStart = cpuTime;
  }
  
  /**
   * Captures the difference in CPU time at the end of a request versus the
   * start of the request.  The current CPU time should be provided in
   * nanoseconds.  The resulting total CPU time is in milliseconds
   * (we divide by 1,000,000 here).
   */
  public void setCpuTimeAtEnd(long cpuTime)
  {
    if (this.cpuTimeAtStart > 0L)
    {
      // Only bother with the divide operation if we've got >= 1ms of time 
      // sampled.
      if (cpuTime - this.cpuTimeAtStart > NANOS_PER_MILLI)
      {
        this.totalCpuTime = (int)((cpuTime - this.cpuTimeAtStart) / NANOS_PER_MILLI);
      }
      else
      {
        this.totalCpuTime = 0;
      }
    }
  }
  
  /**
   * Gets the total CPU time.
   */
  @JsonProperty("cpu")
  public int getTotalCpuTime()
  {
    return this.totalCpuTime;
  }
  
  /**
   * Gets the total CPU time in progress.
   */
  @JsonIgnore
  public int getTotalCpuTimeInProgress()
  {
    long current = ManagementFactory.getThreadMXBean().getThreadCpuTime(this.threadID);
    return (int)((current - this.cpuTimeAtStart) / NANOS_PER_MILLI);
  }
  
  /**
   * Tracks the start of a "special" (application-specific) operation.
   */
  public void specialStarting()
  {
    this.specialStart = System.currentTimeMillis();
  }
  
  /**
   * Tracks the completion of a "special" (application-specific) operation.
   */
  public void specialCompleting()
  {
    if (this.specialStart > 0L)
    {
      this.specialTime += (int)(System.currentTimeMillis() - this.specialStart);
      this.specialStart = 0L;
    }
  }
  
  /**
   * Tracks a query.
   */
  public void queryStarting()
  {
    this.queries++;
    this.queryStart = System.currentTimeMillis();
  }
  
  /**
   * Gets the number of queries.
   */
  @JsonProperty("queries")
  public int getQueries()
  {
    return this.queries;
  }
  
  /**
   * Tracks a query completing.
   */
  public void queryCompleting()
  {
    if (this.queryStart > 0L)
    {
      this.queryTime += (int)(System.currentTimeMillis() - this.queryStart);
      this.queryStart = 0L;
    }
  }
  
  /**
   * Gets the total query time.
   */
  @JsonProperty("querytime")
  public int getQueryTime()
  {
    return this.queryTime;
  }
  
  /**
   * Gets the total special-operation time.
   */
  @JsonProperty("special")
  public int getSpecialTime()
  {
    return this.specialTime;
  }
  
  /**
   * Tracks a query error/exception.
   */
  public void queryException()
  {
    this.queryExceptions++;
  }
  
  /**
   * Gets the count of query exceptions.
   */
  @JsonProperty("queryexc")
  public int getQueryExceptions()
  {
    return this.queryExceptions;
  }
  
  /**
   * Gets the request number for this Sample.
   */
  @JsonIgnore
  public long getRequestNumber()
  {
    return this.requestNumber;
  }
  
  /**
   * Sets the request number.
   */
  public void setRequestNumber(long reqNum)
  {
    this.requestNumber = reqNum;
  }
  
  /**
   * @return the ID of the thread executing this request. 
   */
  @JsonIgnore
  public long getThreadID()
  {
    return this.threadID;
  }

  /**
   * Tracks a dispatch.  Return true if no command was previously captured
   * (that is, this is the first dispatch for the current request).
   */
  public boolean dispatchStarting(String commandString)
  {
    this.dispatches++;
    boolean first = (this.command == null);
    this.command = commandString;
    return first;
  }
  
  /**
   * Gets the number of dispatches.
   */
  @JsonProperty("disp")
  public int getDispatches()
  {
    return this.dispatches;
  }
  
  /**
   * Gets the command.
   */
  @JsonIgnore
  public String getDispatchCommand()
  {
    return this.command;
  }
  
  /**
   * Tracks the completion of logic execution (immediately prior to loading
   * a JSP)
   */
  public void logicComplete()
  {
    if (this.logicTime == 0)
    {
      this.logicTime = (int)(System.currentTimeMillis() - this.startTime);
    }
  }
  
  /**
   * Gets the logic time.
   */
  @JsonProperty("logic")
  public int getLogicTime()
  {
    return this.logicTime;
  }
  
  /**
   * Tracks the completion of a JSP execution (immediately following the
   * JSP rendering).
   */
  public void renderComplete()
  {
    if (this.renderTime == 0)
    {
      this.renderTime = (int)(System.currentTimeMillis() - this.startTime - this.logicTime);
    }
  }
  
  /**
   * Gets the render time.
   */
  @JsonProperty("render")
  public int getRenderTime()
  {
    return this.renderTime;
  }
  
  /**
   * Gets the start time of the request.
   */
  @JsonProperty("time")
  public long getStartTime()
  {
    return this.startTime;
  }
  
  /**
   * Gets the total time of the request (logic + render).
   */
  @JsonProperty("total")
  public int getTotalTime()
  {
    return getLogicTime() + getRenderTime();
  }
  
  /**
   * Sets the request signature.
   */
  public void setRequestSignature(String requestSignature)
  {
    this.requestSignature = requestSignature;
  }
  
  /**
   * Gets the request signature.
   */
  @JsonProperty("sig")
  public String getRequestSignature()
  {
    return this.requestSignature;
  }
  
  //
  // Special "in-progress" get methods.  These should be used only for the
  // special case of showing "current requests."  For any past request, use
  // the standard get methods.
  //
  
  /**
   * Gets an in-progress snapshot of query time.
   */
  @JsonIgnore
  public int getQueryTimeInProgress()
  {
    if (this.queryStart > 0L)
    {
      return this.queryTime + (int)(System.currentTimeMillis() - this.queryStart);
    }
    else
    {
      return this.queryTime;
    }
  }
  
  /**
   * Gets an in-progress snapshot of special time.
   */
  @JsonIgnore
  public int getSpecialTimeInProgress()
  {
    if (this.specialStart > 0L)
    {
      return this.specialTime + (int)(System.currentTimeMillis() - this.specialStart);
    }
    else
    {
      return this.specialTime;
    }
  }
  
  /**
   * Gets an in-progress snapshot of logic time.
   */
  @JsonIgnore
  public int getLogicTimeInProgress()
  {
    if (this.logicTime == 0)
    {
      return (int)(System.currentTimeMillis() - this.startTime);
    }
    else
    {
      return this.logicTime;
    }
  }
  
  /**
   * Gets an in-progress snapshot of render time.
   */
  @JsonIgnore
  public int getRenderTimeInProgress()
  {
    if (this.renderTime == 0)
    {
      if (this.logicTime == 0)
      {
        return 0;
      }
      else
      {
        return (int)(System.currentTimeMillis() - this.startTime - this.logicTime);
      }
    }
    else
    {
      return this.renderTime;
    }
  }
  
  /**
   * Gets an in-progress snapshot of total time.
   */
  @JsonIgnore
  public int getTotalTimeInProgress()
  {
    return getLogicTimeInProgress() + getRenderTimeInProgress();
  }

  /**
   * Get the request load, which is typically 1.  The load may be zero if the
   * request's command was not tracked when the request arrived (but will
   * be tracked at the request's completion).
   */
  @JsonIgnore
  public int getRequestLoad()
  {
    return this.requestLoad;
  }

  /**
   * Set the request load.
   */
  public void setRequestLoad(int requestLoad)
  {
    this.requestLoad = requestLoad;
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "MS [" + this.dispatches + " disp; " + this.queries + " qs; " + this.queryExceptions + " qexcs; " + this.logicTime + "ms logic; " + this.renderTime + "ms render]";
  }

  /**
   * Create a "request in progress" view of this sample.
   */
  public Map<String,Object> asRequestInProgressView()
  {
    final Map<String, Object> view = new HashMap<>(13);  // Number of items below.
    view.put("time", getStartTime());
    view.put("total", getTotalCpuTimeInProgress());
    view.put("cpu", getTotalCpuTimeInProgress());
    view.put("disp", getDispatches());
    view.put("logic", getLogicTimeInProgress());
    view.put("queries", getQueries());
    view.put("querytime", getQueryTimeInProgress());
    view.put("queryexc", getQueryExceptions());
    view.put("render", getRenderTimeInProgress());
    view.put("special", getSpecialTimeInProgress());
    view.put("reqnum", getRequestNumber());
    view.put("thread", getThreadID());
    view.put("command", getDispatchCommand());
    return view;
  }
  
  public static final Comparator<MonitorSample> BY_NAME = new Comparator<MonitorSample>() {
    @Override
    public int compare(MonitorSample o1, MonitorSample o2) {
      return StringHelper.compareToIgnoreCase(o1.command, o2.command);
    }
  };
  
}
