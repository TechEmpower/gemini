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

import java.io.*;
import java.util.*;

import com.github.mustachejava.*;
import com.techempower.gemini.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.scheduler.*;
import com.techempower.util.*;

/**
 * Compiles and renders mustache templates.
 *
 * <p>Templates are read from a configurable directory.  The name of this
 * property is {@code Mustache.Directory}.  Here is a sample entry for a
 * Gemini .conf file:</p>
 *
 *<pre>
 *  # Mustache.Directory
 *  #   Specifies the physical directory for Mustache templates.
 *
 *  Mustache.Directory = ${Servlet.WebInf}/mustache/
 *</pre>
 *
 * <p>Other configuration options:</p>
 * <ul>
 * <li>Mustache.Enabled - Defaults to enabled, but allows Mustache to be 
 *     disabled.</li>
 * <li>Mustache.TemplateCacheEnabled - Defaults if the deployment descriptor
 *     is Test or Production.  But this configuration option allows the
 *     template caching to be controlled specifically.</li>
 * <li>Mustache.TemplateCacheResetInterval - A configurable interval, in
 *     seconds, in which the template cache will be reset.  If a production
 *     environment sees templates updated via sftp, for example, an interval
 *     of 120 seconds may be fair.  Default is 0, which means do not ever
 *     reset.</li>
 * </ul>
 */
