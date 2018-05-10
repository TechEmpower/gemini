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

package com.techempower.gemini.notification;

import java.util.*;

import com.techempower.helper.*;

/**
 * A simple implementation of the Notification interface.  Instances of this
 * class are constructed by the Notifier if notifications are received as
 * Strings rather than fully-constructed Notification objects.
 */
public class BasicNotification
  implements Notification
{
  
  //
  // Member variables.
  //
  
  private final String   source;
  private final String   synopsis;
  private       String   details;
  private final Date     time;
  private final Severity severity;
  private       int      sequenceNumber;
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   * 
   * @param source A String identifier for the source of this notification.
   * @param synopsis A brief description of the notification.
   * @param details A longer description of the notification.
   * @param severity A Severity level (LOW, MEDIUM, or HIGH).
   */
  public BasicNotification(String source, String synopsis, String details,
      Severity severity)
  {
    this.source = source;
    this.synopsis = synopsis;
    this.details = details;
    this.time = new Date();
    this.severity = severity;
  }

  /**
   * Constructor.  Assumes MEDIUM Severity.
   * 
   * @param source A String identifier for the source of this notification.
   * @param synopsis A brief description of the notification.
   * @param details A longer description of the notification.
   */
  public BasicNotification(String source, String synopsis, String details)
  {
    this(source, synopsis, details, Severity.MEDIUM);
  }

  @Override
  public String getSource()
  {
    return this.source;
  }

  @Override
  public String getSynopsis()
  {
    return this.synopsis;
  }

  @Override
  public String getDetails()
  {
    return this.details;
  }

  @Override
  public Severity getSeverity()
  {
    return this.severity;
  }
  
  @Override
  public Date getTime()
  {
    return DateHelper.copy(this.time);
  }

  @Override
  public void purgeDetails()
  {
    this.details = null;
  }

  /**
   * @return the sequenceNumber
   */
  @Override
  public int getSequenceNumber()
  {
    return this.sequenceNumber;
  }

  /**
   * @param sequenceNumber the sequenceNumber to set
   */
  @Override
  public void setSequenceNumber(int sequenceNumber)
  {
    this.sequenceNumber = sequenceNumber;
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "Notification [" + getSequenceNumber() 
        + "; " + DateHelper.STANDARD_TECH_FORMAT.format(getTime()) 
        + "; from " + getSource() 
        + "; synopsis: " + getSynopsis()
        + "]";
  }
  
}
