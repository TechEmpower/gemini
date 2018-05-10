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
package com.techempower.gemini.feature;

import java.util.*;

/**
 * Represents a node within the feature hierarchy.
 */
public interface FeatureNode
{

  /**
   * Gets the feature's key.
   */
  String getKey();
  
  /**
   * Gets the brief description (a few words to a sentence) describing the
   * feature.
   */
  String getDescription();
  
  /**
   * Is the feature specifically enabled (regardless of parent features)?
   */
  boolean isEnabled();
  
  /**
   * Sets the enabled flag.
   */
  void setEnabled(boolean enabled);
  
  /**
   * Adds a child feature.  The -current- FeatureNode (not the child that
   * was added) is returned.
   * 
   * @param featureKey an identifier for the feature.
   */
  FeatureNode add(String featureKey, String description);
  
  /**
   * Add a child feature.  The -current- FeatureNode (not the child that
   * was added) is returned.
   * 
   * @param featureKey an identifier for the feature.
   * @param defaultState true for enabled; false for disabled.
   */
  FeatureNode add(String featureKey, String description, boolean defaultState);
  
  /**
   * Gets an iterator over the child features.  Most not return null.  Return
   * a zero-element Iterator if there are no children.
   */
  Iterator<FeatureNode> getChildren();
  
}