public abstract class MustacheManager
    implements Configurable
{
  
  public static final String DEFAULT_MUSTACHE_EXTENSION = ".mustache"; 

  protected final GeminiApplication application;
  private final TemplateAppReferences applicationReferences;
  private final ComponentLog log;
  protected MustacheFactory mustacheFactory;
  protected String mustacheDirectory;
  protected boolean enabled;
  protected boolean useTemplateCache = true;
  protected long resetTemplateCacheInterval = 0L;
  protected final CacheResetEvent cacheResetEvent;

  /**
   * Constructor.
   */
  public MustacheManager(GeminiApplication application)
  {
    this.application = application;
    this.log = application.getLog("MusM");
    this.application.getConfigurator().addConfigurable(this);
    this.applicationReferences = constructApplicationReferences();
    this.cacheResetEvent = new CacheResetEvent();
    application.getConfigurator().addConfigurable(this.applicationReferences);
  }
  
  /**
   * Construct the MustacheApplicationReferences object.
   */
  protected TemplateAppReferences constructApplicationReferences()
  {
    return new TemplateAppReferences(this.application);
  }
  
  @Override
  public void configure(EnhancedProperties props)
  {
    final EnhancedProperties.Focus focus = props.focus("Mustache.");
    this.enabled = focus.getBoolean("Enabled", true);
    this.useTemplateCache = focus.getBoolean("TemplateCacheEnabled", !application.getVersion().isDevelopment());
    log.log("Mustache " + (this.useTemplateCache ? "" : "not ") + "using template cache.");
    final int resetSeconds = focus.getInt("TemplateCacheResetInterval", 0);
    this.resetTemplateCacheInterval = resetSeconds * UtilityConstants.SECOND;

    // Warn about deprecated "MustacheDirectory" configuration directive.
    if (props.has("MustacheDirectory"))
    {
      log.log("MustacheDirectory is deprecated. Use Mustache.Directory instead.");
      this.mustacheDirectory = props.get("MustacheDirectory", mustacheDirectory);
    }
  }
  
  /**
   * Is the template cache enabled?
   */
  public boolean isTemplateCacheEnabled()
  {
    return useTemplateCache;
  }
  
  /**
   * Construct the MustacheFactory.
   */
  public abstract void resetTemplateCache();
  
  /**
   * Gets a TemplateReferences object containing a reference to the
   * MustacheApplicationReferences and the provided request-scope object.
   */
  public TemplateReferences getTemplateReferences(BasicContext context, Object requestScope)
  {
    return new TemplateReferences(context, applicationReferences, requestScope);
  }
  
  /**
   * Get the application-scope template references.
   */
  protected TemplateAppReferences getApplicationReferences()
  {
    return applicationReferences;
  }

  protected abstract MustacheFactory getMustacheFactory();

  /**
   * Renders the given mustache template file to the given writer, provided
   * the given {@code scope} objects for the template.  The scope objects
   * provide the values to the template to render.
   *
   * <p>"Typical" scope objects, according to the creator of Mustache.java,
   * are instances of classes that were created for the sole purpose of
   * rendering in one or more Mustache templates.  For example, if you have
   * a class {@code Video} representing a video as a data entity, and
   * you want to render values from or related to a {@code Video} in a
   * template, you would create a second class with methods to return those
   * values.</p>
   *
   * <pre>   
   *   public class Video implements Identifiable
   *   {
   *     private int id;
   *     private int uploaderId;
   *     private String title;
   *     ...
   *   }
   *
   *   public class VideoRendering
   *   {
   *     public int id() { ... }
   *     public String uploaderName { ... }
   *     public String title() { ... }
   *     ...
   *   }
   * </pre>
   *
   * <p>Then when rendering properties of a {@code Video} in HTML code, you
   * would create an instance {@code VideoRendering} and use that as your
   * scope.</p>
   *
   * <pre>   
   *   public boolean handleVideoView(Context context)
   *   {
   *     Video video = cache.get(Video.class, context.getIntRequestValue("videoid");
   *
   *     application.getMustacheRenderer().render(
   *         "video.mustache",
   *         context.getWriter(),
   *         new VideoRendering(video));
   *     return true;
   *   }
   * </pre>
   * 
   * @param filename The Mustache template's filename, including extension.
   * @param writer A writer to which to send the rendering.
   * @param scope Any scope objects to provide to the Mustache template.
   */
  public void render(String filename, Writer writer, Object... scope)
  {
    if (  (filename == null)
       || (writer == null)
       || (scope == null)
       )
    {
      throw new IllegalArgumentException("MustacheManager.render received at least one null argument: " 
          + filename + " " + writer + " " + Arrays.toString(scope));
    }
    
    final Mustache mustache = this.getMustacheFactory().compile(filename);
    mustache.execute(writer, scope);
  }
  
  /**
   * Render a mustache template to a provided Context.
   * 
   * @param filename The Mustache template's filename, including extension.
   * @param context The request Context.
   * @param scope Any scope object to provide to the Mustache template.
   * 
   * @return true if successful
   */
  public boolean render(String filename, BasicContext context, Object scope)
  {
    try
    {
      final Writer writer = context.getWriter();
      final TemplateReferences refs = getTemplateReferences(context, scope);
      render(filename, writer, refs);
      return true;
    }
    catch (IOException ioexc)
    {
      return false;
    }
  }
  
  /**
   * Cache reset event.
   */
  private final class CacheResetEvent extends ScheduledEvent
  {
    private CacheResetEvent()
    {
      super("Mustache Template Cache Reset Event",
          "Resets the Mustache template cache every " 
              + DateHelper.getHumanDuration(resetTemplateCacheInterval, 2));
    }
    
    @Override
    public long getDefaultScheduledTime()
    {
      return System.currentTimeMillis() + resetTemplateCacheInterval;
    }

    @Override
    public boolean requiresOwnThread()
    {
      return false;
    }

    @Override
    public void execute(Scheduler scheduler, boolean onDemandExecution)
    {
      // 0 = disabled.
      if (resetTemplateCacheInterval > 0L)
      {
        resetTemplateCache();
        scheduler.scheduleEvent(this, getDefaultScheduledTime());
      }
    }
  }
  
  protected void setupTemplateCache()
  {
    resetTemplateCache();
    
    if (this.resetTemplateCacheInterval > 0L)
    {
      application.getScheduler().scheduleEvent(cacheResetEvent);
    }
  }
}
