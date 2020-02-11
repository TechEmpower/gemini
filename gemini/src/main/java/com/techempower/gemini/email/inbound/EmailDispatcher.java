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

package com.techempower.gemini.email.inbound;

import java.util.*;

import com.techempower.asynchronous.*;
import com.techempower.gemini.*;
import com.techempower.gemini.email.*;
import com.techempower.thread.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EmailDispatcher is the core of the revised inbound email processing
 * functionality of Gemini (replacing the EmailNotificationServicer that
 * existed previously).
 *   <p>
 * The model for inbound email processing is similar to web request and
 * distributed-application messaging.  That is: a dispatcher is used to
 * route inbound items to handlers.  In this case, the EmailDispatcher
 * routes e-mails to EmailHandlers.
 *   <p>
 * As part of the development of the inbound functionality, the EmailTransport
 * class was re-factored slightly.  In particular, the format of the email
 * server specification within an application's configuration file is somewhat
 * updated to account for mail servers being either for outbound, inbound,
 * or both kinds of e-mail traffic.
 *   <p>
 * In fact, EmailTransport now functions as the implementation of the
 * "heavy lifting" portion of e-mail functionality both outbound and inbound.
 * However, because it has historically been located in the outbound package,
 * I have left it there.  The EmailHandler interface is in the root email
 * package because EmailTransport uses those objects as well.  Therefore,
 * Inbound email is dependent on the Outbound email package being available,
 * but not vice-versa.
 *   <p>
 * Configurable options:
 *   <ul>
 * <li>InboundMail.MinimumSleepMs - The minimum number of milliseconds to wait
 *     before checking for inbound e-mail.  Default is 15000 (15 seconds).</li>
 * <li>InboundMail.MaximumSleepMs - The maximum number of milliseconds to wait
 *     before checking for inbound e-mail.  Default is 30000 (30 seconds).</li>
 *   </ul>
 */
