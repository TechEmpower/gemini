package com.techempower.gemini.jaxrs.core;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class EndpointRegistryTest
{
  @RunWith(Parameterized.class)
  public static class ShouldGetTheRightEndpoint
  {

    private static final Endpoint endpointA = new TestEndpoint("Endpoint A");
    private static final Endpoint endpointB = new TestEndpoint("Endpoint B");
    private static final Endpoint endpointC = new TestEndpoint("Endpoint C");

    @Parameters(name = "request: \"{1}\" - \"{2}\" - \"{3}\"")
    public static Object[][] params()
    {
      return new Object[][]{
          // Should support basic matches
          {endpointA, HttpMethod.GET, "foo/bar", List.of()},
          {endpointA, HttpMethod.GET, "foo/bar", List.of(Map.entry(HttpHeaders.CONTENT_TYPE, "application/json"))},
          // Should support advanced matches
          {endpointB, HttpMethod.GET, "foo/bar", List.of(Map.entry(HttpHeaders.CONTENT_TYPE, "text/html"))},
          {endpointC, HttpMethod.GET, "foo/bar", List.of(Map.entry(HttpHeaders.CONTENT_TYPE, "application/xml"))},
          {endpointA, HttpMethod.GET, "foo/bar", List.of(Map.entry(HttpHeaders.CONTENT_TYPE, "application/xml; q=0.2, application/json; q=0.7"))},
          // TODO: Test/implement ACCEPT header/produces media types.
      };
    }

    @Parameter
    public Endpoint expected;

    @Parameter(1)
    public String httpMethod;

    @Parameter(2)
    public String uri;

    @Parameter(3)
    public List<Map.Entry<String, String>> headers;

    public EndpointRegistry registry;

    @Before
    public void setUp() throws Exception
    {
      registry = new SimpleEndpointRegistry();
      registry.register(new EndpointMetadata(
          "foo/bar", Set.of(HttpMethod.GET),
          new QMediaTypeGroup(List.of(mediaType("application", "json", 1, Map.of()))),
          new QMediaTypeGroup(List.of())
      ), endpointA);
      registry.register(new EndpointMetadata(
          "foo/bar", Set.of(HttpMethod.GET),
          new QMediaTypeGroup(List.of(mediaType("text", "html", 0.9, Map.of()))),
          new QMediaTypeGroup(List.of())
      ), endpointB);
      registry.register(new EndpointMetadata(
          "foo/bar", Set.of(HttpMethod.GET),
          new QMediaTypeGroup(List.of(mediaType("application", "xml", 0.8, Map.of()))),
          new QMediaTypeGroup(List.of())
      ), endpointC);
    }

    @Test
    public void getEndpointFor()
    {
      assertEquals(expected,
          registry.getEndpointFor(httpMethod, uri, headers));
    }
  }

  private static class TestEndpoint implements Endpoint
  {
    private final String name;

    public TestEndpoint(String name)
    {
      this.name = name;
    }

    @Override
    public Object invoke(String httpMethod,
                         String uri,
                         List<Map.Entry<String, String>> headers,
                         Map<String, String> pathParams,
                         Map<String, String> queryParams,
                         String body)
    {
      // Invocation is not part of this test.
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
      return "TestEndpoint{" +
          "name='" + name + '\'' +
          '}';
    }
  }

  private static QMediaType mediaType(String type,
                                      String subtype,
                                      double qValue,
                                      Map<String, String> parameters)
  {
    return new WrappedQMediaType(new MediaType(type, subtype, parameters), qValue);
  }
}