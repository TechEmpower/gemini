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

import gnu.trove.iterator.*;
import gnu.trove.map.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.*;
import gnu.trove.set.hash.*;

import java.util.*;

/**
 * A one-to-many relation between longs.
 * <p>
 * By default, the relation is stored in a single map.  As a result,
 * the <tt>rightValues</tt> and <tt>containsLeftValue</tt> methods
 * perform poorly.  An alternative constructor is provided to store
 * the relation in two maps, ensuring good performance of those
 * methods at the cost of memory and the performance of write methods.
 * <p>
 * This implementation is not synchronized.  If at least one thread
 * is writing to the relation while others access it, access to the
 * relation must be synchronized.
 */
public class OneToManyLongRelation extends AbstractLongRelation
{
  private static final long serialVersionUID = 1L;
  private final TLongObjectMap<TLongSet> leftMap;
  private final TLongLongMap rightMap = new TLongLongHashMap();
  private int size;

  /**
   * Constructs a new one-to-many relation stored as a single
   * map from long keys to long values.
   */
  public OneToManyLongRelation()
  {
    this(null, false);
  }

  /**
   * Constructs a new one-to-many relation stored as a single
   * map from long keys to long values.  The values in the given
   * relation are added to this one.
   *
   * @param relation values to be added to this relation
   */
  public OneToManyLongRelation(LongRelation relation)
  {
    this(relation, false);
  }

  /**
   * Constructs a new one-to-many relation.  If <tt>doublyMapped</tt>
   * is <tt>true</tt>, the relation will be stored as two maps.
   *
   * @param doublyMapped whether to store the relation in two maps
   */
  public OneToManyLongRelation(boolean doublyMapped)
  {
    this(null, doublyMapped);
  }

  /**
   * Constructs a new one-to-many relation.  The values in
   * the given relation are added to this one.  If <tt>doublyMapped</tt>
   * is <tt>true</tt>, the relation will be stored as two maps.
   *
   * @param relation values to be added to this relation
   * @param doublyMapped whether to store the relation in two maps
   */
  public OneToManyLongRelation(LongRelation relation, boolean doublyMapped)
  {
    this.leftMap = doublyMapped ? new TLongObjectHashMap<>() : null;
    addAll(relation);
  }

  @Override
  public boolean add(long left, long right)
  {
    if (containsRightValue(right))
    {
      return false;
    }
    this.size++;

    //add to left map
    if (this.leftMap != null)
    {
      TLongSet rightSet = this.leftMap.get(left);
      if (rightSet == null)
      {
        rightSet = new TLongHashSet();
        this.leftMap.put(left, rightSet);
      }
      rightSet.add(right);
    }

    //add to right map
    this.rightMap.put(right, left);

    return true;
  }

  @Override
  public void clear()
  {
    if (this.leftMap != null)
    {
      this.leftMap.clear();
    }
    this.rightMap.clear();
    this.size = 0;
  }

  @Override
  public Object clone()
  {
    return new OneToManyLongRelation(this, this.leftMap != null);
  }

  @Override
  public boolean contains(long left, long right)
  {
    return this.rightMap.containsKey(right) && this.rightMap.get(right) == left;
  }

  @Override
  public boolean containsLeftValue(long left)
  {
    if (this.leftMap != null)
    {
      return this.leftMap.containsKey(left);
    }
    else
    {
      return this.rightMap.containsValue(left);
    }
  }

  @Override
  public boolean containsRightValue(long right)
  {
    return this.rightMap.containsKey(right);
  }

  @Override
  public int leftSize(long right)
  {
    return this.rightMap.containsKey(right) ? 1 : 0;
  }

  @Override
  public long[] leftValues(long right)
  {
    if (this.rightMap.containsKey(right))
    {
      return new long[] { this.rightMap.get(right) };
    }
    else
    {
      return new long[0];
    }
  }

