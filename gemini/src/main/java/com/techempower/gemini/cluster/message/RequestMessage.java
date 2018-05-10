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
package com.techempower.gemini.cluster.message;


/**
 * A Message that will be sent as a "Request" from an application (the Cluster
 * Client) to the Master Server.  A Request-type message is a message that
 * expects a matching Response from the Master.  Each Request will wait for
 * a specified number of milliseconds to receive a Response before timing out.
 */
public class RequestMessage
     extends Message
{
  private static final long serialVersionUID = 1L;

  
  /**
   * Default timeout is 10 seconds.
   */
  public static final long DEFAULT_REQUEST_TIMEOUT = 10000;

  /**
   * The default request timeout is 10 seconds.
   */
  private transient long requestTimeout = DEFAULT_REQUEST_TIMEOUT;
  
  /**
   * The system time when the request message was sent out.
   */
  private transient long sentTime = 0L;

  /**
   * How many responses do we expect to receive?  When a client sends a 
   * request to the master, we expect 1 response.  When the master sends
   * a request to all nodes in a channel, we expect a response from each.
   */
  private transient int expectedResponseCount = 1;

  /**
   * Get the number of milliseconds that this request should wait for a 
   * response.
   */
  public long getRequestTimeout()
  {
    return this.requestTimeout;
  }
  
  /**
   * Sets the sent time to the current system time.
   */
  public void captureSentTime()
  {
    this.sentTime = System.currentTimeMillis();
  }
  
  /**
   * Gets the number of timeout milliseconds remaining, assuming the
   * sent-time has been captured.  Returns 0 if the sent time was not
   * captured and 0 if no time remains. 
   */
  public long getRequestTimeoutRemaining()
  {
    if (this.sentTime == 0L)
    {
      return 0L;
    }
    
    // The remaining time is the sum of the sent time and the timeout period
    // minus the current time.
    long remaining = (this.sentTime + getRequestTimeout())
        - System.currentTimeMillis();
    return (remaining > 0 ? remaining : 0);
  }

  /**
   * Set the number of milliseconds that this request should wait for a 
   * response.
   */
  public void setRequestTimeout(long requestTimeout)
  {
    this.requestTimeout = requestTimeout;
  }
  
  /**
   * Gets the expected response count.
   */
  public int getExpectedResponseCount()
  {
    return this.expectedResponseCount;
  }
  
  /**
   * Sets the expected response count.
   */
  public void setExpectedResponseCount(int expected)
  {
    this.expectedResponseCount = expected;
  }
  
}
