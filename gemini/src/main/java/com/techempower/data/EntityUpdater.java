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
package com.techempower.data;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.techempower.asynchronous.*;
import com.techempower.gemini.*;
import com.techempower.thread.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a queue of updates to DataEntities and commits them to the database
 * on a periodic interval.  This is useful for non-critical updates to 
 * entities that may be occurring in rapid succession.  Entities are 
 * de-duplicated in the add() method, so repeatedly adding the same object
 * reference will typically result in only a single write (depending on 
 * timing and whether the queue is flushed amid the calls to add).  Without a 
 * mechanism like this, several very similar database updates to the same
 * entity could be executed.
 *   <p>
 * Note that this component may not be suitable for distributed applications
 * because the references held by the queue will not be refreshed when a
 * cache update occurs.  The implementation of this component is fairly
 * simplistic.
 */
public class EntityUpdater
  implements Asynchronous
{
  
  //
  // Member variables.
  //
  
  private final GeminiApplication                   application;
  private final Logger                              log = LoggerFactory.getLogger(getClass());
  private final ConcurrentLinkedQueue<Identifiable> queue = new ConcurrentLinkedQueue<>();
  private final EntityUpdaterThread        thread;
  private final AtomicInteger              totalUpdateCount = new AtomicInteger();
  
  private int                        threadPriority = Thread.NORM_PRIORITY;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public EntityUpdater(GeminiApplication application,
    ConnectorFactory factory)
  {
    this.application = application;
    this.thread = new EntityUpdaterThread();
    
    application.addAsynchronous(this);
  }
  
  /**
   * @return The Priority for the updater thread.
   */
  public int getThreadPriority()
  {
    return threadPriority;
  }

  /**
   * @param threadPriority The Priority for the updater thread to set.
   */
  public void setThreadPriority(int threadPriority)
  {
    this.threadPriority = threadPriority;
    if (thread != null)
    {
      thread.setPriority(threadPriority);
    }
  }

  @Override
  public void begin()
  {
    // Start the thread.
    thread.setName("Entity Updater Thread (" + application.getVersion().getProductName() + ")");
    thread.setPriority(this.threadPriority);
    log.debug("Starting entity updater thread.");
    thread.begin();
  }

  @Override
  public void end()
  {
    // Stop the thread.
    stopThread();
    
    // Flush the queue.
    flushQueue();
  }
  
  /**
   * Stops the current thread.
   */
  protected void stopThread()
  {
    log.debug("Stopping entity updater thread.");
    thread.setKeepRunning(false);
  }

  /**
   * Flushes the queue by updating the DataEntities to the database.
   */
  protected int flushQueue()
  {
    int updateCount = 0;
    
    // Don't do anything if the queue is empty.
    while (queue.peek() != null)
    {
      final Identifiable entity = queue.poll();
      //log.debug("Updating " + entity);
      application.getStore().put(entity);
      updateCount++;
    }

    totalUpdateCount.addAndGet(updateCount);

    return updateCount;
  }
  
  /**
   * Adds a DataEntity to the update queue.
   */
  public void add(Identifiable entity)
  {
    if (!queue.contains(entity))
    {
      queue.add(entity);
    }
  }
  
  /**
   * The thread that actually consumes entities from the queue.
   */
  class EntityUpdaterThread
    extends EndableThread
  {
    /**
     * Constructor.
     */
    public EntityUpdaterThread()
    {
      super("Entity Updater Thread", 10000, 60000, 5000, 5000);
    }
    
    @Override
    public void run()
    {
      // Capture the start time.
      setStartTime();
      
      while (checkPause())
      {
        int updated = 0;
        try
        {
          updated = flushQueue();
        }
        catch (Exception exc)
        {
          log.error("Exception while flushing entity updater queue", exc);
        }
        if (updated > 0)
        {
          setMinimumSleep();
        }
        else
        {
          incrementSleep();
        }
        simpleSleep();
      }
    }
  }   // End EntityUpdaterThread.
  
  /**
   * Gets the number of Entities updated via the EntityUpdater.
   */
  public int getUpdateCount()
  {
    return totalUpdateCount.get();
  }
  
  @Override
  public String toString()
  {
    return "EntityUpdater (" + getUpdateCount() + " updates)";
  }
  
}  // End EntityUpdater.
