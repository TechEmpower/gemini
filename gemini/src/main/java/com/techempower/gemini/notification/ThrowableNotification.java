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
package com.techempower.gemini.notification;

import com.techempower.gemini.*;
import com.techempower.gemini.exceptionhandler.*;

/**
 * A notification suitable for exceptions or errors.
 */
public class ThrowableNotification
     extends BasicNotification {

  /**
   * Constructor.  Assumes MEDIUM severity.
   * 
   * @param source The source of the notification.
   * @param app The Application.
   * @param throwable The exception or error.
   */
  public ThrowableNotification(String source, GeminiApplication app, 
      Throwable throwable) {
    this(source, app, throwable, null, Severity.MEDIUM, null);
  }
  
  /**
   * Constructor.  Assumes MEDIUM severity.
   * 
   * @param source The source of the notification.
   * @param app The Application.
   * @param throwable The exception or error.
   * @param description Optional description of the situation at hand.
   */
  public ThrowableNotification(String source, GeminiApplication app, 
      Throwable throwable, String description) {
    this(source, app, throwable, description, Severity.MEDIUM, null);
  }
  
  /**
   * Constructor.
   * 
   * @param source The source of the notification.
   * @param app The Application.
   * @param throwable The exception or error.
   * @param description Optional description of the situation at hand.
   * @param severity The severity of the situation.
   */
  public ThrowableNotification(String source, GeminiApplication app,
      Throwable throwable,  String description, Severity severity) {
    this(source, app, throwable, description, severity, null);
  }
  
  /**
   * Constructor.
   * 
   * @param source The source of the notification.
   * @param app The Application.
   * @param throwable The exception or error.
   * @param description Optional description of the situation at hand.
   * @param severity The severity of the situation.
   * @param context An optional request context.
   */
  public ThrowableNotification(String source, GeminiApplication app, 
      Throwable throwable, String description, Severity severity, 
      BasicContext context) {
    super(source,
        throwable.getClass().getName(),
        ExceptionHandlerHelper.renderExceptionAsReport(
            context, 
            app, 
            throwable, 
            description, 
            0)
        );
  }
  
}
