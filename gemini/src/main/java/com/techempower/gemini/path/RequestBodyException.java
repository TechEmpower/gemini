package com.techempower.gemini.path;

/**
 * An exception that should be thrown by implementations of
 * {@link RequestBodyAdapter} when something goes wrong. You
 * must provide an HTTP status code and message that will be
 * used to generate an error response to the user.
 */
public class RequestBodyException extends Exception
{
  private static final long serialVersionUID = 3381361290493468966L;
  private final int statusCode;

  public RequestBodyException(int statusCode, String message, Throwable cause)
  {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public RequestBodyException(int statusCode, String message)
  {
    this(statusCode, message, null);
  }

  /**
   * The status code to set on the response.
   */
  public int getStatusCode()
  {
    return statusCode;
  }
}
