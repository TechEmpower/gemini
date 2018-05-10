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
package com.techempower.collection;

import java.util.*;

/**
 * This class provides the ability to compare two sets of ints, provided
 * as int arrays, and resulting in two differences arrays and a superset
 * array.
 */
public class IntegerSetComparison
{
  
  //
  // Member variables.
  //

  private int[] setA, setB, aSorted, bSorted, aMinusB, bMinusA, superSet;
  private int largerSize;
  private int smallerSize;
  private boolean computed = false;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public IntegerSetComparison(int[] setA, int[] setB)
  {
    this.setA = setA;
    this.setB = setB;
    
    // Determine the larger and smaller size.  These sizes are used to
    // create temporary arrays to capture the differences (can't be any 
    // larger than the larger array) and the superset (can't be any larger 
    // than the two arrays together).
    if (setA.length > setB.length)
    {
      this.largerSize = setA.length;
      this.smallerSize = setB.length;
    }
    else
    {
      this.largerSize = setB.length;
      this.smallerSize = setA.length;
    }
  }
  
  /**
   * Computes the resulting arrays if they have not yet been computed.
   */
  public void computeIfNeeded()
  {
    if (!this.computed)
    {
      compute();
    }
  }
  
  /**
   * Computes the resulting arrays.
   */
  public void compute()
  {
    // Create sorted versions of the original sets.
    this.aSorted = new int[this.setA.length];
    this.bSorted = new int[this.setB.length];
    
    System.arraycopy(this.setA, 0, this.aSorted, 0, this.setA.length);
    System.arraycopy(this.setB, 0, this.bSorted, 0, this.setB.length);

    // The sort operation is usually, by a large margin, the most time-
    // consuming operation.
    Arrays.sort(this.aSorted);
    Arrays.sort(this.bSorted);

    // Create temporary comparison arrays.
    int[] tempAminusB = new int[this.largerSize];
    int[] tempBminusA = new int[this.largerSize];
    int[] tempSuper = new int[this.largerSize + this.smallerSize];
    
    int positionA = 0, positionB = 0, nextA = 0, nextB = 0;
    int positionAminusB = 0, positionBminusA = 0, positionSuper = 0;

    // Compare the two sorted arrays and create the differences and superset
    // arrays.
    while ( (positionA < this.aSorted.length)
         || (positionB < this.bSorted.length)
         )
    {
      if (positionA < this.aSorted.length)
      {
        nextA = this.aSorted[positionA];
      }
      else
      {
        nextA = Integer.MAX_VALUE;
      }
      
      if (positionB < this.bSorted.length)
      {
        nextB = this.bSorted[positionB];
      }
      else
      {
        nextB = Integer.MAX_VALUE;
      }

      // The next item is unique to set A.
      if (nextA < nextB)
      {
        tempAminusB[positionAminusB++] = nextA;
        tempSuper[positionSuper++] = nextA;
        positionA++;
      }
      
      // The next item is unique to set B.
      else if (nextB < nextA)
      {
        tempBminusA[positionBminusA++] = nextB;
        tempSuper[positionSuper++] = nextB;
        positionB++;
      }
      
      // The next item is in both sets.
      else
      {
        tempSuper[positionSuper++] = nextA;
        positionA++;
        positionB++;
      }
    }
    
    // Shrink the temporary arrays and copy them over to the main arrays.
    this.aMinusB = new int[positionAminusB];
    this.bMinusA = new int[positionBminusA];
    this.superSet = new int[positionSuper];
    
    System.arraycopy(tempAminusB, 0, this.aMinusB, 0, positionAminusB);
    System.arraycopy(tempBminusA, 0, this.bMinusA, 0, positionBminusA);
    System.arraycopy(tempSuper, 0, this.superSet, 0, positionSuper);

    // Set the computed flag.
    this.computed = true;
  }
  
  /**
   * Gets the original Set A.
   */
  public int[] getSetA()
  {
    return this.setA;
  }
  
  /**
   * Gets the original Set B.
   */
  public int[] getSetB()
  {
    return this.setB;
  }
  
  /**
   * Gets Set A sorted in ascending order.
   */
  public int[] getSetASorted()
  {
    computeIfNeeded();
    return this.aSorted; 
  }

  /**
   * Gets Set B sorted in ascending order.
   */
  public int[] getSetBSorted()
  {
    computeIfNeeded();
    return this.bSorted; 
  }

  /**
   * Gets the differences set of A minus B (A-B).
   */
  public int[] getAMinusB()
  {
    computeIfNeeded();
    return this.aMinusB; 
  }

  /**
   * Gets the differences set of B minus A (B-A).
   */
  public int[] getBMinusA()
  {
    computeIfNeeded();
    return this.bMinusA; 
  }

  /**
   * Gets the super set of A+B.  Only one instance of each ID will be
   * included; duplicates are removed.
   */
  public int[] getSuperset()
  {
    computeIfNeeded();
    return this.superSet; 
  }

}   // End IntegerSetComparison.
