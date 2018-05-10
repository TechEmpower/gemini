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
package com.techempower.gemini.session;

import java.util.*;

/**
 * An implementation of the {@link Session} interface that is based on a servlet
 * session object.
 */
public class HttpSession
  implements Session
{
  
  //
  // Members.
  //
  
  private final javax.servlet.http.HttpSession session;

  /**
   * Constructs a new HTTP session based on the provided servlet session object.
   *
   * @param session the servlet session object
   */
  private HttpSession(javax.servlet.http.HttpSession session)
  {
    this.session = session;
  }

  /**
   * Returns a new HTTP session based on the provided servlet session object.
   *
   * @param session the servlet session object
   * @return a new HTTP session based on the provided servlet session object
   */
  public static HttpSession createSession(javax.servlet.http.HttpSession session)
  {
    if (session == null)
    {
      return null;
    }
    else
    {
      return new HttpSession(session);
    }
  }
  
  public javax.servlet.http.HttpSession getSession()
  {
    return this.session;
  }

  @Override
  public boolean isNew()
  {
    return this.session.isNew();
  }

  @Override
  public Enumeration<String> getAttributeNames()
  {
    try
    {
      return this.session.getAttributeNames();
    }
    catch (IllegalStateException ise)
    {
      return new Enumeration<String>() {
        @Override
        public boolean hasMoreElements()
        {
          return false;
        }

        @Override
        public String nextElement()
        {
          return null;
        }
      };
    }
  }

  @Override
  public Object getAttribute(String name)
  {
    try
    {
      // now we must check to see if this is a wrapped HttpSessionBindingListenerWrapper
      // if so, wrap it
      Object o = this.session.getAttribute(name);
      if(o instanceof HttpSessionBindingListenerWrapper)
      {
        o = ((HttpSessionBindingListenerWrapper)o).unwrap();
      }

      return o;
    }
    catch (IllegalStateException ise)
    {
      return null;
    }
  }

  @Override
  public int getMaxInactiveInterval()
  {
    return this.session.getMaxInactiveInterval();
  }

  @Override
  public void setAttribute(String name, Object o)
  {
    try
    {
      this.session.setAttribute(name, o);
    }
    catch (IllegalStateException ise)
    {
      // Do nothing.
    }
  }

  @Override
  public void setAttribute(String name, SessionListener o)
  {
    this.setAttribute(name, new HttpSessionBindingListenerWrapper(o));
  }

  @Override
  public void removeAttribute(String name)
  {
    try
    {
      this.session.removeAttribute(name);
    }
    catch (IllegalStateException ise)
    {
      // Do nothing.
    }
  }

  @Override
  public String getId()
  {
    return this.session.getId();
  }

  @Override
  public void invalidate()
  {
    this.session.invalidate();
  }
  
  @Override
  public void setMaxInactiveInterval(int timeout)
  {
    try
    {
      this.session.setMaxInactiveInterval(timeout);
    }
    catch (IllegalStateException ise)
    {
      // Do nothing.
    }
  }

  @Override
  public boolean equals(Object obj)
  {
    if(obj instanceof HttpSession)
    {
      HttpSession object = (HttpSession)obj;
      return this.session.equals(object.getSession());
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    // Pass-through to the underlying session.
    return this.session.hashCode();
  }
  
}
