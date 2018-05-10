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

package com.techempower.scheduler;

import com.techempower.thread.*;
import com.techempower.util.*;

/**
 * Used by the Scheduler to run events on their own thread, if the event
 * requests that it be run separate from the Scheduler itself.
 *    <p>
 * <b>Note</b>: This thread will have its priority automatically set to
 * minimum priority.  It is assumed that events can run at low priority.
 *
 * @see Scheduler
 */
public class EventRunnerThread
  extends    EndableThread
{

  //
  // Static variables.
  //
  
  private static final ThreadLocal<EventRunnerThread> CURRENT_THREAD = new ThreadLocal<>();
  
  //
  // Member variables.
  //

  private final ScheduledEvent event;
  private final Scheduler      scheduler;
  private final boolean        onDemandExecution;

  /**
   * Constructor.
   *
   * @param event the event being launched.
   * @param scheduler the scheduler launching the event.
   * @param onDemandExecution True if this is an on-demand (admin-initiated);
   *   false if it's a scheduled execution.
   */
  public EventRunnerThread(ScheduledEvent event, Scheduler scheduler,
      boolean onDemandExecution)
  {
    super("Event Runner (" + event.getName() + ")");

    setPriority(MIN_PRIORITY);

    this.event     = event;
    this.scheduler = scheduler;
    this.onDemandExecution = onDemandExecution;
  }

  /**
   * Standard run method.
   */
  @Override
  public void run()
  {
    setStartTime();
    final Chronograph chrono = new Chronograph();
    
    CURRENT_THREAD.set(this);
    
    try
    {
      event.execute(scheduler, onDemandExecution);
    }
    catch (Exception exc)
    {
      scheduler.getLog().log("Exception while executing (new thread) "
          + event, exc);
    }
    catch (Error error)
    {
      scheduler.getLog().log("Error while executing (new thread) " 
          + event, error);
    }
    finally
    {
      event.setExecuting(false);
      scheduler.getLog().log(event.getName() + " complete. " + chrono);
    }
  }
  
  /**
   * Gets the EventRunnerThread for the current Event runner thread.
   */
  public static EventRunnerThread getCurrentThread()
  {
    return CURRENT_THREAD.get();
  }
  
}   // End EventRunnerThread.
