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

import java.lang.reflect.*;

import com.techempower.gemini.*;
import com.techempower.gemini.Request.*;
import com.techempower.gemini.path.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.annotation.*;
import com.techempower.gemini.pyxis.authorization.*;
import com.techempower.js.*;

/**
 * A specialization of BasicPathHandler that enforces authorization
 * constraints as provided by an optional Authorizer.  If no Authorizer is
 * used, a user is required to be authenticated, but <b>any</b> user will 
 * suffice.  An optional Rejector can also be provided, allowing custom 
 * request rejection behavior.  The default Rejector will redirect any 
 * unauthorized requests to the login view.
 */
public class SecureMethodSegmentHandler<C extends Context, U extends PyxisUser>
     extends MethodSegmentHandler<C>
{
  
  //
  // Member variables.
  // 

  private final Rejector       rejector;
  private final Authorizer     authorizer;
  private final PyxisSecurity  security;
  private final ThreadLocal<U> user;
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   * 
   * @param app The GeminiApplication reference.
   * @param componentCode a four-letter code for this handler's ComponentLog.
   * @param jsw The JavaScriptWriter to use for JSON serialization.
   * @param authorizer The Authorizer to determine if the user is authorized;
   *        this is optional, if null any user that is present will be
   *        assumed to be authorized.
   * @param rejector The Rejector to use to reject unauthorized user requests;
   *        this is optional, if null, the Security's force-login Rejector
   *        will be used.
   */
  public SecureMethodSegmentHandler(GeminiApplication app, String componentCode,
      JavaScriptWriter jsw, Authorizer authorizer, Rejector rejector)
  {
    super(app, componentCode, jsw);
    
    this.rejector = rejector != null 
        ? rejector 
        : app.getSecurity().getForceLoginRejector();
    this.authorizer = authorizer;
    this.user = new ThreadLocal<>();
    this.security = app.getSecurity();
  }

  /**
   * Constructor.  Use the application's default JavaScriptWriter for JSON
   * serialization.
   * 
   * @param app The GeminiApplication reference.
   * @param componentCode a four-letter code for this handler's ComponentLog.
   * @param authorizer The Authorizer to determine if the user is authorized;
   *        this is optional, if null any user that is present will be
   *        assumed to be authorized.
   * @param rejector The Rejector to use to reject unauthorized user requests;
   *        this is optional, if null, the Security's force-login Rejector
   *        will be used.
   */
  public SecureMethodSegmentHandler(GeminiApplication app, String componentCode,
      Authorizer authorizer, Rejector rejector)
  {
    this(app, componentCode, null, authorizer, rejector);
  }

  /**
   * Constructor.  Use the application's default JavaScriptWriter for JSON
   * serialization.  No special authorization check is assumed; any
   * authenticated user will be permitted.  The application security's 
   * force-login Rejector will be used by default. 
   * 
   * @param app The GeminiApplication reference.
   * @param componentCode a four-letter code for this handler's ComponentLog.
   */
  public SecureMethodSegmentHandler(GeminiApplication app, String componentCode)
  {
    this(app, componentCode, null, null, null);        
  }

  /**
   * Conduct a (supplemental) authorization check.  All @PathSegment-annotated
   * methods will automatically be gated by an authorization check using the
   * Authorizer provided at construction time.  This method allows subclasses
   * to conduct additional (presumably finer-grained) authorization checks
   * within methods without traversing the object graph to the application's
   * Security. 
   *   <p>
   * The rejector used at construction time will be used if the authorization
   * fails.
   * 
   * @param supplementalAuthorizer A presumably-finer grained authorizer than
   *        the one specified at construction-time.
   */
  protected boolean authCheck(Authorizer supplementalAuthorizer)
  {
    return security.authCheck(context(), supplementalAuthorizer, rejector);
  }
  
  /**
   * Specialize the analysis of annotated methods to look for @PathBypassAuth
   * annotations, which indicate methods for which the authorization check
   * should be bypassed.
   */
  @Override
  protected PathSegmentMethod analyzeAnnotatedMethod(Method method, HttpMethod httpMethod)
  {
    final PathSegmentMethod superAnalysis = super.analyzeAnnotatedMethod(method, httpMethod);
    final PathBypassAuth bypassAnnotation = method.getAnnotation(PathBypassAuth.class);
    final boolean authorizationRequired = (bypassAnnotation == null);

    return new SecurePathSegmentMethod(
        superAnalysis.name,
        superAnalysis.httpMethod,
        superAnalysis.index, 
        superAnalysis.contextParameter, 
        authorizationRequired);
  }

  @Override
  public boolean prehandle(PathSegments segments, C context)
  {
    if (super.prehandle(segments, context))
    {
      // Don't bother doing any additional work if the super-class already
      // handled the request.
      return true;
    }
    
    final SecurePathSegmentMethod method = getAnnotatedMethod(segments, context);
    
    // If method is false, then this handler cannot handle this request.
    if (method == null)
    {
      return false;
    }

    // If the method being requested requires an authorization check, do so.
    if (  (!method.authorizationRequired)
       || (security.authCheck(context, authorizer, rejector))
       )
    {
      // Get a reference to the current user.
      @SuppressWarnings("unchecked")
      final U currentUser = (U)security.getUser(context);
      
      // Set the thread-local user reference.
      user.set(currentUser);
      
      // We have not handled the request because the user is authorized to
      // proceed.  We return false to send the request to the handle method.
      return false;
    }
    else
    {
      // Fail-safe removal of user.
      user.remove();
      
      // The authorization check's Rejector will have handled the request, so
      // return true to indicate it has been handled.
      return true;
    }
  }

  @Override
  public void posthandle(PathSegments segments, C context)
  {
    try
    {
      super.posthandle(segments, context);
    }
    finally
    {
      // Clear out the thread-local user reference.
      user.remove();
    }
  }

  @Override
  protected SecurePathSegmentMethod getAnnotatedMethod(PathSegments segments, C context)
  {
    return (SecurePathSegmentMethod)super.getAnnotatedMethod(segments, context);
  }
  
  /**
   * Gets the Security.
   */
  public PyxisSecurity security()
  {
    return security;
  }
  
  /**
   * Gets the User who made the request.
   */
  public U user()
  {
    return user.get();
  }
  
  /**
   * A specialization of the PathSegmentMethod that includes a flag indicating
   * if the authorization check should be bypassed.
   */
  protected static class SecurePathSegmentMethod
    extends PathSegmentMethod
  {
    final boolean authorizationRequired;
    
    protected SecurePathSegmentMethod(String name, HttpMethod httpMethod, int index, 
        boolean contextParameter, boolean authorizationRequired)
    {
      super(name, httpMethod, index, contextParameter);
      this.authorizationRequired = authorizationRequired;
    }
  }
  
}
