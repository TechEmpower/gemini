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

package com.techempower.gemini.firenio.monitor;

import com.techempower.gemini.*;
import com.techempower.gemini.monitor.GeminiMonitor;
import com.techempower.gemini.monitor.session.*;

/**
 * The main class for Gemini application-monitoring functionality.
 * Applications should instantiate an instance of Monitor and then attach
 * the provided MonitorListener as a DatabaseConnectionListener and Dispatch
 * Listener.
 *   <p>
 * The Monitor has four sub-components:
 *   <ol>
 * <li>Performance monitoring, the main component, observes the execution
 *     of requests to trend the performance of each type of request over
 *     time.</li>
 * <li>Health monitoring, an optional component, observes the total amount
 *     of memory used, the number of threads, and other macro-level concerns
 *     to evaluate the health of the application.</li>
 * <li>CPU Usage Percentage monitoring, an optional component, uses JMX to
 *     observe the CPU time of Java threads and provide a rough usage
 *     percentage per thread in 1-second real-time samples.</li>
 * <li>Web session monitoring, an optional component, that counts and
 *     optionally maintains a set of active web sessions.</li>
 *   </ol>
 * Configurable options:
 *   <ul>
 * <li>Feature.monitor - Is the Gemini Monitoring component enabled as a
 *     whole?  Defaults to yes.</li>
 * <li>Feature.monitor.health - Is the Health Monitoring sub-component
 *     enabled?  Defaults to yes.</li>
 * <li>Feature.monitor.cpu - Is the CPU Usage Percentage sub-component
 *     enabled?  Defaults to yes.</li>
 * <li>Feature.monitor.session - Is the Session Monitoring sub-component
 *     enabled?</li>
 * <li>GeminiMonitor.HealthSnapshotCount - The number of health snapshots to
 *     retain in memory.  The default is 120.  Cannot be lower than 2 or
 *     greater than 30000.</li>
 * <li>GeminiMonitor.HealthSnapshotInterval - The number of milliseconds
 *     between snapshots.  The default is 300000 (5 minutes).  Cannot be set
 *     below 500ms or greater than 1 year.</li>
 * <li>GeminiMonitor.SessionSnapshotCount - The number of session snapshots to
 *     retain in memory.  The defaults are the same as Health snapshots.</li>
 * <li>GeminiMonitor.SessionSnapshotInterval - The number of milliseconds
 *     between snapshots.  Defaults same as for health.</li>
 * <li>GeminiMonitor.SessionTracking - If true, active sessions will be
 *     tracked by the session monitor to allow for listing active sessions.</li>
 *  </ul>
 *   <p>
 * Note that some of the operations executed by the health snapshot are non
 * trivial (e.g., 10-20 milliseconds).  Setting a very low snapshot interval
 * such as 500ms would mean that every 500ms, you may be consuming about
 * 25ms of CPU time to take a snapshot.  An interval of 1 minute should be
 * suitable for most applications.
 */
public class FirenioMonitor
        extends GeminiMonitor
{

    /**
     * Constructor.
     */
    public FirenioMonitor(GeminiApplication app)
    {
        super(app);
    }

    @Override
    public SessionState getSessionState()
    {
        // fixme
        return new SessionState(this) {
            @Override
            public int getSessionCount() {
                return super.getSessionCount();
            }
        };
    }

    @Override
    protected void addSessionListener()
    {
        // fixme
    }

}
