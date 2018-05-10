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
package com.techempower.gemini;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

import com.techempower.helper.*;

/**
 * A file submitted as part of a {@linkplain Request request}.
 * <p>
 * The functionality of this class is dependent on the servlet 3.0 API.  Any
 * application using this class must have the following entry in {@code
 * web.xml}: <pre>{@code <multipart-form enable="true"/>}</pre>
 * <p>
 * Additionally, the servlet handling the request must have the
 * {@linkplain MultipartConfig} annotation.  For example:
 * <pre>{@literal @}MultipartConfig(fileSizeThreshold=1024*1024*20,
 *                 maxRequestSize=1024*1024*5)</pre>
 * <p>
 * In order for a file to be considered present, it must:
 * <ul>
 * <li>have been submitted in a {@code POST} request
 * <li>have been submitted in a {@code multipart/form-data} request
 * <li>have a filename specified in its {@code Content-Disposition} header
 * <li>have a mime type specified in its {@code Content-Type} header
 * </ul>
 * <p>
 * Those conditions are met by all modern browsers for files submitted in {@code
 * <input type="file">} fields within a {@code
 * <form method="POST" enctype="multipart/form-data">} form. 
 * <p>
 * Example usage:
 * <pre>
 * RequestFile file = context.getFile("foo");
 *
 * // If the file is present, print some information about it to the log, then
 * // write the file to disk:
 * if (file != null)
 * {
 *   String submittedFilename = file.getFilename();
 *   String mimeType = file.getContentType();
 *   long fileSize = file.getSize();
 *   log.debug(String.format(
 *       "Received file "%s".  Its type is "%s" and it is %s bytes long.",
 *       submittedFilename, mimeType, fileSize));
 *   file.writeTo(Paths.get("/path/to/foo.txt"));
 * }
 *
 * // Obtain the file content to perform manual operations...
 *
 * // ...as a stream:
 * InputStream fileStream = file.getContentAsStream();
 *
 * // ...as a byte array:
 * byte[] fileBytes = file.getContentAsBytes();
 *
 * // ...as a string:
 * String fileString = file.getContentAsString(StandardCharsets.UTF_8);
 * </pre>
 */
public final class ResinRequestFile implements RequestFile {
  //
  // Constants
  //

  /**
   * The size of the buffer used when writing to byte arrays.
   */
  private static final int BYTE_BUFFER_SIZE = 0x1000; // 4K

  /**
   * The size of the buffer used when writing to strings.
   */
  private static final int CHAR_BUFFER_SIZE = 0x800; // 2K chars (4K bytes)

  /**
   * Used to extract the filename information from the {@code
   * Content-Disposition} header within parts of multipart requests.  A typical
   * header looks like this: <pre>
   * Content-Disposition: form-data; name="someFile"; filename="foo.txt"</pre>
   */
  private static final Pattern FILENAME_PATTERN = Pattern.compile("(.*)filename=\"(.*)\"");

  //
  // Helper methods
  //

  /**
   * If the given object is {@code null}, throws an {@code
   * IllegalArgumentException}.  Otherwise, returns the given object.
   *
   * @param object the object to be checked for {@code null}
   * @param name the name of the object, for debugging
   * @return the object if it is not {@code null}
   * @throws IllegalArgumentException if the object is {@code null}
   */
  private static <T> T checkNotNull(T object, String name)
  {
    if (object == null)
    {
      throw new IllegalArgumentException(
          "Argument '" + name + "' must not be null.");
    }
    return object;
  }

  /**
   * Returns {@code true} if it is safe to treat the given string as the name of
   * a file.  That is, writing to a file at {@code Paths.get(dir, filename)} or
   * {@code Paths.get(dir + filename)} should write to a file directly within
   * {@code dir}, and should not overwrite {@code dir}, write outside of {@code
   * dir}, or write to a subdirectory of {@code dir}.
   *
   * @param filename the input string to test for validity
   * @return {@code true} if the input string is a valid filename
   */
  private static boolean isValidFilename(String filename)
  {
    if (filename == null)
    {
      return false;
    }
    final Path path;
    try
    {
      path = Paths.get(filename);
    }
    catch (InvalidPathException e)
    {
      return false;
    }
    if (path.getNameCount() != 1             // "foo/bar"
        || path.isAbsolute())                // "/foo"
    {
      return false;
    }
    final Path normalized;
    try
    {
    	normalized = path.normalize();
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
    	// Gets thrown on Linux only, it seems
    	return false;
    }
    if (!path.equals(normalized))            // "."
    {
      return false;
    }
    final String pathname = normalized.toString();
    if (pathname.isEmpty()                   // ""
        || pathname.equals("..")             // ".."
        || !pathname.equals(filename))       // "foo/"
    {
      return false;
    }
    return true;
  }

