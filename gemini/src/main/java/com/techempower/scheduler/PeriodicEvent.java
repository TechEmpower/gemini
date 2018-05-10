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
import com.techempower.util.*;

/**
 * An event that is executed on a regular interval, measured in seconds.  This
 * is a convenience subclass to ScheduledEvent that leaves you to implement a
 * call to the constructor and a method named "doIt" where you do your 
 * business.
 */
public abstract class PeriodicEvent
  extends ScheduledEvent
{

  //
  // Member variables.
  //
  
  private final int initialDelaySeconds;
  private final int intervalSeconds;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   * 
   * @param name The event's name.
   * @param description A description to display to administrators.
   * @param initialDelaySeconds the number of seconds to delay for the initial
   *        run.
   * @param intervalSeconds the number of seconds to delay between the
   *        completion of a run and the start of the next run.
   */
  public PeriodicEvent(String name, String description, int initialDelaySeconds, 
      int intervalSeconds)
  {
    super(name, description);
    
    // Bounds check.
    Args.intBound(initialDelaySeconds, "initialDelaySeconds", 0, Integer.MAX_VALUE);
    Args.intBound(intervalSeconds, "intervalSeconds", 0, Integer.MAX_VALUE);
    
    this.initialDelaySeconds = initialDelaySeconds;
    this.intervalSeconds = intervalSeconds;
  }
  
  
  /**
   * Constructor.
   * 
   * @param name The event's name.
   * @param description A description to display to administrators.
   * @param intervalSeconds the initial delay and the number of seconds to 
   *        delay between the completion of a run and the start of the next 
   *        run.
   */
  public PeriodicEvent(String name, String description, int intervalSeconds)
  {
    this(name, description, intervalSeconds, intervalSeconds);
  }

  /**
   * Gets a default scheduled time for this event.
   */
  @Override
  public long getDefaultScheduledTime()
  {
    return getNextRun(initialDelaySeconds).getTimeInMillis();
  }

  /**
   * Moves a Calendar object forward until the next run date.
   */
  protected Calendar getNextRun(int seconds)
  {
    final Calendar cal = DateHelper.getCalendarInstance();
    cal.add(Calendar.SECOND, seconds);
    
    return cal;
  }

  /**
   * Executes this event.
   */
  @Override
  public void execute(Scheduler scheduler, boolean onDemandExecution)
  {
    try
    {
      doIt();
    }
    catch (Exception exc)
    {
      scheduler.getLog().log("Exception while executing " + this, exc);
    }
    finally
    {
      // Reschedule this event.
      scheduler.scheduleEvent(this, 
          getNextRun(intervalSeconds).getTimeInMillis());
    }
  }
  
  /**
   * Subclasses implement this to do their business.
   */
  protected abstract void doIt();
  
}
