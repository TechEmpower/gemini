package com.techempower.gemini.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LegacyLog
{
  private final Logger log;

  LegacyLog(final Class<? extends LegacyLogging> aClass)
  {
    this.log = LoggerFactory.getLogger(aClass);
  }

  public final void log(String text) {
    log.info(text);
  }

  public final void log(String text, Throwable e) {
    log.info(text, e);
  }
}
