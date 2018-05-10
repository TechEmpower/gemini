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

import com.techempower.util.*;

/**
 * Any object can be notified of events that affect the cache by implementing
 * this interface and "subscribing" to cache events with the cache controller
 * (EntityStore).  For now, the only events that are sent to CacheListeners
 * are pertaining to the expiration of objects or object types.
 *    <p>
 * Note that implementations of this interface should try to return as
 * quickly as possible from these methods because the methods will be
 * called within the thread that is causing these events.
 */
public interface CacheListener
{

  /**
   * The entire cache has been reset (flushed).  Note that during a full 
   * reset, each individual type is reset first, resulting in several calls
   * to cacheTypeReset, and then cacheFullReset is called.
   */  
  void cacheFullReset();
  
  /**
   * A single object type's cache has been reset.
   */
  <T extends Identifiable> void cacheTypeReset(Class<T> type);

  /**
   * A single object has been expired.
   */
  <T extends Identifiable> void cacheObjectExpired(Class<T> type, long identifier);

  /**
   * A single object has been removed.
   */
  <T extends Identifiable> void removeFromCache(Class<T> type, long identifier);

}   // End CacheListener.
