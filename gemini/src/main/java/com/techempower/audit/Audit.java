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
 * An individual Audit event.
 */
public class Audit
{
  
  //
  // Audit type constants.
  //
  
  public static final int NONE                   = 0;
  public static final int ADDITION               = 1;
  public static final int CHANGE                 = 2;
  public static final int DELETION               = 3;

  private static final String[] TYPE_NAMES = new String[]
    { "None", "Addition", "Change", "Deletion" }; 
  
  //
  // Other constants.
  //
  
  public static final long AUDIT_ID_NOT_SET       = 0L;
  
  //
  // Member variables.
  //
  
  private long               auditID;
  private int                type;
  private Date               timestamp;
  private Auditable          affected;
  private int                attributeID;
  private String             originalValue;
  private String             newValue;
  private Map<String,String> customAttributes;
  private AuditSession       session;
  
  //
  // Member methods.
  //
  
  /**
   * Construct a new Audit.
   */
  public Audit(AuditSession session, int type, Auditable affected, 
    int attributeID, String originalValue, String newValue)
  {
    this.auditID       = AUDIT_ID_NOT_SET;
    this.type          = NumberHelper.boundInteger(type, 0, TYPE_NAMES.length - 1);
    this.session       = session;
    this.affected      = affected;
    this.attributeID   = attributeID;
    this.timestamp     = new Date();
    setOriginalValue(originalValue);
    setNewValue(newValue);
  }
  
  /**
   * @return Returns the session.
   */
  public AuditSession getSession()
  {
    return session;
  }
  
  /**
   * @param session The session to set.
   */
  public void setSession(AuditSession session)
  {
    this.session = session;
  }
  
  /**
   * Gets the audit type name (Addition, Change, Deletion).
   */
  public String getAuditTypeName()
  {
    return getAuditTypeName(getType());
  }
  
  /**
   * Gets the audit type name for a specified type (Addition, Change, 
   * Deletion), returning None if the provided type is out of range.
   */
  public static String getAuditTypeName(int type)
  {
    if (  (type >= 0)
       && (type < TYPE_NAMES.length)
       )
    {
      return TYPE_NAMES[type];
    }
    else
    {
      return TYPE_NAMES[0];
    }
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
    this.type = NumberHelper.boundInteger(type, 0, TYPE_NAMES.length - 1);
  }
  
  /**
   * @return Returns the affected Auditable object.
   */
  public Auditable getAffected()
  {
    return affected;
  }
  
  /**
   * @param affected The affected Auditable object.
   */
  public void setAffected(Auditable affected)
  {
    this.affected = affected;
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
   * @return Returns the newValue.
   */
  public String getNewValue() 
  {
    return newValue;
  }
  
  /**
   * @param value The newValue to set.
   */
  public void setNewValue(String value) 
  {
    // Truncate value if necessary.
    int maxLength = session.getManager().getMaximumValueLength();
    if ( (value != null)
      && (value.length() > maxLength)
      )
    {
      newValue = value.substring(0, maxLength);
    }
    else
    {
      newValue = value;
    }
  }
  
  /**
   * @return Returns the originalValue.
   */
  public String getOriginalValue() 
  {
    return originalValue;
  }
  
  /**
   * @param value The originalValue to set.
   */
  public void setOriginalValue(String value)
  {
    // Truncate value if necessary.
    int maxLength = session.getManager().getMaximumValueLength();
    if ( (value != null)
      && (value.length() > maxLength)
      )
    {
      originalValue = value.substring(0, maxLength);
    }
    else
    {
      originalValue = value;
    }
  }
  
  /**
   * @return Returns the timestamp.
   */
  public Date getTimestamp()
  {
    return timestamp;
  }
  
  /**
   * @param timestamp The timestamp to set.
   */
  public void setTimestamp(Date timestamp) 
  {
    this.timestamp = timestamp;
  }
  
  /**
   * Adds a custom attribute.
   */
  public void addCustom(String name, String value)
  {
    // Lazy initializer.
    if (customAttributes == null)
    {
      customAttributes = new HashMap<>();
    }
    
    // Truncate value if necessary.
    int maxLength = session.getManager().getMaximumValueLength();
    if (value.length() > maxLength)
    {
      customAttributes.put(name, value.substring(0, maxLength));
    }
    else
    {
      customAttributes.put(name, value);
    }
  }
  
  /**
   * Determine if this Audit has custom attributes.
   */
  public boolean hasCustom()
  {
    return !CollectionHelper.isEmpty(customAttributes);
  }
  
  /**
   * Gets the custom attributes.
   */
  public Map<String,String> getCustomAttributes()
  {
    return customAttributes;
  }
  
  /**
   * Removes a custom attribute by name.
   */
  public void removeCustom(String name)
  {
    // If not initialized, ignore this request.
    if (customAttributes != null)
    {
      customAttributes.remove(name);
    }
  }
  
}  // End Audit.
