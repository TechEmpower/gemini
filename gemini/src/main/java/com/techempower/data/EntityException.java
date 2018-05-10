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
package com.techempower.data;

/**
 * This exception is thrown by {@link EntityGroup} when exceptions are thrown
 * internally and there is no way to recover.  It is expected that a properly
 * configured application will never throw these exceptions.  That is, either
 * the application is misconfigured and {@code EntityException} will be thrown
 * on every operation, or it's configured properly and this type of exception
 * will never occur.
 * <p>
 * Types of situations that might lead to {@code EntityException} being thrown
 * include:
 * <ul>
 * <li>The database can't be reached.</li>
 * <li>Database metadata can't be read.</li>
 * <li>The table doesn't exist or its metadata can't be read.</li>
 * <li>The ID column doesn't exist or it isn't an auto-incrementing integer.</li>
 * <li>The entity group was configured with a malformed WHERE clause.</li>
 * </ul>
 */
public class EntityException
  extends    RuntimeException
{
  private static final long serialVersionUID = -1819335132145225982L;

  /**
   * Creates an entity exception with no message or cause.
   */
  public EntityException()
  {
    super();
  }

  /**
   * Creates an entity exception with the given message and cause.
   *
   * @param message the message for the exception
   * @param cause the cause of the exception
   */
  public EntityException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Creates an entity exception with the given message and no cause.
   *
   * @param message the message for the exception
   */
  public EntityException(String message)
  {
    super(message);
  }

  /**
   * Creates an entity exception with no message and the given cause.
   *
   * @param cause the cause of the exception
   */
  public EntityException(Throwable cause)
  {
    super(cause);
  }

}
