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

import com.techempower.data.annotation.*;
import com.techempower.util.*;

/**
 * A single user's password history, captured as a simple String concatenation
 * of hashes.  In the list of hashes, the first is the most recent and the
 * last is the oldest.  New entries are added to the front of the String.
 */
@Entity
public class   PasswordHistory
    implements Identifiable,
               PersistenceAware
{
  
  private long    userId;        // the user ID.
  private boolean persisted;
  private String  hashes = "";

  @Override
  public long getId()
  {
    return userId;
  }

  @Override
  public void setId(long identity)
  {
    this.userId = identity;
  }

  @Override
  public boolean isPersisted()
  {
    return persisted;
  }

  @Override
  public void setPersisted(boolean persisted)
  {
    this.persisted = persisted;
  }
  
  /**
   * Get a space-delimited set of historical password hashes for the user.
   */
  public String getHashes()
  {
    return hashes;
  }
  
  /**
   * Set the space-delimited set of historical password hashes for the user.
   */
  public PasswordHistory setHashes(String hashes)
  {
    this.hashes = hashes;
    return this;
  }
  
  /**
   * Gets the hashes as an array.
   */
  public String[] getHashesArray()
  {
    return hashes.split(" ");
  }
  
  /**
   * Add a new hash to the front of the hashes string.  If the number of 
   * hashes in the String 
   */
  public PasswordHistory addHash(String hash, int maximum)
  {
    final String[] hashesArray = getHashesArray();
    final StringList result = new StringList(" ");
    
    // Add the new hash to the beginning of the result.
    result.add(hash);
    
    // Add the old hashes in sequence until we meet the maximum count.
    int position = 0;
    int remaining = Math.min(maximum - 1, hashesArray.length);
    while (remaining-- > 0)
    {
      result.add(hashesArray[position++]);
    }
    
    // Concatenate into a String.
    this.hashes = result.toString();
    
    return this;
  }

}
