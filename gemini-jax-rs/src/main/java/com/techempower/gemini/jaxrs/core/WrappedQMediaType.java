package com.techempower.gemini.jaxrs.core;

import javax.ws.rs.core.MediaType;
import java.util.Map;

public class WrappedQMediaType extends QMediaType
{
  private final MediaType mediaType;

  /**
   * Creates a QMediaType wrapping the given media type, associating it with
   * the default q value 1.
   */
  public WrappedQMediaType(MediaType mediaType)
  {
    this.mediaType = mediaType;
  }

  @Override
  public double getQValue()
  {
    return 1;
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
