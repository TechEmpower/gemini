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

import java.util.*;

import com.fasterxml.jackson.annotation.*;

/**
 * A simple data structure storing a reference to a Thread, its total
 * execution time in milliseconds (not nanoseconds!) and the rough
 * percentage of CPU time consumed in the last second.
 *   <p>
 * Note that for multi-core systems, CPU time is calculated as a percentage
 * of a single core.  Multiple threads can approach 100% CPU utilization
 * concurrently on a multi-core system.
 */
public class PercentageSample
{
  //
  // Constants.
  //
  
  public static final long NS_PER_MS = 1000000L;
  public static final int  CENT      = 100;
  public static final int  SLOP_THRESHOLD_PERCENT = 105;
  
  //
  //
  //
  
  private final String name;
  private final long   id;
  private final long   ms;

  private short  usage;
  
  /**
   * Constructor.
   */
  public PercentageSample(String threadName, long threadID, long cpuTimeNs)
  {
    this.name = threadName;
    this.id = threadID;
    this.ms = cpuTimeNs / NS_PER_MS;
  }
  
  /**
   * Computes usage percentage between this sample and a preceding reference
   * sample.  The time delta between the two samples is presumed to be
   * approximately one second, but that is not required.
   */
  public void setUsage(PercentageSample previous, long timeDeltaBetweenSamples)
  {
    long threadMs = this.ms - previous.ms;
    if (  (threadMs > 0)
       && (timeDeltaBetweenSamples > 0)
       )
    {
      this.usage = (short)(CENT * threadMs / timeDeltaBetweenSamples);
      
      // Due to the level of accuracy (or lack thereof), it's possible to have
      // the threadMs exceed the time delta, resulting in >100% utilization,
      // so we'll normalize that down to 100.
      if ( (this.usage > CENT) && (this.usage < SLOP_THRESHOLD_PERCENT) )
      {
        this.usage = CENT;
      }
    }
    else
    {
      this.usage = 0;
    }
  }

  /**
   * @return the name
   */
  @JsonProperty("name")
  public String getName()
  {
    return this.name;
  }

  /**
   * @return the id
   */
  @JsonProperty("id")
  public long getId()
  {
    return this.id;
  }

  /**
   * @return the ms
   */
  @JsonProperty("ms")
  public long getMs()
  {
    return this.ms;
  }

  /**
   * @return the usage
   */
  @JsonProperty("usage")
  public short getUsage()
  {
    return this.usage;
  }
  
  /**
   * A simple toString.
   */
  @Override
  public String toString()
  {
    return "ID " + getId() + " (" + getName() + ") " + getMs() + "ms total time; " + getUsage() + "% current usage";
  }

  public static final Comparator<PercentageSample> BY_NAME = new Comparator<PercentageSample>() {
    @Override
    public int compare(PercentageSample o1, PercentageSample o2) {
      return o1.name.compareToIgnoreCase(o2.name);
    }
  };
  
}
