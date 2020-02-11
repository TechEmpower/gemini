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
package com.techempower.gemini.lifecycle;

import com.techempower.gemini.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays a start-up banner to the log.  Includes basic information about 
 * the Java virtual machine version, operating system, and the application 
 * itself.
 */
public class InitDisplayEnvironment
  implements InitializationTask
{
  private Logger log = LoggerFactory.getLogger(COMPONENT_CODE);

  @Override
  public void taskInitialize(GeminiApplication app)
  {
    log.info("Environment details follow.");
    log.info(app.getVersion().getVerboseDescription());
    
    final String javaVm = System.getProperty("java.vm.name", "Unknown");
    final String javaVmVersion = System.getProperty("java.vm.version", "?");
    final String osName = System.getProperty("os.name", "Unknown");
    final String osVersion = System.getProperty("os.version", "?");
    final String osArch = System.getProperty("os.arch", "Unknown");
    
    log.info("{} (v{})", javaVm, javaVmVersion);
    log.info("{} (v{}; {})", osName, osVersion, osArch);

    if (app.getServletConfig() != null)
    {
      log.info("Servlet Container: {}",
          app.getServletConfig().getServerInfo());
    }
    
    log.info("JVM memory: {}Mb; free: {}Mb",
        Runtime.getRuntime().totalMemory() / UtilityConstants.MEGABYTE,
        Runtime.getRuntime().freeMemory() / UtilityConstants.MEGABYTE);
  }
  
}
