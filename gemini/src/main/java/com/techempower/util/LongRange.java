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

import java.util.*;

import com.techempower.helper.*;

/**
 * A minimum and maximum integer, together defining a range.
 */
public class LongRange
  implements Iterable<Long>
{

  //
  // Some standard ranges.
  //
  
  public static final LongRange POSITIVE = new LongRange(1, Long.MAX_VALUE);
  public static final LongRange POSITIVE_OR_ZERO = new LongRange(0, Long.MAX_VALUE);
  public static final LongRange NEGATIVE = new LongRange(Long.MIN_VALUE, -1);
  public static final LongRange NEGATIVE_OR_ZERO = new LongRange(Long.MIN_VALUE, 0);

  public final long min;
  public final long max;
  
  /**
   * Constructor.
   */
  public LongRange(long min, long max)
  {
    Args.longMin(max, "max", min);
    this.min = min;
    this.max = max;
  }

  @Override
  public Iterator<Long> iterator() {
    return new Iterator<Long>() {
      long position = min;
      @Override public void remove() { }
      @Override public Long next() { return position++; }
      @Override public boolean hasNext() { return position <= max; }
    };
  }
  
  /**
   * Return the full extent that the range covers from end to end; the width
   * of the range as depicted on a number line.  For example, the width of
   * the range (0,2) is 2 and the width of (-1,1) is also 2.
   * 
   * <pre>
   * new LongRange(4,5).span(); // 1
   * </pre>
   */
  public long width()
  {
    return Math.abs(max - min);
  }
  
  /**
   * Return the number of longs contained in this range, inclusive of both
   * edges.  In other words, the count is the width plus 1.  For example, the
   * count of (0,2) is 3 because the range encompasses three longs: 0, 1, 
   * and 2.
   * 
   * <pre>
   * new LongRange(4,5).count(); // 2
   * </pre>
   */
  public long count()
  {
    return this.width() + 1;
  }
  
  /**
   * Is a provided number in the range?
   */
  public boolean contains(long number)
  {
    return ( (number >= min) && (number <= max) );
  }
  
  /**
   * Bound a provided parameter by this range, meaning that inputs outside
   * of the range will be trimmed to the edge of the range.  An input
   * exceeding the maximum will be set to the maximum; an input lower than
   * the minimum will be set to the minimum.
   */
  public long bound(long number)
  {
    return NumberHelper.boundLong(number, this);
  }

}
