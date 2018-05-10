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
 * A one-to-many relation between integers.  
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
public class OneToManyIntegerRelation extends AbstractIntegerRelation
{
  private static final long serialVersionUID = 1L;
  private final TIntObjectMap<TIntSet> leftMap;
  private final TIntIntMap rightMap = new TIntIntHashMap();
  private int size;

  /**
   * Constructs a new one-to-many relation stored as a single 
   * map from int keys to int values.
   */
  public OneToManyIntegerRelation()
  {
    this(null, false);
  }

  /**
   * Constructs a new one-to-many relation stored as a single 
   * map from int keys to int values.  The values in the given 
   * relation are added to this one.
   * 
   * @param relation values to be added to this relation
   */
  public OneToManyIntegerRelation(IntegerRelation relation)
  {
    this(relation, false);
  }

  /**
   * Constructs a new one-to-many relation.  If <tt>doublyMapped</tt> 
   * is <tt>true</tt>, the relation will be stored as two maps.  
   * 
   * @param doublyMapped whether to store the relation in two maps
   */
  public OneToManyIntegerRelation(boolean doublyMapped)
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
  public OneToManyIntegerRelation(IntegerRelation relation, boolean doublyMapped)
  {
    this.leftMap = doublyMapped ? new TIntObjectHashMap<>() : null;
    addAll(relation);
  }

  @Override
  public boolean add(int left, int right)
  {
    if (containsRightValue(right))
    {
      return false;
    }
    this.size++;
    
    //add to left map
    if (this.leftMap != null)
    {
      TIntSet rightSet = this.leftMap.get(left);
      if (rightSet == null)
      {
        rightSet = new TIntHashSet();
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
    return new OneToManyIntegerRelation(this, this.leftMap != null);
  }

  @Override
  public boolean contains(int left, int right)
  {
    return this.rightMap.containsKey(right) && this.rightMap.get(right) == left;
  }

  @Override
  public boolean containsLeftValue(int left)
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
  public boolean containsRightValue(int right)
  {
    return this.rightMap.containsKey(right);
  }

  @Override
  public int leftSize(int right)
  {
    return this.rightMap.containsKey(right) ? 1 : 0;
  }

  @Override
  public int[] leftValues(int right)
  {
    if (this.rightMap.containsKey(right))
    {
      return new int[] { this.rightMap.get(right) };
    }
    else
    {
      return new int[0];
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
    if (this.leftMap != null)
    {
      TIntSet rightSet = this.leftMap.get(left);
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
  public boolean removeLeftValue(int left)
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
    TIntIntIterator iter = this.rightMap.iterator();
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
  public boolean removeRightValue(int right)
  {
    if (!containsRightValue(right))
    {
      return false;
    }
    this.size--;
    
    //remove from left map
    if (this.leftMap != null)
    {
      for (TIntObjectIterator<TIntSet> iter = this.leftMap.iterator(); iter.hasNext();)
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
  public int rightSize(int left)
  {
    if (this.leftMap != null)
    {
      TIntSet rightSet = this.leftMap.get(left);
      return rightSet == null ? 0 : rightSet.size();
    }
    else
    {
      int computedSize = 0;
      TIntIntIterator iter = this.rightMap.iterator();
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
  public int[] rightValues(int left)
  {
    if (this.leftMap != null)
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
    else
    {
      TIntSet rightSet = new TIntHashSet();
      TIntIntIterator iter = this.rightMap.iterator();
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
  public TIntSet rightValuesIntegerSet(int left)
  {
    if (this.leftMap != null)
    {
      TIntSet rightSet = this.leftMap.get(left);
      if (rightSet != null)
      {
        return new TIntHashSet(rightSet);
      }
      else
      {
        return new TIntHashSet(0);
      }
    }
    else
    {
      TIntSet rightSet = new TIntHashSet();
      TIntIntIterator iter = this.rightMap.iterator();
      while (iter.hasNext())
      {
        iter.advance();
        if (iter.value() == left)
        {
          rightSet.add(iter.key());
        }
      }
      return new TIntHashSet(rightSet);
    }
  }

  @Override
  public IntegerRelationIterator iterator()
  {
    return new IntegerRelationIterator() {

      private boolean start = false;
      private int left = 0;
      private int right = 0;
      private TIntIntIterator mapIterator = OneToManyIntegerRelation.this.rightMap.iterator();

      @Override
      public boolean hasNext()
      {
        return this.mapIterator.hasNext();
      }

      @Override
      public int left()
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
      public int right()
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
