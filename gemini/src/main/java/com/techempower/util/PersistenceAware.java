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
package com.techempower.util;

/**
 * Entities that are "persistence aware" know whether or not they have been
 * newly created and are not yet persisted.  Implementing this interface
 * allows an entity to expose its persistence state and also allow 
 * controllers such as the Cache to set the persistence state to true once
 * they are persisted.
 *   <p>
 * The majority of entities implement the Identifiable interface and we
 * assume a 0 identity implies "new" and non-0 implies "persisted".  But that
 * is not sufficient for all cases.  When 0 does not necessarily mean "new" 
 * -or- when non-0 doesn't necessarily mean "persisted", implement this 
 * interface.
 */
public interface PersistenceAware
{

  /**
   * Has the entity been persisted, or is considered "new" (meaning it has
   * not yet been persisted)?
   * 
   * @return true if the entity has been persisted.
   */
  boolean isPersisted();
  
  /**
   * Sets the persisted state of this entity.  An entity that is not persisted
   * is considered "new."
   * 
   * @param persisted true if the entity has been persisted; false otherwise.
   */
  void setPersisted(boolean persisted);
  
}
