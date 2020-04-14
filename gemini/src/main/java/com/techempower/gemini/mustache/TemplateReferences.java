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

package com.techempower.gemini.mustache;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.messaging.*;
import com.techempower.gemini.pyxis.*;

/**
 * Provides application-scope references (e.g., asset directories), 
 * environment flags (indicating development, test, or production) and 
 * request-scope objects.  Deployment environment flags (within the "app.env"
 * context) can be used as a Mustache section:
 *   <p>
 * <pre><code>
 * {{#app.env}}
 *   {{#dev}}&lt;p&gt;This is a development environment.&lt;/p&gt;{{/dev}}
 *   {{#devOrTest}}&lt;p&gt;This is a development or test environment.&lt;/p&gt;{{/devOrTest}}
 *   {{#test}}&lt;p&gt;This is a test environment.&lt;/p&gt;{{/test}}
 *   {{#testOrProduction}}&lt;p&gt;This is a test or production environment.&lt;/p&gt;{{/testOrProduction}}
 *   {{#prod}}&lt;p&gt;This is a production environment.&lt;/p&gt;{{/prod}}
 * {{/app.env}}
 * </code></pre>
 *   <p>
 * Asset directories/paths are available within the "app.path" context:
 *   <ul>
 * <li><b>app.path.img</b> - Image asset directory.</li>
 * <li><b>app.path.css</b> - CSS asset directory.</li>
 * <li><b>app.path.js</b> - JavaScript asset directory.</li>
 * <li><b>app.path.root</b> - Standard URL root.</li>
 * <li><b>app.path.sroot</b> - Secure URL root (typically https).</li>
 *   </ul>
 *   <p>
 * All request-scope objects are contained within the "req" context.  For
 * example if an object with "title" and "details" attributes were provided
 * to BasicPathHandler.mustache, those attributes can be rendered as so:
 *   <p>
 * <pre><code>
 * {{#req}}
 *   &lt;p&gt;{{title}}&lt;/p&gt;
 *   &lt;p&gt;{{details}}&lt;/p&gt;
 * {{/req}}
 * </code></pre>
 *   <p>
 */
public class TemplateReferences
{

  /** Application-scope attributes. */
  public final TemplateAppReferences app;
  
  /** The current user. */
  public final PyxisUser currentUser;
  
  /** Request-scope attributes. */
  public final Object req;
  
  /** Session-stored Messages consumed by this request. */
  public final List<Message> messages;
  
  /** The user's query parameters. */
  public final Map<String, String> query;
  
  /**
   * Constructor.
   */
  public TemplateReferences(
      BasicContext context,
      TemplateAppReferences applicationScope, 
      Object requestScope)
  {
    this.app = applicationScope;
    this.req = requestScope;
    this.query = context.query().asMap();
    this.messages = context.messages().list();
    if (applicationScope.application.getSecurity() != null)
    {
      this.currentUser = 
          applicationScope.application.getSecurity().getUser(context);
    }
    else
    {
      this.currentUser = null;
    }
  }

}
