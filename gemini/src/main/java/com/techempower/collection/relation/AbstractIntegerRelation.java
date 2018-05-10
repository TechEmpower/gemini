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

package com.techempower.collection.relation;

import gnu.trove.set.*;

import java.util.*;

/**
 * An abstract implementation of IntegerRelation.  This class 
 * implements the <tt>addAll</tt>, <tt>removeAll</tt>, 
 * <tt>containsAll</tt> and <tt>toString</tt> methods.
 */
public abstract class AbstractIntegerRelation implements IntegerRelation
{
  private static final long serialVersionUID = 1L;

  @Override
  public boolean addAll(IntegerRelation relation)
  {
    if (relation == null)
    {
      return false;
    }
    boolean modified = false;
    IntegerRelationIterator iter = relation.iterator();
    while (iter.hasNext())
    {
      iter.next();
      modified = add(iter.left(), iter.right()) || modified;
    }
    return modified;
  }

  @Override
  public abstract Object clone();

  @Override
  public boolean containsAll(IntegerRelation relation)
  {
    if (relation == null)
    {
      return true;
    }
    IntegerRelationIterator iter = relation.iterator();
    while (iter.hasNext())
    {
      iter.next();
      if (!contains(iter.left(), iter.right()))
      {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean removeAll(IntegerRelation relation)
  {
    if (relation == null)
    {
      return false;
    }
    boolean modified = false;
    IntegerRelationIterator iter = relation.iterator();
    while (iter.hasNext())
    {
      iter.next();
      modified = remove(iter.left(), iter.right()) || modified;
    }
    return modified;
  }
  
  /**
   * Returns a string representation of this relation in the form 
   * of <tt>{(1,2),(1,3),(2,7)}</tt>. 
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    IntegerRelationIterator iter = iterator();
    while (iter.hasNext())
    {
      iter.next();
      sb.append("(");
      sb.append(iter.left());
      sb.append(",");
      sb.append(iter.right());
      sb.append(")");
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  public int rightSize(int right)
  {
    return rightSize(right, (Collection<Integer>)null);
  }

  @Override
  public int rightSize(int right, Collection<Integer> filterRightIds)
  {
    // Only implemented in ManyToManyIntegerRelation so far. All the others
    // will inherit this, just in case someone tries to use it.
    throw new UnsupportedOperationException(
        "The method rightSize has not been implemented yet.");
  }

  @Override
  public int rightSize(int right, TIntSet filterRightIds)
  {
    // Only implemented in ManyToManyIntegerRelation so far. All the others
    // will inherit this, just in case someone tries to use it.
    throw new UnsupportedOperationException(
        "The method rightSize has not been implemented yet.");
  }
}