  /**
   * Returns {@code true} if the given request is one that could possibly
   * contain any files (in a shape recognized by this class).
   *
   * @param request the request to be tested
   * @return {@code true} if the request might contain files
   */
  private static boolean mightContainFiles(Request request)
  {
    // No files can be present if the request isn't a POST.  This check also
    // only works with HttpRequests; Simulated requests do not support files.
    if (  (!(request instanceof HttpRequest))
       || (!request.isPost())
       )
    {
      return false;
    }
    
    // Check the content type.  Note that requests do not necessarily have
    // a content type at all.
    final HttpRequest hr = (HttpRequest)request;
    return StringHelper.startsWithIgnoreCase(
        hr.getRawRequest().getContentType(), "multipart/form-data");
  }

  /**
   * Returns the file object representing the given part, or {@code null} if
   * this part doesn't represent a valid file.
   *
   * @param part the part to be interpreted as a file
   * @return the file object representing the given part, or {@code null}
   */
  private static RequestFile fromPart(Part part)
  {
    if (part == null)
    {
      return null;
    }
    // Require standard Content-Disposition header with a filename in order to
    // differentiate files from regular form fields.  For instance, an
    // <input type="text"> field would exist as a Part, but it would not have a
    // Content-Disposition header with a filename.
    final String contentDisposition = part.getHeader("content-disposition");
    if (contentDisposition == null)
    {
      return null;
    }
    final Matcher matcher = FILENAME_PATTERN.matcher(contentDisposition);
    if (!matcher.matches())
    {
      return null;
    }
    final String filename = matcher.group(2);
    // Require a valid filename of a single file and no directory hierarchy,
    // which should be no problem if the request isn't from an attacker.  This
    // is simply an extra guard against filenames like "../../malicious.txt", so
    // that applications using this utility don't have to worry about sanitizing
    // the filenames quite as much.
    if (!isValidFilename(filename))
    {
      return null;
    }
    final String contentType = part.getContentType();
    if (contentType == null)
    {
      return null;
    }
    return new ResinRequestFile(part, filename, contentType);
  }

  //
  // Factory methods
  //

  /**
   * Returns the file submitted in this request with the given parameter name.
   * If the given request does not contain a file with the given parameter name,
   * this method returns {@code null}.
   *
   * @param request the request containing the file to be read
   * @param parameterName the name of the request parameter under which the file
   *                      was submitted
   * @return the file with the given name, or {@code null}
   */
  public static RequestFile get(Request request, String parameterName)
  {
    checkNotNull(request, "request");
    checkNotNull(parameterName, "parameterName");
    if (!mightContainFiles(request))
    {
      return null;
    }
    final HttpServletRequest servletRequest = ((HttpRequest) request).getRawRequest();
    final Part part;
    try
    {
      part = servletRequest.getPart(parameterName);
    }
    catch (ServletException | IOException e)
    {
      return null;
    }
    return fromPart(part);
  }

