package com.techempower.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exists to support existing applications that rely on the old
 * logging system. Long term use is not recommended as it recreates the
 * logger for each method call.
 */
@Deprecated(since = "3.1.0")
public final class ComponentLog
{
  private final Logger log;

  ComponentLog(final Class<?> aClass)
  {
    this.log = LoggerFactory.getLogger(aClass);
  }

  public final void log(String text)
  {
    log.info(text, LogLevel.NORMAL);
  }

  public final void log(String text, int logLevel)
  {
    log(text, LogLevel.NORMAL, null);
  }

  public final void log(String text, Throwable e)
  {
    log(text, LogLevel.NORMAL, e);
  }

  public final void log(String text, int logLevel, Throwable e)
  {
    if (text != null)
    {
      // Avoids any unexpected formatting from SLF4J.
      text = text.replaceAll("\\{}", "\\\\{}");
    }
    if (logLevel < LogLevel.DEBUG)
    {
      if (e != null)
      {
        log.trace(text, e);
      }
      else
      {
        log.trace(text);
      }
    }
    else if (logLevel < LogLevel.NORMAL)
    {
      if (e != null)
      {
        log.debug(text, e);
      }
      else
      {
        log.debug(text);
      }
    }
    else if (logLevel < LogLevel.ALERT)
    {
      if (e != null)
      {
        log.info(text, e);
      }
      else
      {
        log.info(text);
      }
    }
    else if (logLevel < LogLevel.CRITICAL)
    {
      if (e != null)
      {
        log.warn(text, e);
      }
      else
      {
        log.warn(text);
      }
    }
    else
    {
      if (e != null)
      {
        log.error(text, e);
      }
      else
      {
        log.error(text);
      }
    }
  }
}
