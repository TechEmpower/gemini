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
package com.techempower.thread;

import java.util.*;
import java.util.concurrent.*;

import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * Provides a thin abstraction over the Java Concurrency API's CountDownLatch 
 * to provide a simple means to fan work out to multiple threads and then
 * block, waiting for all these threads to complete.
 *   <p>
 * Runnables are added and then runAndWait is called to start all Runnables 
 * using the ThreadHelper's cached Executor.  Thread management is therefore
 * hidden from view.
 *   <p>
 * SimpleLatches are intended for one use only--that is, constructed, used,
 * and then discarded.  However, that pattern is not enforced and 
 * theoretically, the same workload could be repeated by calling runAndWait
 * multiple times.
 */
public class SimpleLatch
{

  //
  // Member variables.
  //
  
  private final List<Runnable> runnables = new ArrayList<>();
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public SimpleLatch()
  {
    // Does nothing.
  }
  
  /**
   * Adds a Runnable.  The Runnable is not executed on a thread until the 
   * runAndWait method is called.
   */
  public SimpleLatch add(Runnable runnable)
  {
    this.runnables.add(runnable);
    
    return this;
  }
  
  /**
   * Adds a bunch of Runnables.  The Runnables are not executed on threads
   * until the runAndWait method is called.
   */
  public SimpleLatch addAll(List<Runnable> runnablesToAdd)
  {
    this.runnables.addAll(runnablesToAdd);
    
    return this;
  }
  
  /**
   * Gets the number of Runnables that have been added.
   */
  public int size()
  {
    return this.runnables.size();
  }
  
  /**
   * Starts all Runnables using a cached Executor and awaits their completion.
   * Waits a default of 24 hours for completion.
   */
  public SimpleLatch runAndWait()
  {
    return runAndWait(UtilityConstants.DAY);
  }

  /**
   * Starts all Runnables using a cached Executor and awaits their completion.
   */
  public SimpleLatch runAndWait(long timeoutMs)
  {
    final CountDownLatch cdl = new CountDownLatch(size());
    
    // Wrap each Runnable and start it up.
    for (Runnable r : this.runnables)
    {
      final Runnable runnable = r;
      final Runnable wrapped = new Runnable() {
        @Override
        public void run()
        {
          runnable.run();
          cdl.countDown();
        }
      };
      
      ThreadHelper.submit(wrapped);
    }
    
    // Wait for the completion.
    try
    {
      cdl.await(timeoutMs, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException iexc)
    {
      // Do nothing.
    }
    
    return this;
  }
  
}
