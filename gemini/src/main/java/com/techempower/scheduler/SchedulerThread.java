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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The thread that runs the scheduler event checks.
 *   
 * @see Scheduler
 */
public class SchedulerThread
  extends    EndableThread
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "sthr";    // Four-letter component ID

  //
  // Member variables.
  //

  private final Scheduler    scheduler;
  private final Logger       log = LoggerFactory.getLogger(COMPONENT_CODE);
  private       long         nextCheck;
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public SchedulerThread(Scheduler scheduler)
  {
    super("Scheduler Thread (" + scheduler.getApplication().getVersion().getProductName() + ")");

    this.scheduler = scheduler;
  }

  /**
   * Thread.run() runs this thread.  This is where the scheduling is
   * evaluated and events executed, as necessary.
   */
  @Override
  public void run()
  {
    // Capture the start time.
    setStartTime();
    
    log.info("Scheduler thread started [{}; {}].",
        scheduler.getApplication().getVersion().getProductName(),
        scheduler.hashCode());

    // Keep going until setKeepRunning(false) is called.
    while (checkPause())
    {
      scheduler.checkSchedule();

      // Sleep for a while.  This sleep time, of course, specifies the
      // clarity of the scheduler.
      nextCheck = System.currentTimeMillis() + scheduler.getSleepTime(); 
      simpleSleep(scheduler.getSleepTime());
    }

    log.info("Scheduler thread stopped [{}; {}].",
        scheduler.getApplication().getVersion().getProductName(),
        scheduler.hashCode());
  }
  
  /**
   * Gets the time of the next schedule check.
   */
  public long getNextCheckTime()
  {
    return nextCheck;
  }

  @Override
  public void setKeepRunning(boolean keepRunning)
  {
    super.setKeepRunning(keepRunning);

    if (!isRunning())
    {
      log.info("Stopping scheduler thread.");
    }
  }

  @Override
  public String toString()
  {
    return "SchedulerThread " + scheduler;
  }

}