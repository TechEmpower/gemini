package com.techempower.gemini.jaxrs.core;

import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JaxRsMediaTypeDispatchingTest
{
  @Path("test0")
  private static class TestResource0 {
    @Path("x")
    @Consumes(MediaType.APPLICATION_JSON)
    @GET
    public String handleX0() {
      return "handle-test0-x0";
    }

    @Path("x")
    @Consumes(MediaType.APPLICATION_XML)
    @GET
    public String handleX1() {
      return "handle-test0-x1";
    }
  }

  private JaxRsDispatcher newJaxRsDispatcher() {
    var dispatcher = new JaxRsDispatcher();
    dispatcher.register(new TestResource0());
    return dispatcher;
  }

  @Test
  public void doTest() {
    var dispatcher = newJaxRsDispatcher();
    assertEquals("handle-test0-x0", dispatcher.dispatch(HttpMethod.GET,
        "/test0/x", List.of(
            Map.entry(HttpHeaders.CONTENT_TYPE,
                "application/xml; q=0.2, application/json; q=0.7"))));
    assertEquals("handle-test0-x1", dispatcher.dispatch(HttpMethod.GET,
        "/test0/x", List.of(
            Map.entry(HttpHeaders.CONTENT_TYPE,
                "application/xml; q=0.7, application/json; q=0.2"))));
  }
}
