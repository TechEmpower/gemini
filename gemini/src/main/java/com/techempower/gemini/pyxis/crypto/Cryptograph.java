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
package com.techempower.gemini.pyxis.crypto;

import java.security.*;

import com.techempower.util.*;

/**
 * Provides the interface for configurable Cryptograph objects
 * using various cipher suites and implementations.
 */
public interface Cryptograph
  extends Configurable
{
  public static final String PROPS_PREFIX = "Security.Cryptograph.";
  
  /**
   * Attempts to encrypt the given input bytes.
   * @param input data
   * @return The encrypted bytes or null if encryption failed
   */
  public byte[] encrypt(byte[] input);
  
  /**
   * Attempts to decrypt the given ciphertext.
   * @param ciphertext
   * @return The decrypted bytes or null if decryption failed
   */
  public byte[] decrypt(byte[] ciphertext);
  
  /**
   * Attempts to generate a securely randomized Key for any Cryptograph
   * implementation.
   * @return The generated key
   */
  public Key generateKey();
  
  /**
   * Attempts to set the underlying key for this Cryptograph.
   * @param key
   */
  public void setKey(Key key);
}