public class EmailDispatcher
  implements Asynchronous,
             Configurable,
             EmailHandler
{
  
  //
  // Constants.
  //
  
  public static final String COMPONENT_CODE = "edsp";
  public static final String DEFAULT_PROPERTY_PREFIX = "InboundMail.";
  public static final String PROPERTY_MAXIMUM_SLEEP = "MaximumSleepMs";
  public static final String PROPERTY_MINIMUM_SLEEP = "MinimumSleepMs";
  public static final int    DEFAULT_MAXIMUM_SLEEP = 30 * (int)UtilityConstants.SECOND;
  public static final int    DEFAULT_MINIMUM_SLEEP = 15 * (int)UtilityConstants.SECOND;
  
  //
  // Member variables.
  //
  
  private final GeminiApplication application;
  private final Logger            log = LoggerFactory.getLogger(COMPONENT_CODE);
  private final String            propertyPrefix;
  private final List<EmailHandler> handlers;
  
  private final EmailDispatcherThread thread;
  private int                   totalProcessed = 0;
  
  private int               maximumSleep = DEFAULT_MAXIMUM_SLEEP;
  private int               minimumSleep = DEFAULT_MINIMUM_SLEEP;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public EmailDispatcher(GeminiApplication application, String propertyPrefix)
  {
    this.application = application;
    this.propertyPrefix = propertyPrefix;
    this.handlers = new ArrayList<>(8);
    this.thread = new EmailDispatcherThread(this);
    
    application.addAsynchronous(this);
    application.getConfigurator().addConfigurable(this);
  }

  /**
   * Constructor.
   */
  public EmailDispatcher(GeminiApplication application)
  {
    this(application, DEFAULT_PROPERTY_PREFIX);
  }

  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    EnhancedProperties.Focus focus = props.focus(propertyPrefix);
    maximumSleep = focus.getInt(PROPERTY_MAXIMUM_SLEEP, DEFAULT_MAXIMUM_SLEEP);
    minimumSleep = focus.getInt(PROPERTY_MINIMUM_SLEEP, DEFAULT_MINIMUM_SLEEP);
    
    log.info("Email Dispatcher configured.");
  }
  
  /**
   * Gets an iterator of Mail Server descriptors for checking e-mail.
   */
  protected EmailServerDescriptor[] getInboundMailServers()
  {
    return application.getEmailTransport().getInboundServers();
  }
  
  /**
   * Checks for and processes inbound email from all defined servers.  
   * Returns the number of mails processed in total.
   * 
   * @param executionThread The EndableThread instance running this method.
   *        Can be null if called from something other than an EndableThread.
   */
  public synchronized int checkForMail(EndableThread executionThread)
  {
    EmailServerDescriptor[] servers = getInboundMailServers();
    int processed = 0;

    for (EmailServerDescriptor server : servers)
    {
      // Proceed if the execution thread should keep running.
      if ((executionThread == null)
          || (executionThread.checkPause())
          )
      {
        // Check for mail from the server.
        processed += checkForMail(server);
      }
    }
    
    return processed;
  }
  
  /**
   * Checks for and processes inbound email from a specific server.  Returns
   * the number of mails processed.
   */
  public int checkForMail(EmailServerDescriptor server)
  {
    // Check for emails.  First we get a session.
    int processed = application.getEmailTransport().checkForMail(server, this);
  
    // Increment processed and totalProcessed.
    totalProcessed += processed;
    
    return processed;
  }
  
  /**
   * Handles an inbound Email received by the EmailTransport.
   */
  @Override
  public boolean handleEmail(EmailPackage email)
  {
    boolean toReturn = false;
    
    // If we have any handlers, let's process it.
    if (handlers != null)
    {
      Iterator<EmailHandler> iter = handlers.iterator();
      EmailHandler handler;
      
      // Ask each Handler to process the email in turn.
      while (iter.hasNext())
      {
        handler = (iter.next());
        toReturn |= handler.handleEmail(email);
      }
    }
    
    // If any Handler asked for the message to be deleted, delete it.
    return toReturn;
  }
  
  /**
   * Adds an EmailHandler.
   */
  public void addHandler(EmailHandler handler)
  {
    handlers.add(handler);
  }
  
  /**
   * Removes an EmailHandler.
   */
  public void removeHandler(EmailHandler handler)
  {
    if (handlers != null)
    {
      handlers.remove(handler);
    }
  }
  
  /**
   * Gets the collection of EmailHandlers.
   */
  public List<EmailHandler> getHandlers()
  {
    return new ArrayList<>(handlers);
  }

  /**
   * @return the totalProcessed
   */
  public int getTotalProcessed()
  {
    return totalProcessed;
  }
  
  /**
   * Gets a reference to the EmailDispatcherThread.
   */
  public EmailDispatcherThread getThread()
  {
    return thread;
  }
  
  /**
   * @return the maximumSleep
   */
  protected int getMaximumSleep()
  {
    return maximumSleep;
  }

  /**
   * @return the minimumSleep
   */
  protected int getMinimumSleep()
  {
    return minimumSleep;
  }

  /**
   * Pauses the checking of inbound email.
   */
  public void pause()
  {
    if (thread != null)
    {
      thread.setEmailPause(true);
    }
  }
  
  /**
   * Resumes the checking of inbound email.
   */
  public void resume()
  {
    if (thread != null)
    {
      thread.setEmailPause(false);
    }
  }
  
  /**
   * Determines if the thread is paused.
   */
  public boolean isPaused()
  {
    return thread != null && thread.isEmailPaused();
  }
  
  /**
   * Processes inbound email now by interrupting the dispatcher thread.
   */
  public void requestImmediateCheck()
  {
    if (thread != null)
    {
      thread.requestImmediateCheck();
    }
  }
  
  /**
   * Starts this asynchronous component.
   */
  @Override
  public void begin()
  {
    log.info("Email Dispatcher starting.");
    thread.setName("Email Dispatcher Thread (" + application.getVersion().getAbbreviatedProductName() + ")");
    thread.start();
  }
  
  /**
   * Stops this asynchronous component.
   */
  @Override
  public void end()
  {
    log.info("Email Dispatcher ending.");
    thread.setKeepRunning(false);
  }
  
}  // End EmailDispatcher.
