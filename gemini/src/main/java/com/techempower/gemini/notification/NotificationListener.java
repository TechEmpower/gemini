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
 * A NotificationListener evaluates notifications as they arrive and can
 * act in any way on those notifications.  Notification processing occurs
 * in an asynchronous thread shared among all NotificationListeners.  As a 
 * result, it is not quite the case that operations should be immediate as
 * would be the case if the processing occurred within a web request thread.
 */
public interface NotificationListener
{

  /**
   * Process a notification.  This occurs within the Notifier's thread of 
   * execution (and not a web request) so it is permissible to take some time
   * processing the notification, within reason.
   */
  void processNotification(Notification notification, Notifier notifier);
  
  /**
   * Process the current history of notifications.  The Notifier will call
   * this method at a periodic interval (e.g., 10 minutes) and the Listeners
   * may do anything they want to in this method.  For example, a Listener
   * responsible for sending e-mail alerts for notifications may elect to
   * do so only when this method is called so that an e-mail is not sent
   * for every single notification.
   * 
   * @param history the "full" history of notifications retained by the 
   *        Notifier.
   * @param sinceLastInterval a sublist consisting only of notifications that
   *        have arrived since the last call to processHistory.
   * @param notifier a reference to the Notifier.
   */
  void processHistory(List<Notification> history, 
      List<Notification> sinceLastInterval, Notifier notifier);
  
}
