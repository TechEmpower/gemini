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

package com.techempower.helper;

import gnu.trove.set.*;
import gnu.trove.set.hash.*;

import java.io.*;

import com.techempower.util.*;

/**
 * FileHelper provides utility functions for working with files.
 */
public final class FileHelper
{
  
  //
  // Static variables.
  //
  
  /**
   * Permissible characters for a filename.
   */
  public static final String PERMISSIBLE_FILENAME_CHARACTERS_STRING
    = UtilityConstants.ASCII_DIGITS
    + UtilityConstants.ASCII_LOWERCASE 
    + UtilityConstants.ASCII_UPPERCASE 
    + UtilityConstants.ASCII_SPACE
    + "{}[].,;'!@#$%^&()_-=+~`";
  public static final TCharSet PERMISSIBLE_FILENAME_CHARACTERS = new TCharHashSet();
  static 
  {
    for (char c : PERMISSIBLE_FILENAME_CHARACTERS_STRING.toCharArray())
    {
      PERMISSIBLE_FILENAME_CHARACTERS.add(c);
    }
  }

  //
  // Static methods.
  //

  /**
   * Gets a file extension from a provided full filename.  Returns empty
   * string if no extension can be found.  Note: Does not include the period
   * in the returned String.
   */
  public static String getExtension(String filename)
  {
    int lastPeriod = filename.lastIndexOf('.');
    int lastForwardslash = filename.lastIndexOf('/');
    int lastBackslash = filename.lastIndexOf('\\');

    if ( (lastPeriod > lastForwardslash)
      && (lastPeriod > lastBackslash)
      )
    {
      if (lastPeriod < filename.length())
      {
        return filename.substring(lastPeriod + 1);
      }
    }

    // Defaults to returning nothing.
    return "";
  }

  /**
   * Replaces the file extension on a filename with the extension provided.
   * Do not include the period character in the new extension.
   */
  public static String replaceExtension(String filename, String newExtension)
  {
    // Make a buffer.
    StringBuilder newFilename = new StringBuilder(filename.length() + newExtension.length());

    // Get the current extension.
    String currentExtension = getExtension(filename);

    // Grab the part of the filename before the extension.
    newFilename.append(filename.substring(0, filename.length() - currentExtension.length()));

    // Append the new extension.
    newFilename.append(newExtension);

    // Return the new filename.
    return newFilename.toString();
  }

  /**
   * Normalizes a provided filename by replacing any characters that are
   * not permissible in a filename with a minus-sign/dash.  The parameter
   * must be the filename part exclusively and should not include the path.
   * If the path is provided, the root and path separators will be changed
   * to minus-signs.
   */
  public static String normalizeFilename(String filename)
  {
    StringBuilder toReturn = new StringBuilder(filename.length());

    char current;
    for (int i = 0; i < filename.length(); i++)
    {
      current = filename.charAt(i);
      if (PERMISSIBLE_FILENAME_CHARACTERS.contains(current))
      {
        toReturn.append(current);
      }
      else
      {
        toReturn.append('-');
      }
    }

    return toReturn.toString();
  }

  /**
   * Creates a file path by concatenating two Strings, inserting a
   * File.separator (back-slash on Windows) between them if necessary and
   * removing redundant forward-slashes and back-slashes between them if
   * necessary.
   */
  public static String concatenatePaths(String part1, String part2)
  {
    String p1 = (part1 != null ? part1 : "");
    String p2 = (part2 != null ? part2 : "");
    
    p1 = stripSlashesRight(p1);
    p2 = stripSlashesLeft(p2);

    return p1 + File.separator + p2;
  }

  /**
   * Removes back-slashes and forward-slashes from the front (left-side) of
   * the given string.
   */
  public static String stripSlashesLeft(String str)
  {
    // If the string is null or if it doesn't start with a forward-slash
    // or backslash.
    if ( str == null
      || str.length() == 0
      || !(str.startsWith("/") || str.startsWith(File.separator)))
    {
      return str;
    }
    else
    {
      return stripSlashesLeft(str.substring(1));
    }
  }

  /**
   * Removes backslashes and forward-slashes from the end (right-side) of the
   * given string.
   */
  public static String stripSlashesRight(String str)
  {
    // If the string is null or if it doesn't end with a forward-slash or
    // backslash.
    if ( str == null
      || str.length() == 0
      || !(str.endsWith("/") || str.endsWith(File.separator)))
    {
      return str;
    }
    else
    {
      return stripSlashesRight(str.substring(0, str.length() - 1));
    }
  }

  private static final int BYTE_BUFFER_SIZE = 0x1000; // 4K

  /**
   * Copies the contents of an input stream to an output stream up to a
   * specified number of bytes.  This method does not close either
   * stream, but it <i>does</i> flush the output stream.
   *
   * @param inputStream  the InputStream to read from.
   * @param outputStream the OutputStream to write to.
   * @param length       the length, in bytes, to copy; 0 = all
   */
  public static void copyStreamContents(
    InputStream inputStream,
    OutputStream outputStream,
    long length
  )
    throws IOException
  {
    long fileRemaining;
    if (length == 0)
    {
      fileRemaining = inputStream.available();
    }
    else
    {
      fileRemaining = length;
    }

    byte[]  buffer  = new byte[BYTE_BUFFER_SIZE];
    int     toRead;
    int     read;

    //
    // Copy blocks until we're done.
    //
    while (fileRemaining > 0)
    {
      toRead = BYTE_BUFFER_SIZE;
      if (toRead > fileRemaining)
      {
        toRead = (int)fileRemaining;
      }

      read = inputStream.read(buffer, 0, toRead);

      //
      // Write to the output stream.
      //
      outputStream.write(buffer, 0, read);

      //
      // Update state information.
      //
      fileRemaining -= read;

      if (length == 0)
      {
        fileRemaining = inputStream.available();
      }
    }

    //
    // Flush the output stream.
    //
    outputStream.flush();
  }

  /**
   * You may not instantiate this class.
   */
  private FileHelper()
  {
    // Does nothing.
  }

}  // End FileHelper.

