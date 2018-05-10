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
package com.techempower.gemini.context;

import java.util.*;
import java.util.concurrent.*;

import com.techempower.gemini.*;
import com.techempower.gemini.messaging.*;
import com.techempower.helper.*;

/**
 * Interface for dealing with a session's Messages.  A reference is fetched
 * via context.messages().
 */
public class Messages
{
  
  private static final int MAXIMUM_MESSAGE_COUNT           = 100;
  private static final String SO_MESSAGE_QUEUE             = "_message_queue";
  private static final List<Message> EMPTY_MESSAGES        =
      Collections.unmodifiableList(new ArrayList<Message>(0));

  private final SessionNamedValues session;
  
  /**
   * Constructor.
   */
  public Messages(Context context)
  {
    this.session = context.session();
  }

  /**
   * Puts a message into the session's message queue.  Note that if more
   * 100 messages are already in the message queue, additional messages
   * will be discarded.  In typical use-cases, 100 m 
   * 
   * @param message The message to add
   */
  public Messages put(Message message)
  {
    if (message != null)
    {
      List<Message> messages = session.getObject(SO_MESSAGE_QUEUE);
      if (messages == null)
      {
        messages = new CopyOnWriteArrayList<>();
        session.putObject(SO_MESSAGE_QUEUE, messages);
      }
      
      // As a failsafe, do not store a large number of messages.
      if (messages.size() < MAXIMUM_MESSAGE_COUNT)
      {
        messages.add(message);
      }      
    }
    
    return this; 
  }

  /**
   * Puts a normal message into the session's message queue.
   * 
   * @param message The message
   */
  public Messages normal(String message)
  {
    return put(message, MessageType.NORMAL);
  }

  /**
   * Puts a warning message into the session's message queue.
   * 
   * @param message The message
   */
  public Messages warning(String message)
  {
    return put(message, MessageType.WARNING);
  }

  /**
   * Puts a success message into the session's message queue.
   * 
   * @param message The message
   */
  public Messages success(String message)
  {
    return put(message, MessageType.SUCCESS);
  }

  /**
   * Puts an error message into the session's message queue.
   * 
   * @param message The message
   */
  public Messages error(String message)
  {
    return put(message, MessageType.ERROR);
  }

  /**
   * Put a message into the session's message queue.
   * 
   * @param message message text
   * @param type type of message (success, error, or normal)
   * @return if it was successfully added
   */
  public Messages put(String message, MessageType type)
  {
    if (message != null)
    {
      final Message m;
      if (type == null)
      {
        m = new Message(message);
      }
      else
      {
        m = new Message(message, type);
      }
    
      put(m);
    }
    
    return this;
  }

  /**
   * Remove all messages from the message queue.
   */
  public Messages clear()
  {
    session.remove(SO_MESSAGE_QUEUE);
    return this;
  }
  
  /**
   * Reset the queue to contain only the provided messages.
   */
  public Messages reset(List<Message> messages)
  {
    if (CollectionHelper.isNonEmpty(messages))
    {
      // Convert the list to a concurrent list.
      final List<Message> queue = new CopyOnWriteArrayList<>(messages);
      session.putObject(SO_MESSAGE_QUEUE, queue);
    }
    else
    {
      clear();
    }
    
    return this;
  }
  
  /**
   * Get the message queue as a list without removing any messages.
   * 
   * @return the queue of messages to display, unmodified.
   */
  public List<Message> queue()
  {
    final List<Message> queue = session.getObject(SO_MESSAGE_QUEUE);
    if (queue != null)
    {
      // Copy the list for returning to caller.
      return new ArrayList<>(queue);
    }
    else
    {
      return EMPTY_MESSAGES;
    }
  }

  /**
   * Get all the messages in the message queue.  All messages are removed 
   * from the queue during this process.
   * 
   * @return a List of all the messages.
   */
  public List<Message> list()
  {
    // Get the list reference before removing it from the session.
    final List<Message> queue = queue();
    clear();
    return queue;
  }
  
  /**
   * Get all the messages in the queue that are of the given type(s). Matching
   * messages are removed from the queue during this process.
   * 
   * @param type the type(s) of messages to get
   * @return all the messages of those types
   */
  public List<Message> listFiltered(MessageType... type)
  {
    final List<MessageType> types = Arrays.asList(type);
    final List<Message> toReturn = new ArrayList<>();
    final List<Message> queue = queue();
    final ListIterator<Message> iterator = queue.listIterator();
    while (iterator.hasNext())
    {
      final Message m = iterator.next();
      if (types.contains(m.getType()))
      {
        iterator.remove();
        toReturn.add(m);
      }
    }
    
    // Reset the session's queue to the remainder.
    reset(queue);
    
    return toReturn;
  }

  /**
   * Get all the error messages in the Message queue.  Messages are removed 
   * from the queue during this process.
   * 
   * @return List containing all the error messages
   */
  public List<Message> listErrors()
  {
    return listFiltered(MessageType.ERROR);
  }

  /**
   * Get all the normal messages in the queue.  Messages are removed from the
   * queue during this process.
   * 
   * @return all the normal messages
   */
  public List<Message> listNormal()
  {
    return listFiltered(MessageType.NORMAL);
  }

  /**
   * Get all the warning messages in the queue.  Messages are removed from the
   * queue during this process.
   * 
   * @return all the normal messages
   */
  public List<Message> listWarning()
  {
    return listFiltered(MessageType.WARNING);
  }
  
  /**
   * Get all the success messages in the queue.  Messages are removed from the
   * queue during this process.
   * 
   * @return all the success messages
   */
  public List<Message> listSuccess()
  {
    return listFiltered(MessageType.SUCCESS);
  }

}
