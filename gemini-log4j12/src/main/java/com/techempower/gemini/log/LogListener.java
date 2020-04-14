package com.techempower.gemini.log;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public abstract class LogListener extends AppenderSkeleton
{
  public Level getLevel()
  {
    return (Level) getThreshold();
  }

  /**
   * If a stricter log level is required for this listener, then call this
   * method with the appropriate log level. Note that this only works
   * "upwards". Any logged lines below the log level of the attached logger
   * will not be captured.
   */
  public void setLevel(Level level)
  {
    this.setThreshold(level);
  }

  @Override
  protected void append(LoggingEvent eventObject)
  {
    if (getLevel() == null
        || eventObject.getLevel().isGreaterOrEqual(getLevel()))
    {
      handle(eventObject);
    }
  }

  public String format(LoggingEvent event)
  {
    Layout layout = getLayout();
    if (layout != null)
    {
      return layout.format(event);
    }
    return null;
  }

  protected void handle(LoggingEvent eventObject)
  {
    String message = format(eventObject);
    Level level = eventObject.getLevel();
    ThrowableInformation throwableInformation = eventObject.getThrowableInformation();
    Throwable throwable;
    if (throwableInformation != null)
    {
      throwable = throwableInformation.getThrowable();
    }
    else
    {
      throwable = null;
    }
    handle(message, level, throwable);
  }

  protected abstract void handle(String message,
                                 Level logLevel,
                                 Throwable throwable);

  @Override
  public void close()
  {
  }

  @Override
  public boolean requiresLayout()
  {
    return false;
  }
}
