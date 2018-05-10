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

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import com.techempower.util.*;

/**
 * NetworkHelper provides utility functions for web applications. Although
 * these helper functions are useful for Gemini applications, this class is
 * not Gemini-specific.
 */
public final class NetworkHelper
{

  //
  // Static variables.
  //

  private static final String[] HTML_ESCAPE_FINDS = new String[] {
      "<", ">", "\"", "&", "\'"
  };

  private static final String[] HTML_ESCAPE_REPLACES = new String[] {
      "&lt;", "&gt;", "&#034;", "&amp;", "&#039;"
  };
  
  // A simple mapping of common HTML entities to plaintext suitable for use
  // in e-mail.
  private static final String[] HTML_COMMON_ENTITIES_FINDS = new String[] {
      "&nbsp;", "&quot;", "&rdquo;", "&ldquo;", "&rsquo;", "&lsquo;", "&amp;"
  };

  private static final String[] HTML_COMMON_ENTITIES_REPLACES = new String[] {
      " ", "\"", "\"", "\"", "'", "'", "&"
  };
  
  // Conversion of line-breaks from plaintext to <br> HTML tags.
  private static final String[] HTML_LINEBREAK_FINDS = new String[] {
      "\r\n", "\n", "\r"
  };
  
  private static final String[] HTML_LINEBREAK_REPLACES = new String[] {
      "<br/>", "<br/>", "<br/>"
  };

  /**
   * The regular expression defined by the WHATWG HTML specification for email
   * address validation.
   *
   * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/#valid-e-mail-address">valid e-mail address</a>
   */
  private static final Pattern HTML5_EMAIL_PATTERN = Pattern.compile(
      "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,253}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,253}[a-zA-Z0-9])?)*$");

  /**
   * This is per RFC 5322.
   */
  public static final IntRange EMAIL_ADDRESS_LENGTH = new IntRange(3, 
      254);

  //
  // Static methods.
  //

  /**
   * Determines if a provided String is a web URL (that is, whether it
   * starts with http: or https:).
   */
  public static boolean isWebUrl(String possibleUrl)
  {
    if (StringHelper.isNonEmpty(possibleUrl))
    {
      String lowercase = possibleUrl.toLowerCase();
      return ((lowercase.startsWith("http:")) || (lowercase
          .startsWith("https:")));
    }

    return false;
  }

  /**
   * Converts an IPv4 IP address (32-bit) to a 32-bit int.
   */
  public static int convertIpv4ToInt(String ip)
  {
    if (StringHelper.isNonEmpty(ip))
    {
      StringTokenizer tokens = new StringTokenizer(ip, ".");

      int firstOctet = NumberHelper.boundInteger(NumberHelper
          .parseInt(tokens.nextToken()), 0, 255);
      int secondOctet = NumberHelper.boundInteger(NumberHelper
          .parseInt(tokens.nextToken()), 0, 255);
      int thirdOctet = NumberHelper.boundInteger(NumberHelper
          .parseInt(tokens.nextToken()), 0, 255);
      int fourthOctet = NumberHelper.boundInteger(NumberHelper
          .parseInt(tokens.nextToken()), 0, 255);

      int result = fourthOctet;
      result += thirdOctet << 8;
      result += secondOctet << 16;
      result += firstOctet << 24;

      return result;
    }

    return 0;
  }

  /**
   * Converts a 32-bit int to a 32-bit IPv4 IP address.
   */
  public static String convertIntToIpv4(int ip)
  {
    int firstOctet, secondOctet, thirdOctet, fourthOctet, work = ip;

    firstOctet = (work >> 24) & 255;
    work = (work & 16777215);
    secondOctet = (work >> 16) & 255;
    work = (work & 65535);
    thirdOctet = (work >> 8) & 255;
    work = (work & 255);
    fourthOctet = (work) & 255;

    return firstOctet + "." + secondOctet + "." + thirdOctet + "."
        + fourthOctet;
  }

