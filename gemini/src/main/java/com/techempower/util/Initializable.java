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
package com.techempower.util;

/**
 * An interface that indicates an entity is "initializable," and perhaps more
 * importantly allows client classes to know whether an instance has been
 * initialized.
 *   <p>
 * The principal use of this interface is the Auditable sub-interface (which
 * extends Initializable).  While Auditable entities are being instantiated, 
 * and have not yet has their initialized flag set to "true," the AuditManager
 * will <b>not</b> capture audits when set methods are called.  This allows
 * an external builder such as Entities to build an instance of an Auditable
 * and when finished calling a bunch of set methods, set the initialized
 * flag to "true."
 *   <p>
 * Once the initialized flag is true, then calls to an Auditable entity's
 * set methods <b>will</b> be captured with audits.
 *   <p>
 * To summarize, the expected sequence of events is:
 * <ol>
 *   <li>An Initializable object is instantiated/constructed.</li>
 *   <li>The object is configured by zero or more calls to set methods.</li>
 *   <li>The initialize method is called to indicate that the object is
 *       constructed and configured and ready to rock.</li>
 * </ol>
 */
public interface Initializable
{
  
  /**
   * Perform any initialization work that should execute <b>after</b>
   * the instance has been constructed and possibly configured by a bunch
   * of calls to set methods.  It is expected that as part of this process,
   * an "initialized" flag will be set to true, so that calls to isInitialized
   * will return true after the initialize method has completed.
   *   <p>
   * Aside from that, however, there are no specific expectations for what
   * happens inside an initialize method implementation.
   */
  void initialize();
  
  /**
   * Is the instance initialized?  Has the initialize method completed?
   */
  boolean isInitialized();

}
