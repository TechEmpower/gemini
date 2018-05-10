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

/**
 * Represents a snapshot of an interval of time (typically one second) across
 * all threads.  For each thread, we will capture a PercentageSample object
 * that contains that thread's CPU utilization percentage.
 */
public class PercentageInterval
{

  //
  // Variables.
  //
  
  private final Map<Long,PercentageSample> samples;
  private String evaluation;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public PercentageInterval(Thread[] threads, PercentageInterval previous, ThreadMXBean jmx, long timeDelta)
  {
    // Capture the samples.
    this.samples = new HashMap<>(threads.length);
    for (Thread thread : threads)
    {
      long id;
      PercentageSample sample, previousSample;
      if (thread != null)
      {
        id = thread.getId();

        // Create the sample for this thread.
        sample = new PercentageSample(
            thread.getName(),
            id,
            jmx.getThreadCpuTime(id));

        // Put it in our map.
        this.samples.put(id, sample);

        // Compute CPU utilization percentage since the last interval.
        if (previous != null)
        {
          previousSample = previous.getPercentageSample(id);
          if (previousSample != null)
          {
            sample.setUsage(previousSample, timeDelta);
          }
        }
      }
    }
  }
  
  /**
   * Sets the evaluation String, indicating that this interval is exceptional.
   */
  public void setEvaluation(String evaluation)
  {
    this.evaluation = evaluation;
  }
  
  /**
   * Gets the evaluation String, if it's set.  Returns null if this interval
   * is not exceptional.
   */
  public String getEvaluation()
  {
    return this.evaluation;
  }
  
  /**
   * Is this interval exceptional?  (That is, has an evaluation string been
   * set?)
   */
  public boolean isExceptional()
  {
    return (this.evaluation != null);
  }
  
  /**
   * Gets a specific Thread's sample.
   */
  public PercentageSample getPercentageSample(long threadId)
  {
    return this.samples.get(threadId);
  }
  
  /**
   * Gets a List of PercentageSamples, ordered by the Thread's name.
   */
  public List<PercentageSample> getSamples()
  {
    List<PercentageSample> toReturn = new ArrayList<>(this.samples.values());
    Collections.sort(toReturn, PercentageSample.BY_NAME);
    return toReturn;
  }
  
}