  /**
   * Decode common HTML entities for rendering a String as plaintext.  This
   * does not strip HTML tags, but rather replaces entities such as &nbsp;
   * with a space character.
   *   <p>
   * Note that the list of entities processed is quite short and this
   * definitely does not process numeric entities.
   * 
   * @param inputString source text that may contain some common HTML 
   *        entities.
   */
  public static String decodeCommonEntities(String inputString)
  {
    return StringHelper.replaceSubstrings(inputString, 
        HTML_COMMON_ENTITIES_FINDS, 
        HTML_COMMON_ENTITIES_REPLACES);
  }
  
  /**
   * Escapes double-quotes as &quot for HTML. If a null String is provided
   * as a parameter, an empty String is returned.
   * 
   * @param inputString
   *          a String needing some double quotes escaped
   * @return a String with the double quotes escaped--woohoo!
   */
  public static String escapeDoubleQuotesForHtml(String inputString)
  {
    return StringHelper.replaceSubstrings(inputString, "\"", "&quot;");
  }

  /**
   * Renders an untrusted String value for rendering directly within an HTML
   * response.  If a null String is provided as an input parameter, an empty
   * String is returned. The following replacements are made:
   * 
   * <ul>
   * <li><code>&amp;gt;</code> for for greater-than signs</li>
   * <li><code>&amp;lt;</code> for less-than signs</li>
   * <li><code>&amp;#034;</code> for plaintext double-quotes</li>
   * <li><code>&amp;039;</code> for plaintext single-quotes</li>
   * <li><code>&amp;amp;</code> for ampersands</li>
   * </ul>
   * 
   * @param untrustedString a String of content that may be user-provided but
   *        in any event is untrusted; meaning its content may contain HTML
   *        markup that needs to be escaped before rendering in an HTML
   *        response document. 
   */
  public static String render(String untrustedString)
  {
    return StringHelper.replaceSubstrings(untrustedString, 
        HTML_ESCAPE_FINDS,
        HTML_ESCAPE_REPLACES);
  }

  /**
   * Escapes text for HTML usage. <strong>All spaces are made into
   * non-breaking spaces (<code>&amp;nbsp;</code>).</strong> If a null
   * String is provided as an input parameter, an empty String is returned.
   * The following replacements are made:
   * 
   * <ul>
   * <li><code>&amp;#039;</code> for '</li>
   * <li><code>&amp;gt;</code> for ></li>
   * <li><code>&amp;lt;</code> for <</li>
   * <li><code>&amp;#034;</code> for \</li>
   * <li><code>&amp;amp;</code> for ampersands</li>
   * <li><strong><code>&amp;nbsp;</code> for spaces, tabs, and line
   * breaks</strong></li>
   * </ul>
   * 
   * @param untrustedString a String of content that may be user-provided but
   *        in any event is untrusted; meaning its content may contain HTML
   *        markup that needs to be escaped before rendering in an HTML
   *        response document. 
   */
  public static String escapeNonbreakingStringForHtml(String untrustedString)
  {
    return render(untrustedString).replaceAll("\\s", "&nbsp;");
  }

  /**
   * Strips all the html tags from a string. If a null String is provided as
   * an input parameter, an empty String is returned.
   * 
   * @return a string devoid of html tags
   */
  public static String stripHtmlTags(String inputString)
  {
    return inputString == null ? "" : inputString.replaceAll("\\<(.|\\n|\\r)*?\\>",
        "");
  }

  /**
   * Encodes a String using the Java standard library's URL encoding and 
   * UTF-8.  This should be used only on the portion of the URL past the
   * protocol (e.g., past "https://").
   *   <p>
   * Returns null if provided a null URL.
   */
  public static String encodeUrl(String inputUrl)
  {
    if (StringHelper.isNonEmpty(inputUrl))
    {
      try
      {
        return java.net.URLEncoder.encode(inputUrl, StandardCharsets.UTF_8.name());
      }
      catch (UnsupportedEncodingException uexc)
      {
        // Shouldn't happen.
      }
    }
    
    return inputUrl;
  }
  
