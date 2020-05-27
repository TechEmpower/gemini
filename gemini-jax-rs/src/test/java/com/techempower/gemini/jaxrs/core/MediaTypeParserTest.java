package com.techempower.gemini.jaxrs.core;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.runners.Parameterized.Parameters;
import static org.junit.runners.Parameterized.Parameter;

@RunWith(Enclosed.class)
public class MediaTypeParserTest
{
  @RunWith(Parameterized.class)
  public static class ShouldParseSuccessfully
  {
    @Parameters(name = "q-value key: \"{1}\", input: \"{2}\"")
    public static Object[][] params()
    {
      return new Object[][]{
          // Should support basic captures
          {group(new MediaTypeData("text", "html", 1.0d)), "q", "text/html"},
          // Should support q-value
          {group(new MediaTypeData("text", "html", 0.9d)), "q", "text/html;q=0.9"},
          // Should support other q-value keys
          {group(new MediaTypeData("text", "html", 0.9d)), "qs", "text/html;qs=0.9"},
          {group(new MediaTypeData("text", "html", 0.8d)), "qs", "text/html;q=0.9;qs=0.8"},
          // Should support multi-type captures
          {group(new MediaTypeData("text", "html", 1.0d), new MediaTypeData("text", "xml", 0.9d)), "q", "text/html,text/xml;q=0.9"},
          // Should support non q-value parameters
          {group(new MediaTypeData("text", "html", 0.8d), new MediaTypeData("text", "xml", 0.9d)), "q", "text/html;f=d;q=0.8,text/xml;q=0.9"},
          // Should respect quote rules (these are a few random variations)
          {group(new MediaTypeData("text", "html", 1.0d), new MediaTypeData("text", "xml", 0.9d)), "q", "text/html;f=\"dog\",text/xml;q=0.9"},
          {group(new MediaTypeData("text", "html", 1.0d), new MediaTypeData("text", "xml", 0.9d)), "q", "text/html;f=\"d,og\",text/xml;q=0.9"},
          {group(new MediaTypeData("text", "html", 0.8d), new MediaTypeData("text", "xml", 0.9d)), "q", "text/html;f=\"d,o\";q=0.8,text/xml;q=0.9"},
          // Should respect proper whitespace rules
          {group(new MediaTypeData("text", "html", 1.0d), new MediaTypeData("text", "xml", 0.9d)), "q", "text/html,text/xml;   \tq=0.9"},
      };
    }

    @Parameter
    public MediaTypeDataGroup expected;

    @Parameter(1)
    public String qValueKey;

    @Parameter(2)
    public String mediaType;

    @Test
    public void parse()
    {
      assertEquals(expected, new MediaTypeParserImpl(qValueKey).parse(mediaType));
    }

    private static MediaTypeDataGroup group(MediaTypeData... mediaTypeData)
    {
      return new MediaTypeDataGroup(List.of(mediaTypeData));
    }
  }

  @RunWith(Parameterized.class)
  public static class ShouldFailToParse
  {
    @Parameters(name = "q-value key: \"{0}\", input: \"{1}\"")
    public static Object[][] params()
    {
      return new Object[][]{
          // Should fail to parse improper placement of quotes
          {"q", "text/html;f=\\\"d,og\\\",text/xml;q=0.9"},
          {"q", "text/html;f=\\\"d\";q=0.8,text/xml;q=0.9"},
          {"q", "text/html;f=\\\"d\\\";q=0.8,text/xml;q=0.9"},
          // Should fail to parse improper placement of whitespace
          {"q", "text/html,text/xml;q=   \t0.9"},
      };
    }

    @Parameter
    public String qValueKey;

    @Parameter(1)
    public String mediaType;

    @Test(expected = ProcessingException.class)
    public void parse()
    {
      new MediaTypeParserImpl(qValueKey).parse(mediaType);
    }
  }
}