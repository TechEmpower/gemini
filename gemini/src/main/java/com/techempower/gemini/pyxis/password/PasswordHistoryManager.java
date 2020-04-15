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
package com.techempower.gemini.pyxis.password;

import com.techempower.cache.*;
import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.listener.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retains a configurable-length history of passwords used by a user.  The 
 * passwords are retained in a BCrypt-hashed state.  To to use, attach this as
 * both a SecurityListener (so as to be informed of password changes)
 * and as a PasswordRequirement (so as to disallow reuse of previously-used
 * passwords) on the application's Security.
 *   <p>
 * Configuration options:
 *   </p><ul>
 * <li>PasswordHistory.MaximumHistorySize - How many previous passwords to
 *     retain.  Permitted range is 1 to 100.</li>
 * <li>PasswordHistory.CaptureNonPrincipalChanges - Capture all password
 *     changes, including those executed by an agent other than the principal
 *     user themselves, such as by a system administrator.</li>
 *   </ul>
 */
public class PasswordHistoryManager
  implements SecurityListener<Context>,
             PasswordRequirement,
             Configurable
{
  
  public static final int    HARD_MAXIMUM_HISTORY_SIZE    = 100;
  public static final int    DEFAULT_MAXIMUM_HISTORY_SIZE = 5;
  
  private final Logger        log = LoggerFactory.getLogger(getClass());
  private final PyxisSecurity security;
  private final EntityStore   store;
  
  private int     maximumHistorySize         = DEFAULT_MAXIMUM_HISTORY_SIZE;
  private boolean captureNonPrincipalChanges = false;
  
  /**
   * Constructor.
   */
  public PasswordHistoryManager(GeminiApplication application, PyxisSecurity security)
  {
    this.security = security;
    this.store    = application.getStore();

    // Add ourselves as a security listener.
    security.addListener(this);
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    final EnhancedProperties.Focus focus = props.focus("PasswordHistory.");
    
    maximumHistorySize = focus.getInt("MaximumHistorySize", 
        DEFAULT_MAXIMUM_HISTORY_SIZE, 1, HARD_MAXIMUM_HISTORY_SIZE);
    captureNonPrincipalChanges = focus.getBoolean("CaptureNonPrincipalChanges",
        false);

    // TODO: Build a better way to percolate up configuration errors.
    if (!security.getPasswordHasher().isSecure()) 
    {
      throw new IllegalArgumentException(
          "PasswordHistoryManager cannot be used with an insecure password hasher. "
          + "Provided: " + security.getPasswordHasher().getName() + ".");
    }
  }

  /**
   * Capture password changes to a history for the user.
   */
  @Override
  public void passwordChanged(PasswordProposal proposal)
  {
    final PyxisUser cause = (proposal.context != null)
        ? security.getUser(proposal.context)
        : null;
    if (  (captureNonPrincipalChanges)
       || (proposal.user == cause)
       )
    {
      // Update the password history for the user.
      PasswordHistory history = store.get(PasswordHistory.class, proposal.user.getId());
      if (history == null)
      {
        history = new PasswordHistory();
        history.setId(proposal.user.getId());
      }
      
      log.info("Recording new password hash for {}.",
          proposal.user.getUserUsername());
      history.addHash(proposal.hashedPassword, maximumHistorySize);
      store.put(history);
    }
    else
    {
      log.info("Not recording historical hash for {} because password " +
          "change was made by non-principal.",
          proposal.user.getUserUsername());
    }
  }

  /**
   * Validate proposed new passwords for a user by rejecting those that are
   * still in the history.
   */
  @Override
  public String validate(PasswordProposal proposal)
  {
    if (proposal.user != null)
    {
      final PasswordHistory history = store.get(PasswordHistory.class, 
          proposal.user.getId());
      
      if (history != null)
      {
        final String[] hashes = history.getHashesArray();
        log.info("Checking proposed new password for {} against {} previous hashes.",
            proposal.username, hashes.length);
  
        for (String hash : hashes)
        {
          if (security.getPasswordHasher().testPassword(proposal.password, hash))
          {
            log.info("Proposed new password for {} matches a previous " +
                "hash; it does not pass validation.", proposal.username);
            return "The provided password was used previously. Please create a new password.";
          }
        }
        
        log.info("Proposed new password for {} is not in recent " +
            "history; it passes validation.", proposal.username);
      }
    }
    
    return null;
  }

  @Override
  public void loginSuccessful(Context context, PyxisUser user)
  {
    // We don't care about this here.
  }

  @Override
  public void logoutSuccessful(Context context, PyxisUser user)
  {
    // We don't care about this here.
  }

  @Override
  public void loginFailed(Context context)
  {
    // We don't care about this here.
  }

}
