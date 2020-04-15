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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.techempower.gemini.*;
import com.techempower.gemini.email.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a simple e-mail templating functionality to Gemini applications.
 * An application can subclass EmailTemplater to read in application e-mail
 * templates.  Then, constructing a templatized e-mail is as easy as calling 
 * getEmail() to receive an EmailPackage.
 *
 * @see EmailPackage
 */
public class SimpleEmailTemplater
  extends    EmailTemplater
{

  //
  // Member variables.
  //

  private final ConcurrentHashMap<String,EmailPackage> templates = new ConcurrentHashMap<>();
  
  private List<String> templatesToLoad;
  private boolean      requireSubject = true;
  private Logger       log            = LoggerFactory.getLogger(getClass());

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public SimpleEmailTemplater(GeminiApplication application)
  {
    super(application);
  }

  /**
   * Configures this component.  Overload this method to read in additional
   * attributes such as a commonly used e-mail author.  Be sure to call
   * super.configure(props) somewhere in the subclass's implementation!
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    super.configure(props);
    
    loadTemplates(props);
  }
  
  /**
   * Loads e-mails that have been specified using addEmailToLoad method.
   */
  public int loadTemplates(EnhancedProperties props)
  {
    int loaded = 0;
    
    if (this.templatesToLoad != null)
    {
      for (String templateID : this.templatesToLoad)
      {
        loaded += addEmail(props, templateID);
      }
      this.templatesToLoad = null;
    }
    
    log.info("{} e-mail{} loaded.", loaded, StringHelper.pluralize(loaded));
    
    return loaded;
  }
  
  @Override
  public void addTemplateToLoad(String templateID)
  {
    if (this.templatesToLoad == null)
    {
      this.templatesToLoad = new ArrayList<>();
    }
    
    this.templatesToLoad.add(templateID);
  }

  @Override
  public EmailPackage process(String templateID, Map<String, ? extends Object> macros, 
    String authorAddress, String recipientAddress)
  {
    final EmailPackage emailPkg = this.templates.get(templateID);
      
    // If the package exists.
    if ((emailPkg != null) && (emailPkg.getTextBody() != null))
    {
      // Do the macro expansion on both the message body and the subject.
      final String messageBody = StringHelper.macroExpand(macros, emailPkg.getTextBody());
      final String messageSubj = StringHelper.macroExpand(macros, emailPkg.getSubject());

      final EmailPackage newPackage = new EmailPackage(messageSubj, messageBody, 
        recipientAddress, authorAddress);
      
      // If there's an HTML body in the template, be sure to grab that and
      // convert its macros as well.
      if (emailPkg.isHtmlEnabled())
      {
        final String messageHtmlBody = StringHelper.macroExpand(macros, emailPkg.getHtmlBody());
        newPackage.setHtmlBody(messageHtmlBody);
      }

      return newPackage;
    }
    else
    {
      log.info("Template not found by ID: {}", templateID);
    }
    
    return null;
  }
  
  @Override
  public EmailPackage process(String plainBody, String htmlBody, String subject, 
      Map<String, ? extends Object> data, String authorAddress, String recipientAddress)
  {
    // Do the macro expansion on both the message body and the subject.
    final String messageBody = StringHelper.macroExpand(data, plainBody);
    final String htmlMessageBody = StringHelper.macroExpand(data, htmlBody);
    final String messageSubj = StringHelper.macroExpand(data, subject);

    return new EmailPackage(messageSubj, messageBody, htmlMessageBody,
        recipientAddress, authorAddress);
  }

  @Override
  public EmailPackage process(String body, String subject, 
      Map<String, ? extends Object> data, String authorAddress, String recipientAddress)
  {
    // Do the macro expansion on both the message body and the subject.
    final String messageBody = StringHelper.macroExpand(data, body);
    final String messageSubj = StringHelper.macroExpand(data, subject);

    return new EmailPackage(messageSubj, messageBody, recipientAddress,
        authorAddress);
  }
  
  //
  // Template-loading methods.
  //

  /**
   * Adds zero or more e-mail templates defined in an application's
   * configuration file, where the name of the e-mails starts with a 
   * particular prefix in the config file.  Note that the prefix will 
   * -not- be appended to the internal hashtable.  In other words, if
   * an e-mail template named Foo had its filename defined in the config
   * file as "Emails.Foo", this method would be invoked with "Emails." as
   * its prefix, but the e-mail template would still be named "Foo" and not
   * "Emails.Foo".   
   */
  public int addEmails(EnhancedProperties props, String propertyPrefix)
  {
    Set<String> propertyNames = props.names();
    int count = 0;
    
    // Look for properties starting with the provided prefix.
    for (String propertyName : propertyNames)
    {
      if (propertyName.startsWith(propertyPrefix))
      {
        // Load it.
        String filename = props.get(propertyName);
        String emailName = propertyName.substring(propertyPrefix.length());
        //log.debug("Email name: " + emailName);
        count += addEmail(filename, emailName);
      }
    }
    
    return count;
  }
  
  /**
   * Reads a stock e-mail from a file using a filename gathered from
   * the properties file.  See the addEmail(String, String) for more
   * details.
   */
  protected int addEmail(EnhancedProperties props, String mailID)
  {
    String filename = props.get(mailID);

    return addEmail(filename, mailID);
  }
  
  /**
   * Reads a stock e-mail from a file.  Returns boolean success as an
   * int (1 for good, 0 for no good).  This allows easy counting.
   *   <p>
   * Note that an HTML alternate will be automatically read in at the same
   * time as the plaintext file if and only if a file with the same filename
   * (except with an .html extension) exists alongside the plaintext file.
   *   <p>
   * Assumes an absolute path to a file is provided.
   *
   * @param filename the filename of the file containing the template; the
   *        filename should be absolute.
   * @param mailID the identification for this template.
   */
  protected int addEmail(String filename, String mailID)
  {
    // If there's a filename, go ahead.
    if (filename != null)
    {
      log.debug("Attempting to load {}", filename);
      
      final File textFile = new File(filename);
      final String htmlFilename = FileHelper.replaceExtension(filename, "html");
      final File htmlFile = new File(htmlFilename);
      
      try (
          InputStream textIs = textFile.exists() ? new FileInputStream(textFile) : null;
          Reader textReader = textIs != null ? new InputStreamReader(textIs) : null;
          InputStream htmlIs = htmlFile.exists() ? new FileInputStream(htmlFile) : null;
          Reader htmlReader = htmlIs != null ? new InputStreamReader(htmlIs) : null
          )
      {
        return addEmail(textReader, htmlReader, mailID);
      }
      catch (IOException ioexc)
      {
        log.error("Cannot read email contents for {}", mailID, ioexc);
      }
    }

    return 0;
  }
  
  /**
   * Reads a stock e-mail from a file.  Returns boolean success as an
   * int (1 for good, 0 for no good).  This allows easy counting.
   *   <p>
   * Note that an HTML alternate will be automatically read in at the same
   * time as the plaintext file if and only if a file with the same filename
   * (except with an .html extension) exists alongside the plaintext file.
   *
   * @param text the input stream containing the template.
   * @param html the input stream containing the template.
   * @param mailID the identification for this template.
   */
  protected int addEmail(Reader text, Reader html, String mailID)
  {
    // If there's a filename, go ahead.
    if (text != null)
    {
      try (LineNumberReader lnReader = new LineNumberReader(text))
      {
        String line = lnReader.readLine();

        if (line != null)
        {
          String subject = "";
          
          boolean foundSubject = false;
          
          // Look for the subject line.
          if (line.toUpperCase().startsWith("SUBJECT: "))
          {
            foundSubject = true;
            subject = line.substring(9);

            // Skip a blank line.  There's always a blank line after the subject!
            line = lnReader.readLine();
          }
          
          if (foundSubject || !this.requireSubject)
          {
            StringBuilder messageBody = new StringBuilder(1000);

            if (line.length() == 0)
            {
              // Read next line.
              line = lnReader.readLine();
            }

            // Read the body.
            while (line != null)
            {
              messageBody.append(line);
              messageBody.append("\r\n");

              // Read next line.
              line = lnReader.readLine();
            }

            // Put the e-mail into the hashtable.
            EmailPackage newEmail = new EmailPackage(subject, messageBody.toString());
            this.templates.put(mailID, newEmail);
            
            // Read an HTML version if we have a valid Reader
            if (html != null)
            {
            	try (LineNumberReader htmlLineReader = new LineNumberReader(html))
            	{
	              messageBody = new StringBuilder(1000);
	              line = htmlLineReader.readLine();
	              while (line != null)
	              {
	                messageBody.append(line);
	                messageBody.append("\r\n");
	
	                // Read next line.
	                line = htmlLineReader.readLine();
	              }
	              
	              newEmail.setHtmlBody(messageBody.toString());
	              log.trace("Read HTML alternate for {}", mailID);
	              //log.debug("Read HTML alternate for ", mailID);
            	}
            }
          }
          else
          {
            log.warn("E-mail template file for {} has no valid subject!", mailID);
          }
        }
        else
        {
          log.warn("E-mail template file for {} has no contents!", mailID);
        }

        return 1;
      }
      catch (IOException ioexc)
      {
        log.error("Cannot read email contents for {}", mailID, ioexc);
      }
      finally
      {
        try
        {
          text.close();
          
          if (html != null)
          {
            html.close();
          }
        }
        catch (IOException ioexc)
        {
          // Do nothing if we get an IOExc while trying to close.
        }
      }
    }

    return 0;
  }

  /**
   * Adds a stock e-mail programmatically.
   *
   * @param subject The email's subject.
   * @param text The email's body text.
   * @param mailID the identification for this template.
   */
  protected void addEmail(String subject, String text, String mailID)
  {
    this.templates.put(mailID, new EmailPackage(subject, text));
  }

  /**
   * Standard Java toString.
   */
  @Override
  public String toString()
  {
    return "EmailTemplater [" + hashCode() + "]";
  }

  /**
   * Sets requireSubject.
   * 
   * @param requireSubject true if we want to require a subject to be present in the template.
   */
  public void setRequireSubject(boolean requireSubject)
  {
    this.requireSubject = requireSubject;
  }

}   // End EmailTemplater.
