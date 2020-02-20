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

package com.techempower.audit;

import java.sql.*;
import java.util.*;

import com.techempower.*;
import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic database audit listener that uses a BasicConnectorFactory and
 * runs simple INSERT queries to insert audits.  The standard schema for
 * the Audit table is below:
 * <p>
 * <pre>{@code
 * CREATE TABLE Audit (
 *   AuditID bigint IDENTITY (1, 1) NOT NULL ,
 *   Type int NOT NULL ,
 *   AuditTime datetime NOT NULL ,
 *   Affected bigint NOT NULL ,
 *   AffectedTypeID int NOT NULL ,
 *   Cause bigint NOT NULL ,
 *   CauseTypeID int NOT NULL ,
 *   AttributeID int NOT NULL ,
 *   OriginalValue varchar (100) ,
 *   NewValue varchar (100) 
 * )
 * }</pre>
 */
public class BasicDatabaseAuditListener
  implements AuditListener
{

  //
  // Member variables.
  //
  
  private final ConnectorFactory       connFactory;
  private final Logger                 log        = LoggerFactory.getLogger(getClass());
  private       String                 auditTable = "Audit";
  
  //
  // Member methods.
  //

  /**
   * Empty constructor.
   */
  public BasicDatabaseAuditListener(TechEmpowerApplication application,
    ConnectorFactory connectorFactory)
  {
    this.connFactory = connectorFactory;
  }
  
  /**
   * @return Returns the auditTable.
   */
  public String getAuditTable()
  {
    return this.auditTable;
  }
  
  /**
   * @param auditTable The auditTable to set.
   */
  public void setAuditTable(String auditTable)
  {
    this.auditTable = auditTable;
  }
  
  @Override
  public void auditSessionCommitted(AuditSession session)
  {
    // Does nothing.
  }

  @Override
  public void auditCommitted(AuditSession session, Audit audit)
  {
    if (audit.getAffected() == null
        || session.getCause() == null)
    {
      log.debug(
          "Unexpected input to auditCommitted.  " +
              "Session: {}, Audit: {}, Affected: {}, Cause: {}.",
          session, audit, audit.getAffected(), session.getCause());
      return;
    }

    StringList fields = new StringList();
    StringList values = new StringList();
    
    // Convert the timestamp.
    java.sql.Timestamp time = new java.sql.Timestamp(audit.getTimestamp().getTime());
    
    // Add the fields.
    fields.add("Type,AuditTime,Affected,AffectedTypeID,Cause,CauseTypeID,AttributeID,OriginalValue,NewValue");
    values.add("" + audit.getType());
    values.add("'" + time.toString() + "'");
    values.add("" + audit.getAffected().getId());
    values.add("" + audit.getAffected().getAuditableTypeID());
    values.add("" + session.getCause().getId());
    values.add("" + session.getCause().getAuditableTypeID());
    values.add("" + audit.getAttributeID());
    values.add((audit.getOriginalValue() == null) ? "NULL" : DatabaseHelper.prepare(audit.getOriginalValue()));
    values.add((audit.getNewValue() == null) ? "NULL" : DatabaseHelper.prepare(audit.getNewValue()));

    // Look at custom attributes.
    if (audit.hasCustom())
    {
      Map<String,String> customAttributes = audit.getCustomAttributes();
      for (Map.Entry<String, String> entry : customAttributes.entrySet())
      {
        fields.add(entry.getKey());
        values.add(DatabaseHelper.prepare(entry.getValue()));
      }
    }
    
    String query = "INSERT INTO " + auditTable
      + " (" + fields + ") VALUES (" + values + ");";

    try (ConnectionMonitor monitor = this.connFactory.getConnectionMonitor())
    {
      try (PreparedStatement statement = monitor.getConnection().prepareStatement(query))
      {
        statement.executeUpdate();
      }
    }
    catch (SQLException exc)
    {
      log.error("Unable to persist audit.", exc);
    }
  }

  @Override
  public void auditSessionCommitComplete(AuditSession session)
  {
    // Does nothing.
  }

  @Override
  public void auditsCleared(AuditSession session)
  {
    // Does nothing.
  }
  
  @Override
  public String getAuditListenerName()
  {
    return "Basic Database Audit Listener";
  }
  
}  // End BasicDatabaseAuditListener.
