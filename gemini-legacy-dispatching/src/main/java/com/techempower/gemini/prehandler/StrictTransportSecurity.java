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
package com.techempower.gemini.prehandler;

import com.techempower.gemini.*;
import com.techempower.util.*;

/**
 * Adds a Strict-Transport-Security header to all responses.
 */
public class StrictTransportSecurity
  implements Prehandler,
             Configurable
{

  private static final int DEFAULT_DURATION = 31536000;  // 1 year in seconds.
  
  private String headerText;
  private boolean enabled = true;
  
  /**
   * Constructor.
   * 
   * @param durationInSeconds The number of seconds the browser should assume
   * the site is SSL-only.
   */
  public StrictTransportSecurity(GeminiApplication app, int durationInSeconds)
  {
    setDuration(durationInSeconds);
    app.getConfigurator().addConfigurable(this);
  }
  
  /**
   * Constructor.  This version defaults the duration to be 1 year.
   */
  public StrictTransportSecurity(GeminiApplication app)
  {
    this(app, DEFAULT_DURATION);
  }
  
  /**
   * Sets the duration in seconds.
   */
  public StrictTransportSecurity setDuration(int seconds)
  {
    this.headerText = "max-age=" + seconds;
    return this;
  }
  
  @Override
  public boolean prehandle(Context context)
  {
    if (enabled)
    {
      context.headers().put("Strict-Transport-Security", headerText);
    }
    return false;
  }

  @Override
  public void configure(EnhancedProperties props) 
  {
    final EnhancedProperties.Focus focus = props.focus("StrictTransportSecurity");
    setDuration(focus.getInt("Seconds", DEFAULT_DURATION));
    enabled = focus.getBoolean("Enabled", true);
  }

}
