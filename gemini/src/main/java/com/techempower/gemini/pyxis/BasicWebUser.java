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

package com.techempower.gemini.pyxis;

import java.security.*;
import java.util.*;

import com.techempower.gemini.session.Session;
import com.techempower.gemini.session.SessionListener;
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * BasicWebUser is a small extension on BasicUser.  It provides some typical
 * additional user attributes that can be expected for web application users.
 * Web-based applications are encouraged to use BasicWebUser as a starting
 * point for application-specific user classes.
 *
 * @see BasicUser
 */
public class BasicWebUser
  extends    BasicUser
  implements SessionListener
{
  
  //
  // Constants.
  //
  
  public static final IntRange EMAIL_LENGTH = NetworkHelper.EMAIL_ADDRESS_LENGTH;

  //
  // Member variables.
  //

  private String userEmail               = "";  // 254 chars
  private String userPasswordHint        = "";
  private String emailVerificationTicket = "";  // 15 chars by default.
  private Date   emailVerificationDate   = null;
  private String passwordResetTicket     = "";  // 15 chars by default.
  private Date   passwordResetExpiration = null;

  //
  // Member methods.
  //

  /**
   * Constructor.  Takes a reference to the security.
   */
  public BasicWebUser(BasicSecurity<? extends PyxisUser, ? extends PyxisUserGroup> security)
  {
    super(security);
  }

  /**
   * Attempts to "verify" the user's email address by comparing a provided
   * ticket versus the user's ticket. If the user's ticket is empty or if the
   * verification fails, nothing will happen and the method will return false.
   * If the verification succeeds, the current date will be set to the
   * verification date and true will be returned.
   * <p>
   * If a user has already been verified, and the ticket is still correct,
   * this will return true. This allows someone who knows the valid ticket to
   * re-submit the form without alarm but doesn't really reduce security.
   */
  public boolean verify(String ticket)
  {
    if (StringHelper.isNonEmpty(emailVerificationTicket))
    {
      if (MessageDigest.isEqual(emailVerificationTicket.getBytes(), 
          ticket.getBytes()))
      {
        if (emailVerificationDate == null)
        {
          emailVerificationDate = new Date();
        }
        
        return true;
      }
    }

    return false;  
  }
  
  /**
   * Determine whether the provided password reset ticket matches a valid
   * ticket on file for this user.  A ticket is only valid until its 
   * expiration date is reached.
   */
  public boolean isPasswordResetAuthorized(String resetTicket)
  {
    if (  (StringHelper.isNonEmpty(resetTicket))
       && (this.passwordResetExpiration != null)
       && (this.passwordResetExpiration.after(new Date()))
       && (getSecurity().getPasswordHasher().testPassword(resetTicket, passwordResetTicket))
       )
    {
      return true;
    }
    
    // Default behavior.
    return false;
  }
  
  /**
   * Generates a random email verification ticket and assigns the value
   * to the member variable.
   *   
   * @return The new email verification ticket.
   */
  public String generateNewEmailVerificationTicket()
  {
    setEmailVerificationTicket(
      StringHelper.secureRandomString.alphanumeric(
        getEmailVerificationTicketLength()));
    
    return getEmailVerificationTicket();
  }
  
  /**
   * Generates a new random password reset authorization ticket and expiration
   * date, assigning each to the appropriate member variables.  The calling
   * code should persist the user.
   * 
   * @param expirationDays The number of days in the future for this ticket
   *   to expire.
   *   
   * @return The new password reset ticket.
   */
  public String generateNewPasswordResetTicket(int expirationDays)
  {
    // Generate the reset ticket, which will be returned from this method and
    // sent to the user, but forgotten locally.  Only a hash of the ticket will
    // be retained.
    final String ticket = StringHelper.secureRandomString.alphanumeric(
        getEmailVerificationTicketLength());
    final String hashedTicket = getSecurity().getPasswordHasher()
        .encryptPassword(ticket);

    // Set the reset ticket.
    setPasswordResetTicket(hashedTicket);

    // Set the expiration date.
    final Calendar cal = DateHelper.getCalendarInstance();
    cal.add(Calendar.DAY_OF_YEAR, expirationDays);
    setPasswordResetExpiration(cal.getTime());
    
    return ticket;
  }
  
  /**
   * Gets the number of characters to use for email verification tickets.
   */
  public int getEmailVerificationTicketLength()
  {
    return 15;
  }

  @Override
  public void sessionBound(Session session)
  {
    // Does nothing when bound to a session.
  }

  @Override
  public void sessionUnbound(Session session)
  {
    // Do nothing if we don't have a security reference.
    if (getSecurity() != null)
    {
      // When unbound, check to see if the session is being closed normally.
      // If not, notify the security that the session is closing as a result
      // of a timeout and the user should be logged out.

      
      try
      {
        Object close = session.getAttribute(PyxisConstants.SESSION_CLOSE_INDICATOR);
  
        // No indicator; this session is expiring.
        if (close == null)
        {
          getSecurity().logout(this);
        }
      }
      catch (IllegalStateException isexc)
      {
        // Do nothing.  The session may already be dead or the application
        // is shutting down.
      }
    }
  }

  //
  // Mutators.
  //

  /**
   * Sets the user's username.
   */
  public BasicWebUser setUserEmail(String email)
  {
    if (email == null)
    {
      this.userEmail = null;
    }
    else
    {
      this.userEmail = getSecurity().sanitizeEmailAddress(email);
    }
    return this;
  }

  /**
   * Sets the user's password hint.
   */
  public void setUserPasswordHint(String passwordHint)
  {
    this.userPasswordHint = passwordHint;
  }

  /**
   * Gets the password-reset authorization ticket.
   */
  public String getPasswordResetTicket()
  {
    return this.passwordResetTicket;
  }

  /**
   * Sets the password-reset authorization ticket.  This ticket must either be
   * empty, or if non-empty, it must be encrypted with BCrypt.
   */
  public void setPasswordResetTicket(String passwordResetTicket)
  {
    this.passwordResetTicket = passwordResetTicket;
  }

  /**
   * Gets the expiration date on the password-reset authorization ticket.
   */
  public Date getPasswordResetExpiration()
  {
    return DateHelper.copy(this.passwordResetExpiration);
  }

  /**
   * Sets the expiration date on the password-reset authorization ticket.
   */
  public void setPasswordResetExpiration(Date passwordResetExpiration)
  {
    this.passwordResetExpiration = DateHelper.copy(passwordResetExpiration);
  }

  /**
   * @param emailVerificationTicket the emailVerificationTicket to set
   */
  public void setEmailVerificationTicket(String emailVerificationTicket)
  {
    this.emailVerificationTicket = emailVerificationTicket;
  }

  /**
   * @param emailVerificationDate the emailVerificationDate to set
   */
  public void setEmailVerificationDate(Date emailVerificationDate)
  {
    this.emailVerificationDate = DateHelper.copy(emailVerificationDate);
  }
  
  /**
   * @return the emailVerificationTicket
   */
  public String getEmailVerificationTicket()
  {
    return this.emailVerificationTicket;
  }

  /**
   * @return the emailVerificationDate
   */
  public Date getEmailVerificationDate()
  {
    return DateHelper.copy(this.emailVerificationDate);
  }

  /**
   * Gets the user's username.
   */
  public String getUserEmail()
  {
    return this.userEmail;
  }

  /**
   * Gets the user's password hint.
   */
  public String getUserPasswordHint()
  {
    return this.userPasswordHint;
  }

}   // End BasicWebUser
