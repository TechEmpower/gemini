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

package com.techempower.gemini.handler;

import java.io.*;
import java.util.*;

import com.techempower.gemini.*;

/**
 * The FileTossHandler provides a subclassable means to provide restricted-
 * access file serving within a Gemini-based web application.  The files must
 * be stored on disk.
 *   <p>
 * FileTossHandler listens for commands beginning with the prefix "file-"
 * such as "file-get".
 *   <ul>
 * <li>file-get: An additional "id" parameter specifies the arbitrary id
 *     given to a file.
 *   </ul>
 * Subclasses should overload (getFiles -or- getFile) and accessGranted.
 *   <p>
 * Example use: if photos are stored by users and photo number (1 through 4
 * for each user), an id could be u2310p4 - indicating photo 4 for user
 * 2310.  In this case, getFile should be overloaded to get the File object
 * that would be associated with this ID.  accessGranted should also be
 * overloaded to indicate whether access to that file will be granted.
 *
 * @see com.techempower.gemini.Handler
 */
public class FileTossHandler<D extends BasicDispatcher, C extends LegacyContext>
  implements Handler<D,C>
{

  //
  // Constants.
  //

  public static final String COMPONENT_CODE = "hFts";
  public static final String HANDLER_PREFIX = "file-";
  public static final String CMD_GET_FILE   = HANDLER_PREFIX + "get";

  //
  // Member variables.
  //
  
  private final GeminiApplication application;

  //
  // Member methods.
  //

  /**
   * Constructor.  Sets up references.  Subclasses' constructors should
   * call super(application).
   */
  public FileTossHandler(GeminiApplication application)
  {
    this.application = application;
  }
  
  /**
   * Gets the application reference.
   */
  protected GeminiApplication getApplication()
  {
    return this.application;
  }

  /**
   * Gets a description of the Handler.
   */
  @Override
  public String getDescription()
  {
    return "Restricted-access file tossing";
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
   * Accept and handle a request if the command is acceptable.
   * Return false if: the command is unacceptable OR the request
   * could not be handled.
   */
  @Override
  public boolean acceptRequest(D dispatcher, C context, String command)
  {
    return (command.startsWith(HANDLER_PREFIX));
  }

  /**
   * Handle the request, regardless of whether the command is
   * acceptable.  Return false if the request could not be handled.
   */
  @Override
  public boolean handleRequest(D dispatcher, C context, String command)
  {
    String fileIdentifier = context.query().get("id", "");

    // Only proceed if an identifier is present.
    // TODO: Future, provide a default file?
    if (fileIdentifier.length() > 0)
    {
      // Look up the file.
      File file = getFile(fileIdentifier);

      // Does the file exist?
      if ( (file != null) 
        && (file.exists()) )
      {
        // Ask subclass if access should be granted.
        if (accessGranted(context, fileIdentifier))
        {
          return tossFile(context, file);
        }
      }
    }

    return false;
  }

  /**
   * Tosses a file to the Context's output stream.  Returns true if
   * successful; false otherwise.
   */
  protected boolean tossFile(C context, File file)
  {
    return context.includeFile(file, file.getName(), true);
  }
  
  /**
   * Returns a hashtable lookup of file identifiers (arbitrary Strings) to
   * File objects.  Subclasses are expected to overload this method.
   *    <p>
   * The keys are ids (e.g., "text-file-1") and the elements are File 
   * objects (e.g., "new File("c:\\temp\\text.txt")")
   *    <p>
   * Subclasses are expected to overload this method.
   */
  protected Map<String,File> getFiles()
  {
    // Base class returns an empty hashtable.
    return new HashMap<>();
  }

  /**
   * If overloading getFiles is not sufficiently functional for the subclass,
   * overloading getFile can provide additional customization.  The file
   * identifier is presented as-is to this method.  This method is expected
   * to return a File object given the file id.
   *    <p>
   * If this method is <i>not</i> overloaded, the default behavior is to call
   * the getFiles() method to get the hashtable mapping IDs to File objects
   * and lookup the id in said hashtable.  If this method <i>is</i> 
   * overloaded, the hashtable is not even necessary.
   *    <p>
   * Returning null indicates that the file could not be found.
   */
  protected File getFile(String fileIdentifier)
  {
    return (getFiles().get(fileIdentifier));
  }

  /**
   * Returns a boolean flag indicating whether the Context provided should
   * be granted access to a file specified by its identifier.  Note that
   * this method will not be called if the identifier is <i>not</i> found in
   * the getFiles() hashtable (or, if getFile is used, instead, this method
   * will <i>not</i> be called if getFile returns null).
   *    <p>
   * Subclasses are expected to overload this method and make it FINAL.
   * 
   * @param context the request context.
   * @param fileIdentifier the file being requested.
   */
  protected boolean accessGranted(C context, String fileIdentifier)
  {
    // Base class always returns false.
    return false;
  }

}   // End FileTossHandler.
