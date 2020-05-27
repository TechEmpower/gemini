package com.techempower.gemini.jaxrs.core;

import java.util.Objects;

class MediaTypeData
{
  // TODO: CharSpan
  private final String type;
  private final String subtype;
  private final double qValue;

  public MediaTypeData(String type, String subtype, double qValue)
  {
    this.type = Objects.requireNonNull(type);
    this.subtype = Objects.requireNonNull(subtype);
    this.qValue = qValue;
  }

  public String getType()
  {
    return type;
  }

  public String getSubtype()
  {
    return subtype;
  }

  public double getQValue()
  {
    return qValue;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MediaTypeData that = (MediaTypeData) o;
    return Double.compare(that.qValue, qValue) == 0 &&
        type.equals(that.type) &&
        subtype.equals(that.subtype);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(type, subtype, qValue);
  }

  @Override
  public String toString()
  {
    return "MediaTypeData{" +
        "type='" + type + '\'' +
        ", subtype='" + subtype + '\'' +
        ", qValue=" + qValue +
        '}';
  }
}
