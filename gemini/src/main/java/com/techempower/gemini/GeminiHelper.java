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
import java.nio.charset.*;
import java.sql.*;
import java.util.*;

import com.techempower.data.util.*;
import com.techempower.helper.*;
import com.techempower.js.*;
import com.techempower.util.*;

import gnu.trove.map.*;
import gnu.trove.map.hash.*;
import org.slf4j.Logger;

/**
 * Provides some static helper functionality for Gemini applications.
 */
public final class GeminiHelper
{

  //
  // Constants.
  //

  public  static final int    BUFFER_SIZE       = 4096;
  private static final TIntObjectMap<String> HTTP_ERROR_CODES;
  
  static 
  {
    HTTP_ERROR_CODES = new TIntObjectHashMap<>();
    HTTP_ERROR_CODES.put(400, "Bad Request");
    HTTP_ERROR_CODES.put(401, "Unauthorized");
    HTTP_ERROR_CODES.put(403, "Forbidden");
    HTTP_ERROR_CODES.put(404, "Not Found");
    HTTP_ERROR_CODES.put(405, "Method Not Allowed");
  }

  //
  // Static methods.
  //
  
  /**
   * Gets a name for an HTTP response code.
   * 
   * @param httpResponseCode an HTTP response code such as 404.
   */
  public static String getHttpResponseName(int httpResponseCode)
  {
    final String found = HTTP_ERROR_CODES.get(httpResponseCode);
    return (found != null) ? found : "Unknown";
  }
  
  /**
   * Prepare to write a file or data in a response.
   */
  public static void prepareDump(GeminiApplication application, 
      Context context, String filename)
  {
    // Did the container find the file's content type?
    boolean setContentType = false;

    // Set up the file attachment.
    final InitConfig servletConfig = application.getServletConfig();
    if (servletConfig != null)
    {
      String mimeType = servletConfig.getMimeType(filename);
      if (mimeType != null)
      {
        context.setContentType(mimeType);
        setContentType = true;
      }
    }

    // If we could not determine the content type, set it to octet-stream.
    // At least the user will be able to save the file.
    if (!setContentType)
    {
      context.setContentType("text/plain;name=\"" + filename + "\"");
    }

    // Set the response header to try to get the browser to save the file.
    context.headers().put("Content-disposition",
        "attachment; filename=\"" + filename + "\"");
  }
  
  /**
   * Dump an arbitrary file to the response.
   */
  public static boolean fileDump(GeminiApplication application, 
      Context context, String filename, byte[] bytes, Logger log)
  {
    prepareDump(application, context, filename);
    try 
    {
      context.getOutputStream().write(bytes, 0, bytes.length);
      
      return true;
    }
    catch (IOException ioexc)
    {
      log.info("IOException while dumping file: ", ioexc);
    }
    
    return false;
  }

  /**
   * Reusable data dumping method for use with a database query.
   * @throws SQLException
   */
  public static boolean dataDump(GeminiApplication application,
      Context context, List<? extends TabularColumn> fieldAttribs, String query,
      String exportFilename, Logger log) throws SQLException
  {
    try (
        Connection c = application.getConnectorFactory().getConnectionMonitor().getConnection();
        PreparedStatement statement = c.prepareStatement(
        query))
    {
      ResultSet resultSet = statement.executeQuery();

      // Use the method below to fulfill the rest of the operation.
      return dataDump(application, context, fieldAttribs, resultSet,
          exportFilename, log);
    }
  }

