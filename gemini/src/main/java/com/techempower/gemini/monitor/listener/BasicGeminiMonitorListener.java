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

package com.techempower.gemini.monitor.listener;

import static com.techempower.util.UtilityConstants.*;

import java.io.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.monitor.*;
import com.techempower.gemini.monitor.cpupercentage.*;
import com.techempower.gemini.monitor.health.*;
import com.techempower.helper.*;
import com.techempower.text.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple GeminiMonitorListener that writes alerts to the file system.
 */
public class BasicGeminiMonitorListener
  implements GeminiMonitorListener,
             Configurable
{
  
  //
  // Constants.
  //
  
  public static final SynchronizedSimpleDateFormat DATE_FORMAT = new SynchronizedSimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS");
  public static final String CONFIGURATION_PREFIX = "SimpleGeminiMonitorListener.";

  //
  // Member variables.
  //
  
  private final Logger log = LoggerFactory.getLogger(getClass());
  
  private String fsLocation;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public BasicGeminiMonitorListener(GeminiApplication application)
  {
    application.getConfigurator().addConfigurable(this);
  }
 
  @Override
  public void healthSnapshotExceptional(HealthSnapshot snapshot)
  {
    final Calendar now = DateHelper.getCalendarInstance();
    
    StringBuilder alert = new StringBuilder();
    alert.append(getStandardHeader("Health Exception", now));
    alert.append(snapshot.getEvaluationString())
         .append(CRLF)
         .append(CRLF);
    alert.append(MonitorListenerHelper.renderHealthSnapshot(snapshot));
    writeAlert(alert.toString(), now);
  }

  @Override
  public void cpuUtilizationIntervalExceptional(PercentageInterval interval)
  {
    final Calendar now = DateHelper.getCalendarInstance();
    
    StringBuilder alert = new StringBuilder();
    alert.append(getStandardHeader("CPU Utilization Exception", now));
    alert.append(MonitorListenerHelper.renderCpuUtilization(interval));    
    writeAlert(alert.toString(), now);
  }
  
  /**
   * Writes an alert to the file system.
   */
  protected void writeAlert(String content, Calendar now)
  {
    final String filename = this.fsLocation + File.separator + "alert-" 
        + DATE_FORMAT.format(now.getTime()) + ".txt";
    try
    {
      try (FileWriter fw = new FileWriter(filename))
      {
        fw.write(content);
      }
    }
    catch (IOException ioexc)
    {
      this.log.warn("Could not write alert file: {}", filename, ioexc);
    }
  }
  
  /**
   * Creates a standard header for an alert file.
   */
  protected String getStandardHeader(String type, Calendar now)
  {
    return "Gemini Monitor Alert - " + type + CRLF
      + DATE_FORMAT.format(now.getTime())
      + CRLF
      + CRLF;
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    // Create the directory to store alerts.
    String loc = props.get("Servlet.ApplicationRoot") + "monitor-alerts";
    loc = props.get(CONFIGURATION_PREFIX + "Location", loc);
    this.fsLocation = loc;
    File file = new File(loc);
    if (!file.mkdirs())
    {
      this.log.info("Could not create directory: {}", this.fsLocation);
    }
  }
  
}
