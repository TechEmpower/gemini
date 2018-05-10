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
package com.techempower.gemini.monitor.listener;

import static com.techempower.util.UtilityConstants.*;

import com.techempower.gemini.monitor.cpupercentage.*;
import com.techempower.gemini.monitor.health.*;
import com.techempower.helper.*;

/**
 * Provides some simple rendering of exceptional events into plaintext
 * Strings for use by the listeners.
 */
public final class MonitorListenerHelper
{

  /**
   * Renders a health snapshot to a plaintext String ready for sending as
   * a notification or e-mail.
   */
  public static String renderHealthSnapshot(HealthSnapshot snapshot)
  {
    final StringBuilder alert = new StringBuilder();

    // Render the various snapshot counts.
    alert.append("Processing" + CRLF)
         .append("Request count:      " + snapshot.getRequestCount() + CRLF)
         .append("Disp. concurrency:  " + snapshot.getDispatchConcurrency() + CRLF)
         .append("Disp. count:        " + snapshot.getDispatchCount() + CRLF)
         .append("Render concurrency: " + snapshot.getPageRenderConcurrency() + CRLF)
         .append("Render count:       " + snapshot.getPageRenderCount() + CRLF)
         .append("Query concurrency:  " + snapshot.getQueryConcurrency() + CRLF)
         .append("Query count:        " + snapshot.getQueryCount() + CRLF)
         .append(CRLF + "Threads" + CRLF)
         .append("Total:   " + snapshot.getTotalThreads() + CRLF)
         .append("Blocked: " + snapshot.getBlockedThreads() + CRLF)
         .append("Waiting: " + snapshot.getWaitingThreads() + CRLF)
         .append(CRLF + "Memory" + CRLF)
         .append("Total:   " + snapshot.getTotalMemory() + CRLF)
         .append("Free:    " + snapshot.getFreeMemory() + CRLF);
    
    return alert.toString();
  }
  
  /**
   * Renders a CPU Utilization exception (provided as a PercentageInterval
   * into a plaintext String ready for sending as a notification or e-mail.
   */
  public static String renderCpuUtilization(PercentageInterval interval)
  {
    final StringBuilder alert = new StringBuilder();

    alert.append(interval.getEvaluation());

    // Render all of the thread samples.
    alert.append("Other threads:" + CRLF);
    for (PercentageSample sample : interval.getSamples())
    {
      alert.append("ID")
           .append(StringHelper.padSpace(sample.getId(), 7))
           .append(" ")
           .append(StringHelper.padArbitraryRight(' ',
               StringHelper.truncateEllipsis(sample.getName(), 49), 49))
           .append(StringHelper.padSpace(sample.getMs(), 10))
           .append("ms total time ")
           .append(StringHelper.padSpace(sample.getUsage(), 3))
           .append("% current usage")
           .append(CRLF);
    }
    
    return alert.toString();
  }
  
  /**
   * No constructor.
   */
  private MonitorListenerHelper()
  {
    // Does nothing.
  }
  
}
