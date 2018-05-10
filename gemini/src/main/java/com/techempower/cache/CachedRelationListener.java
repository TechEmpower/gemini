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

package com.techempower.cache;

import com.techempower.collection.relation.*;

/**
 * Listens for modifications to cached relations.
 */
public interface CachedRelationListener
{
  /**
   * Called when a pair is added to the relation.
   */
  void add(long relationID, long leftID, long rightID);
  
  /**
   * Called when a set of pairs is added to the relation.
   */
  void addAll(long relationID, LongRelation relation);
  
  /**
   * Called when the relation is cleared.
   */
  void clear(long relationID);
  
  /**
   * Called when a pair is removed from the relation.
   */
  void remove(long relationID, long leftID, long rightID);
  
  /**
   * Called when a set of pairs is removed from the relation.
   */
  void removeAll(long relationID, LongRelation relation);
  
  /**
   * Called when a left value is removed from the relation.
   */
  void removeLeftValue(long relationID, long leftID);
  
  /**
   * Called when a right value is removed from the relation.
   */
  void removeRightValue(long relationID, long rightID);
  
  /**
   * Called when the set of pairs in a relation is replaced.
   */
  void replaceAll(long relationID, LongRelation relation);
  
  /**
   * Called when the relation is reset.
   */
  void reset(long relationID);
}
