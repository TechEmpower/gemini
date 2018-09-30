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
package com.techempower.gemini.lifecycle;

import java.sql.*;

import com.techempower.data.*;
import com.techempower.gemini.*;
import com.techempower.gemini.data.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * Applies any pending database migrations.
 */
public class InitDatabaseMigrations implements InitializationTask, Configurable
{
  private boolean enabled = false;

  /**
   * Constructor.
   */
  public InitDatabaseMigrations(GeminiApplication app)
  {
    app.getConfigurator().addConfigurable(this);
  }

  @Override
  public void taskInitialize(GeminiApplication app)
  {
    final ComponentLog log = app.getLog(COMPONENT_CODE);
    if (!enabled)
    {
      log.log("Database migrations disabled.");
      return;
    }

    final ConnectorFactory cf = app.getConnectorFactory();
    if (!cf.isEnabled())
    {
      log.log("ConnectorFactor not enabled. Skipping database migrations.");
      return;
    }

    final DatabaseMigrator migrator = app.getDatabaseMigrator();
    if (migrator == null)
    {
      log.log("DatabaseMigrator unavailable. Skipping database migrations.");
      return;
    }

    log.log("Applying database migrations.");

    try (ConnectionMonitor monitor = cf.getConnectionMonitor())
    {
      // Start the migration
      int migrationsApplied = migrator.migrate(monitor);
      log.log("Database migrations applied: " + migrationsApplied);
    }
    catch (SQLException e)
    {
      log.log("Database migrations caught exception ", e);
    }
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    enabled = props.getBoolean("Initialization.DbMigrations.Enabled", false);
  }

}
