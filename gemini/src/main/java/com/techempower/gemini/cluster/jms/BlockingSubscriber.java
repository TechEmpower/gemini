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

package com.techempower.gemini.cluster.jms;

import com.techempower.gemini.GeminiApplication;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * BlockingSubscriber wrapper/helper for a JMS Consumer (for use with
 * publisher-subscriber message pattern)
 */
public class BlockingSubscriber
    extends BlockingReceiver
{
  /**
   * Constructor. This is of a type <b>AutoCloseable</b>, so this should be
   * used in a <b>try-with</b> construct.
   * <p>
   * <b>Note</b>: Do not recreate Connection objects unless you are targeting
   * a different connection/socket, since different sessions can be built from
   * the same connection object. Connection objects are resource heavy<br>
   */
  public BlockingSubscriber(GeminiApplication application,
      Connection connection, String destination)
  {
    super(application, connection, destination);
    this.log = application.getLog("BSub");
  }

  /**
   * Starts a BlockingSubscriber and connects to the topic "<i>destination</i>"
   */
  @Override
  public BlockingReceiver start() throws JMSException
  {
    connection.start();
    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    consumer = session.createConsumer(session.createTopic(destination));

    log.log(connection + " BlockingSubscriber@'" + destination + "'");
    return this;
  }

  /**
   * Closes this session and subscriber
   */
  @Override
  public void close()
  {
    log.log("BlockingSubscriber [" + consumer + "] is closing session ["
        + session + "] for <topic://" + destination + ">");
    super.close();
  }
}
