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

package com.techempower.log;

/**
 * A simple object that packages up everything that is used by the logging
 * tools: a String to log, a date + time stamp, a log level, etc.
 */
public class LogItem
{
  
  //
  // Constants
  //
  
  public static final int TYPE_LOG       = 1;
  public static final int TYPE_ASSERTION = 2;

  //
  // Member variables.
  //
  
  private final String  logString;
  private final String  componentCode;
  private final long    timeStamp;
  private final int     type;
  private final int     level;

  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public LogItem(String componentCode, String logString, int type, int level)
  {
    this.componentCode = componentCode;
    this.logString     = logString;
    this.timeStamp     = System.currentTimeMillis();
    this.level         = level;
    this.type          = type;
  }

  /**
   * @return the logString
   */
  public String getLogString()
  {
    return this.logString;
  }

  /**
   * @return the componentCode
   */
  public String getComponentCode()
  {
    return this.componentCode;
  }

  /**
   * @return the timeStamp
   */
  public long getTimeStamp()
  {
    return this.timeStamp;
  }

  /**
   * @return the type
   */
  public int getType()
  {
    return this.type;
  }

  /**
   * @return the level
   */
  public int getLevel()
  {
    return this.level;
  }
  
}  // End LogItem.
