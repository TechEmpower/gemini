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
package com.techempower.gemini.feature;

/**
 * A "Feature" is an application component that can be enabled or disabled
 * at configuration-time or run-time.  A Feature is responsible for 
 * populating at least a single root-level node into the Gemini Feature 
 * Manager representing the feature as a whole.  The Feature then may also
 * populate zero or more child nodes representing sub features.  For
 * example, the GeminiMonitor is a feature and it specifies the following
 * nodes:
 *   <p>
 *   monitor - A root-level node configurable as Feature.monitor
 *   monitor.health - A sub-feature for health monitoring, configurable as
 *     Feature.monitor.health
 *   monitor.cpu - A sub-feature for CPU percentage monitoring, configurable
 *     as Feature.monitor.cpu
 *   <p>
 * When you construct a Feature, inform the FeatureManager.  For example,
 * GeminiMonitor implements Feature and then does the following in its
 * constructor:
 *   <code>
 *   public GeminiMonitor(GeminiApplication application, ...)
 *   {
 *     FeatureManager manager = application.getFeatureManager();
 *     manager.add("monitor", "Gemini Monitor")
 *            .add("health", "Gemini Monitor's health monitoring")
 *            .add("cpu", "Gemini Monitor's CPU utilization monitoring");
 *   }
 *   </code>
 * Presently, the interface does not yet specify any required methods; it is
 * a placeholder for possible future feature management.
 */
public interface Feature
{
  
}
