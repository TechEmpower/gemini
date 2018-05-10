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
package com.techempower.gemini.pyxis;

import com.techempower.gemini.*;

/**
 * JsonWebTokenAuthenticationArbiter provides implementations for direct user 
 * actions within the context of a PyxisSecurity.
 * <p>
 * @see <a href="https://jwt.io">Jwt</a>
 */
public class JsonWebTokenAuthenticationArbiter
     extends TokenAuthenticationArbiter
{
  //
  // PRIVATE VARIABLES
  //
  
  private final JsonWebTokenCreator creator;
  private final JsonWebTokenReader  reader;
  
  //
  // CONSTRUCTOR
  //
  
  /**
   * Pass-through constructor, however JsonWebTokenAuthenticationArbiter only
   * operates on JsonWebTokenCreator and JsonWebTokenReader instances.
   */
  public JsonWebTokenAuthenticationArbiter(GeminiApplicationInterface application, 
      JsonWebTokenCreator creator, JsonWebTokenReader reader)
  {
    super(application);
    this.creator = creator;
    this.reader = reader;
    
  }
  
  public JsonWebTokenAuthenticationArbiter(GeminiApplicationInterface application)
  {
    this(application, new JsonWebTokenCreator(application), 
        new JsonWebTokenReader(application));
  }

  //
  // PUBLIC METHODS
  //
  
  @Override
  public JsonWebTokenReader getTokenReader()
  {
    return reader;
  }
  
  @Override
  public JsonWebTokenCreator getTokenCreator()
  {
    return creator;
  }
}
