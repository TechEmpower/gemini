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
package com.techempower.cache;

import com.techempower.helper.*;

/**
 * Manages a reference that expires after a period of time and is recomputed
 * after each expiration.
 */
public abstract class PeriodicRecompute<E> 
{

  private E reference;
  private long nextRefresh;
  
  /**
   * Constructor.
   * 
   * @param initialReference an initial value of the contained reference.
   */
  public PeriodicRecompute(E initialReference)
  {
    reference = initialReference;
  }
  
  /**
   * Constructor.  The initial value will be computed on the first call to 
   * get.
   */
  public PeriodicRecompute()
  {
  }
  
  /**
   * Gets the reference, recomputing only if needed.
   */
  public E get()
  {
    E value = reference;
    final long now = System.currentTimeMillis();
        
    if (  (value == null)
       || (now >= nextRefresh)
       )
    {
      value = compute();
      reference = value;
      nextRefresh = nextExpiration();
    }
    
    return value;
  }
  
  /**
   * Forcefully set the reference.
   */
  public void set(E reference)
  {
    this.reference = reference;
  }
  
  /**
   * Forcefully clears the reference so that the next get will recompute.
   */
  public void clear()
  {
    reference = null;
  }
  
  /**
   * Computes the reference when it is first requested after an expiration.
   * This operation should be idempotent because this class provides no
   * locking and multiple concurrent get() calls after expiration may result
   * in multiple concurrent computation of the new value.  If a lock is
   * desired, the implementing class should add a lock in the compute method
   * implementation. 
   */
  protected abstract E compute();
  
  /**
   * Determines the next expiration time.
   */
  protected abstract long nextExpiration();
  
  /**
   * A timed reference that expires every day at midnight local time.
   */
  public abstract static class Daily<E> extends PeriodicRecompute<E>
  {
    public Daily()
    {
      super();
    }
    
    public Daily(E initialReference)
    {
      super(initialReference);
    }
    
    @Override
    protected long nextExpiration()
    {
      return DateHelper.getEndOfDay().getTimeInMillis() + 1;
    }
  }
  
  /**
   * An implementation that uses a specified minimum interval in milliseconds.
   */
  public abstract static class Interval<E> extends PeriodicRecompute<E>
  {
    private final long interval;
    
    public Interval(long intervalMs)
    {
      super();
      this.interval = intervalMs;
    }
    
    public Interval(E initialReference, long intervalMs)
    {
      super(initialReference);
      this.interval = intervalMs;
    }
    
    @Override
    protected long nextExpiration()
    {
      return System.currentTimeMillis() + interval;
    }
  }
  
}
