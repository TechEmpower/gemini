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

package com.techempower.thread;

import com.techempower.asynchronous.*;
import com.techempower.helper.*;

/**
 * A thread that provides a simple method for ending its execution.
 * Call setKeepRunning(false) whenever you want subclassed threads
 * to stop executing.  Note, the subclassed thread should frequently
 * call isRunning() in loops and stop executing when this method
 * returns true.
 *    <p>
 * This class differs from a Daemon-mode Thread in that these threads
 * should be stoppable at any time, not just when the application
 * shuts down.
 *    <p>
 * Also provides a simple thread sleeping mechanism that allows
 * optional sleep-period adjustment.
 */
public class EndableThread
  extends    Thread
  implements Asynchronous
{

  //
  // Constants.
  //

  public static final int DEFAULT_MAXIMUM_SLEEP    = 10000;     // 10 seconds.
  public static final int DEFAULT_MINIMUM_SLEEP    = 500;       // 0.5 seconds.
  public static final int DEFAULT_SLEEP_ADJUSTMENT = 500;       // +- 0.5 seconds.
  public static final int DEFAULT_SLEEP_PERIOD     = 1000;      // 1 second.

  //
  // Member variables.
  //

  private volatile boolean running = true;
  private volatile boolean paused  = false;
  private final    int     maxSleep;
  private final    int     minSleep;
  private final    int     sleepAdjustment;
  private final    Object  lockObject = new Object();
  
  private boolean sleeping        = false;                // An approximation
  private boolean pauseChecked    = false;
  private int     sleepPeriod     = DEFAULT_SLEEP_PERIOD;
  private long    startTime       = 0L;
  private long    stopTime        = 0L;

  //
  // Member methods.
  //

  /**
   * Constructor.
   *
   * @param name thread name.
   * @param sleepPeriod the starting sleep period, in milliseconds.
   * @param maxSleep the maximum sleep period.
   * @param minSleep the minimum sleep period.
   * @param sleepAdjustment the quanta adjustment for sleep periods.
   */
  public EndableThread(String name, int sleepPeriod, int maxSleep,
    int minSleep, int sleepAdjustment)
  {
    super(name);

    this.sleepPeriod     = sleepPeriod;
    this.maxSleep        = maxSleep;
    this.minSleep        = minSleep;
    this.sleepAdjustment = sleepAdjustment;
  }

  /**
   * Constructor.
   *
   * @param sleepPeriod the starting sleep period, in milliseconds.
   * @param maxSleep the maximum sleep period.
   * @param minSleep the minimum sleep period.
   * @param sleepAdjustment the quanta adjustment for sleep periods.
   */
  public EndableThread(int sleepPeriod, int maxSleep, int minSleep, 
      int sleepAdjustment)
  {
    this("Endable Thread", sleepPeriod, maxSleep, minSleep, sleepAdjustment);
  }

  /**
   * Simpler constructor.
   *
   * @param name thread name.
   * @param sleepPeriod the starting sleep period, in milliseconds.
   */
  public EndableThread(String name, int sleepPeriod)
  {
    this(name, sleepPeriod, DEFAULT_MAXIMUM_SLEEP, 
      DEFAULT_MINIMUM_SLEEP, DEFAULT_SLEEP_ADJUSTMENT);
  }

  /**
   * Simpler constructor.
   *
   * @param name thread name.
   */
  public EndableThread(String name)
  {
    this(name, DEFAULT_SLEEP_PERIOD);
  }

  /**
   * Simplest constructor.
   */
  public EndableThread()
  {
    this("Endable thread");
  }

  /**
   * Sets the keepRunning flag.  Set the flag to false to stop this 
   * thread.
   */
  public void setKeepRunning(boolean keepRunning)
  {
    this.running = keepRunning;
    this.interrupt();
    
    if (!keepRunning)
    {
      setStopTime();
    }
  }
  
  /**
   * Sets the paused flag.  Set the flag to true to (temporarily) pause the
   * execution of this thread.  You can then resume execution by setting
   * the pause flag to false.
   */
  public void setPaused(boolean paused)
  {
    // Resume.
    if (this.paused && !paused)
    {
      synchronized (lockObject)
      {
        this.paused = paused;
        this.pauseChecked = false;
        lockObject.notifyAll();
      }
    }
    
    // Pause (or no change).
    else
    {
      this.paused = paused;
    }
  }
  
  /**
   * Checks the paused flag and pauses execution until the paused flag is
   * cleared.  This is typically called within the thread's run() method at
   * points where execution can pause.
   *   <p>
   * The return value of this method can be ignored.  However, it returns the
   * "keep running" flag as a helpful utility.  This can be used to both
   * pause if needed or stop execution if needed.  For example, within the
   * run() method:
   *   <p>
   * if (!checkPause())
   * {
   *   return;
   * }
   */
  public boolean checkPause()
  {
    if (this.paused)
    {
      this.pauseChecked = true;
      try
      {
        while (this.paused)
        {
          synchronized (lockObject)
          {
            if (this.paused)
            {
              lockObject.wait(DEFAULT_MAXIMUM_SLEEP);
            }
          }
        }
      }
      catch (InterruptedException iexc)
      {
        // Do nothing.
      }
    }
    
    return isRunning();
  }
  
  /**
   * Tracks the start time so that calls to getThreadLifetime return a useful
   * result.  Subclasses should call this at the beginning of their run()
   * method -or- start by using the begin() method.
   */
  public void setStartTime()
  {
    this.startTime = System.currentTimeMillis();
  }
  
  /**
   * Returns the start time tracked by a call to setStartTime.
   */
  public long getStartTime()
  {
    return this.startTime;
  }
  
  /**
   * Tracks the stop time.  Calls to getThreadLifetime will normally return
   * the current time minus the start time, unless the thread has been
   * stopped/completed.  In that case, the thread lifetime will be static. 
   */
  public void setStopTime()
  {
    this.stopTime = System.currentTimeMillis();
  }
  
  /**
   * Returns the stop time tracked by a call to setStopTime.
   */
  public long getStopTime()
  {
    return this.stopTime;
  }
  
  /**
   * @return the maxSleep
   */
  protected int getMaxSleep()
  {
    return this.maxSleep;
  }

  /**
   * Gets the amount of time that this Thread has been in its "run()" method.
   * In order for this to return a useful result, the run() method should call
   * setStartTime when it starts.
   */
  public long getThreadLifetime()
  {
    if (getStopTime() >= getStartTime())
    {
      return getStopTime() - getStartTime();
    }
    else
    {
      return System.currentTimeMillis() - getStartTime();
    }
  }

  /**
   * Starts the thread.
   *   <p>
   * This method is nearly identical to a call to start() except that it
   * automatically calls setStartTime and will not attempt to start the thread
   * if it's already running.
   */
  @Override
  public void begin()
  {
    if (getState() == Thread.State.NEW)
    {
      setStartTime();
      start();
    }
  }
  
  /**
   * Ends the thread as soon as the next check to isRunning is made.
   * This is analogous to calling stop() except that it is not a
   * forceful stop.
   *   <p>
   * This method is <i>identical</i> to a call to setKeepRunning(false).
   */
  @Override
  public void end()
  {
    setKeepRunning(false);
  }

  /**
   * Is this thread still supposed to be running?  When this method
   * returns false, the subclassed thread should stop executing.
   */
  public boolean isRunning()
  {
    return this.running;
  }
  
  /**
   * Is this thread paused or pending a pause?  A thread will be "pending
   * pause" if a request to pause has been received but a call to checkPause
   * has not yet happened.
   */
  public boolean isPaused()
  {
    return this.paused;
  }
  
  /**
   * Has the pending pause been fulfilled?  That is, has checkPause been
   * called yet (putting the thread into an actual paused state)? 
   */
  public boolean isPauseChecked()
  {
    return this.pauseChecked;
  }

  /**
   * Is this thread sleeping?  This method is merely an approximation
   * as we do not synchronize any threads before setting or removing the
   * sleep flag.
   */
  public boolean isAsleep()
  {
    return this.sleeping;
  }

  /**
   * Gets the current sleep period, in milliseconds.  Calls to incrementSleep
   * and setMinimumSleep will cause this to fluctuate, so it may be of some
   * use to know what the currently-specified sleep period is since the next
   * call to simpleSleep will use the current sleep period.
   */
  public int getSleepPeriod()
  {
    return this.sleepPeriod;
  }
  
  /**
   * Convenience method for sleeping.  Handles interrupted exception
   * and does nothing.  Used the default sleep period.
   */
  public void simpleSleep()
  {
    simpleSleep((long)this.sleepPeriod);
  }

  /**
   * Convenience method for sleeping.
   * 
   * @param milliseconds the amount of time to sleep
   */
  public void simpleSleep(int milliseconds)
  {
    simpleSleep((long)milliseconds);
  }
  
  /**
   * Convenience method for sleeping.  Handles interrupted exception
   * and does nothing in the event of interruption.  This method sets
   * the sleeping flag to true, sleeps, and then sets the flag to
   * false.  This flag is merely an approximation, as it is 
   * conceivable that the flag could be read as "true" prior to the
   * actual sleep starting, and vice-versa.
   *
   * @param milliseconds the amount of time to sleep
   */
  public void simpleSleep(long milliseconds)
  {
    if (milliseconds > 0)
    {
      // Set the sleep flag.
      this.sleeping = true;
  
      // Do the sleeping.
      ThreadHelper.sleep(milliseconds);
  
      // Remove the sleep flag.
      this.sleeping = false;
    }
    else
    {
      // If zero milliseconds are specified, just yield.
      Thread.yield();
    }
  }

  /**
   * Increments the sleep period.
   */
  public void incrementSleep()
  {
    if (this.sleepPeriod < this.maxSleep)
    {
      this.sleepPeriod += this.sleepAdjustment;
      if (this.sleepPeriod > this.maxSleep)
      {
        this.sleepPeriod = this.maxSleep;
      }
    }
  }

  /**
   * Decrements the sleep period.
   */
  public void decrementSleep()
  {
    if (this.sleepPeriod > this.minSleep)
    {
      this.sleepPeriod -= this.sleepAdjustment;
      if (this.sleepPeriod < this.minSleep)
      {
        this.sleepPeriod = this.minSleep;
      }
    }
  }

  /**
   * Sets the sleep to minimum.
   */
  public void setMinimumSleep()
  {
    this.sleepPeriod = this.minSleep;
  }

  /**
   * Sets the sleep to maximum.
   */
  public void setMaximumSleep()
  {
    this.sleepPeriod = this.maxSleep;
  }

}   // End EndableThread.
