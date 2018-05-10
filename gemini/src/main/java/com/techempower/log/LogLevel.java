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

package com.techempower.log;

import com.techempower.helper.*;

/**
 * Log notification level constants, used by Log and any class that uses
 * the Log or ComponentLog classes.
 *
 * @see Log
 * @see ComponentLog
 */
public interface LogLevel
{

  String[] LEVEL_NAMES = { 
    "Minimum (0)", "Debug (10)", "Notice (30)", "Normal (50)",
    "Alert (70)", "Critical (90)", "Maximum (100)"
  };
  int[] LEVEL_VALUES = new int[] { 0, 10, 30, 50, 70, 90, 100 };
  String[] LEVEL_VALUES_STRING = 
    CollectionHelper.toStringArray(LEVEL_VALUES);

  String[] DENSE_LEVEL_NAMES = { 
    "0 (Minimum)", "5", "10 (Debug)", "15", "20", "25", "30 (Notice)", "35", "40", "45", "50 (Normal)",
    "55", "60", "65", "70 (Alert)", "75", "80", "85", "90 (Critical)", "95", "100 (Maximum)"
  };
  int[] DENSE_LEVEL_VALUES = new int[] { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100 };
  String[] DENSE_LEVEL_VALUES_STRING = 
    CollectionHelper.toStringArray(DENSE_LEVEL_VALUES);

  /*
   * Notification level constants.  The value of the constants range from 0 to
   * 100.  However the user may use -any- values in that range.  The constants 
   * are merely for convenience.  If a debug level is not provided, logging 
   * will occur at level 50 (NORMAL).
   */

  /**
   * Notification constant 1 (value=0).  This is lowest notification level.
   */
  int     MINIMUM                 = 0;
  
  /**
   * Notification constant 2 (value=10).
   */
  int     DEBUG                   = 10;
  
  /**
   * Notification constant 3 (value=30).
   */
  int     NOTICE                  = 30;
  
  /**
   * Notification constant 4 (value=50).
   */
  int     NORMAL                  = 50;
  
  /**
   * Notification constant 5 (value=70).
   */
  int     ALERT                   = 70;
  
  /**
   * Notification constant 6 (value=90).
   */
  int     CRITICAL                = 90;
  
  /**
   * Notification constant 7 (value=100).
   */
  int     MAXIMUM                 = 100;
}
