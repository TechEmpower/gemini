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

/**
 * Simple encapsulation of a TimeZone and a human-friendly name/description.
 * These are used to provide a dramatically-truncated listing of Time Zones
 * for selection by users.  See UtilityConstants.SIMPLE_TIME_ZONES.
 *   <p>
 * TODO: Consider deprecation.
 */
public class TimeZoneDescriptor
{

  //
  // Variables.
  //
  
  private final int      index;
  private final TimeZone timezone;
  private final String   description;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   * 
   * @param index An externally-sourced unique index for the Zone.
   * @param timezoneID The String identifier used by the Java TimeZone class
   *        to identify the zone (e.g., "UTC").
   * @param description A human-readable description of the Zone. 
   */
  public TimeZoneDescriptor(int index, String timezoneID, String description)
  {
    this.index = index;
    this.timezone = TimeZone.getTimeZone(timezoneID);
    this.description = description;
  }
  
  /**
   * Gets the index for this Zone.  This is simply an externally-sourced 
   * integer identifier.
   */
  public int getIndex()
  {
    return this.index;
  }
  
  /**
   * Gets the Java TimeZone object for which this descriptor is an
   * abstraction.
   */
  public TimeZone getTimeZone()
  {
    return this.timezone;
  }
  
  /**
   * Gets a human-readable description of the Zone.  E.g., 
   * "UTC-11: American Samoa, Samoa"
   */
  public String getDescription()
  {
    return this.description;
  }
  
}
