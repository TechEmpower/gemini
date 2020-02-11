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

import com.techempower.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuditManager is the primary means of interaction with the com.techempower
 * auditing functionality.  AuditManager is responsible for constructing and
 * returning AuditSession references for use within an application.  The
 * typical usage is to attach an AuditSession to the current thread by
 * calling newSession, add audits through the audit method, and then 
 * ultimately call commit to save audits.
 *   <p>
 * When using within a Gemini application, generally a getAuditManager method
 * is added to the GeminiApplication subclass, and the following code is added
 * to the Dispatcher subclass:
 * <p><pre>{@code
 * public boolean dispatch(Context context)
 * {
 *   try
 *   {
 *     // Get the current user and provide that as the cause.
 *     application.getAuditManager().newSession(getUser(context));
 *     // Dispatch as normal.
 *     return super.dispatch(context);
 *   }
 *   finally
 *   {
 *     // At the end of the request, commit the audits, if any were made.
 *     AuditManager.commit();
 *   }
 * }
 * }</pre>
 *   <p>
 * An implementation of getUser that returns a user that implements Auditable
 * will also be necessary.
 */
public class AuditManager
{
  
  //
  // Constants.
  //
  
  public static final String COMPONENT_CODE = "audt";
  public static final int    DEFAULT_MAXIMUM_VALUE_LENGTH = 100;

  //
  // Static variables.
  //
  
  private static final ThreadLocal<AuditSession> SESSION_BY_THREAD = new ThreadLocal<>();
  
  //
  // Member variables.
  //
    
  private final Logger log                = LoggerFactory.getLogger(COMPONENT_CODE);
  private       int    maximumValueLength = DEFAULT_MAXIMUM_VALUE_LENGTH;
  private final List<AuditListener> listeners;
  
  //
  // Member methods.
  //
  
  /**
   * Construct a new AuditManager. 
   */
  public AuditManager(TechEmpowerApplication application)
  {
    listeners = new ArrayList<>(2);
  }
  
  /**
   * @return Returns the maximumValueLength.
   */
  public int getMaximumValueLength()
  {
    return maximumValueLength;
  }

  /**
   * @param maximumValueLength The maximumValueLength to set.
   */
  public void setMaximumValueLength(int maximumValueLength)
  {
    this.maximumValueLength = maximumValueLength;
  }

  /**
   * Gets the log reference.
   */
  public Logger getLog()
  {
    return log;
  }
  
  /**
   * Constructs a new AuditSession, but does not attach it to the current
   * thread.  Use this method if your usage pattern will rely on passing
   * the AuditSession through the call stack so that it is available to
   * all code that needs to make audits.
   *   <p>
   * See also newSession, which automatically attaches the session to the
   * current thread, allowing future calls to getSession to return the same
   * session during a thread's execution.
   */
  public AuditSession constructSession(Auditable cause)
  {
    return new AuditSession(this, cause);
  }
  
  /**
   * Gets the current session (uses ThreadLocal).
   */
  public static AuditSession getSession()
  {
    return SESSION_BY_THREAD.get();
  }
  
  /**
   * Attaches a new AuditSession to the current thread.
   */
  public void newSession(Auditable cause)
  {
    SESSION_BY_THREAD.set(constructSession(cause));
  }
  
  /**
   * Gets the count of the current thread's audits.
   */
  public static int size()
  {
    AuditSession session = getSession();
    if (session != null)
    {
      return session.size();
    }
    
    return 0;
  }
  
  /**
   * Returns true if there is a current session and it has audits accumulated.
   * Returns false if the audit list is empty or if there is no current
   * session.
   */
  public static boolean hasAudits()
  {
    return (size() > 0);
  }
  
  /**
   * Commits the current thread's session.  Returns true if the session had
   * not already been committed and false if the session had already been
   * committed (and, therefore, the requested commit was not executed).
   * Note that false also may be returned if there is no active session.
   */
  public static void commit()
  {
    AuditSession session = getSession();
    if (session != null)
    {
      session.commit();
    }
  }
  
