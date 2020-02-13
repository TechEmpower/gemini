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
import java.util.concurrent.atomic.*;

import com.techempower.asynchronous.*;
import com.techempower.gemini.*;
import com.techempower.thread.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Notifier along with its suite of listeners is responsible for 
 * collecting, processing, and ultimately sending out notifications to system
 * administrators of exceptional conditions within the web application.
 *   <p>
 * The basic model is simple: the Notifier manages a list of Notifications
 * that are constructed/gathered by its suite of listeners.  This list is
 * in turn evaluated by NotificationListeners.  The basic implementation
 * of NotificationListener provided in this package sends alerts to system
 * administrators via e-mail.
 *   <p>
 * Configurable options:
 *   <ul>
 *   <li>Notifier.History - How many Notifications to retain in memory; 
 *       default is 50.</li>
 *   <li>Notifier.HistoryDetails - How many Notifications should retain their
 *       full details; can be set lower than the above value, but the default
 *       is also 50.</li>
 *   <li>Notifier.HistoryProcessIntervalSeconds - How often to have the
 *       Listeners evaluate the notification history.  The default is every
 *       10 minutes.</li>
 *   </ul>
 */
public class Notifier
  implements Configurable,
             Asynchronous
{

  //
  // Constants.
  //
  
  public static final String COMPONENT_CODE = "Ntfr";
  public static final String CONFIGURATION_PREFIX = "Notifier.";
  
  public static final int DEFAULT_HISTORY_SIZE = 50;
  public static final int DEFAULT_HISTORY_DETAIL_SIZE = 50;
  public static final int DEFAULT_HISTORY_PROCESS_INTERVAL = 600; // 10 minutes
  public static final int DEFAULT_LIST_SIZE = 10;
  
  //
  // Member variables.
  //
  
  private final GeminiApplication app;
  private final Logger            log = LoggerFactory.getLogger(COMPONENT_CODE);
  private final NotifierThread    thread;
  
  private NotificationListener[] listeners;
  
  private int historySize = DEFAULT_HISTORY_SIZE;
  private int historyDetails = DEFAULT_HISTORY_DETAIL_SIZE;
  private int historyProcessInterval = DEFAULT_HISTORY_PROCESS_INTERVAL;
  private int lastHistoryProcess = 0;
  
  private final AtomicInteger sequencer = new AtomicInteger(0);
  
  private final List<Notification> inbound = new ArrayList<>(DEFAULT_LIST_SIZE);
  private final List<Notification> history = new ArrayList<>(this.historySize);

  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public Notifier(GeminiApplication app)
  {
    this.app = app;
    this.listeners = new NotificationListener[0];
    this.thread = new NotifierThread();

    app.getConfigurator().addConfigurable(this);
    app.addAsynchronous(this);
  }
  
  /**
   * Accepts a new Notification, adding it to the inbound queue.  This queue
   * is periodically consumed by the NotificationConsumptionThread.
   *   <p>  
   * Components that generate notifications can submit them at will, but by
   * convention notifications should not be numerous.  It's recommended that
   * at most 1 notification be sent per hour to reduce notification spam
   * in the event of a severe problem.
   */
  public void addNotification(Notification notification)
  {
    synchronized (this.inbound)
    {
      this.inbound.add(notification);
    }
  }
  
  /**
   * Adds a new notification for a Throwable.
   * 
   * @param source A brief String identifying the source; avoid spaces.
   * @param throwable An exception or error.
   */
  public void addNotification(String source, Throwable throwable)
  {
    addNotification(new ThrowableNotification(source, app, throwable));
  }
  
  /**
   * Adds a new notification provided as simple Strings.  A BasicNotification
   * object will be constructed here.  Details are optional. 
   *   <p>  
   * Components that generate notifications can submit them at will, but by
   * convention notifications should not be numerous.  It's recommended that
   * at most 1 notification be sent per hour to reduce notification spam
   * in the event of a severe problem.
   * 
   * @param source A brief String identifying the source; avoid spaces.
   * @param synopsis A brief synopsis of the notification.
   * @param details Any details for the notification.
   * @param severity the severity level of the notification (LOW, MEDIUM, or 
   *        HIGH).
   */
  public void addNotification(String source, String synopsis, String details,
      Notification.Severity severity)
  {
    BasicNotification not = new BasicNotification(source, synopsis, details, severity);
    addNotification(not);
  }
  
  /**
   * Adds a new notification provided as simple Strings.  A MEDIUM severity 
   * BasicNotification object will be constructed here.  Details are optional. 
   * 
   * @param source A brief String identifying the source; avoid spaces.
   * @param synopsis A brief synopsis of the notification.
   * @param details Any details for the notification.
   */
  public void addNotification(String source, String synopsis, String details)
  {
    BasicNotification not = new BasicNotification(source, synopsis, details);
    addNotification(not);
  }
  
  /**
   * Consumes the inbound queue of Notifications, producing a new List data
   * structure provided as a return value.
   */
  protected List<Notification> consumeInbound()
  {
    // Let's add one here just in case one item gets queued before we acquire
    // the synchronization lock.
    ArrayList<Notification> toReturn = new ArrayList<>(this.inbound.size() + 1);

    // Do this business.
    synchronized (this.inbound)
    {
      toReturn.addAll(this.inbound);
      this.inbound.clear();
    }
    
    return toReturn;
  }
  
  /**
   * Processes the inbound queue.  This occurs roughly every 5 seconds to
   * capture and process any notifications "as they occur" and distribute
   * them to the listeners.
   */
  protected void processQueue()
  {
    if (this.inbound.size() > 0)
    {
      List<Notification> toProcess = consumeInbound();
      
      // Do something with the notifications.
      for (Notification notification : toProcess)
      {
        notification.setSequenceNumber(this.sequencer.incrementAndGet());
        distribute(notification);

        // Add this notification to the history.
        if (this.history.size() > this.historySize)
        {
          // Remove the first item if we're at our maximum capacity.
          this.history.remove(0);
        }
        this.history.add(notification);

        // Purge the details if we've been asked to do so.
        if (this.historyDetails < this.historySize)
        {
          int pos = this.historySize - this.historyDetails;
          this.history.get(pos).purgeDetails();
        }
      }
    }
  }
  
  /**
   * Processes the notification history.  This occurs roughly every 10 
   * minutes to give the Listeners an opportunity to evaluate recent history
   * of notifications.  Presently, this is only executed if new notifications
   * have arrived since the last interval.
   */
  protected void processHistory()
  {
    int lastSequence = this.sequencer.get();
    if (lastSequence > this.lastHistoryProcess)
    {
      // Get a copy of the notification history.
      List<Notification> hist = getHistory();
      
      // Create a smaller list consisting only of those notifications
      // that have arrived since the last interval.
      List<Notification> sinceLast = new ArrayList<>(lastSequence - this.lastHistoryProcess);
      for (Notification n : hist)
      {
        if (n.getSequenceNumber() > this.lastHistoryProcess)
        {
          sinceLast.add(n);
        }
      }

      // Call the listeners.
      for (NotificationListener listener : this.listeners)
      {
        try
        {
          listener.processHistory(hist, sinceLast, this);
        }
        catch (Exception exc)
        {
          this.log.info("Exception while processing history.", exc);
        }
      }
      
      this.lastHistoryProcess = lastSequence;
    }
  }
  
  /**
   * Distributes a Notification to the listeners.
   */
  protected void distribute(Notification notification)
  {
    for (NotificationListener listener : this.listeners)
    {
      try
      {
        listener.processNotification(notification, this);
      }
      catch (Exception exc)
      {
        this.log.info("Exception while distributing {}.", notification, exc);
      }
    }
  }
  
  /**
   * Gets the count of historical Notifications available, which can range
   * from 0 to the maximum history size specified in the configuration.
   */
  public int getHistorySize()
  {
    return this.history.size();
  }
  
  /**
   * Gets the current historical list of Notifications.
   */
  public List<Notification> getHistory()
  {
    List<Notification> toReturn = new ArrayList<>(this.history);
    Collections.sort(toReturn, NotificationSort.DATE_DESC);
    return toReturn;
  }
  
  /**
   * Gets a specific historical Notification.  Returns null if no such 
   * Notification is available.
   */
  public Notification getHistory(int index)
  {
    try
    {
      return this.history.get(index);
    }
    catch (ArrayIndexOutOfBoundsException exc)
    {
      return null;
    }
  }
  
  @Override
  public void configure(EnhancedProperties props)
  {
    EnhancedProperties.Focus focus = props.focus(CONFIGURATION_PREFIX);
    
    this.historySize = focus.getInt("History", 
        DEFAULT_HISTORY_SIZE);
    this.historyDetails = focus.getInt("HistoryDetails", 
        DEFAULT_HISTORY_DETAIL_SIZE);
    this.historyProcessInterval = focus.getInt("HistoryProcessIntervalSeconds", 
        DEFAULT_HISTORY_PROCESS_INTERVAL);
  }
  
  /**
   * Adds a NotificationListener.  This is not a thread-safe operation and is
   * assumed to only occur at application start-up time.
   */
  public void addListener(NotificationListener listener)
  {
    // Expand the list array by one slot and copy the new listener in.
    NotificationListener[] temp = new NotificationListener[this.listeners.length + 1];
    System.arraycopy(this.listeners, 0, temp, 0, this.listeners.length);
    temp[temp.length - 1] = listener;
    this.listeners = temp;
  }

  // @see com.techempower.asynchronous.Asynchronous#begin()
  @Override
  public void begin()
  {
    this.thread.begin();
  }

  // @see com.techempower.asynchronous.Asynchronous#end()
  @Override
  public void end()
  {
    this.thread.end();
  }
  
  class NotifierThread
    extends EndableThread
  {
    public static final int SLEEP_PERIOD = 5 * (int)UtilityConstants.SECOND; 
    
    public NotifierThread()
    {
      super("Gemini Notifier Thread ("
          + Notifier.this.app.getVersion().getAbbreviatedProductName() + ")",
          SLEEP_PERIOD);
    }
    
    @Override
    public void run()
    {
      long now = System.currentTimeMillis();
      long intervalMs = (Notifier.this.historyProcessInterval * UtilityConstants.SECOND);
      long nextProcess = now + intervalMs;
      
      while (checkPause())
      {
        now = System.currentTimeMillis();
        
        // Process the history every ~10 minutes (configurable).
        if (now >= nextProcess)
        {
          try
          {
            processHistory();
          }
          catch (Exception exc)
          {
            Notifier.this.log.info("Exception while processing notification history.", exc);
          }
          nextProcess = now + intervalMs;
        }
      
        // Process the inbound queue every ~5 seconds.
        try
        {
          processQueue();
        }
        catch (Exception exc)
        {
          Notifier.this.log.info("Exception while processing inbound notification queue.", exc);
        }
        
        simpleSleep();
      }
    }
  }

}
