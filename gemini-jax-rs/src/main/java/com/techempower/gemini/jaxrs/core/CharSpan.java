package com.techempower.gemini.jaxrs.core;

public class CharSpan implements CharSequence
{
  private final CharSequence charSequence;
  private final int          start;
  private final int          end;
  private       int          hash = 1;
  private       String       toString;

  CharSpan(CharSequence charSequence, int start, int end)
  {
    this.charSequence = charSequence;
    this.start = start;
    this.end = end;
  }

  CharSpan(String string)
  {
    this(string, 0, string.length());
    this.toString = string;
  }

  public int getStart()
  {
    return start;
  }

  public int getEnd()
  {
    return end;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }
    CharSpan charSpan = (CharSpan) o;
    int length = length();
    if (length != charSpan.length())
    {
      return false;
    }
    for (int i = 0; i < length; i++)
    {
      if (charAt(i) != charSpan.charAt(i))
      {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode()
  {
    if (hash == 1 && length() > 0)
    {
      int h = 1;
      for (int i = end - 1; i >= start; i--)
      {
        h = 31 * h + charAt(i - start);
      }
      hash = h;
    }
    return hash;
  }

  @Override
  public int length()
  {
    return end - start;
  }

  @Override
  public char charAt(int index)
  {
    return charSequence.charAt(start + index);
  }

  @Override
  public CharSequence subSequence(int start, int end)
  {
    if (start == 0 && end == length())
    {
      return this;
    }
    return charSequence.subSequence(this.start + start, this.start + end);
  }

  @Override
  public String toString()
  {
    if (toString == null)
    {
      toString = charSequence.subSequence(start, end).toString();
    }
    return toString;
  }
}