  /**
   * Generic reusable data dumping method. This version can accept an Iterator
   * of Objects (assuming ObjectDumpField specifications are provided in the
   * fieldAttribs list) or a DatabaseConnectorIterator (assuming normal
   * DumpField specifications are provided int he fieldAttribs list.)
   * @throws SQLException
   */
  public static boolean dataDump(GeminiApplication application,
      Context context, List<? extends TabularColumn> fieldAttribs,
      ResultSet resultSet, String exportFilename, Logger log) throws SQLException
  {
    prepareDump(application, context, exportFilename);
    
    try
    {
      final OutputStream os = context.getOutputStream();

      StringList line = new StringList();
      for (TabularColumn dumpField : fieldAttribs)
      {
        line.add(
            DatabaseHelper.prepareDoubleQuote(dumpField.getDisplayName()));
      }

      String toWrite = line.toString() + UtilityConstants.CRLF;
      os.write(toWrite.getBytes(), 0, toWrite.length());

      while (resultSet.next())
      {
        line = new StringList();

        for (TabularColumn dumpField : fieldAttribs)
        {
          String dumpString = DatabaseHelper.prepareDoubleQuote(
              dumpField.getValue(resultSet));
          dumpString = StringHelper.replaceSubstrings(dumpString, "\r\n",
              "\\n");
          line.add(dumpString);
        }

        toWrite = line.toString() + UtilityConstants.CRLF;
        os.write(toWrite.getBytes(), 0, toWrite.length());
      }

      // Flush and close.
      os.flush();

      return true;
    }
    catch (IOException ioexc)
    {
      log.info("IOException while dumping data: ", ioexc);
    }

    return false;
  }

  /**
   * Returns a Map representing all of the request parameters provided in a
   * URL. This version differs from the implementation that exists within the
   * Servlet API in that this version will tokenize on question marks (?) in
   * addition to ampersands (&amp;). The standard implementation follows HTTP spec
   * and only tokenizes on ampersands. Some applications use question marks to
   * separate parameters. In cases where that is done, this method can be
   * useful.
   * <p>
   * A US-ASCII encoding of the query string is expected.
   * <p>
   * Parameters are expected to follow the "name=value" format, such as
   * "cmd=home".
   * 
   * @param context a Context containing the request.
   * @return a Map instance with keys to values.
   */
  public static Map<String, String> getAllRequestParameters(Context context)
  {
    return getAllRequestParameters(context.getQueryString());
  }

  /**
   * Returns a Map representing all of the request parameters provided in a
   * URL.
   */
  public static Map<String, String> getAllRequestParameters(String query)
  {
    final Map<String, String> map = new HashMap<>();

    try
    {
      // Switched to using UTF-8 now that Gemini is Java 1.4+ only.
      String decoded = java.net.URLDecoder.decode(query, StandardCharsets.UTF_8.name());

      // Tokenize on question marks and ampersands.
      StringTokenizer tokenizer = new StringTokenizer(decoded, "?&");

      // Grab each piece and look for name=value pairs.
      String piece;
      while (tokenizer.hasMoreElements())
      {
        piece = tokenizer.nextToken();
        int equalsPosition = piece.indexOf('=');

        // If an equals is found, insert a name, value pair.
        if (equalsPosition > 0)
        {
          // If the equals is at the end, include a name=null pair.
          if (equalsPosition == piece.length() - 1)
          {
            map.put(piece.substring(0, equalsPosition), null);
          }
          else
          {
            map.put(piece.substring(0, equalsPosition),
                piece.substring(equalsPosition + 1));
          }
        }
        // If an equals was found at position 0, there was no name, so we'll
        // ignore that. If the equals was not found at all, we'll insert
        // a name=null pair.
        else if (equalsPosition == -1)
        {
          map.put(piece, null);
        }
      }
    }
    catch (UnsupportedEncodingException uee)
    {
      // Ignore.
    }

    return map;
  }
  
  /**
   * Sends a JSON encoding of a single object as a response.  The object is
   * not wrapped as a named value in a JSON map; rather, it is encoded and
   * sent as-is.
   *   <p>
   * The default JavaScriptWriter is used.
   *   <p>
   * This method always returns true to allow for the following usage in
   * Handlers: 
   *   return GeminiHelper.sendJson(context, object);
   * 
   * @param context The request Context.
   * @param object The object to send.
   */
  public static boolean sendJson(Context context, Object object)
  {
    return sendJson(context, null, object, null);
  }

