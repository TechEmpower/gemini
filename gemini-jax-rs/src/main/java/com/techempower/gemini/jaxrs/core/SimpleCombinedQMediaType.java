package com.techempower.gemini.jaxrs.core;

import java.util.Objects;

public class SimpleCombinedQMediaType
    implements Comparable<SimpleCombinedQMediaType>
{
  public static final  SimpleCombinedQMediaType INCOMPATIBLE = new SimpleCombinedQMediaType("", "", 0, 0, 0);
  private static final String                   WILDCARD     = "*";

  private final String type;
  private final String subtype;
  private final double clientQ;
  private final double serverQ;
  private final int    distance;

  public SimpleCombinedQMediaType(String type,
                                  String subtype,
                                  double clientQ,
                                  double serverQ,
                                  int distance)
  {
    this.type = type;
    this.subtype = subtype;
    this.clientQ = clientQ;
    this.serverQ = serverQ;
    this.distance = distance;
  }

  public String getType()
  {
    return type;
  }

  public String getSubtype()
  {
    return subtype;
  }

  public double getClientQ()
  {
    return clientQ;
  }

  public double getServerQ()
  {
    return serverQ;
  }

  public int getDistance()
  {
    return distance;
  }

  @Override
  public int compareTo(SimpleCombinedQMediaType that)
  {
    if (this == INCOMPATIBLE)
    {
      return that == INCOMPATIBLE ? 0 : -1;
    }
    else if (that == INCOMPATIBLE)
    {
      return 1;
    }
    int wildcardsInThis = 0;
    int wildcardsInThat = 0;
    if (this.getType().equals(WILDCARD))
    {
      wildcardsInThis++;
    }
    if (this.getSubtype().equals(WILDCARD))
    {
      wildcardsInThis++;
    }
    if (that.getType().equals(WILDCARD))
    {
      wildcardsInThat++;
    }
    if (that.getSubtype().equals(WILDCARD))
    {
      wildcardsInThat++;
    }
    // Step i
    if (wildcardsInThis != wildcardsInThat)
    {
      return wildcardsInThis < wildcardsInThat ? 1 : -1;
    }
    // Step ii
    if (this.getClientQ() != that.getClientQ())
    {
      return this.getClientQ() > that.getClientQ() ? 1 : -1;
    }
    // Step iii
    if (this.getServerQ() != that.getServerQ())
    {
      return this.getServerQ() > that.getServerQ() ? 1 : -1;
    }
    // Step iv
    if (this.getDistance() != that.getDistance())
    {
      return this.getDistance() < that.getDistance() ? 1 : -1;
    }
    return 0;
  }

  public static SimpleCombinedQMediaType create(QMediaType clientType,
                                                QMediaType serverType)
  {
    String type;
    String subtype;
    int distance = 0;
    if (clientType.getType().equals(WILDCARD))
    {
      if (clientType.getType().equals(serverType.getType()))
      {
        type = WILDCARD;
      }
      else
      {
        distance++;
        type = serverType.getType();
      }
    }
    else if (serverType.getType().equals(WILDCARD))
    {
      if (clientType.getType().equals(serverType.getType()))
      {
        type = WILDCARD;
      }
      else
      {
        distance++;
        type = clientType.getType();
      }
    }
    else if (clientType.getType().equalsIgnoreCase(serverType.getType()))
    {
      type = clientType.getType();
    }
    else
    {
      return INCOMPATIBLE;
    }
    if (clientType.getSubtype().equals(WILDCARD))
    {
      if (clientType.getSubtype().equals(serverType.getSubtype()))
      {
        subtype = WILDCARD;
      }
      else
      {
        distance++;
        subtype = serverType.getSubtype();
      }
    }
    else if (serverType.getSubtype().equals(WILDCARD))
    {
      if (clientType.getSubtype().equals(serverType.getSubtype()))
      {
        subtype = WILDCARD;
      }
      else
      {
        distance++;
        subtype = clientType.getSubtype();
      }
    }
    else if (clientType.getSubtype().equalsIgnoreCase(serverType.getSubtype()))
    {
      subtype = clientType.getSubtype();
    }
    else
    {
      return INCOMPATIBLE;
    }
    double clientQ = clientType.getQValue();
    double serverQ = serverType.getQValue();
    return new SimpleCombinedQMediaType(type, subtype, clientQ, serverQ,
        distance);
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    else if (this == INCOMPATIBLE || o == INCOMPATIBLE)
    {
      return false;
    }
    if (!(o instanceof SimpleCombinedQMediaType))
    {
      return false;
    }
    SimpleCombinedQMediaType that = (SimpleCombinedQMediaType) o;
    return Double.compare(that.getClientQ(), getClientQ()) == 0
        && Double.compare(that.getServerQ(), getServerQ()) == 0
        && getDistance() == that.getDistance()
        && getType().equalsIgnoreCase(that.getType())
        && getSubtype().equalsIgnoreCase(that.getSubtype());
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(getType().toLowerCase(), getSubtype().toLowerCase(),
        getClientQ(), getServerQ(), getDistance());
  }

  @Override
  public String toString()
  {
    if (this == INCOMPATIBLE)
    {
      return "⊥";
    }
    return String.format("%s/%s;q=%s;qs=%s;d=%s", getType(), getSubtype(),
        getClientQ(), getServerQ(), getDistance());
  }
}
