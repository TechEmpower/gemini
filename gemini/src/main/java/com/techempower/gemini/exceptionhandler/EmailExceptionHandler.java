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
import com.techempower.gemini.email.*;
import com.techempower.gemini.email.outbound.*;
import com.techempower.gemini.feature.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of ExceptionHandler that emails a system administrator
 * in the event of an exception.
 *   <p>
 * Applications are encouraged to use the NotificationExceptionHandler instead
 * of this EmailExceptionHandler.  The Gemini administration notification
 * functionality includes the ability to e-mail system administrators of
 * notifications, which renders the functionality of this particular class
 * unnecessary in most cases.
 *   <p>
 * Configuration options:
 *   <ul>
 * <li>EmailExceptionHandler.ToEmailAddress - Who should receive exception
 *     alert e-mails.</li>
 * <li>EmailExceptionHandler.FromEmailAddress - The e-mail address used to
 *     send the alerts.</li>
 * <li>EmailExceptionHandler.MinimumInterval - A minimum interval in seconds
 *     to allow e-mails to be sent; if multiple exceptions are raised during
 *     the interval, a summary will be rendered rather than spamming the
 *     recipient with each exception.</li>
 * <li>EmailExceptionHandler.Enabled - Set to false to disable this
 *     component.</li>
 *   </ul>
 *   
 * @see NotificationExceptionHandler
 */
public class EmailExceptionHandler
  implements ExceptionHandler,
             Feature,
             Configurable
{
  
  //
  // Constants
  //
  
  public static final String COMPONENT_CODE = "emEH";
  public static final String CRLF = UtilityConstants.CRLF;
  
  //
  // Member variables.
  //
  
  private final GeminiApplication application;
  private final FeatureManager fm;
  private final Logger         log = LoggerFactory.getLogger(getClass());
  
  private String            fromMailAddress = "exceptions@techempower.com";
  private String            toMailAddress = this.fromMailAddress;
  private long              minimumInterval = 10L * UtilityConstants.MINUTE;
  private AtomicInteger     skipped = new AtomicInteger(0);
  private long              nextDelivery = 0L;
  private EmailServicer     emailServicer;

  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public EmailExceptionHandler(GeminiApplication app)
  {
    this.application = app;
    app.getConfigurator().addConfigurable(this);
    
    this.fm = this.application.getFeatureManager();
    this.fm.add("exc-email", "Email Exception Handler");
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    this.toMailAddress = props.get("EmailExceptionHandler.ToEmailAddress", this.toMailAddress);
    this.fromMailAddress = props.get("EmailExceptionHandler.FromEmailAddress", this.toMailAddress);
    this.minimumInterval = props.getInt("EmailExceptionHandler.MinimumInterval", 10 * 60) * UtilityConstants.SECOND;
    
    if (props.get("EmailExceptionHandler.Enabled") != null)
    {
      this.log.info("EmailExceptionHandler.Enabled is deprecated.  Use Feature.exc-email instead.");
      this.fm.set("exc-email", props.getBoolean("EmailExceptionHandler.Enabled", true));
    }
    
    // Get a reference to the email servicer.
    this.emailServicer = this.application.getEmailServicer();
    
    // We're disabled if the enabled flag is off -or- either of the e-mail
    // addresses is empty.
    if (  (!isEnabled())
       || (StringHelper.isEmpty(this.fromMailAddress))
       || (StringHelper.isEmpty(this.toMailAddress))
       )
    {
      this.log.info("EmailExceptionHandler disabled.");
    }
  }
  
  /**
   * Is the EmailExceptionHandler enabled?
   */
  public boolean isEnabled()
  {
    return this.fm.on("exc-email");
  }

  @Override
  public void handleException(Context context, Throwable exc)
  {
    handleException(context, exc, null);
  }

  @Override
  public void handleException(Context context, Throwable exception,
                              String description)
  {
    // Are we enabled?
    if (  (isEnabled())
       && (StringHelper.isNonEmpty(this.fromMailAddress))
       && (StringHelper.isNonEmpty(this.toMailAddress))
       )
    {
      long currentTime = System.currentTimeMillis();
      if (currentTime < this.nextDelivery)
      {
        // Skip this exception.
        this.skipped.incrementAndGet();
      }
      else
      {
        // Build the mail message body.
        String message = ExceptionHandlerHelper.renderExceptionAsReport(context, this.application, exception, description, this.skipped.get());
        this.skipped.set(0);
        
        this.log.info("Sending exception report to {}", this.toMailAddress);
        //log.debug("Message: " + message);
        
        // Send the mail.
        EmailPackage email = new EmailPackage(
          "<auto> Exception Report - " 
            + this.application.getVersion().getNameAndDeployment(), 
          message, 
          this.toMailAddress, 
          this.fromMailAddress);
        this.emailServicer.sendMail(email);
        
        // Set the next delivery threshold time.
        this.nextDelivery = currentTime + this.minimumInterval;
      }
    }
  }

}  // End EmailExceptionHandler
