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

package com.techempower.gemini.email;

import java.io.*;
import java.util.*;

import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;

import com.techempower.gemini.*;
import com.techempower.gemini.manager.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accepts EmailPackage objects and delivers them using JavaMail.
 *    <p>
 * As of Gemini 1.19, e-mail delivery has been enhanced with a notion of 
 * retries on delivery and multiple mail servers.  When delivering an e-mail,
 * Gemini will always try to use the mail servers in order.  Multiple mail
 * servers are <b>not</b> provided as a load-balancing measure, but rather
 * a fail-over measure.  When an e-mail exceeds the maximum number of delivery
 * attempts, it is considered "failed."  At that point, it is either purged
 * or written to an archive.  The archive consists of Externalized 
 * EmailPackage objects.  Currently, no provision exists for reading these
 * objects, but reading could be easily accomplished if needed.
 *    <p>
 * Note that the parameters that Gemini reads from the .conf file for e-mail
 * support have changed in Gemini 1.19.  The parameters are now:
 *   <ul>
 * <li>MailDeliveryRetries - The number of retries permitted before 
 *     considering an email to be "failed."  Default: 10.
 * <li>MailFailToDisk - If delivery of an e-mail fails to be delivered in
 *     the permitted amount of retries, it can be written to disk.  Default:
 *     no.
 * <li>MailFailureArchive - If MailFailToDisk is enabled, an archive
 *     filename should be provided.  Default: failed-emails.dat in current
 *     directory.
 * <li>MailDeliveryDomains - If provided, restricts outbound e-mails to only
 *     the domains provided by this comma-separated list.  Any outbound
 *     e-mails to other domains will be ignored.  By default, this attribute
 *     is not specified, which allows all outbound e-mails. TODO: The
 *     implementation of this is quite weak and just relies on checking the
 *     end of the "TO" String.
 * <li>MailServer1.ServerAddress - DNS or IP address of the mail server.
 *     If more that one mail server is specified by MailServerCount, use
 *     MailServer2.ServerAddress and so on to specify additional servers.
 *     Default: mail.
 * <li>MailServer1.SmtpPort - The port to use for outbound e-mail (if
 *     enabled for this mail server).  Default: 25.  (Was previously named 
 *     "ServerPort".)
 * <li>MailServer1.PopPort - The port to use for inbound e-mail (if enabled
 *     for this mail server).  Default: 110.
 * <li>MailServer1.MailProtocolInitTimeout - Timeout, in seconds, before 
 *     giving up on socket connections to the mail server or protocol 
 *     initiation. Default: 100.
 * <li>MailServer1.MailSocketIOTimeout - Timeout, in seconds, for IO with the
 *     mail server to be idle.  Default: 100.
 * <li>MailServer1.Username - The username to use for STMP-Auth for this
 *     mail server.  If no username is provided, SMTP-Auth will not be used.
 * <li>MailServer1.Password - The password to use for SMTP-Auth for this
 *     mail server.  Only used if a a username is also provided. 
 * <li>MailServer1.ServerRole - Specifies whether the server is used for
 *     outbound email, inbound email, or both.  Acceptable values are 
 *     "Outbound", "Inbound", and "Both".  By default, a mail server is
 *     assumed to be Outbound only.
 * <li>OutboundMailEnabled - Allows disabling outbound e-mail wholesale by
 *     setting this attribute to false.  By default outbound e-mail is 
 *     enabled.  (Was previously named "MailServerEnabled".)
 * <li>MailDefaultUsername - The username to use for STMP-Auth when an 
 *     EmailPackage defines its own mail server (uncommon usage scenario).  
 *     If no username is provided, SMTP-Auth will not be used.
 * <li>MailDefaultPassword - The password to use for SMTP-Auth when an 
 *     EmailPackage defines its own mail server (uncommon usage scenario).  
 *     Only used if a username is also provided. 
 *   </ul>
 *    <p>
 * Note that the timeout parameters and SMTP-Auth functionality only function
 * when using the Sun default JavaMail Provider.
 */
