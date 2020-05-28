package com.techempower.gemini.jaxrs.core;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

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
          {group(mediaType("text", "html", 1.0d, Map.of())), "q", "text/html"},
          // Should support q-value
          {group(mediaType("text", "html", 0.9d, Map.of("q", "0.9"))), "q", "text/html;q=0.9"},
          {group(mediaType("text", "html", 1.0d, Map.of("q", "1.0"))), "q", "text/html;q=1.0"},
          {group(mediaType("text", "html", 0.999d, Map.of("q", "0.999"))), "q", "text/html;q=0.999"},
          // Should support other q-value keys
          {group(mediaType("text", "html", 0.9d, Map.of("qs", "0.9"))), "qs", "text/html;qs=0.9"},
          {group(mediaType("text", "html", 0.8d, Map.of("q", "0.9", "qs", "0.8"))), "qs", "text/html;q=0.9;qs=0.8"},
          // Should support multi-type captures
          {group(mediaType("text", "html", 1.0d, Map.of()), mediaType("text", "xml", 0.9d, Map.of("q", "0.9"))), "q", "text/html,text/xml;q=0.9"},
          // Should support non q-value parameters
          {group(mediaType("text", "html", 0.8d, Map.of("f", "d", "q", "0.8")), mediaType("text", "xml", 0.9d, Map.of("q", "0.9"))), "q", "text/html;f=d;q=0.8,text/xml;q=0.9"},
          // Should respect quote rules (these are a few random variations)
          {group(mediaType("text", "html", 1.0d, Map.of("f", "dog")), mediaType("text", "xml", 0.9d, Map.of("q", "0.9"))), "q", "text/html;f=\"dog\",text/xml;q=0.9"},
          {group(mediaType("text", "html", 1.0d, Map.of("f", "d,og")), mediaType("text", "xml", 0.9d, Map.of("q", "0.9"))), "q", "text/html;f=\"d,og\",text/xml;q=0.9"},
          {group(mediaType("text", "html", 0.8d, Map.of("f", "d,o", "q", "0.8")), mediaType("text", "xml", 0.9d, Map.of("q", "0.9"))), "q", "text/html;f=\"d,o\";q=0.8,text/xml;q=0.9"},
          // Should respect proper whitespace rules
          {group(mediaType("text", "html", 1.0d, Map.of()), mediaType("text", "xml", 0.9d, Map.of("q", "0.9"))), "q", "text/html,text/xml;   \tq=0.9"},
      };
    }

    @Parameter
    public QMediaTypeGroup expected;

    @Parameter(1)
    public String qValueKey;

    @Parameter(2)
    public String mediaType;

    @Test
    public void parse()
    {
      assertEquals(expected, new MediaTypeParserImpl(qValueKey).parse(mediaType));
    }

    private static QMediaType mediaType(String type, String subtype, double qValue, Map<String, String> parameters)
    {
      return new WrappedQMediaType(new MediaType(type, subtype, parameters))
      {
        @Override
        public double getQValue()
        {
          return qValue;
        }
      };
    }

    private static QMediaTypeGroup group(QMediaType... mediaTypes)
    {
      return new QMediaTypeGroup(List.of(mediaTypes));
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
          // Should fail to parse excessive numbers after decimal point
          {"q", "text/html,text/xml;q=0.9999"},
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