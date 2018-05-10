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
 * A basic implementation of the FeatureNode interface.
 */
public class BasicFeatureNode
  implements FeatureNode
{

  //
  // Variables.
  //
  
  private final List<FeatureNode>   children;
  private final BasicFeatureManager manager;
  private final BasicFeatureNode    parent;

  private String                 key;
  private String                 description;
  private boolean                enabled;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public BasicFeatureNode(String key, String description, boolean enabled, 
      BasicFeatureManager manager, BasicFeatureNode parent)
  {
    setKey(key);
    setEnabled(enabled);
    setDescription(description);
    this.children = new ArrayList<>(2);
    this.manager = manager;
    this.parent = parent;
    manager.enroll(this);
  }
  
  @Override
  public String getKey()
  {
    return this.key;
  }

  /**
   * @param key the key to set
   */
  public void setKey(String key)
  {
    this.key = key;
  }

  @Override
  public String getDescription()
  {
    return this.description;
  }

  /**
   * @param enabled the enabled to set
   */
  @Override
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description)
  {
    this.description = description;
  }

  @Override
  public boolean isEnabled()
  {
    return this.enabled;
  }

  @Override
  public FeatureNode add(String featureKey, String featureDescription)
  {
    return add(featureKey, featureDescription, true);
  }

  @Override
  public FeatureNode add(String featureKey, String featureDescription, boolean defaultState)
  {
    this.children.add(new BasicFeatureNode(featureKey, featureDescription, defaultState, this.manager, this));
    return this;
  }

  @Override
  public Iterator<FeatureNode> getChildren()
  {
    return this.children.iterator();
  }
  
  /**
   * Are this node and all of its parent nodes enabled?
   */
  protected boolean isFullyEnabled()
  {
    // If we're disabled, let's return immediately.
    if (!isEnabled()) 
    {
      return false;
    }
    
    // Loop until we've reached the root.
    BasicFeatureNode node = this.parent;
    while (node != null)
    {
      if (!node.isEnabled()) 
      {
        return false;
      }
      node = node.parent;
    }
    
    // IF we got here, we're enabled.
    return true;
  }

}
