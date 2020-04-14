package com.techempower.gemini.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Provides a logback pattern key for inserting request information into log
 * output like the old logging system would do behind the scenes. To use this,
 * include %req in the logback pattern. An example use of this can be found in
 * the Gemini-logback sample.logback.xml file.
 * The following must be included for the pattern to be recognized:
 * <p>
 * {@code <conversionRule conversionWord="req" converterClass="com.techempower.gemini.log.RequestPatternConverter" />}
 */
public class RequestPatternConverter extends ClassicConverter
{
  @Override
  public String convert(ILoggingEvent event)
  {
    String contextInfo = ContextLogInfo.getContextInformation();
    return contextInfo != null ? contextInfo : "";
  }
}
