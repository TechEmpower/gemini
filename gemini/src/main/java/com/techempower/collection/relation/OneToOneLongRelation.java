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
 * A one-to-one relation between longs.
 * <p>
 * By default, the relation is stored in a single map.  As a result,
 * the <tt>leftValues</tt> and <tt>containsRightValue</tt> methods
 * perform poorly.  An alternative constructor is provided to store
 * the relation in two maps, ensuring good performance of those
 * methods at the cost of memory and the performance of write methods.
 * <p>
 * This implementation is not synchronized.  If at least one thread
 * is writing to the relation while others access it, access to the
 * relation must be synchronized.
 */
public class OneToOneLongRelation extends AbstractLongRelation
{
  private static final long serialVersionUID = 1L;
  private final TLongLongMap leftMap = new TLongLongHashMap();
  private final TLongLongMap rightMap;
  private int size;

  /**
   * Constructs a new one-to-one relation stored as a single
   * map from long keys to long values.
   */
  public OneToOneLongRelation()
  {
    this(null, false);
  }

  /**
   * Constructs a new one-to-one relation stored as a single
   * map from long keys to long values.  The values in the given
   * relation are added to this one.
   *
   * @param relation values to be added to this relation
   */
  public OneToOneLongRelation(LongRelation relation)
  {
    this(relation, false);
  }

  /**
   * Constructs a new one-to-one relation.  If <tt>doublyMapped</tt>
   * is <tt>true</tt>, the relation will be stored as two maps.
   *
   * @param doublyMapped whether to store the relation in two maps
   */
  public OneToOneLongRelation(boolean doublyMapped)
  {
    this(null, doublyMapped);
  }

  /**
   * Constructs a new one-to-one relation.  The values in
   * the given relation are added to this one.  If <tt>doublyMapped</tt>
   * is <tt>true</tt>, the relation will be stored as two maps.
   *
   * @param relation values to be added to this relation
   * @param doublyMapped whether to store the relation in two maps
   */
  public OneToOneLongRelation(LongRelation relation, boolean doublyMapped)
  {
    this.rightMap = doublyMapped ? new TLongLongHashMap() : null;
    addAll(relation);
  }

  @Override
  public boolean add(long left, long right)
  {
    if (containsLeftValue(left) || containsRightValue(right))
    {
      return false;
    }
    this.size++;

    //add to left map
    this.leftMap.put(left, right);

    //add to right map
    if (this.rightMap != null)
    {
      this.rightMap.put(right, left);
    }

    return true;
  }

  @Override
  public void clear()
  {
    this.leftMap.clear();
    if (this.rightMap != null)
    {
      this.rightMap.clear();
    }
    this.size = 0;
  }

  @Override
  public Object clone()
  {
    return new OneToOneLongRelation(this, this.rightMap != null);
  }

  @Override
  public boolean contains(long left, long right)
  {
    return this.leftMap.containsKey(left) && this.leftMap.get(left) == right;
  }

  @Override
  public boolean containsLeftValue(long left)
  {
    return this.leftMap.containsKey(left);
  }

  @Override
  public boolean containsRightValue(long right)
  {
    if (this.rightMap != null)
    {
      return this.rightMap.containsKey(right);
    }
    else
    {
      return this.leftMap.containsValue(right);
    }
  }

  @Override
  public int leftSize(long right)
  {
    if (this.rightMap != null)
    {
      return this.rightMap.containsKey(right) ? 1 : 0;
    }
    else
    {
      TLongLongIterator iter = this.leftMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value() == right)
        {
          return 1;
        }
      }
      return 0;
    }
  }

  @Override
  public long[] leftValues(long right)
  {
    if (this.rightMap != null)
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
    else
    {
      TLongLongIterator iter = this.leftMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value() == right)
        {
          return new long[] { iter.key() };
        }
      }
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
    this.leftMap.remove(left);

    //remove from right map
    if (this.rightMap != null)
    {
      this.rightMap.remove(right);
    }

    return true;
  }

  @Override
  public boolean removeLeftValue(long left)
  {
    if (!containsLeftValue(left))
    {
      return false;
    }
    this.size--;

    //remove from left map
    this.leftMap.remove(left);

    //remove from right map
    if (this.rightMap != null)
    {
      TLongLongIterator iter = this.rightMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value() == left)
        {
          iter.remove();
        }
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
    TLongLongIterator iter = this.leftMap.iterator();
    while (iter.hasNext())
    {
      iter.advance();
      if (iter.value() == right)
      {
        iter.remove();
      }
    }

    //remove from right map
    if (this.rightMap != null)
    {
      this.rightMap.remove(right);
    }

    return true;
  }

  @Override
  public int rightSize(long left)
  {
    return this.leftMap.containsKey(left) ? 1 : 0;
  }

  @Override
  public long[] rightValues(long left)
  {
    if (this.leftMap.containsKey(left))
    {
      return new long[] { this.leftMap.get(left) };
    }
    else
    {
      return new long[0];
    }
  }

  @Override
  public TLongSet rightValuesLongSet(long left)
  {
    if (this.leftMap.containsKey(left))
    {
      TLongHashSet rightValuesSet = new TLongHashSet(1);
      rightValuesSet.add(this.leftMap.get(left));
      return rightValuesSet;
    }
    else
    {
      return new TLongHashSet(0);
    }
  }

  @Override
  public LongRelationIterator iterator()
  {
    return new LongRelationIterator() {

      private boolean start = false;
      private long left = 0;
      private long right = 0;
      private TLongLongIterator mapIterator = OneToOneLongRelation.this.leftMap.iterator();

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
        this.left = this.mapIterator.key();
        this.right = this.mapIterator.value();
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
