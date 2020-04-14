package com.techempower.gemini.log;

import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class GeminiPatternLayout extends EnhancedPatternLayout
{
  @Override
  public String format(LoggingEvent event)
  {
    Object message = event.getMessage();
    if (message instanceof String)
    {
      String contextInfo = ContextLogInfo.getContextInformation();
      if (contextInfo != null && !contextInfo.isEmpty())
      {
        message = contextInfo + "- " + message;
      }
    }
    return super.format(new LoggingEvent(event.getFQNOfLoggerClass(),
        event.getLogger(), event.getTimeStamp(), event.getLevel(), message,
        event.getThreadName(), event.getThrowableInformation(),
        event.getNDC(), event.getLocationInformation(),
        event.getProperties()));
  }
}
