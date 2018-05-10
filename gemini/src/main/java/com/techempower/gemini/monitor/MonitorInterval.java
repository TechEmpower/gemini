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

import com.fasterxml.jackson.annotation.*;
import com.techempower.gemini.*;
import com.techempower.helper.*;

/**
 * A MonitorInterval stores statistical data associated with a specific
 * MonitoredCommand (e.g., "login") for an interval of time, typically one
 * hour.  The associated MonitoredCommand object will retain a short array
 * of these objects so that it can display historical trends in its monitoring
 * of the command over time.
 */
public class MonitorInterval
{

  //
  // Constants.
  //
  
  public static final int MAXIMUM_REQUEST_SIGNATURE_LENGTH = 500;
  
  //
  // Member variables.
  //
  
  private MonitorSample mostQueries;
  private MonitorSample mostQueryTime;
  private MonitorSample mostLogicTime;
  private MonitorSample mostRenderTime;
  private MonitorSample mostExceptions;
  private MonitorSample mostSpecialTime;
  private MonitorSample mostCpuTime;
  
  private int totalDispatches;
  private int totalQueries;
  private int totalLogicTime;
  private int totalRenderTime;
  private int totalQueryExceptions;
  private int totalQueryTime;
  private int totalSamples;
  private int totalSpecialTime;
  private int totalCpuTime;
  
  private long intervalStart;
  private long intervalEnd;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public MonitorInterval(long intervalStart, long intervalEnd)
  {
    this.intervalStart = intervalStart;
    this.intervalEnd = intervalEnd;
  }
  
  /**
   * Evaluate a MonitorSample, factor the sample into averages and then store
   * a reference to any of the exceptional case references if needed.
   */
  public void process(MonitorSample sample, Context context)
  {
    // Increase the totals appropriately.
    this.totalSamples++;
    this.totalDispatches += sample.getDispatches();
    this.totalQueries += sample.getQueries();
    this.totalLogicTime += sample.getLogicTime();
    this.totalRenderTime += sample.getRenderTime();
    this.totalQueryExceptions += sample.getQueryExceptions();
    this.totalQueryTime += sample.getQueryTime();
    this.totalSpecialTime += sample.getSpecialTime();
    this.totalCpuTime += sample.getTotalCpuTime();
    
    // Does this qualify as an exceptional case?  That is, does it exceed
    // our current records on any of these metrics?
    if (getWorstQueries() < sample.getQueries())
    {
      this.mostQueries = sample;
    }
    if (getWorstQueryTime() < sample.getQueryTime())
    {
      this.mostQueryTime = sample;
    }
    if (getWorstLogicTime() < sample.getLogicTime())
    {
      this.mostLogicTime = sample;
    }
    if (getWorstRenderTime() < sample.getRenderTime())
    {
      this.mostRenderTime = sample;
    }
    if (getWorstQueryExceptions() < sample.getQueryExceptions())
    {
      this.mostExceptions = sample;
    }
    if (getWorstSpecialTime() < sample.getSpecialTime())
    {
      this.mostSpecialTime = sample;
    }
    if (getWorstCpuTime() < sample.getTotalCpuTime())
    {
      this.mostCpuTime = sample;
    }
    
    // If we just recorded this as an exceptional case, let's grab the request
    // signature for possible debugging purposes.
    if ( (this.mostQueries == sample)
      || (this.mostQueryTime == sample)
      || (this.mostLogicTime == sample)
      || (this.mostRenderTime == sample)
      || (this.mostExceptions == sample)
      || (this.mostSpecialTime == sample)
      || (this.mostCpuTime == sample)
      )
    {
      sample.setRequestSignature(StringHelper.truncateEllipsis(context.getRequestSignature(), MAXIMUM_REQUEST_SIGNATURE_LENGTH));
    }
  }
  
  /**
   * Gets the interval start time.
   */
  @JsonProperty("time")
  public long getIntervalStart()
  {
    return this.intervalStart;
  }
  
  /**
   * Gets the interval end time.
   */
  @JsonIgnore
  public long getIntervalEnd()
  {
    return this.intervalEnd;
  }
  
  /**
   * Gets the count of samples for this interval.
   */
  @JsonProperty("count")
  public int getSampleCount()
  {
    return this.totalSamples;
  }
  
  /**
   * Gets the average dispatches per request.
   */
  @JsonProperty("avgdisp")
  public int getAverageDispatches()
  {
    return this.totalDispatches / this.totalSamples;
  }
  
