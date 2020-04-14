package com.techempower.gemini.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

public abstract class LogListener extends AppenderBase<ILoggingEvent>
{
  private Layout<ILoggingEvent> layout;
  private Level                 level;

  public Layout<ILoggingEvent> getLayout()
  {
    return layout;
  }

  public void setLayout(Layout<ILoggingEvent> layout)
  {
    this.layout = layout;
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
  protected void append(ILoggingEvent eventObject)
  {
    if (getLevel() == null
        || eventObject.getLevel().isGreaterOrEqual(getLevel()))
    {
      handle(eventObject);
    }
  }

  protected String format(ILoggingEvent eventObject)
  {
    Layout<ILoggingEvent> layout = getLayout();
    if (layout != null)
    {
      return layout.doLayout(eventObject);
    }
    return null;
  }

  protected void handle(ILoggingEvent eventObject)
  {
    String message = format(eventObject);
    ThrowableProxy throwableProxy;
    if (eventObject instanceof LoggingEvent)
    {
      LoggingEvent event = (LoggingEvent) eventObject;
      throwableProxy = (ThrowableProxy) event.getThrowableProxy();
    }
    else
    {
      throwableProxy = null;
    }
    if (throwableProxy != null)
    {
      handleException(message, throwableProxy.getThrowable());
    }
    else
    {
      handle(message);
    }
  }

  protected abstract void handle(String message);

  protected abstract void handleException(String message, Throwable throwable);
}
