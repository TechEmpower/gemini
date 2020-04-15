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

package com.techempower.gemini.email.outbound;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.techempower.asynchronous.*;
import com.techempower.gemini.*;
import com.techempower.gemini.email.*;
import com.techempower.helper.*;
import com.techempower.thread.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EmailServicer is used by applications to send outbound e-mail.  The 
 * servicer manages a pool of threads used to send e-mails via the 
 * EmailTransport.  Applications are not expected to communicate with the
 * EmailTransport directly; rather, they call EmailServicer.sendMail in order
 * to have an e-mail queued for delivery.
 *   <p>
 * Reads the following configuration options from the .conf file:
 *   <ul>
 * <li>OutboundEmail.Enabled - Set to no to disable all outbound e-mail.
 *     Default: yes.</li>
 * <li>OutboundEmail.Threads - The maximum number of threads to use for 
 *     outbound email (default: 10).  This will also be read as 
 *     "EmailerThreads" for legacy support.
 * <li>OutboundEmail.BeforeDeliveryDelayMillis - A number of milliseconds to
 *     wait before sending an outbound e-mail.  This delay will <b>not</b>
 *     affect other mail deliveries.
 * <li>OutboundEmail.AfterDeliverySleepMillis - A number of milliseconds to
 *     sleep after sending an outbound e-mail.  This sleep <b>may</b> affect
 *     other mail deliveries.
 *   </ul>
 *
 * @see EmailTransport
 * @see EmailPackage
 */
