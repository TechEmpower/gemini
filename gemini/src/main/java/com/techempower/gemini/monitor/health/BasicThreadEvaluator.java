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

import com.techempower.gemini.monitor.*;

/**
 * The BasicThreadEvaluator is a simple implementation of HealthEvaluator that
 * compares the current thread situation on the server against "threshold"
 * parameters provided at construction time.
 *   <p>
 * This allows for "exceptional" health situations to be triggered by:
 * <ul>
 *   <li>Number of threads in general<li>
 *   <li>Number of blocked threads</li>
 *   <li>Number of waiting threads</li>
 * </ul>
 * Typical usage:
 * <code>
 *   // Consider 100+ threads, 25+ blocked threads, or 50+ waiting threads
 *   // as an exceptional case.
 *   monitor.addHealthEvaluator(new BasicThreadEvaluator(100, 25, 50));
 * </code>
 * 
 * @see HealthEvaluator
 * @see com.techempower.gemini.monitor.GeminiMonitor#addHealthEvaluator(HealthEvaluator)
 */
public class BasicThreadEvaluator
  implements HealthEvaluator
{

  private final int threadCount;
  private final int blockedCount;
  private final int waitingCount;
  
  /**
   * Constructor.  Provide the threshold counts for the various types of
   * threads to consider during health evaluation.
   * 
   * @param totalThreadCount If non-zero, a number of threads of all types
   *   that is considered exceptional.
   * @param blockedCount If non-zero, a number of BLOCKED threads that is
   *   considered exceptional.
   * @param waitingCount If non-zero, a number of WAITING threads that is
   *   considered exceptional.
   */
  public BasicThreadEvaluator(int totalThreadCount, int blockedCount, int waitingCount)
  {
    this.threadCount = totalThreadCount;
    this.blockedCount = blockedCount;
    this.waitingCount = waitingCount;
  }
  
  @Override
  public String isExceptional(HealthSnapshot snapshot, GeminiMonitor monitor)
  {
    // Check total thread count.
    if ( (this.threadCount > 0)
      && (snapshot.getTotalThreads() >= this.threadCount)
      )
    {
      return snapshot.getTotalThreads() + " total threads exceeds threshold of " + this.threadCount + ".";
    }
    
    // Check blocked thread count.
    if ( (this.blockedCount > 0)
      && (snapshot.getBlockedThreads() >= this.blockedCount)
      )
    {
      return snapshot.getBlockedThreads() + " blocked threads exceeds threshold of " + this.blockedCount + ".";
    }

    // Check waiting thread count.
    if ( (this.waitingCount > 0)
      && (snapshot.getWaitingThreads() >= this.waitingCount)
      )
    {
      return snapshot.getWaitingThreads() + " waiting threads exceeds threshold of " + this.waitingCount + ".";
    }
    
    // Not exceptional.
    return null;
  }

  @Override
  public String getEvaluatorName()
  {
    return "Thread Evaluator [" + this.threadCount + ";" + this.blockedCount + ";" + this.waitingCount + "]";
  }

}
