package com.techempower.gemini.jaxrs.core;

import java.util.List;
import java.util.Objects;

class MediaTypeDataGroup
{
  private final List<MediaTypeData> mediaTypeDataList;

  public MediaTypeDataGroup(List<MediaTypeData> mediaTypeDataList)
  {
    this.mediaTypeDataList = Objects.requireNonNull(mediaTypeDataList);
  }

  public List<MediaTypeData> getMediaTypeDataList()
  {
    return mediaTypeDataList;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MediaTypeDataGroup that = (MediaTypeDataGroup) o;
    return mediaTypeDataList.equals(that.mediaTypeDataList);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(mediaTypeDataList);
  }

  @Override
  public String toString()
  {
    return "MediaTypeDataGroup{" +
        "mediaTypeDataList=" + mediaTypeDataList +
        '}';
  }
}
