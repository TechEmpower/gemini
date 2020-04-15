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

package com.techempower.gemini.log;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.session.*;
import com.techempower.helper.*;

/**
 * ContextLogInfo provides a string with the the active request's session
 * id, among other things. If the application implements PyxisApplication, and
 * its user class implements PyxisUser, and stores its user in session using
 * PyxisConstants.SESSION_USER, it will also add the userid to the debug
 * string.
 */
public class ContextLogInfo
{

  /**
   * This is a ThreadLocal class used only by the ContextLogInfo.
   */
  private static class ThreadLocalContextInfo extends ThreadLocal<String>
  {
    @Override
    public String initialValue()
    {
      return "";
    }

    public String getStringValue()
    {
      return super.get();
    }

    public void setStringValue(String s)
    {
      super.set(s);
    }
  }

  /**
   * This is the ThreadLocal variable that stores the Context info to display
   * with each log message.
   */
  private static final ThreadLocalContextInfo CONTEXT_INFO = new ThreadLocalContextInfo();

  /**
   * You may not instantiate this class.
   */
  private ContextLogInfo()
  {
    // Does nothing.
  }

  /**
   * If this Thread is handling a request, returns its name which will contain
   * session/user info.
   *   <p>
   * Otherwise, this returns an empty String.
   * 
   * @return String
   */
  public static String getContextInformation()
  {
    return CONTEXT_INFO.getStringValue();
  }

  /**
   * Sets the Context information to be displayed in every log message issued
   * by the Thread handling this request.
   */
  public static void setContextInformation(Context context)
  {
    final StringBuilder buffer = new StringBuilder();

    final Session session = context.getSession(false);
    buffer.append("s[");
    buffer.append(StringHelper.truncateAtEnd((session == null
        ? "none" : session.getId()), 4));
    buffer.append("] ");

    if (context.getRequestNumber() > 0) // Means we are counting requests.
    {
      buffer.append("r[");
      buffer.append(context.getRequestNumber());
      buffer.append("] ");
    }

    final PyxisSecurity security = ((BasicContext)context).getApplication().getSecurity();
    PyxisUser user = null;

    if (security != null)
    {
      user = security.getUser(context);
    }
    if (user != null)
    {
      buffer.append("u[");
      buffer.append(user.getId());
      buffer.append("] ");
    }

    CONTEXT_INFO.setStringValue(buffer.toString());
  }

  /**
   * Clears the Context information.  Meant to be called when a Thread is done
   * handling a request.
   */
  public static void clearContextInformation()
  {
    CONTEXT_INFO.setStringValue("");
  }

} // End ContextLogInfo
