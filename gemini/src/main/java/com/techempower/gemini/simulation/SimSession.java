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
package com.techempower.gemini.simulation;

import java.util.*;

import com.techempower.gemini.session.*;

/**
 * An implementation of the {@link Session} interface that represents a
 * simulated session
 */
public class SimSession
  implements Session
{
  
  //
  // Member variables.
  //
  
  private String                    id;
  private Hashtable<String, Object> attributes          = new Hashtable<>();
  private boolean                   isNew               = true;
  private int                       maxInactiveInterval = 3000;
  //private boolean                   valid               = true;
  
  @Override
  public boolean isNew()
  {
    return this.isNew;
  }

  @Override
  public Enumeration<String> getAttributeNames()
  {
    return this.attributes.keys();
  }

  @Override
  public Object getAttribute(String name)
  {
    return this.attributes.get(name);
  }

  @Override
  public int getMaxInactiveInterval()
  {
    return this.maxInactiveInterval;
  }

  @Override
  public void setAttribute(String name, Object o)
  {
    if(o != null)
    {
      this.attributes.put(name, o);
    }
  }

  @Override
  public void setAttribute(String name, SessionListener o)
  {
    if(o != null)
    {
      this.attributes.put(name, o);
    }
  }

  @Override
  public void removeAttribute(String name)
  {
    this.attributes.remove(name);
  }

  @Override
  public String getId()
  {
    return this.id;
  }

  @Override
  public void invalidate()
  {
    //this.valid = false;
  }

  @Override
  public void setMaxInactiveInterval(int timeout)
  {
    this.maxInactiveInterval = timeout;
  }

}
