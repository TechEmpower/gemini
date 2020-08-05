package com.techempower.gemini.jaxrs.core;

import java.util.List;
import java.util.Map;

public interface Endpoint
{
  // TODO: Will likely want to change this to something more meaningful, like
  //  UriPathInfo or whatever it's called
  Object invoke(String httpMethod,
                String uri,
                List<Map.Entry<String, String>> headers,
                Map<String, String> pathParams,
                Map<String, String> queryParams,
                String body);
}
