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
import com.techempower.util.*;

/**
 * A Read-only version of an Audit to be used in scenarios where a series
 * of audits are being read in from a database to be rendered in a user
 * interface.  Do not use this class to create new audits.  See the Audit
 * and AuditManager classes for that.
 * 
 * @see Audit
 * @see AuditManager
 */
public class ReadonlyAudit 
  implements Identifiable
{
  
  //
  // Constants.
  //
  
  public static final Comparator<ReadonlyAudit> SORT_DATE_DESC = new Comparator<ReadonlyAudit>()
  {
    @Override
    public int compare(ReadonlyAudit arg0, ReadonlyAudit arg1)
    {
      return arg1.getAuditTime().compareTo(arg0.getAuditTime());
    }
  };
  
  //
  // Member variables.
  //
  
  private long   auditID;
  private int    type;
  private Date   auditTime;
  private long   affected;
  private int    affectedTypeID;
  private long   cause;
  private int    causeTypeID;
  private int    attributeID;
  private String originalValue;
  private String newValue;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public ReadonlyAudit()
  {
  }

  public ApiView apiView() { return new ApiView(); }
  public class ApiView {
    public final long id = getId();
    public final int type = getType();
    public final Date time = getAuditTime();
    public final long affected = getAffected();
    public final int affectedType = getAffectedTypeID();
    public final long cause = getCause();
    public final long causeType = getCauseTypeID();
    public final int attribute = getAttributeID();
    public final String from = getOriginalValue();
    public final String to = getNewValue();
  }
  
  @Override
  public long getId()
  {
    return auditID;
  }

  @Override
  public void setId(long identity)
  {
    this.auditID = identity;
  }
  
  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "ReadonlyAudit [" + DateHelper.STANDARD_SQL_FORMAT.format(getAuditTime())
      + " " + getAffected() + " (" + getAffectedTypeID() + ") by " 
      + getCause() + " (" + getCauseTypeID() + ") changed " + getAttributeID()
      + " to " + getNewValue() + "]";
  }

  /**
   * @return Returns the affected.
   */
  public long getAffected()
  {
    return affected;
  }

  /**
   * @param affected The affected to set.
   */
  public void setAffected(int affected)
  {
    this.affected = affected;
  }

  /**
   * @return Returns the affectedTypeID.
   */
  public int getAffectedTypeID()
  {
    return affectedTypeID;
  }

  /**
   * @param affectedTypeID The affectedTypeID to set.
   */
  public void setAffectedTypeID(int affectedTypeID)
  {
    this.affectedTypeID = affectedTypeID;
  }

  /**
   * @return Returns the attributeID.
   */
  public int getAttributeID()
  {
    return attributeID;
  }

  /**
   * @param attributeID The attributeID to set.
   */
  public void setAttributeID(int attributeID)
  {
    this.attributeID = attributeID;
  }

  /**
   * @return Returns the auditID.
   */
  public long getAuditID()
  {
    return auditID;
  }

  /**
   * @param auditID The auditID to set.
   */
  public void setAuditID(long auditID)
  {
    this.auditID = auditID;
  }

  /**
   * @return Returns the auditTime.
   */
  public Date getAuditTime()
  {
    return auditTime;
  }

  /**
   * @param auditTime The auditTime to set.
   */
  public void setAuditTime(Date auditTime)
  {
    this.auditTime = auditTime;
  }

  /**
   * @return Returns the cause.
   */
  public long getCause()
  {
    return cause;
  }

  /**
   * @param cause The cause to set.
   */
  public void setCause(int cause)
  {
    this.cause = cause;
  }

  /**
   * @return Returns the causeTypeID.
   */
  public int getCauseTypeID()
  {
    return causeTypeID;
  }

  /**
   * @param causeTypeID The causeTypeID to set.
   */
  public void setCauseTypeID(int causeTypeID)
  {
    this.causeTypeID = causeTypeID;
  }

  /**
   * @return Returns the newValue.
   */
  public String getNewValue()
  {
    return newValue;
  }

  /**
   * @param newValue The newValue to set.
   */
  public void setNewValue(String newValue)
  {
    this.newValue = newValue;
  }

  /**
   * @return Returns the originalValue.
   */
  public String getOriginalValue()
  {
    return originalValue;
  }

  /**
   * @param originalValue The originalValue to set.
   */
  public void setOriginalValue(String originalValue)
  {
    this.originalValue = originalValue;
  }

  /**
   * @return Returns the type.
   */
  public int getType()
  {
    return type;
  }

  /**
   * @param type The type to set.
   */
  public void setType(int type)
  {
    this.type = type;
  }
  
  /**
   * Gets the audit type name (Addition, Change, Deletion).
   */
  public String getAuditTypeName()
  {
    return Audit.getAuditTypeName(getType());
  }

}  // End ReadonlyAudit
