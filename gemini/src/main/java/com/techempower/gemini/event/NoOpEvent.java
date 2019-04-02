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
package com.techempower.gemini.event;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.scheduler.*;
import com.techempower.util.*;

/**
 * Does nothing; for testing/demonstration purposes only.  Typical usage for
 * events is to instantiate and schedule events in the "start" method of
 * the Infrastructure subclass.  E.g.,
 *   <p><pre>
 * public void start()
 * {
 *   getScheduler().clear();
 *   getScheduler().scheduleEvent(new NoOpEvent(this.application));
 * }
 * </pre>
 */
public class NoOpEvent
  extends    ScheduledEvent
  implements Configurable
{
  //
  // Constants.
  //

  public static final String LOCAL_COMPONENT_CODE = "nevt";

  //
  // Member variables.
  //

  private final GeminiApplication     application;
  private final ComponentLog          log;
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public NoOpEvent(GeminiApplication application)
  {
    super("No operation / Testing",
        "Executes for 30 seconds but does nothing; safe for testing");

    // Get local references set up.
    this.application = application;
    this.log         = application.getLog(LOCAL_COMPONENT_CODE);
    
    // Get configured.
    this.application.getConfigurator().addConfigurable(this);
  }
  
  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // Sometimes the time of day for an event is configurable, so events
    // commonly implement the Configurable interface.
    
    // In this demonstration, however, we are doing nothing.
  }

  /**
   * Gets the initial/default scheduled time for the first run of this
   * event.
   */
  @Override
  public long getDefaultScheduledTime()
  {
    // Get a current-time SimpleDate.
    Calendar nextRun = DateHelper.getCalendarInstance();

    // In this example, we're going to specify an initial execution of
    // 12:30pm.  We can't just set the hour to 12 because we may be
    // starting up after that time, so we'll just add hours until we get
    // to 12:30pm.  This is a rough calculation, but good enough.
    
    // Find 12pm.
    nextRun.add(Calendar.HOUR_OF_DAY, 1);
    while (nextRun.get(Calendar.HOUR_OF_DAY) != 12)
    {
      nextRun.add(Calendar.HOUR_OF_DAY, 1);
    }

    // Set the minutes and seconds accordingly.
    nextRun.set(Calendar.MINUTE, 30);
    nextRun.set(Calendar.SECOND, 0);

    // Return the time.
    return nextRun.getTimeInMillis();
  }

  /**
   * Overload this method to return true if this event needs to be run
   * on a thread separate from the Scheduler itself.  By default, events
   * get run ON the scheduler thread.
   */
  @Override
  public boolean requiresOwnThread()
  {
    // Let's get a separate thread.
    return true;
  }

  /**
   * Executes this event.
   * 
   * @param scheduler A reference to the scheduler so that we can reschedule
   *        this event after execution is complete.
   * @param onDemandExecute true if an administrator has requested an on-
   *        demand execution of this event; false if the event is running
   *        according to its schedule.
   */
  @Override
  public void execute(Scheduler scheduler, boolean onDemandExecute)
  {
    // We wrap the actual work in a try/catch block so that even if the
    // run is interrupted by an uncaught exception, we'll reschedule
    // properly.
    try
    {
      // Call our example do-nothing method.
      doNothing();
    }
    catch (Exception exc)
    {
      this.log.log("Exception: " + exc);
    }

    // Log the event completion.
    this.log.log(this + " complete.");

    // If this execution was a scheduled execution, let's reschedule for
    // the same time, but one day later.
    if (!onDemandExecute)
    {
      // Reschedule this event.
      Calendar newEventTime = DateHelper.getCalendarInstance(getScheduledTime());
  
      // Add a day.
      newEventTime.add(Calendar.DATE, 1);
  
      // Tell the scheduler to reschedule.
      scheduler.scheduleEvent(this, newEventTime.getTime());
    }
  }

  /**
   * Does nothing; this is a no-op event.
   */
  public void doNothing()
  {
    this.log.log("Doing nothing; this is a no-op event.");
  }
  
  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "[NoOpEvent: " + getScheduledTime() + "]";
  }

}  // End NoOpEvent.