  /**
   * Decodes a String using the Java standard library's URL decoding and 
   * UTF-8.  This should be used only on the portion of the URL past the
   * protocol (e.g., past "https://").
   *   <p>
   * Returns null if provided a null URL.
   *
   * @throws IllegalArgumentException if the argument is an illegal String,
   * most notably if the argument contains a '%' (character interpreted as
   * the start of a special escaped sequence) not in the form of the
   * sequence '%xy' where xy is treated as its two-digit hexadecimal
   * representation. Ex. %22 translates to '"', %25 to '%', %2B to '+', etc.
   *
   * @see "http://docs.oracle.com/javase/6/docs/api/java/net/URLDecoder.html"
   */
  public static String decodeUrl(String inputUrl)
      throws IllegalArgumentException
  {
    if (StringHelper.isNonEmpty(inputUrl))
    {
      try
      {
        return java.net.URLDecoder.decode(inputUrl, StandardCharsets.UTF_8.name());
      }
      catch (UnsupportedEncodingException uexc)
      {
        // Shouldn't happen.
      }
    }
    
    return inputUrl;
  }
  
  /**
   * A rather simple URL formatting method that generates full URLs from
   * incompletely formatted URLs. E.g., www.techempower.com would become
   * http://www.techempower.com/
   * <p>
   * This method assumes that the URL past the protocol prefix has already
   * been escaped.
   *    <p>
   * If the input URL is an empty string or null, an empty string will be
   * returned.
   */
  public static String formatUrl(String inputUrl)
  {
    if (inputUrl != null)
    {
      if (inputUrl.length() > 0)
      {
        StringBuilder newUrl = new StringBuilder(inputUrl.length() + 8);
        newUrl.append(inputUrl);

        // Add http:// if necessary.
        if ((!inputUrl.startsWith("http://"))
            && (!inputUrl.startsWith("ftp://")))
        {
          newUrl.insert(0, "http://");
        }

        // Adds trailing / if no file is specified.
        if (newUrl.toString().substring(7).indexOf('/') == -1)
        {
          newUrl.append("/");
        }

        return newUrl.toString();
      }

      // input was zero length.
      return inputUrl;
    }

    // input was null
    return "";
  }

  /**
   * Gets a domain name from a full URL. http://techempower.com/contact
   * becomes techempower.com
   * 
   * @param url
   *          The full URL
   * @return just the domain name
   */
  public static String getDomainFromURL(String url)
  {
    if (StringHelper.isNonEmpty(url))
    {
      int doubleSlashPos = url.indexOf("//");

      if (doubleSlashPos >= 0)
      {
        try
        {
          String sansProtocol = url.substring(doubleSlashPos + 2);
          int nextSlashPos = sansProtocol.indexOf('/');
          if (nextSlashPos > 0)
          {
            return sansProtocol.substring(0, nextSlashPos);
          } 
          else
          {
            return sansProtocol;
          }
        }
        catch (ArrayIndexOutOfBoundsException aioobexc)
        {
          // Do nothing.
        }
      }
    }

    return "";
  }
  
  /**
   * Converts a provided String to a URL slug.  E.g., "Hello, World!" would be
   * converted to "hello-world".  This process converts all sequences of
   * non-alphanumeric characters to at most one hyphen (minus), and removes
   * any leading and trailing non-alphanumeric characters.
   *   <p>
   * Returns null if the parameter is null.  Returns an empty string if all
   * characters are non-alphanumeric.
   */
  public static String convertToSlug(String input)
  {
    if (input == null)
    {
      return null;
    }
    
    final StringBuilder result = new StringBuilder(input.length());
    boolean lastCharMinus = true;
    for (int i = 0; i < input.length(); i++)
    {
      final char ch = input.charAt(i);
      if (Character.isLetterOrDigit(ch))
      {
        lastCharMinus = false;
        result.append(Character.toLowerCase(ch));
      }
      else
      {
        if (!lastCharMinus)
        {
          // Append a minus for this block of one or more non-alphanumeric
          // characters.
          lastCharMinus = true;
          result.append("-");
        }
      }
    }
    
    // Trim off a trailing minus if needed.
    if (  (lastCharMinus)
       && (result.length() > 0)
       )
    {
      result.deleteCharAt(result.length() - 1);
    }
    
    return result.toString();
  }  

