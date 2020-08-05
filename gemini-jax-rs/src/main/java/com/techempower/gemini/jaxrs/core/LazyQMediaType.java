package com.techempower.gemini.jaxrs.core;

import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Map;

public class LazyQMediaType extends QMediaType
{
  private final CharSpan            type;
  private final CharSpan            subtype;
  private final double              qValue;
  private final Map<String, String> unmodifiableParameters;
  private       MediaType           mediaType;

  public LazyQMediaType(CharSpan type,
                         CharSpan subtype,
                         double qValue,
                         Map<CharSpan, CharSpan> parameters)
  {
    this.type = type;
    this.subtype = subtype;
    this.qValue = qValue;
    this.unmodifiableParameters = Collections.unmodifiableMap(
        new StringStringCharSpanMap(parameters));
  }

  @Override
  public String getType()
  {
    return type.toString();
  }

  @Override
  public String getSubtype()
  {
    return subtype.toString();
  }

  @Override
  public double getQValue()
  {
    return qValue;
  }

  @Override
  public Map<String, String> getParameters()
  {
    return unmodifiableParameters;
  }

  @Override
  public MediaType getMediaType()
  {
    if (mediaType == null)
    {
      mediaType = new MediaType(getType(), getSubtype(), getParameters());
    }
    return mediaType;
  }
}
