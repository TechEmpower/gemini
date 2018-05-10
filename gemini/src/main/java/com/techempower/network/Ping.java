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

package com.techempower.network;

import java.io.*;
import java.net.*;
import java.util.*;

import com.techempower.util.*;

/**
 * Makes for a very simple means to ping IP addresses to determine if they
 * are reachable or not.  In fact, this is just a simplification on the
 * Java 1.5 java.net.InetAddress.isReachable method.
 */
public final class Ping
{
  
  //
  // Constants.
  //
  
  private static final int DEFAULT_TIMEOUT = (int)UtilityConstants.SECOND;
  private static final String DEFAULT_NAME = "eth0";

  //
  // Static variables.
  //
  
  private static volatile List<NetworkInterface> interfaces;
  private static volatile NetworkInterface       defaultInterface;
  
  //
  // Static methods.
  //
  
  /**
   * Initializes the references to network interfaces.
   */
  private static void initialize()
  {
    if (interfaces == null)
    {
      ArrayList<NetworkInterface> in = new ArrayList<>();
      try
      {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        NetworkInterface currentInterface;
        while (enumeration.hasMoreElements())
        {
          currentInterface = enumeration.nextElement();
          //System.out.println("Interface: " + currentInterface.getDisplayName() + "; " + currentInterface.getName());
          
          // If the interface is named eth0, we'll assume that this is the
          // default interface.
          if (DEFAULT_NAME.equalsIgnoreCase(currentInterface.getName()))
          {
            defaultInterface = currentInterface;
          }
          
          in.add(currentInterface);
        }
      }
      catch (SocketException soctetexc)
      {
        // Not sure why this would happen.  Do nothing.
      }
      interfaces = in;
    }
    
    // If nothing was named eht0, we'll just use the first interface as the
    // default.
    if ( (defaultInterface == null) 
      && (interfaces.size() > 0)
      )
    {
      defaultInterface = interfaces.get(0);
    }
  }
  
  /**
   * Attempt to ping an address, using the default network interface and a 
   * timeout of 1 second.  Returns true if successful, false otherwise.
   */
  public static boolean ping(String ip)
  {
    return ping(getDefaultNetworkInterface(), ip, DEFAULT_TIMEOUT);
  }
  
  /**
   * Attempt to ping an address using the default network interface.  Returns
   * true if successful, false otherwise.
   */
  public static boolean ping(String ip, int timeoutMilliseconds)
  {
    return ping(getDefaultNetworkInterface(), ip, timeoutMilliseconds);
  }
  
  /**
   * Attempt to ping an address, using a timeout of 1 second.  Returns true
   * if successful, false otherwise.
   */
  public static boolean ping(NetworkInterface networkInterface, String ip)
  {
    return ping(networkInterface, ip, DEFAULT_TIMEOUT);
  }
  
  /**
   * Attempt to ping an address.  Returns true if successful, false otherwise.
   */
  public static boolean ping(NetworkInterface networkInterface, String ip, int timeoutMilliseconds)
  {
    try
    {
      initialize();
      InetAddress address = InetAddress.getByName(ip);
      return address.isReachable(networkInterface, 0, timeoutMilliseconds);
    }
    catch (IOException ioexc)
    {
      // Do nothing.  We'll return false.
    }
    
    return false;
  }
  
  /**
   * Gets the Network Interfaces.
   */
  public static List<NetworkInterface> getNetworkInterfaces()
  {
    initialize();
    return interfaces;
  }
  
  /**
   * Gets the default Network Interface.
   */
  public static NetworkInterface getDefaultNetworkInterface()
  {
    initialize();
    return defaultInterface;
  }
 
  /*
  // Test method
  public static void main(String[] args)
  {
    ArrayList interfaces = getNetworkInterfaces();
    for (int i = 0; i < interfaces.size(); i++)
    {
      NetworkInterface iface = (NetworkInterface)interfaces.get(i);
      System.out.println("Interface: " + iface.getDisplayName() + "; " + iface.getName());
      System.out.println("65.115.126.10: " + ping(iface, "65.115.126.10"));
    }
    System.out.println("65.115.126.10: " + ping("65.115.126.10"));
    System.out.println("127.0.0.1: " + ping("127.0.0.1"));
    System.out.println("10.1.2.3: " + ping("10.1.2.3"));
  }
  */
  
  /**
   * You may not instantiate this class.
   */
  private Ping()
  {
    // Does nothing.
  }
  
}  // End Ping.
