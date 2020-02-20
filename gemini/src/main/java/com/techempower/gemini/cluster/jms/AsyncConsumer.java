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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * AsyncConsumer is a thin wrapper for a JMS Consumer with an attached
 * MessageListener (publisher-subscriber message pattern)
 */
public class AsyncConsumer
{
  private Connection      connection;
  private Logger          log = LoggerFactory.getLogger(getClass());
  private Session         session;
  private MessageConsumer consumer;
  private String          destination;

  /**
   * Constructor. This is of a type <b>AutoCloseable</b>, so this should be
   * used in a <b>try-with</b> construct.
   * <p>
   * <b>Note</b>: Do not recreate Connection objects unless you are targeting
   * a different connection/socket, since different sessions can be built from
   * the same connection object. Connection objects are resource heavy
   */
  public AsyncConsumer(final Connection connection, final String destination)
  {
    this.connection = connection;
    this.destination = destination;
  }

  @Deprecated(forRemoval = true)
  public AsyncConsumer(final GeminiApplication application,
      final Connection connection, final String destination)
  {
    this(connection, destination);
  }

  /**
   * Starts the consumer and connects to the queue "<i>destination</i>",
   * attaching a listener to the session
   */
  public AsyncConsumer start(MessageListener listener) throws JMSException
  {
    connection.start();
    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    consumer = session.createConsumer(session.createQueue(destination));
    consumer.setMessageListener(listener);

    log.info("{} AsyncConsumer@'{}'", connection, destination);
    return this;
  }

  /**
   * Closes this session and consumer
   */
  public void close()
  {
    log.info("AsyncConsumer <{}> is closing session <{}> @queue://{}",
        consumer, session, destination);
    try
    {
      if (consumer != null)
      {
        consumer.close();
      }
      if (session != null)
      {
        session.close();
      }
    }
    catch (JMSException e)
    {
      log.error(
          "AsyncConsumer <{}> exception while closing session <{}> @queue://{}",
          consumer, session, destination, e);
    }
  }
}
