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

import com.techempower.gemini.*;

/**
 * Provides quickly accessible references to useful variables/objects from
 * within a JSP page.  Typically, a RequestVariables object is created in
 * the "include-variables.jsp" page that is included as part of most/all
 * JSPs in an application.  The instance is usually exposed as "vars".
 *   <p>
 * This allows the JSP author to quickly reference things like:
 * 
 * <ul>
 * <li>vars.context - the current Context (this is typically defined in
 *     concrete subclasses of RequestVariables so that the reference is, in
 *     turn, a concrete subclass of Context (e.g., MyAppContext).</li>
 * <li>vars.user - the current User object.  As above, defined in subclasses.</li>
 * <li>vars.isAdministrator - a boolean indicating if the user is a system 
 *     administrator.</li>
 * <li>vars.sas - request-scope references to external JavaScript and CSS
 *     files; note that the typical application will only use application-scope
 *     and page-scope scripts and sheets so use of vars.sas is uncommon.</li>
 * </ul>
 */
public class RequestVariables
{
  
  public static final int DEFAULT_MINIMUM_BUFFER_SIZE = 2000;

  /**
   * The title of the HTML page, which is typically rendered into a 
   * &lt;title&gt; tag, but could be used elsewhere such as in a &lt;h1&gt;
   * tag.
   */
  public String title = "";
  
  /**
   * Any simple JavaScript to include in the "onload" attribute of the page's
   * &lt;body&gt; tag.
   */
  public String onload = null;
  
  /**
   * A ScriptsAndSheets reference for request-scope external JavaScript and
   * CSS style-sheet files.  Note that request-scope scripts and sheets are
   * quite uncommon.  In most cases, only application-scope (e.g., jQuery or
   * myapp-core.js) and page-scope (e.g., forum-thread-view.js) are used.
   */
  public ScriptsAndSheets sas;
  
  /**
   * An optional StringBuilder that may be used to buffer up JavaScript code
   * to be embedded within the footer of the page via renderEmbeddedScript().
   * Note that embedded script is discouraged in general and this is only
   * provided as a convenience for situations where it is being used for
   * whatever reason.  
   */
  private StringBuilder embeddedScript;
  
  /**
   * An optional StringBuilder that may be used to buffer up CSS code
   * to be embedded within the footer of the page via renderEmbeddedSheet().
   * Note that embedded CSS is discouraged in general and this is only
   * provided as a convenience for situations where it is being used for
   * whatever reason.  
   */
  private StringBuilder embeddedSheet;
  
  /**
   * Constructor.
   */
  public RequestVariables(BasicContext context)
  {
    this.sas = new ScriptsAndSheets(context.getApplication());
  }
  
  /**
   * Add JavaScript code to the embedded script buffer.  Note that embedded
   * script is discouraged.  This is provided as a convenience for situations
   * where embedded script is being used, but all applications should move
   * as much script as possible to external files (.js files) to benefit from
   * client-side caching. 
   */
  public void addEmbeddedScript(String javaScriptCode)
  {
    if (this.embeddedScript == null)
    {
      this.embeddedScript = new StringBuilder(
          javaScriptCode.length() > DEFAULT_MINIMUM_BUFFER_SIZE ? javaScriptCode.length()
              : DEFAULT_MINIMUM_BUFFER_SIZE);
    }
    this.embeddedScript.append(javaScriptCode);
  }
  
  /**
   * Add JavaScript code to the embedded sheet buffer.  Note that embedded
   * CSS is discouraged.  This is provided as a convenience for situations
   * where embedded CSS is being used, but all applications should move
   * as much CSS as possible to external files (.css files) to benefit from
   * client-side caching. 
   */
  public void addEmbeddedSheet(String cssCode)
  {
    if (this.embeddedSheet == null)
    {
      this.embeddedSheet = new StringBuilder(
          cssCode.length() > DEFAULT_MINIMUM_BUFFER_SIZE ? cssCode.length()
              : DEFAULT_MINIMUM_BUFFER_SIZE);
    }
    this.embeddedSheet.append(cssCode);
  }
  
  /**
   * Renders the embedded Script buffer.  See notes on addEmbeddedScript.
   *   <p>
   * Note that InfrastructureJsp.renderScripts, which is the recommended
   * means to render references to external JavaScript files, does <b>not</b>
   * render this embedded script.  If embedded script is being used on your
   * page, your application should call vars.renderEmbeddedScript() directly
   * within the page footer.
   */
  public String renderEmbeddedScript()
  {
    if (this.embeddedScript != null)
    {
      return "<script>" + this.embeddedScript.toString() + "</script>";
    }
    else
    {
      return "";
    }
  }
  
  /**
   * Renders the embedded sheet buffer.  See notes on addEmbeddedSheet.
   *   <p>
   * Note that InfrastructureJsp.renderSheets, which is the recommended
   * means to render references to external CSS files, does <b>not</b>
   * render this embedded sheet.  If embedded CSS is being used on your
   * page, your application should call vars.renderEmbeddedSheet() directly
   * within the page header.
   */
  public String renderEmbeddedSheet()
  {
    if (this.embeddedSheet != null)
    {
      return "<style>" + this.embeddedSheet.toString() + "</style>";
    }
    else
    {
      return "";
    }
  }
  
}
