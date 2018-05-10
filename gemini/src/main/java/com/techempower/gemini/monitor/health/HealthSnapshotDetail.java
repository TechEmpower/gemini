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

import java.lang.management.*;

/**
 * When a HealthSnapshot is determined to be exceptional, additional detail
 * will be gathered and stored in a HealthSnapshotDetail data structure.
 *   <p>
 * This simple data structure is managed by HealthSnapshot.
 */
public class HealthSnapshotDetail
{

  //
  // Variables.
  //
  
  private String       evaluation = null;
  private ThreadInfo[] threadInfo = null; 
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public HealthSnapshotDetail()
  {
  }

  /**
   * @return the evaluation
   */
  public String getEvaluation()
  {
    return this.evaluation;
  }

  /**
   * @param evaluation the evaluation to set
   */
  public void setEvaluation(String evaluation)
  {
    this.evaluation = evaluation;
  }

  /**
   * @return the threadInfo
   */
  public ThreadInfo[] getThreadInfo()
  {
    return this.threadInfo.clone();
  }

  /**
   * @param threadInfo the threadInfo to set
   */
  public void setThreadInfo(ThreadInfo[] threadInfo)
  {
    this.threadInfo = threadInfo.clone();
  }
  
}
