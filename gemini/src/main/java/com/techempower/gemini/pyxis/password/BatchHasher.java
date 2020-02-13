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
package com.techempower.gemini.pyxis.password;

import java.util.*;

import com.techempower.cache.*;
import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.helper.*;
import com.techempower.scheduler.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts non-hashed passwords of pre-existing Pyxis user records to 
 * hashed passwords. 
 */
public class BatchHasher<U extends PyxisUser>
  implements Configurable
{
  
  private static final long DEFAULT_PAUSE_INTERVAL = 100;
  
  private final GeminiApplication app;
  private final Logger log = LoggerFactory.getLogger("bHas");
  private final PyxisSecurity security;
  private final Class<U> userClass;
  private final EntityStore store;
  private final BatchHasherEvent event;
  private long pauseInterval = DEFAULT_PAUSE_INTERVAL;
  
  /**
   * Constructor.
   */
  public BatchHasher(GeminiApplication application, Class<U> userClass)
  {
    this.app = application;
    this.store = app.getStore();
    this.security = app.getSecurity();
    this.userClass = userClass;
    this.event = new BatchHasherEvent();
    app.getConfigurator().addConfigurable(this);
  }  

  /**
   * Do the work.
   */
  public void batchHashPasswords()
  {
    log.info("Batch hasher looking for plaintext passwords.");
    
    int converted = 0;
    
    // Look through all users, searching for passwords that do not have the
    // prefix that identifies a BCrypt-hashed password.
    final List<U> users = store.list(userClass);
    final String identifyingPrefix = security.getPasswordHasher()
        .getIdentifyingPrefix();
    
    for (U user : users)
    {
      // Only process users whose password does not begin with the identifying
      // character sequence.
      final String cleartext = user.getUserPassword();
      if (cleartext != null)
      {
        if (!(cleartext.startsWith(identifyingPrefix)))
        {
          // This password does not have the identifying prefix, so we will
          // construct a password-change proposal and ask the Security to make
          // the change.
          log.info("Hashing password for user {}.", user.getId());
          
          final PasswordProposal proposal = new PasswordProposal(
              cleartext,
              user.getUserUsername(),
              user,
              null);
          
          // Bypass validation of password complexity rules because legacy
          // passwords may not meet these requirements, but hashing is still
          // desired.
          proposal.bypassValidation = true;
          
          // Do the actual password change.
          security.passwordChange(proposal);
          
          // Save the updated user.
          store.put(user);
          converted++;
          
          // Pause a bit since we don't want this process bogging down anything.
          if (pauseInterval > 0L)
          {
            ThreadHelper.sleep(pauseInterval);
          }
        }
      }
      else
      {
        log.info("Password for user {} is null.", user.getId());
      }
    }
    
    log.info("Batch hasher work complete, {} user password{} hashed.",
        converted, StringHelper.pluralize(converted));
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    pauseInterval = props.getLong("BatchHasher.Pause", DEFAULT_PAUSE_INTERVAL);
    app.getScheduler().removeEvent(event);
    app.getScheduler().scheduleEvent(event);
  }
  
  private final class BatchHasherEvent extends ScheduledEvent
  {
    private BatchHasherEvent()
    {
      super("Batch password hasher", 
          "Converts legacy plaintext passwords to hashed passwords.");
    }
    
    @Override
    public long getDefaultScheduledTime()
    {
      // Run two minutes after startup.
      return System.currentTimeMillis() + (2 * UtilityConstants.MINUTE);
    }

    @Override
    public boolean requiresOwnThread()
    {
      return true;
    }

    @Override
    public void execute(Scheduler scheduler, boolean onDemandExecution)
    {
      try
      {
        batchHashPasswords();
      }
      catch (Exception exc)
      {
        log.warn("Exception while hashing passwords.", exc);
      }
      
      // This event is not rescheduled.
      scheduler.removeEvent(this);
    }
  }
  
}
