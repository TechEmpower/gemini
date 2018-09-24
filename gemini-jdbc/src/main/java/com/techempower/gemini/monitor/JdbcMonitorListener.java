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

package com.techempower.gemini.monitor;

import java.sql.*;

import com.techempower.data.*;
import com.techempower.data.jdbc.*;
import com.techempower.util.*;

/**
 * Listens to database and Dispatcher events for the Gemini application-
 * monitoring functionality. For database monitoring, this class sends "do
 * nothing" signals in response to exception notifications since this is a
 * passive listener. Should be used with a DatabaseConnectionListenerList so
 * that an existing listener can be used as well.
 * <p>
 * To be clear: the MonitorListener is a component used by the GeminiMonitor to
 * listen to events occurring within the web application. This is <b>not</b> an
 * interface used to listen to the GeminiMonitor itself. For that need, see
 * GeminiMonitorListener.
 *
 * @see GeminiMonitorListener
 */
public class JdbcMonitorListener extends MonitorListener implements DatabaseConnectionListener
{

  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "MonL";

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public JdbcMonitorListener(GeminiMonitor monitor)
  {
    super(monitor);
  }

  //
  // DatabaseConnectionListener interface.
  //

  @Override
  public int exceptionInExecuteBatch(SQLException exc, JdbcConnector conn)
  {
    if (monitor.isEnabled())
    {
      MonitorSample.get().queryException();
    }
    return INSTRUCT_DO_NOTHING;
  }

  @Override
  public int exceptionInRunQuery(SQLException exc, JdbcConnector conn)
  {
    if (monitor.isEnabled())
    {
      MonitorSample.get().queryException();
    }
    return INSTRUCT_DO_NOTHING;
  }

  @Override
  public int exceptionInRunUpdateQuery(SQLException exc, JdbcConnector conn)
  {
    if (monitor.isEnabled())
    {
      MonitorSample.get().queryException();
    }
    return INSTRUCT_DO_NOTHING;
  }

  @Override
  public void queryStarting()
  {
    if (monitor.isEnabled())
    {
      // Notify the performance monitor sample.
      MonitorSample.get().queryStarting();

      // Notify the health monitor.
      monitor.queryStarting();
    }
  }

  @Override
  public void queryCompleting()
  {
    if (monitor.isEnabled())
    {
      // Notify the performance monitor sample.
      MonitorSample.get().queryCompleting();

      // Notify the health monitor.
      monitor.queryCompleting();
    }
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    // Does nothing.
  }

}
