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

import com.techempower.helper.*;
import com.techempower.thread.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread used by EmailDispatcher to regularly check for inbound email.
 */
public class EmailDispatcherThread
  extends    EndableThread
{
  
  //
  // Member variables.
  //
  
  private final EmailDispatcher dispatcher;
  
  private boolean         pause = false;
  private boolean         onDemandCheck = false;
  private Logger          log = LoggerFactory.getLogger(EmailDispatcher.COMPONENT_CODE);

  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public EmailDispatcherThread(EmailDispatcher dispatcher)
  {
    super("Email Dispatcher Thread",
        dispatcher.getMaximumSleep(), 
        dispatcher.getMaximumSleep(),
        dispatcher.getMaximumSleep(), 
        1000);
    this.dispatcher = dispatcher;
  }

  /**
   * Default run method.
   */
  @Override
  public void run()
  {
    // Capture the start time.
    setStartTime();
    
    // Pause for five seconds before starting.
    simpleSleep(5000);

    try
    {
      // Keep running until ended; pause if instructed to do so.
      while (checkPause())
      {
        // Only check for mail if we're not paused.
        if (!isEmailPaused())
        {
          try
          {
            if (this.onDemandCheck)
            {
              log.info("Processing on-demand email check.");
              this.onDemandCheck = false;
            }
            
            // Check for mail.
            int processed = this.dispatcher.checkForMail(this);
            
            if (processed > 0)
            {
              log.info("{} inbound email{} processed.", processed,
                  StringHelper.pluralize(processed));
              setMinimumSleep();
            }
            else
            {
              incrementSleep();
            }
          }
          catch (Exception exc)
          {
            log.error("Exception while checking inbound email", exc);
          }
        }
        else
        {
          incrementSleep();
        }
        
        simpleSleep();
      }
    }
    finally
    {
      log.info("EmailDispatcherThread ending.");
    }
  }
  
  /**
   * Requests an immediate email check by interrupting the thread.
   */
  public void requestImmediateCheck()
  {
    this.onDemandCheck = true;
    if (isAsleep())
    {
      this.interrupt();
    }
  }
  
  /**
   * Returns true if the checking of mail is paused.
   */
  public boolean isEmailPaused()
  {
    return this.pause;
  }
  
  /**
   * Pauses the checking of email.
   */
  public void setEmailPause(boolean pause)
  {
    this.pause = pause;
    this.interrupt();
  }
   
}  // End EmailDispatcherThread.
