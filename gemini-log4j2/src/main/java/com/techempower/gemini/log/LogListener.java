package com.techempower.gemini.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public abstract class LogListener extends AbstractAppender
{
  private Level level;

  public LogListener(final String name, final StringLayout layout)
  {
    super(name, null, layout, false, Property.EMPTY_ARRAY);
  }

  public Level getLevel()
  {
    return level;
  }

  /**
   * If a stricter log level is required for this listener, then call this
   * method with the appropriate log level. Note that this only works
   * "upwards". Any logged lines below the log level of the attached logger
   * will not be captured.
   */
  public void setLevel(Level level)
  {
    this.level = level;
  }

  @Override
  public StringLayout getLayout()
  {
    return (StringLayout) super.getLayout();
  }

  @Override
  public void append(LogEvent eventObject)
  {
    if (getLevel() == null
        || eventObject.getLevel().isMoreSpecificThan(getLevel()))
    {
      handle(eventObject);
    }
  }

  public String format(LogEvent event)
  {
    StringLayout layout = getLayout();
    if (layout != null)
    {
      return new String(layout.toByteArray(event), getLayout().getCharset());
    }
    return null;
  }

  protected void handle(LogEvent eventObject)
  {
    handle(format(eventObject), eventObject.getLevel(), eventObject.getThrown());
  }

  protected abstract void handle(String message,
                                 Level logLevel,
                                 Throwable throwable);
}
