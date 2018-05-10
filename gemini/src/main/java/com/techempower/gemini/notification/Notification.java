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

/**
 * A Notification represents some "exceptional" situation that is present
 * within the web application.  An example may be a database connectivity
 * problem or an unusually high number of concurrent threads.
 */
public interface Notification
{

  /**
   * The severity of a notification.
   */
  enum Severity { LOW, MEDIUM, HIGH }

  /**
   * Gets the source or type of the notification.  This is used to categorize
   * "like" Notifications such that if Notification summarization is enabled,
   * skipped Notifications can be grouped.
   *   <p>
   * The source should be a plain-text single word with no spaces.  Examples:
   * "Monitor", "Exception"
   */
  String getSource();

  /**
   * Gets the notification synopsis.  Attempt to keep this as brief as 
   * possible, ideally less than one line of text so that it can fit in an
   * e-mail or text message.
   */
  String getSynopsis();
  
  /**
   * Gets the full notification in a ready-to-render text form.  This full
   * version may be included in an e-mail or possibly written to a log file.
   * It can be as large as necessary to be of value.
   *   <p>
   * Details are optional.  If null is returned, only the Synopsis should be
   * used by NotificationListeners.
   */
  String getDetails();
  
  /**
   * Gets the creation date and time of this Notification.
   */
  Date getTime();
  
  /**
   * Gets the severity level of this Notification.
   */
  Severity getSeverity();
  
  /**
   * Purges whatever details were gathered.  This can be ignored, but for
   * any Notifications that have a potentially large Details payload, it may
   * be worthwhile to nullify references to the large payload.  This will
   * be called by the Notifier when this Notification has been processed
   * by the listeners and is being stored in a recent history of 
   * notifications.  Once a Notification is stored into the history, details
   * are no longer used.
   *    <p>
   * Purging is simply a matter of setting any reference to details to null,
   * and returning null from the getDetails() method.
   */
  void purgeDetails();
  
  /**
   * Sets the sequence number for this notification.  This is assigned by the
   * Notifier as it consumes inbound notifications.  This can simply set
   * a member variable.
   */
  void setSequenceNumber(int sequenceNumber);
  
  /**
   * Gets the sequence number previously set by setSequenceNumber.  This won't
   * be called prior to setting a value; so it's okay to return 0 until the
   * value is set.
   */
  int getSequenceNumber();
  
}
