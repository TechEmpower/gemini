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

/**
 * Encapsulates a Zip code range.
 */
public class ZipCodeRange
{

  //
  // Member variables.
  //

  private ZipCodeManager manager;

  private double latitudeMinimum  = 0.0D;
  private double latitudeCenter   = 0.0D;
  private double latitudeMaximum  = 0.0D;

  private double longitudeMinimum = 0.0D;
  private double longitudeCenter  = 0.0D;
  private double longitudeMaximum = 0.0D;

  //
  // Member methods.
  //

  /**
   * Plain constructor.
   *   <p>
   * Use ZipCodeManager.getZipCodeRange instead.
   */
  protected ZipCodeRange(ZipCodeManager manager, double latitudeMin,
    double latitudeMax, double longitudeMin, double longitudeMax)
  {
    this.manager          = manager;

    this.latitudeMinimum  = latitudeMin;
    this.latitudeMaximum  = latitudeMax;

    this.longitudeMinimum = longitudeMin;
    this.longitudeMaximum = longitudeMax;

    this.latitudeCenter        = Math.abs((this.latitudeMaximum - this.latitudeMinimum) / 2);
    this.longitudeCenter       = Math.abs((this.longitudeMaximum - this.longitudeMinimum) / 2);
  }

  /**
   * Use ZipCodeManager.getZipCodeRange instead.
   *
   * @param sourceLon the source longitude, in degrees.
   * @param sourceLat the source latitude, in degrees.
   * @param distanceInMiles the preferred distance from center in miles.
   */
  protected ZipCodeRange(ZipCodeManager manager, double sourceLon, 
    double sourceLat, int distanceInMiles)
  {
    this.manager          = manager;

    double dLat = dLat(distanceInMiles);
    double dLon = dLon(sourceLat, distanceInMiles);

    this.latitudeMinimum  = sourceLat - (dLat / manager.getDeltaDivisor());
    this.latitudeMaximum  = sourceLat + (dLat / manager.getDeltaDivisor());

    this.longitudeMinimum = sourceLon - (dLon / manager.getDeltaDivisor());
    this.longitudeMaximum = sourceLon + (dLon / manager.getDeltaDivisor());

    this.latitudeCenter        = Math.abs((this.latitudeMaximum - this.latitudeMinimum) / 2);
    this.longitudeCenter       = Math.abs((this.longitudeMaximum - this.longitudeMinimum) / 2);
  }

  /**
   * Provides an expression for a WHERE clause (does not return a 
   * complete WHERE clause!).
   */
  public String getWhereExpression()
  {
    return "((" + this.manager.getLatitudeColumn() 
      + " BETWEEN " + getLatitudeMinimum() 
      + " AND " + getLatitudeMaximum()
      + ") AND (" + this.manager.getLongitudeColumn() 
      + " BETWEEN " + getLongitudeMinimum() 
      + " AND " + getLongitudeMaximum() + "))";
  }

  /**
   * Determining if a range's <i>center point</i> is within the longitude
   * and latitude ranges of this object.
   */
  public boolean isZipCodeWithinRange(ZipCodeRange aZipRange)
  {
    return aZipRange != null && isZipCodeWithinRange(
        aZipRange.getLatitudeCenter(), aZipRange.getLongitudeCenter());
  }

  /**
   * Determines if a provided latitude and longitude are within the ranges
   * of this object.
   */
  public boolean isZipCodeWithinRange(double givenLat, double givenLon)
  {
    return ( (givenLat >= this.latitudeMinimum && givenLat <= this.latitudeMaximum)
      &&     (givenLon >= this.longitudeMinimum && givenLon <= this.longitudeMaximum)
    );
  }

  /**
   * Gets the latitude minimum.
   */
  public double getLatitudeMinimum()
  {
    return this.latitudeMinimum;
  }

  /**
   * Gets the latitude center.
   */
  public double getLatitudeCenter()
  {
    return this.latitudeCenter;
  }

  /**
   * Gets the latitude maximum.
   */
  public double getLatitudeMaximum()
  {
    return this.latitudeMaximum;
  }

  /**
   * Gets the longitude minimum.
   */
  public double getLongitudeMinimum()
  {
    return this.longitudeMinimum;
  }

  /**
   * Gets the longitude center.
   */
  public double getLongitudeCenter()
  {
    return this.longitudeCenter;
  }

  /**
   * Gets the longitude maximum.
   */
  public double getLongitudeMaximum()
  {
    return this.longitudeMaximum;
  }

  /**
   * Provides a standard toString.
   */
  @Override
  public String toString()
  {
    return "ZipCodeRange [lat: " + getLatitudeMinimum() + " to " + getLatitudeMaximum() 
      + "; lon: " + getLongitudeMinimum() + " to " + getLongitudeMaximum() + "]";
  }

  /**
   * Gets a delta longitude (in degrees) from a source longitude (in 
   * degrees) and input distance.
   *
   * @param sourceLat the source latitude, in degrees.
   * @param distanceInMiles the preferred distance from center in miles.
   */
  public double dLon(double sourceLat, int distanceInMiles)
  {
    double sourceLatRadians = Math.toRadians(sourceLat);

    double dLon = (distanceInMiles) / 
      (Math.cos(sourceLatRadians) * this.manager.getRadiusOfEarth());

    return Math.abs(Math.toDegrees(dLon));
  }

  /**
   * Gets a delta latitude (in degrees) from an input distance.
   *
   * @param distanceInMiles the preferred distance from center in miles.
   */
  public double dLat(int distanceInMiles)
  {
    double dLat = ((double)distanceInMiles) / (this.manager.getRadiusOfEarth());

    return Math.abs(Math.toDegrees(dLat));
  }

}   // End ZipCodeRange.
