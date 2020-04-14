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
package com.techempower.gemini.jsp;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.techempower.gemini.*;

/**
 * Provides standard JSP utility functionality to Gemini applications,
 * inheriting some of that functionality from InfrastructureJsp.
 */
public abstract class BasicJsp
  extends             InfrastructureJsp
{

  /**
   * Gets a GeminiApplication reference for this application.  Subclass
   * the GeminiApplication class to return a reference to the
   * running GeminiApplication subclass used by your application.
   *   <p>
   * Typically, this is done like so: return MyApplication.getInstance();
   *
   * @return an instance of GeminiApplication in use by this specific
   *         application.
   */
  protected abstract GeminiApplication getApplicationReference();

  /**
   * Performs custom initialization from the given context.  Will be called 
   * after the context is derived.  Overload if necessary.
   * 
   * @param context the context being initialized.
   */
  public void contextInit(BasicContext context)
  {
    // Do nothing.
  }

  /**
   * The entry point into service.
   */
  @Override
  public void service(ServletRequest req, ServletResponse res)
    throws ServletException, IOException
  {
    // casting exceptions will be raised if an internal error.
    final HttpServletRequest  request  = (HttpServletRequest)req;
    final HttpServletResponse response = (HttpServletResponse)res;

    // See if the context is available.  If not, this JSP was invoked
    // directly.
    BasicContext context = (BasicContext)request.getAttribute("Context");

    // Get a reference to the application if one has not already been
    // gathered.
    if (getApplication() == null)
    {
      setApplication(getApplicationReference());
    }

    if ((getApplication().getInfrastructure().canInvokeJSPsDirectly() == false) && (context == null))
    {
      response.sendRedirect(getApplication().getInfrastructure().getCmdUrl(GeminiConstants.CMD_HOME));
    }
    else
    {
      boolean    contextExisted;

      if (context == null)
      {
        // Set the flag.
        contextExisted = false;

        // Put a new context into the attributes.
        context = getApplication().getContext(new ResinHttpRequest(request, response,
          getServletConfig().getServletContext(), getApplication()));
        request.setAttribute("Context", context);
      }
      // If we have a Context, get a reference to the dispatcher.
      else
      {
        // Set the flag.
        contextExisted = true;
      }

      try
      {
        this.contextInit(context);
        _jspService(request, response);
      }
      catch (Exception exc)
      {
        // Ask the dispatcher to handle this exception.
        getApplication().getDispatcher().dispatchException(context, exc, null);
      }
      finally
      {
        // Unbind the Context from the current thread if we had to create it.
        if (contextExisted == false)
        {
          BasicContext.complete();
        }
      }
    }
  }


}
