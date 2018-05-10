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

package com.techempower.gemini.messaging;

import java.io.*;

import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * A message to be shown to the user. Typically indicates an outcome of a
 * requested operation (the operation completed successfully, encountered
 * an error, etc.).
 *   <p>
 * Messages are stored in the session and displayed once to the user; once
 * displayed, they are removed from the session.
 *   <p>
 * Messages have a type: ERROR, SUCCESS, WARNING, or NORMAL. They can also 
 * have an HTML DOM id and CSS classes specified.
 */
public class Message
  implements Serializable
{
  private static final long serialVersionUID = 4979511634011145151L;

  /**
   * A DOM ID for the message (to use when rendering as HTML).
   */
  private final String        id;
  
  /**
   * A list of CSS classes to use when rendering as HTML.
   */
  private final StringList    classNames = new StringList(" ");
  
  /**
   * The message to display to the user
   */
  private final String        message;
  
  /**
   * The type of message
   */
  private final MessageType   type;

  /**
   * Create a new message to be displayed to the user.  The type of the 
   * message will be NORMAL and it will have a CSS class of normal.
   * 
   * @param message the message to show the user (will be escaped for HTML 
   *        when rendered)
   */
  public Message(String message)
  {
    super();
    this.id = null;
    this.message = message;
    this.type = MessageType.NORMAL;
    addClass(this.type.getClassName());
  }

  /**
   * Create a new message with a provided type. The message will get a CSS 
   * class depending on the type.
   * 
   * @param message the message to display to the user (will be escaped for 
   *        HTML when rendered)
   * @param type the type of the message
   */
  public Message(String message, MessageType type)
  {
    super();
    this.id = null;
    this.message = message;
    this.type = type;
    addClass(type.getClassName());
  }

  /**
   * Create a new message with a provided type. The message will get a CSS 
   * class depending on the type.
   * 
   * @param id the HTML DOM id for the message; leading or following spaces 
   *        will be trimmed off.
   * @param message the message to display to the user (will be escaped for 
   *        HTML when rendered)
   * @param type the type of the message
   */
  public Message(String id, String message, MessageType type)
  {
    super();
    this.id = StringHelper.trim(id);
    this.message = message;
    this.type = type;
    addClass(type.getClassName());
  }

  /**
   * Create a new message with a provided type. The message will NOT get a 
   * CSS class from the type. It will only have the CSS class specified.
   * 
   * @param message the message to display to the user (will be escaped for 
   *        HTML when rendered)
   * @param type the type of the message
   * @param className the CSS class name to give to this message
   */
  public Message(String message, MessageType type, String className)
  {
    super();
    this.id = null;
    this.message = message;
    this.type = type;
    this.classNames.add(className);
  }

  /**
   * Get the HTML DOM ID for this message.
   * 
   * @return the DOM id of the message.
   */
  public String getId()
  {
    return this.id;
  }

  /**
   * Get the names of the CSS classes for this message.
   * 
   * @return the classNames as a StringList (toString will render to a space-
   *         delimited String).
   */
  public StringList getClassNames()
  {
    return this.classNames;
  }

  /**
   * Add a CSS class name to the list.
   * 
   * @param className the name of the class to add
   */
  public void addClass(String className)
  {
    getClassNames().add(className);
  }

  /**
   * Get the message to display to the user.
   * 
   * @return the message
   */
  public String getMessage()
  {
    return this.message;
  }

  /**
   * Get the type of message.
   * 
   * @return the type
   */
  public MessageType getType()
  {
    return this.type;
  }

  /**
   * Render the message to HTML.
   * 
   * @param tagName name of a tag to enclose the message (e.g., "P" to make
   *        the message an HTML Paragraph).
   * @param renderInlineStyle render with inline CSS styling; not recommended.
   * 
   * @return the HTML element with the message
   */
  public String render(String tagName, boolean renderInlineStyle)
  {
    final StringBuilder buffer = new StringBuilder();
    
    buffer.append('<');
    buffer.append(tagName);
    if (StringHelper.isNonEmpty(this.id))
    {
      buffer.append(" id=\"");
      buffer.append(this.id);
      buffer.append('"');
    }
    
    if (  (this.classNames != null)
       && (this.classNames.size() > 0)
       )
    {
      buffer.append(" class=\"");
      buffer.append(this.classNames.toString());
      buffer.append('"');
    }
    
    if (renderInlineStyle && this.type != null)
    {
      buffer.append(" style=\"");
      buffer.append(this.type.getInlineStyle());
      buffer.append('"');
    }
    
    buffer.append('>');
    buffer.append(NetworkHelper.render(this.message));
    buffer.append("</");
    buffer.append(tagName);
    buffer.append('>');
    
    return buffer.toString();
  }

  /**
   * Render the message within a paragraph tag.
   * 
   * @param withInlineStyles use default CSS styles inline
   * @return the HTML element with the message
   */
  public String renderAsP(boolean withInlineStyles)
  {
    return render("p", withInlineStyles);
  }

  /**
   * Render the message within a paragraph tag.  No inline styling will be 
   * used.
   * 
   * @return the HTML element with the message
   */
  public String renderAsP()
  {
    return renderAsP(false);
  }

  /**
   * Render the message within a div tag.
   * 
   * @param withInlineStyles use default CSS styles inline
   * @return the HTML element with the message
   */
  public String renderAsDiv(boolean withInlineStyles)
  {
    return render("div", withInlineStyles);
  }

  /**
   * Render the message within a div tag.  No inline styling will be used.
   * @return the HTML element with the message
   */
  public String renderAsDiv()
  {
    return renderAsDiv(false);
  }

  /**
   * Render the message within a list item tag.
   * 
   * @param withInlineStyles use default CSS styles inline
   * @return the HTML element with the message
   */
  public String renderAsLi(boolean withInlineStyles)
  {
    return render("li", withInlineStyles);
  }

  /**
   * Render the message within a list item tag.  No inline styling will be 
   * used.
   * 
   * @return the HTML element with the message
   */
  public String renderAsLi()
  {
    return renderAsLi(false);
  }

  /**
   * Render the message within a span tag.
   * 
   * @param withInlineStyles use default CSS styles inline
   * @return the HTML element with the message
   */
  public String renderAsSpan(boolean withInlineStyles)
  {
    return render("span", withInlineStyles);
  }

  /**
   * Render the message within a span tag.  No inline styling will be used.
   * 
   * @return the HTML element with the message
   */
  public String renderAsSpan()
  {
    return renderAsSpan(false);
  }

  /**
   * Render the message within a table cell tag.
   * 
   * @param withInlineStyles use default CSS styles inline
   * @return the HTML element with the message
   */
  public String renderAsTd(boolean withInlineStyles)
  {
    return render("td", withInlineStyles);
  }

  /**
   * Render the message within a table cell tag.  No inline styling will be 
   * used.
   * 
   * @return the HTML element with the message
   */
  public String renderAsTd()
  {
    return renderAsTd(false);
  }

  @Override
  public String toString()
  {
    return renderAsP();
  }

}
