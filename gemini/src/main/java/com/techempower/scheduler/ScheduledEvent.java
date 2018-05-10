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

import java.util.*;

import com.techempower.helper.*;

/**
 * A superclass for all ScheduledEvents handled by the Infrastructure's
 * Scheduler.  When an event is executed, it will be executed in the
 * Scheduler's thread context unless the event requires its own thread.
 * The scheduler thread is low-priority and should not be changed.  If the
 * event runs on its own thread, the thread will be initially set to low
 * priority, but the priority of that thread can be modified if desired.
 *
 * @see Scheduler
 */
public abstract class ScheduledEvent
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "sevt";
  
  public static final Comparator<ScheduledEvent> SORT_BY_NAME = 
      new Comparator<ScheduledEvent>() {
        @Override
        public int compare(ScheduledEvent e1, ScheduledEvent e2) {
          return e1.getName().compareTo(e2.getName());
        }
      };

  //
  // Member variables.
  //

  private long    scheduledTime     = 0L;
  private int     executions        = 0;
  private long    lastRunTime       = 0L;
  private long    lastCompleteTime  = 0L;
  private long    lastRunDuration   = 0L;
  private boolean executing         = false;
  private boolean enabled           = true;
  private final String  name;
  private final String  description;

  //
  // Member methods.
  //

  /**
   * Constructor.
   * 
   * @param name A short name for the event.
   * @param description A one or two sentence description of what the event 
   *        does.
   */
  public ScheduledEvent(String name, String description)
  {
    this.name = name;
    this.description = description;
  }

  /**
   * Computes the next scheduled execution time.  Subclasses should override
   * this method to implement some mechanism by which the next execution time
   * is determined.  For example, "at the next midnight" or "one hour from
   * right now."
   * 
   * TODO: Rename to computeNextExecutionTime.
   */
  public long getDefaultScheduledTime()
  {
    return getScheduledTime();
  }

  /**
   * Gets the currently scheduled next execution time.  This method <b>does
   * not</b> do any computation.  It simply returns a previously-computed
   * and scheduled next-execution time.
   */
  public long getScheduledTime()
  {
    return scheduledTime;
  }

  /**
   * Set the scheduled time.  Should be called by invoking
   * scheduler.scheduleEvent().
   */
  public ScheduledEvent setScheduledTime(long time)
  {
    this.scheduledTime = time;
    return this;
  }
  
  /**
   * Set the scheduled time.  Should not be called directly.  Rather, this
   * method is called by the scheduler ( scheduler.scheduleEvent() ).
   */
  public ScheduledEvent setScheduledTime(Date date)
  {
    this.scheduledTime = date.getTime();
    return this;
  }

  /**
   * Overload this method to return true if this event needs to be run
   * on a thread separate from the Scheduler itself.  By default, events
   * get run ON the scheduler thread.
   */
  public boolean requiresOwnThread()
  {
    return false;
  }

  /**
   * Is this event enabled?  Disabled events will not be executed by the
   * Scheduler when their scheduled time passes.  In the situation where a
   * disabled event is enabled after it was scheduled to execute, it will
   * be executed by the Scheduler when it next evaluates its state (typically
   * within a few seconds).
   */
  public boolean isEnabled()
  {
    return this.enabled;
  }

  /**
   * Sets the enabled flag.  See the notes for isEnabled.
   */
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }

  /**
   * Sets the executing flag.  This will be set by the scheduler itself
   * before calling execute, and then unset by the caller, as well.
   *   <p>
   * When execution of the event is starting (when this method is called
   * with a true parameter), the start time is recorded.  When execution is
   * ending, the duration is recorded.
   */
  public void setExecuting(boolean executing)
  {
    this.executing = executing;
    
    // Record start, end, and duration.
    if (executing)
    {
      lastRunTime = System.currentTimeMillis();
      executions++;
    }
    else
    {
      lastCompleteTime = System.currentTimeMillis();
      lastRunDuration = lastCompleteTime - lastRunTime;
    }
  }

  /**
   * Checks the executing flag.
   */
  public boolean isExecuting()
  {
    return executing;
  }
  
  /**
   * Gets the last run time (the last time this event was started).  Note
   * that if the event is still executing when this method is called, the
   * last run time will be -greater- than the last complete time.
   */
  public long getLastRunTime()
  {
    return lastRunTime;
  }
  
  /**
   * Gets the last completion time (the last time this event completed).
   */
  public long getLastCompleteTime()
  {
    return lastCompleteTime;
  }
  
  /**
   * Gets the duration (in milliseconds) of the last execution time that
   * has completed.
   */
  public long getLastRunDuration()
  {
    return lastRunDuration;
  }
  
  /**
   * Gets the number of times this event has been executed.
   */
  public int getExecutions()
  {
    return executions;
  }

  /**
   * Immediately begin execution of the event's principal work.  If the
   * requiresOwnThread method returns true, this execution will already be
   * on a new thread; otherwise, execution occurs within the scheduler thread.
   *   <p>
   * Implementations are required to either remove the event or reschedule
   * the event at the end of their execute method.  Failing to either remove
   * or reschedule will cause the Scheduler to continuously retry execution
   * of the event.
   *   <p>
   * To remove the event at completion:
   * <pre>
   *   // Do the principal work.
   *   // ...
   *   // When done, remove this event from the Scheduler.
   *   scheduler.removeEvent(this);
   * </pre>
   * To reschedule:
   * <pre>
   *   // Do the principal work.
   *   // ...
   *   // When done, reschedule this event.
   *   Calendar newEventTime = DateHelper.getCalendarInstance(getScheduledTime());
   *   // Add a month, for example.
   *   newEventTime.add(Calendar.MONTH, 1);
   *   // Then add another day, for example.
   *   newEventTime.add(Calendar.DATE, 1);
   *   // Tell the Scheduler to reschedule this event.
   *   scheduler.scheduleEvent(this, newEventTime);
   * </pre>
   * 
   * @param scheduler A reference to the Scheduler executing this event.
   * @param onDemandExecution True if this execution was requested by a
   *        system administrator and is running independently of its routine
   *        schedule.
   */
  public void execute(Scheduler scheduler, boolean onDemandExecution)
  {
    // Does nothing in this superclass except remove this event from the
    // Scheduler.

    scheduler.removeEvent(this);
  }
  
  /**
   * Gets the name of this Event.
   */
  public String getName()
  {
    return name;
  }
  
  /**
   * Gets a brief description of the event (optional).  If a description is
   * not provided, will return the name instead.
   */
  public String getDescription()
  {
    return StringHelper.emptyDefault(description, getName());
  }
  
  /**
   * Provides some basic toString functionality for scheduled events.
   * Ideally, subclasses would overload this to provide a little more
   * information.
   */
  @Override
  public String toString()
  {
    return "[" + getName() + ": " 
        + DateHelper.STANDARD_SQL_FORMAT.format(new Date(scheduledTime))
        + (isEnabled() ? "" : " (disabled)" )
        + "]";
  }

}   // End ScheduledEvent.
