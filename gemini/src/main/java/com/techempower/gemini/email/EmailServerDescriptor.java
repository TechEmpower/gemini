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

import java.util.*;

import javax.mail.*;

import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * Descriptive class for Mail Servers.  Each mail server has a DNS name
 * and some other attributes.  A mail server can be an Outbound server,
 * Inbound server, or both simultaneously.  Typical usage is Outbound only
 * but some applications process Inbound email as well.
 */
public class EmailServerDescriptor
{
  
  //
  // Constants.
  //
  
  public static final String   ROLE_OUTBOUND = "Outbound";
  public static final String   ROLE_INBOUND  = "Inbound";
  public static final String   ROLE_BOTH     = "Both";
  
  public static final String   INBOUND_POP3  = "pop3";
  public static final String   INBOUND_POP3S = "pop3s";
  
  //
  // Member variables.
  //

  private final String        serverAddress;
  private final int           smtpPort;
  private final int           pop3Port;
  private final int           pop3sPort;
  private final long          timeoutProtoInit;
  private final long          timeoutSocket;
  private final Properties    serverProps;
  private final Authenticator authenticator;
  private final boolean       outbound;
  private final boolean       inbound;
  private final String        inboundProtocol;

  //
  // Member methods.
  //

  /**
   * Minimal constructor.  Depends on calling configure() later.
   */
  protected EmailServerDescriptor(EnhancedProperties.Focus props)
  {
    serverAddress = props.get("ServerAddress", EmailTransport.DEFAULT_SERVER_ADDRESS);
    
    // Determine the server's role.
    final String role = props.get("ServerRole", ROLE_OUTBOUND);
    if (ROLE_INBOUND.equalsIgnoreCase(role))
    {
      outbound = false;
      inbound = true;
    }
    else if (ROLE_BOTH.equalsIgnoreCase(role))
    {
      outbound = true;
      inbound = true;
    }
    else
    {
      outbound = true;
      inbound = false;
    }
    
    // Determine the server's inbound protocol.
    final String ibProt = props.get("InboundProtocol", INBOUND_POP3);
    if (ibProt.equalsIgnoreCase(INBOUND_POP3S))
    {
      inboundProtocol = INBOUND_POP3S;
      pop3sPort = props.getInt("Pop3sPort", EmailTransport.DEFAULT_POP3S_PORT);
      pop3Port = -1;
    }
    else 
    {
      inboundProtocol = INBOUND_POP3;
      pop3Port = props.getInt("PopPort", EmailTransport.DEFAULT_POP3_PORT);
      pop3sPort = -1;
    }

    // Support legacy "ServerPort" property name.
    int newSmtpPort  = props.getInt("ServerPort", EmailTransport.DEFAULT_SMTP_PORT);
    smtpPort = props.getInt("SmtpPort", newSmtpPort);
    
    timeoutProtoInit = props.getInt("MailProtocolInitTimeout", 
        EmailTransport.DEFAULT_PROTO_INIT_TIMEOUT) * UtilityConstants.SECOND;
    timeoutSocket    = props.getInt("MailSocketIOTimeout", 
        EmailTransport.DEFAULT_SOCKET_TIMEOUT) * UtilityConstants.SECOND;
    
    // Read authentication information using legacy property names that
    // included the prefix "Smtp".
    String username = props.get("SmtpUsername", "");
    String password = props.get("SmtpPassword", "");

    // Read authentication information using the new general property names,
    // preferring these if available.
    username = props.get("Username", username);
    password = props.get("Password", password);
    
    // Read recipient source information to distinguish different
    // authentication information that is used for the same mail server.
    final String recipientSource = props.get("RecipientSource", "");

    // Construct Authenticator if required.
    if (StringHelper.isNonEmpty(username))
    {
      authenticator = new EmailAuthenticator(username, password);
    }
    else
    {
      authenticator = null;
    }
    
    // Create the server's properties.
    serverProps = new Properties();

    // Add SMTP properties if the server is an Outbound server.
    if (isOutbound())
    {
      serverProps.put("mail.smtp.host", serverAddress);
      serverProps.put("mail.smtp.port", "" + smtpPort);
      serverProps.put("mail.smtp.connectiontimeout", "" + timeoutProtoInit);
      serverProps.put("mail.smtp.timeout", "" + timeoutSocket);
      if (authenticator != null)
      {
        serverProps.put("mail.smtp.auth", "true");
      }
      serverProps.put("mail.smtp.recipientsource", recipientSource);
    }
    
    // Add POP3 properties if the server is an Inbound server.
    if (isInbound())
    {
      if (isPop3S())
      {
        serverProps.put("mail.pop3s.host", serverAddress);
        serverProps.put("mail.pop3s.port", "" + pop3sPort);
        serverProps.put("mail.pop3s.auth", "true");
        serverProps.put("mail.pop3s.starttls.enable", "true");
        serverProps.put("mail.pop3s.connectiontimeout", "" + timeoutProtoInit);
        serverProps.put("mail.pop3s.timeout", "" + timeoutSocket);
      }
      else
      {
        serverProps.put("mail.pop3.host", serverAddress);
        serverProps.put("mail.pop3.port", "" + pop3Port);
        serverProps.put("mail.pop3.connectiontimeout", "" + timeoutProtoInit);
        serverProps.put("mail.pop3.timeout", "" + timeoutSocket);
      }
    }
  }

  /**
   * Gets the JavaMail Session object associated with this mail server.
   */
  protected Session getSession()
  {
    return Session.getInstance(getProperties(), authenticator);
  }

  /**
   * Gets an inhound Store. 
   */
  protected Store getStore()
  {
    if (isInbound())
    {
      try
      {
        return getSession().getStore(inboundProtocol);
      }
      catch (NoSuchProviderException exc)
      {
        // TODO: Raise a notification.
      }
    }
    
    return null;
  }
  
  /**
   * Gets a properly formatted Properties object for this mail server.
   */
  protected Properties getProperties()
  {
    return serverProps;
  }
  
  /**
   * Is this an Outbound mail server?
   */
  public boolean isOutbound()
  {
    return outbound;
  }
  
  /**
   * Is this an Inbound mail server?
   */
  public boolean isInbound()
  {
    return inbound;
  }
  
  /**
   * Is this an Inbound server using POP3S?
   */
  public boolean isPop3S()
  {
    return (isInbound() && pop3sPort > -1);
  }
  
  /**
   * Returns the serverAddress
   */
  public String getServerAddress()
  {
    return serverAddress;
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "MailServer [" + serverAddress 
      + (isInbound() ? "; Inbound (" + inboundProtocol + " port " + (isPop3S() ? pop3sPort : pop3Port) + ")" : "") 
      + (isOutbound() ? "; Outbound (port " + smtpPort + ")" : "") 
      + (authenticator != null ? "; " + authenticator : "")
      + "]";
  }
  
}  // End EmailServerDescriptor.
