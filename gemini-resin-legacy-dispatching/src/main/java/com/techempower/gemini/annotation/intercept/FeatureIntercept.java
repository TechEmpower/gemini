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
package com.techempower.gemini.annotation.intercept;

import java.lang.annotation.*;
import java.lang.reflect.*;

import com.techempower.gemini.*;
import com.techempower.gemini.feature.*;

/**
 * Implementation of HandlerIntercept that ensures that a required features are enabled.
 * Features are checked against those in registered with the FeatureManager. If a required feature
 * has been disabled, then the jsp unauthorized.jsp is included.
 * <pre> 
 *  &#064;CMD("editComment")
 *  &#064;RequireFeature({
 *    "edit-comment"
 *    "delete-comment"
 *  })
 *  public boolean editComment(Context context)
 *  {
 *    
 *  } 
 * </pre>
 */
public class FeatureIntercept<D extends BasicDispatcher, C extends LegacyContext> 
  implements HandlerIntercept<D, C>
{
  private FeatureManager manager;
  
  public FeatureIntercept(GeminiApplication application)
  {
    this.manager = application.getFeatureManager();
  }
  
  /**
   * Intercepts the request if any of the features are disabled.
   */
  @Override
  public boolean intercept(Method m, BasicDispatcher dispatcher, LegacyContext context, String command, Annotation annotation) 
  {
    if (this.manager != null && annotation != null)
    {
      for(String feature : ((RequireFeature)annotation).value())
      {
        if (!this.manager.on(feature))
        {
          return true;
        }
      }
    }
    
    // either there's no manager for this application, or annotation was null
    // so we have no feature to test against
    return false;
  }

  /**
   * Includes a jsp telling the user that they are not authorized to view that command.
   */
  @Override
  public boolean handleIntercept(Method m, BasicDispatcher dispatcher, LegacyContext context, String command, Annotation annotation) 
  {
    return context.render(((RequireFeature)annotation).jsp());
  }
  
}
