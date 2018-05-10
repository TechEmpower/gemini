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

import java.util.*;

import org.mindrot.jbcrypt.*;

/**
 * The default implementation of PasswordHasher that uses BCrypt as the
 * hashing algorithm.  This is considered secure and suitable for use in a
 * production or test environment.
 *   <p>
 * The implementation is backwards compatible with the PlaintextPasswordHasher,
 * meaning that plaintext passwords will be used until the next time a user's
 * password is reset, at which point the password will be stored in a hashed
 * form.  To avoid this gradual conversion of plaintext passwords, 
 * applications should use BcryptPasswordHasher from inception.
 */
public class BCryptPasswordHasher 
  implements PasswordHasher
{
  
  public static final int DEFAULT_WORK_FACTOR = 12;
  
  /**
   * The BCrypt work factor.
   */
  private final int workFactor;
  
  /**
   * Constructor.
   */
  public BCryptPasswordHasher(int workFactor)
  {
    this.workFactor = workFactor;
  }
  
  /**
   * Constructor.  Sets the work factor (rounds) to 12.
   */
  public BCryptPasswordHasher()
  {
    this(DEFAULT_WORK_FACTOR);
  }
  
  /**
   * Gets the work factor.
   */
  public int getWorkFactor()
  {
    return workFactor;
  }

  @Override
  public String encryptPassword(String cleartextPassword)
  {
    // If you have a casual understanding of password hashing, as most of us
    // do, you might wonder why the generated salt is provided to the hash
    // algorithm and then discarded.  The reason is that the generated salt
    // is embedded in the String returned by the BCrypt algorithm.  See the 
    // answers to the following StackOverflow question for more detail.
    // http://stackoverflow.com/questions/6832445/how-can-bcrypt-have-built-in-salts
    
    return BCrypt.hashpw(cleartextPassword, BCrypt.gensalt(workFactor));
  }

  @Override
  public boolean testPassword(String cleartextPassword,
    String encryptedPassword)
  {
    // Use BCrypt to test any password hashes that begin with the BCrypt-
    // specific prefix, $2a$
    if (  (encryptedPassword != null)
       && (encryptedPassword.startsWith(getIdentifyingPrefix()))
       )
    {
      return (BCrypt.checkpw(cleartextPassword, encryptedPassword));
    }
    // Default to simple comparison if the encrypted password is not yet
    // stored in a properly encrypted fashion.
    else
    {
      return Objects.equals(cleartextPassword, encryptedPassword);
    }
  }

  @Override
  public String getName()
  {
    return "BCrypt";
  }
  
  @Override
  public boolean isSecure()
  {
    return true;
  }

  @Override
  public String getIdentifyingPrefix()
  {
    return "$2a$";
  }

}
