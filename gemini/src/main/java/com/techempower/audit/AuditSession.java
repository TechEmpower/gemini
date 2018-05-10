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

import java.util.*;

import com.techempower.helper.*;

/**
 * An audit session is constructed when user activity is occurring, audits 
 * are attached to the session, and then the audits are ultimately committed
 * or discarded (usually by just throwing away the AuditSession reference).
 *   <p>
 * Typical usage is abstracted through the use of AuditManager.newSession()
 * and AuditManager.audit() methods, which use ThreadLocal objects to attach
 * a session to a current thread, removing the need to pass the AuditSession
 * as a parameter through the call stack.
 */
public class AuditSession
{
  
  public static final int INITIAL_CAPACITY = 4;
  
  //
  // Member variables.
  //
  
  private final AuditManager     manager;
  private       List<Audit>      audits;
  private       Auditable        cause;
  
  //
  // Member methods.
  //

  /**
   * Constructs a new AuditSession. 
   */
  protected AuditSession(AuditManager manager, Auditable cause)
  {
    this.manager = manager;
    this.cause   = cause;
  }
  
  /**
   * Adds a new Audit if the old and new values differ.  This is a convenience
   * method that constructs an Audit.
   */
  public void add(int type, Auditable affected,
    int attributeID, String originalValue, String newValue)
  {
    // If the original value differs from the new value, proceed.
    if ( (affected.isInitialized())
      && (!Objects.equals(originalValue, newValue))
      )
    {
      Audit audit = new Audit(this, type, affected, attributeID, 
        originalValue, newValue);
      add(audit);
    }
  }
  
  /**
   * Adds a new Audit if the old and new values differ.  This is a convenience
   * method that constructs an Audit.
   */
  public void add(int type, Auditable affected,
    int attributeID, Date originalValue, Date newValue)
  {
    // If the original value differs from the new value, proceed.
    if ( (affected.isInitialized())
      && (!Objects.equals(originalValue, newValue))
      )
    {
      Audit audit = new Audit(this, type, affected, attributeID, 
        (originalValue == null) ? null : DateHelper.STANDARD_SQL_FORMAT.format(originalValue), 
        (newValue == null) ? null : DateHelper.STANDARD_SQL_FORMAT.format(newValue));
      add(audit);
    }
  }
  
  /**
   * Adds a new Audit if the old and new values differ.  This is a convenience
   * method that constructs an Audit.
   */
  public void add(int type, Auditable affected,
    int attributeID, Object originalValue, Object newValue)
  {
    // If the original value differs from the new value, proceed.
    if ( (affected.isInitialized())
      && (!Objects.equals(originalValue, newValue))
      )
    {
      Audit audit = new Audit(this, type, affected, attributeID, 
        (originalValue == null) ? null : originalValue.toString(), 
        (newValue == null) ? null : newValue.toString());
      add(audit);
    }
  }
  
  /**
   * Adds a new Audit if the old and new values differ.  This is a convenience
   * method that constructs an Audit.
   */
  public void add(int type, Auditable affected,
    int attributeID, long originalValue, long newValue)
  {
    if ( (affected.isInitialized())
      && (originalValue != newValue)
      )
    {
      Audit audit = new Audit(this, type, affected, attributeID,
        "" + originalValue, "" + newValue);
      add(audit);
    }
  }
  
  /**
   * Adds a new Audit if the old and new values differ.  This is a convenience
   * method that constructs an Audit.
   */
  public void add(int type, Auditable affected,
    int attributeID, float originalValue, float newValue)
  {
    if ( (affected.isInitialized())
      && (originalValue != newValue)
      )
    {
      Audit audit = new Audit(this, type, affected, attributeID,
        "" + originalValue, "" + newValue);
      add(audit);
    }
  }
  
  /**
   * Adds a new Audit if the old and new values differ.  This is a convenience
   * method that constructs an Audit.
   */
  public void add(int type, Auditable affected,
    int attributeID, boolean originalValue, boolean newValue)
  {
    if ( (affected.isInitialized())
      && (originalValue != newValue)
      )
    {
      Audit audit = new Audit(this, type, affected, attributeID,
        (originalValue ? "True" : "False"), (newValue ? "True" : "False"));
      add(audit);
    }
  }
  
  /**
   * Adds a new Audit to the session.
   */
  public void add(Audit audit)
  {
    if (this.audits == null)
    {
      this.audits = new ArrayList<>(INITIAL_CAPACITY);
    }
    this.audits.add(audit);
  }
  
  /**
   * Gets an audit.  Returns null if no such audit.
   */
  public Audit get(int index)
  {
    if ( (this.audits != null)
      && (index >= 0)
      && (this.audits.size() >= index)
      )
    {
      return this.audits.get(index);
    }
    else
    {
      return null;
    }
  }
  
  /**
   * Gets the size of this audit session (in number of audits).
   */
  public int size()
  {
    if (this.audits != null)
    {
      return this.audits.size();
    }
    else
    {
      return 0;
    }
  }
  
  /**
   * Removes all audits and resets the committed flag.
   */
  public void clear()
  {
    if (this.audits != null)
    {
      this.audits.clear();
    }
  }
  
  /**
   * Commits the audits in this session if any exist.  Once committed, the
   * session's list of audits is cleared. 
   */
  public void commit()
  {
    // Commit the audits.
    if (CollectionHelper.isNonEmpty(this.audits))
    {
      this.manager.commitSession(this);
      
      // Clear the audits.
      clear();
    }
  }
  
  /**
   * Gets a reference to the AuditManager.
   */
  public AuditManager getManager()
  {
    return this.manager;
  }
  
  /**
   * @return Returns the cause.
   */
  public Auditable getCause()
  {
    return this.cause;
  }
  
  /**
   * @param cause The cause to set.
   */
  public void setCause(Auditable cause)
  {
    this.cause = cause;
  }
  
  /**
   * Renders to a String.
   */
  @Override
  public String toString()
  {
    return "AuditSession [" + hashCode() + "; " + size() + " audits]";
  }
  
}  // End AuditSession.