  /**
   * Sends a JSON encoding of a single object as a response.  The object is
   * not wrapped as a named value in a JSON map; rather, it is encoded and
   * sent as-is.
   *   <p>
   * If the jsw parameter is null, the default JavaScriptWriter is used.
   *   <p>
   * This method always returns true to allow for the following usage in
   * Handlers: 
   *   return GeminiHelper.sendJson(context, object);
   * 
   * @param context The request Context.
   * @param object The object to send.
   * @param jsw A JavaScriptWriter instance configured to write the provided
   *        object. If null, a default writer will be used.
   */
  public static boolean sendJson(Context context, Object object, 
      JavaScriptWriter jsw)
  {
    return sendJson(context, null, object, jsw);
  }

  /**
   * Sends a JSON encoding of a single object as a response.  The object is
   * optionally wrapped as a named value in a JSON map if the "objectName" 
   * parameter is provided.  Otherwise, the object is encoded and sent as-is.
   *   <p>
   * The default JavaScriptWriter is used.
   *   <p>
   * This method always returns true to allow for the following usage in
   * Handlers: 
   *   return GeminiHelper.sendSingleJsonObject(...);
   * 
   * @param context The request Context.
   * @param objectName an optional Javascript name for the object, if not
   *        provided, the object is encoded as-is without a wrapping map.
   * @param object The object to send.
   */
  public static boolean sendJson(Context context,
      String objectName, Object object)
  {
    return sendJson(context, objectName, object, null);
  }

  /**
   * Sends a JSON encoding of a single object as a response.  The object is
   * optionally wrapped as a named value in a JSON map if the "objectName" 
   * parameter is provided.  Otherwise, the object is encoded and sent as-is.
   *   <p>
   * If the jsw parameter is null, the default JavaScriptWriter is used.
   *   <p>
   * This method always returns true to allow for the following usage in
   * Handlers: 
   *   return GeminiHelper.sendJson(...);
   * 
   * @param context The request Context.
   * @param objectName an optional Javascript name for the object, if not
   *        provided, the object is encoded as-is without a wrapping map.
   * @param object The object to send.
   * @param jsw A JavaScriptWriter instance configured to write the provided
   *        object. If null, a default writer will be used.
   */
  public static boolean sendJson(Context context,
      String objectName, Object object, JavaScriptWriter jsw)
  {
    // Use the provided JSW or the default if none is provided.
    final JavaScriptWriter writer = (jsw != null 
        ? jsw 
        : context.getApplication().getJavaScriptWriter()
        );
    
    context.setContentType(GeminiConstants.CONTENT_TYPE_JSON);
    
    // If a name is provided, wrap the object as a JSON-encoded map with a 
    // single named entry.
    if (StringHelper.isNonEmpty(objectName))
    {
      context.print('{' + writer.write(objectName) + ':' + writer.write(object) + '}');
    }
    // Otherwise, encode the object as-is.
    else 
    {
      context.print(writer.write(object));
    }
    
    return true;
  }
  
  /**
   * Send a plain-text response.
   */
  public static boolean sendPlaintext(Context context, String text)
  {
    context.setContentType(GeminiConstants.CONTENT_TYPE_TEXT);
    context.print(text);
    
    return true;
  }

  /**
   * Supporting variables for convertHTMLToText.
   */
  private static final String[] HTML_FINDS        = new String[] { "<BR>",
    "<br>", "<P>", "<p>", "&nbsp;"               };
  private static final String[] HTML_REPLACEMENTS = new String[] { "\n",
    "\n", "\n", "\n", " "                        };
  
  /**
   * Convert a HTML string to plain text. This method has not been
   * thoroughly tested on user-provided content and is intended for use with
   * system content. This method converts paragraph and BR tags plaintext
   * line breaks.
   */
  public static String convertHtmlToText(String textValue)
  {
    // Replace the special cases.
    String firstPass = StringHelper.replaceSubstrings(textValue, HTML_FINDS,
        HTML_REPLACEMENTS);

    // Remove tags.
    StringBuilder toReturn = new StringBuilder();
    char[] characters = firstPass.toCharArray();
    boolean inTag = false;
    for (char character : characters)
    {
      if (inTag)
      {
        if (character == '>')
        {
          inTag = false;
        }
      }
      else
      {
        if (character == '<')
        {
          inTag = true;
        }
        else
        {
          toReturn.append(character);
        }
      }
    }

    // Return.
    return toReturn.toString();
  }
  
