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

package com.techempower.gemini.pyxis.handler;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.context.*;
import com.techempower.gemini.email.*;
import com.techempower.gemini.email.outbound.*;
import com.techempower.gemini.input.*;
import com.techempower.gemini.input.processor.*;
import com.techempower.gemini.input.validator.*;
import com.techempower.gemini.path.*;
import com.techempower.gemini.path.annotation.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.password.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * Provides conventional password-reset functionality for Gemini/Pyxis web
 * applications.  The basic user experience flow is as follows:
 *   <ol>
 * <li>STEP 0: User navigates a link labeled "I've forgotten my password."
 * <li>STEP 1: The resulting page prompts the user for their username.
 * <li>STEP 2: Assuming a matching user is found, a temporary authorization 
 *     ticket is generated and e-mailed to the user's e-mail account on file;
 *     a confirmation page is rendered.
 * <li>STEP 3: In the generated e-mail, a URL will be rendered (optionally, 
 *     the ticket alone can be rendered).
 * <li>STEP 4: When the user navigates to that URL, a second page will allow 
 *     the user to provide a new password.
 * <li>STEP 5: After providing a new password, the user is directed to a final 
 *     "complete" page.  The user is not automatically logged in.
 * </ol>
 * The e-mail template ID is E-PasswordResetAuthorization
 *   <p>
 * The following mustache templates are used:
 *   <ol>
 * <li>auth/password-reset-request.mustache - For STEP 1 above.
 * <li>auth/password-reset-request-confirmed.mustache - For STEP 2 above. 
 * <li>auth/password-reset-process.mustache - For STEP 4 above.
 * <li>auth/password-reset-complete.mustache - For STEP 5 above.
 *   </ol>
 * This functionality requires that your User class is based on BasicWebUser
 * and includes the following fields:
 *   <ol>
 * <li>PasswordResetTicket - 15 alphanumeric characters
 * <li>PasswordResetExpiration - Date
 *   </ol>
 * Configuration properties:
 * <ul>
 *   <li>PasswordReset.FromAddress - the email address from which to send
 *       the authorization ticket email.</li>
 *   <li>PasswordReset.ExpirationDays - How many days should a new password-
 *       reset ticket remain valid?  Default is 5.</li>
 *   <li>PasswordReset.TemplateRelativePath - What is the relative path to the 
 *       Mustache templates?  By default the relative path is "auth/".</li>
 * </ul>
 */
