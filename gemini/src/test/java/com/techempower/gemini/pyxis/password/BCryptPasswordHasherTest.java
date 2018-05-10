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
package com.techempower.gemini.pyxis.password;

import static org.junit.Assert.*;

import org.junit.*;

import com.techempower.helper.*;

/**
 * Tests for BCryptPasswordHasher.
 */
public class BCryptPasswordHasherTest {

  @Test
  public void defaultWorkFactor() {
    final BCryptPasswordHasher hasher = new BCryptPasswordHasher();
    testHasher(hasher, 2000L);
  }

  @Test
  public void workFactor10() {
    final BCryptPasswordHasher hasher = new BCryptPasswordHasher(10);
    testHasher(hasher, 250L);
  }

  @Test
  public void workFactor11() {
    final BCryptPasswordHasher hasher = new BCryptPasswordHasher(11);
    testHasher(hasher, 500L);
  }

  @Test
  public void workFactor12() {
    final BCryptPasswordHasher hasher = new BCryptPasswordHasher(12);
    testHasher(hasher, 1000L);
  }

  @Test
  public void workFactor13() {
    final BCryptPasswordHasher hasher = new BCryptPasswordHasher(13);
    testHasher(hasher, 2000L);
  }
  
  private void testHasher(BCryptPasswordHasher hasher, long durationLimit)
  {
    final String password = StringHelper.secureRandomString.password(10); 
    final long start = System.currentTimeMillis(); 
    hasher.encryptPassword(password);
    final long duration = System.currentTimeMillis() - start;
    System.out.println("Work factor " + hasher.getWorkFactor() + ": " + duration + "ms");
    assertFalse(duration > durationLimit);
  }

}
