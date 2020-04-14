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
package com.techempower.gemini.input;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.techempower.gemini.*;
import com.techempower.gemini.context.*;
import com.techempower.helper.*;
import com.techempower.js.legacy.*;

/**
 * A representation of input, composed of the user-provided values and any
 * validation errors raised by validators.  This is not instantiated directly
 * by applications, but rather provided as a result of processing a set of
 * validators via an instance of ValidatorSet. 
 */
public class Input
  implements JavaScriptObject
{
  //
  // Variables.
  //

  private final BasicContext context;
  private final Query         query;
  private int                 statusCode = 400;
  private List<String>        errorMessages;
  private Map<String, Object> erroredElements;
  private Object              auxiliary;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  protected Input(BasicContext context)
  {
    this.context = context;
    this.query = context.query();
  }
  
  /**
   * Gets the input values.
   */
  public Query values()
  {
    return query;
  }
  
  /**
   * Gets the Context.
   */
  public BasicContext context()
  {
    return context;
  }
  
  /**
   * Gets the list of error messages.
   */
  @JsonProperty("errors")
  public List<String> errors()
  {
    return errorMessages;
  }
  
  /**
   * Gets the map of elements in error.
   */
  @JsonProperty("elements")
  public Map<String, Object> erroredElements()
  {
    return erroredElements;
  }
  
  /**
   * Adds an error that is not tied to a specific element.
   * 
   * @param errorMessage is typically a String, but can optionally be any
   *        other type that can be serialized to JSON and rendered in a 
   *        template.
   */
  public Input addError(final String errorMessage)
  {
    if (errorMessages == null)
    {
      errorMessages = new ArrayList<>(10);
    }
    
    errorMessages.add(errorMessage);
    
    return this;
  }
  
  /**
   * Adds an element that is in error.  If an element is already in error
   * for another reason (already present in this Result), the new message
   * will still be added.
   * 
   * @param errorMessage A message describing the validation error.
   */
  public Input addError(final String element, final String errorMessage)
  {
    return addError(element, errorMessage, true);
  }
  
  /**
   * Adds an element that is in error.
   * 
   * @param element the element's identity string/name.
   * @param errorMessage A message describing the validation error.
   * @param addIfElementAlreadyPresent If the element is already in error
   *        for some other reason (already present in this Result), should
   *        this new message be added?
   */
  public Input addError(
      final String element,
      final String errorMessage,
      final boolean addIfElementAlreadyPresent)
  {
    if (erroredElements == null)
    {
      erroredElements = new HashMap<>(10);
    }

    // Capture single errors per element as plain Strings, and promote
    // multiple errors per element into Lists of Strings.
    final Object contained = erroredElements.get(element);
    if (contained == null)
    {
      erroredElements.put(element, errorMessage);
    }
    else if (  (contained instanceof String)
            && (addIfElementAlreadyPresent)
            )
    {
      final List<String> list = new ArrayList<>(10);
      list.add((String)contained);
      list.add(errorMessage);
      erroredElements.put(element, list);
    }
    else if (  (contained instanceof List<?>)
            && (addIfElementAlreadyPresent)
            )
    {
      @SuppressWarnings("unchecked")
      final List<String> list = (List<String>)contained;
      list.add(errorMessage);
    }

    addError(errorMessage);
    
    return this;
  }
  
  /**
   * Sets the HTTP status code to something specific.  The default is 400
   * ("bad request").
   */
  public Input setStatusCode(int statusCode)
  {
    this.statusCode = statusCode;
    return this;
  }
  
  /**
   * Gets the HTTP status code.  The default is 400 ("bad request").
   */
  @JsonIgnore
  public int getStatusCode()
  {
    return this.statusCode;
  }
  
  /**
   * Are there no errors?
   */
  public boolean passed()
  {
    return (errorMessages == null);
  }
  
  /**
   * Are there any errors?
   */
  public boolean failed()
  {
    return (!passed());
  }
  
  @Override
  public String toString()
  {
    return "Validation.Result ["
        + (passed() ? "Good; " : "Bad; ")
        + CollectionHelper.toString(errorMessages, ";")
        + "]";
  }

  /**
   * Return the auxiliary object reference.
   */
  @JsonProperty("aux")
  public Object getAuxiliary()
  {
    return auxiliary;
  }

  /**
   * Set the auxiliary object reference.
   */
  public Input setAuxiliary(final Object auxiliary)
  {
    this.auxiliary = auxiliary;
    
    return this;
  }

  private static final VisitorFactory<Input> VISITOR_FACTORY = 
      new VisitorFactory<Input>()
  {
    @Override
    public Visitor visitor(Input result)
    {
      return Visitors.map(
          "errors", result.errors(),
          "elements", result.erroredElements(),
          "aux", result.getAuxiliary()
          );
    }
  };
  
  /**
   * Get a VisitorFactory for writing Validation Results to JavaScript.
   */
  @Override
  @JsonIgnore
  public VisitorFactory<Input> getJsVisitorFactory()
  {
    return VISITOR_FACTORY;
  }

}