  /**
   * Clears the current thread's session.
   */
  public static void clear()
  {
    AuditSession session = getSession();
    if (session != null)
    {
      session.clear();
    }
  }
  
  /**
   * A pass-through to the current AuditSession's add method.  This uses
   * getSession to get the session attached to the current Thread.
   */
  public static void audit(int type, Auditable affected, int attributeID,
    String originalValue, String newValue)
  {
    AuditSession session = getSession();
    if (session != null)
    {
      session.add(type, affected, attributeID, originalValue, newValue);
    }
  }

  /**
   * A pass-through to the current AuditSession's add method.  This uses
   * getSession to get the session attached to the current Thread.
   */
  public static void audit(int type, Auditable affected, int attributeID,
    Date originalValue, Date newValue)
  {
    AuditSession session = getSession();
    if (session != null)
    {
      session.add(type, affected, attributeID, originalValue, newValue);
    }
  }

  /**
   * A pass-through to the current AuditSession's add method.  This uses
   * getSession to get the session attached to the current Thread.
   */
  public static void audit(int type, Auditable affected, int attributeID,
    long originalValue, long newValue)
  {
    AuditSession session = getSession();
    if (session != null)
    {
      session.add(type, affected, attributeID, originalValue, newValue);
    }
  }

  /**
   * A pass-through to the current AuditSession's add method.  This uses
   * getSession to get the session attached to the current Thread.
   */
  public static void audit(int type, Auditable affected, int attributeID,
    float originalValue, float newValue)
  {
    AuditSession session = getSession();
    if (session != null)
    {
      session.add(type, affected, attributeID, originalValue, newValue);
    }
  }

  /**
   * A pass-through to the current AuditSession's add method.  This uses
   * getSession to get the session attached to the current Thread.
   */
  public static void audit(int type, Auditable affected, int attributeID,
    boolean originalValue, boolean newValue)
  {
    AuditSession session = getSession();
    if (session != null)
    {
      session.add(type, affected, attributeID, originalValue, newValue);
    }
  }

  /**
   * A pass-through to the current AuditSession's add method.  This uses
   * getSession to get the session attached to the current Thread.
   */
  public static void audit(int type, Auditable affected, int attributeID,
    Object originalValue, Object newValue)
  {
    AuditSession session = getSession();
    if (session != null)
    {
      session.add(type, affected, attributeID, originalValue, newValue);
    }
  }  

  /**
   * Register a new listener.  This method is not threadsafe.
   */
  public void addListener(AuditListener listener)
  {
    listeners.add(listener);
    log.info("Listener registered: {}", listener.getAuditListenerName());
  }
  
  /**
   * De-register a listener.  This method is not threadsafe.
   */
  public void removeListener(AuditListener listener)
  {
    listeners.remove(listener);
    log.info("Listener de-registered: {}", listener.getAuditListenerName());
  }
  
  /**
   * Commit a session of audits.
   */
  protected void commitSession(AuditSession session)
  {
    synchronized (session)
    {
      if (!listeners.isEmpty())
      {
        // Notify the listeners of a commit.
        Iterator<AuditListener> listen = listeners.iterator();
        AuditListener listener;
        while (listen.hasNext())
        {
          listener = listen.next();
          listener.auditSessionCommitted(session);
        }
        
        // Notify of each audit.
        Audit audit;
        for (int i = 0; i < session.size(); i++)
        {
          audit = session.get(i);
          listen = listeners.iterator();
          while (listen.hasNext())
          {
            listener = listen.next();
            listener.auditCommitted(session, audit);
          }
        }
        
        // Notify of completion of audit session.
        listen = listeners.iterator();
        while (listen.hasNext())
        {
          listener = listen.next();
          listener.auditSessionCommitComplete(session);
        }
      }
      // listeners is 0 in size?  That's no good.
      else
      {
        log.info("No AuditListeners are defined!  Audit information will not be recorded.");
      }
    }
  }

  
}  // End AuditManager.
