package com.techempower.gemini.jaxrs.core;

import javax.ws.rs.core.MediaType;
import java.util.Map;

public class WrappedQMediaType extends QMediaType
{
  static final WrappedQMediaType DEFAULT_WILDCARD = new WrappedQMediaType(MediaType.WILDCARD_TYPE);
  private final MediaType mediaType;
  private final double qValue;

  /**
   * Creates a QMediaType wrapping the given media type, associating it with
   * the default q value 1.
   */
  public WrappedQMediaType(MediaType mediaType)
  {
    this(mediaType, 1);
  }

  /**
   * Creates a QMediaType wrapping the given media type, associating it with
   * the given q value.
   */
  public WrappedQMediaType(MediaType mediaType, double qValue)
  {
    this.mediaType = mediaType;
    this.qValue = qValue;
  }

  @Override
  public double getQValue()
  {
    return qValue;
  }

  @Override
  public String getType()
  {
    return mediaType.getType();
  }

  @Override
  public String getSubtype()
  {
    return mediaType.getSubtype();
  }

  @Override
  public Map<String, String> getParameters()
  {
    return mediaType.getParameters();
  }

  @Override
  public MediaType getMediaType() {
    return mediaType;
  }
}
