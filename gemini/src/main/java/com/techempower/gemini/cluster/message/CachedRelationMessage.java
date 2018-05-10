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

import com.techempower.collection.relation.*;

/**
 * Contains a message about a change to be made to a cached relation.
 */
public class CachedRelationMessage
     extends BroadcastMessage
{
  private static final long serialVersionUID          = 1L;

  public static final int   ACTION_ADD                = 0;
  public static final int   ACTION_ADD_ALL            = 1;
  public static final int   ACTION_CLEAR              = 2;
  public static final int   ACTION_REMOVE             = 3;
  public static final int   ACTION_REMOVE_ALL         = 4;
  public static final int   ACTION_REMOVE_LEFT_VALUE  = 5;
  public static final int   ACTION_REMOVE_RIGHT_VALUE = 6;
  public static final int   ACTION_REPLACE_ALL        = 7;
  public static final int   ACTION_RESET              = 8;

  private int               action;
  private long              relationId;
  private long              leftId;
  private long              rightId;
  private LongRelation      relation;

  public int getAction()
  {
    return this.action;
  }

  public void setAction(int action)
  {
    this.action = action;
  }

  public long getRelationId()
  {
    return this.relationId;
  }

  public void setRelationId(long relationId)
  {
    this.relationId = relationId;
  }

  public long getLeftId()
  {
    return this.leftId;
  }

  public void setLeftId(long leftId)
  {
    this.leftId = leftId;
  }

  public long getRightId()
  {
    return this.rightId;
  }

  public void setRightId(long rightId)
  {
    this.rightId = rightId;
  }

  public LongRelation getRelation()
  {
    return this.relation;
  }

  public void setRelation(LongRelation relation)
  {
    this.relation = relation;
  }

  /**
   * toString.
   */
  @Override
  public String toString()
  {
    return "CachedRelationMessage [" + hashCode()
        + "; " + getMessageId()
        + "; a" + getAction()
        + "; rel" + getRelationId()
        + "; l" + getLeftId()
        + "; r" + getRightId()
        + "; " + (getObjectProperties() != null ? getObjectProperties().size() : 0) + " properties"
        + "]";
  }

}
