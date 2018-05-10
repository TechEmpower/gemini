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

package com.techempower.log;

import java.util.*;
import java.util.concurrent.*;

import com.techempower.*;
import com.techempower.thread.*;

/**
 * This helper class monitors the most recent log file access time. If it's
 * more than 5 minutes ago, it closes the file. The class is also responsible
 * for periodically flushing the queued list of log items to files on its
 * thread. This allows for the removal of synchronization within the
 * LogWriters themselves.
 * <p>
 * The LogWriterManager is a special case of Asynchronous resource. Because it
 * is responsible for seeing that log files are flushed, we want to start it
 * before any other resources and stop it as late as possible. So the
 * application should start it once constructed (see TechEmpowerApplication's
 * constructor) and stop it after all other asynchronous resources have been
 * stopped.
 */
public class LogWriterManager
  extends    EndableThread
{

  //
  // Constants.
  //

  private static final int  INITIAL_SLEEP_MILLIS          = 100;          // 0.1 second
  private static final int  MINIMUM_SLEEP_MILLIS          = 25;           // 0.05 second
  private static final int  MAXIMUM_SLEEP_MILLIS          = 150;          // 0.15 second
  private static final int  SLEEP_ADJUSTMENT              = 5;            // 0.005 second
  private static final long DEFAULT_LAST_ACCESS_THRESHOLD = 2L * 60000L;  // 2 minutes

  //
  // Static variables.
  //

  //
  // Member variables.
  //

  private final List<LogWriterDescriptor> logWriters = new CopyOnWriteArrayList<>();
  private long threshold = DEFAULT_LAST_ACCESS_THRESHOLD;

  //
  // Member methods.
  //

  /**
   * Constructor.  Generally, this should not be called directly unless you
   * want multiple LogWriterClosers each running on separate threads.  To use
   * a single instance of this class to monitor multiple LogWriters, call
   * getInstance instead.
   */
  public LogWriterManager(TechEmpowerApplication application)
  {
    super("Log Writer Manager (" + application.getVersion().getProductName() + ")",
      INITIAL_SLEEP_MILLIS, 
      MAXIMUM_SLEEP_MILLIS, 
      MINIMUM_SLEEP_MILLIS, 
      SLEEP_ADJUSTMENT
      );
  }

  /**
   * Adds a LogWriter to monitor.
   */
  public void addLogWriter(LogWriter logWriter, String description)
  {
    // Create a descriptor and add it to the list.
    LogWriterDescriptor descriptor = new LogWriterDescriptor(logWriter,
        description);
    this.logWriters.add(descriptor);
  }

  /**
   * Removes a LogWriter.
   */
  public void removeLogWriter(LogWriter logWriter)
  {
    Iterator<LogWriterDescriptor> iter = this.logWriters.iterator();
    LogWriterDescriptor descriptor;

    // Remove any references to this LogWriter.
    while (iter.hasNext())
    {
      descriptor = iter.next();
      if (descriptor.logWriter == logWriter)
      {
        iter.remove();
      }
    }
  }

  /**
   * Sets the inactivity time at which a log file will be closed.
   *
   * @param idleThreshold the amount of time a log file should be idle,
   *        in milliseconds, before automatically closing.
   */
  public void setIdleThreshold(long idleThreshold)
  {
    this.threshold = idleThreshold;
  }

  /**
   * Run method.
   */
  @Override
  public void run()
  {
    // Capture the start time.
    setStartTime();
    
    long                currentTime;
    boolean             anyActivity, activity;

    // Check to see if we should keep running or pause.
    while (checkPause())
    {
      // Sleep for a bit.
      simpleSleep();

      // Reset the activity flag and check the current time.
      anyActivity = false;
      currentTime = System.currentTimeMillis();
      
      // Go through each element and check the last-access times.
      for (LogWriterDescriptor descriptor : this.logWriters)
      {
        // Ask the LogWriter to flush any queued log items.
        activity = descriptor.logWriter.flushFile();
        anyActivity = (anyActivity || activity);
        
        // There was activity in this log.  Set the lastAccess time
        // accordingly.
        if (activity)
        {
          descriptor.lastAccess = currentTime;
        }
        else
        {
          // If the LogWriter has been inactive for the maximum idle time,
          // close the file.
          if ((descriptor.logWriter.isOpen())
              && (currentTime > descriptor.lastAccess + this.threshold))
          {
            descriptor.logWriter.closeFile(
                descriptor.description + " inactive; closing file.");
          }
        }
      }
      
      // Adjust our sleep period accordingly.
      if (anyActivity)
      {
        setMinimumSleep();
      }
      else
      {
        incrementSleep();
      }
    }
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "LogWriterManager [" + this.logWriters.size() + " LogWriters being watched]";
  }

  //
  // Internal classes.
  //

  /**
   * Helper class packages a LogWriter, a last access time, and a
   * description of the LogWriter.
   */
  private static final class LogWriterDescriptor
  {
    private final LogWriter logWriter;
    private final String    description;
    private       long      lastAccess;

    /**
     * Constructor.
     */
    private LogWriterDescriptor(LogWriter logWriter, String description)
    {
      this.logWriter   = logWriter;
      this.description = description;
      this.lastAccess  = System.currentTimeMillis();
    }
    
  }   // End LogWriterDescriptor

}   // End LogWriterManager
