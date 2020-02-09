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
package com.techempower.data.jdbc;

import java.sql.*;
import java.util.*;

import org.slf4j.Logger;

/**
 * A pure-static helper class for the JDBC components.
 */
final class JdbcHelper
{

  /**
   * Load a JDBC driver.
   * 
   * @param driverClassName the JDBC driver's classname.
   */
  public static void loadDriver(String driverClassName, Logger log)
  {
    if (log != null)
    {
      log.info("Loading JDBC driver: {}", driverClassName);
    }

    try
    {
      // Load driver class.
      Class.forName(driverClassName);
    }
    catch (java.lang.ClassNotFoundException cnfexc)
    {
      if (log != null)
      {
        log.error(
            "ClassNotFound while attempting to load JDBC driver",
            cnfexc);
      }
    }
  }
  
  /**
   * Gets an enumeration of the loaded Drivers.
   */
  public static Enumeration<Driver> getLoadedDrivers()
  {
    return DriverManager.getDrivers();
  }

  /**
   * No constructor.
   */
  private JdbcHelper() { }
  
}
