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
package com.techempower.gemini.simulation;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.session.*;
import com.techempower.util.*;

/**
 * An implementation of the {@link SessionManager} interface for simulated
 * sessions.
 */
public class SimSessionManager
  implements SessionManager
{
  //
  // Constants.
  //

  public static final String COMPONENT_CODE     = "mSim";    // Four-letter component ID

  //
  // Member variables.
  //

  private Map<String, SimSession> sessions;

  /**
   * Constructor.
   */
  public SimSessionManager(GeminiApplication application)
  {
    // Create a synchronized set for the sessions.
    this.sessions = new HashMap<>();
    application.getConfigurator().addConfigurable(this);
  }

  @Override
  public void configure(EnhancedProperties props)
  {
  }

  @Override
  public Session getSession(Request request, boolean create)
  {
    return this.getSession(create, ((SimRequest)request));
  }
  
  /**
   * Returns the session of the given request.
   *
   * @param create if {@code true} and the session does not exist, it will be
   *               created
   * @param request the request
   * @return the session of the given request
   */
  private SimSession getSession(boolean create, SimRequest request)
  {
    if(create)
    {
      this.sessions.put(request.getClientId(), new SimSession());
    }
    else if(!this.sessions.containsKey(request.getClientId()))
    {
      this.sessions.put(request.getClientId(), new SimSession());
    }
    
    return this.sessions.get(request.getClientId());
  }

  @Override
  public int getTimeoutSeconds()
  {
    return 0;
  }
}
