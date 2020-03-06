package com.techempower.log;

/**
 * This class serves as a quick, convenient way to provide the logging methods
 * previously present on several classes, like handlers. Long term use is not
 * recommended as it recreates the logger for each method call.
 */
@Deprecated
public interface LegacyLogging
{
  default ComponentLog log()
  {
    return new ComponentLog(getClass());
  }
}
