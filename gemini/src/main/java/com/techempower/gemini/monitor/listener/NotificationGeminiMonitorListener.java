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

import com.techempower.gemini.*;
import com.techempower.gemini.monitor.*;
import com.techempower.gemini.monitor.cpupercentage.*;
import com.techempower.gemini.monitor.health.*;
import com.techempower.gemini.notification.*;
import com.techempower.util.*;

/**
 * An implementation of GeminiMonitorListener that analyzes events to send
 * administrative Notifications via the Gemini Notifier.
 */
public class NotificationGeminiMonitorListener
  implements GeminiMonitorListener,
             Configurable
{
  
  //
  // Constants.
  //
  
  public static final String SOURCE = "Monitor";
  
  //
  // Member variables.
  //
  
  private final GeminiApplication application;
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public NotificationGeminiMonitorListener(GeminiApplication application)
  {
    this.application = application;
    
    application.getConfigurator().addConfigurable(this);
  }
  
  @Override
  public void configure(EnhancedProperties props)
  {
    // Configure this component.
  }

  @Override
  public void healthSnapshotExceptional(HealthSnapshot snapshot)
  {
    String synopsis = "Health - " + snapshot.getEvaluationString();
    String details = MonitorListenerHelper.renderHealthSnapshot(snapshot);
    
    // Build and send a notification.
    sendNotification(synopsis, details, Notification.Severity.HIGH);
  }

  @Override
  public void cpuUtilizationIntervalExceptional(PercentageInterval interval)
  {
    String synopsis = "CPU Utilization - " + interval.getEvaluation();
    String details = MonitorListenerHelper.renderCpuUtilization(interval);

    // Build and send a notification.
    sendNotification(synopsis, details, Notification.Severity.MEDIUM);
  }
  
  /**
   * Sends a notification.
   */
  protected void sendNotification(String synopsis, String details, 
      Notification.Severity severity)
  {
    final BasicNotification n = new BasicNotification(
        SOURCE, synopsis, details, severity);
    this.application.getNotifier().addNotification(n);
  }

}
