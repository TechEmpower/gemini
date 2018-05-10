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
package com.techempower.gemini.input.validator;

import com.techempower.gemini.input.*;

/**
 * A Short-Circuit Validator will stop validation processing before its own
 * processing/validation if the Input has failed by any previously-executed 
 * Validator.  No additional methods are defined by this interface; it is
 * simply a marker used by CompositeValidator.process.
 */
public interface ShortCircuitValidator
  extends Validator
{
 
  /**
   * A no-operation implementation of ShortCircuitValidator, useful for 
   * inserting a short-circuit in a validation list.
   */
  public static class NoOp
    implements ShortCircuitValidator
  {
    @Override
    public void process(Input input) 
    {
      // Does nothing.
    }
  }
  
  /**
   * A wrapper implementation of ShortCircuitValidator that creates a 
   * pass-through to any existing validator. 
   */
  public static class Wrapper
    implements ShortCircuitValidator
  {
    private final Validator wrapped;
    public Wrapper(Validator toWrap)
    {
      this.wrapped = toWrap;
    }
    @Override
    public void process(Input input) 
    {
      wrapped.process(input);
    }
  }
}
