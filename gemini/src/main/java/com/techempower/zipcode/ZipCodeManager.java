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

package com.techempower.zipcode;

import com.techempower.util.*;

/**
 * Provides Zip code range utility.  The general usage is to find a set
 * of zip codes within a given radius of a provided zip code.  Could
 * be adapted to do other calculations such as compute the difference
 * between two zip codes.
 *   <p>
 * This class contains application-specific configuration variables and
 * constructs ZipCodeRange objects to provide usable results to range
 * searches.
 *   <p>
 * The properties that this component reads from the configuration file
 * are below.  Usually, it's only necessary to provide a name for the
 * two database columns.  The other parameters can be omitted to use
 * their default values:
 *   <ul>
 * <li> zip.RadiusOfEarth - the radius of the Earth in miles, use default.
 * <li> zip.DeltaDivisor - an adjustment factor, use default.
 * <li> zip.LatitudeColumn - the table column that stores latitudes.
 * <li> zip.LongitudeColumn - the table column that stores longitudes.
 *   </ul>
 */
public class ZipCodeManager
{

  //
  // Constants.
  //

  public static final String  COMPONENT_CODE           = "zipm";
  public static final int     DEFAULT_RADIUS_OF_EARTH  = 3956;       // miles (= 6373 km).
  public static final double  DEFAULT_DELTA_DIVISOR    = 1.0D;
  public static final String  DEFAULT_LATITUDE         = "ZipLat";   // Field name in ZipCode table
  public static final String  DEFAULT_LONGITUDE        = "ZipLon";   // Field name in ZipCode table

  //
  // Member variables.
  //

  private int    radiusOfEarth   = DEFAULT_RADIUS_OF_EARTH;
  private double deltaDivisor    = DEFAULT_DELTA_DIVISOR;
  private String latitudeColumn  = DEFAULT_LATITUDE;
  private String longitudeColumn = DEFAULT_LONGITUDE;

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public ZipCodeManager()
  {
  }

  /**
   * Configures this component.
   */
  public void configure(EnhancedProperties props)
  {
    this.radiusOfEarth   = props.getInt("zip.RadiusOfEarth", this.radiusOfEarth);
    this.deltaDivisor    = props.getDouble("zip.DeltaDivisor", this.deltaDivisor);
    this.latitudeColumn  = props.get("zip.LatitudeColumn", this.latitudeColumn);
    this.longitudeColumn = props.get("zip.LongitudeColumn", this.longitudeColumn);
  }

  /**
   * Constructs a ZipCodeRange object off of a provided latitude and
   * longitude range.
   */
  public ZipCodeRange getZipCodeRange(double latitudeMin, 
    double latitudeMax, double longitudeMin, double longitudeMax)
  {
    return new ZipCodeRange(this, latitudeMin, latitudeMax,
      longitudeMin, longitudeMax);
  }

  /**
   * Constructs a ZipCodeRange object off of a provided center point
   * and a distance in miles.
   *
   * @param sourceLon the source longitude, in degrees.
   * @param sourceLat the source latitude, in degrees.
   * @param distanceInMiles the preferred distance from center in miles.
   */
  public ZipCodeRange getZipCodeRange(double sourceLon, double sourceLat, 
    int distanceInMiles)
  {
    return new ZipCodeRange(this, sourceLon, sourceLat, distanceInMiles);
  }

  /**
   * Gets the radius of the earth.
   */
  public int getRadiusOfEarth()
  {
    return this.radiusOfEarth;
  }

  /**
   * Gets the delta divisor.
   */
  public double getDeltaDivisor()
  {
    return this.deltaDivisor;
  }

  /**
   * Gets the longitude column name.
   */
  public String getLongitudeColumn()
  {
    return this.longitudeColumn;
  }

  /**
   * Gets the latitude column name.
   */
  public String getLatitudeColumn()
  {
    return this.latitudeColumn;
  }

}   // End ZipCodeManager.
