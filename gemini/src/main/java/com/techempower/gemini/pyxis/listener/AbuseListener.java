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

package com.techempower.gemini.pyxis.listener;

import com.techempower.gemini.*;
import com.techempower.gemini.notification.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.password.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors login attempts and notifies administrators of especially
 * suspicious activity.
 */
public class AbuseListener
  implements SecurityListener<Context>,
             Configurable
{
  
  //
  // Constants.
  //
  
  public static final String DEFAULT_PROPS_PREFIX = "AbuseListener.";
  public static final int    THRESHOLD_MINUTE     = 20;
  public static final int    THRESHOLD_HOUR       = 200;
  public static final int    THRESHOLD_DAY        = 2000;
  public static final String CRLF                 = UtilityConstants.CRLF;
  
  //
  // Member variables.
  //
  
  private final GeminiApplication application;
  private final Logger            log = LoggerFactory.getLogger(getClass());
  private final String            propsPrefix;

  private int                     thresholdMinute      = THRESHOLD_MINUTE;
  private int                     thresholdHour        = THRESHOLD_HOUR;
  private int                     thresholdDay         = THRESHOLD_DAY;
  private int                     countMinute          = 0;
  private int                     countHour            = 0;
  private int                     countDay             = 0;
  private long                    nextMinute           = 0L;
  private long                    nextHour             = 0L;
  private long                    nextDay              = 0L;
  private long                    lastAlert            = 0L;
  private final long              alertInterval        = UtilityConstants.HOUR;
  private int                     alertsSinceLast      = 0;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public AbuseListener(GeminiApplication application, String propsPrefix)
  {
    this.application = application;
    if (StringHelper.isNonEmpty(propsPrefix))
    {
      this.propsPrefix = propsPrefix;
    }
    else
    {
      this.propsPrefix = DEFAULT_PROPS_PREFIX;
    }
    
    // Get configured.
    application.getConfigurator().addConfigurable(this);
  }

  /**
   * A login attempt failed.
   */
  @Override
  public void loginFailed(Context context)
  {
    long current = System.currentTimeMillis();
    
    // Check login failures per minute.
    if (current > this.nextMinute)
    {
      this.nextMinute = current + UtilityConstants.MINUTE;
      this.countMinute = 0;
    }
    
    this.countMinute++;
    
    if (this.countMinute > this.thresholdMinute)
    {
      processAlert("Excessive login failures in past minute.  " + this.countMinute + " failure(s) tracked so far.");
    }
    
    // Check login failures per hour.
    if (current > this.nextHour)
    {
      this.nextHour = current + UtilityConstants.HOUR;
      this.countHour = 0;
    }
    
    this.countHour++;
    
    if (this.countHour > this.thresholdHour)
    {
      processAlert("Excessive login failures in past hour.  " + this.countHour + " failure(s) tracked so far.");
    }

    // Check login failures per day.
    if (current > this.nextDay)
    {
      this.nextDay = current + UtilityConstants.DAY;
      this.countDay = 0;
    }
    
    this.countDay++;
    
    if (this.countDay > this.thresholdDay)
    {
      processAlert("Excessive login failures in past day.  " + this.countDay + " failure(s) tracked so far.");
    }
  }
  
  /**
   * Process an alert.
   */
  protected synchronized void processAlert(String alert)
  {
    log.info(alert);
    
    long current = System.currentTimeMillis();

    // Only send an alert if we've not sent an alert in the past interval.
    if (current > this.lastAlert + this.alertInterval)
    {
      StringBuilder buff = new StringBuilder(1000);

      buff.append("Security Alert: ")
          .append(alert)
          .append(CRLF);
      if (this.alertsSinceLast > 0)
      {
        buff.append("----")
            .append(CRLF);
        buff.append(this.alertsSinceLast)
            .append(" alert")
            .append(StringHelper.pluralize(this.alertsSinceLast))
            .append(" skipped since last report")
            .append(CRLF);
        this.alertsSinceLast = 0;
      }

      // Send the notification.
      BasicNotification notification = new BasicNotification("abuse", alert, buff.toString(), Notification.Severity.LOW);
      this.application.getNotifier().addNotification(notification);
      
      this.lastAlert = current;
    }
    // We've already sent an alert recently, so we'll just count that we
    // had an alert to process but ignored it.
    else
    {
      this.alertsSinceLast++;
    }
  }

  @Override
  public void loginSuccessful(Context context, PyxisUser user)
  {
    // Does nothing here.
  }

  @Override
  public void logoutSuccessful(Context context, PyxisUser user)
  {
    // Does nothing here.
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    this.thresholdMinute = props.getInt(this.propsPrefix + "ThresholdMinute", this.thresholdMinute);
    this.thresholdHour = props.getInt(this.propsPrefix + "ThresholdHour", this.thresholdHour);
    this.thresholdDay = props.getInt(this.propsPrefix + "ThresholdDay", this.thresholdDay);
  }

  @Override
  public void passwordChanged(PasswordProposal proposal)
  {
  }

}   // End AbuseListener.
