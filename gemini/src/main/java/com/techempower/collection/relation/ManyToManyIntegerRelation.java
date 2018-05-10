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
 * A many-to-many relation between integers.
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
public class ManyToManyIntegerRelation extends AbstractIntegerRelation
{
  private static final long serialVersionUID = 1L;
  private final TIntObjectMap<TIntSet> leftMap = new TIntObjectHashMap<>();
  private final TIntObjectMap<TIntSet> rightMap;
  private int size;
  
  /**
   * Constructs a new many-to-many relation stored as a single 
   * map from int keys to sets of int values.
   */
  public ManyToManyIntegerRelation()
  {
    this(null, false);
  }
  
  /**
   * Constructs a new many-to-many relation stored as a single 
   * map from int keys to sets of int values.  The values in 
   * the given relation are added to this one.
   * 
   * @param relation values to be added to this relation
   */
  public ManyToManyIntegerRelation(IntegerRelation relation)
  {
    this(relation, false);
  }
  
  /**
   * Constructs a new many-to-many relation.  If <tt>doublyMapped</tt> 
   * is <tt>true</tt>, the relation will be stored as two maps.  
   * 
   * @param doublyMapped whether to store the relation in two maps
   */
  public ManyToManyIntegerRelation(boolean doublyMapped)
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
  public ManyToManyIntegerRelation(IntegerRelation relation, boolean doublyMapped)
  {
    this.rightMap = doublyMapped ? new TIntObjectHashMap<>() : null;
    addAll(relation);
  }

  @Override
  public boolean add(int left, int right)
  {
    if (contains(left, right))
    {
      return false;
    }
    this.size++;
    
    //add to left map
    TIntSet rightSet = this.leftMap.get(left);
    if (rightSet == null)
    {
      rightSet = new TIntHashSet();
      this.leftMap.put(left, rightSet);
    }
    rightSet.add(right);
    
    //add to right map
    if (this.rightMap != null)
    {
      TIntSet leftSet = this.rightMap.get(right);
      if (leftSet == null)
      {
        leftSet = new TIntHashSet();
        this.rightMap.put(right, leftSet);
      }
      leftSet.add(left);
    }
    
    return true;
  }

  /**
   * A helper that calls add(left, right) for each Integer in the given
   * Collection.
   */
  public boolean add(int left, Collection<Integer> rightIds)
  {
    boolean modified = false;
    for (Integer right : rightIds)
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
    return new ManyToManyIntegerRelation(this, this.rightMap != null);
  }

  @Override
  public boolean contains(int left, int right)
  {
    return this.leftMap.containsKey(left) && (this.leftMap.get(left)).contains(right);
  }

  @Override
  public boolean containsLeftValue(int left)
  {
    return this.leftMap.containsKey(left);
  }

  @Override
  public boolean containsRightValue(int right)
  {
    if (this.rightMap != null)
    {
      return this.rightMap.containsKey(right);
    }
    else
    {
      TIntObjectIterator<TIntSet> iter = this.leftMap.iterator();
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
  public int leftSize(int right)
  {
    if (this.rightMap != null)
    {
      TIntSet leftSet = this.rightMap.get(right);
      return leftSet == null ? 0 : leftSet.size();
    }
    else
    {
      int computedSize = 0;
      TIntObjectIterator<TIntSet> iter = this.leftMap.iterator();
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
  public int[] leftValues(int right)
  {
    if (this.rightMap != null)
    {
      TIntSet leftSet = this.rightMap.get(right);
      if (leftSet != null)
      {
        int[] leftArray = new int[leftSet.size()];
        leftSet.toArray(leftArray);
        return leftArray;
      }
      else
      {
        return new int[0];
      }
    }
    else
    {
      TIntObjectIterator<TIntSet> iter = this.leftMap.iterator();
      TIntSet leftSet = new TIntHashSet();
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
  public boolean remove(int left, int right)
  {
    if (!contains(left, right))
    {
      return false;
    }
    this.size--;
    
    //remove from left map
    TIntSet rightSet = this.leftMap.get(left);
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
      TIntSet leftSet = this.rightMap.get(right);
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
  public boolean removeLeftValue(int left)
  {
    if (!containsLeftValue(left))
    {
      return false;
    }
    
    TIntSet rightSet = this.leftMap.get(left);
    if (rightSet != null)
    {
      this.size -= rightSet.size();
    }
    
    //remove from left map
    this.leftMap.remove(left);
    
    //remove from right map
    if (this.rightMap != null)
    {
      for (TIntObjectIterator<TIntSet> iter = this.rightMap.iterator(); iter.hasNext();)
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
  public boolean removeRightValue(int right)
  {
    if (!containsRightValue(right))
    {
      return false;
    }
    
    //remove from left map
    for (TIntObjectIterator<TIntSet> iter = this.leftMap.iterator(); iter.hasNext();)
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
  public int rightSize(int left, Collection<Integer> filterRightIds)
  {
    TIntSet rightSet = this.leftMap.get(left);
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
      for (TIntIterator iter = rightSet.iterator(); iter.hasNext(); )
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
  public int rightSize(int left, TIntSet filterRightIds)
  {
    TIntSet rightSet = this.leftMap.get(left);
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
      for (TIntIterator iter = rightSet.iterator(); iter.hasNext(); )
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
  public int[] rightValues(int left)
  {
    TIntSet rightSet = this.leftMap.get(left);
    if (rightSet != null)
    {
      int[] rightArray = new int[rightSet.size()];
      rightSet.toArray(rightArray);
      return rightArray;
    }
    else
    {
      return new int[0];
    }
  }

  @Override
  public TIntSet rightValuesIntegerSet(int left)
  {
    TIntSet rightSet = this.leftMap.get(left);
    if (rightSet != null)
    {
      return new TIntHashSet(rightSet); // Don't expose our internal TIntSet.
    }
    else
    {
      return new TIntHashSet(0);
    }
  }

  @Override
  public IntegerRelationIterator iterator()
  {
    return new IntegerRelationIterator() {

      private int left = 0;
      private int right = 0;
      private TIntObjectIterator<TIntSet> mapIterator = ManyToManyIntegerRelation.this.leftMap.iterator();
      private TIntIterator setIterator = null;

      @Override
      public boolean hasNext()
      {
        return this.setIterator != null && this.setIterator.hasNext() || this.mapIterator.hasNext();
      }

      @Override
      public int left()
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
          TIntSet currentSet = this.mapIterator.value();
          this.setIterator = currentSet.iterator();
          this.left = this.mapIterator.key();
          this.right = this.setIterator.next();
        }
      }

      @Override
      public int right()
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