  /**
   * Returns the list of all files submitted in this request.
   * <p>
   * The returned list is unmodifiable and not {@code null}.  If the given
   * request does not contain files, then this method returns the empty list.  
   *
   * @param request the request containing the files to be read
   * @return the list of all files submitted in this request
   */
  public static List<RequestFile> get(Request request)
  {
    checkNotNull(request, "request");
    if (!mightContainFiles(request))
    {
      return Collections.emptyList();
    }
    final HttpServletRequest servletRequest = ((HttpRequest) request).getRawRequest();
    final Collection<Part> parts;
    try
    {
      parts = servletRequest.getParts();
    }
    catch (ServletException | IOException e)
    {
      return Collections.emptyList();
    }
    if (parts == null || parts.isEmpty())
    {
      return Collections.emptyList();
    }
    List<RequestFile> files = null;
    for (Part part : parts)
    {
      RequestFile file = fromPart(part);
      if (file == null)
      {
        continue;
      }
      if (files == null)
      {
        files = new ArrayList<>();
      }
      files.add(file);
    }
    if (files == null)
    {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(files);
  }

  //
  // Non-static internals
  //

  private final Part   part;
  private final String filename;
  private final String contentType;

  private ResinRequestFile(Part part, String filename, String contentType)
  {
    this.part = checkNotNull(part, "part");
    this.filename = checkNotNull(filename, "filename");
    this.contentType = checkNotNull(contentType, "contentType");
  }

  //
  // Non-static public API
  //

  /**
   * Returns the name of the request parameter for this file.
   *
   * @return the name of the request parameter for this file
   */
  @Override
  public String getParameterName()
  {
    return part.getName();
  }

  /**
   * Returns the filename assigned by the request to this file.
   *
   * @return the filename assigned by the request to this file
   */
  @Override
  public String getFilename()
  {
    return filename;
  }

  /**
   * Returns the content type (mime type) of this file.
   *
   * @return the content type of this file
   */
  @Override
  public String getContentType()
  {
    return contentType;
  }

  /**
   * Returns the size of this file in bytes.
   *
   * @return the size of this file in bytes
   */
  @Override
  public long getSize()
  {
    return part.getSize();
  }

  /**
   * Returns the contents of this file as an input stream.
   *
   * @return the contents of this file as an input stream
   * @throws IOException if an I/O error occurs
   * @see #getContentAsBytes
   * @see #getContentAsString(Charset)
   */
  @Override
  public InputStream getContentAsStream() throws IOException
  {
    return part.getInputStream();
  }
  
  /**
   * Writes the contents of the file to an OutputStream.
   */
  @Override
  public void writeTo(OutputStream out) throws IOException
  {
    try (
        InputStream in = getContentAsStream();
        )
    {
      FileHelper.copyStreamContents(in, out, 0);
    }
  }

  /**
   * Returns the contents of this file as a byte array.
   *
   * @return the contents of this file as a byte array
   * @throws IOException if an I/O error occurs
   * @see #getContentAsStream
   * @see #getContentAsString(Charset)
   */
  @Override
  public byte[] getContentAsBytes() throws IOException
  {
    try (
        InputStream in = getContentAsStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        )
    {
      final byte[] buffer = new byte[BYTE_BUFFER_SIZE];
      while (true)
      {
        int bytesRead = in.read(buffer);
        if (bytesRead == -1)
        {
          break;
        }
        out.write(buffer, 0, bytesRead);
      }
      return out.toByteArray();
    }
  }

  /**
   * Returns the contents of this file as a string in the given character set.
   *
   * @param charset the character set of the returned string
   * @return the contents of this file as a string
   * @throws IOException if an I/O error occurs
   * @see #getContentAsBytes
   * @see #getContentAsStream
   */
  @Override
  public String getContentAsString(Charset charset) throws IOException
  {
    checkNotNull(charset, "charset");
    
    try (
        InputStreamReader in = new InputStreamReader(getContentAsStream(), charset)
        )
    {
      final StringBuilder out = new StringBuilder();
      final CharBuffer buffer = CharBuffer.allocate(CHAR_BUFFER_SIZE);
      while (in.read(buffer) != -1)
      {
        buffer.flip();
        out.append(buffer);
        buffer.clear();
      }
      return out.toString();
    }
  }

  /**
   * Writes the contents of this file to the given destination.  If a file
   * already exists at that location, that file is replaced with this one.
   *
   * @param destination the target destination for the contents of this file
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void writeTo(Path destination) throws IOException
  {
    checkNotNull(destination, "destination");
    Files.copy(
        getContentAsStream(),
        destination,
        StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Returns a string representation of this file meant for debugging purposes.
   * It does not return the contents of the file as a string.  For that, use
   * {@linkplain #getContentAsString(Charset) file.getContentAsString(cs)}.
   *
   * @return a string representation of this file
   */
  @Override
  public String toString()
  {
    return String.format(
        "RequestFile{parameterName=%s, fileName=%s, type=%s, size=%s}",
        getParameterName(), getFilename(), getContentType(), getSize());
  }
}