public class EmailTransport
  extends    BasicManager<GeminiApplication>
{
  //
  // Constants.
  //

  public static final String DEFAULT_SERVER_ADDRESS     = "mail";    // No domain specified.
  public static final String MULTIPLE_SERVER_PREFIX     = "MailServer";
  public static final int    DEFAULT_SMTP_PORT          = 25;
  public static final int    DEFAULT_POP3_PORT          = 110;
  public static final int    DEFAULT_POP3S_PORT         = 995;
  public static final int    DEFAULT_PROTO_INIT_TIMEOUT = 100;
  public static final int    DEFAULT_SOCKET_TIMEOUT     = 100;
  public static final int    DEFAULT_RETRIES            = 10;

  //
  // Member variables.
  //
  
  private int                 maximumRetries        = DEFAULT_RETRIES;
  private EmailServerDescriptor[]   mailServers     = new EmailServerDescriptor[0];
  private EmailServerDescriptor[]   outboundServers = new EmailServerDescriptor[0];
  private EmailServerDescriptor[]   inboundServers  = new EmailServerDescriptor[0];
  private final Object              lockObject            = new Object();
  //private boolean             failToDisk            = false;
  private boolean             outboundMailEnabled   = true;
  //private String              diskArchive           = "failed-emails.dat";
  private Collection<String>  deliveryDomains       = null;
  private String              defaultUsername       = null;
  private String              defaultPassword       = null;
  private Authenticator       defaultAuthenticator  = null;
  private Logger              log                   = LoggerFactory.getLogger(getClass());
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public EmailTransport(GeminiApplication application)
  {
    super(application);
  }

  /**
   * Gets an application prefix for this EmailTransport object.  Overload
   * this if you have multiple applications reading from a single config
   * file, and each application will have a separate set of mail servers.
   *    <p>
   * Returns an empty string by default.
   */
  public String getApplicationPrefix()
  {
    return "";
  }

  /**
   * Configure this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    final EnhancedProperties.Focus focus = props.focus(getApplicationPrefix());
    
    // Read general parameters (not specific to mail servers).
    final EnhancedProperties.Focus outboundFocus = focus.focus("OutboundEmail.");
    if (focus.has("MailDeliveryRetries"))
    {
      maximumRetries = focus.getInt("MailDeliveryRetries", maximumRetries);
      log.info("MailDeliveryRetries is deprecated.  Use OutboundEmail.Retries instead.");
    }
    maximumRetries = outboundFocus.getInt("Retries", maximumRetries);
        
    if (focus.has("MailServerEnabled"))
    {
      outboundMailEnabled = focus.getBoolean("MailServerEnabled", outboundMailEnabled);
      log.info("MailServerEnabled is deprecated.  Use OutboundEmail.Enabled instead.");
    }
    if (focus.has("OutboundMailEnabled"))
    {
      outboundMailEnabled = focus.getBoolean("OutboundMailEnabled", outboundMailEnabled);
      log.info("OutboundMailEnabled is deprecated.  Use OutboundEmail.Enabled instead.");
    }
    outboundMailEnabled = outboundFocus.getBoolean("Enabled", outboundMailEnabled);

    if (!outboundMailEnabled)
    {
      // Disable mail service.
      log.info("Outbound email disabled.  Server definitions are still processed to permit inbound email as needed.");
    }

    // Support the old "DeliveryDomains" value name as well as the new
    // MailDeliveryDomains.
    String domains  = focus.get("DeliveryDomains", "");
    domains = focus.get("MailDeliveryDomains", domains);
    
    // Read the default username and password using the original property 
    // names (with "Smtp" as a prefix) first, to support older configuration
    // files.  But ultimately, prefer the new property names.
    defaultUsername = focus.get("SmtpUsername", defaultUsername);
    defaultPassword = focus.get("SmtpPassword", defaultPassword);
    defaultUsername = focus.get("MailDefaultUsername", defaultUsername);
    defaultPassword = focus.get("MailDefaultPassword", defaultPassword);

    // Do not complete configuration until we can get a lock.
    synchronized (lockObject)
    {
      // Set up a default authenticator.
      if (StringHelper.isNonEmpty(defaultUsername))
      {
        defaultAuthenticator = new EmailAuthenticator(defaultUsername, defaultPassword);
      }
      else
      {
        defaultAuthenticator = null;
      }
      
      // Parse delivery domains.
      StringTokenizer domainTokenizer = new StringTokenizer(domains, ",");
      deliveryDomains = null;
      while (domainTokenizer.hasMoreTokens())
      {
        String domain = domainTokenizer.nextToken();
        if (deliveryDomains == null)
        {
          deliveryDomains = new ArrayList<>(2);
        }
        deliveryDomains.add(domain);
        if (outboundMailEnabled)
        {
          log.info("Email permitted to domain {}", domain);
        }
      }
      
      // Count mail servers.
      int serverCount = countMailServerDefinitions(props);
      
      // Read the configuration for each of the mail servers specified.
      mailServers = new EmailServerDescriptor[serverCount];
      for (int i = 0; i < serverCount; i++)
      {
        final EnhancedProperties.Focus serverFocus = focus.focus(MULTIPLE_SERVER_PREFIX + (i + 1) + ".");
        mailServers[i] = new EmailServerDescriptor(serverFocus);
      }
      
      // Cache references to outbound and inbound servers.
      deriveServerReferenceCaches(mailServers);
      
      // Show a little summary report.
      log.info("{} mail server{} specified{}", serverCount,
          StringHelper.pluralize(serverCount),
          serverCount > 0 ? ", shown below." : ".");
      for (int i = 0; i < serverCount; i++)
      {
        log.info("{} - {}", i + 1, mailServers[i]);
      }
    }
  }
  
  /**
   * Derives the cached references to inbound and outbound servers from
   * the provided array of all servers.
   */
  protected void deriveServerReferenceCaches(EmailServerDescriptor[] servers)
  {
    // Count them.
    int countIn = 0, countOut = 0;
    for (EmailServerDescriptor server : servers)
    {
      if (server.isInbound())
      {
        countIn++;
      }
      if (server.isOutbound())
      {
        countOut++;
      }
    }
    
    // Create the arrays.
    final EmailServerDescriptor[] inbound  = new EmailServerDescriptor[countIn];
    final EmailServerDescriptor[] outbound = new EmailServerDescriptor[countOut];
    
    countIn = 0;
    countOut = 0;
    
    // Now copy the references.
    for (EmailServerDescriptor server : servers)
    {
      if (server.isInbound())
      {
        inbound[countIn++] = server;
      }
      if (server.isOutbound())
      {
        outbound[countOut++] = server;
      }
    }
    
    // Update our caches.
    inboundServers = inbound;
    outboundServers = outbound;
  }
  
  /**
   * Determines how many mail servers are specified in the configuration file
   * by looking for the highest numbered server (MailServer1, MailServer2,
   * and so on).
   */
  public int countMailServerDefinitions(EnhancedProperties props)
  {
    int count = 0;
    String serverName = "";

    while ( (count == 0)
       || (props.has(serverName))
       )
    {
      count++;
      serverName = getApplicationPrefix() + MULTIPLE_SERVER_PREFIX 
        + count + ".ServerAddress";
    }
    
    return count - 1;
  }

  /**
   * Sends an email from an EmailPackage.  Returns a true if the message
   * was successfully sent, false if not.
   */
  public boolean sendEmail(EmailPackage email)
  {
    // If the email server is disabled, do not send an email.
    if (!outboundMailEnabled)
    {
      log.info("Outbound email disabled.");
      
      // Return true to indicate the mail is consumed.
      return true;
    }
    
    // Can't do anything if the email is null.  It really shouldn't be by the
    // time we're here (should have been caught by the EmailServicer), but
    // we'll do one more check.
    if (email != null)
    {
      // Set the charset string if one exists
      String charsetString = "";
      if(StringHelper.isNonEmpty(email.getCharset())) 
      {
        charsetString = "; charset=" + email.getCharset();
      }
      
      // Check to see if the e-mail is to a permitted domain.  If there is
      // no list of permitted domains, then all domains are permitted.
      if (deliveryDomains != null)
      {
        boolean permitted = false;
        for (String domain : deliveryDomains)
        {
          if (  (email.getRecipient() != null)
             && (email.getRecipient().toLowerCase().endsWith(domain))
             )
          {
            permitted = true;
            break;
          }
        }
        
        // Not a permitted domain.
        if (!permitted)
        {
          log.info("Mail not permitted to {}", email.getRecipient());

          // Return true to indicate the mail is consumed.
          return true;
        }
      }
      
      // Determine how many retries this mail has been through.
      int tryNumber = email.getDeliveryAttempts();
    
      // A reference to the mail session, set by finding the mail server
      // below.
      Session mailSession = null;

      // Use the default mail server if none is specified in the
      // EmailPackage.
      if (email.getMailServer() == null)
      {
        // Get the properties for the mail server for this try number.  Try 
        // numbers are divided by the number of mail servers, so that if
        // it's try 5 and there are 3 mail servers, we use mail server 2.
        // 5 % 3 = 2 remainder.

        EmailServerDescriptor[] servers = getOutboundServers();
        if (servers.length > 0)
        {
          mailSession = servers[tryNumber % servers.length].getSession();
        }
        else
        {
          log.info("Mail failed to send because there are no servers defined.");
          return false;
        }
      }
      else
      {
        // If a recipient source is specified, check to see if the mail server specified exists
        // in our list of outbounds that also specifies the same recipient source.
        if (StringHelper.isNonEmpty(email.getRecipientSource()))
        {
          for (EmailServerDescriptor descriptor : getOutboundServers())
          {
            if (descriptor.getServerAddress().equalsIgnoreCase(email.getMailServer())
                && email.getRecipientSource().equalsIgnoreCase(descriptor.getProperties().getProperty("mail.smtp.recipientsource")))
            {
              mailSession = descriptor.getSession();
              break;
            }
          }
        }

        // If no recipient source was specified or no matching mail server with recipient source
        // was found, check to see if only the mail server specified exists in our list of outbounds.
        if (mailSession == null)
        {
          for (EmailServerDescriptor descriptor : getOutboundServers())
          {
            if (descriptor.getServerAddress().equalsIgnoreCase(email.getMailServer())
                && (StringHelper.isEmpty(descriptor.getProperties().getProperty("mail.smtp.recipientsource"))))
            {
              mailSession = descriptor.getSession();
              break;
            }
          }
        }

        // If there is a mail server specified in the EmailPackage itself,
        // but it was not listed amongst the outbound servers,
        // we'll use that.  Note that this is not very common.
        if (mailSession == null)
        {
          Properties props = new Properties();
          props.put("mail.smtp.host", email.getMailServer());
          props.put("mail.smtp.connectiontimeout", "" + (UtilityConstants.SECOND 
              * DEFAULT_PROTO_INIT_TIMEOUT));
          props.put("mail.smtp.timeout", "" + (UtilityConstants.SECOND 
              * DEFAULT_SOCKET_TIMEOUT));
          
          // Request that SMTP authentication be used if we have an Authenticator.
          if (defaultAuthenticator != null || email.getEmailAuthenticator() != null)
          {
            props.put("mail.smtp.auth", "true");
          }

          if (email.getEmailAuthenticator() != null)
          {
            mailSession = javax.mail.Session.getInstance(props, email.getEmailAuthenticator());
          }
          else
          {
            // Get a new session for these properties.
            mailSession = javax.mail.Session.getInstance(props, defaultAuthenticator);
          }
        }
      }

      // Create a reference to the MimeMessage.
      MimeMessage message;
      boolean alternativeEmail = email.isTextEnabled() && email.isHtmlEnabled();

      // Construct the MimeMessage.
      try
      {
        message = new MimeMessage(mailSession);
        message.addFrom(InternetAddress.parse(email.getAuthor(), false));
        message.setSentDate(new Date());

        // Set the reply-to to the same as from.
        message.setReplyTo(InternetAddress.parse(email.getAuthor(), false));

        message.addRecipients(javax.mail.Message.RecipientType.TO,
                              InternetAddress.parse(email.getRecipient(), false));

        // Set the BCC address if specified.
        if (StringHelper.isNonEmpty(email.getBccRecipient()))
        {
          message.addRecipients(javax.mail.Message.RecipientType.BCC,
              InternetAddress.parse(email.getBccRecipient(), false));
        }

        message.setSubject(email.getSubject(), email.getCharset());
        //
        // Setting the headers of the email.  If the email package has
        // headers, use those.  If not, use the default gemini headers.
        //
        if (email.getHeaders() != null && !email.getHeaders().isEmpty())
        {
          for (EmailHeader emailHeader : email.getHeaders())
          {
            message.addHeader(emailHeader.getHeaderName(),
                emailHeader.getHeaderValue());
          }
        }
        else
        {
          message.addHeader("X-Gemini-EBT", email.getRecipient());
        }
        
        // First, check to see if there are attachments or if the package has
        // both text and HTML bodies.  If none of these are true, we can avoid
        // all of this multipart nonsense.
        if ( alternativeEmail ||
             email.hasAttachments())
        {
          MimeMultipart multipart;

          // If the email has both text and HTML, we need to use the 
          // 'alternative' Multipart subtype.  Otherwise, a regular
          // Multipart subtype is sufficient.
          if (alternativeEmail)
          {
            // Create the alternative Multipart.
            multipart = new MimeMultipart("alternative");

            // Add text part.
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(email.getTextBody(), GeminiConstants.CONTENT_TYPE_TEXT + charsetString);
            multipart.addBodyPart(textPart);

            // Add html part.
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(email.getHtmlBody(), GeminiConstants.CONTENT_TYPE_HTML + charsetString);
            multipart.addBodyPart(htmlPart);
          }

          // Only HTML or text, exclusively, is enabled.  We do not
          // need the 'alternative' Multipart subtype.
          else
          {
            // Create the regular Multipart.
            multipart = new MimeMultipart();

            MimeBodyPart thePart = new MimeBodyPart();
            if (email.isHtmlEnabled())
            {
              thePart.setContent(email.getHtmlBody(), GeminiConstants.CONTENT_TYPE_HTML + charsetString);
            }
            else
            {
              // We assume that if HTML is not enabled, then text is.
              thePart.setContent(email.getTextBody(), GeminiConstants.CONTENT_TYPE_TEXT + charsetString);
            }
            multipart.addBodyPart(thePart);
          }

          //
          // Add the attachments, if any exist.
          //
          // Because the email system is so well thought out, if you have
          // html, txt and attachments, you have use a real kludgy way
          // of making the email.
          //
          if (email.hasAttachments() && 
              alternativeEmail)
          {
            Multipart mixed = new MimeMultipart("mixed");
            MimeBodyPart wrap = new MimeBodyPart();
            wrap.setContent(multipart);
            mixed.addBodyPart(wrap);
            
            Collection<EmailAttachment> attachments = email.getAttachments();
            Iterator<?> iter = attachments.iterator();
            while (iter.hasNext())
            {
              MimeBodyPart filePart = new MimeBodyPart();

              EmailAttachment attachment = (EmailAttachment)iter.next();

              // If the attachment object is a Java Mail Message object then
              // the attachment is an email message. To handle this we have to
              // use content type "message/rfc822".
              if (attachment.getObjectAttachment() instanceof Message)
              {
                filePart.setContent(attachment.getObjectAttachment(), "message/rfc822");
                filePart.setFileName(attachment.getName());
              }
              else
              {
                File file = attachment.getFile();
                DataHandler dh = new DataHandler(new FileDataSource(file));
                if(attachment.getName() != null)
                {
                  filePart.setFileName(attachment.getName());
                }
                else
                {
                  filePart.setFileName(file.getName());
                }
                filePart.setDataHandler(dh);
              }

              mixed.addBodyPart(filePart);
	          }
            message.setContent(mixed);
          }
          else if (email.hasAttachments())
          {
            Collection<EmailAttachment> attachments = email.getAttachments();
            Iterator<?> iter = attachments.iterator();
            while (iter.hasNext())
            {
              EmailAttachment attachment = (EmailAttachment)iter.next();
              MimeBodyPart filePart = new MimeBodyPart();

              // If the attachment object is a Java Mail Message object then
              // the attachment is an email message. To handle this we have to
              // use content type "message/rfc822".
              if (attachment.getObjectAttachment() instanceof Message)
              {
                filePart.setContent(attachment.getObjectAttachment(), "message/rfc822");
                filePart.setFileName(attachment.getName());
              }
              else
              {
                File file = attachment.getFile();
                DataHandler dh = new DataHandler(new FileDataSource(file));
                filePart.setFileName(file.getName());
                filePart.setDataHandler(dh);
              }
              multipart.addBodyPart(filePart);
	          }
            message.setContent(multipart);
          }
          else
          {
            message.setContent(multipart);
          }
        }

        // No HTML or attachments.  We do not need a Multipart at all!  If
        // HTML is enabled, we'll create a single part with text/html.
        else if (email.isHtmlEnabled())
        {
          message.setContent(email.getTextBody(), GeminiConstants.CONTENT_TYPE_HTML + charsetString);
        }
        // Single part, plain text.
        else
        {
          // We assume that if HTML is not enabled, then text is.
          message.setContent(email.getTextBody(), GeminiConstants.CONTENT_TYPE_TEXT + charsetString);
        }

        // Attempt the delivery.
        try
        {
          // This method -should- return either very quickly or within the
          // timeout period specified in the Properties file provided
          // during the construction of the Session object.
          Transport.send(message);

          // Set the sent flag.
          email.setSent(true);

          return true;
        }
        catch (MessagingException exc)
        {
          email.setSent(false);
          log.warn("Exception during JavaMail transport.", exc);
        }
      } 
      catch (MessagingException exc)
      {
        email.setSent(false);
        log.warn("Exception prior to JavaMail transport.", exc);
      }
      finally
      {
        // Nullify the MimeMessage object.  This is superfluous.
        message = null;
      }
    }
    else
    {
      // Oops, the EmailPackage was null.
      log.info("Email is null!");
    }

    // If we get here, there was a problem.
    return false;
  }

  /**
   * Checks for new messages on a given server.  Returns a collection of
   * EmailPackage objects or null if no new messages have been received.
   * 
   * @param server a reference to the server to check
   * @param handler an EmailHandler to call back to process each e-mail; this
   * is typically the EmailDispatcher, which in turn dispatches the Email to
   * a list of EmailHandlers.
   */
  public int checkForMail(EmailServerDescriptor server, 
    EmailHandler handler)
  {
    int processed = 0;    
    Folder folder = null;
    Store store = null;
    
    try 
    {
      store = server.getStore();
      store.connect();
      folder = store.getFolder("INBOX");
      folder.open(Folder.READ_WRITE);
      
      // Get the messages.
      Message[] messages = folder.getMessages();
      EmailPackage email;
      boolean delete; 
      
      //log.debug("Messages: " + messages);
      
      // Proceed if there are messages.
      if ( (messages != null)
        && (messages.length > 0)
        )
      {        
        // Process each message.
        for (Message message : messages)
        {
          // Convert the message to an Email Package and mark the message
          // as deleted.
          //log.debug("Reading " + messages[i]);
          email = convertMessageToEmailPackage(message);

          // If the message didn't convert well, we can't process it.
          if (email != null)
          {
            // Ask the handler to process the email.
            //log.debug("Handling " + email);
            delete = handler.handleEmail(email);
            processed++;
          }
          else
          {
            // Didn't process; delete it.
            delete = true;
          }
          
          // Mark the message to be deleted if the Handler so requested or
          // if the message couldn't be converted.
          if (delete)
          {
            message.setFlag(Flags.Flag.DELETED, true);
          }
        }
      }
    }
    catch (AuthenticationFailedException afexc)
    {
      log.warn("Authentication failure while checking for mail.", afexc);
    }
    catch (Exception exc)
    {
      log.warn("Exception while checking for mail.", exc);
    }
    finally
    {
      try
      {
        // Close the folder.
        if (folder != null)
        {
          // Close and purge emails.
          folder.close(true);
        }

        // Close the javax.mail.Store.
        if (store != null)
        {
          store.close();
        }
      }
      catch (javax.mail.MessagingException mexc)
      {
        log.warn("Exception while closing store/folder.", mexc);
      }
    }
    
    return processed;
  }
  
  /**
   * Converts a javax.mail.Message object to an EmailPackage object.
   * Returns null if the message cannot be converted correctly.
   */
  protected EmailPackage convertMessageToEmailPackage(Message message)
  {
    // Elements of an EmailPackage.
    String subject         = "";
    StringBuilder messageBody     = new StringBuilder();
    StringBuilder messageHtmlBody = new StringBuilder();
    String recipient       = "";
    String author          = "";
    List<EmailAttachment> attachments  = new ArrayList<>(2);
    
    // Get the subject.
    try
    {
      subject = message.getSubject();
    }
    catch (MessagingException mexc)
    {
      // Do nothing.
    }
    
    // Get the recipient.
    try
    {
      recipient = message.getRecipients(Message.RecipientType.TO)[0].toString();
    }
    catch (Exception exc) 
    {
      // Do nothing.
    }
    
    // Get the author.
    try
    {
      author = message.getFrom()[0].toString();
    }
    catch (Exception exc) 
    {
      // Do nothing.
    }

    try
    {
      // Process the body.  This is where it gets complicated.  First we need
      // to look at the content type.
      processBody(message, messageBody, messageHtmlBody, attachments);
      
      // Create the EmailPackage to return.
      EmailPackage toReturn = new EmailPackage(
        subject, 
        messageBody.toString(), 
        messageHtmlBody.toString(), 
        recipient, 
        author);
      
      // Add the attachments.
      if (attachments.size() > 0)
      {
        toReturn.setAttachments(attachments);
      }

      return toReturn;
    }
    catch (MessagingException exc)
    {
      log.warn("MessagingException while processing e-mail.", exc);
    }
    catch (IOException exc)
    {
      log.warn("IOException while processing e-mail.", exc);
    }
    
    return null;
  }
  
  /**
   * Processes the body of a message.
   */
  protected void processBody(Message message, StringBuilder plaintext, 
    StringBuilder html, List<EmailAttachment> attachments)
    throws MessagingException, IOException
  {
    String contentType = message.getContentType();
    
    // Plain text messages are the easiest (and best!)
    if (contentType.startsWith("text/plain"))
    {
      log.debug("Adding plaintext part.");
      plaintext.append((String)message.getContent());
    }
    // Some jokers send only HTML messages.  They are jokers.
    else if (contentType.startsWith("text/html"))
    {
      log.debug("Adding html part.");
      // Store the HTML in the HtmlBody but let's also store a reference
      // in the plaintext body as well.
      html.append((String)message.getContent());
      plaintext.append((String)message.getContent());
    }
    // The content type will start with multipart if we have attachments
    // or a mixed-mode e-mail.
    else if (contentType.startsWith("multipart"))
    {
      log.debug("Parsing multipart email.");
      if (message.getContent() instanceof Multipart)
      {
        Multipart multi = (Multipart)message.getContent();
        processMultipart(multi, plaintext, html, attachments);
      }
      else
      {
        log.info("Content type identified as multipart, but content is not!");
      }
    }
  }
  
  /**
   * Processes a Multipart message part.
   */
  protected void processMultipart(Multipart multi, StringBuilder plaintext, 
    StringBuilder html, List<EmailAttachment> attachments)
    throws MessagingException, IOException
  {
    int partCount = multi.getCount();
    
    // Iterate through each Part of the Multipart.
    for (int i = 0; i < partCount; i++)
    {
      BodyPart part = multi.getBodyPart(i);
      String partType = part.getContentType();
      String filename = part.getFileName();
      int size = part.getSize();
      //log.debug("Part: " + part);
      log.debug("Type: {}; filename: {}; {} bytes.", partType, filename, size);
                  
      // Is the part plaintext?
      if (partType.startsWith("text/plain"))
      {
        log.debug("Adding plaintext part.");
        plaintext.append((String)part.getContent());
      }
      // How about HTML?
      else if (partType.startsWith("text/html"))
      {
        log.debug("Adding HTML part.");
        html.append((String)part.getContent());
      }
      else if ( (partType.startsWith("message"))
             && (part.getContent() instanceof Message)
             )
      {
        log.debug("Recursing embedded message.");
        processBody((Message)part.getContent(), plaintext, html, attachments);
      }
      // An attached file?
      else if (StringHelper.isNonEmpty(filename))
      {
        InputStream is = part.getInputStream();
        log.debug("Adding attachment part with {} available bytes.",
            is.available());
        EmailAttachment attachment = new EmailAttachment(
          is, filename);
        attachments.add(attachment);
      }
      else if (part.getContent() instanceof Multipart)
      {
        log.debug("Recursing embedded multipart.");
        processMultipart((Multipart)part.getContent(), plaintext, html, 
          attachments);
      }
      else
      {
        log.info("Unknown part. Content: " + part.getContent());
      }
    }
  }
  
  /**
   * Get the reference to the outbound mail servers.
   */
  public EmailServerDescriptor[] getOutboundServers()
  {
    return outboundServers;
  }

  /**
   * Get the reference to the inbound mail servers.
   */
  public EmailServerDescriptor[] getInboundServers()
  {
    return inboundServers;
  }

  /**
   * Standard Java toString.
   */
  @Override
  public String toString()
  {
    return "EmailTransport [" + mailServers.length + " servers; " + hashCode() + "]";
  }

  /**
   * Gets the maximum number of retries.
   */
  public int getRetryLimit()
  {
    return maximumRetries;
  }

  /**
   * @return the deliveryDomains
   */
  public Collection<String> getDeliveryDomains()
  {
    return deliveryDomains;
  }
  
  /**
   * Is outbound email delivery enabled?
   */
  public boolean isOutboundEnabled()
  {
    return outboundMailEnabled;
  }

}   // End EmailTransport.
