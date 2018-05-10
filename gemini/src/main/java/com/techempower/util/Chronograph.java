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

import java.util.concurrent.atomic.*;

import com.techempower.helper.*;

/**
 * Provides simple time measurement for debugging, diagnostic, and logging
 * purposes.  Basic use is to create a chronograph and periodically render the
 * elapsed time by using the toString method (e.g., in a log statement).
 *   <p>
 * This class is so named to avoid conflicting with Java's Timer class and 
 * Google Guava's similar Stopwatch class (which renders in a manner 
 * unfriendly for log searches).
 */
public class Chronograph 
{
  
  private static final AtomicInteger identityCounter = new AtomicInteger(0);
  
  private final int id;
  private final long start;
  private       long last;
  private final String name;
  private final String prefix;
  private final boolean ns;
  private final boolean renderHumanReadable;
  private final boolean renderSinceLast;
  
  /**
   * Constructor.
   * 
   * @param name An optional name identifying this chronograph; can be null.
   * @param useNanos Use nanoseconds rather than milliseconds; false will use
   *   milliseconds.
   * @param renderHumanReadable renders elapsed time in human readable form
   *   in addition to milliseconds; if false, only milliseconds will be
   *   rendered.
   * @param renderSinceLast include a measurement of incremental time since
   *   since the last call to elapsedNanos, elapsed, or toString.
   */
  public Chronograph(String name, boolean useNanos, 
      boolean renderHumanReadable, boolean renderSinceLast)
  {
    this.id = identityCounter.incrementAndGet();
    this.ns = useNanos;
    this.name = name;
    this.prefix = "[" + id + "] "
        + (StringHelper.isNonEmpty(name) ? name + " " : "");
    this.renderHumanReadable = renderHumanReadable;
    this.start = System.nanoTime();
    this.renderSinceLast = renderSinceLast;
    this.last = start;
  }
  
  /**
   * Simplified constructor.  This assumes a rendering with milliseconds and
   * human-readable rendering.
   */
  public Chronograph(String name)
  {
    this(name, false, true, true);
  }
  
  /**
   * Simplified constructor.  This assumes a rendering with milliseconds and
   * human-readable rendering but with no name assigned to the Chronograph.
   */
  public Chronograph()
  {
    this(null, false, true, true);
  }
  
  /**
   * Gets the name of this Chronograph.
   */
  public String name() 
  {
    return name;
  }
  
  /**
   * Gets the id of this Chronograph.
   */
  public int id()
  {
    return id;
  }
  
  /**
   * Determines the elapsed time in nanoseconds.
   */
  public long elapsedNanos()
  {
    last = System.nanoTime();
    return (last - start);
  }
  
  /**
   * Determines the elapsed time since the last call to elapsedNanos,
   * elapsed, or toString.  Returned in nanoseconds.
   */
  public long sinceLastNanos()
  {
    return (System.nanoTime() - last);
  }
  
  /**
   * Determines the elapsed time in milliseconds.
   */
  public long elapsed()
  {
    last = System.nanoTime();
    return (last - start) / UtilityConstants.NANOS_PER_MILLISECOND;
  }

  /**
   * Determines the elapsed time since the last call to elapsedNanos,
   * elapsed, or toString.  Returned in milliseconds.
   */
  public long sinceLast()
  {
    return (System.nanoTime() - last) / UtilityConstants.NANOS_PER_MILLISECOND;
  }

  @Override
  public String toString()
  {
    final long now = System.nanoTime();
    final long elapsedNs = now - start;
    final long elapsedMs = elapsedNs / UtilityConstants.NANOS_PER_MILLISECOND;
    final long sinceLastNs = now - last;
    final long sinceLastMs = sinceLastNs / UtilityConstants.NANOS_PER_MILLISECOND;
    last = now;
    
    return prefix
        + (ns
            ? elapsedNs + "ns"
            : elapsedMs + "ms")            
        + (renderHumanReadable
            ? " (" + DateHelper.getHumanDuration(elapsedMs, 2, true) + ")" 
            : "")
        + (renderSinceLast 
            ? " total; " + (ns
                ? sinceLastNs + "ns"
                : sinceLastMs + "ms")            
                + (renderHumanReadable 
                ? " (" + DateHelper.getHumanDuration(sinceLastMs, 2, true) + ")" 
                : "")
                + " since last"
            : "");
  }

}
