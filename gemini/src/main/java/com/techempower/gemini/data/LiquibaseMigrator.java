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

import java.sql.*;
import java.util.*;
import java.util.Map.*;

import javax.sql.*;

import org.slf4j.*;

import com.techempower.gemini.*;
import com.techempower.helper.*;
import com.techempower.util.*;

import liquibase.*;
import liquibase.changelog.*;
import liquibase.database.*;
import liquibase.database.jvm.*;
import liquibase.exception.*;
import liquibase.resource.*;

/**
 * Implementation of DatabaseMigrator that uses the Liquibase library.
 */
public class LiquibaseMigrator implements DatabaseMigrator
{

  private Logger log = LoggerFactory.getLogger(getClass());
  private GeminiApplication app;
  private String changeLogFileName = "changelog.xml";

  /**
   * Constructor
   */
  public LiquibaseMigrator(GeminiApplication app)
  {
    this.app = app;
    this.app.getConfigurator().addConfigurable(this);
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    // Build a map of Liquibase-specific configuration
    final String confPrefix = "liquibase.";
    Map<String, String> conf = new HashMap<>();
    for (String name : props.names()) {
      if (StringHelper.startsWithIgnoreCase(name, confPrefix)) {
        conf.put(name, props.get(name));
      }
    }

    // Log configuration customizations
    for (Entry<String, String> e : conf.entrySet()) {
      log.info("Liquibase configuration customization: {}: {}", e.getKey(), e.getValue());
    }

    // The only configuration value we actually support is the changelog file name.
    String configuredChangeLog = conf.get("changelog");
    if (StringHelper.isNonEmpty(configuredChangeLog)) {
      this.changeLogFileName = configuredChangeLog;
    }
    log.info("Liquibase changelog: {}", changeLogFileName);
  }

  /**
   * Private helper for consistency between migrate() and listPendingMigrations()
   */
  private Contexts createContexts()
  {
    // This is so we can determine whether to run the "insert sample data"
    // migrations or not.
    if (this.app.getVersion().isProduction()) {
      return new Contexts("production");
    } else if (this.app.getVersion().isTest()) {
      return new Contexts("test");
    } else if (this.app.getVersion().isDevelopment()) {
      return new Contexts("development");
    }
    return new Contexts();
  }

  @Override
  public int migrate(DataSource dataSource)
  {
    log.info("Starting migrate");
    try (Connection conn = dataSource.getConnection();
        Liquibase liquibase = new liquibase.Liquibase(changeLogFileName, new ClassLoaderResourceAccessor(),
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn)))) {
      LabelExpression labels = new LabelExpression();
      Contexts c = createContexts();
      int countUnrunChangesetsBefore = liquibase.listUnrunChangeSets(c, labels).size();
      log.info("Unrun changesets: {}", countUnrunChangesetsBefore);
      if (countUnrunChangesetsBefore <= 0) {
        // Nothing to do.
        return 0;
      }
      liquibase.update(c, labels);
      int countUnrunChangesetsAfter = liquibase.listUnrunChangeSets(c, labels).size();
      return countUnrunChangesetsBefore - countUnrunChangesetsAfter;
    } catch (SQLException | LiquibaseException e) {
      log.error("migrate caught ", e);
      return 0;
    }
  }

  @Override
  public List<String> listPendingMigrations(DataSource dataSource)
  {
    log.info("Starting listPendingMigrations");
    try (Connection conn = dataSource.getConnection();
        Liquibase liquibase = new liquibase.Liquibase(changeLogFileName, new ClassLoaderResourceAccessor(),
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn)))) {
      LabelExpression labels = new LabelExpression();
      Contexts c = createContexts();
      List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(c, labels);
      List<String> toReturn = new ArrayList<>();
      for (ChangeSet changeSet : unrunChangeSets) {
        toReturn.add("Unrun changeset: " + changeSet + ". Change log: " + changeSet.getChangeLog() + ". Changes: "
            + changeSet.getChanges());
      }
      return toReturn;
    } catch (SQLException | LiquibaseException e) {
      log.error("listPendingMigrations caught ", e);
      // Returning an empty list implies success, so we must return a non-empty list
      // since we don't know the migration state.
      List<String> toReturn = new ArrayList<>();
      toReturn.add("Unable to determine pending migrations, caught: " + e);
      return toReturn;
    }
  }
}
