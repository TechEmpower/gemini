package com.techempower.gemini.path;

import com.techempower.gemini.Context;
import com.techempower.gemini.Request;
import com.techempower.gemini.path.annotation.Get;
import com.techempower.gemini.path.annotation.Path;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnnotationDispatcherTest
{
  @Path("foo")
  public static class FooResource {
    @Get
    @Path("bar")
    public Map bar() {
      return Map.of();
    }
  }

  @Path("foo")
  public static class FooResourceVar {
    @Get
    @Path("{bar}")
    public Map bar(String bar) {
      return Map.of();
    }
  }

  //static final int ITERATIONS = 2_700_000;
  static final int ITERATIONS = 2_700_000;

  /*@Test
  public void blah() {
    AnnotationDispatcher<Context> dispatcher = new AnnotationDispatcher<>();
    dispatcher.register(new FooResource());
    long start;
    Context context = mock(Context.class);
    Request request = mock(Request.class);
    when(context.getRequestUri()).thenReturn("foo/bar");
    when(context.getRequest()).thenReturn(request);
    when(request.getRequestMethod()).thenReturn(Request.HttpMethod.GET);
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      dispatcher.dispatch(context);
    }
    System.out.println("Total time kain-approach: " + (System.currentTimeMillis() - start) + "ms");
  }*/

  public static void main(String...args) {
    AnnotationDispatcher<Context> dispatcher = new AnnotationDispatcher<>();
    dispatcher.register(new FooResource());
    AnnotationDispatcherTest test = new AnnotationDispatcherTest();
    test.warmUpBlah(dispatcher);
    long start;
    start = System.currentTimeMillis();
    test.doBlah(dispatcher);
    System.out.println("Total time kain-approach: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  // I know these are identical, but it's easier to distinguish the warm up
  // from the "real" in the profiler this way.
  public void warmUpBlah(AnnotationDispatcher<Context> dispatcher) {
    String uri = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      dispatcher.dispatch(Request.HttpMethod.GET, uri);
    }
  }

  public void doBlah(AnnotationDispatcher<Context> dispatcher) {
    String uri = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      dispatcher.dispatch(Request.HttpMethod.GET, uri);
    }
  }

  @Test
  public void blah() {
    AnnotationDispatcher<Context> dispatcher = new AnnotationDispatcher<>();
    dispatcher.register(new FooResource());
    String uri = "foo/bar";
    for (int i = 0; i < ITERATIONS; i++)
    {
      dispatcher.dispatch(Request.HttpMethod.GET, uri);
    }
    long start;
    /*Context context = mock(Context.class);
    Request request = mock(Request.class);*/
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      dispatcher.dispatch(Request.HttpMethod.GET, uri);
    }
    System.out.println("Total time kain-approach: "
        + (System.currentTimeMillis() - start) + "ms");
  }

  @Test
  public void blahVariable() {
    AnnotationDispatcher<Context> dispatcher = new AnnotationDispatcher<>();
    dispatcher.register(new FooResourceVar());
    var uuidStr = UUID.randomUUID().toString();
    var uri = "foo/" + uuidStr;
    long start;
    /*Context context = mock(Context.class);
    Request request = mock(Request.class);*/
    for (int i = 0; i < ITERATIONS; i++)
    {
      dispatcher.dispatch(Request.HttpMethod.GET, uri);
    }
    start = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++)
    {
      dispatcher.dispatch(Request.HttpMethod.GET, uri);
    }
    System.out.println("Total time kain-approach: "
        + (System.currentTimeMillis() - start) + "ms");
  }
}
