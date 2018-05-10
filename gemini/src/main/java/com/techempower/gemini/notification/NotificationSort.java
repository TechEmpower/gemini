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
package com.techempower.gemini.notification;

import java.util.*;

/**
 * Some static comparators for Notifications.
 */
public final class NotificationSort
{

  /**
   * Compare Notifications by their date in ascending order.
   */
  public static final Comparator<Notification> DATE = new Comparator<Notification>() 
  {
    @Override
    public int compare(Notification o1, Notification o2)
    {
      return o1.getTime().compareTo(o2.getTime());
    }
  };
  
  /**
   * Compare Notifications by their date in descending order.
   */
  public static final Comparator<Notification> DATE_DESC = new Comparator<Notification>() 
  {
    @Override
    public int compare(Notification o1, Notification o2)
    {
      return o2.getTime().compareTo(o1.getTime());
    }
  };
  
  /**
   * Compare Notifications by their severity in descending order.
   */
  public static final Comparator<Notification> SEVERITY_DESC = new Comparator<Notification>() 
  {
    @Override
    public int compare(Notification o1, Notification o2)
    {
      return o2.getSeverity().ordinal() - o1.getSeverity().ordinal();
    }
  };
  
  /**
   * No constructor.
   */
  private NotificationSort()
  {
    // Does nothing.
  }
  
}
