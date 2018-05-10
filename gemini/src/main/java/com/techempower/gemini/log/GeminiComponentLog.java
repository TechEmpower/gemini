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
import com.techempower.log.*;

/**
 * GeminiComponentLog provides a means to easily attach component codes to 
 * output that is being written to a Log implementation.
 *   <p>
 * Unlike ComponentLog, GeminiComponentLog adds the session id to the debug
 * string before passing it to the regular log.  If the application 
 * implements PyxisApplication, and its user class implements PyxisUser,
 * and stores its user in session using PyxisConstants.SESSION_USER, it 
 * will also add the userid to the debug string.
 *   <p>
 * Generally, the ComponentLog constructor is not called directly, but rather
 * through calls to GeminiApplication.getLog(), which provides an appropriate
 * reference to the application's Log instance.
 *   <p>
 * See com.techempower.Log for the specifications of the debug levels.
 *
 * @see Log
 * @see ComponentLog
 */
public class GeminiComponentLog
     extends ComponentLog
{

  /**
   * This is a ThreadLocal class used only by the GeminiComponentLog.
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
   * Constructor.  Generally not invoked directly, but rather through
   * GeminiApplication.getLog().
   */
  public GeminiComponentLog(Log log, String componentCode)
  {
    super(log, componentCode);
  }

  /**
   * If this Thread is handling a request, returns its name which will contain
   * session/user info.
   *   <p>
   * Otherwise, this returns an empty String.
   * 
   * @return String
   */
  private static String getContextInformation()
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
    buffer.append(StringHelper.truncateAtEnd((session == null ? "none" : session.getId()), 4));
    buffer.append(" ");
    
    if (context.getRequestNumber() > 0) // Means we are counting requests.
    {
      buffer.append("- ");
      buffer.append(context.getRequestNumber());
      buffer.append(" ");
    }

    final PyxisSecurity security = context.getApplication().getSecurity();
    PyxisUser user = null;
      
    if (security != null)
    {
      user = security.getUser(context);
    }
    if (user != null)
    {
      buffer.append(user.getId());
      buffer.append(" ");
    }
    else
    {
      buffer.append("- ");
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

  //
  // Normal debug methods.
  //

  /**
   * Debug output.
   */
  @Override
  public void log(String debugString, int debugLevel, Throwable exc)
  {
    super.log(getContextInformation() + debugString, debugLevel, exc);
  }

  /**
   * Debug output.
   */
  @Override
  public void log(String debugString, Throwable exc)
  {
    super.log(getContextInformation() + debugString, exc);
  }

  /**
   * Log output.
   */
  @Override
  public void log(String logString, int debugLevel)
  {
    super.log(getContextInformation() + logString, debugLevel);
  }

  /**
   * Log output.
   */
  @Override
  public void log(String logString)
  {
    super.log(getContextInformation() + logString);
  }

  /**
   * @see com.techempower.log.ComponentLog#assertion(boolean, String, int)
   */
  @Override
  public void assertion(
    boolean evalExpression,
    String debugString,
    int debugLevel)
  {
    super.assertion(
      evalExpression,
      getContextInformation() + debugString,
      debugLevel);
  }

  /**
   * @see com.techempower.log.ComponentLog#assertion(boolean, String)
   */
  @Override
  public void assertion(boolean evalExpression, String debugString)
  {
    super.assertion(evalExpression, getContextInformation() + debugString);
  }

} // End GeminiComponentLog
