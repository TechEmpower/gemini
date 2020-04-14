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
package com.techempower.gemini.pyxis.listener;

import java.util.*;

import com.techempower.cache.*;
import com.techempower.data.*;
import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.password.*;
import com.techempower.util.*;

/**
 * Provides simple database-table auditing of successful logins and logouts.
 * The schema for the necessary SecurityAuditEvent table is:
 * 
 * <p><pre>{@code
 * CREATE TABLE SecurityAuditEvent(
 *   [id] [bigint] IDENTITY(1,1) NOT NULL,
 *   [userid] [bigint] NOT NULL,
 *   [time] [datetime] NOT NULL,
 *   [referenceLogin] [bigint] NOT NULL,
 *   [type] [int] NOT NULL,
 *   CONSTRAINT [PK_SecurityAuditEvent] PRIMARY KEY CLUSTERED
 * (
 *   [id] ASC
 * ) ON [PRIMARY]
 * }</pre>
 * 
 * The above is SQL Server syntax.
 */
public class   LoginAuditListener<C extends BasicContext>
    implements SecurityListener<C>
{
  
  //
  // Constants.
  //
  
  public static final String SESSION_REFERENCE_LOGIN = "-ReferenceLoginID";
  
  //
  // Variables.
  //
  
  private final EntityStore cache;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public <G extends GeminiApplication> LoginAuditListener(G application)
  {
    this.cache = application.getStore();
    this.cache.register(EntityGroup.of(SecurityAuditEvent.class));
  }

  /**
   * Write a login audit to the SecurityAuditEvent database table.  Although
   * this remains a best-effort process, we do run these updates on the 
   * current thread to help ensure proper persistence before proceeding. 
   */
  @Override
  public void loginSuccessful(C context, PyxisUser user)
  {
    final SecurityAuditEvent audit = new SecurityAuditEvent();
    
    audit.setTime(new Date())
         .setType(getLoginType(context))
         .setUserId(user.getId());
    this.cache.put(audit);
    
    // Store the newly-persisted audit for reference in a future audit for
    // a logout event.
    context.session().put(SESSION_REFERENCE_LOGIN, audit.getId());
  }

  /**
   * Write a logout audit to the SecurityAuditEvent database table.  Although
   * this remains a best-effort process, we do run these updates on the 
   * current thread to help ensure proper persistence before proceeding. 
   */
  @Override
  public void logoutSuccessful(C context, PyxisUser user)
  {
    final SecurityAuditEvent audit = new SecurityAuditEvent();
    final long referenceLogin = context.session().getLong(SESSION_REFERENCE_LOGIN, 0);
    context.session().remove(SESSION_REFERENCE_LOGIN);
    
    audit.setTime(new Date())
         .setType(getLogoutType(context))
         .setUserId(user.getId())
         .setReferenceLogin(referenceLogin);
    this.cache.put(audit);
  }

  @Override
  public void loginFailed(C context)
  {
    // Does nothing.
  }
  
  /**
   * Return the type of audit representing a login by looking at the 
   * provided context.  Overload this method to customize; by default this
   * returns 0 (SecurityAuditEvent.TYPE_LOGIN). 
   */
  protected int getLoginType(C context)
  {
    return SecurityAuditEvent.TYPE_LOGIN;
  }
  
  /**
   * Return the type of audit representing a logout by looking at the 
   * provided context.  Overload this method to customize; by default this
   * returns 1 (SecurityAuditEvent.TYPE_LOGOUT). 
   */
  protected int getLogoutType(C context)
  {
    return SecurityAuditEvent.TYPE_LOGOUT;
  }
  
  /**
   * A simple entity for auditing logins and logouts.
   */
  public static class SecurityAuditEvent
    extends BasicIdentifiable
  {
    public static final int TYPE_LOGIN  = 0;
    public static final int TYPE_LOGOUT = 1;
    
    private long userId;
    private Date time;
    private int  type;
    private long referenceLogin;
    
    public SecurityAuditEvent()
    {
      // Does nothing.
    }
    
    public SecurityAuditEvent setUserId(long userId)
    {
      this.userId = userId;
      
      return this;
    }
    
    public long getUserId()
    {
      return this.userId;
    }
    
    public SecurityAuditEvent setTime(Date time)
    {
      this.time = time;
      
      return this;
    }
    
    public Date getTime()
    {
      return this.time;
    }
    
    public SecurityAuditEvent setType(int type)
    {
      this.type = type;
      
      return this;
    }
    
    public int getType()
    {
      return this.type;
    }
    
    public SecurityAuditEvent setReferenceLogin(long id)
    {
      this.referenceLogin = id;
      
      return this;
    }
    
    public long getReferenceLogin()
    {
      return this.referenceLogin;
    }

  }

  @Override
  public void passwordChanged(PasswordProposal proposal)
  {
  }

}  // End LoginAuditListener.