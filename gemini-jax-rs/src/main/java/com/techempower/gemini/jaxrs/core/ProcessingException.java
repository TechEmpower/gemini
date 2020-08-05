package com.techempower.gemini.jaxrs.core;

public class ProcessingException extends RuntimeException
{
  private static final long serialVersionUID = 8411477705739130341L;

  public ProcessingException(String message)
  {
    super(message);
  }

  public ProcessingException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
