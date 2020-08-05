package com.techempower.gemini.jaxrs.core;

import java.util.List;
import java.util.Map;

public interface EndpointRegistry
{
  Endpoint getEndpointFor(String httpMethod,
                          String uri,
                          // TODO: Eventually this will all be in a lazy-loaded UriInfo
                          List<Map.Entry<String, String>> headers);

  void register(EndpointMetadata metadata, Endpoint endpoint);
}
