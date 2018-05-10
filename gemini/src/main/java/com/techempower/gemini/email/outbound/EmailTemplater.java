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

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.email.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * An EmailTemplater is responsible for management and execution/processing
 * of e-mail templates.  The SimpleEmailTemplater is a legacy templater that
 * features an extremely simple macro-expansion template syntax.  Other
 * EmailTemplaters can be created with more sophisticated templating
 * languages. 
 *   <p>
 * For the purposes of EmailTemplater and its implementation subclasses, the
 * following terms apply:
 *   <ul>
 * <li>Email template - A template that can be populated with specifics at
 *     send-time.  The template should be inclusive of any/all formats
 *     (commonly text and HTML) and the message subject.  The manner by which
 *     those elements are specified is up to the implementation.</li>
 * <li>Template ID - a String identifier for the template.  Should not include
 *     space and should be suitable for use as part of a filename.</li>
 *   </ul>
 */
public abstract class EmailTemplater
  implements Configurable
{

  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "emtm";

  //
  // Member variables.
  //

  private final GeminiApplication application;
  private final ComponentLog      log;

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public EmailTemplater(GeminiApplication application)
  {
    this.application = application;
    this.log         = application.getLog(COMPONENT_CODE);
    
    // Get configured.
    application.getConfigurator().addConfigurable(this);
  }

  /**
   * Configure this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // Does nothing here.
  }

  /**
   * Adds a template to load/pre-parse, typically meaning the contents of an
   * external template file are loaded and processed in some fashion so that
   * the template is ready for use later.  This is an optional method and
   * may be ignored by an implementation if the notion of pre-parsing does
   * not apply.
   *   <p>
   * The manner in which the templateID is used may vary by implementation,
   * however the implementation should accept any valid file-system style
   * name.  By convention, however, the templateID does not include a file
   * extension.
   *   <p>
   * E.g., templateID = E-ForgotPassword
   *   <p>
   * BasicEmailTemplater will check the configuration file to determine which
   * file on disk this maps to.  But typically the configuration file maps
   * the template ID to a file with the same name + an extension.  In this
   * case, /web-inf/emails/E-ForgotPassword.txt
   */
  public abstract void addTemplateToLoad(String templateID);
  
  /**
   * Produce an EmailPackage from the specified template (specified by ID) and
   * map of names to values.
   * 
   * @param templateID a template's identifier.
   * @param data the map of names to values.
   * @param authorAddress an e-mail address to use as the e-mail's author.
   * @param recipientAddress the recipient's e-mail address.
   * 
   * @return A new EmailPackage ready to send, or null if template was 
   *         not found.
   */
  public abstract EmailPackage process(
      String templateID, 
      Map<String, ? extends Object> data,
      String authorAddress,
      String recipientAddress
      );
  
  /**
   * Produces an EmailPackage using literal templates provided as String
   * parameters.
   *
   * @param plainBody a literal template for the plaintext body of the e-mail.
   * @param htmlBody a literal template for the HTML body of the e-mail.
   * @param subject a literal template for the subject of the e-mail.
   * @param data the map of names to values.
   * @param authorAddress an e-mail address to use as the e-mail's author.
   * @param recipientAddress the recipient's e-mail address.
   *
   * @return A new EmailPackage ready to send.
   */
  public abstract EmailPackage process(
      String plainBody, 
      String htmlBody, 
      String subject, 
      Map<String, ? extends Object> data, 
      String authorAddress, 
      String recipientAddress);
  
  /**
   * Produces an EmailPackage using literal templates provided as String
   * parameters.
   *
   * @param body a literal template for the body of the e-mail.
   * @param subject a literal template for the subject of the e-mail.
   * @param data the map of names to values.
   * @param authorAddress an e-mail address to use as the e-mail's author.
   * @param recipientAddress the recipient's e-mail address.
   *
   * @return A new EmailPackage ready to send.
   */
  public abstract EmailPackage process(
      String body, 
      String subject, 
      Map<String, ? extends Object> data, 
      String authorAddress, 
      String recipientAddress);
  
  /**
   * Gets the application reference.
   */
  protected GeminiApplication getApplication()
  {
    return this.application;
  }
  
  /**
   * Gets the ComponentLog reference.
   */
  protected ComponentLog getLog()
  {
    return this.log;
  }
  
  //
  // Utility methods.
  //
  
  /**
   * Demotes a map of <String,String> to <String,Object>.
   */
  protected Map<String,Object> demoteStringMap(Map<String,String> data)
  {
    Map<String, Object> objData = new HashMap<>(data.size());
    objData.putAll(data);
    return objData;
  }

}
