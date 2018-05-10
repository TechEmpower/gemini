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

package com.techempower.gemini.cluster.message;

/**
 * Indicates that a cache should be reset.
 */
public class CacheMessage
    extends BroadcastMessage
{
  private static final long serialVersionUID     = 1L;

  public static final int   ACTION_OBJECT_RESET  = 0;
  public static final int   ACTION_OBJECT_REMOVE = 1;
  public static final int   ACTION_GROUP_RESET   = 2;
  public static final int   ACTION_FULL_RESET    = 3;

  private int               action;
  private int               groupId;
  private long              objectId;

  public int getAction()
  {
    return this.action;
  }

  public void setAction(int action)
  {
    this.action = action;
  }

  public int getGroupId()
  {
    return this.groupId;
  }

  public void setGroupId(int groupId)
  {
    this.groupId = groupId;
  }

  public long getObjectId()
  {
    return this.objectId;
  }

  public void setObjectId(long objectId)
  {
    this.objectId = objectId;
  }

  /**
   * toString.
   */
  @Override
  public String toString()
  {
    return "CacheMessage [" + hashCode() 
        + "; " + getMessageId()
        + "; a" + getAction()
        + "; g" + getGroupId()
        + "; o" + getObjectId()
        + "; " + (getObjectProperties() != null ? getObjectProperties().size() : 0) + " properties"
        + "]";
  }

}