public class EmailServicer
  implements Asynchronous,
             Configurable
{
  //
  // Constants.
  //

  public static final int    DEFAULT_SENDER_THREADS = 10;
  public static final long   DEFAULT_DELAY_MILLIS   = 0L;
  public static final IntRange REASONABLE_THREAD_COUNT = new IntRange(1, 500);
  
  //
  // Member variables.
  //

  private final    Logger         log = LoggerFactory.getLogger(getClass());
  private final    EmailTransport transport;

  private final    AtomicInteger         queued;
  private final    AtomicInteger         sent;
  private final    AtomicInteger         removed;
  private volatile PausableScheduledThreadPoolExecutor executor;
  
  private          int                   senderThreads = DEFAULT_SENDER_THREADS;
  private          long                  beforeDeliveryDelayMillis = DEFAULT_DELAY_MILLIS;
  private          long                  afterDeliverySleepMillis  = DEFAULT_DELAY_MILLIS; 

  //
  // Member methods.
  //

  /**
   * Constructor.  Takes a reference to the application using this
   * servicer.
   */
  public EmailServicer(GeminiApplication application)
  {
    this.executor    = new PausableScheduledThreadPoolExecutor(this.senderThreads);
    this.queued      = new AtomicInteger();
    this.sent        = new AtomicInteger();
    this.removed     = new AtomicInteger();
    this.transport   = application.getEmailTransport();
    
    // Add self as an asynchronous resource.
    application.addAsynchronous(this);
    
    // Get configured.
    application.getConfigurator().addConfigurable(this);
  }

  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    final EnhancedProperties.Focus focus = props.focus("OutboundEmail.");    
    
    // Set the delays.
    setBeforeDeliveryDelayMillis(focus.getLong("BeforeDeliveryDelayMillis", getBeforeDeliveryDelayMillis()));
    log.info("Email delivery will be delayed by {}ms.", getBeforeDeliveryDelayMillis());
    setAfterDeliverySleepMillis(focus.getLong("AfterDeliverySleepMillis", getAfterDeliverySleepMillis()));
    log.info("Outbound email threads will sleep for {}ms after each delivery.", getAfterDeliverySleepMillis());
    
    // Get the number of threads.
    int newSenderThreads = this.senderThreads;
    if (props.has("OutboundEmailThreads"))
    {
      newSenderThreads = props.getInt("OutboundEmailThreads", newSenderThreads);
      log.info("OutboundEmailerThreads is deprecated. Use OutboundEmail.Threads instead.");
    }
    if (props.has("EmailerThreads"))
    {
      newSenderThreads = props.getInt("EmailerThreads", newSenderThreads);
      log.info("EmailerThreads is deprecated. Use OutboundEmail.Threads instead.");
    }
    newSenderThreads = focus.getInt("Threads", newSenderThreads);
    
    // Bound the thread count to something reasonable.
    newSenderThreads = NumberHelper.boundInteger(newSenderThreads, REASONABLE_THREAD_COUNT);
    
    // Is there a new maximum set?  If so, shutdown the current executor and
    // create a new one.
    if (newSenderThreads != senderThreads)
    {
      senderThreads = newSenderThreads;
      final PausableScheduledThreadPoolExecutor oldExecutor = executor;
      executor = new PausableScheduledThreadPoolExecutor(newSenderThreads);

      // Shut down the old Executor after 10 seconds.
      Runnable shutdown = new Runnable()
      {
        @Override
        public void run()
        {
          oldExecutor.shutdown();
        }
      };
      ThreadHelper.schedule(shutdown, 10L, TimeUnit.SECONDS);
    }
  }

  @Override
  public void begin()
  {
  }

  @Override
  public void end()
  {
    // Terminate e-mail servicer threads.
    executor.shutdown();
  }
  
  /**
   * Increments the queued count.
   */
  protected void incrementQueued()
  {
    queued.incrementAndGet();
  }
  
  /**
   * Increments the sent count.
   */
  protected void incrementSent()
  {
    sent.incrementAndGet();
  }
  
  /**
   * Increments the removed count.
   */
  protected void incrementRemoved()
  {
    removed.incrementAndGet();
  }
  
  /**
   * Gets the removed count.
   */
  public int getRemovedCount()
  {
    return removed.get();
  }
  
  /**
   * Gets the pending count, which is the queued minus the sent and removed.
   */
  public int getPendingCount()
  {
    int currentSent = getSentCount();
    int currentQueued = getQueuedCount();
    int currentRemoved = getRemovedCount();
    
    return currentQueued - currentSent - currentRemoved;
  }
  
  /**
   * Gets the queued count.
   */
  public int getQueuedCount()
  {
    return queued.get();
  }
  
  /**
   * Gets the sent count.
   */
  public int getSentCount()
  {
    return sent.get();
  }
  
  /**
   * Gets the number of Sender threads.
   */
  public int getSenderThreadCount()
  {
    return executor.getActiveCount();
  }

  /**
   * Gets the high-water count of Sender threads.
   */
  public int getPeakSenderThreadCount()
  {
    return executor.getLargestPoolSize();
  }
  
  /**
   * Gets the maximum count of Sender threads.
   */
  public int getMaximumSenderThreadCount()
  {
    return senderThreads;
  }

  /**
   * Gets the email transport for the servicer.
   */
  protected EmailTransport getTransport()
  {
    return transport;
  }
  
  /**
   * Gets the fixed delay, in milliseconds, to use before sending each mail.
   */
  public long getBeforeDeliveryDelayMillis()
  {
    return beforeDeliveryDelayMillis;
  }
  
  /**
   * Sets a fixed delay prior to sending mails, in milliseconds. 
   */
  public EmailServicer setBeforeDeliveryDelayMillis(long delay)
  {
    this.beforeDeliveryDelayMillis = delay;
    return this;
  }

  /**
   * Gets a period of time, in milliseconds, to delay after sending each mail,
   * which can be used to throttle the delivery rate.
   */
  public long getAfterDeliverySleepMillis() 
  {
    return afterDeliverySleepMillis;
  }

  /**
   * Sets a period of time, in milliseconds, to delay after sending each mail,
   * which can be used to throttle the delivery rate.
   */
  public EmailServicer setAfterDeliverySleepMillis(long afterDeliverySleepMillis) 
  {
    this.afterDeliverySleepMillis = afterDeliverySleepMillis;
    return this;
  }

  /**
   * Pauses the servicer threads.  E-mail delivery will be paused after
   * the currently transferring mails complete.
   */
  public void pause()
  {
    executor.pause();
  }

  /**
   * Resumes the servicer threads.  E-mail delivery will resume.
   */
  public void unpause()
  {
    executor.resume();
  }

  /**
   * Returns the paused flag.
   */
  public boolean isPaused()
  {
    return executor.isPaused();
  }

  /**
   * Queues an email for delivery by a servicer thread.  This method does not
   * block; it will likely return before the mail is actually delivered.  
   * Returns true if the e-mail has been queued successfully; or false if 
   * there was an error queuing the e-mail.  Note: successful queuing of a
   * mail is not a guarantee that it will be delivered.
   *
   * @param email the EmailPackage to queue up for delivery.
   * 
   * @return true for successfully queuing; false otherwise
   */
  public boolean sendMail(EmailPackage email)
  {
    return sendMail(email, true);
  }
  
  /**
   * Queues an email for delivery by a servicer thread.
   */
  protected boolean sendMail(EmailPackage email, boolean incrementQueued)
  {
    // Only proceed if outbound e-mail delivery is enabled.
    if (getTransport().isOutboundEnabled())
    {
      if (email == null)
      {
        log.info("Email is null.  Cannot queue.");
        return false;
      }
      else
      {
        // Make certain we have a valid sender and receiver.
        if ( (StringHelper.isNonEmpty(email.getRecipient()))
          && (StringHelper.isNonEmpty(email.getAuthor()))
          )
        {
          // Queue for sending.
          scheduleSender(email);
          if (incrementQueued)
          {
            incrementQueued();
          }
          
          return true;
        }
        else
        {
          log.info("Cannot send e-mail.Author: {}; Recipient: {}",
              email.getAuthor(), email.getRecipient());
          return false;
        }
      }
    }
    else
    {
      // If outbound e-mail delivery is disabled, just say we were 
      // successful.
      log.info("Email Servicer not enabled.");
      return true;
    }
  }
  
  /**
   * Schedules a Sender for an EmailPackage.
   */
  protected void scheduleSender(final EmailPackage email)
  {
    executor.schedule(new Sender(this, email), getBeforeDeliveryDelayMillis(), 
        TimeUnit.MILLISECONDS);
  }
  
  /**
   * A Runnable responsible for sending an e-mail via the Transport.
   */
  private static class Sender 
    implements Runnable
  {
    private final EmailServicer  servicer;
    private final EmailPackage   email;
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public Sender(EmailServicer servicer, EmailPackage email)
    {
      this.servicer = servicer;
      this.email = email;
    }
    
    @Override
    public void run()
    {
      //debug("Attempting to send email to " + email.getRecipient());
      boolean success = servicer.getTransport().sendEmail(email);

      // If we were successful, increment the sent count.
      if (success)
      {
        servicer.incrementSent();
      }
      // If we were not successful, either kill the e-mail or requeue it.
      else
      {
        email.incrementDeliveryAttempts();
        log.info(
            "Mail to {} failed on try {}. RecipientSource: {}. Headers: {}",
            email.getRecipient(), email.getDeliveryAttempts(),
            email.getRecipientSource(), email.getHeaders());

        // If we have exceeded the number of delivery attempts, then trash
        // the e-mail.
        if (email.getDeliveryAttempts() >= servicer.getTransport().getRetryLimit())
        {
          log.info("Mail removed from queue.");
          servicer.incrementRemoved();
        }

        // Otherwise, re-queue the email at the end of the queue.
        else
        {
          // Ask for this to be requeued but do not increment the queued count.
          servicer.sendMail(email, false);
          log.info("Mail requeued.");
        }
      }
      
      // If there is an after-delivery sleep, do so.
      if (servicer.getAfterDeliverySleepMillis() > 0L)
      {
        ThreadHelper.sleep(servicer.getAfterDeliverySleepMillis());
      }
    }
  }
  
  /**
   * Standard Java toString.
   */
  @Override
  public String toString()
  {
    String pausedString = "";
    if (isPaused())
    {
      pausedString = "; Paused";
    }
    int currentSent = getSentCount();
    int currentQueued = getQueuedCount();
    int currentRemoved = getRemovedCount();
    int currentPending = getPendingCount();
    return "EmailServicer "
        + "[" + currentSent + " sent" 
        + "; " + currentQueued + " queued" 
        + "; " + currentRemoved + " removed"
        + "; " + currentPending + " pending"
        + pausedString
        + "]";
  }

}   // End EmailServicer.
