package com.techempower.log;

import com.techempower.gemini.GeminiApplication;

/**
 * This class serves as a quick, convenient way to provide the getLog method
 * previously present on the TechEmpowerApplication class. Long term use is
 * not recommended as it recreates the logger for each method call.
 */
@Deprecated(since = "3.1.0")
public interface LegacyApplicationLogging
{
  default ComponentLog getLog(String name)
  {
    return new ComponentLog(getClass());
  }

  default ComponentLog getComponentLog() {
    return new ComponentLog(GeminiApplication.class);
  }
}
