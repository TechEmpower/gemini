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
import com.techempower.scheduler.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple event that can be used to purge the application's cache every few
 * hours. Primarily meant to force synchronization between peer applications 
 * in a server cluster in case there are problems with the distributor. Can 
 * also be extended to perform other sorts of synchronization if needed, such
 * as re-indexing.
 *   <p>
 * The interval for this event is relative to the start of the current day. 
 * What this means is that if the interval is set to 6 hours then it will run
 * at 12am, 6am, 12pm, and 6pm regardless of when the application actually 
 * started up.
 *
 * To add this event to your application you can do something similar to the 
 * following in your cache class:
 *
 *   protected CacheResetEvent resetEvent;
 *
 *   public YourCache(YourApplication application, ConnectorFactory connectorFactory)
 *   {
 *     super(application, connectorFactory);
 *
 *     this.resetEvent = new CacheResetEvent(application);
 *   }
 *
 * Note: Depends on the application scheduler reference to be the same as the
 * infrastructure reference.
 */
public class CacheResetEvent
     extends ScheduledEvent 
  implements Configurable
{

  public static final String PROPS_PREFIX = "CacheReset.";
  public static final String PROPS_INTERVAL = "IntervalHours";
  public static final String PROPS_ENABLED = "Enabled";
  public static final String PROPS_NOTIFY = "NotifyPeers";

  public static final int DEFAULT_INTERVAL = 24;

  private int interval = DEFAULT_INTERVAL;
  private boolean notifyListeners = false;

  private final GeminiApplication application;
  private final Logger            log = LoggerFactory.getLogger(getClass());

  /**
   * Constructor.
   */
  public CacheResetEvent(GeminiApplication application)
  {
    super("Cache Reset Event", "Fully resets the application cache.");
    this.application = application;

    application.getConfigurator().addConfigurable(this);
  }

  /**
   * Configures this event. This method will also schedule itself using the application's scheduler.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    this.interval = props.getInt(PROPS_PREFIX + PROPS_INTERVAL, this.interval);
    setEnabled(props.getBoolean(PROPS_PREFIX + PROPS_ENABLED, isEnabled()));
    this.notifyListeners = props.getBoolean(PROPS_PREFIX + PROPS_NOTIFY, this.notifyListeners);

    // Schedule self
    this.application.getScheduler().scheduleEvent(this);
  }

  /**
   * Gets the initial scheduled time.
   */
  @Override
  public long getDefaultScheduledTime()
  {
    Calendar scheduledTime = DateHelper.getCalendarInstance();

    int eventHour = this.interval;
    int currentHour = scheduledTime.get(Calendar.HOUR_OF_DAY);

    // If the current hour is after the first interval slot find the next slot after now
    if (currentHour >= eventHour)
    {
      eventHour = this.interval * ((currentHour / this.interval) + 1);
    }

    scheduledTime.set(Calendar.HOUR_OF_DAY, eventHour);
    scheduledTime.set(Calendar.MINUTE, 0);
    scheduledTime.set(Calendar.SECOND, 0);

    return scheduledTime.getTimeInMillis();
  }

  @Override
  public void execute(Scheduler scheduler, boolean onDemandExecution)
  {
    if (isEnabled())
    {
      this.doReset();
    }

    scheduler.scheduleEvent(this, this.getNextRunTime());
  }

  /**
   * Gets the next time to run this event.
   */
  public long getNextRunTime()
  {
    Calendar next = DateHelper.getCalendarInstance();
    next.add(Calendar.HOUR, this.interval);
    next.set(Calendar.MINUTE, 0);
    next.set(Calendar.SECOND, 0);

    return (next.getTimeInMillis());
  }

  /**
   * Performs the actual resetting.
   * Extend this to reset additional resources.
   */
  protected void doReset()
  {
    this.log.info("Performing full cache reset");

    this.application.getStore().reset(this.notifyListeners, false);
  }
}
