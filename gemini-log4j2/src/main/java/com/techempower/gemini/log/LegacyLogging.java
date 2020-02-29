package com.techempower.gemini.log;

/**
 * This class serves as a quick, convenient way to provide the logging methods
 * previously present on several classes, like handlers. Use is not
 * recommended long term as it recreates the logger for each method call.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public interface LegacyLogging
{
  default LegacyLog log()
  {
    return new LegacyLog(getClass());
  }
}
