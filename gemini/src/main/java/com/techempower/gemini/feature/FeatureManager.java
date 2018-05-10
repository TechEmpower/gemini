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
 * Manages "Features" of an application, allowing macro-level (or micro-level)
 * functionality (or "features") to be disabled/enabled at runtime or 
 * configure-time.
 *   <p>
 * The basic idea is that a hierarchical tree of functionality is created
 * at instantiation-time.  A running instance of an application builds
 * a matching structure of enabled/disabled flags for each feature at 
 * run-time.  Any features that can be disabled using this mechanism simply 
 * should check to see if they are enabled before acting by using the "on" 
 * method.  Disabling a parent in the tree will have the effect of disabling
 * all of the children.
 *   <p>
 * Features are identified by a unique "key" (or name) that is used "if" 
 * statements to only enter code blocks when the feature and its parent 
 * features are enabled.  Keys are case-sensitive and must be unique.  The
 * hierarchy is simply for readability in a configuration file, but when
 * checking the enabled status with the "on" method, the hierarchy is not
 * used.  So make sure your keys are unique!
 *   <p>
 * Note that it is not expected that a FeatureManager be thread-safe.  That
 * is, the "add" methods that build the feature tree should be called at
 * instantiation time and once created, the tree should be considered static.
 * However, reconfiguration can occur at any time.
 */
public interface FeatureManager
{

  /**
   * Add a root-level feature, which is considered to be enabled by default.
   * The newly added FeatureNode is returned.
   * 
   * @param featureKey an identifier for the feature.  Keys are case-sensitive.
   * @param description a brief description; a few words to a sentence.
   */
  FeatureNode add(String featureKey, String description);
  
  /**
   * Add a root-level feature.  The newly added FeatureNode is returned.
   * 
   * @param featureKey an identifier for the feature.  Keys are case-sensitive.
   * @param description a brief description; a few words to a sentence.
   * @param defaultState true for enabled; false for disabled.
   */
  FeatureNode add(String featureKey, String description, boolean defaultState);
  
  /**
   * Gets a FeatureNode for a specified feature key, anywhere in the feature
   * hierarchy.  This method should only be used to build the feature 
   * hierarchy and not during the run-time of the application.  At run-time,
   * only the on() method should be used.  Returns null if no matching
   * FeatureNode can be found.
   * 
   * @param featureKey an identifier for the feature.  Keys are case-sensitive.
   */
  FeatureNode get(String featureKey);
  
  /**
   * Sets the enabled status for a specific feature, anywhere in the feature
   * hierarchy.  If the feature is disabled, all child nodes will be 
   * effectively disabled (via future calls to the "on" method) regardless
   * of their specific status.  If the feature is enabled, the specific
   * status of the child nodes will be used.
   *   <p>
   * The set method does not add features to the hierarchy; rather it simply
   * allows run-time changes to their status (enabled/disabled).
   */
  void set(String featureKey, boolean enabled);
  
  /**
   * Determines if a feature is enabled or "on".  This should return true only
   * if the feature in question and all of its parent features are enabled.
   * 
   * @param featureKey an identifier for the feature.  Keys are case-sensitive.
   */
  boolean on(String featureKey);
  
  /**
   * Gets an iterator over the root FeatureNodes.  This is for monitoring and
   * run-time configuration changes within an administrative user interface.
   */
  List<FeatureNode> getRoots();
  
}
