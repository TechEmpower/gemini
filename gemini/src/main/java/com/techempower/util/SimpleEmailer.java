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

package com.techempower.util;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import com.techempower.helper.*;

/**
 * SimpleEmailer is intended for use from a command line.  It can be used
 * to send an e-mail to a specified e-mail server using a template file and
 * a list of "macros" to insert into the template.  Theoretically, it could
 * be used from within an application, but generally the gemini.email
 * package should be used for that purpose.
 */
public class SimpleEmailer
{
  
  //
  // Member variables.
  //
  
  private String mailServer;
  
  //
  // Member methods.
  //

  /**
   * Empty constructor.
   */
  public SimpleEmailer(String mailServer)
  {
    this.mailServer = mailServer;
  }
  
  /**
   * Sends an e-mail.
   */
  public boolean sendEmail(String username, String password, String from, String to, 
    String subject, String body, Map<String,Object> macros)
  {
    String messageBody = StringHelper.macroExpand(macros, body);
    String messageSubject = StringHelper.macroExpand(macros, subject);
    return sendEmail(username, password, from, to, messageSubject, messageBody);
  }
  
  /**
   * Sends an e-mail (no macros).
   */
  public boolean sendEmail(String username, String password, String from, 
    String to, String subject, String body)
  {
    // A reference to the mail session, set by finding the mail server
    // below.
    Session mailSession;

    // If there is a mail server specified in the EmailPackage itself, 
    // we'll use that.  Note that this is not very common.
    Properties props = new Properties();
    props.put("mail.smtp.host", this.mailServer);
    props.put("mail.smtp.connectiontimeout", "60000");  // 60 seconds
    props.put("mail.smtp.timeout", "60000");  // 60 seconds
    
    Authenticator authenticator = null;
    if (StringHelper.isNonEmpty(username))
    {
      authenticator = new EmailAuthenticator(username, password); 
      props.put("mail.smtp.auth", "true");
    }

    // Get a new session for these properties.
    mailSession = javax.mail.Session.getInstance(props, authenticator);

    // Create a reference to the MimeMessage.
    MimeMessage message;

    // Construct the MimeMessage.
    try
    {
      message = new MimeMessage(mailSession);
      message.addFrom(InternetAddress.parse(from, false));
      message.setReplyTo(InternetAddress.parse(from, false));
      message.addRecipients(javax.mail.Message.RecipientType.TO,
                            InternetAddress.parse(to, false));
      message.setSubject(subject);
      message.setContent(body, "text/plain");

      // Attempt the delivery.
      try
      {
        // This method -should- return either very quickly or within the
        // timeout period specified in the Properties file provided
        // during the construction of the Session object.
        Transport.send(message);
      }
      catch (Exception exc)
      {
        log("Exception during JavaMail transport: " + exc);
        return false;
      }
    }
    catch (Exception exc)
    {
      log("Exception prior to JavaMail transport:" + exc);
      return false;
    }
   
    return true;
  }
  
  /**
   * Process command line options.
   */
  public static void main(String[] args)
  {
    if (args.length >= 5)
    {
      String server  = args[0];
      String from    = args[1];
      String to      = args[2];
      String subject = args[3];
      String file    = args[4];
      
      int macrosStart = 5;
      
      String username = "";
      String password = "";
      if ((args.length >= 7) && ("-u".equals(args[5])))
      {
        username = args[6];
        macrosStart = 7;
      }
      if ((args.length >= 9) && ("-p".equals(args[7])))
      {
        password = args[8];
        macrosStart = 9;
      }
      
      log("Srv:  " + server);
      log("From: " + from);
      log("To:   " + to);
      log("Subj: " + subject);
      log("Body: " + file);
      if (StringHelper.isNonEmpty(username))
      {
        log("User: " + username);
      }
      if (StringHelper.isNonEmpty(password))
      {
        log("Pass: Provided");
      }
      
      Map<String,Object> macros = new HashMap<>();
      for (int i = macrosStart; i < args.length; i++)
      {
        int index = i - macrosStart + 1;
        if (index < 10)
        {
          macros.put("$0" + index, args[i]);
          log("$0" + index + ":  " + args[i]);
        }
        else
        {
          macros.put("$" + index, args[i]);
          log("$" + index + ":  " + args[i]);
        }
      }
      
      Date now = new Date();
      DateFormat timeFormat = new SimpleDateFormat("HH:mm");
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      
      macros.put("$time", timeFormat.format(now));
      macros.put("$date", dateFormat.format(now));
      
      String body = "";
      
      try (
          FileInputStream fis = new FileInputStream(file)
          )
      {
        byte[] buffer = new byte[fis.available()];
        fis.read(buffer, 0, buffer.length);
        body = new String(buffer);
      }
      catch (IOException ioexc)
      {
        log("IOException: " + ioexc);
      }
      
      SimpleEmailer emailer = new SimpleEmailer(server);
      emailer.sendEmail(username, password, from, to, subject, body, macros);
    }
    else
    {
      log("SimpleEmailer usage");
      log("SimpleEmailer.bat mailserver from to subject templatefile [-u username] [-p password] macro1..n");
    }
  }
  
  /**
   * Displays messages on System.out.
   */
  public static void log(String logString)
  {
    System.out.println("SmEm: " + logString);
  }

  /**
   * Authenticator subclass.
   */
  static class EmailAuthenticator
    extends    Authenticator
  {
    private final PasswordAuthentication pauth;
  
    /**
     * Constructor
     */
    public EmailAuthenticator(final String username, final String password)
    {
      this.pauth = new PasswordAuthentication(username, password);
    }
    
    /**
     * Gets a PasswordAuthentication object.
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
      return this.pauth; 
    }
    
  }

}  // End SimpleEmailer.

