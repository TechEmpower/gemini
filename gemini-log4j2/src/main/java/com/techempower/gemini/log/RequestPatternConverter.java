package com.techempower.gemini.log;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

/**
 * Provides a log4j2 pattern key for inserting request information into log
 * output like the old logging system would do behind the scenes. To use this,
 * include %req in the log4j2 pattern. An example use of this can be found in
 * the Gemini archetype's log4j2.xml file.
 */
@Plugin(name = "RequestPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"req", "request"})
public class RequestPatternConverter extends LogEventPatternConverter
{
  /**
   * Singleton.
   */
  private static final RequestPatternConverter INSTANCE = new RequestPatternConverter();

  /**
   * Private constructor.
   */
  private RequestPatternConverter()
  {
    super("Request", "request");
  }

  /**
   * Obtains an instance of RequestPatternConverter.
   *
   * @param options options, currently ignored, may be null.
   * @return instance of RequestPatternConverter.
   */
  public static RequestPatternConverter newInstance(final String[] options)
  {
    return INSTANCE;
  }

  @Override
  public void format(LogEvent event, StringBuilder toAppendTo)
  {
    String contextInfo = GeminiComponentLog.getContextInformation();
    if (contextInfo != null)
    {
      toAppendTo.append(contextInfo);
    }
  }
}
