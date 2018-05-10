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

package com.techempower.gemini.monitor.health;

import com.fasterxml.jackson.annotation.*;
import com.techempower.gemini.monitor.*;

/**
 * GeminiMonitor component for capturing a "snapshot" view of several 
 * application health indicators.  The Gemini Monitor can be configured
 * to take these snapshots at a configurable interval and retain a history
 * for evaluation.
 *   <p>
 * Snapshots can also be considered exceptional, in which case a 
 * HealthListener can act accordingly (e.g., by sending an e-mail to 
 * administrators).
 *   <p>
 * Some of the metrics included here should be considered approximations.
 * For example, the number of concurrentDispatches for a snapshot is the
 * number that had started but had not yet completed at the time snapshot's
 * interval ended.  But the counting is not strictly an atomic operation, so
 * the precision of the counting is not perfect.
 */
public class HealthSnapshot
{

  //
  // Member variables.
  //
  
  private final long startTime;

  private long    endTime;
  private long    totalMemory;
  private long    freeMemory;
  private long    requestCount;               // This is a long because we used longs to record request counts elsewhere.
  private int     dispatchCount;              // Dispatches for this interval only. 
  private int     dispatchConcurrency;
  private int     pageRenderCount;
  private int     pageRenderConcurrency;
  private int     queryCount;
  private int     queryConcurrency;
  private int     totalThreads;
  private int     blockedThreads;
  private int     waitingThreads;
  private boolean exceptional = false;        // The checks for unusually high or low values marked this sample as exceptional. 
  private HealthSnapshotDetail detail = null;
  
  //
  // Methods.
  //
  
  public HealthSnapshot(long intervalLength)
  {
    this.startTime = System.currentTimeMillis();
    this.endTime = this.startTime + intervalLength;
  }
  
  /**
   * Called when the Snapshot is "complete" (the system clock has reached
   * this snapshot's end time).  This will collect metrics.
   */
  public void complete(GeminiMonitor monitor)
  {
    // Get request count.
    this.requestCount = monitor.getRequestCount();
    
    // Get a snapshot view of the concurrent load right now.
    this.dispatchConcurrency = monitor.getDispatchLoad();
    this.pageRenderConcurrency = monitor.getPageRenderLoad();
    this.queryConcurrency = monitor.getQueryLoad();
    
    // Get memory statistics.
    this.totalMemory = Runtime.getRuntime().totalMemory();
    this.freeMemory = Runtime.getRuntime().freeMemory();
    
    // Get thread statistics.
    
    // Grab lightweight thread information here.
    Thread[] threads = monitor.getThreadArray();

    for (Thread thread : threads)
    {
      if (thread != null)
      {
        this.totalThreads++;
        switch (thread.getState())
        {
          case BLOCKED:
            this.blockedThreads++;
            break;
          case WAITING:
            this.waitingThreads++;
            break;
          default:
            break;
        }
      }
    }
    
    // Evaluate this health snapshot to decide if it is exceptional.
    String evaluation = monitor.evaluateHealthSnapshot(this);
    
    // If exceptional, do more expensive thread analysis.
    if (evaluation != null)
    {
      this.exceptional = true;
      this.detail = new HealthSnapshotDetail();
      this.detail.setEvaluation(evaluation);
      this.detail.setThreadInfo(monitor.getThreadDetails());
    }
  }

  /**
   * @return the exceptional flag
   */
  @JsonProperty("exc")
  public boolean isExceptional()
  {
    return this.exceptional;
  }
  
  /**
   * Gets the String evaluation of this health snapshot assuming it is
   * exceptional and has an attached HealthSnapshotDetail.  Returns null
   * otherwise.
   */
  @JsonProperty("evaluation")
  public String getEvaluationString()
  {
    if (this.exceptional && this.detail != null)
    {
      return this.detail.getEvaluation();
    }
    else
    {
      return null;
    }
  }

  /**
   * @return the endTime
   */
  @JsonIgnore
  public long getEndTime()
  {
    return this.endTime;
  }

  /**
   * @param endTime the endTime to set
   */
  public void setEndTime(long endTime)
  {
    this.endTime = endTime;
  }

  /**
   * @return the totalMemory
   */
  @JsonProperty("totalmem")
  public long getTotalMemory()
  {
    return this.totalMemory;
  }

  /**
   * @return the freeMemory
   */
  @JsonProperty("freemem")
  public long getFreeMemory()
  {
    return this.freeMemory;
  }
  
  /**
   * Increments the dispatch count.
   */
  public void incrementDispatchCount()
  {
    this.dispatchCount++;
  }

  /**
   * @return the dispatchCount
   */
  @JsonProperty("disps")
  public int getDispatchCount()
  {
    return this.dispatchCount;
  }

  /**
   * @return the dispatchConcurrency
   */
  @JsonProperty("dispcon")
  public int getDispatchConcurrency()
  {
    return this.dispatchConcurrency;
  }

  /**
   * Increments the page render count.
   */
  public void incrementPageRenderCount()
  {
    this.pageRenderCount++;
  }
  
  /**
   * @return the pageRenderCount
   */
  @JsonProperty("pages")
  public int getPageRenderCount()
  {
    return this.pageRenderCount;
  }

  /**
   * @return the pageRenderConcurrency
   */
  @JsonProperty("pagecon")
  public int getPageRenderConcurrency()
  {
    return this.pageRenderConcurrency;
  }

  /**
   * Increments the query count.
   */
  public void incrementQueryCount()
  {
    this.queryCount++;
  }
  
  /**
   * @return the queryCount
   */
  @JsonProperty("queries")
  public int getQueryCount()
  {
    return this.queryCount;
  }

  /**
   * @return the queryConcurrency
   */
  @JsonProperty("querycon")
  public int getQueryConcurrency()
  {
    return this.queryConcurrency;
  }

  /**
   * @return the totalThreads
   */
  @JsonProperty("threads")
  public int getTotalThreads()
  {
    return this.totalThreads;
  }
  
  /**
   * @return the blockedThreads
   */
  @JsonProperty("blocked")
  public int getBlockedThreads()
  {
    return this.blockedThreads;
  }

  /**
   * @return the waitingThreads
   */
  @JsonProperty("waiting")
  public int getWaitingThreads()
  {
    return this.waitingThreads;
  }
  
  /**
   * Gets the TOTAL number of requests since application start.
   */
  @JsonProperty("reqs")
  public long getRequestCount()
  {
    return this.requestCount;
  }

  /**
   * @return the startTime
   */
  @JsonProperty("start")
  public long getStartTime()
  {
    return this.startTime;
  }
  
}
