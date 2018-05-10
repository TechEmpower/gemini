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

import com.techempower.*;
import com.techempower.asynchronous.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * A simple task scheduler.  Maintains a separate thread that periodically
 * checks the clock and runs any events that are scheduled to execute.  An 
 * instance of this object is created by the application at its start time.
 *    <p>
 * Events should be added to the Scheduler at construction time within the
 * constructScheduler method in your application's Application subclass.
 *    <p>
 * Events should all be subclasses of ScheduledEvent.  When events are 
 * executed, they can be executed on the Scheduler thread or a separate 
 * thread, if desired; this is specified by the requiresOwnThread method.
 *    <p>
 * After performing their work, Events can reschedule themselves by calling 
 * scheduleEvent, even if they are already scheduled.  This will change the 
 * scheduled execution time (or reschedule the event if the scheduleEvent 
 * method is called from within the Event's execute method).
 *    <p>
 * Configuration file settings:
 *    <ul>
 * <li>SchedulerSleepSeconds - Sets the number of seconds to sleep the
 *     Scheduler's thread before checking for events that need to be run.
 * <li>SchedulerEnabled - Defaults to true; if set to false, the Scheduler
 *     will not run any events.
 *    </ul>
 *
 * @see ScheduledEvent
 * @see EventRunnerThread
 * @see SchedulerThread
 */
public class Scheduler
  implements Asynchronous,
             Configurable
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE     = "schd";    // Four-letter component ID
  public static final int    DEFAULT_SLEEP_TIME = 5000;      // five seconds
  public static final int    MINIMUM_SLEEP_TIME_SECONDS = 0;
  public static final int    MAXIMUM_SLEEP_TIME_SECONDS = 600;  // 10 minutes.

  //
  // Member variables.
  //

  private final TechEmpowerApplication  application;
  private final ComponentLog            log;
  private final List<ScheduledEvent>    scheduledEvents  = new ArrayList<>();
  private final SchedulerThread         schedulerThread;
  private       long                    sleepTime        = DEFAULT_SLEEP_TIME;
  private       boolean                 schedulerEnabled = true;

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public Scheduler(TechEmpowerApplication application)
  {
    this.application     = application;
    this.log             = application.getLog(COMPONENT_CODE);
    this.schedulerThread = new SchedulerThread(this);
    
    // Register as an asynchronous resource.
    application.addAsynchronous(this);
  }

  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // Configure the Scheduler.
    setSleepTime(props.getInt("SchedulerSleepSeconds", 
        (int)(getSleepTime() * UtilityConstants.SECOND)));
    setEnabled(props.getBoolean("SchedulerEnabled", true));
  }
  
  /**
   * Gets a reference to the Scheduler's ComponentLog.
   */
  protected ComponentLog getLog()
  {
    return log;
  }

  /**
   * Schedule a new event.  Provide an event and a date/time to invoke.
   * Repeated events should reschedule themselves when invoked by recalling
   * this method.
   */
  public synchronized void scheduleEvent(ScheduledEvent event, Date whenToInvoke)
  {
    scheduleEvent(event, whenToInvoke.getTime());
  }

  /**
   * Schedule a new event.  Provide an event and a date/time to invoke.
   * Repeated events should reschedule themselves when invoked by recalling
   * this method.
   */
  public synchronized void scheduleEvent(ScheduledEvent event, long whenToInvoke)
  {
    event.setScheduledTime(whenToInvoke);

    if (scheduledEvents.contains(event))
    {
      // Rescheduled.
      log.log(event.getName() + " rescheduled for " 
          + DateHelper.STANDARD_TECH_FORMAT.format(new Date(whenToInvoke)) );
    }
    else
    {
      // If the scheduled events collection does not yet have a record of
      // this event, add it.
      scheduledEvents.add(event);

      log.log(event + " scheduled for " 
          + DateHelper.STANDARD_TECH_FORMAT.format(new Date(whenToInvoke)) );
    }
  }

  /**
   * Simpler scheduleEvent method that schedules an event to the event's
   * defaultScheduledTime.
   */
  public synchronized void scheduleEvent(ScheduledEvent event)
  {
    scheduleEvent(event, new Date(event.getDefaultScheduledTime()));
  }

  /**
   * Removes all events from the Scheduler.
   */
  public synchronized void clear()
  {
    scheduledEvents.clear();
  }

  /**
   * Removes an event from the scheduled events.  Once an event is executed,
   * it is the event's responsibility to either reschedule itself
   */
  public synchronized void removeEvent(ScheduledEvent event)
  {
    if (event != null)
    {
      scheduledEvents.remove(event);
    }
  }

  /**
   * Gets an iterator over the events collection.
   */
  public synchronized List<ScheduledEvent> getEvents()
  {
    return new ArrayList<>(scheduledEvents);
  }

  /**
   * Determines if the Scheduler is known to be running.
   */
  public synchronized boolean isRunning()
  {
    return (schedulerThread != null);
  }

  /**
   * Gets a reference to the SchedulerThread.
   */
  public synchronized SchedulerThread getSchedulerThread()
  {
    return schedulerThread;
  }

  /**
   * Stops the scheduler.
   */
  @Override
  public synchronized void end()
  {
    schedulerThread.setKeepRunning(false);
  }

  /**
   * Starts the scheduler.
   */
  @Override
  public synchronized void begin()
  {
    schedulerThread.start();
  }

  /**
   * Gets the Application.
   */
  public TechEmpowerApplication getApplication()
  {
    return application;
  }

  /**
   * Is the Scheduler enabled?  Are events going to be run?
   */
  public boolean isEnabled()
  {
    return schedulerEnabled;
  }

  /**
   * Enable or disable the scheduler.  If disabled, calls to "checkSchedule"
   * will immediately return, having done nothing.
   */
  public void setEnabled(boolean schedulerEnabled)
  {
    this.schedulerEnabled = schedulerEnabled;
    if (!schedulerEnabled)
    {
      log.log("Scheduler disabled.  No events will run.");
    }
    else
    {
      log.log("Scheduler enabled.");
    }
  }

  /**
   * Gets the sleep time, in milliseconds.  (Note that setSleepTime takes
   * seconds rather than milliseconds.)
   */
  public long getSleepTime()
  {
    return sleepTime;
  }

  /**
   * Sets the sleep time of the scheduler, in seconds.  Default is FIVE
   * SECONDS.  Only values between 1 and 600 are allowed.
   */
  public void setSleepTime(int seconds)
  {
    int newSeconds = NumberHelper.boundInteger(seconds, 
        MINIMUM_SLEEP_TIME_SECONDS, MAXIMUM_SLEEP_TIME_SECONDS);
    sleepTime = newSeconds * UtilityConstants.SECOND;
    log.log("Sleep time set to " + newSeconds + " second" 
        + StringHelper.pluralize(newSeconds) + ".");
  }

  /**
   * Checks the schedule and executes events that are due.
   */
  public void checkSchedule()
  {
    // Do nothing if the Scheduler is disabled.
    if (isEnabled())
    {
      // Get the current date and time.
      final long now = System.currentTimeMillis();

      // Get an iterator over the events.
      final List<ScheduledEvent> events = getEvents();
      final Iterator<ScheduledEvent> iter = events.iterator();
      
      ScheduledEvent event;

      while (  (iter.hasNext())
            && (isEnabled())  // Stop looping if the scheduler becomes disabled.
            )
      {
        event = iter.next();

        // If the event is enabled, is not currently executing, and its 
        // scheduled time has passed, then let's go.
        if (  (event.isEnabled())
           && (!event.isExecuting())
           && (event.getScheduledTime() <= now)
           )
        {
          if (event.requiresOwnThread())
          {
            // Start a thread if the event requires it.
            executeEventNewThread(event, false);
          }
          else
          {
            // Execute on the current thread.
            executeEventCurrentThread(event, false);
          }
        }
      }
    }
  }

  /**
   * Executes a provided event on a new EventRunnerThread.  Returns true if
   * an EventRunnerThread was started.
   *
   * @param event The ScheduledEvent to run.
   * @param onDemandExecution Whether this execution is the result of
   *   an administrative request.
   */
  public boolean executeEventNewThread(ScheduledEvent event, boolean onDemandExecution)
  {
    if (!event.isExecuting())
    {
      // Set executing flag.
      event.setExecuting(true);

      log.log("Executing " + event.getName() + " on new thread.");
      final EventRunnerThread ert = new EventRunnerThread(event, this, onDemandExecution);
      ert.start();

      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * Executes a provided event on the current thread.  Returns true if the
   * execution completed; false if there was an exception.
   *
   * @param event The ScheduledEvent to run.
   * @param onDemandExecution Whether this execution is the result of
   *   an administrative request.
   */
  public boolean executeEventCurrentThread(ScheduledEvent event, boolean onDemandExecution)
  {
    if (!event.isExecuting())
    {
      // Set executing flag.
      event.setExecuting(true);
      log.log("Executing " + event);
      final Chronograph chrono = new Chronograph();
      try
      {
        event.execute(this, onDemandExecution);
      }
      catch (Exception exc)
      {
        log.log("Exception while executing " + event, exc);
        return false;
      }
      catch (Error error)
      {
        log.log("Error while executing " + event, error);
        return false;
      }
      finally
      {
        event.setExecuting(false);
        log.log(event.getName() + " complete. " + chrono);
      }

      return true;
    }
    else
    {
      // If the event is already executing, then return false to indicate
      // that we didn't start it.
      return false;
    }
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "[Scheduler: " + scheduledEvents.size() + " event(s)]";
  }

}   // End Scheduler.
