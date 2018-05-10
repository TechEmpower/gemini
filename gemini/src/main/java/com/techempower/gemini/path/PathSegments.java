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
package com.techempower.gemini.path;

import com.techempower.collection.*;
import com.techempower.helper.*;

/**
 * A thin abstraction over a String array composed of the path segments (or
 * "directories") in a request URI.  E.g., /user/list/1 is represented as a
 * PathSegments object containing these entries:
 *   <ul>
 * <li>0: user</li>
 * <li>1: list</li>
 * <li>2: 1</li>
 *   </ul>>
 * The zero index is offset before handing this object off to PathHandlers.
 * This allows the PathHandlers to always consume their arguments starting at
 * index 0.
 */
public class PathSegments
{

  //
  // Variables.
  //
  
  private final String     uri;
  private final String[]   segments;
  private int              offset = 0;
  private ImmutableNamedStrings namedSegments;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public PathSegments(String uri)
  {
    // Strip off the leading '/' character.
    if (  (uri.length() > 1)
       && (uri.charAt(0) == '/')
       )
    {
      this.uri = uri.substring(1);
    }
    else
    {
      this.uri = uri;
    }
    
    segments = this.uri.split("/");
  }
  
  /**
   * Sets the index offset.
   */
  protected PathSegments offset(int newOffset)
  {
    this.offset = NumberHelper.boundInteger(newOffset, 0, segments.length);
    return this;
  }
  
  /**
   * Increases the index offset.
   */
  public PathSegments increaseOffset()
  {
    return offset(this.offset + 1);
  }
  
  /**
   * Decreases the index offset.
   */
  public PathSegments decreaseOffset()
  {
    return offset(this.offset - 1);
  }
  
  /**
   * Assign a name to one of the segments, creating the named-segment (or
   * "argument") map.
   * 
   * @param index An offset-adjusted index, similar to the parameter for the
   *     get method.
   */
  protected PathSegments assignName(int index, String name)
  {
    // Lazy initialization.
    if (namedSegments == null)
    {
      namedSegments = new ImmutableNamedStrings(Math.max(segments.length, 100));
    }

    // Do nothing if the named segments are sealed.
    if (!namedSegments.isSealed())
    {
      // Do we have a proper offset-adjusted index?
      final int assignIndex = index + offset;
      if (  (assignIndex >= 0)
         && (assignIndex < segments.length)
         )
      {
        namedSegments.put(name, segments[assignIndex]);
      }
    }
    
    return this;
  }
  
  /**
   * Gets the arguments.  Returns an empty map if there are not arguments
   * available.
   */
  public NamedValues getArguments()
  {
    // Instantiate an empty map if we have no arguments.
    if (namedSegments == null)
    {
      namedSegments = new ImmutableNamedStrings(0);
    }
    
    return namedSegments.seal();
  }
  
  /**
   * Gets the full URI, without the leading '/'.
   */
  public String getUri()
  {
    return this.uri;
  }
  
  /**
   * Gets the full URI, with leading '/'.
   */
  public String getUriFromRoot()
  {
    return (this.uri.length() > 1 ? "/" : "") + this.uri;
  }
  
  /**
   * Gets the URI from the root to just below the offset path.  E.g., if 
   * offset is 2 and the full URI is /foo/bar/baz/abc, the returned value will
   * be /foo/bar.  Returned string does not include a trailing slash.
   */
  public String getUriBelowOffset()
  {
    if (this.offset == 0)
    {
      return "/";
    }
    
    final StringBuilder toReturn = new StringBuilder(this.uri.length() + 1);
    for (int i = 0; i < this.offset; i++)
    {
      toReturn.append('/');
      toReturn.append(segments[i]);
    }
    
    return toReturn.toString();
  }
  
  /**
   * Gets the count of path segments available to consume.  This is computed
   * as the total number of path segments in the source URI less the current
   * offset.  That is, from the perspective of a PathHandler, this is 
   * typically 1 less than the total number of segments in the URI. 
   */
  public int getCount()
  {
    return segments.length - this.offset;
  }
  
  /**
   * Gets the offset.
   */
  public int getOffset()
  {
    return this.offset;
  }
  
  /**
   * Gets a segment as a String.  The index is adjusted for the current 
   * offset.  So if a request of "/user/list/1" was dispatched to a 
   * UserHandler using the initial path segment of "user", the remaining
   * segments are: 
   *   <ul>
   * <li>0: list</li>
   * <li>1: 1</li>
   *   </ul>
   * Returns null if the index is out of range, which is not an exceptional
   * case, since this may simply indicate the URI did not include an optional
   * segment.
   *   
   * @param index The offset-adjusted index.  See method description.
   */
  public String get(int index)
  {
    final int getIndex = index + offset;
    
    if (  (getIndex >= 0)
       && (getIndex < segments.length)
       )
    {
      return segments[getIndex];
    }
    else
    {
      return null;
    }
  }
  
  /**
   * Gets a segment, returning a default value if the segment is missing.
   *   
   * @param index The offset-adjusted index.  See the method description for
   * get().
   * @param defaultValue A default value to return if the segment is missing.
   */
  public String get(int index, String defaultValue)
  {
    final String rawValue = get(index);
    return rawValue == null 
        ? defaultValue
        : rawValue; 
  }
  
  /**
   * Gets a segment as an int.
   *   
   * @param index The offset-adjusted index.  See the method description for
   * get().
   * @param defaultValue A default value to return if the segment is missing
   * or is not an integer.
   */
  public int getInt(int index, int defaultValue)
  {
    return NumberHelper.parseInt(get(index), defaultValue);
  }
  
  /**
   * Gets a segment as an int, defaulting to 0 if the segment is missing or
   * is not an integer.
   *   
   * @param index The offset-adjusted index.  See the method description for
   * get().
   */
  public int getInt(int index)
  {
    return getInt(index, 0);
  }
  
  /**
   * Gets a segment as a long.
   *   
   * @param index The offset-adjusted index.  See the method description for
   * get().
   * @param defaultValue A default value to return if the segment is missing
   * or is not an integer.
   */
  public long getLong(int index, long defaultValue)
  {
    return NumberHelper.parseLong(get(index), defaultValue);
  }
  
  /**
   * Gets a segment as a long, defaulting to 0 if the segment is missing or
   * is not an integer.
   *   
   * @param index The offset-adjusted index.  See the method description for
   * get().
   */
  public long getLong(int index)
  {
    return getLong(index, 0L);
  }
  
  /**
   * Standard toString functionality.  Separates segments with " / ".
   */
  @Override
  public String toString()
  {
    return "PathSegments [" + StringHelper.join(" / ", segments) + "]";
  }
  
}