  /**
   * Returns a string that is valid for use as a JavaScript variable or part
   * of a method name.
   * 
   * @param s The starting string, to be sanitized.
   * @return s, with problematic chars replaced with underscores.
   */
  public static String generateJavaScriptValidName(String s)
  {
    // For null or empty strings, return the empty string.
    if (s == null || s.length() == 0)
    {
      return "";
    }

    // JavaScript variables can start with '_', '$', or a letter, but nothing
    // else.
    String firstChar = s.substring(0, 1);
    firstChar = firstChar.replaceAll("[^a-zA-Z_$]", "_");

    if (s.length() == 1)
    {
      // If we only have one character to work with, we're done.
      return firstChar;
    }

    // The rest of the variable name is allowed to contain numbers in addition
    // to letters, '_', and '$', but nothing else beyond that.
    String rest = s.substring(1);
    rest= rest.replaceAll("[^0-9a-zA-Z_$]", "_");

    // Return sanitized firstChar plus sanitized rest.
    return firstChar + rest;
  }

  /**
   * Creates an EnhancedProperties object with all Servlet initialization
   * parameters from a ServletConfig reference.
   */
  public static EnhancedProperties convertServletConfigToProps(InitConfig config)
  {
    EnhancedProperties props = new EnhancedProperties();
    Enumeration<String> params = config.getInitParameterNames();
    
    while (params.hasMoreElements())
    {
      String name = params.nextElement();
      props.put(name, config.getInitParameter(name));
    }
    
    return props;
  }
  
  /**
   * Determines if the provided request is an "AJAX" request that expects a
   * JSON-formatted response.
   */
  public static boolean isJsonRequest(Context context)
  {
    // A request is expecting JSON if the browser sents the "accept" request
    // header as such or if the URL parameters include "format=json".
    return (  (StringHelper.containsNullSafe(context.headers().accept(), 
               GeminiConstants.CONTENT_TYPE_JSON))
           || ("json".equals(context.query().get("format", "")))
           );
  }
  
  /**
   * Sends an error to the client as a JSON-encoded map.  Gemini specifies 
   * that JSON error responses contain at least four fields (additional fields
   * may be added to the map using the "ancillary" parameter).
   *   <p>
   * 
   * <ul>
   *   <li>error - A basic name for the error.</li>
   *   <li>errorType - A classification of the error, such as "exception" or
   *       "authentication".  Should be a single word.</li>
   *   <li>errorMessage - A message suitable for rendering to a browser 
   *       client.</li>
   *   <li>errorDescription - Any ancillary information suitable for rendering
   *       to a browser client.  If not provided, the words "No detail 
   *       available" will be provided in the map.</li>
   *   <li>ancillary - Any additional entries to add into the response map.
   * </ul>
   */
  public static boolean sendJsonError(Context context, String error, 
      String errorType, String errorMessage, String errorDescription, 
      Map<String, String> ancillary)
  {
    // Allocate the map with a little more space in case the calling code
    // wants to add some other items.
    final Map<String, String> map = new HashMap<>(6);
    
    map.put("error", error);
    map.put("errorType", errorType);
    map.put("errorMessage", errorMessage);
    map.put("errorDescription", StringHelper.isNonEmpty(errorDescription) ? errorDescription : "No detail available.");
    
    // Copy the ancillary map.
    if (ancillary != null)
    {
      for (Map.Entry<String, String> entry : ancillary.entrySet())
      {
        map.put(entry.getKey(), entry.getValue());
      }
    }
    
    // Set the HTTP response to "server error."
    context.setStatus(500);
    
    return sendJson(context, map);
  }
  
  //
  // Protected Methods
  //
  
  //
  // Member methods.
  //
  
  /**
   * Sonar-recommended private constructor.  Really, Sonar?
   *   <p>
   * Yes, you may not instantiate a GeminiHelper object.
   */
  private GeminiHelper()
  {
    // Does nothing.
  }

} // End GeminiHelper
