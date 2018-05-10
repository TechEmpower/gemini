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

package com.techempower.gemini.filestore;

import com.techempower.gemini.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * A basic implementation of a File Store Handler, allowing a user to
 * interact with files in their user directory.
 *
 * @see com.techempower.gemini.Handler
 */
public class BasicFileStoreHandler
  implements Handler<BasicDispatcher,LegacyContext>,
             Configurable
{
  
  //
  // Constants.
  //

  public static final String COMPONENT_CODE       = "hFsh";
  public static final String DEFAULT_PROPS_PREFIX = "FileStoreHandler.";

  public static final String CMD_PREFIX           = "fs-";
  public static final String CMD_LIST_FILES       = CMD_PREFIX + "list";
  public static final String CMD_DELETE_FILE      = CMD_PREFIX + "delete";
  public static final String CMD_MANAGE_FILES     = CMD_PREFIX + "manage";
  
  public static final String JSP_LIST_FILES       = CMD_LIST_FILES + ".jsp";

  //
  // Member variables.
  //

  private final GeminiApplication application;
  private final ComponentLog      log;
  private final FileStore         fileStore;

  //
  // Member methods.
  //

  /**
   * Constructor.
   * 
   * @param application The application reference
   * @param fileStore the File Store to work with.
   */
  public BasicFileStoreHandler(GeminiApplication application, FileStore fileStore)
  {
    this(application, fileStore, FileStore.DEFAULT_CONFIGURATION_PREFIX);
  }

  /**
   * Constructor.
   * 
   * @param application The application reference
   * @param fileStore the File Store to work with.
   * @param propsPrefix Optional properties file attribute name prefix; if
   *        null, the default is "DirectHandler." (including the period.)
   */
  public BasicFileStoreHandler(GeminiApplication application, FileStore fileStore, String propsPrefix)
  {
    this.fileStore = fileStore;
    this.log = application.getLog(COMPONENT_CODE);

    this.application = application;
    application.getConfigurator().addConfigurable(this);
  }
  
  /**
   * Gets a description of the Handler.
   */
  @Override
  public String getDescription()
  {
    return "Basic File Store handler";
  }

  /**
   * Gets the desired thread priority of this handler.  Default should be 
   * Handler.PRIORITY_NO_CHANGE.
   * 
   * @see java.lang.Thread
   */
  @Override
  public int getPriority()
  {
    return Handler.PRIORITY_NO_CHANGE;
  }
  
  /**
   * Accept a request if the command is acceptable.  Return false if the 
   * command is unacceptable.
   */
  @Override
  public boolean acceptRequest(BasicDispatcher dispatcher, LegacyContext context, String command)
  {
    return (command.startsWith(CMD_PREFIX));
  }

  /**
   * Handle the request, regardless of whether the command is
   * acceptable.  Return false if the request could not be handled.
   */
  @Override
  public boolean handleRequest(BasicDispatcher dispatcher, LegacyContext context, String command)
  {
    this.log.log("Handling: " + command);
    if (command.equalsIgnoreCase(CMD_LIST_FILES))
    {
      return listFiles(dispatcher, context);
    }
    else if (command.equalsIgnoreCase(CMD_MANAGE_FILES))
    {
      return manageFiles(dispatcher, context);
    }

    return false;
  }
  
  /**
   * Lists files.
   */
  protected boolean listFiles(BasicDispatcher dispatcher, LegacyContext context)
  {
    //log.debug("Listing a user's files.");
    
    PyxisUser user = this.application.getSecurity().getUser(context);
    if (user != null)
    {
      this.fileStore.prepDirectory(user);
      StoredFile[] files = this.fileStore.getFileList(user);
      
      //for (int i = 0; i < files.length; i++)
      //{
      //  log.debug(i + ": " + files[i]);
      //}
      
      context.delivery().putObject("Files", files);

      // Render baseline JSP.
      return context.render(JSP_LIST_FILES);
    }
    
    // No user, we can't handle the request.
    return false;
  }
  
  /**
   * Process a file management request on a user directory.
   */
  protected boolean manageFiles(BasicDispatcher dispatcher, LegacyContext context)
  {
    //log.debug("Processing file management.");
    
    PyxisUser user = this.application.getSecurity().getUser(context);
    if (user != null)
    {
      this.fileStore.processManagement(user, context);

      // Render baseline JSP.
      return context.render(JSP_LIST_FILES);
    }
    
    // No user, we can't handle the request.
    return false;
  }
  
  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    /*
    allowPaths = props.getYesNoProperty(propsPrefix + "AllowPaths", allowPaths);
    filenameVariable = props.getProperty(propsPrefix + "FilenameVariable", filenameVariable);
    filenameSuffix = props.getProperty(propsPrefix + "FilenameSuffix", filenameSuffix);
    */
  }

}  // End BasicFileStoreHandler.
