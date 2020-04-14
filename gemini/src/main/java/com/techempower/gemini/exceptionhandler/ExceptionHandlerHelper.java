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

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.session.*;
import com.techempower.helper.*;

/**
 * Provides simple helper functions to the ExceptionHandlers, specifically
 * re-usable renderings of Exceptions that can be sent as Notifications 
 * (NotificationExceptionHandler) or e-mail alerts (EmailExceptionHandler).
 */
public final class ExceptionHandlerHelper
{
  
  /**
   * A list of HTTP request headers to omit from exception reports because
   * they reveal too much information for transmission via e-mail or are
   * simply too noisy and provide no value.  The list should be sorted (by 
   * traditional ASCII ordering). 
   */
  private static final String[] IGNORED_HEADERS = new String[] { 
    "Cookie", 
    "SSL_CLIENT_CERT", 
    "SSL_SERVER_CERT" 
  };

  /**
   * Render an Exception to a readable report.
   * 
   * @param context the current request context; the context may be null if
   *        the Exception was raised by an asynchronous component.
   * @param application the Gemini application; send null to not render a
   *        preamble about the application.
   * @param exception the exception that was thrown.
   * @param description any descriptive text to include with the rendering.
   * @param skipped how many other exceptions were skipped in the current
   *        reporting interval in an effort to reduce an exception storm.
   */
  public static String renderExceptionAsReport(BasicContext context,
                                               GeminiApplication application, Throwable exception, String description,
                                               int skipped)
  {
    final StringBuilder message = new StringBuilder(4000);

    // Only render the preamble if the application reference is non-null.
    if (application != null)
    {
      message.append("Gemini Exception Report\n\n");
      message.append(application.getVersion().getNameAndDeployment())
             .append('\n');
      message.append(DateHelper.STANDARD_TECH_FORMAT.format(new Date()))
             .append("\n\n");
    }
    
    // Details about the request.
    if (context != null)
    {
      message.append("Request URL: ")
             .append(context.getRequestSignature())
             .append('\n');
      message.append("Client IP:   ")
             .append(context.getClientId())
             .append('\n');
      final Session session = context.getSession(false);
      message.append("Session ID:  ")
             .append(session == null ? "no session" : session.getId())
             .append('\n');
      final PyxisSecurity security = context.getApplication().getSecurity();
      if (security != null)
      {
        final PyxisUser user = security.getUser(context);
        message.append("User:        ")
               .append(user == null ? "no user" : user)
               .append('\n');
      }
    }
    else
    {
      message.append("No associated HTTP request.\n");
    }
    if (StringHelper.isNonEmpty(description))
    {
      message.append("Description: ")
             .append(description)
             .append('\n');
    }
    message.append('\n');

    // Any exceptions skipped to cut down on alert spam?
    if (skipped > 0)
    {
      message.append(skipped)
             .append(" exception")
             .append(StringHelper.pluralize(skipped))
             .append(" skipped since last report\n\n");
    }
    
    // Show the stack trace.
    message.append("----\n");
    message.append("Stack trace\n");
    message.append(ThrowableHelper.convertStackTraceToString(exception));
    message.append("----\n");


    if(exception.getCause() != null)
    {
      message.append("Attempting to get cause\n");
      message.append(ThrowableHelper.convertStackTraceToString(exception.getCause()));
      message.append("----\n");
    }

    
    if (context != null)
    {
      message.append("Base URL: ")
             .append(context.getRequest().getRequestURL())
             .append('\n');
      message.append("Request headers\n");
      
      for (Enumeration<String> e = context.getRequest().getHeaderNames(); e.hasMoreElements(); )
      {
        String s = e.nextElement();
        
        // Omit "Cookie" and "SSL_SERVER_CERT" headers because they disclose
        // user information.
        if (Arrays.binarySearch(IGNORED_HEADERS, s) < 0)
        {
          String val = context.getRequest().getHeader(s);
          message.append("  ")
                 .append(s)
                 .append(": ")
                 .append(val)
                 .append('\n');
        }
      }
    }

    return message.toString();
  }

  /**
   * No constructor.
   */
  private ExceptionHandlerHelper()
  {
    // Does nothing.
  }
  
}
