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

import com.techempower.helper.*;

/**
 * A many-to-many relation between longs.
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
public class ManyToManyLongRelation extends AbstractLongRelation
{
  private static final long serialVersionUID = 1L;
  private final TLongObjectMap<TLongSet> leftMap = new TLongObjectHashMap<>();
  private final TLongObjectMap<TLongSet> rightMap;
  private int size;

  /**
   * Constructs a new many-to-many relation stored as a single
   * map from long keys to sets of long values.
   */
  public ManyToManyLongRelation()
  {
    this(null, false);
  }

  /**
   * Constructs a new many-to-many relation stored as a single
   * map from long keys to sets of long values.  The values in
   * the given relation are added to this one.
   *
   * @param relation values to be added to this relation
   */
  public ManyToManyLongRelation(LongRelation relation)
  {
    this(relation, false);
  }

  /**
   * Constructs a new many-to-many relation.  If <tt>doublyMapped</tt>
   * is <tt>true</tt>, the relation will be stored as two maps.
   *
   * @param doublyMapped whether to store the relation in two maps
   */
  public ManyToManyLongRelation(boolean doublyMapped)
  {
    this(null, doublyMapped);
  }

  /**
   * Constructs a new many-to-many relation.  The values in the given
   * relation are added to this one.  If <tt>doublyMapped</tt>
   * is <tt>true</tt>, the relation will be stored as two maps.
   *
   * @param relation values to be added to this relation
   * @param doublyMapped whether to store the relation in two maps
   */
  public ManyToManyLongRelation(LongRelation relation, boolean doublyMapped)
  {
    this.rightMap = doublyMapped ? new TLongObjectHashMap<>() : null;
    addAll(relation);
  }

  @Override
  public boolean add(long left, long right)
  {
    if (contains(left, right))
    {
      return false;
    }
    this.size++;

    //add to left map
    TLongSet rightSet = this.leftMap.get(left);
    if (rightSet == null)
    {
      rightSet = new TLongHashSet();
      this.leftMap.put(left, rightSet);
    }
    rightSet.add(right);

    //add to right map
    if (this.rightMap != null)
    {
      TLongSet leftSet = this.rightMap.get(right);
      if (leftSet == null)
      {
        leftSet = new TLongHashSet();
        this.rightMap.put(right, leftSet);
      }
      leftSet.add(left);
    }

    return true;
  }

  /**
   * A helper that calls add(left, right) for each Long in the given
   * Collection.
   */
  public boolean add(long left, Collection<Long> rightIds)
  {
    boolean modified = false;
    for (Long right : rightIds)
    {
      if (add(left, right))
      {
        modified = true;
        this.size++;
      }
    }
    return modified;
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
    return new ManyToManyLongRelation(this, this.rightMap != null);
  }

  @Override
  public boolean contains(long left, long right)
  {
    return this.leftMap.containsKey(left) && (this.leftMap.get(left)).contains(right);
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
      TLongObjectIterator<TLongSet> iter = this.leftMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value().contains(right))
        {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public int leftSize(long right)
  {
    if (this.rightMap != null)
    {
      TLongSet leftSet = this.rightMap.get(right);
      return leftSet == null ? 0 : leftSet.size();
    }
    else
    {
      int computedSize = 0;
      TLongObjectIterator<TLongSet> iter = this.leftMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value().contains(right))
        {
          computedSize++;
        }
      }
      return computedSize;
    }
  }

  @Override
  public long[] leftValues(long right)
  {
    if (this.rightMap != null)
    {
      TLongSet leftSet = this.rightMap.get(right);
      if (leftSet != null)
      {
        long[] leftArray = new long[leftSet.size()];
        leftSet.toArray(leftArray);
        return leftArray;
      }
      else
      {
        return new long[0];
      }
    }
    else
    {
      TLongObjectIterator<TLongSet> iter = this.leftMap.iterator();
      TLongSet leftSet = new TLongHashSet();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value().contains(right))
        {
          leftSet.add(iter.key());
        }
      }
      return leftSet.toArray();
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
    TLongSet rightSet = this.leftMap.get(left);
    if (rightSet != null)
    {
      rightSet.remove(right);
      if (rightSet.isEmpty())
      {
        this.leftMap.remove(left);
      }
    }

    //remove from right map
    if (this.rightMap != null)
    {
      TLongSet leftSet = this.rightMap.get(right);
      if (leftSet != null)
      {
        leftSet.remove(left);
        if (leftSet.isEmpty())
        {
          this.rightMap.remove(right);
        }
      }
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

    TLongSet rightSet = this.leftMap.get(left);
    if (rightSet != null)
    {
      this.size -= rightSet.size();
    }

    //remove from left map
    this.leftMap.remove(left);

    //remove from right map
    if (this.rightMap != null)
    {
      for (TLongObjectIterator<TLongSet> iter = this.rightMap.iterator(); iter.hasNext();)
      {
        iter.advance();
        iter.value().remove(left);
        if (iter.value().isEmpty())
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

    //remove from left map
    for (TLongObjectIterator<TLongSet> iter = this.leftMap.iterator(); iter.hasNext();)
    {
      iter.advance();
      boolean found = iter.value().remove(right);
      if (found)
      {
        this.size--;
        if (iter.value().isEmpty())
        {
          iter.remove();
        }
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
  public int rightSize(long left, Collection<Long> filterRightIds)
  {
    TLongSet rightSet = this.leftMap.get(left);
    if (rightSet == null)
    {
      return 0;
    }
    else if (CollectionHelper.isEmpty(filterRightIds))
    {
      return rightSet.size();
    }
    else
    {
      int count = 0;
      for (TLongIterator iter = rightSet.iterator(); iter.hasNext(); )
      {
        if (filterRightIds.contains(iter.next()))
        {
          count++;
        }
      }
      return count;
    }
  }

  @Override
  public int rightSize(long left, TLongSet filterRightIds)
  {
    TLongSet rightSet = this.leftMap.get(left);
    if (rightSet == null)
    {
      return 0;
    }
    else if (filterRightIds == null || filterRightIds.isEmpty())
    {
      return rightSet.size();
    }
    else
    {
      int count = 0;
      for (TLongIterator iter = rightSet.iterator(); iter.hasNext(); )
      {
        if (filterRightIds.contains(iter.next()))
        {
          count++;
        }
      }
      return count;
    }
  }

  @Override
  public long[] rightValues(long left)
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

  @Override
  public TLongSet rightValuesLongSet(long left)
  {
    TLongSet rightSet = this.leftMap.get(left);
    if (rightSet != null)
    {
      return new TLongHashSet(rightSet); // Don't expose our internal TLongSet.
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

      private long left = 0;
      private long right = 0;
      private TLongObjectIterator<TLongSet> mapIterator = ManyToManyLongRelation.this.leftMap.iterator();
      private TLongIterator setIterator = null;

      @Override
      public boolean hasNext()
      {
        return this.setIterator != null && this.setIterator.hasNext() || this.mapIterator.hasNext();
      }

      @Override
      public long left()
      {
        if (this.setIterator == null)
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

        if (this.setIterator != null && this.setIterator.hasNext())
        {
          this.right = this.setIterator.next();
        }
        else
        {
          this.mapIterator.advance();
          TLongSet currentSet = this.mapIterator.value();
          this.setIterator = currentSet.iterator();
          this.left = this.mapIterator.key();
          this.right = this.setIterator.next();
        }
      }

      @Override
      public long right()
      {
        if (this.setIterator == null)
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