public class PasswordResetHandler<C extends BasicContext>
     extends MethodSegmentHandler<C>
  implements Configurable
{

  //
  // Constants.
  //
  
  public static final String COMPONENT_CODE           = "hPsR";
  public static final String DEFAULT_TEMPLATE_PATH    = "/auth/";
  public static final int    DEFAULT_EXPIRATION_DAYS  = 5;
 
  public static final String TEMPLATE_RESET_REQUEST   = "password-reset-request";
  public static final String TEMPLATE_RESET_CONFIRMED = "password-reset-request-confirmed";
  public static final String TEMPLATE_RESET           = "password-reset-process";
  public static final String TEMPLATE_RESET_COMPLETE  = "password-reset-complete";
  public static final String TEMPLATE_RESET_NOT_FOUND = "password-reset-not-found";

  public static final String EMAIL_TEMPLATE_NAME      = "E-PasswordResetAuthorization"; 
  
  //
  // Member variables.
  //
  
  private final PyxisSecurity     security;
  
  private String                  fromAddress = "";
  private int                     expirationDays = DEFAULT_EXPIRATION_DAYS;
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public PasswordResetHandler(GeminiApplication application)
  {
    super(application, COMPONENT_CODE);
    
    this.security = application.getSecurity();

    // Ask the EmailTemplater to load our template when it configures.
    EmailTemplater templater = application.getEmailTemplater();
    templater.addTemplateToLoad(getEmailTemplateName());
    
    application.getConfigurator().addConfigurable(this);
  }
  
  @Override
  public void configure(EnhancedProperties props)
  {
    EnhancedProperties.Focus focus = props.focus("PasswordReset.");
    
    this.fromAddress = focus.get("FromAddress", this.fromAddress);
    this.expirationDays = focus.getInt("ExpirationDays", DEFAULT_EXPIRATION_DAYS);
    setBaseTemplatePath(focus.get("TemplateRelativePath", DEFAULT_TEMPLATE_PATH));
  }
  
  /**
   * Gets the reset-request validation rules.
   */
  protected ValidatorSet getResetRequestValidatorSet()
  {
    return standardResetRequestValidatorSet;
  }
  
  /**
   * Hard-coded default reset-request validation rules.
   */
  private final ValidatorSet standardResetRequestValidatorSet = new ValidatorSet(
      new Lowercase("un"),
      new LengthValidator("un", BasicUser.USERNAME_LENGTH, false)
          .message("Please provide a valid username.")
      );
  
  /**
   * Gets the password-reset validation rules.
   */
  protected ValidatorSet getPasswordResetValidatorSet()
  {
    return new ValidatorSet(
        new RequiredValidator("newpw")
            .message("A new password is required."),
        new RepeatValidator("newpw", "confirmpw")
            .message("New password and confirmation do not match."),
        new ShortCircuitValidator.Wrapper(
            new PasswordComplexityValidator("newpw", security))
        );
  }

  /**
   * STEP 1: Handles a request for a new password reset ticket to be generated
   * and sent to the user's email address.  This will display a confirmation 
   * page to the user.  Templates used:
   * <ul>
   *   <li>auth/password-reset-request.mustache: In this page, a form will 
   *       prompt the user to identify themselves via username.  The form 
   *       elements on the page are "un" (username field) and "submit" (submit
   *       button).</li>
   *   <li>auth/password-reset-request-confirmed.mustache: Announces that an
   *       e-mail has been sent out.</li>
   * </ul>
   */
  @PathDefault
  @Get
  public boolean getResetRequest(BasicContext context)
  {
    template(TEMPLATE_RESET_REQUEST);
    return render();
  }
  
  /**
   * Handles the form submission from STEP 1.
   */
  @PathDefault
  @Post
  public boolean resetRequest(BasicContext context)
  {
    template(TEMPLATE_RESET_REQUEST);
    // Check for submission.
    final Input input = getResetRequestValidatorSet().process(context);
    if (input.passed())
    {
      // Send the e-mail and notify the user that we've done so.
      final Query values = input.values();
      final String username = values.get("un");
      final BasicWebUser user = (BasicWebUser)security.findUser(username);
      
      // Did we find the user?
      if (user != null)
      {
        // Update the user.
        final String ticket = user.generateNewPasswordResetTicket(this.expirationDays);
        saveUser(user);
        
        // Send the email.
        sendAuthorizationEmail(context, user, ticket);
        
        return handleResetRequestSuccess();
      }
      else
      {
        return handleResetRequestInvalid();
      }
    }
    
    return validationFailure(input);
  }
  
  /**
   * Handle a successful reset request.
   */
  protected boolean handleResetRequestSuccess()
  {
    template(TEMPLATE_RESET_CONFIRMED);
    delivery().status("ticket-mailed");
    return message("A password reset ticket has been e-mailed.");
  }
  
  /**
   * Handle an invalid username reset request.
   */
  protected boolean handleResetRequestInvalid()
  {
    delivery().message("User not found.");
    return badRequest("invalid");
  }
  
  /**
   * Sends the authorization e-mail to the user.  Macros:
   * <ul>
   *   <li>$UN = Username</li>
   *   <li>$FN = First name</li>
   *   <li>$LN = Last name</li>
   *   <li>$EM = Email address</li>
   *   <li>$VT = Authorization ticket</li>
   *   <li>$ED = Number of days before the ticket expires</li>
   *   <li>$URL = Full authorized URL</li>
   * </ul>
   * The default email template name is "E-PasswordResetAuthorization".
   */
  protected void sendAuthorizationEmail(BasicContext context, BasicWebUser user, String ticket)
  {
    final EmailTemplater templater = app().getEmailTemplater();
    final Map<String,Object> macros = new HashMap<>(10);
    macros.put("$UN", user.getUserUsername());
    macros.put("$UUN", NetworkHelper.encodeUrl(user.getUserUsername()));
    macros.put("$FN", user.getUserFirstname());
    macros.put("$LN", user.getUserLastname());
    macros.put("$EM", user.getUserEmail());
    macros.put("$VT", ticket);
    macros.put("$SD", app().getInfrastructure().getStandardDomain());
    macros.put("$SSD", app().getInfrastructure().getSecureDomain());
    macros.put("$ED", "" + this.expirationDays);

    // Construct the URL.
    macros.put("$URL", getAuthorizationUrl(user, ticket));

    // Get a suitable author address.
    final String authorAddress = this.getFromAddress();
    
    // Send the mail.
    final EmailPackage email = templater.process(getEmailTemplateName(), 
        macros, authorAddress, user.getUserEmail());
    
    if (email != null)
    {
      app().getEmailServicer().sendMail(email);
    }
    else
    {
      l("Email could not be fetched from EmailTemplater.");
    }
  }
  
  /**
   * Returns a suitable email address to use in an email's "from" field.
   * @return the fromAddress
   */
  protected String getFromAddress()
  {
    if (StringHelper.isEmpty(this.fromAddress))
    {
      l("Using administrator e-mail address for sending password-reset email: " 
          + app().getAdministratorEmail(), LogLevel.MINIMUM);
      
      return app().getAdministratorEmail();
    }
    else
    {
      return this.fromAddress;
    }
  }
  
  /**
   * Generates an authorization URL that will be sent to the user by the
   * method sendAuthorizationEmail.  This can be overloaded to return a URL
   * that is suitable for any URL re-writing rules in place.
   */
  protected String getAuthorizationUrl(BasicWebUser user, String ticket)
  {
    final StringBuilder builder = new StringBuilder(500);
    builder.append(app().getInfrastructure().getSecureUrl());
    if (app().getInfrastructure().getSecureUrl().endsWith("/"))
    {
      builder.append(getBaseUri().substring(1));  // Omit leading /
    }
    else
    {
      builder.append(getBaseUri());
    }
    builder.append("/auth?un=")
           .append(NetworkHelper.encodeUrl(user.getUserUsername()));
    builder.append("&vt=")
           .append(ticket);
    
    return builder.toString();
  }
  
  /**
   * Saves a user to the database.  Overload to implement any special cache
   * maintenance that may be necessary (such as notifying peer applications). 
   */
  protected void saveUser(BasicWebUser user)
  {
    store().put(user);
  }
  
  /**
   * STEP 4: Handles an authorized request to change password.  Templates 
   * used:  
   * <ul>
   *   <li>auth/password-reset-process.mustache: Displays a password-reset 
   *       form with elements "pw" (a FormPasswordField configured to require 
   *       confirmation) and "submit" (a FormSubmitButton).</li>
   *   <li>auth/password-reset-complete.mustache: The password-reset process 
   *       is complete.</li>
   * </ul>
   */
  @PathSegment("auth")
  @Post
  @Get
  public boolean authorize(BasicContext context)
  {
    final String username = query().get("un", "");
    final String ticket = query().get("vt", "");

    template(TEMPLATE_RESET_NOT_FOUND);
    
    // Get a reference to the user.
    final BasicWebUser user = (BasicWebUser)this.security.findUser(username);
    if (user != null)
    {
      // Is the authorization ticket correct and not expired?
      if (user.isPasswordResetAuthorized(ticket))
      {
        template(TEMPLATE_RESET);
        
        // Check for submission.
        if (context.isPost())
        {
          final Input input = getPasswordResetValidatorSet().process(context);
          if (input.passed())
          {
            // Change the password.
            final PasswordProposal proposal = new PasswordProposal(
                input.values().get("newpw"), 
                user.getUserUsername(), 
                user, 
                context);
            security.passwordChange(proposal);
            
            saveUser(user);
            
            // Go to the success/complete page.
            template(TEMPLATE_RESET_COMPLETE);
            return message("Password change complete.");
          }
          else
          {
            return validationFailure(input);
          }
        }
        
        return render();
      }
    }

    delivery().message("Invalid password-reset ticket.");
    return badRequest("invalid-ticket");
  }

  /**
   * Overload if desired to return a different email template name.
   */
  protected String getEmailTemplateName()
  {
    return EMAIL_TEMPLATE_NAME;
  }
  
}  // End PasswordResetHandler.