  /**
   * Replaces spaces with HTML non-breaking spaces (&amp;nbsp; HTML
   * entities).
   * 
   * @param source original text
   * @return text with spaces as non-breaking spaces
   */
  public static String convertSpacesToNonBreaking(String source)
  {
    if (source == null)
    {
      return null;
    }

    return StringHelper.replaceSubstrings(source, " ", "&nbsp;");
  }
  
  /**
   * Render a user-provided untrusted String for use in HTML and convert
   * plaintext line-break to HTML [br] tags.
   */
  public static String renderWithLinebreaks(String source)
  {
    return convertPlaintextLinebreaksToHtml(render(source));
  }
  
  /**
   * Render a user-provided untrusted String for use in HTML and convert
   * plaintext line-break to HTML [br] tags, optionally trimming excess 
   * line-breaks.
   * 
   * @param trimExcessBlankLines If true, allow only up to two [br] tags in
   *        a row.
   */
  public static String renderWithLinebreaks(String source, 
      boolean trimExcessBlankLines)
  {
    if (trimExcessBlankLines)
    {
      String trimmed = StringHelper.join("\n", 
          StringHelper.trimExcessBlankLines(
              StringHelper.splitIntoLines(source)));
      
      return convertPlaintextLinebreaksToHtml(render(trimmed));
    }
    else
    {
      return convertPlaintextLinebreaksToHtml(render(source));
    }
  }
  
  /**
   * Converts plaintext line-breaks to HTML [br] tags.
   */
  public static String convertPlaintextLinebreaksToHtml(String source)
  {
    return StringHelper.replaceSubstrings(source, 
        HTML_LINEBREAK_FINDS, 
        HTML_LINEBREAK_REPLACES);
  }

  /**
   * Validates that an email address is an email address according to the
   * definition used by HTML5 in validating input elements with type email.
   * Note that this differs from RFC 5322.
   * 
   * @param email
   *          The email address to validate
   * @return <tt>true</tt> if and only if the email address is valid
   * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/#valid-e-mail-address">valid e-mail address</a>
   */
  public static boolean validateHTML5EmailAddress(String email)
  {
    return email != null && HTML5_EMAIL_PATTERN.matcher(email).matches();
  }

  /**
   * Generates a simple query string given the arrays of param keys and values.
   * The key/values pairs are separated by "&".
   *
   * The returned query string does -not- start with either "?" or "&".
   * 
   * @param paramKeys An array of additional param keys associated with the paramValues.
   * @param paramValues An array of param values associated with the paramKeys.
   */
  public static String getQueryString(String[] paramKeys, String[] paramValues)
  {
    StringBuilder sb = new StringBuilder();

    if (CollectionHelper.isNonEmpty(paramKeys) &&
        CollectionHelper.isNonEmpty(paramValues))
    {
      for (int i = 0; i < paramKeys.length && i < paramValues.length; i++)
      {
        if (sb.length() > 0)
        {
          sb.append("&");
        }

        sb.append(paramKeys[i]);
        sb.append("=");
        sb.append(encodeUrl(paramValues[i]));
      }
    }

    return sb.toString();
  }

  /**
   * You may not instantiate this class.
   */
  private NetworkHelper()
  {
    // Does nothing.
  }
  
} // End NetworkHelper.

