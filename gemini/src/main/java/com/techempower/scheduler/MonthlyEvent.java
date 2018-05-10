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
 * An event that is executed on a monthly basis at a given day-hour-minute
 * combination.  This is a convenience subclass to ScheduledEvent that leaves
 * you to implement a call to the constructor and a method named "doIt" where
 * you do your business.
 */
public abstract class MonthlyEvent
  extends ScheduledEvent
{

  //
  // Member variables.
  //
  
  private final int day;
  private final int hour;
  private final int minute;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   * 
   * @param name The event's name.
   * @param description A description to display to administrators.
   * @param day A day from 1 to 28; no higher days are permitted to avoid
   *        problems with February. 
   * @param hour An hour on a zero-indexed 24-hour clock (0 to 23).
   * @param minute A minute from 0 to 59.
   */
  public MonthlyEvent(String name, String description, int day, int hour, 
      int minute)
  {
    super(name, description);
    
    // Bounds check.
    Args.intBound(day, "day", 0, 28);
    Args.intBound(minute, "minute", 0, 59);
    Args.intBound(hour, "hour", 0, 23);
    
    this.day = day;
    this.hour = hour;
    this.minute = minute;
  }

  /**
   * Gets a default scheduled time for this event.
   */
  @Override
  public long getDefaultScheduledTime()
  {
    return getNextRun().getTimeInMillis();
  }

  /**
   * Moves a Calendar object forward until the next run date.
   */
  protected Calendar getNextRun()
  {
    // Move to the upcoming midnight.
    final Calendar cal = DateHelper.getStartOfDay();
    cal.add(Calendar.DAY_OF_MONTH, 1);
    
    // Keep moving until we get to the right day.
    while (cal.get(Calendar.DAY_OF_MONTH) != this.day)
    {
      cal.add(Calendar.DAY_OF_MONTH, 1);
    }

    // Set the remaining bits.
    cal.set(Calendar.HOUR_OF_DAY, this.hour);
    cal.set(Calendar.MINUTE, this.minute);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    
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
      scheduler.scheduleEvent(this, getDefaultScheduledTime());
    }
  }
  
  /**
   * Subclasses implement this to do their business.
   */
  protected abstract void doIt();
  
}
