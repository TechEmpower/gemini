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
import com.techempower.gemini.pyxis.*;

/**
 * Checks that a user is a part of the specified groups. The user must be a part of all groups.
 * If the user is not a part of any group in the list, then the unauthorized.jsp gets included.
 * You can specify the jsp to be included in the annotation.
 */
public class GroupIntercept<D extends BasicDispatcher, C extends LegacyContext> 
  implements HandlerIntercept<D, C>
{
  
  public GroupIntercept(GeminiApplication application)
  {
  }
  
  /**
   * Intercepts the request if a user isn't a part of the specified groups.
   */
  @Override
  public boolean intercept(Method m, D dispatcher, C context, String command, Annotation annotation) 
  {
    PyxisSecurity security = context.getApplication().getSecurity();
    if (security == null)
    {
      // No security?  You're not interested.
      return false;
    }
    
    PyxisUser user = security.getUser(context);
    // make sure the user isn't null, this shouldn't be the case 
    // since @RequireGroup should always be accompanied by @RequireLogin
    if (user == null)
    {
      return false;
    }

    // Check the "allOf" groups and ensure the user is in all of them.
    int[] groups = ((RequireGroup)annotation).allOf();
    boolean inGroups = true;
    for (int group : groups)
    {
      if (!user.isMember(group))
      {
        inGroups = false;
        break;
      }
    }
    
    // If we're still good, then ensure the user is in any of "anyOf"
    groups = ((RequireGroup)annotation).anyOf();
    if (inGroups && groups.length > 0)
    {
      inGroups = false;
      for (int group : groups)
      {
        if (user.isMember(group))
        {
          inGroups = true;
          break;
        }
      } 
    }
    
    return !inGroups;
  }

  /**
   * Includes a jsp telling the user that they are not authorized to view that command.
   */
  @Override
  public boolean handleIntercept(Method m, D dispatcher, C context, String command, Annotation annotation) 
  {
    String msg = ((RequireGroup)annotation).msg();
    if (msg != null)
    {
      context.delivery().put("Message", msg);
    }
    return context.render(((RequireGroup)annotation).jsp());
  }
}
