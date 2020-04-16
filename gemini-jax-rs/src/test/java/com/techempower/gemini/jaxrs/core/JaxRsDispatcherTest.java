package com.techempower.gemini.jaxrs.core;

import org.junit.Test;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

public class JaxRsDispatcherTest
{
  @Path("/foo/{dog: .+}{cat: .+}")
  public static class Foo {
    @Path("/bar")
    public Response doIt(@PathParam("dog") String dog) {
      return Response.ok().build();
    }
  }

  @Test
  public void testIt() {

  }
}