  /**
   * Gets the average queries per request.
   */
  @JsonProperty("avgqr")
  public int getAverageQueries()
  {
    return this.totalQueries / this.totalSamples;
  }
  
  /**
   * Gets the average logic time per request.
   */
  @JsonProperty("avglg")
  public int getAverageLogicTime()
  {
    return this.totalLogicTime / this.totalSamples;
  }
  /**
   * Gets the average render time per request.
   */
  @JsonProperty("avgrn")
  public int getAverageRenderTime()
  {
    return this.totalRenderTime / this.totalSamples;
  }
  
  /**
   * Gets the average query exceptions per request.
   */
  @JsonProperty("avgqe")
  public int getAverageQueryExceptions()
  {
    return this.totalQueryExceptions / this.totalSamples;
  }
  
  /**
   * Gets the average query time per request.
   */
  @JsonProperty("avgqt")
  public int getAverageQueryTime()
  {
    return this.totalQueryTime / this.totalSamples;
  }
  
  /**
   * Gets the average special-operation time per request.
   */
  @JsonProperty("avgsp")
  public int getAverageSpecialTime()
  {
    return this.totalSpecialTime / this.totalSamples;
  }
  
  /**
   * Gets the average CPU time per request.
   */
  @JsonProperty("avgcp")
  public int getAverageCpuTime()
  {
    return this.totalCpuTime / this.totalSamples;
  }
  
  /**
   * Gets the average total time (logic + render)
   */
  @JsonProperty("avgto")
  public int getAverageTotalTime()
  {
    return getAverageLogicTime() + getAverageRenderTime();
  }
  
  /**
   * Gets the most queries exceptional case.
   */
  @JsonIgnore
  public MonitorSample getMostQueries()
  {
    return this.mostQueries;
  }
  
  /**
   * Gets the most query time exceptional case.
   */
  @JsonIgnore
  public MonitorSample getMostQueryTime()
  {
    return this.mostQueryTime;
  }
  
  /**
   * Gets the most query errors exceptional case.
   */
  @JsonIgnore
  public MonitorSample getMostQueryExceptions()
  {
    return this.mostExceptions;
  }
  
  /**
   * Gets the most logic time exceptional case.
   */
  @JsonIgnore
  public MonitorSample getMostLogicTime()
  {
    return this.mostLogicTime;
  }
  
  /**
   * Gets the most render time exceptional case.
   */
  @JsonIgnore
  public MonitorSample getMostRenderTime()
  {
    return this.mostRenderTime;
  }
  
  /**
   * Gets the most special-operation time exceptional case.
   */
  @JsonIgnore
  public MonitorSample getMostSpecialTime()
  {
    return this.mostSpecialTime;
  }
  
  /**
   * Gets the most CPU time case.
   */
  @JsonIgnore
  public MonitorSample getMostCpuTime()
  {
    return this.mostCpuTime;
  }
  
  /**
   * Gets the worst query count.
   */
  @JsonProperty("worqr")
  public int getWorstQueries()
  {
    return (getMostQueries() != null) ? getMostQueries().getQueries() : 0;
  }
  
  /**
   * Gets the worst query time.
   */
  @JsonProperty("worqt")
  public int getWorstQueryTime()
  {
    return (getMostQueryTime() != null) ? getMostQueryTime().getQueryTime() : 0;
  }
  
  /**
   * Gets the worst query exceptions count.
   */
  @JsonProperty("worqe")
  public int getWorstQueryExceptions()
  {
    return (getMostQueryExceptions() != null) ? getMostQueryExceptions().getQueryExceptions() : 0;
  }
  
  /**
   * Gets the worst logic time.
   */
  @JsonProperty("worlg")
  public int getWorstLogicTime()
  {
    return (getMostLogicTime() != null) ? getMostLogicTime().getLogicTime() : 0;
  }
  
  /**
   * Gets the worst render time.
   */
  @JsonProperty("worrn")
  public int getWorstRenderTime()
  {
    return (getMostRenderTime() != null) ? getMostRenderTime().getRenderTime() : 0;
  }
  
  /**
   * Gets the worst special-operation time.
   */
  @JsonProperty("worsp")
  public int getWorstSpecialTime()
  {
    return (getMostSpecialTime() != null) ? getMostSpecialTime().getSpecialTime() : 0;
  }
  
  /**
   * Gets the worst CPU time.
   */
  @JsonProperty("worcp")
  public int getWorstCpuTime()
  {
    return (getMostCpuTime() != null) ? getMostCpuTime().getTotalCpuTime() : 0;
  }
  
}