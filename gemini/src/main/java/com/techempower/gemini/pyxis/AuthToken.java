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

/**
 * Simple interface for abstracting the notion of an auth token for a given
 * user.
 */
public interface AuthToken
{
  /**
   * Sets the id of a user as whom this user will masquerade.
   */
  void beginMasquerade(long id);
  
  /**
   * Ends the masquerading of one user by this user.
   */
  void endMasquerade();
  
  /**
   * Returns the timestamp (in milliseconds) that this AuthToken was created.
   */
  long getIssuedAt();
  
  /**
   * Returns the id of the user to whom this AuthToken represents/belongs or
   * the user id of the user as whom this user is masquerading.
   */
  long getUserId();
  
  /**
   * Returns the timestamp (in milliseconds) of the last password change date
   * of the user to whom this AuthToken represents/belongs. This value should
   * be persisted for equality checks as a part of authorization. Changing the
   * persisted value at the application layer is expected to effectively
   * invalidate this token. 
   */
  long getUserLastPasswordChange();
  
  /**
   * Returns the validation hash of the user to whom this AuthToken 
   * represents/belongs. This value should be persisted for equality checks
   * as a part of authorization. Clearing the persisted value at the 
   * application layer is expected to effectively invalidate this token.
   */
  String getUserValidationHash();
  
  /**
   * Forcibly invalidates the token such that subsequent attempts to 
   * authenticate with the same unaltered token will fail.
   */
  void invalidate();
  
  /**
   * Returns whether the user represented by this auth token is currently
   * masquerading as another user.
   */
  boolean isMasquerading();
  
  /**
   * Returns the string tokenization of this AuthToken for the purpose of
   * delivering to a client.
   */
  String tokenize();
}
