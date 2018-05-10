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
package com.techempower.js;

import java.io.*;

/**
 * Implementations of this interface can write Java objects to JSON.  The
 * now-standard implementation uses the popular Jackson library, but a legacy
 * implementation uses a Gemini-specific writer.
 */
public interface JavaScriptWriter {

 /**
  * Writes the object in JavaScript notation.
  *
  * @param object The object to be written in JavaScript notation.
  * @return The object in JavaScript notation.
  * @throws IllegalArgumentException If traversing the object would have
  *                                  resulted in an infinite loop because of a
  *                                  circular reference.
  */
 String write(Object object);
 
 /**
  * Writes the object to the output in JavaScript notation.
  *
  * @param object The object to be written in JavaScript notation.
  * @param out The target for the object in JavaScript notation.
  * @throws IllegalArgumentException If traversing the object would have
  *                                  resulted in an infinite loop because of a
  *                                  circular reference.
  * @throws IOException If an I/O error occurs.
  */
 void write(Object object, Appendable out) throws IOException;

}
