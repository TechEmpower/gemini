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

import java.io.*;
import java.util.*;

import com.techempower.data.*;
import com.techempower.gemini.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.scheduler.*;
import com.techempower.text.*;
import com.techempower.util.*;

/**
 * Automates routine backups of a MySQL 6.x database.  Note that the default
 * SQL statement issued by this tool are only compatible with MySQL 6.x.  The
 * statement can be customized, however.  Use setBackupStatement to set an
 * alternate statement, with $BFN to indicate where to put the backup filename
 * within the statement.
 */
public class MySqlBackupEvent
  extends    ScheduledEvent
  implements Configurable
{
  //
  // Constants.
  //
  
  public static final String LOCAL_COMPONENT_CODE = "eMyS";
  public static final String CONFIGURATION_PREFIX = "MySqlBackupEvent";
  public static final int    DEFAULT_MAXIMUM_BACKUPS = 10;
  
  //
  // Member variables
  //
  
  private final GeminiApplication app;
  private final ComponentLog log;

  private long   intervalMillis  = UtilityConstants.HOUR;
  private String backupPath      = "c:\\backups\\mysql\\";
  private String filenamePrefix  = "mysqlbackup-";
  private String filenameDateformat = "yyyy-MM-dd-HH-mm-ss-SSS";
  private String filenameSuffix  = ".bak";
  private String backupStatement = "BACKUP DATABASE * TO '$BFN';";
  private int    maximumBackups  = DEFAULT_MAXIMUM_BACKUPS;
  private SynchronizedSimpleDateFormat dateFormat;
  private Scheduler scheduler;
  
  //
  // Methods.
  //
  
  public MySqlBackupEvent(GeminiApplication application)
  {
    super("MySQL Backup Event", "Automates backup of a MySQL 6.x database.");
    
    this.app = application;
    this.log = this.app.getLog(LOCAL_COMPONENT_CODE);
    this.dateFormat = new SynchronizedSimpleDateFormat(this.filenameDateformat);
    application.getConfigurator().addConfigurable(this); 
  }
  
  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    setBackupPath(props.get(CONFIGURATION_PREFIX + ".Backuppath", getBackupPath()));
    setFilenamePrefix(props.get(CONFIGURATION_PREFIX + ".FilenamePrefix", getFilenamePrefix()));
    setFilenameSuffix(props.get(CONFIGURATION_PREFIX + ".FilenameSuffx", getFilenameSuffix()));
    setFilenameDateFormat(props.get(CONFIGURATION_PREFIX + ".FilenameDateFormat", getFilenameDateFormat()));
    this.dateFormat = new SynchronizedSimpleDateFormat(this.filenameDateformat);
    setBackupStatement(props.get(CONFIGURATION_PREFIX + ".BackupStatement", getBackupStatement()));
    setMaximumBackups(props.getInt(CONFIGURATION_PREFIX + ".MaximumBackups", getMaximumBackups()));
    
    long newInterval = props.getLong(CONFIGURATION_PREFIX + ".IntervalMillis", getIntervalMillis());
    if (newInterval != getIntervalMillis())
    {
      setIntervalMillis(newInterval);
      
      // In the event that the interval has changed, reschedule this event.
      // Note that doing so will reset the next scheduled backup to the
      // current time + the interval amount.
      this.scheduler = this.app.getScheduler();
      this.scheduler.removeEvent(this);
      
      // Schedule the event.
      scheduleSelf(this.scheduler);
    }
  }
  
  /**
   * Gets a default scheduled time for this event.
   */
  @Override
  public long getDefaultScheduledTime()
  {
    // Get a current-time SimpleDate.
    Calendar schedule = DateHelper.getCalendarInstance();

    // Add the current interval.
    schedule.add(Calendar.SECOND, (int)(this.intervalMillis / UtilityConstants.SECOND));

    return schedule.getTimeInMillis();
  }

  /**
   * Schedules this event.
   */
  public void scheduleSelf(Scheduler applicationScheduler)
  {
    // Get a current-time SimpleDate.
    Calendar schedule = DateHelper.getCalendarInstance();

    // Add the current interval.
    schedule.add(Calendar.SECOND, (int)(this.intervalMillis / UtilityConstants.SECOND));
    
    applicationScheduler.scheduleEvent(this, schedule.getTimeInMillis());
  }
  
  /**
   * Overload this method to return true if this event needs to be run
   * on a thread separate from the Scheduler itself.  By default, events
   * get run ON the scheduler thread.
   */
  @Override
  public boolean requiresOwnThread()
  {
    return true;
  }

  /**
   * Executes this event.
   */
  @Override
  public void execute(Scheduler applicationScheduler, boolean onDemandExecution)
  {
    try (
        DatabaseConnector dbconn = this.app.getConnectorFactory().getConnector()
        )
    {
      String backupFilename = getFilenamePrefix() 
        + this.dateFormat.format(new Date())
        + getFilenameSuffix();
      
      HashMap<String,Object> macros = new HashMap<>(1);
      macros.put("$BFN", backupFilename);
      
      dbconn.setQuery(StringHelper.macroExpand(macros, getBackupStatement()));
      this.log.log("Executing backup of MySQL to " + backupFilename);
      dbconn.runQuery();
      this.log.log("Backup of MySQL to " + backupFilename + " complete.");
      
      cleanOldFiles();
    }
    catch (Exception exc)
    {
      this.log.log("Exception during database backup: " + exc);
    }

    // Log the event completion.
    this.log.log(this + " complete.");

    scheduleSelf(applicationScheduler);
  }
  
  /**
   * Cleans up old backup files.  Retains only the maximum number of backup
   * files, keeping the newest files.
   */
  public void cleanOldFiles()
  {
    // Specifying 0 maximum backup files means that we retain an infinite
    // number.
    if (getMaximumBackups() > 0)
    {
      try
      {
        File directory = new File(getBackupPath());
        
        // Find previous backup files.
        File[] files = directory.listFiles(new FilenameFilter()
        {
          @Override
          public boolean accept(File dir, String name)
          {
            return name.startsWith(getFilenamePrefix());
          }
        });

        // Proceed if we have more backup files than we're permitted to
        // retain.
        if (files.length > getMaximumBackups())
        {
          // Sort the array by modification date.
          Arrays.sort(files, MODIFICATION_DATE_FILE_COMPARATOR);
          
          // Keep all but the most recent.
          for (int i = 0; i < (files.length - getMaximumBackups()); i++)
          {
            this.log.log("Removing old backup file: " + files[i]);
            if (!files[i].delete())
            {
              this.log.log("Could not delete " + files[i]);
            }
          }
        }
      }
      catch (Exception exc)
      {
        this.log.log("Exception while cleaning up old backup files: " + exc);
      }
    }
  }
  
  /**
   * Just used to sort files by modification date.  Sonar hates anonymous
   * inner classes, so here we are.
   */
  private static final ModificationDateFileComparator 
    MODIFICATION_DATE_FILE_COMPARATOR = new ModificationDateFileComparator();
  static class ModificationDateFileComparator
    implements Comparator<File>
  {
    @Override
    public int compare(File o1, File o2)
    {
      if (o1.lastModified() < o2.lastModified())
      {
        return -1;
      }
      else
      {
        return 1;
      }
    } 
  }

  //
  // Getters and setters.
  //
  
  /**
   * @return the intervalMillis
   */
  public long getIntervalMillis()
  {
    return this.intervalMillis;
  }

  /**
   * @param intervalMillis the intervalMillis to set
   */
  public void setIntervalMillis(long intervalMillis)
  {
    this.intervalMillis = NumberHelper.boundLong(
        intervalMillis,
        UtilityConstants.MINUTE,
        Long.MAX_VALUE);
  }

  /**
   * @return the filenamePrefix
   */
  public String getFilenamePrefix()
  {
    return this.filenamePrefix;
  }

  /**
   * @param filenamePrefix the filenamePrefix to set
   */
  public void setFilenamePrefix(String filenamePrefix)
  {
    this.filenamePrefix = filenamePrefix;
  }

  /**
   * @return the filenameDateformat
   */
  public String getFilenameDateFormat()
  {
    return this.filenameDateformat;
  }

  /**
   * @param filenameDateFormat the filenameDateformat to set
   */
  public void setFilenameDateFormat(String filenameDateFormat)
  {
    this.filenameDateformat = filenameDateFormat;
    this.dateFormat = new SynchronizedSimpleDateFormat(filenameDateFormat);
  }

  /**
   * @return the filenameSuffix
   */
  public String getFilenameSuffix()
  {
    return this.filenameSuffix;
  }

  /**
   * @param filenameSuffix the filenameSuffix to set
   */
  public void setFilenameSuffix(String filenameSuffix)
  {
    this.filenameSuffix = filenameSuffix;
  }

  /**
   * @return the backupStatement
   */
  public String getBackupStatement()
  {
    return this.backupStatement;
  }

  /**
   * @param backupStatement the backupStatement to set.  Use $BFN to indicate
   * the place to insert the backup filename.
   */
  public void setBackupStatement(String backupStatement)
  {
    this.backupStatement = backupStatement;
  }

  /**
   * @return the backupPath
   */
  public String getBackupPath()
  {
    return this.backupPath;
  }

  /**
   * @param backupPath the backupPath to set
   */
  public void setBackupPath(String backupPath)
  {
    this.backupPath = backupPath;
  }

  /**
   * @return the maximumBackups
   */
  public int getMaximumBackups()
  {
    return this.maximumBackups;
  }

  /**
   * @param maximumBackups the maximumBackups to set
   */
  public void setMaximumBackups(int maximumBackups)
  {
    this.maximumBackups = maximumBackups;
  }
  
}  // End MySqlBackupEvent.
