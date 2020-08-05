package com.techempower.gemini.jaxrs.core;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Objects;

public abstract class QMediaType
{
  static final String ONE = "1";

  private boolean hashFound;
  private int     hash;

  public abstract double getQValue();

  public abstract String getType();

  public abstract String getSubtype();

  public abstract Map<String, String> getParameters();

  public abstract MediaType getMediaType();

  public boolean isCompatible(MediaType other)
  {
    return other != null && // return false if other is null, else
        (getType().equals(MediaType.MEDIA_TYPE_WILDCARD) || other.getType().equals(MediaType.MEDIA_TYPE_WILDCARD) || // both are wildcard types, or
            (getType().equalsIgnoreCase(other.getType()) && (getSubtype().equals(MediaType.MEDIA_TYPE_WILDCARD)
                || other.getSubtype().equals(MediaType.MEDIA_TYPE_WILDCARD))) || // same types, wildcard sub-types, or
            (getType().equalsIgnoreCase(other.getType()) && getSubtype().equalsIgnoreCase(other.getSubtype()))); // same types & sub-types
  }

  @Override
  public final boolean equals(Object o)
  {
    if (this == o) return true;
    if (!(o instanceof QMediaType)) return false;
    QMediaType that = (QMediaType) o;
    return getQValue() == that.getQValue()
        && getType().equalsIgnoreCase(that.getType())
        && getSubtype().equalsIgnoreCase(that.getSubtype())
        && getParameters().equals(that.getParameters());
  }

  @Override
  public int hashCode()
  {
    if (!hashFound)
    {
      hash = Objects.hash(getQValue(), getType().toLowerCase(),
          getSubtype().toLowerCase(), getParameters());
      hashFound = true;
    }
    return hash;
  }

  @Override
  public String toString()
  {
    return "QMediaType{" +
        "type='" + getType() + '\'' +
        ", subtype='" + getSubtype() + '\'' +
        ", parameters=" + getParameters() +
        ", qValue=" + getQValue() +
        '}';
  }
}
