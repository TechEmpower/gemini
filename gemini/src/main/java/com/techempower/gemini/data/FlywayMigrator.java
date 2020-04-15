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
package com.techempower.gemini.data;

import java.util.*;
import java.util.Map.*;

import javax.sql.*;

import org.flywaydb.core.*;
import org.flywaydb.core.api.*;
import org.flywaydb.core.api.configuration.*;

import com.techempower.gemini.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DatabaseMigrator that uses the Flyway library.
 */
public class FlywayMigrator implements DatabaseMigrator
{

  private FluentConfiguration flywayConfig;
  private Logger              log = LoggerFactory.getLogger(getClass());
  private GeminiApplication   app;

  /**
   * Constructor
   */
  public FlywayMigrator(GeminiApplication app)
  {
    this.app = app;
    this.app.getConfigurator().addConfigurable(this);
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    // Build a map of Flyway-specific configuration
    final String confPrefix = "flyway.";
    Map<String, String> conf = new HashMap<>();
    for (String name : props.names())
    {
      if (StringHelper.startsWithIgnoreCase(name, confPrefix))
      {
        conf.put(name, props.get(name));
      }
    }

    // Log configuration customizations
    for (Entry<String, String> e : conf.entrySet())
    {
      log.info("Flyway configuration customization: {}: {}",
          e.getKey(), e.getValue());
    }

    // Create the Flyway configuration
    flywayConfig = Flyway.configure()
        .outOfOrder(true)
        .configuration(conf);

    // Log migration locations being used
    for (Location l : flywayConfig.getLocations())
    {
      log.info("Flyway location: {}", l);
    }
  }

  @Override
  public int migrate(DataSource dataSource)
  {
    if (flywayConfig != null)
    {
      Flyway f = flywayConfig.dataSource(dataSource).load();
      for (MigrationInfo i : f.info().pending())
      {
        log.info("Pending migration: {}", i.getScript());
      }
      return f.migrate();
    }
    log.info("Flyway not initialized. Skipping database migrations.");
    return 0;
  }
}
