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

package com.techempower.gemini.filestore;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.email.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes inbound e-mails and (assuming a good match with the subclass-
 * provided rules) stores any attachments into the FileStore.
 *   <p>
 * Note that a subclass is required to provide the necessary rules/logic
 * to determine which Identifiable object to use as a FileStore destination.
 */
public class FileStoreEmailHandler
  implements EmailHandler
{

  //
  // Member variables.
  //
  
  private final FileStore store;
  private final Logger    log = LoggerFactory.getLogger(getClass());
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public FileStoreEmailHandler(GeminiApplication app, FileStore store)
  {
    this.store = store;
  }

  /**
   * Handle the inbound email.
   */
  @Override
  public boolean handleEmail(EmailPackage email)
  {
    Identifiable ident = getIdentifiable(email);
    if (ident != null)
    {
      try
      {
        log.info("Processing email for {}", ident);
        
        // Store the e-mail text.
        String emailText = "Subject: " + email.getSubject() 
          + UtilityConstants.CRLF 
          + UtilityConstants.CRLF 
          + email.getTextBody();
        ByteArrayInputStream bais = 
          new ByteArrayInputStream(emailText.getBytes(StandardCharsets.UTF_8));
        this.store.storeFile(bais, ident, "email-" 
          + DateHelper.STANDARD_FILENAME_FORMAT.format(new Date()) + ".txt");

        int processed = 1;
        
        // Store the file attachments.
        Collection<EmailAttachment> attach = email.getAttachments();
        if (attach != null)
        {
          Iterator<?> iter = attach.iterator();
          EmailAttachment att;
          while (iter.hasNext())
          {
            att = (EmailAttachment)iter.next();
            this.store.storeFile(att.getInputStream(), ident, att.getName());
            processed++;
          }
        }
        
        log.info("Processed {} attachment{} (including body text)",
            processed, StringHelper.pluralize(processed));
        
        // Let's delete the e-mail since we've processed it.
        return true;
      }
      catch (Exception exc)
      {
        log.error("Exception while handling email.", exc);
        // Do nothing.
      }
    }
    
    // No Identifiable object can be found to associate with this email,
    // so let's not delete it.
    return false;
  }
  
  /**
   * Gets an Identifiable based on logic provided in a subclass.  An example
   * implementation would look at the To and From attributes of the email
   * package and find a User (Identifiable) object accordingly.
   *   <p>
   * Return null if no Identifiable object can be found and the email package
   * should be ignored.
   */
  public Identifiable getIdentifiable(EmailPackage email)
  {
    return null;
  }

}
