package com.techempower.log;

/**
 * This class serves as a quick, convenient way to provide the getLog method
 * previously present on the TechEmpowerApplication class. Long term use is
 * not recommended as it recreates the logger for each method call.
 */
@Deprecated
public interface LegacyApplicationLogging
{
  default ComponentLog getLog(String name)
  {
    return new ComponentLog(getClass());
  }
}
