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
import com.techempower.helper.*;

/**
 * A basic implementation of LogListener.  This implementation saves each log
 * entry over the debug threshold into a file.  The file location is determined
 * by config parameters passed to BasicLog.  
 * 
 * BasicLog starts with three LogListeners; FileLogListener (this one), 
 * GraphicLogListener, and ConsoleLogListener.  There is no need to install
 * it into BasicLog unless you want ANOTHER FileLogListener.
 */
public class FileLogListener 
     extends AbstractLogListener 
  implements LogWriter
{
  
  public static final int    ITEM_BUFFER_MARGIN     = 40;

  public static final String DEFAULT_LOGFILE_DIR    = "logs";
  public static final String DEFAULT_LOGFILE_EXT    = ".log";
  public static final String DEFAULT_LOGFILE        = "log"
                                                      + DEFAULT_LOGFILE_EXT;
  public static final String DEFAULT_LOGFILE_PREFIX = "";
  public static final String DEFAULT_LOGFILE_SUFFIX = "";

  private String           logFilenamePre         = DEFAULT_LOGFILE_PREFIX;
  private String           logFilenameSuf         = DEFAULT_LOGFILE_SUFFIX;
  private String           logFilename            = DEFAULT_LOGFILE;
  private String           logDirectory           = DEFAULT_LOGFILE_DIR
                                                      + File.separator;
  private FileWriter       fileWriter             = null;
  
  private final ConcurrentLinkedQueue<String> queue     = new ConcurrentLinkedQueue<>();

  /**
   * Constructor for FileLogListener.
   * 
   * @param directory the directory for the log files
   * @param threshold the debug threshold for this listener
   */
  public FileLogListener(TechEmpowerApplication application,
    String directory, String filenamePrefix, String filenameSuffix,
    int threshold)
  {
    super(application);
    
    this.logFilename = generateFilename();
    setLogfileDirectory(directory);
    setLogFilenamePrefix(filenamePrefix);
    setLogFilenameSuffix(filenameSuffix);
    setDebugThreshold(threshold);

    // Get the LogWriterCloser to monitor this LogWriter.  This allows
    // us to not worry about closing the file.
    application.getLogManager().addLogWriter(this, application.getVersion().getProductName() + " debug log");
  }

  /**
   * Returns the name of this listener.
   */
  @Override
  public String getName()
  {
    return "File Log";
  }

  //
  // log methods
  //
  
  /**
   * Log a string to the log file. Also displays to console. Takes a priority
   * level and <i>does not</i> log this item if the priority level is less
   * than the debug level threshold. Do not call this method directly; it is
   * for a Log class to call.
   * 
   * @param componentCode a four-letter component code of the caller.
   * @param logString the string to write into the file.
   * @param debugLevel the priority level.
   */
  @Override
  public void log(String componentCode, String logString, int debugLevel)
  {
    // Is the priority of the item being logged higher or equal to the
    // debugging threshold set for this Log? If not, ignore this item.
    if (debugLevel >= getDebugThreshold())
    {
      // Strip out carriage returns and other control characters that may
      // mess up our nicely formatted log.
      String toLog = StringHelper.stripISOControlCharacters(logString, " ");
      
      // Update the time in the calendar.
      computeTimestamps();

      // Build the line.
      StringBuilder buffer = new StringBuilder(ITEM_BUFFER_MARGIN + toLog.length());
      buffer.append(getApplication().getVersion().getProductCode());
      buffer.append(' ');
      buffer.append(getFullTimestamp());
      buffer.append(' ');
      buffer.append(componentCode);
      buffer.append(": ");
      buffer.append(toLog);
      buffer.append("\r\n");

      // Queue this item to be written.
      this.queue.add(buffer.toString());
    }
  }

  //
  // end log methods
  //

  //
  // file methods
  //

  /**
   * Closes the log file.  It will be reopened by writeLogEntry the next 
   * time a log entry needs to be written.  This method will be called
   * by the LogFileCloser.
   */
  @Override
  public void closeFile()
  {
    try
    {
      if (this.fileWriter != null)
      {
        this.fileWriter.close();
      }
    }
    catch (IOException ioexc)
    {
      debug(Log.COMPONENT_CODE, "IOException while closing log file!");
    }
    this.fileWriter = null;
  }

  /**
   * Closes the log file.  Before closing, first writes a single line to 
   * the file.
   */
  @Override
  public void closeFile(String statement)
  {
    closeFile();
  }

  /**
   * Is the log file open?
   */
  @Override
  public boolean isOpen()
  {
    return (this.fileWriter != null);
  }

  /**
   * Sets the filename prefix for Logs created by this logging component.  
   * The prefix will be appended prior to the date portion of the filename.
   *
   * @param prefixString the prefix to append to filenames.
   */
  private void setLogFilenamePrefix(String prefixString)
  {
    this.logFilenamePre = prefixString;
  }

  /**
   * Sets the filename suffix for Logs created by this logging component.  
   * The suffix will be appended after the date portion of the filename
   * but prior to the file's extension.
   *
   * @param suffixString the suffix to append to filenames.
   */
  private void setLogFilenameSuffix(String suffixString)
  {
    this.logFilenameSuf = suffixString;
  }

  /**
   * Sets the logfile directory.
   *
   * @param dir the directory to use for log files.
   */
  private void setLogfileDirectory(String dir)
  {
    if (dir.endsWith(File.separator))
    {
      this.logDirectory = dir;
    }
    else
    {
      this.logDirectory = dir + File.separator;
    }
  }
  
  /**
   * Checks to see if a new file needs to be opened.
   */
  private boolean needNewFile()
  {
    return super.pastEndOfDay();
  }
  
  /**
   * Flush the contents of the log item queue.
   */
  @Override
  public boolean flushFile()
  {
    // Only proceed if we need to write something (we have something in
    // the queue).
    String logItem = this.queue.poll();
    if (logItem != null)
    {
      // Create a new file if necessary.
      if (needNewFile())
      {
        this.logFilename = generateFilename();
        closeFile();
  
        debug(Log.COMPONENT_CODE, "New log file: " + this.logFilename);
      }
  
      // Open a new file if necessary.
      if (this.fileWriter == null)
      {
        openFile(this.logDirectory + this.logFilename);
      }
  
      // Just a sanity check that we have a valid file writer reference.
      if (this.fileWriter != null)
      {
        try
        {
          // Write everything we have in the queue.
          while (logItem != null)
          {
            this.fileWriter.write(logItem);
            logItem = this.queue.poll();
          }
  
          // Flush.
          this.fileWriter.flush();
          return true;
        }
        catch (IOException ioexc)
        {
          System.out.println("Cannot write to log file. " + ioexc);
        }
      }
    }
    
    return false;
  }

  /**
   * Opens the fileWriter and initializes the fileWriter object.
   */
  private void openFile(String filename)
  {
    // Open for append.
    try
    {
      this.fileWriter = new FileWriter(filename, true);
    }
    catch (IOException ioexc)
    {
      System.out.println(Log.COMPONENT_CODE + "Cannot open log: " + filename);
    }
  }
  
  /**
   * Generate a new filename.
   */
  private String generateFilename()
  {
    // Use a local calendar instance here.
    Calendar fnCal = DateHelper.getCalendarInstance();

    StringBuilder buffer = new StringBuilder(30);
    
    buffer.append(this.logFilenamePre);
    buffer.append(StringHelper.padZero(fnCal.get(Calendar.YEAR), 4));
    buffer.append('-');
    buffer.append(StringHelper.padZero(fnCal.get(Calendar.MONTH) + 1, 2));
    buffer.append('-');
    buffer.append(StringHelper.padZero(fnCal.get(Calendar.DAY_OF_MONTH), 2));
    buffer.append(this.logFilenameSuf);
    buffer.append(DEFAULT_LOGFILE_EXT);

    return buffer.toString();
  }

  //
  // end file methods
  //

}  // End FileLogListener.
