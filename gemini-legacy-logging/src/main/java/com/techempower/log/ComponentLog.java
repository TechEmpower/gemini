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
    log.info(text);
  }

  public final void log(String text, Throwable e)
  {
    log.info(text, e);
  }
}
