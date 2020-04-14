package com.techempower.log;

/**
 * This class serves as a quick, convenient way to provide the logging methods
 * previously present on several classes, like handlers. Long term use is not
 * recommended as it recreates the logger for each method call.
 */
@Deprecated(since = "3.1.0")
public interface LegacyLogging
{
  default ComponentLog log()
  {
    return new ComponentLog(getClass());
  }

  default ComponentLog getLog()
  {
    return new ComponentLog(getClass());
  }

  default void l(String text)
  {
    getLog().log(text);
  }

  default void l(String text, int logLevel)
  {
    getLog().log(text, logLevel);
  }

  default void l(String text, Throwable throwable)
  {
    getLog().log(text, throwable);
  }

  default void l(String text, int logLevel, Throwable throwable)
  {
    getLog().log(text, logLevel, throwable);
  }

  default void log(String text)
  {
    getLog().log(text);
  }

  default void log(String text, int logLevel)
  {
    getLog().log(text, logLevel);
  }

  default void log(String text, Throwable throwable)
  {
    getLog().log(text);
  }

  default void log(String text, int logLevel, Throwable throwable)
  {
    getLog().log(text, logLevel, throwable);
  }

  default void debug(String text)
  {
    getLog().log(text);
  }

  default void debug(String text, int logLevel)
  {
    getLog().log(text, logLevel);
  }

  default void debug(String text, Throwable throwable)
  {
    getLog().log(text);
  }

  default void debug(String text, int logLevel, Throwable throwable)
  {
    getLog().log(text, logLevel, throwable);
  }
}
