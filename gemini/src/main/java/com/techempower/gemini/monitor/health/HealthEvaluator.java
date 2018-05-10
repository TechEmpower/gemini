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

package com.techempower.gemini.monitor.health;

import com.techempower.gemini.monitor.*;

/**
 * As part of the GeminiMonitor's health-monitoring functionality, each
 * health snapshot is provided to a list of HealthEvaluators.  Each
 * HealthEvaluator can determine if the health meets expectations or is
 * "exceptional."  Exceptional health situations may be exceptionally high
 * concurrent traffic or memory utilization.  Generally an exceptional case
 * will result in an alert being generated and processed by a secondary
 * component.
 *   <p>
 * The purpose of a HealthEvaluator is merely to determine whether the
 * current health is exceptional and -not- to act on exceptions in the form of
 * any alert or remediation.  Those actions should occur within separate
 * components.
 */
public interface HealthEvaluator
{

  /**
   * Determine if the provided HealthSnapshot is exceptional.  Aim to 
   * determine this as quickly as possible and without incurring undue
   * additional processing burden that would exacerbate an already stressed
   * environment.
   *   <p>
   * Return null to indicate that the snapshot is not exceptional and any
   * non-null String to indicate an exceptional snapshot.  The string will
   * be included as part of any alerting by separate components.
   */
  String isExceptional(HealthSnapshot snapshot, GeminiMonitor monitor);
  
  /**
   * Gets the name/description of this HealthEvaluator.  Keep it brief (10 
   * characters would be good).
   */
  String getEvaluatorName();
  
}
