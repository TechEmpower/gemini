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

import com.techempower.gemini.*;
import com.techempower.log.*;
import com.techempower.util.*;

/**
 * A basic implementation of the FeatureManager interface.  The Basic 
 * implementation cannot respond with any authority to "on" requests until
 * it has been configured.  If your application makes a call to the "on"
 * method prior to configuration, false will be returned as a fail-safe
 * measure.
 */
public class BasicFeatureManager
  implements FeatureManager,
             Configurable
{

  //
  // Variables.
  //
  
  private final ComponentLog             log;
  private final Map<String, FeatureNode> nodes;
  private final List<FeatureNode>        roots;
  
  private       Map<String, Boolean>     status;
  
  //
  // Methods.
  //
  
  /**
   * Constructor.
   */
  public BasicFeatureManager(GeminiApplication application)
  {
    this.log = application.getLog("fMgr");
    this.status = new HashMap<>();
    this.nodes = new HashMap<>();
    this.roots = new ArrayList<>(2);
    
    // Let's get configured.
    application.getConfigurator().addConfigurable(this);
  }
  
  @Override
  public FeatureNode add(String featureKey, String description)
  {
    return add(featureKey, description, true);
  }

  @Override
  public FeatureNode add(String featureKey, String description, boolean defaultState)
  {
    FeatureNode toReturn = new BasicFeatureNode(featureKey, description, defaultState, this, null);
    this.roots.add(toReturn);
    return toReturn;
  }

  @Override
  public FeatureNode get(String featureKey)
  {
    FeatureNode toReturn = this.nodes.get(featureKey);
    if (toReturn != null)
    {
      return toReturn;
    }
  
    // Default case.
    return null;
  }

  @Override
  public void set(String featureKey, boolean enabled)
  {
    // Find the feature.
    FeatureNode toChange = this.nodes.get(featureKey);
    if (toChange != null)
    {
      toChange.setEnabled(enabled);
      
      // Reset the status map so that child nodes are effected by this change.
      resetStatusMap();
    }
  }

  @Override
  public boolean on(String featureKey)
  {
    Boolean toReturn = this.status.get(featureKey);
    if (toReturn != null)
    {
      return toReturn;
    }
    
    // Default case.
    return false;
  }
  
  @Override
  public List<FeatureNode> getRoots()
  {
    return new ArrayList<>(this.roots);
  }

  /**
   * Captures a FeatureNode within the flattened maps.
   */
  protected void enroll(BasicFeatureNode node)
  {
    // Being asked to enroll a node that we are already aware of.  Let's throw
    // an exception.
    if (this.nodes.get(node.getKey()) != null)
    {
      throw new IllegalArgumentException("Feature [" + node.getKey() + "] is already in the hierarchy.");
    }
    
    this.nodes.put(node.getKey(), node);
    this.status.put(node.getKey(), node.isFullyEnabled());
  }
  
  /**
   * Computes a portion of the flattened status map. 
   */
  protected void buildStatusMap(Map<String, Boolean> statusMap, 
      boolean enabled, Iterator<FeatureNode> iter)
  {
    FeatureNode node;
    while (iter.hasNext())
    {
      node = iter.next();
      statusMap.put(node.getKey(), enabled && node.isEnabled());
      buildStatusMap(statusMap, enabled && node.isEnabled(), node.getChildren());
    }
  }
  
  /**
   * Resets/recomputes the flattened status map.
   */
  protected void resetStatusMap()
  {
    // Create a new status map.
    HashMap<String, Boolean> newStatus = new HashMap<>();
    buildStatusMap(newStatus, true, this.roots.iterator());
    
    // Replace the main status map reference with the new one.
    this.status = newStatus;
  }
  
  /**
   * Builds a "configuration-file" style rendering of the current 
   * running configuration (including specific node status).
   */
  public String getConfiguration()
  {
    StringBuilder toReturn = new StringBuilder(5000);
    
    // Add all of the roots and their children.
    for (FeatureNode root : this.roots)
    {
      renderConfiguration(toReturn, "Feature.", root);
    }
    
    return toReturn.toString();
  }
  
  /**
   * Produces a rendering for a specific node using its specific enabled
   * status.
   */
  public void renderConfiguration(StringBuilder buffer, String prefix, FeatureNode node)
  {
    // Add this node.
    buffer.append(prefix)
          .append(node.getKey())
          .append(" = ")
          .append(node.isEnabled() ? "On" : "Off")
          .append(UtilityConstants.CRLF);

    // Add children.
    Iterator<FeatureNode> iter = node.getChildren();
    while (iter.hasNext())
    {
      renderConfiguration(buffer, prefix + node.getKey() + ".", iter.next());
    }
  }
  
  /**
   * Dumps the current configuration status, collapsing the parents' enabled 
   * status.  This is useful only for debugging/logging purposes as it will
   * hide the specific configuration settings of children of disabled nodes.
   */
  public String getCollapsedConfiguration()
  {
    StringBuilder toReturn = new StringBuilder(5000);
    
    // Go through all of the nodes.
    for (FeatureNode node : this.nodes.values())
    {
      toReturn.append("Feature.")
              .append(node.getKey())
              .append(" = ")
              .append(this.status.get(node.getKey()) ? "On" : "Off")
              .append(UtilityConstants.CRLF);
    }
    
    return toReturn.toString();
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    this.log.log("Configuring features.");
    
    String key;
    String[] nodeArray;
    
    EnhancedProperties features = props.extractProperties("Feature.", true);
    final Set<String> keys = features.names();
    for (String originalKey : keys)
    {
      //this.log.debug("Full key: " + originalKey);
      
      // If the configuration key includes parent nodes (optional), get the
      // last node.
      if (originalKey.indexOf('.') >= 0)
      {
        nodeArray = originalKey.split("\\.");
        key = nodeArray[nodeArray.length - 1];
      }
      else
      {
        key = originalKey;
      }
      //this.log.debug("Unique key: " + key);
      
      // See if we have that key in our hierarchy.
      FeatureNode node = get(key);
      if (node != null)
      {
        boolean enabled = features.getBoolean(originalKey);
        node.setEnabled(enabled);
      }
      else
      {
        this.log.log("Feature not found: " + key);
      }
    }
    
    resetStatusMap();
  }

}
