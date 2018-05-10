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

import javax.servlet.http.*;

import com.techempower.gemini.monitor.*;
import com.techempower.gemini.session.HttpSession;
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
public class ResinSessionState
  extends SessionState
  implements HttpSessionListener
{

  /**
   * Constructor.
   */
  public ResinSessionState(GeminiMonitor monitor)
  {
    super(monitor);
  }

  /**
   * The Servlet container will call this method to notify us of a newly
   * created session.  Note that sessions that exist at start-up time (which
   * may exist if the Servlet container persists sessions as Resin 4 does)
   * will not be known to the application.
   */
  @Override
  public synchronized void sessionCreated(HttpSessionEvent evt)
  {
    if (this.monitor.isSessionEnabled())
    {
      // Increment count in case session tracking is disabled.
      this.sessionCount++;
      
      if (this.sessionCount > this.peakSessions)
      {
        this.peakSessions = this.sessionCount;
      }
      
      if (this.sessionTracking)
      {
        this.sessionSet.add(HttpSession.createSession(evt.getSession()));
        this.sessionCount = this.sessionSet.size();
        
        //ComponentLog.d("Created; count: " + count + "; sessionSet size: " + sessionSet.size());
      }
    }
  }

  /**
   * The Servlet container will call this method to notify us that an existing
   * session is being destroyed.  Note that sessions that exist at start-up 
   * time (which may exist if the Servlet container persists sessions as Resin
   * 4 does) will not be known to the application.
   */
  @Override
  public synchronized void sessionDestroyed(HttpSessionEvent evt)
  {
    if (this.monitor.isSessionEnabled())
    {
      // Decrement count in case session tracking is disabled.
      this.sessionCount--;
      if (this.sessionTracking)
      {
        this.sessionSet.remove(HttpSession.createSession(evt.getSession()));
        this.sessionCount = this.sessionSet.size();
  
        //ComponentLog.d("Removed; count: " + this.sessionCount.get() + "; sessionSet size: " + sessionSet.size());
      }
      
      // Sanity check: bound session count to zero on low-end.
      if (this.sessionCount < 0)
      {
        this.sessionCount = 0;
      }
    }
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
