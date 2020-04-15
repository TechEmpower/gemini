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
package com.techempower.gemini.seo;

import java.io.*;
import java.nio.file.*;

import com.techempower.gemini.*;
import com.techempower.gemini.path.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests for the /robots.txt file.  To use, attach this to the
 * PathDispatcher using a root URI segment of "robots.txt".  This handler
 * reads and responds with the contents of the file named by "Robots.File" in
 * the configuration file.  The default behavior is to disallow all user
 * agents, but a typical production environment will be more permissive. 
 */
public class RobotsHandler
     extends BasicPathHandler<Context>
  implements Configurable
{
  private final       Logger log        = LoggerFactory.getLogger(getClass());
  public static final String DISALLOWED = "User-agent: *\nDisallow: /";
  
  private String body = DISALLOWED;
  
  /**
   * Constructor.
   */
  public RobotsHandler(GeminiApplication app)
  {
    super(app);
    app.getConfigurator().addConfigurable(this);
  }

  @Override
  public boolean handle(PathSegments segments, Context context) 
  {
    return text(body);
  }

  @Override
  public void configure(EnhancedProperties props) 
  {
    final String filename = props.get("Robots.File", "");
    if (StringHelper.isNonEmpty(filename)) 
    {
      final File file = new File(filename);
      if (file.exists())
      {
        try
        {
          final byte[] bytes = Files.readAllBytes(file.toPath());
          body = new String(bytes);
        }
        catch (IOException ioexc)
        {
          log.info("Unable to read {}; using disallow-all robots.txt response.",
              filename);
        }
      }
    }
  }

}
