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

import com.techempower.helper.*;

/**
 * GeminiMonitor component representing a sample of web session statistics.
 * This is conceptually similar to a MonitorSample.
 *   <p>
 * If the session-monitoring sub-component of the GeminiMonitor is enabled,
 * the Monitor will maintain an array of historic SessionSample objects spaced
 * by an interval of time.  This gives a big-picture view of the web session 
 * trend.
 *   <p>
 * Each SessionSnapshot is constructed by pulling the current session state
 * information from the singleton SessionState instance.
 */
public class SessionSnapshot
{

  //
  // Member variables.
  //
  
  private final long sampleTime;
  private final int  sessionCount;
  
  /**
   * Constructor.
   * 
   * @param state The current session state from which to copy information.
   */
  public SessionSnapshot(SessionState state)
  {
    this.sampleTime = System.currentTimeMillis();
    this.sessionCount = state.getSessionCount();
  }
  
  /**
   * Gets the sample time for this snapshot.
   */
  public long getSampleTime()
  {
    return this.sampleTime;
  }
  
  /**
   * Gets the count of sessions for this snapshot.
   */
  public int getSessionCount()
  {
    return this.sessionCount;
  }
  
  /**
   * Simple toString.
   */
  @Override
  public String toString()
  {
    return "SessionSnapshot" 
       + " [" + DateHelper.STANDARD_TECH_FORMAT.format(new Date(this.sampleTime))
       + "; " + getSessionCount() + " sessions"
       + "]";
  }
  
}
