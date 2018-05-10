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

import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * A simple specialization of ThreadPoolExecutor that allows pausing.  This
 * is basically lifted from the Javadocs for ThreadPoolExecutor.
 */
public class PausableThreadPoolExecutor
  extends    ThreadPoolExecutor
{

  //
  // Member variables.
  //
  
  private       boolean paused;
  private final ReentrantLock pauseLock = new ReentrantLock();
  private final Condition unpaused = this.pauseLock.newCondition();
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   * 
   * Use a LinkedBlockingQueue for the backing work queue so that we don't
   * require an available thread in order to accept new work.
   */
  public PausableThreadPoolExecutor(int coreThreads)
  {
    super(coreThreads, 
        coreThreads, 
        60L, 
        TimeUnit.SECONDS, 
        new LinkedBlockingQueue<Runnable>()
        );
  }
  
  /**
   * Overload the Executor's beforeExecute method to allow execution to be
   * paused.
   */
  @Override
  protected void beforeExecute(Thread t, Runnable r)
  {
    super.beforeExecute(t, r);
    this.pauseLock.lock();
    try
    {
      while (this.paused)
      {
        this.unpaused.await();
      }
    }
    catch (InterruptedException ie)
    {
      t.interrupt();
    }
    finally
    {
      this.pauseLock.unlock();
    }
  }

  /**
   * Pause the execution of tasks.
   */
  public void pause()
  {
    this.pauseLock.lock();
    try
    {
      this.paused = true;
    }
    finally
    {
      this.pauseLock.unlock();
    }
  }

  /**
   * Resume the execution of tasks.
   */
  public void resume()
  {
    this.pauseLock.lock();
    try
    {
      this.paused = false;
      this.unpaused.signalAll();
    }
    finally
    {
      this.pauseLock.unlock();
    }
  }

  /**
   * Is execution paused?
   */
  public boolean isPaused()
  {
    return this.paused;
  }
  
}
