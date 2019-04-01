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
import com.techempower.gemini.cluster.message.Message;
import com.techempower.log.ComponentLog;

import javax.jms.*;
import java.util.Map;
import java.util.Map.Entry;

/**
 * GeminiSender abstract base class for sending Gemini's message type with
 * JMS. Extends to a Publisher or Producer.
 */
public abstract class GeminiSender
    implements AutoCloseable
{
  protected final Connection   connection;
  protected final ComponentLog log;
  protected Session            session;
  protected MessageProducer    producer;
  protected final String       destination;

  /**
   * Constructor. This is of a type <b>AutoCloseable</b>, so this should be
   * used in a <b>try-with</b> construct.
   * <p>
   * <b>Note</b>: Do not recreate Connection objects unless you are targeting
   * a different connection/socket, since different sessions can be built from
   * the same connection object. Connection objects are resource heavy
   */
  public GeminiSender(final GeminiApplication application,
      final Connection connection, final String destination)
  {
    this.connection = connection;
    this.log = application.getLog("JmsS");
    this.destination = destination;
  }

  /**
   * Sub classes need to implement this (connect non-transactionally or
   * transactionally to a topic or queue, etc)
   * 
   * @throws JMSException
   */
  public abstract GeminiSender start() throws JMSException;

  /**
   * Send a cluster.Message type to the destination; converts the
   * <code>cluter.Message <i>message</i></code> to a
   * <code>jms.ObjectMessage</code>
   * 
   * @throws JMSException
   */
  public GeminiSender send(Message message) throws JMSException
  {
    producer.send(prepMessage(message, null));
    return this;
  }

  /**
   * Send a cluster.Message type to the destination with an addition header
   * property to the message. Convenience method.
   * 
   * @throws JMSException
   */
  public GeminiSender send(Message message, String propertyKey,
      String propertyValue) throws JMSException
  {
    ObjectMessage m = prepMessage(message, null);
    m.setStringProperty(propertyKey, propertyValue);
    producer.send(m);
    return this;
  }

  /**
   * Send a cluster.Message type to the destination with a map of
   * {@code Properties<String, Object>} attached to the message
   * 
   * @throws JMSException
   */
  public GeminiSender send(Message message, Map<String, Object> properties)
      throws JMSException
  {
    producer.send(prepMessage(message, properties));
    return this;
  }

  /**
   * Send a cluster.Message type with an attached ReplyTo destination for
   * response management.
   * 
   * @throws JMSException
   */
  public GeminiSender sendWithReplyTo(Destination replyTo, Message message,
      Map<String, Object> properties) throws JMSException
  {
    ObjectMessage m = prepMessage(message, properties);
    m.setJMSReplyTo(replyTo);
    producer.send(m);
    return this;
  }

  private ObjectMessage prepMessage(Message message,
      Map<String, Object> properties) throws JMSException
  {
    ObjectMessage m = session.createObjectMessage(message);
    m.setStringProperty("Gemini-Message-Tag", message.toString());
    if (properties != null)
    {
      for (Entry<String, Object> entry : properties.entrySet())
      {
        m.setObjectProperty(entry.getKey(), entry.getValue());
      }
    }
    return m;
  }

  /**
   * Closes the producer and session. Override if extra functionality is
   * necessary (eg, logging or committing a transacted session)
   */
  @Override
  public void close()
  {
    log.log("GeminiSender is closing.");
    try
    {
      if (producer != null)
      {
        producer.close();
      }
      if (session != null)
      {
        session.close();
      }
    }
    catch (JMSException e)
    {
      log.log("GeminiSender exception while closing producer <" + producer
          + "> session <" + session + "> @topic://" + destination, e);
    }
  }
}
