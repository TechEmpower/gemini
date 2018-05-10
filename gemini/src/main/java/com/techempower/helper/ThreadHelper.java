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

package com.techempower.helper;

import java.util.*;
import java.util.concurrent.*;

/**
 * ThreadHelper provides utility functions for working with Threads.
 *   <p>
 * The ThreadHelper provides a ExecutorService for executing Callables via
 * the submit method.
 */
public final class ThreadHelper
{
  
  //
  // Static variables.
  //
  
  private static final Comparator<Thread> THREAD_NAME_COMPARATOR = new Comparator<Thread>() {
    @Override
    public int compare(Thread t1, Thread t2)
    {
      return t1.getName().compareToIgnoreCase(t2.getName());
    }
  };
  
  private static final ThreadGroup ROOT_THREAD_GROUP;
  private static final ExecutorService JOB_EXECUTOR = 
      Executors.newCachedThreadPool();
  private static final ScheduledExecutorService SCHEDULED_EXECUTOR = 
      Executors.newScheduledThreadPool(2);
  private static final int ACTIVE_THREAD_OVERBUFFER = 10;
  
  static
  {
    ThreadGroup group = Thread.currentThread().getThreadGroup();
    while (group.getParent() != null)
    {
      group = group.getParent();
    }
    
    ROOT_THREAD_GROUP = group;
  }
  
  //
  // Static methods.
  //

  /**
   * Sleeps for a specified number of milliseconds, ignoring any exceptions.
   */
  public static void sleep(long milliseconds)
  {
    try
    {
      Thread.sleep(milliseconds);
    }
    catch (InterruptedException iexc)
    {
      // Do nothing.
    }
  }
  
  /**
   * Queues up a Callable to be executed by the thread pool Executor. 
   */
  public static Future<?> submit(Callable<?> callable)
  {
    return JOB_EXECUTOR.submit(callable);
  }
  
  /**
   * Queues up a Runnable to be executed by the thread pool Executor. 
   */
  public static void submit(Runnable runnable)
  {
    JOB_EXECUTOR.execute(runnable);
  }

  /**
   * Queues up a Callable to be executed by the scheduled Executor. 
   */
  public static Future<?> schedule(Callable<?> callable, long delay, TimeUnit unit)
  {
    return SCHEDULED_EXECUTOR.schedule(callable, delay, unit);
  }
  
  /**
   * Queues up a Runnable to be executed by the scheduled Executor. 
   */
  public static void schedule(Runnable runnable, long delay, TimeUnit unit)
  {
    SCHEDULED_EXECUTOR.schedule(runnable, delay, unit);
  }

  /**
   * Queues up a Runnable to be executed by the scheduled Executor. 
   */
  public static void scheduleWithFixedDelay(Runnable callable, 
      long initialDelay, long delay, TimeUnit unit)
  {
    SCHEDULED_EXECUTOR.scheduleWithFixedDelay(callable, initialDelay, 
        delay, unit);
  }
  
  /**
   * Queues up a Runnable to be executed by the scheduled Executor. 
   */
  public static void scheduleAtFixedRate(Runnable callable, 
      long initialDelay, long period, TimeUnit unit)
  {
    SCHEDULED_EXECUTOR.scheduleAtFixedRate(callable, initialDelay, 
        period, unit);
  }
  
  /**
   * Gets the root ThreadGroup by navigating from the current thread to the
   * root of the ThreadGroup hierarchy.
   */
  public static ThreadGroup getRootThreadGroup()
  {
    return ROOT_THREAD_GROUP;
  }
  
  /**
   * Gets the currently active threads.
   */
  public static Thread[] getActiveThreads(boolean sorted)
  {
    ThreadGroup group = getRootThreadGroup();
    int activeThreads = group.activeCount();
    
    // Allocate room for ten more threads to start in between calls here.
    Thread[] temporaryThreads = new Thread[activeThreads + ACTIVE_THREAD_OVERBUFFER];
    int returned = group.enumerate(temporaryThreads);
    
    // Create an array to return that contains only active thread references.
    Thread[] threads = new Thread[returned];
    System.arraycopy(temporaryThreads, 0, threads, 0, returned);

    if (sorted)
    {
      Arrays.sort(threads, THREAD_NAME_COMPARATOR);
    }
    
    return threads;
  }

  /**
   * Gets a specific thread by its id.  Returns null if no matching thread
   * can be found. 
   */
  public static Thread getThread(long id)
  {
    // As far as I can tell, the only way to do this is by enumerating all
    // threads and looking for the thread with the ID in question.  There
    // does not appear to get a getThread(id) method anywhere in the standard
    // API.
    
    Thread[] threads = getActiveThreads(false);
    for (Thread thread : threads)
    {
      if (thread.getId() == id)
      {
        return thread;
      }
    }
    
    return null;
  }

  /**
   * You may not instantiate this class.
   */
  private ThreadHelper()
  {
    // Does nothing.
  }

}  // End ThreadHelper.

