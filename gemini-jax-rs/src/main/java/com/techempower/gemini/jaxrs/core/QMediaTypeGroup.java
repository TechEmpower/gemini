package com.techempower.gemini.jaxrs.core;

import java.util.List;
import java.util.Objects;

class QMediaTypeGroup
{
  private final List<QMediaType> mediaTypes;

  public QMediaTypeGroup(List<QMediaType> mediaTypes)
  {
    this.mediaTypes = Objects.requireNonNull(mediaTypes);
  }

  public List<QMediaType> getMediaTypes()
  {
    return mediaTypes;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QMediaTypeGroup that = (QMediaTypeGroup) o;
    return mediaTypes.equals(that.mediaTypes);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(mediaTypes);
  }

  @Override
  public String toString()
  {
    return "MediaTypeDataGroup{" +
        "mediaTypes=[" + mediaTypes +
        "]}";
  }
}