  @Override
  public boolean remove(long left, long right)
  {
    if (!contains(left, right))
    {
      return false;
    }
    this.size--;

    //remove from left map
    if (this.leftMap != null)
    {
      TLongSet rightSet = this.leftMap.get(left);
      if (rightSet != null)
      {
        rightSet.remove(right);
        if (rightSet.isEmpty())
        {
          this.leftMap.remove(left);
        }
      }
    }

    //remove from right map
    this.rightMap.remove(right);

    return true;
  }

  @Override
  public boolean removeLeftValue(long left)
  {
    if (!containsLeftValue(left))
    {
      return false;
    }

    //remove from left map
    if (this.leftMap != null)
    {
      this.leftMap.remove(left);
    }

    //remove from right map
    TLongLongIterator iter = this.rightMap.iterator();
    while (iter.hasNext())
    {
      iter.advance();
      if (iter.value() == left)
      {
        iter.remove();
        this.size--;
      }
    }

    return true;
  }

  @Override
  public boolean removeRightValue(long right)
  {
    if (!containsRightValue(right))
    {
      return false;
    }
    this.size--;

    //remove from left map
    if (this.leftMap != null)
    {
      for (TLongObjectIterator<TLongSet> iter = this.leftMap.iterator(); iter.hasNext();)
      {
        iter.advance();
        iter.value().remove(right);
        if (iter.value().isEmpty())
        {
          iter.remove();
        }
      }
    }

    //remove from right map
    this.rightMap.remove(right);

    return true;
  }

  @Override
  public int rightSize(long left)
  {
    if (this.leftMap != null)
    {
      TLongSet rightSet = this.leftMap.get(left);
      return rightSet == null ? 0 : rightSet.size();
    }
    else
    {
      int computedSize = 0;
      TLongLongIterator iter = this.rightMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value() == left)
        {
          computedSize++;
        }
      }
      return computedSize;
    }
  }

  @Override
  public long[] rightValues(long left)
  {
    if (this.leftMap != null)
    {
      TLongSet rightSet = this.leftMap.get(left);
      if (rightSet != null)
      {
        long[] rightArray = new long[rightSet.size()];
        rightSet.toArray(rightArray);
        return rightArray;
      }
      else
      {
        return new long[0];
      }
    }
    else
    {
      TLongSet rightSet = new TLongHashSet();
      TLongLongIterator iter = this.rightMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value() == left)
        {
          rightSet.add(iter.key());
        }
      }
      return rightSet.toArray();
    }
  }

  @Override
  public TLongSet rightValuesLongSet(long left)
  {
    if (this.leftMap != null)
    {
      TLongSet rightSet = this.leftMap.get(left);
      if (rightSet != null)
      {
        return new TLongHashSet(rightSet);
      }
      else
      {
        return new TLongHashSet(0);
      }
    }
    else
    {
      TLongSet rightSet = new TLongHashSet();
      TLongLongIterator iter = this.rightMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value() == left)
        {
          rightSet.add(iter.key());
        }
      }
      return new TLongHashSet(rightSet);
    }
  }

  @Override
  public LongRelationIterator iterator()
  {
    return new LongRelationIterator() {

      private boolean start = false;
      private long left = 0;
      private long right = 0;
      private TLongLongIterator mapIterator = OneToManyLongRelation.this.rightMap.iterator();

      @Override
      public boolean hasNext()
      {
        return this.mapIterator.hasNext();
      }

      @Override
      public long left()
      {
        if (!this.start)
        {
          throw new IllegalStateException("Attempt to get element from iterator that has no current element. Call next() first.");
        }

        return this.left;
      }

      @Override
      public void next()
      {
        if (!hasNext())
        {
          throw new NoSuchElementException("Attempt to iterate past iterator's last element.");
        }

        this.mapIterator.advance();
        this.left = this.mapIterator.value();
        this.right = this.mapIterator.key();
        this.start = true;
      }

      @Override
      public long right()
      {
        if (!this.start)
        {
          throw new IllegalStateException("Attempt to get element from iterator that has no current element. Call next() first.");
        }

        return this.right;
      }

    };
  }

  @Override
  public int size()
  {
    return this.size;
  }
}
