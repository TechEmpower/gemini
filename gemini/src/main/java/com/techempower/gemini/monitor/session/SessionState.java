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
package com.techempower.gemini.monitor.session;

import java.util.*;

import com.techempower.gemini.monitor.*;
import com.techempower.gemini.session.Session;
import com.techempower.util.*;

/**
 * If session monitoring is enabled in the GeminiMonitor, a single instance of
 * this class is maintained to represent the most-current session statistics
 * (specifically, the number of current sessions).
 *   <p>
 * The contents of this object are then copied to SessionSnapshot objects at
 * the completion of each sampling interval in order to produce a historic
 * trend.
 *   <p>
 * At application start time, it is possible for sessions to exist that this
 * class will not know about because those sessions were persisted by the
 * Servlet container when a previously-running instance of the application
 * shut down.  In that event, the session counting may be inaccurate unless
 * session tracking is also enabled.  If session tracking is enabled, the
 * count will eventually become accurate as pre-existing sessions expire.
 *   <p>
 * Configuration options:
 *   <ul>
 * <li>GeminiMonitor.SessionTracking - Yes/no, should session sbe tracked,
 *     by which we mean should a Set of active sessions be maintained so
 *     that administrators may view a list of sessions.</li>
 *   </ul>
 */
public abstract class SessionState
  implements Configurable
{
  
  //
  // Member variables.
  //
  
  protected final GeminiMonitor monitor;
  protected     int           sessionCount;
  protected     boolean       sessionTracking = false;
  protected final Set<Session>  sessionSet;
  protected     int           peakSessions = 0;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public SessionState(GeminiMonitor monitor)
  {
    this.monitor = monitor;
    this.monitor.getApplication().getConfigurator().addConfigurable(this);
    this.sessionSet = new HashSet<>();
    this.sessionCount = 0;
  }
  
  /**
   * Gets the session count.
   */
  public int getSessionCount()
  {
    return this.sessionCount;
  }
  
  /**
   * Gets a Set of the current sessions.
   */
  public synchronized Set<Session> getSessions()
  {
    if (  (this.sessionTracking)
       && (this.sessionSet != null)
       )
    {
      return new HashSet<>(this.sessionSet);
    }
    else
    {
      // Session tracking is not enabled.
      return null;
    }
  }
  
  /**
   * Gets the peak session count.
   */
  public int getPeakSessions()
  {
    return this.peakSessions;
  }

  /**
   * Configure this component.
   */
  @Override
  public synchronized void configure(EnhancedProperties props)
  {
    this.sessionTracking = props.getBoolean(
        "GeminiMonitor.SessionTracking", false);
    
    if (!this.sessionTracking)
    {
      this.sessionSet.clear();
    }
  }

}
