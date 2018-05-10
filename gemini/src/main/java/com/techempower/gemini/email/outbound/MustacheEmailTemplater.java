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
import java.nio.file.*;
import java.util.*;

import com.github.mustachejava.*;
import com.techempower.gemini.*;
import com.techempower.gemini.email.*;
import com.techempower.util.*;

/**
 * An implementation of EmailTemplater that uses Mustache.java as the template
 * language.  Configurable options:
 * <ul>
 * <li>Mustache.Email.TemplatePath - Path to templates used by Mustache.</li>
 * <li>Mustache.Email.PlaintextSuffix - Filename suffix for plaintext templates,
 *     defaults to .txt</li>
 * <li>Mustache.Email.HtmlSuffix - Filename suffix for HTML templates,
 *     defaults to .html</li>
 * <li>Mustache.Email.SubjectSuffix - Filename suffix for message subject
 *     templates, defaults to .subject</li>
 * </ul>
 *
 * TODO: Parameterize the MustacheManager to allow a custom configuration
 * prefix and then use another MustacheManager here, providing configurable
 * caching, cache expiration, etc.
 */
public abstract class MustacheEmailTemplater
     extends EmailTemplater
{

  //
  // Constants.
  //

  public static final String PROPS_PREFIX = "Mustache.Email.";

  //
  // Variables.
  //

  protected MustacheFactory mustacheFactory;
  protected String          templatePath;
  private String          plainSuffix = ".txt";
  private String          htmlSuffix = ".html";
  private String          subjectSuffix = ".subject";

  //
  // Methods.
  //

  /**
   * Constructor.
   */
  public MustacheEmailTemplater(GeminiApplication application)
  {
    super(application);
  }

  /**
   * Configure.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    super.configure(props);
   
    this.plainSuffix = props.get(PROPS_PREFIX + "PlaintextSuffix", this.plainSuffix);
    this.htmlSuffix = props.get(PROPS_PREFIX + "HtmlSuffix", this.htmlSuffix);
    this.subjectSuffix = props.get(PROPS_PREFIX + "SubjectSuffix", this.subjectSuffix);
  }
  
  /**
   * Renders the email template with the given filename and data and returns
   * the result as a string.  If no template with the given filename exists, the
   * {@code defaultValue} is returned instead.
   */
  protected String renderTemplateFile(String templateFilename, 
      Map<String, ? extends Object> data, String defaultValue)
  {
    if (!Files.exists(Paths.get(this.templatePath, templateFilename)))
    {
      return defaultValue;
    }
    
    final Writer writer = new StringWriter();
    final Mustache mustache = getMustacheFactory().compile(templateFilename);
    mustache.execute(writer, data);
    return writer.toString();
  }

  /**
   * Renders the email template string with the given data and returns the
   * result as a string.
   */
  protected String renderTemplateString(String templateString, 
      Map<String, ? extends Object> data)
  {
    final Writer writer = new StringWriter();
    final Reader reader = new StringReader(templateString);
    final Mustache mustache = getMustacheFactory().compile(reader, "");
    mustache.execute(writer, data);
    return writer.toString();
  }

  /**
   * Returns a mustache factory.  Uses a template cache if the application's
   * main MustacheManager is so configured; otherwise re-reads templates on
   * each use.
   */
  protected abstract MustacheFactory getMustacheFactory();

  /**
   * Gets the plaintext template filename suffix.
   */
  protected String getPlainSuffix()
  {
    return this.plainSuffix;
  }

  /**
   * Gets the HTML template filename suffix.
   */
  protected String getHtmlSuffix()
  {
    return this.htmlSuffix;
  }

  /**
   * Gets the Subject template filename suffix.
   */
  protected String getSubjectSuffix()
  {
    return this.subjectSuffix;
  }

  @Override
  public void addTemplateToLoad(String templateID)
  {
    // Does nothing.
  }
  
  /**
   * For legacy macros that begin with a dollar-sign, add another key-value
   * pair with a key stripped of the leading dollar sign mapped to the same
   * value.
   */
  private <O extends Object> void transformLegacyMacros(Map<String, O> data)
  {
    final List<String> keys = new ArrayList<>(data.keySet());
    for (String key : keys)
    {
      if (  (key.startsWith("$"))
         && (key.length() > 1)
         )
      {
        final String noDollar = key.substring(1);
        if (!data.containsKey(noDollar))
        {
          data.put(noDollar, data.get(key));
        }
      }
    }
  }

  @Override
  public EmailPackage process(String templateID,
      Map<String, ? extends Object> data, String authorAddress, String recipientAddress)
  {
    transformLegacyMacros(data);
    
    final String plainBody = renderTemplateFile(templateID + getPlainSuffix(), 
        data, null);
    final String htmlBody = renderTemplateFile(templateID + getHtmlSuffix(), 
        data, null);
    final String subject = renderTemplateFile(templateID + getSubjectSuffix(), 
        data, "No subject");

    if (htmlBody != null)
    {
      return new EmailPackage(subject, plainBody, htmlBody, recipientAddress, 
          authorAddress);
    }
    else
    {
      return new EmailPackage(subject, plainBody, recipientAddress, 
          authorAddress);
    }
  }

  @Override
  public EmailPackage process(String plainBody, String htmlBody,
      String subject, Map<String, ? extends Object> data, String authorAddress,
      String recipientAddress)
  {
    transformLegacyMacros(data);
    
    final String processedPlainBody = renderTemplateString(plainBody, data);
    final String processedSubject = renderTemplateString(subject, data);

    if (htmlBody != null)
    {
      final String processedHtmlBody = renderTemplateString(htmlBody, data);
      return new EmailPackage(processedSubject, processedPlainBody,
          processedHtmlBody, recipientAddress, authorAddress);
    }
    else
    {
      return new EmailPackage(processedSubject, processedPlainBody,
          recipientAddress, authorAddress);
    }
  }

  @Override
  public EmailPackage process(String body, String subject,
      Map<String, ? extends Object> data, String authorAddress, String recipientAddress)
  {
    return process(body, null, subject, data, authorAddress, recipientAddress);
  }

}
