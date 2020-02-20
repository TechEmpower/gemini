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
 * BlockingConsumer wrapper/helper/base-class for a JMS Consumer
 * (producer-consumer message pattern)
 */
public abstract class BlockingReceiver
    implements AutoCloseable
{
  protected final Connection        connection;
  protected       Logger            log = LoggerFactory.getLogger(getClass());
  protected Session                 session;
  protected MessageConsumer         consumer;
  protected final String            destination;

  /**
   * Constructor. This is of a type <b>AutoCloseable</b>, so this should be
   * used in a <b>try-with</b> construct.
   * <p>
   * <b>Note</b>: Do not recreate Connection objects unless you are targeting
   * a different connection/socket, since different sessions can be built from
   * the same connection object. Connection objects are resource heavy<br>
   */
  public BlockingReceiver(Connection connection, String destination)
  {
    this.connection = connection;
    this.destination = destination;
  }

  @Deprecated(forRemoval = true)
  public BlockingReceiver(GeminiApplication application,
      Connection connection, String destination)
  {
    this(connection, destination);
  }

  /**
   * Starts a BlockingConsumer and connects to the queue "<i>destination</i>"
   */
  public abstract BlockingReceiver start() throws JMSException;

  /**
   * Blocks until a jms.Message type is received
   * 
   * @throws JMSException
   * @return message of type jms.Message
   */
  public Message receive() throws JMSException
  {
    return consumer.receive();
  }

  /**
   * Blocks until a jms.Message type is received with a timeout (in
   * milliseconds)
   */
  public Message receive(long timeout) throws JMSException
  {
    return consumer.receive(timeout);
  }

  /**
   * Blocks until a message is received and returns the string value of the
   * text
   * 
   * @throws JMSException
   * @return messsage as a string or null if not of type TextMessage
   */
  public String receiveText() throws JMSException
  {
    Message message = receive();
    if (message instanceof TextMessage)
    {
      return ((TextMessage)message).getText();
    }
    return null;
  }

  /**
   * Blocks until a message is received and returns the string value of the
   * text or the wait times out
   * 
   * @throws JMSException
   * @return messsage as a string or null if not of type TextMessage
   */
  public String receiveText(long timeout) throws JMSException
  {
    Message message = receive(timeout);
    if (message instanceof TextMessage)
    {
      return ((TextMessage)message).getText();
    }
    return null;
  }

  /**
   * Blocks until a message is received and casts to Class <b>c</b>
   * 
   * @throws JMSException
   * @return converted message or null if message wasn't of type
   * jms.ObjectMessage
   */
  public <C extends Object> C receiveType(Class<C> c) throws JMSException,
      ClassCastException
  {
    Message message = receive();
    if (message instanceof ObjectMessage)
    {
      ObjectMessage m = (ObjectMessage)message;
      return c.cast(m.getObject());
    }
    return null;
  }

  /**
   * Blocks until a message is received and casts to Class <b>c</b> or the
   * request times out
   * 
   * @throws JMSException
   * @return converted message or null if message wasn't of type
   * jms.ObjectMessage
   */
  public <C extends Object> C receiveType(Class<C> c, long timeout)
      throws JMSException, ClassCastException
  {
    Message message = receive(timeout);
    if (message instanceof ObjectMessage)
    {
      ObjectMessage m = (ObjectMessage)message;
      return c.cast(m.getObject());
    }
    return null;
  }

  /**
   * Closes this session and subscriber
   */
  @Override
  public void close()
  {
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
          "BlockingReceiver <{}> exception while closing <{}> @queue://{}",
          consumer, session, destination, e);
    }
  }
}
