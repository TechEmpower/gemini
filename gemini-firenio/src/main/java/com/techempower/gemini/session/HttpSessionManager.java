/*******************************************************************************
 * Copyright (c) 2020, TechEmpower, Inc.
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
package com.techempower.gemini.session;

import com.techempower.gemini.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the creation of user session objects.  Initializes new sessions
 * to the proper timeout, etc.
 *   <p>
 * The Context class uses SessionManager to create sessions.  This allows
 * for any necessary initialization to happen on all new sessions.
 *   <p>
 * Reads the following configuration options from the .conf file:
 *    <ul>
 * <li>SessionTimeout - Timeout for sessions in seconds.  Default: 3600.
 * <li>StrictSessions - Attempts to prevent session hijacking by hashing
 *     request headers provided at the start of each session with those
 *     received on each subsequent request; resetting the session in the event
 *     of a mismatch.
 * <li>RefererTracking - Captures the HTTP "referer" (sic) request header
 *     provided when a session is new.
 *    </ul>
 */
public class HttpSessionManager
        implements SessionManager
{
    //
    // Constants.
    //

    public static final int    DEFAULT_TIMEOUT    = 3600;      // One hour
    public static final String SESSION_HASH       = "Gemini-Session-Hash";

    //
    // Member variables.
    //

    private int     timeoutSeconds     = DEFAULT_TIMEOUT;
    private Logger  log = LoggerFactory.getLogger(getClass());
    private boolean refererTracking    = false;
    private long              sessionAccumulator = 0L;
    private boolean           strictSessions     = false;

    //
    // Member methods.
    //

    /**
     * Constructor.
     */
    public HttpSessionManager(GeminiApplication application)
    {
        application.getConfigurator().addConfigurable(this);
    }

    /**
     * Configure this component.
     */
    @Override
    public void configure(EnhancedProperties props)
    {
        setTimeoutSeconds(props.getInt("SessionTimeout", DEFAULT_TIMEOUT));
        log.info("Session timeout: {} seconds.", getTimeoutSeconds());

        refererTracking  = props.getBoolean("RefererTracking", refererTracking);
        if (refererTracking)
        {
            log.info("Referer tracking enabled.");
        }
        strictSessions   = props.getBoolean("StrictSessions", strictSessions);
        if (strictSessions)
        {
            log.info("Scrict sessions enabled.");
        }
    }

    /**
     * Sets the session timeout in minutes.  Note: only future sessions will be
     * affected.
     */
    public void setTimeoutMinutes(int minutes)
    {
        timeoutSeconds = minutes * 60;
    }

    /**
     * Sets the session timeout in seconds.  Note: only future sessions will be
     * affected.
     */
    public void setTimeoutSeconds(int seconds)
    {
        timeoutSeconds = seconds;
    }

    /**
     * Gets the session timeout in seconds.
     */
    @Override
    public int getTimeoutSeconds()
    {
        return timeoutSeconds;
    }

    @Override
    public Session getSession(Request request, boolean create)
    {
        // fixme
        return null;
    }
}
