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
package com.techempower.cache;

/**
 * The EntityStore (formerly "CacheController" or simply "Controller") may 
 * throw this Error when used improperly such as by calling 
 * EntityStore.put(object) with a parameter whose class is not registered with
 * the Controller.  The Controller expects to be used properly: get, put, etc.
 * should be called with valid parameters.
 *   <p>
 * In the example above, rather than throw the underlying NullPointerException
 * (the Map lookup for the controlled EntityGroup will be null), the 
 * Controller throws a ControllerError to provide details useful in debugging 
 * an application.  However, applications should never rely on these errors
 * during routine use; the problems identified by these errors should be 
 * discovered during development testing.
 */
public class ControllerError
     extends Error
{
  private static final long serialVersionUID = 7009173082095634668L;

  public ControllerError()
  {
    super();
  }
  
  public ControllerError(String message, Throwable cause)
  {
    super(message, cause);
  }
  
  public ControllerError(String message)
  {
    super(message);
  }
  
  public ControllerError(Throwable cause)
  {
    super(cause);
  }

}
