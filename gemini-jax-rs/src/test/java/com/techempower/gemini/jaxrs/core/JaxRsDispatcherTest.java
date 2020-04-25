package com.techempower.gemini.jaxrs.core;

import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class JaxRsDispatcherTest
{
  @Path("/test")
  public static class TestCase1
  {
    @GET
    @Path("/bar")
    public String doIt()
    {
      return "did-it-test-case-1";
    }
  }

  @Path("/{test}")
  public static class TestCase2
  {
    @GET
    @Path("/bar")
    public String doIt(@PathParam("test") String test)
    {
      return "did-it-test-case-2" + test;
    }
  }

  @Path("/{test: .+}")
  public static class TestCase3
  {
    @GET
    @Path("/bar")
    public String doIt(@PathParam("test") String test)
    {
      return "did-it-test-case-3" + test;
    }
  }

  @Path("/{test1: \\d+}-dog-{test2}")
  public static class TestCase4
  {
    @GET
    @Path("/bar")
    public String doIt(@PathParam("test1") String test1,
                       @PathParam("test2") String test2)
    {
      return "did-it-test-case-4" + test1 + test2;
    }
  }

  @Path("/{test1: \\d+}/-dog-{test2}")
  public static class TestCase5
  {
    @GET
    @Path("/bar")
    public String doIt(@PathParam("test1") String test1,
                       @PathParam("test2") String test2)
    {
      return "did-it-test-case-5" + test1 + test2;
    }
  }

  @Path("/foo")
  public static class TestCase5B
  {
    @GET
    @Path("/{test1}-dog-{test2}")
    public String doIt(@PathParam("test1") String test1,
                       @PathParam("test2") String test2)
    {
      return "did-it-test-case-5B"
          + String.format("{test1:%s,test2:%s}", test1, test2);
    }
  }

  @Path("foo")
  public static class FooResource
  {
    @GET
    @Path("bar")
    public String bar()
    {
      return "did-it-foo-resource";
    }
  }

  @Path("foo")
  public static class FooResourceVar
  {
    @GET
    @Path("{bar}")
    public String bar(@PathParam("bar") String bar)
    {
      return "did-it-foo-resource-var" + bar;
    }
  }

  @Path("/foo")
  public interface TestCase6
  {
    @GET
    @Path("/bar")
    String doIt();
  }

  public static class TestCase6Impl
      implements TestCase6
  {
    @Override
    public String doIt()
    {
      return "did-it-test-case-6";
    }
  }

  @Path("/foo")
  public static class TestCase7
  {
    @GET
    @Path("/{bar}")
    public String doIt(@PathParam("bar") UUID test)
    {
      return "did-it-test-case-7" + test;
    }
  }

  @Path("/{test}")
  public static class TestCase8
  {
    @GET
    @Path("/bar")
    public String doIt(@PathParam("test") String test)
    {
      return "did-it-test-case-8" + test;
    }
  }

  @Test
  public void simpleEndpointsShouldBeDispatched()
  {
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase1());
    assertEquals("did-it-test-case-1",
        dispatcher.dispatch(HttpMethod.GET, "/test/bar"));
  }

  @Test
  public void classRegistrationShouldBeSupported()
  {
    // TODO: Not implemented yet
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(TestCase1.class);
    assertEquals("did-it-test-case-1",
        dispatcher.dispatch(HttpMethod.GET, "/test/bar"));
  }

  @Test
  public void precedentsShouldBeRespected()
  {
    // TODO: Not implemented yet
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase3());
    dispatcher.register(new TestCase2());
    dispatcher.register(new TestCase1());
    assertEquals("did-it-test-case-3dog",
        dispatcher.dispatch(HttpMethod.GET, "/dog/bar"));
    assertEquals("did-it-test-case-1",
        dispatcher.dispatch(HttpMethod.GET, "/test/bar"));
  }

  @Test
  public void pathParamsShouldBeProvided()
  {
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase2());
    assertEquals("did-it-test-case-2dog",
        dispatcher.dispatch(HttpMethod.GET, "/dog/bar"));
  }

  @Test
  public void pathParamRegexesShouldBeSupported1()
  {
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase4());
    assertEquals("did-it-test-case-449cat",
        dispatcher.dispatch(HttpMethod.GET, "/49-dog-cat/bar"));
  }

  @Test
  public void pathParamRegexesShouldBeSupported2()
  {
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase5());
    assertEquals("did-it-test-case-549cat",
        dispatcher.dispatch(HttpMethod.GET, "/49/-dog-cat/bar"));
  }

  @Test
  public void pathParamRegexesShouldBeSupported3()
  {
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase5B());
    assertEquals("did-it-test-case-5B{test1:-dog-,test2:-dog--dog-}",
        dispatcher.dispatch(HttpMethod.GET, "/foo/-dog--dog--dog--dog-"));
  }

  @Test
  public void uuidPathParamsShouldBeSupported()
  {
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase7());
    UUID id = UUID.randomUUID();
    assertEquals("did-it-test-case-7" + id,
        dispatcher.dispatch(HttpMethod.GET, "/foo/" + id));
  }

  @Test
  public void pathParamsAtClassLevelShouldBeSupported()
  {
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase8());
    assertEquals("did-it-test-case-8dog",
        dispatcher.dispatch(HttpMethod.GET, "/dog/bar"));
  }

  @Test
  public void inheritanceShouldBeCaptured()
  {
    // TODO: Not yet implemented
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase6Impl());
    assertEquals("did-it-test-case-6",
        dispatcher.dispatch(HttpMethod.GET, "/foo/bar"));
  }

  static final int ITERATIONS = 2_700_000;

  @Test
  public void perfTestRegexSlow()
  {
    long start;
    for (int i = 0; i < ITERATIONS; i++)
    {
      Pattern pattern = Pattern.compile("/foo/bar");
      Matcher matcher = pattern.matcher("/foo/bar");
      boolean found = matcher.find();
    }
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      Pattern pattern = Pattern.compile("/foo/bar");
      Matcher matcher = pattern.matcher("/foo/bar");
      boolean found = matcher.find();
    }
    System.out.println("Total time regex slow: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  @Test
  public void perfTestRegexFast()
  {
    long start;
    Pattern pattern = Pattern.compile("/foo/bar");
    for (int i = 0; i < ITERATIONS; i++)
    {
      Matcher matcher = pattern.matcher("/foo/bar");
      boolean found = matcher.find();
    }
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      Matcher matcher = pattern.matcher("/foo/bar");
      boolean found = matcher.find();
    }
    System.out.println("Total time regex fast: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  @Test
  public void perfTestMap()
  {
    Map<String, Map<String, Map<String, Object>>> foo = Map.of("foo",
        Map.of("bar", Map.of(HttpMethod.GET, new Object())));
    String uriNoTrailingSlash = "foo/bar";
    long start;
    for (int i = 0; i < ITERATIONS; i++)
    {
      String[] uriSegments = uriNoTrailingSlash.split("/");
      if (foo.containsKey(uriSegments[0]))
      {
        Map<String, Map<String, Object>> inner = foo.get(uriSegments[0]);
        if (inner.containsKey(uriSegments[1]))
        {
          Map<String, Object> endpointsByHttpMethod = inner.get(
              uriSegments[1]);
          Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
        }
      }
    }
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      String[] uriSegments = uriNoTrailingSlash.split("/");
      if (foo.containsKey(uriSegments[0]))
      {
        Map<String, Map<String, Object>> inner = foo.get(uriSegments[0]);
        if (inner.containsKey(uriSegments[1]))
        {
          Map<String, Object> endpointsByHttpMethod = inner.get(
              uriSegments[1]);
          Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
        }
      }
    }
    System.out.println("Total time map: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  static class Foo {
    public static void main(String... args) {
      JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
      jaxRsDispatcher.register(new FooResource());
      JaxRsDispatcher jaxRsDispatcherCached = new JaxRsDispatcher();
      jaxRsDispatcherCached.setCacheEnabled(true);
      jaxRsDispatcherCached.register(new FooResource());
      JaxRsDispatcher jaxRsDispatcherResinCached = new JaxRsDispatcher();
      jaxRsDispatcherResinCached.setCacheEnabled(true);
      jaxRsDispatcherResinCached.setUseResinCache(true);
      jaxRsDispatcherResinCached.register(new FooResource());
      JaxRsDispatcher jaxRsDispatcherCaffCached = new JaxRsDispatcher();
      jaxRsDispatcherCaffCached.setCacheEnabled(true);
      jaxRsDispatcherCaffCached.setUseCaffCache(true);
      jaxRsDispatcherCaffCached.register(new FooResource());
      JaxRsDispatcherTest test = new JaxRsDispatcherTest();
      test.warmUpJaxRs(jaxRsDispatcher);
      test.warmUpJaxRs(jaxRsDispatcherCached);
      test.warmUpJaxRs(jaxRsDispatcherResinCached);
      test.warmUpJaxRs(jaxRsDispatcherCaffCached);
      long start;
      start = System.currentTimeMillis();
      test.justJaxRs(jaxRsDispatcher);
      System.out.println("Total time dispatchBlocks w/o cache: "
          + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();
      test.justJaxRsCached(jaxRsDispatcherCached);
      System.out.println("Total time dispatchBlocks w/ google cache: "
          + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();
      test.justJaxRsResinCached(jaxRsDispatcherResinCached);
      System.out.println("Total time dispatchBlocks w/ resin cache: "
          + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();
      test.justJaxRsCaffCached(jaxRsDispatcherCaffCached);
      System.out.println("Total time dispatchBlocks w/ caffeine cache: "
          + (System.currentTimeMillis() - start) + "ms");
    }
  }

  public void warmUpJaxRs(JaxRsDispatcher jaxRsDispatcher)
  {
    String uriNoTrailingSlash = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET,
          uriNoTrailingSlash);
      if (i == 0)
      {
        JaxRsDispatcher.DispatchBestMatchInfo match = jaxRsDispatcher
            .getBestMatchInfo(HttpMethod.GET, uriNoTrailingSlash);
        System.out.println("Found " + match.getValues());
      }
    }
  }

  public void justJaxRs(JaxRsDispatcher jaxRsDispatcher)
  {
    String uriNoTrailingSlash = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET,
          uriNoTrailingSlash);
    }
  }

  public void justJaxRsCached(JaxRsDispatcher jaxRsDispatcher)
  {
    String uriNoTrailingSlash = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET,
          uriNoTrailingSlash);
    }
  }

  public void justJaxRsResinCached(JaxRsDispatcher jaxRsDispatcher)
  {
    String uriNoTrailingSlash = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET,
          uriNoTrailingSlash);
    }
  }

  public void justJaxRsCaffCached(JaxRsDispatcher jaxRsDispatcher)
  {
    String uriNoTrailingSlash = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET,
          uriNoTrailingSlash);
    }
  }

  @Test
  public void perfTestJaxRs()
  {
    String uriNoTrailingSlash = "foo/bar";
    JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
    jaxRsDispatcher.register(new FooResource());
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET,
          uriNoTrailingSlash);
      if (i == 0)
      {
        JaxRsDispatcher.DispatchBestMatchInfo match = jaxRsDispatcher
            .getBestMatchInfo(HttpMethod.GET, uriNoTrailingSlash);
        System.out.println("Found " + match.getValues());
      }
    }
    long start;
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET,
          uriNoTrailingSlash);
    }
    System.out.println("Total time dispatchBlocks: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  @Test
  public void perfTestMaps()
  {
    Map<String, Map<String, Map<String, Object>>> foo = Map.of("foo",
        Map.of("bar", Map.of(HttpMethod.GET, new Object())));
    String uriNoTrailingSlash = "foo/bar";
    long start;
    for (int i = 0; i < ITERATIONS; i++)
    {
      String[] uriSegments = uriNoTrailingSlash.split("/");
      if (foo.containsKey(uriSegments[0]))
      {
        Map<String, Map<String, Object>> inner = foo.get(uriSegments[0]);
        if (inner.containsKey(uriSegments[1]))
        {
          Map<String, Object> endpointsByHttpMethod = inner.get(
              uriSegments[1]);
          Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
        }
      }
    }
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      String[] uriSegments = uriNoTrailingSlash.split("/");
      if (foo.containsKey(uriSegments[0]))
      {
        Map<String, Map<String, Object>> inner = foo.get(uriSegments[0]);
        if (inner.containsKey(uriSegments[1]))
        {
          Map<String, Object> endpointsByHttpMethod = inner.get(
              uriSegments[1]);
          Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
        }
      }
    }
    System.out.println("Total time map a: "
        + (System.currentTimeMillis() - start) + "ms");
    Map<String, Map<String, Object>> fooB = Map.of("foo/bar",
        Map.of(HttpMethod.GET, new Object()));
    for (int i = 0; i < ITERATIONS; i++)
    {
      if (fooB.containsKey(uriNoTrailingSlash))
      {
        Map<String, Object> endpointsByHttpMethod = fooB.get(uriNoTrailingSlash);
        Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
      }
    }
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      if (fooB.containsKey(uriNoTrailingSlash))
      {
        Map<String, Object> endpointsByHttpMethod = fooB.get(uriNoTrailingSlash);
        Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
      }
    }
    System.out.println("Total time map b: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  @Test
  public void perfTestAll()
  {
    // Warm-up
    {
      long start;
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        Pattern pattern = Pattern.compile("/foo/bar");
        Matcher matcher = pattern.matcher("/foo/bar");
        boolean found = matcher.find();
      }
      start = System.currentTimeMillis();
      Pattern pattern = Pattern.compile("/foo/bar");
      for (int i = 0; i < ITERATIONS; i++)
      {
        Matcher matcher = pattern.matcher("/foo/bar");
        boolean found = matcher.find();
      }
      Map<String, Map<String, Map<String, Object>>> foo = Map.of("foo",
          Map.of("bar", Map.of(HttpMethod.GET, new Object())));
      // This is better for the map approach
      String uriNoTrailingSlash = "foo/bar";
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        String[] uriSegments = uriNoTrailingSlash.split("/");
        if (foo.containsKey(uriSegments[0]))
        {
          Map<String, Map<String, Object>> inner = foo.get(uriSegments[0]);
          if (inner.containsKey(uriSegments[1]))
          {
            Map<String, Object> endpointsByHttpMethod = inner.get(
                uriSegments[1]);
            Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
          }
        }
      }
      {
        String uri = "foo/bar/";
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.register(new FooResource());

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
          if (i == 0)
          {
            JaxRsDispatcher.DispatchBestMatchInfo match = jaxRsDispatcher
                .getBestMatchInfo(HttpMethod.GET, uri);
            System.out.println("Found " + match.getValues());
          }
        }
        System.out.println("Total time dispatchBlocks: "
            + (System.currentTimeMillis() - start) + "ms");
      }
    }
    {
      long start;
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        Pattern pattern = Pattern.compile("/foo/bar");
        Matcher matcher = pattern.matcher("/foo/bar");
        boolean found = matcher.find();
      }
      System.out.println("Total time regex slow: "
          + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();
      Pattern pattern = Pattern.compile("/foo/bar");
      for (int i = 0; i < ITERATIONS; i++)
      {
        Matcher matcher = pattern.matcher("/foo/bar");
        boolean found = matcher.find();
      }
      System.out.println("Total time regex fast: "
          + (System.currentTimeMillis() - start) + "ms");
      String path = "foo/bar";
      Map<String, Map<String, Map<String, Object>>> foo = Map.of("foo",
          Map.of("bar", Map.of(HttpMethod.GET, new Object())));
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        String[] uriSegments = path.split("/");
        if (foo.containsKey(uriSegments[0]))
        {
          Map<String, Map<String, Object>> inner = foo.get(uriSegments[0]);
          if (inner.containsKey(uriSegments[1]))
          {
            Map<String, Object> endpointsByHttpMethod = inner.get(
                uriSegments[1]);
            Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
          }
        }
      }
      System.out.println("Total time tree-map approach (w/ URI split): "
          + (System.currentTimeMillis() - start) + "ms");
      Map<String, Map<String, Object>> foo2 = Map.of("foo/bar", Map.of(HttpMethod.GET, new Object()));
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        String[] uriSegments = path.split("/");
        if (foo2.containsKey(path))
        {
          Map<String, Object> endpointsByHttpMethod = foo2.get(path);
          Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
        }
      }
      System.out.println("Total time flat-map approach w/ URI split: "
          + (System.currentTimeMillis() - start) + "ms");
      Map<String, Map<String, Object>> foo3 = Map.of("foo/bar", Map.of(HttpMethod.GET, new Object()));
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        //String[] uriSegments = path.split("/");
        if (foo3.containsKey(path))
        {
          Map<String, Object> endpointsByHttpMethod = foo3.get(path);
          Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
        }
      }
      System.out.println("Total time flat-map approach w/o URI split: "
          + (System.currentTimeMillis() - start) + "ms");
      {
        String uri = "foo/bar/";
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.register(new FooResource());

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
        System.out.println("Total time dispatchBlocks w/o LRU cache: "
            + (System.currentTimeMillis() - start) + "ms");
      }
      {
        String uri = "foo/bar/";
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.setCacheEnabled(true);
        jaxRsDispatcher.register(new FooResource());

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
        System.out.println("Total time dispatchBlocks w/ LRU cache: "
            + (System.currentTimeMillis() - start) + "ms");
      }
      {
        String uri = "foo/bar/";
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.setCacheEnabled(true);
        jaxRsDispatcher.setUseResinCache(true);
        jaxRsDispatcher.register(new FooResource());

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
        System.out.println("Total time dispatchBlocks w/ resin LRU cache: "
            + (System.currentTimeMillis() - start) + "ms");
      }
      {
        String uri = "foo/bar/";
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.setCacheEnabled(true);
        jaxRsDispatcher.setUseCaffCache(true);
        jaxRsDispatcher.register(new FooResource());

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
        System.out.println("Total time dispatchBlocks w/ caffeine cache: "
            + (System.currentTimeMillis() - start) + "ms");
      }
    }
  }

  public static class Bar {
    public static void main(String... args)
    {
      JaxRsDispatcherTest test = new JaxRsDispatcherTest();
      //test.perfTestAllComplicated();
      Map<String, Map<String, Map<String, Object>>> foo = Map.of("foo",
          Map.of("bar", Map.of(HttpMethod.GET, new Object())));
      JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
      jaxRsDispatcher.register(new FooResource());
      JaxRsDispatcher jaxRsDispatcherCached = new JaxRsDispatcher();
      jaxRsDispatcherCached.setCacheEnabled(true);
      jaxRsDispatcherCached.register(new FooResource());
      test.combinedWarmUp(foo, jaxRsDispatcher, jaxRsDispatcherCached);
      test.mapApproach(foo);
      test.dispatcherBlocksApproach(jaxRsDispatcher);
      test.dispatcherBlocksCachedApproach(jaxRsDispatcherCached);
    }
  }

  public void combinedWarmUp(Map<String, Map<String, Map<String, Object>>> foo,
                             JaxRsDispatcher jaxRsDispatcher,
                             JaxRsDispatcher jaxRsDispatcherCached)
  {
    String uriNoTrailingSlash = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      String[] uriSegments = uriNoTrailingSlash.split("/");
      if (foo.containsKey(uriSegments[0]))
      {
        Map<String, Map<String, Object>> inner = foo.get(uriSegments[0]);
        if (inner.containsKey(uriSegments[1]))
        {
          Map<String, Object> endpointsByHttpMethod = inner.get(uriSegments[1]);
          Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
        }
      }
    }
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uriNoTrailingSlash);
    }
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcherCached.getBestMatchInfo(HttpMethod.GET,
          uriNoTrailingSlash);
    }
  }

  public void mapApproach(Map<String, Map<String, Map<String, Object>>> foo)
  {
    long start = System.currentTimeMillis();
    String uriNoTrailingSlash = "foo/bar";
    String httpMethod = HttpMethod.GET;
    for (int i = 0; i < ITERATIONS; i++)
    {
      String[] uriSegments = uriNoTrailingSlash.split("/");
      if (foo.containsKey(uriSegments[0]))
      {
        Map<String, Map<String, Object>> inner = foo.get(uriSegments[0]);
        if (inner.containsKey(uriSegments[1]))
        {
          Map<String, Object> endpointsByHttpMethod = inner.get(uriSegments[1]);
          Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
        }
      }
    }
    System.out.println("Total time map: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  public void dispatcherBlocksApproach(JaxRsDispatcher jaxRsDispatcher)
  {
    // This is better for the blocks approach
    String uriNoTrailingSlash = "foo/bar";
    long start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uriNoTrailingSlash);
    }
    System.out.println("Total time dispatchBlocks w/o cache: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  public void dispatcherBlocksCachedApproach(JaxRsDispatcher jaxRsDispatcher)
  {
    // This is better for the blocks approach
    String uriNoTrailingSlash = "foo/bar";
    long start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uriNoTrailingSlash);
    }
    System.out.println("Total time dispatchBlocks w/ cache: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  @Test
  public void perfTestAllComplicated()
  {
    // Warm-up
    {
      var uuidGroupNameStr = "g" + UUID.randomUUID().toString().replaceAll("-", "");
      var uuidStr = UUID.randomUUID().toString();
      String uri = "foo/" + uuidStr + "/";
      long start;
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        Pattern pattern = Pattern.compile("foo/(?<" + uuidGroupNameStr + ">[^/]+?)/.*");
        Matcher matcher = pattern.matcher(uri);
        boolean found = matcher.find();
        String matchFound = matcher.group(uuidGroupNameStr);
        if (i == 0)
        {
          System.out.println("Found " + matchFound);
        }
      }
      start = System.currentTimeMillis();
      Pattern pattern = Pattern.compile("foo/(?<" + uuidGroupNameStr + ">[^/]+?)/.*");
      for (int i = 0; i < ITERATIONS; i++)
      {
        Matcher matcher = pattern.matcher(uri);
        boolean found = matcher.find();
        String matchFound = matcher.group(uuidGroupNameStr);
        if (i == 0)
        {
          System.out.println("Found " + matchFound);
        }
      }
      JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
      jaxRsDispatcher.register(new FooResourceVar());
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        if (i == 0)
        {
          JaxRsDispatcher.DispatchBestMatchInfo match = jaxRsDispatcher
              .getBestMatchInfo(HttpMethod.GET, uri);
          System.out.println("Found " + match.getValues());
        }
      }
      JaxRsDispatcher jaxRsDispatcherCached = new JaxRsDispatcher();
      jaxRsDispatcherCached.setCacheEnabled(true);
      jaxRsDispatcherCached.register(new FooResourceVar());
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        jaxRsDispatcherCached.getBestMatchInfo(HttpMethod.GET, uri);
        if (i == 5)
        {
          JaxRsDispatcher.DispatchBestMatchInfo match = jaxRsDispatcherCached
              .getBestMatchInfo(HttpMethod.GET, uri);
          System.out.println("Found " + match.getValues());
        }
      }
      JaxRsDispatcher jaxRsDispatcherCachedResin = new JaxRsDispatcher();
      jaxRsDispatcherCachedResin.setCacheEnabled(true);
      jaxRsDispatcherCachedResin.setUseResinCache(true);
      jaxRsDispatcherCachedResin.register(new FooResourceVar());
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        jaxRsDispatcherCachedResin.getBestMatchInfo(HttpMethod.GET, uri);
        if (i == 5)
        {
          JaxRsDispatcher.DispatchBestMatchInfo match = jaxRsDispatcherCachedResin
              .getBestMatchInfo(HttpMethod.GET, uri);
          System.out.println("Found " + match.getValues());
        }
      }
      JaxRsDispatcher jaxRsDispatcherCachedCaff = new JaxRsDispatcher();
      jaxRsDispatcherCachedCaff.setCacheEnabled(true);
      jaxRsDispatcherCachedCaff.setUseCaffCache(true);
      jaxRsDispatcherCachedCaff.register(new FooResourceVar());
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        jaxRsDispatcherCachedCaff.getBestMatchInfo(HttpMethod.GET, uri);
        if (i == 5)
        {
          JaxRsDispatcher.DispatchBestMatchInfo match = jaxRsDispatcherCachedCaff
              .getBestMatchInfo(HttpMethod.GET, uri);
          System.out.println("Found " + match.getValues());
        }
      }
    }
    {
      var uuidGroupNameStr = "g" + UUID.randomUUID().toString().replaceAll("-", "");
      var uuidStr = UUID.randomUUID().toString();
      String uri = "foo/" + uuidStr + "/";
      // This is better for the map approach
      long start;
      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        Pattern pattern = Pattern.compile("foo/(?<" + uuidGroupNameStr + ">[^/]+?)/.*");
        Matcher matcher = pattern.matcher(uri);
        boolean found = matcher.find();
        String matchFound = matcher.group(uuidGroupNameStr);
      }
      System.out.println("Total time regex slow: "
          + (System.currentTimeMillis() - start) + "ms");
      start = System.currentTimeMillis();
      Pattern pattern = Pattern.compile("foo/(?<" + uuidGroupNameStr + ">[^/]+?)/.*");
      for (int i = 0; i < ITERATIONS; i++)
      {
        Matcher matcher = pattern.matcher(uri);
        boolean found = matcher.find();
        String matchFound = matcher.group(uuidGroupNameStr);
      }
      System.out.println("Total time regex fast: "
          + (System.currentTimeMillis() - start) + "ms");
      {
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.register(new FooResourceVar());
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
        System.out.println("Total time dispatchBlocks w/o LRU cache: "
            + (System.currentTimeMillis() - start) + "ms");
      }
      {
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.setCacheEnabled(true);
        jaxRsDispatcher.register(new FooResourceVar());

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
        System.out.println("Total time dispatchBlocks w/ LRU cache: "
            + (System.currentTimeMillis() - start) + "ms");
      }
      {
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.setCacheEnabled(true);
        jaxRsDispatcher.setUseResinCache(true);
        jaxRsDispatcher.register(new FooResourceVar());

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
        System.out.println("Total time dispatchBlocks w/ resin LRU cache: "
            + (System.currentTimeMillis() - start) + "ms");
      }
      {
        JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
        jaxRsDispatcher.setCacheEnabled(true);
        jaxRsDispatcher.setUseCaffCache(true);
        jaxRsDispatcher.register(new FooResourceVar());

        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
        System.out.println("Total time dispatchBlocks w/ caffeine cache: "
            + (System.currentTimeMillis() - start) + "ms");
      }
      {
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
          List<JaxRsDispatcher.DispatchMatch> matches = new ArrayList<>(1);
          matches.add(
              new JaxRsDispatcher.DispatchMatch(null, null, null, null));
          List<JaxRsDispatcher.DispatchMatch> matches2 = new ArrayList<>(1);
          matches2.add(
              new JaxRsDispatcher.DispatchMatch(null, null, null, null));
        }
        System.out.println("Total time create objects: "
            + (System.currentTimeMillis() - start) + "ms");
      }
      /*JaxRsDispatcher.DispatchBlock top = new JaxRsDispatcher.DispatchBlock(null);
      top.fullSegmentChildren = new JaxRsDispatcher.ChildDispatchBlockGroup();
      JaxRsDispatcher.DispatchBlock foo = new JaxRsDispatcher.DispatchBlock(null);
      top.fullSegmentChildren.addChildWordBlock("foo", foo);

      foo.fullSegmentChildren = new JaxRsDispatcher.ChildDispatchBlockGroup();
      JaxRsDispatcher.DispatchBlock variableBlock = new JaxRsDispatcher.DispatchBlock(null);
      foo.fullSegmentChildren.setChildPureVariableBlock(variableBlock);

      start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++)
      {
        String[] uriSegments = uriNoTrailingSlash.split("/");
        if (top.fullSegmentChildren != null) {
          JaxRsDispatcher.DispatchBlock fooInner = top.fullSegmentChildren.getChildWordBlock(uriSegments[0]);
          if (fooInner.fullSegmentChildren != null) {
            JaxRsDispatcher.DispatchBlock pureVariableBlock = fooInner.fullSegmentChildren.getChildPureVariableBlock();
            if (pureVariableBlock != null) {
              Map<String, JaxRsDispatcher.Endpoint> endpointsByHttpMethod = pureVariableBlock.endpointsByHttpMethod;
            }
          }
        }
      }
      System.out.println("Total time dispatchBlocks: " + (System.currentTimeMillis() - start) + "ms");*/
    }
  }
}