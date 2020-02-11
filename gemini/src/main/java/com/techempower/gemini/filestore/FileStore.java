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

import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.techempower.gemini.*;
import com.techempower.gemini.manager.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FileStore provides a managed collection of directories and files that
 * can be associated with arbitrary application objects (e.g., Users).  This
 * is used by the TechEmpower Task Tracker to allow for files to be
 * associated to Users (their personal file directory) and Tasks (files
 * attached to tasks).
 *   <p>
 * See the BasicFileStoreHandler for an implementation of a Gemini Handler
 * that processes file uploads and management activities (delete).
 *   <p>
 * Files are stored on the file system using directories created based on
 * the type of entity (e.g., Users, Tasks) and the identity of the entity
 * (e.g., #51260).  Fan-out sub-directories are created to alleviate file
 * system stress that can arise when too many directories are contained
 * within a single location of the file system.  E.g., for user #51260, the
 * path would be: [file-store-root]/u51/260, where 'u' is a single-letter
 * code for the Users class.  Codes for classes are assigned at run-time, as
 * below:
 *    <p>
 * <pre><br>
 *   fileStore = new FileStore(this);
 *  
 *   // Register Identifiable object types that may have associated files
 *   // along with their filesystem directory prefix.
 *   fileStore.registerType(new User(), 'u');  // Users
 *   fileStore.registerType(new Foo(), 'f');   // Foos
 * </pre>
 */
public class FileStore
  extends    BasicManager<GeminiApplication>
{
  
  //
  // Constants.
  //
  
  public static final int    DIRECTORY_CHUNK_SIZE = 1000;
  public static final int    DIRECTORY_CHUNK_DIGITS = 3;
  public static final String DEFAULT_CONFIGURATION_PREFIX = "FileStore.";
  public static final int    DEFAULT_FILENAME_LENGTH_MAX = 250;
  public static final int    MAXIMUM_FILES_PER_UPLOAD = 10;
  
  //
  // Member variables.
  //
  
  private final Map<Class<?>,String> typesToCodes;
  
  private String            filesystemRoot;
  private String            urlRoot;
  private String            configurationPrefix;
  private boolean           enabled = true;
  private int               filenameLengthMaximum = DEFAULT_FILENAME_LENGTH_MAX;
  private Logger            log = LoggerFactory.getLogger("FSto");
  
  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public FileStore(GeminiApplication application)
  {
    this(application, DEFAULT_CONFIGURATION_PREFIX);
  }

  /**
   * Constructor.
   */
  public FileStore(GeminiApplication application, String configurationPrefix)
  {
    super(application);
    this.configurationPrefix = configurationPrefix;
    typesToCodes = new HashMap<>();
  }
  
  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // Disable during configuration.
    setEnabled(false);

    final EnhancedProperties.Focus focus = props.focus(configurationPrefix);
    
    filesystemRoot = focus.get("FilesystemRoot", null);
    urlRoot = focus.get("URLRoot", "/");
    filenameLengthMaximum = focus.getInt("FilenameLengthMaximum", DEFAULT_FILENAME_LENGTH_MAX);
    
    if (StringHelper.isNonEmpty(filesystemRoot))
    {
      // Only provide File Store services if settings are good.
      setEnabled(true);
    }
    else
    {
      setEnabled(false);
      log.warn("No file system root specified in configuration file!");
    }
  }
  
  /**
   * Registers an Identifiable "type" of object that can have associated
   * files.  For each type registered, a single-letter code will be provided.
   * This code is then used in the directory naming.  In other words, a User
   * type may be given the code "u" meaning that directories for users will
   * include the letter "u".
   */
  public <T extends Identifiable> void registerType(Class<T> identifiableType, char code)
  {
    typesToCodes.put(identifiableType, "" + code);
  }
  
  /**
   * Gets the single-letter code for an Identifiable type that has been
   * registered with the File Store. 
   */
  public String getTypeCode(Identifiable identifiableType)
  {
    return typesToCodes.get(identifiableType.getClass());
  }
  
  /**
   * Gets a relative path for a specific user. 
   */
  public String getRelativePath(String directoryPrefix, long itemIdentity)
  {
    final long relativeNumber = itemIdentity % DIRECTORY_CHUNK_SIZE;
    final long chunkNumber = itemIdentity / DIRECTORY_CHUNK_SIZE;
    return directoryPrefix + chunkNumber + "/" 
        + StringHelper.padZero(relativeNumber, DIRECTORY_CHUNK_DIGITS) + "/";
  }
  
  /**
   * Gets a relative path for an object's files. 
   */
  public String getRelativePath(Identifiable ident)
  {
    return getRelativePath(getTypeCode(ident), ident.getId());
  }

  /**
   * Gets an absolute file system path for an object's files.
   */
  public String getPath(Identifiable ident)
  {
    return filesystemRoot + getRelativePath(ident);
  }

  /**
   * Gets an absolute URL path for an Identifiable object.
   */
  public String getUrl(Identifiable ident)
  {
    return urlRoot + getRelativePath(ident);
  }

  /**
   * Gets an absolute file system path for a file based on an Identifiable
   * object and a filename.
   */
  public String getPathname(Identifiable ident, String filename)
  {
    return getPath(ident) + normalizeFilename(filename);
  }

  /**
   * Performs a size calculation on an identifiable object's files.
   */
  public long getTotalBytes(Identifiable ident)
  {
    return getTotalBytes(getPath(ident));
  }
  
  /**
   * Gets a list of files in a object's directory.
   */
  public StoredFile[] getFileList(Identifiable ident)
  {
    final File[] files = getRawFileList(getPath(ident));
    return getFileList(files, getUrl(ident));
  }
  
  /**
   * Gets a single file in an object's directory given the filename.  Returns
   * null if no matching file is found.
   */
  public StoredFile getStoredFile(Identifiable ident, String filename)
  {
    final File file = new File(getPathname(ident, filename));
    return getStoredFile(file, getUrl(ident));
  }
  
  /**
   * Converts an image file using ImageMagick.  Requires that the full path
   * to the "convert.exe" (or comparable) ImageMagick executable be specified
   * in the configuration file.
   */
  public boolean convertImageFile(Identifiable source, String sourceFilename,
    Identifiable dest, String destFilename, int newWidth, int newHeight)
  {
    // Prepare the destination directory.
    prepDirectory(dest);

    // Construct the full filenames.
    final String sourceName = getPathname(source, sourceFilename);
    final String destName = getPathname(dest, destFilename);
    
    // Construct file objects.
    final File sourceFile = new File(sourceName);
    final File destFile = new File(destName);

    log.info("Converting {} to {}", sourceFile, destFile);
    
    app().getImageHelper().transformImage(
        new ImageHelper.TransformParams(sourceFile, destFile), 
        new ImageHelper.ImageTransform("png", newWidth, newHeight, false, false));

    // Always return true because right now, the ImageHelper doesn't throw
    // its exceptions.
    return true;
  }
  
  /**
   * Renames a file (without moving the file).
   */
  public boolean renameFile(Identifiable ident, String currentFilename, String newFilename)
  {
    final String currentName = getPathname(ident, currentFilename);
    final String newName = getPathname(ident, newFilename);
    
    final File currentFile = new File(currentName);
    final File newFile = new File(newName);
    if (!newFile.exists())
    {
      return currentFile.renameTo(newFile);
    }
    else
    {
      log.info("New filename is already in use: {}", newFilename);
      return false;
    }
  }
  
  
  /**
   * Moves a file from one object's to another object's directory.
   */
  public boolean moveFile(Identifiable source, Identifiable dest, String filename)
  {
    final String inputFile = getPathname(source, filename);
    final String outputDirectory = getPath(dest);
    return moveFile(inputFile, outputDirectory);
  }
  
  /**
   * Moves a file from an arbitrary source to a object's directory.
   */
  public boolean moveFile(String sourceFilename, Identifiable ident)
  {
    final String outputDirectory = getPath(ident);
    return moveFile(sourceFilename, outputDirectory);
  }
  
  /**
   * Stores a file provided by an InputStream to a specified object's
   * directory using a specified filename.  This differs from moveFile in
   * that it (a) doesn't delete the source file; and (b) works with an
   * InputStream rather than a raw file.
   */
  public boolean storeFile(InputStream fileInputStream, Identifiable dest, 
    String filename)
  {
    // Construct the pathname.
    final String destinationFilename = getPathname(dest, filename);

    // Prepare the destination directory.
    prepDirectory(dest);

    try (
        FileOutputStream outputStream = new FileOutputStream(destinationFilename);
        )
    {
      FileHelper.copyStreamContents(fileInputStream, outputStream, 0L);
    }
    catch (IOException ioexc)
    {
      log.error("IOException while storing file {}.", filename, ioexc);
      return false;
    }
    
    // Return success.
    return true;
  }
  
  /**
   * Stores a file provided by a File object to a specified object's
   * directory using the File's filename.  This differs from moveFile in
   * that it does not delete the source file.
   */
  public boolean storeFile(File sourceFile, Identifiable dest)
  {
    try (
        FileInputStream inputStream = new FileInputStream(sourceFile);
        )
    {
      return storeFile(inputStream, dest, sourceFile.getName());
    }
    catch (IOException ioexc)
    {
      log.error("IOException while storing file {}.", sourceFile, ioexc);
      return false;
    }
  }
  
  /**
   * Determines if a file exists.
   */
  public boolean exists(Identifiable ident, String filename)
  {
    final String filepath = getPathname(ident, filename);
    final File file = new File(filepath);
    return file.exists();
  }
  
  /**
   * Deletes a file from an object's directory.
   */
  public boolean deleteFile(Identifiable ident, String filename)
  {
    return deleteFile(getPathname(ident, filename)); 
  }

  /**
   * Deletes all files from an object's directory.  Some applications may 
   * choose to do this at the end of a user's session, making a user's file 
   * store transient.
   */
  public List<String> deleteAllFiles(Identifiable ident)
  {
    return deleteAllFiles(getPath(ident));
  }
  
  /**
   * Prepares an object's directory.  This presently amounts to just ensuring
   * that the directory has been created.
   */
  public boolean prepDirectory(Identifiable ident)
  {
    return makeDirectories(getPath(ident));
  }
  
  /**
   * Process a user's uploads or other management activities on their own 
   * user directory.
   * 
   * @param user The user making the request.
   * @param context The request context. 
   */
  public ManagementOutcome processManagement(PyxisUser user, Context context)
  {
    return processManagement(user, user, context);
  }

  /**
   * Process a user's uploads or other management activities on their own 
   * user directory.
   * 
   * @param user The user making the request.
   * @param forObject THe object whose associated files are being managed.
   * @param context The request context. 
   */
  public ManagementOutcome processManagement(PyxisUser user, 
      Identifiable forObject, Context context)
  {
    return processManagement(user, forObject, context, MANAGEMENT_PERMISSIONS_ALL);
  }
 
  /**
   * Process a user's uploads or other management activities on their own 
   * user directory.
   * 
   * @param user The user making the request.
   * @param forObject THe object whose associated files are being managed.
   * @param context The request context. 
   */
  public ManagementOutcome processManagement(PyxisUser user, 
      Identifiable forObject, Context context, ProcessManagementOptions options)
  {
    final ManagementOutcome outcome = new ManagementOutcome();
    outcome.success = true;
    
    prepDirectory(forObject);
    
    // Process file uploads.  We will accept up to 10 (or 11, if the 
    // unnumbered file field is counted) files.  Only process uploads if the
    // request was the proper size.
    if (  (outcome.success)
       && (options.allowUpload)
       )
    {
      for (int i = 0; i <= MAXIMUM_FILES_PER_UPLOAD; i++)
      {
        final RequestFile file = context.files().get("file" + (i == 0 ? "" : "" + i));
        if (file != null)
        {
          final String filename = file.getFilename();
          try
          {
            file.writeTo(Paths.get(getPathname(forObject, filename)));
            outcome.success = true;
          }
          catch (IOException e)
          {
            outcome.success = false;
          }
          outcome.addUploadedFile(filename);
          log.info("{} uploaded {}", user, filename);
        }
      }
    }
    
    // Process file deletions.
    if (options.allowDelete)
    {
      final String[] deletions = context.query().getStrings("delete");
      if (deletions != null)
      {
        for (String deletion : deletions)
        {
          if (deleteFile(forObject, deletion))
          {
            outcome.addDeletedFile(deletion);
            log.info("{} deleted (multiple)", deletion);
          }
        }
      }
      else
      {
        final String delete = context.query().get("delete");
        if ( (delete != null)
          && (deleteFile(forObject, delete))
          )
        {
          outcome.addDeletedFile(delete);
          log.info("{} deleted (single)", delete);
        }
      }
    }
    
    // Process "delete all" request.
    if (options.allowDeleteAll)
    {
      if (context.query().getInt("deleteall") == 1)
      {
        final List<String> deleted = deleteAllFiles(forObject);
        for (String filename : deleted)
        {
          outcome.addDeletedFile(filename);
        }
      }
    }
    
    return outcome;
  }
  
  /**
   * Defines settings for use when processing a Management request.
   */
  public static class ProcessManagementOptions
  {
    public final boolean allowUpload;
    public final boolean allowDelete;
    public final boolean allowDeleteAll;
    
    public ProcessManagementOptions(boolean allowUpload, boolean allowDelete, 
        boolean allowDeleteAll)
    {
      this.allowDelete = allowDelete;
      this.allowDeleteAll = allowDeleteAll;
      this.allowUpload = allowUpload;
    }
  }
  
  private static final ProcessManagementOptions MANAGEMENT_PERMISSIONS_ALL = 
      new ProcessManagementOptions(true, true, true);
  
  /**
   * Provides outcome information for a Management request.
   */
  public static class ManagementOutcome
  {
    public boolean success = true;
    public List<String> deletedFiles = null;
    public List<String> uploadedFiles = null;
    
    /**
     * Adds a deleted file.
     */
    public void addDeletedFile(String deletedFilename)
    {
      if (this.deletedFiles == null)
      {
        deletedFiles = new ArrayList<>();
        deletedFiles.add(deletedFilename);
      }
    }
    
    /**
     * Adds an uploaded file.
     */
    public void addUploadedFile(String uploadedFilename)
    {
      if (uploadedFiles == null)
      {
        uploadedFiles = new ArrayList<>();
        uploadedFiles.add(uploadedFilename);
      }
    }
  }

  //
  // Utility functions.
  //
  
  /**
   * Moves a file.
   */
  protected boolean moveFile(String sourceFilename, String destDirectoryName)
  {
    try
    {
      // Make the directories for the destination if they haven't already
      // been made.
      makeDirectories(destDirectoryName);
      
      final File sourceFile = new File(sourceFilename);
      final File destDirectory = new File(destDirectoryName);
      
      // If the source and destination now exists, proceed.
      if ( (sourceFile.exists())
        && (destDirectory.exists())
        )
      {
        com.google.common.io.Files.move(sourceFile, new File(destDirectory, sourceFile.getName()));
        return true;
      }
      else
      {
        log.info("Move: Source file {} not found.", sourceFile);
      }
    }
    catch (IOException ioexc)
    {
      log.error("IOException while moving file {}.", sourceFilename, ioexc);
    }

    return false;
  }
  
  /**
   * Deletes a file.
   */
  protected boolean deleteFile(String filename)
  {
    final File file = new File(filename);
    return file.delete();
  }
  
  /**
   * Deletes all files in a provided path.  Returns a list of the names.
   */
  protected List<String> deleteAllFiles(String path)
  {
    final File[] files = getRawFileList(path);
    final List<String> deleted = new ArrayList<>(files.length);
    
    String name;
    boolean success;

    for (File file : files)
    {
      name = file.getName();
      success = file.delete();
      if (success)
      {
        deleted.add(name);
      }
    }
    
    return deleted;
  }
  
  /**
   * Make the necessary directories for a path.
   */
  protected boolean makeDirectories(String path)
  {
    final File directory = new File(path);
    if ( (!directory.exists())
      || (!directory.isDirectory())
      )
    {
      // mkdirs returns true only if the operation had to make new
      // directories.
      return directory.mkdirs();
    }
    
    // Directory already exists.
    return false;
  }
  
  /**
   * Performs a size calculation on an arbitrary directory. 
   */
  protected long getTotalBytes(String absolutePath)
  {
    final File[] fileList = getRawFileList(absolutePath);

    long total = 0L;
    for (File file : fileList)
    {
      total += file.length();
    }
    return total;
  }
  
  /**
   * Gets a file list for an arbitrary absolute path.
   */
  protected StoredFile[] getFileList(File[] files, String urlPrefix)
  {
    final StoredFile[] toReturn = new StoredFile[files.length];
    
    for (int i = 0; i < files.length; i++)
    {
      toReturn[i] = new StoredFile(files[i].getName(), 
        urlPrefix + files[i].getName(),
        files[i].length());
    }
    
    return toReturn;
  }
  
  /**
   * Gets a single file for an arbitrary absolute path.  Returns null if
   * the file does not exist.
   */
  protected StoredFile getStoredFile(File file, String urlPrefix)
  {
    if (file.exists())
    {
      return new StoredFile(file.getName(),
        urlPrefix + file.getName(),
        file.length());
    }
    return null;
  }
  
  /**
   * Gets a file list for an arbitrary absolute path.
   */
  protected File[] getRawFileList(String absolutePath)
  {
    final File directory = new File(absolutePath);
    if ( (directory.exists()) 
      && (directory.isDirectory())
      )
    {
      // Generate a list of files only; ignoring directories.
      return directory.listFiles(ONLY_FILES_FILTER);
    }
    else
    {
      return new File[0];
    }
  }

  private static final OnlyFilesFilter ONLY_FILES_FILTER = new OnlyFilesFilter();
  private static class OnlyFilesFilter
      implements FileFilter
  {
    @Override
    public boolean accept(File file)
    {
      return file.isFile();
    }
  }

  /**
   * Normalizes a filename by enforcing a maximum filename length and using
   * the character normalization handled by FileHelper.normalizeFilename.
   */
  public String normalizeFilename(String source)
  {
    return FileHelper.normalizeFilename(
      StringHelper.truncate(source, filenameLengthMaximum));
  }
  
  //
  // Simple getters and setters.
  //
  
  /**
   * Enabled/disabled state.  Note that this isn't presently used anywhere yet.
   * 
   * @return Returns the enabled flag.
   */
  public boolean isEnabled()
  {
    return enabled;
  }

  /**
   * @param enabled The enabled to set.
   */
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }
  
}  // End FileStore.
