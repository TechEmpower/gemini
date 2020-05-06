package com.techempower.gemini.jaxrs.core;

import com.esotericsoftware.reflectasm.MethodAccess;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

  @Path("/foo")
  public static class TestCase6Impl
      implements TestCase6
  {
    @Override
    public String doIt()
    {
      return "did-it-test-case-6";
    }
  }

  public static class TestCase6Impl2
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
  public void methodAnnotationInheritanceShouldBeCaptured()
  {
    // TODO: Not yet implemented
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase6Impl());
    assertEquals("did-it-test-case-6",
        dispatcher.dispatch(HttpMethod.GET, "/foo/bar"));
  }

  @Test
  public void classAnnotationInheritanceShouldNotBeCaptured()
  {
    // TODO: Not yet implemented
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestCase6Impl2());
    // TODO: Null isn't really a great indicator here, since a given
    //  method could very well have returned null.
    assertNull(dispatcher.dispatch(HttpMethod.GET, "/foo/bar"));
  }

  static class SimpleUriPerformanceTest
  {
    public static class Runner
    {
      public static void main(String... args)
      {
        Performance.test(SimpleUriPerformanceTest.class,
            ITERATIONS,
            List.of(
                List.of(REGEX_SLOW),
                List.of(REGEX_FAST),
                List.of(MAP_TREE),
                List.of(MAP_FLAT),
                List.of(MAP_FLAT_WITH_URI_SPLIT),
                // TODO: Make a builder for generating these combinations.
                //   Something like:
                //     Performance.combinations()
                //         .next(JAX_RS)
                //         .next(GUAVA_LOADING_CACHE, RESIN_LRU_CACHE,
                //             CAFFEINE_LOADING_CACHE)
                //         .next(/* etc */)
                //         .build()
                List.of(JAX_RS, NO_CACHE),
                List.of(JAX_RS, GUAVA_LOADING_CACHE),
                List.of(JAX_RS, RESIN_LRU_CACHE),
                List.of(JAX_RS, CAFFEINE_LOADING_CACHE)
            ));
      }
    }

    static final int    ITERATIONS              = 2_700_000;
    // approaches
    static final String REGEX_SLOW              = "regex slow";
    static final String REGEX_FAST              = "regex fast";
    static final String MAP_TREE                = "tree of maps";
    static final String MAP_FLAT                = "single flat map";
    static final String MAP_FLAT_WITH_URI_SPLIT = "single flat map with URI split";
    static final String JAX_RS                  = "jax-rs";
    // jax-rs customizations
    static final String NO_CACHE                = "no cache";
    static final String GUAVA_LOADING_CACHE     = "Guava LoadingCache";
    static final String RESIN_LRU_CACHE         = "Resin LruCache";
    static final String CAFFEINE_LOADING_CACHE  = "Caffeine LoadingCache";

    public static void main(String... args)
        throws Exception
    {
      if (args.length > 0)
      {
        switch (args[0])
        {
          case REGEX_SLOW:
            perfTestRegexSlow();
            break;
          case REGEX_FAST:
            perfTestRegexFast();
            break;
          case MAP_TREE:
            perfTestMapTree();
            break;
          case MAP_FLAT:
            perfTestMapFlat();
            break;
          case MAP_FLAT_WITH_URI_SPLIT:
            perfTestMapFlatWithUriSplit();
            break;
          case JAX_RS:
          {
            JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
            jaxRsDispatcher.register(new FooResource());
            switch (args[1])
            {
              case NO_CACHE:
                break;
              case GUAVA_LOADING_CACHE:
                jaxRsDispatcher.setCacheEnabled(true);
                break;
              case RESIN_LRU_CACHE:
                jaxRsDispatcher.setCacheEnabled(true);
                jaxRsDispatcher.setUseResinCache(true);
                break;
              case CAFFEINE_LOADING_CACHE:
                jaxRsDispatcher.setCacheEnabled(true);
                jaxRsDispatcher.setUseCaffCache(true);
                break;
              default:
                throw new RuntimeException();
            }
            perfTestJaxRs(jaxRsDispatcher);
            break;
          }
          default:
            throw new RuntimeException();
        }
      }
    }

    public static void perfTestRegexSlow()
    {
      String uriNoTrailingSlash = "foo/bar";
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          Pattern pattern = Pattern.compile("foo/bar");
          Matcher matcher = pattern.matcher(uriNoTrailingSlash);
          boolean found = matcher.find();
        }
      });
    }

    public static void perfTestRegexFast()
    {
      String uriNoTrailingSlash = "foo/bar";
      Pattern pattern = Pattern.compile("foo/bar");
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          Matcher matcher = pattern.matcher(uriNoTrailingSlash);
          boolean found = matcher.find();
        }
      });
    }

    public static void perfTestMapTree()
    {
      Map<String, Map<String, Map<String, Object>>> foo = Map.of("foo",
          Map.of("bar", Map.of(HttpMethod.GET, new Object())));
      String uriNoTrailingSlash = "foo/bar";
      Performance.time(() -> {
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
      });
    }

    public static void perfTestMapFlat()
    {
      Map<String, Map<String, Object>> fooB = Map.of("foo/bar",
          Map.of(HttpMethod.GET, new Object()));
      String uriNoTrailingSlash = "foo/bar";
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          if (fooB.containsKey(uriNoTrailingSlash))
          {
            Map<String, Object> endpointsByHttpMethod = fooB.get(uriNoTrailingSlash);
            Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
          }
        }
      });
    }

    public static void perfTestMapFlatWithUriSplit()
    {
      Map<String, Map<String, Object>> fooB = Map.of("foo/bar",
          Map.of(HttpMethod.GET, new Object()));
      String uriNoTrailingSlash = "foo/bar";
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          String[] uriSegments = uriNoTrailingSlash.split("/");
          if (fooB.containsKey(uriNoTrailingSlash))
          {
            Map<String, Object> endpointsByHttpMethod = fooB.get(uriNoTrailingSlash);
            Object resource = endpointsByHttpMethod.get(HttpMethod.GET);
          }
        }
      });
    }

    public static void perfTestJaxRs(JaxRsDispatcher jaxRsDispatcher)
    {
      String uriNoTrailingSlash = "foo/bar";
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET,
              uriNoTrailingSlash);
        }
      });
    }
  }

  static class ComplexUriPerformanceTest
  {
    public static class Runner
    {
      public static void main(String... args)
      {
        Performance.test(ComplexUriPerformanceTest.class,
            ITERATIONS,
            List.of(
                List.of(REGEX_SLOW),
                List.of(REGEX_FAST),
                List.of(JAX_RS, NO_CACHE),
                List.of(JAX_RS, GUAVA_LOADING_CACHE),
                List.of(JAX_RS, RESIN_LRU_CACHE),
                List.of(JAX_RS, CAFFEINE_LOADING_CACHE),
                List.of(CREATE_OBJECTS)
            ));
      }
    }

    static final int    ITERATIONS             = 2_700_000;
    // approaches
    static final String REGEX_SLOW             = "regex slow";
    static final String REGEX_FAST             = "regex fast";
    static final String JAX_RS                 = "jax-rs";
    static final String CREATE_OBJECTS         = "create objects (subset of jax-rs)";
    // jax-rs customizations
    static final String NO_CACHE               = "no cache";
    static final String GUAVA_LOADING_CACHE    = "Guava LoadingCache";
    static final String RESIN_LRU_CACHE        = "Resin LruCache";
    static final String CAFFEINE_LOADING_CACHE = "Caffeine LoadingCache";

    public static void main(String... args)
        throws Exception
    {
      if (args.length > 0)
      {
        switch (args[0])
        {
          case REGEX_SLOW:
            perfTestRegexSlow();
            break;
          case REGEX_FAST:
            perfTestRegexFast();
            break;
          case JAX_RS:
          {
            JaxRsDispatcher jaxRsDispatcher = new JaxRsDispatcher();
            jaxRsDispatcher.register(new FooResourceVar());
            switch (args[1])
            {
              case NO_CACHE:
                break;
              case GUAVA_LOADING_CACHE:
                jaxRsDispatcher.setCacheEnabled(true);
                break;
              case RESIN_LRU_CACHE:
                jaxRsDispatcher.setCacheEnabled(true);
                jaxRsDispatcher.setUseResinCache(true);
                break;
              case CAFFEINE_LOADING_CACHE:
                jaxRsDispatcher.setCacheEnabled(true);
                jaxRsDispatcher.setUseCaffCache(true);
                break;
              default:
                throw new RuntimeException();
            }
            perfTestJaxRs(jaxRsDispatcher);
            break;
          }
          case CREATE_OBJECTS:
            perfTestCreateObjects();
            break;
          default:
            throw new RuntimeException();
        }
      }
    }

    public static void perfTestRegexSlow()
    {
      var uuidGroupNameStr = "g" + UUID.randomUUID().toString().replaceAll("-", "");
      var uuidStr = UUID.randomUUID().toString();
      String uri = "foo/" + uuidStr + "/";
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          Pattern pattern = Pattern.compile("foo/(?<" + uuidGroupNameStr + ">[^/]+?)/.*");
          Matcher matcher = pattern.matcher(uri);
          boolean found = matcher.find();
          String matchFound = matcher.group(uuidGroupNameStr);
        }
      });
    }

    public static void perfTestRegexFast()
    {
      var uuidGroupNameStr = "g" + UUID.randomUUID().toString().replaceAll("-", "");
      var uuidStr = UUID.randomUUID().toString();
      String uri = "foo/" + uuidStr + "/";
      Pattern pattern = Pattern.compile("foo/(?<" + uuidGroupNameStr + ">[^/]+?)/.*");
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          Matcher matcher = pattern.matcher(uri);
          boolean found = matcher.find();
          String matchFound = matcher.group(uuidGroupNameStr);
        }
      });
    }

    public static void perfTestJaxRs(JaxRsDispatcher jaxRsDispatcher)
    {
      var uuidStr = UUID.randomUUID().toString();
      String uri = "foo/" + uuidStr + "/";
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          jaxRsDispatcher.getBestMatchInfo(HttpMethod.GET, uri);
        }
      });
    }

    public static void perfTestCreateObjects()
    {
      Performance.time(() -> {
        for (int i = 0; i < ITERATIONS; i++)
        {
          //noinspection MismatchedQueryAndUpdateOfCollection
          List<JaxRsDispatcher.DispatchMatch> matches = new ArrayList<>(1);
          matches.add(
              new JaxRsDispatcher.DispatchMatch(null, null, null, null));
        }
      });
    }
  }

  static class MethodCallPerformanceTest
  {
    public static class Runner
    {
      public static void main(String... args)
      {
        Performance.test(MethodCallPerformanceTest.class,
            ITERATIONS,
            List.of(
                List.of(DIRECT_METHOD_CALL),
                List.of(REFLECTION_LIBRARY),
                List.of(METHOD_REFERENCE),
                List.of(STANDARD_REFLECTION)
            ));
      }
    }

    static final int    ITERATIONS          = 1_000_000_000;
    static final String DIRECT_METHOD_CALL  = "direct method call";
    static final String REFLECTION_LIBRARY  = "reflection library";
    static final String METHOD_REFERENCE    = "method reference";
    static final String STANDARD_REFLECTION = "standard reflection";

    static class Cow
    {
      public void moo()
      {
      }
    }

    public static void main(String... args)
        throws Exception
    {
      if (args.length > 0)
      {
        switch (args[0])
        {
          case DIRECT_METHOD_CALL:
            perfTestDirect();
            break;
          case REFLECTION_LIBRARY:
            perfTestMethodAccess();
            break;
          case METHOD_REFERENCE:
            perfTestReference();
            break;
          case STANDARD_REFLECTION:
            perfTestReflection();
            break;
        }
      }
    }

    public static void perfTestDirect()
    {
      Cow cow = new Cow();
      for (int i = 0; i < ITERATIONS; i++)
      {
        cow.moo();
      }
      long start = System.nanoTime();
      for (int i = 0; i < ITERATIONS; i++)
      {
        cow.moo();
      }
      System.out.print(System.nanoTime() - start);
    }

    public static void perfTestReference()
    {
      Cow cow = new Cow();
      Runnable moo = cow::moo;
      for (int i = 0; i < ITERATIONS; i++)
      {
        moo.run();
      }
      long start = System.nanoTime();
      for (int i = 0; i < ITERATIONS; i++)
      {
        moo.run();
      }
      System.out.print(System.nanoTime() - start);
    }

    public static void perfTestReflection()
        throws Exception
    {
      Cow cow = new Cow();
      Method method = cow.getClass().getMethod("moo");
      method.setAccessible(true);
      for (int i = 0; i < ITERATIONS; i++)
      {
        method.invoke(cow);
      }
      long start = System.nanoTime();
      for (int i = 0; i < ITERATIONS; i++)
      {
        method.invoke(cow);
      }
      System.out.print(System.nanoTime() - start);
    }

    public static void perfTestMethodAccess()
    {
      Cow cow = new Cow();
      MethodAccess methodAccess = MethodAccess.get(Cow.class);
      int index = methodAccess.getIndex("moo");
      for (int i = 0; i < ITERATIONS; i++)
      {
        methodAccess.invoke(cow, index);
      }
      long start = System.nanoTime();
      for (int i = 0; i < ITERATIONS; i++)
      {
        methodAccess.invoke(cow, index);
      }
      System.out.print(System.nanoTime() - start);
    }
  }
}