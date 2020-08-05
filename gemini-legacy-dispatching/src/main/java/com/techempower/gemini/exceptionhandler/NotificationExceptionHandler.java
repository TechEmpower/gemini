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
package com.techempower.gemini.exceptionhandler;

import java.util.concurrent.atomic.*;

import com.techempower.gemini.*;
import com.techempower.gemini.notification.*;
import com.techempower.util.*;

/**
 * Converts application Exceptions into Administrative Notifications, which
 * are in turn viewable in the Administration site and may also be sent
 * via e-mail.  This makes this a viable alternative to the 
 * EmailExceptionHandler when the Notifier is being used.
 *   <p>
 * Configurable options:
 *   <ul>
 *   <li>NotificationExceptionHandler.MinimumInterval - A minimum number of 
 *       seconds that must elapse between sending Notifications.  If
 *       additional exceptions occur during this interval, they will be
 *       counted but not detailed.  The default is 120 seconds.</li>
 *   </ul>
 */
public class NotificationExceptionHandler
  implements ExceptionHandler,
             Configurable
{

  //
  // Constants.
  //
  
  public static final String SOURCE = "Exception";
  public static final int    DEFAULT_INTERVAL_SECONDS = 120;
  
  //
  // Variables.
  //
  
  private final GeminiApplication application;
  private final AtomicInteger     skipped = new AtomicInteger(0);
  
  private long              minimumInterval = 2L * UtilityConstants.MINUTE;
  private long              nextDelivery = 0L;

  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public NotificationExceptionHandler(GeminiApplication application)
  {
    this.application = application;
    
    application.getConfigurator().addConfigurable(this);
  }
  
  @Override
  public void handleException(Context context, Throwable exc)
  {
    handleException(context, exc, null);
  }

  @Override
  public void handleException(Context context, Throwable exc,
      String description)
  {
    final long currentTime = System.currentTimeMillis();
    if (currentTime < this.nextDelivery)
    {
      // Skip this exception.
      skipped.incrementAndGet();
    }
    else
    {
      // Render a synopsis and detailed description.
      final int skip = skipped.getAndSet(0);
      final String synopsis = exc.getClass().getName() 
          + (skip > 0 ? " (" + skip + " skipped)" : "");
      final String details = ExceptionHandlerHelper.renderExceptionAsReport(
          context, null, exc, description, skip);
      
      // Create a notification and send it off.
      final Notification n = new BasicNotification(SOURCE, synopsis, details);
      application.getNotifier().addNotification(n);
      
      // Set the next delivery threshold time.
      nextDelivery = currentTime + minimumInterval;
    }
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    minimumInterval = props.getInt(
        "NotificationExceptionHandler.MinimumInterval", DEFAULT_INTERVAL_SECONDS) 
        * UtilityConstants.SECOND;
  }

}
