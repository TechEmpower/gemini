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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.techempower.*;
import com.techempower.util.*;

/**
 * A basic implementation of Log.  This implementation starts with three
 * listeners (specifiable by config file) - FileLogListener, and 
 * ConsoleLogListener.  These can be turned on or off via the config file.
 * <p>
 * These can be removed, and more can be added through the addLogListener
 * method.
 * <p>
 * Reads the following configuration options from the .conf file:
 *    <ul>
 * <li>Log.File.LogDirectory - Where on disk to store logs
 * <li>Log.File.LogDebugThreshold - Sets the logging threshold, lower = more
 *     logging.  Range: 0 to 100 in steps of 10.
 * <li>Log.File.On - is the file log listener enabled?
 * <li>Log.Console.On - is the console log listener enabled?
 * <li>Log.File.debugThreshold - the debug threshold for the file log listener.
 * <li>Log.Console.debugThreshold - the debug threshold for the console log 
 *     listener.
 *    </ul>
 */
public class BasicLog
  extends    AbstractLogListener
  implements Log
{

  //
  // Static references.
  //
  
  private static Log applicationLog;
  
  //
  // Private Members.
  //
  
  private CopyOnWriteArrayList<LogListener> logListeners = new CopyOnWriteArrayList<>();
  private List<LogItem>   preConfigMessages = Collections.synchronizedList(new ArrayList<LogItem>());
  private boolean         configured        = false;

  private ConsoleLogListener        consoleLog;
  private FileLogListener           fileLog;

  /**
   * Constructor.
   *
   */
  public BasicLog(TechEmpowerApplication a)
  {
    super(a);

    // If there is no configuration file, then the Log will never be 
    // configured and we'll never see the error messages that tell us the 
    // configuration is missing.  So we create a default ConsoleLogListener 
    // to use while we're not configured.  Then in the configure() method 
    // this default listener will be removed before adding any new ones.
    this.consoleLog = new ConsoleLogListener(
        getApplication(),
        0 // Debug level
      );
    
    addListener(this.consoleLog);
    
    applicationLog = this;
  }

  /**
   * Returns the name of this listener.
   */
  @Override
  public String getName()
  {
    return "Basic Log";
  }

  /**
   * Configure this component.
   */
  @Override
  public void configure(EnhancedProperties props, Version version)
  {
    // A default debug threshold to use for all listeners if individual
    // thresholds are not defined.
    setDebugThreshold(props.getInt("Log.LogDebugThreshold", getDebugThreshold()));

    // Create an ArrayList of listeners.
    CopyOnWriteArrayList<LogListener> listeners = new CopyOnWriteArrayList<>();
    
    // Add a standard FileLogListener if requested.
    if (props.getBoolean("Log.File.On", false))
    {
      // Get LogFile settings.
      int fileDebugThreshold = props.getInt(
          "Log.File.LogDebugThreshold", getDebugThreshold());

      String logDirectory      = props.get("Log.File.LogDirectory", FileLogListener.DEFAULT_LOGFILE_DIR);
      String logFilenamePrefix = props.get("Log.File.LogFilenamePrefix", FileLogListener.DEFAULT_LOGFILE_PREFIX);
      String logFilenameSuffix = props.get("Log.File.LogFilenameSuffix", FileLogListener.DEFAULT_LOGFILE_SUFFIX);

      // Check to see if the log directory has been set at all.
      if (!props.has("Log.File.LogDirectory"))
      {
        System.out.println(COMPONENT_CODE +
                           ": No log directory has been specified.  Using default.");
        System.out.println(COMPONENT_CODE +
                           ": Default log directory: " + logDirectory);
      }
      else
      {
        // If the log directory has been specified, check if it exists.  If
        // it doesn't attempt to make the directory.
        File logDirectoryFile = new File(logDirectory);
        if (  (!logDirectoryFile.exists())
           || (!logDirectoryFile.isDirectory())
           )
        {
          // Try to create the directory.
          if (!(logDirectoryFile.mkdirs()))
          {
            System.out.println(COMPONENT_CODE + ": Log directory missing and cannot be created.");
          }
        }
      }

      this.fileLog = new FileLogListener(getApplication(), logDirectory,
          logFilenamePrefix, logFilenameSuffix, fileDebugThreshold);

      listeners.add(this.fileLog);
    }
    
    // Add a standard ConsoleLogListener if requested.
    if (props.getBoolean("Log.Console.On", false))
    {
      int consoleDebugThreshold = props.getInt(
          "Log.Console.LogDebugThreshold", getDebugThreshold());

      this.consoleLog = new ConsoleLogListener(getApplication(),
          consoleDebugThreshold);

      listeners.add(this.consoleLog);
    }
    
    // Assign the new Listeners to our member list.
    this.logListeners = listeners;

    // Inform the user that the Log has been configured.
    log(COMPONENT_CODE, "Log successfully configured.");

    this.configured = true;
    writePreConfigMessages();
  }
  
  @Override
  public ComponentLog getComponentLog(String componentCode)
  {
    return new ComponentLog(this, componentCode);
  }
  
  /**
   * Get the main application log reference.
   */
  public static Log getInstance()
  {
    return applicationLog;
  }

  /**
   * Messages sent to the log through debug(), log(), and assertion() before
   * the log is configured (and LogListeners registered) are cached.  Once the
   * log is configured, the cached messages are written to the log.
   */
  private void writePreConfigMessages()
  {
    if (this.preConfigMessages != null && this.preConfigMessages.size() > 0)
    {
      // Since we're using a default ConsoleLogListener we don't need to
      // rewrite the pre-config messages to the console. So temporarily remove
      // it.
      boolean reAddConsoleLog = false;
      if (this.consoleLog != null)
      {
        reAddConsoleLog = this.logListeners.contains(this.consoleLog);
        removeListener(this.consoleLog);
      }

      for (LogItem item : this.preConfigMessages)
      {
        int type = item.getType();
        if (type == LogItem.TYPE_LOG)
        {
          log(item.getComponentCode(), item.getLogString(), item.getLevel());
        }
        else if (type == LogItem.TYPE_ASSERTION)
        {
          // Assume that if the assertion was added to the pre-config messages
          // that the assertion test was true.
          assertion(item.getComponentCode(), true, item.getLogString(), 
              item.getLevel());
        }
      }

      // Re-add the ConsoleLogListener if it's defined.
      if (reAddConsoleLog)
      {
        addListener(this.consoleLog);
      }
    }
    this.preConfigMessages = null;
  }

  //
  // log, debug, assert methods
  //
  
  @Override
  public void log(String componentCode, String logString, int logLevel)
  {
    if (!this.configured && this.preConfigMessages != null)
    {
      this.preConfigMessages.add(
          new LogItem(componentCode, logString, LogItem.TYPE_LOG, logLevel)
          );
    }

    for (LogListener ll : getListeners())
    {
      ll.log(componentCode, logString, logLevel);
    }
  }

  /**
   * Calls the assertion method on each LogListener.
   */
  @Override
  public void assertion( String componentCode, boolean eval, String logString,
    int logLevel)
  {
    if (!eval)
    {
      if (!this.configured && this.preConfigMessages != null)
      {
        this.preConfigMessages.add(
            new LogItem(componentCode, logString, LogItem.TYPE_ASSERTION, logLevel)
            );
      }
    }

    for (LogListener ll : getListeners())
    {
      ll.assertion(componentCode, eval, logString, logLevel);
    }
  }

  //
  // end log, debug, assert methods
  //

  /**
   * Returns a reference to the Log Listeners.
   */
  protected List<LogListener> getListeners()
  {
    return this.logListeners;
  }
  
  /**
   * Return an iterator of the available log listeners.
   */
  @Override
  public List<LogListener> getLogListeners()
  {
    return new ArrayList<>(this.logListeners);
  }

  /**
   * Add a log listener.  This method allows an application to specify
   * a log listener beyond the normal three.
   */
  @Override
  public void addListener(LogListener listener)
  {
    this.logListeners.add(listener);
  }

  /**
   * Remove a log listener.  This method allows an application to remove
   * a log listener.  Returns true if the listener provided was found and 
   * removed; false otherwise.
   */
  @Override
  public boolean removeListener(LogListener listener)
  {
    return this.logListeners.remove(listener);
  }

  //
  // end utility methods
  //

}

// end BasicLog.
