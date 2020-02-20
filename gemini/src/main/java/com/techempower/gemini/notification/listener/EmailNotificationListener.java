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
package com.techempower.gemini.notification.listener;

import static com.techempower.util.UtilityConstants.*;

import java.text.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.email.*;
import com.techempower.gemini.notification.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to administrative notifications, sending them out as e-mails.
 * High-severity notifications are sent immediately, as-is.  Low and medium
 * severity notifications are sent during configurable intervals (typically
 * every 10 minutes).  The interval in question is the Notifier's history-
 * processing interval, which is controlled by:
 * 
 * <ul><li>Notifier.HistoryProcessIntervalSeconds</li></ul>
 */
public class EmailNotificationListener
  implements NotificationListener,
             Configurable
{

  //
  // Constants
  //
  
  public  static final String PROPERTY_PREFIX = "EmailNotificationListener.";
  public  static final int    MAXIMUM_SYNOPSIS_LENGTH = 40;
  public  static final String DEFAULT_FROM_ADDRESS = "exceptions@techempower.com";
  private static final String[] TEXT_FINDS    = new String[] { "\n", "\t", "\r" };
  private static final String[] TEXT_REPLACES = new String[] { " ", " ", " " };
  
  //
  // Member variables.
  //
  
  private final GeminiApplication application;
  private final Logger            log = LoggerFactory.getLogger(getClass());
  private final SimpleDateFormat  format = new SimpleDateFormat("MM-dd HH:mm:ss");
  
  private String            fromMailAddress = DEFAULT_FROM_ADDRESS;
  private List<Recipient>   recipients = new ArrayList<>();

  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public EmailNotificationListener(GeminiApplication application)
  {
    this.application = application;
    application.getConfigurator().addConfigurable(this);
  }

  @Override
  public void processNotification(Notification notification, Notifier notifier)
  {
    // Send the notification as an e-mail immediately if the severity is high.
    if (notification.getSeverity() == Notification.Severity.HIGH)
    {
      final List<Notification> list = Collections.singletonList(notification);
      sendNotifications(list);
    }
  }

  @Override
  public void processHistory(List<Notification> history,
      List<Notification> sinceLastInterval, Notifier notifier)
  {
    // Assume that we've already sent the high-priority notifications as
    // separate e-mails and send the low/med priority notifications as a
    // single unified e-mail.
    if (sinceLastInterval.size() > 0)
    {
      final List<Notification> lowMed = new ArrayList<>(sinceLastInterval.size());
      
      for (Notification n : sinceLastInterval)
      {
        // Ignore high-priority notifications because we will have already
        // sent these as they arrived.
        if (n.getSeverity() != Notification.Severity.HIGH)
        {
          lowMed.add(n);
        }
      }
      
      // Do we have any low/medium severity notifications to send?
      if (lowMed.size() > 0)
      {
        sendNotifications(lowMed);
      }
    }
  }
  
  /**
   * Truncate and remove line breaks from a synopsis, so that we can put it
   * into an e-mail subject.
   */
  public String truncateSynopsis(String synopsis, int length)
  {
    String truncated = StringHelper.truncateEllipsis(synopsis, length);
    return StringHelper.replaceSubstrings(truncated, TEXT_FINDS, TEXT_REPLACES);
  }
  
  /**
   * Sends an e-mail with notifications (1 or more).
   */
  public void sendNotifications(List<Notification> notes)
  {
    // Sort the notifications by descending severity.
    if (notes.size() > 1)
    {
      Collections.sort(notes, NotificationSort.SEVERITY_DESC);
    }

    for (Recipient recipient : recipients)
    {
      final StringBuilder body = new StringBuilder();
      
      int matched = 0;
      Notification highest = null;
      
      // Add each notification.
      for (Notification n : notes)
      {
        // Does the notification match the recipient's filters?
        if (n.getSeverity().ordinal() >= recipient.minSeverity.ordinal())
        {
          if (  (recipient.sources == null)     // Match all sources when null.
             || (recipient.sources.length == 0) // Match all sources when empty.
             || (CollectionHelper.arrayContains(recipient.sources, n.getSource().toLowerCase()))
             )
          {
            // This is a good match, let's count it.
            matched++;
            
            // Add it to the e-mail body.
            body.append(CRLF)
                .append(DIVIDER_DOUBLE)
                .append(CRLF);
            body.append(format.format(n.getTime()))
                .append(" - ")
                .append(n.getSource())
                .append(" - ")
                .append(n.getSeverity())
                .append(" severity")
                .append(CRLF);
            body.append(DIVIDER_SINGLE)
                .append(CRLF);
            body.append(n.getDetails());
            body.append(CRLF);
            
            // Record the highest severity we encountered.  Okay, so it's
            // already sorted in descending order so we could just take the
            // first one, but the check below doesn't hurt.
            if (  (highest == null)
               || (highest.getSeverity().ordinal() < n.getSeverity().ordinal())
               )
            {
              highest = n;
            }
          }
        }
      }
      
      // Don't bother sending an e-mail if none of the notifications matched
      // the recipient's filters.
      if (matched > 0)
      {
        // Build a subject including a count and one of the notification's
        // synopsis.
        final StringBuilder subject = new StringBuilder();
        subject.append("<auto>");
        subject.append("<")
               .append(StringHelper.truncate(
                   application.getVersion().getProductName(),
                   10).toLowerCase())
               .append(">");
        if (matched == 1)
        {
          subject.append("<")
                 .append(
                     StringHelper.truncate(highest.getSource().toLowerCase(),
                         3))
                 .append(">");
        }
        subject.append(" ")
               .append(
                   truncateSynopsis(highest.getSynopsis(),
                       MAXIMUM_SYNOPSIS_LENGTH));
        if (matched > 1)
        {
          subject.append(" (and ")
                 .append(matched - 1)
                 .append(" more)");
        }
        
        long uptime = application.getUptime();

        // Add a preamble to the body.
        body.insert(0, application.getVersion().getNameAndDeployment() 
            + " - " + matched + " new notification" 
            + StringHelper.pluralize(matched) + CRLF
            + DateHelper.getHumanDuration(uptime, 2) + " uptime ("
            + uptime + " ms)" + CRLF);
        
        // Send the mail.
        final EmailPackage email = new EmailPackage(
          subject.toString(), 
          body.toString(), 
          recipient.email, 
          fromMailAddress);
        application.getEmailServicer().sendMail(email);
        
        //log.debug("Email recipient: " + recipient.email);
        //log.debug("Email subject: " + subject);
        //log.debug("Body:" + CRLF + body);
      }
    }
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    log.info("Configuring email notification recipients.");
    final List<Recipient> newRecipients = new ArrayList<>();
    final String recipientPrefix = PROPERTY_PREFIX + "Recipient";
    int index = 1;
    while (props.get(recipientPrefix + index + ".Email") != null)
    {
      final Recipient recipient = new Recipient(props, recipientPrefix + index + ".");
      newRecipients.add(recipient);
      log.info("Recipient {}: {}", index, recipient);
      index++;
    }
    recipients = newRecipients;
    log.info("{} notification recipient{} configured.",
        recipients.size(), StringHelper.pluralize(recipients.size()));
    
    fromMailAddress = props.get(PROPERTY_PREFIX + "FromAddress", 
        DEFAULT_FROM_ADDRESS);

    // This listener is disabled if the e-mail addresses are not specified.
    if (  (StringHelper.isEmpty(fromMailAddress))
       || (recipients.size() == 0)
       )
    {
      log.info("EmailNotificationListener disabled (both To and From email addresses must be provided in configuration).");
    }
  }
  
  /**
   * Represents someone (presumably, a system administrator) who will be
   * receiving e-mail alerts containing notifications.
   */
  static class Recipient
  {
    private final String email;
    private final String[] sources;
    private final Notification.Severity minSeverity;
    
    public Recipient(EnhancedProperties props, String prefix)
    {
      email = props.get(prefix + "Email");
      sources = props.getArray(prefix + "Sources");
      
      // Normalize sources.  An empty array will indicate all sources are 
      // accepted.
      for (int i = 0; i < sources.length; i++)
      {
        sources[i] = sources[i].toLowerCase();
      }
      
      minSeverity = props.getEnum(prefix + "MinSeverity", Notification.Severity.class, Notification.Severity.LOW);
    }
    
    @Override
    public String toString()
    {
      return "Recipient [" + email 
          + "; " 
          + (sources.length > 0 ? CollectionHelper.toString(sources, ",") : "all sources") 
          + "; " + minSeverity.ordinal() 
          + "]";
    }
  }
  
}
