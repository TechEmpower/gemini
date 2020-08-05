package com.techempower.gemini.jaxrs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.util.*;


class SimpleEndpointRegistry implements EndpointRegistry
{
  private final List<EndpointMetadataPair> endpointPairs   = new ArrayList<>();
  private final MediaTypeParser            mediaTypeParser = new MediaTypeParserImpl("q");
  private final Logger                     log             = LoggerFactory.getLogger(getClass());

  // Should probably make it part of the spec that getEndpointFor's headers
  // are already lowercase or something.
  @Override
  public Endpoint getEndpointFor(String httpMethod,
                                 String uri,
                                 List<Map.Entry<String, String>> headers)
  {
    String contentTypeHeader = null;
    String acceptHeader = null;
    List<CombinedEndpointMediaType> contentTypeEntries = new ArrayList<>();
    List<CombinedEndpointMediaType> acceptsEntries = new ArrayList<>();
    for (Map.Entry<String, String> header : headers)
    {
      if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(header.getKey()))
      {
        contentTypeHeader = header.getValue();
      }
      else if (HttpHeaders.ACCEPT.equalsIgnoreCase(header.getKey()))
      {
        acceptHeader = header.getValue();
      }
    }
    QMediaTypeGroup contentTypeGroup = Optional.ofNullable(contentTypeHeader)
        .map(mediaTypeParser::parse)
        .orElse(QMediaTypeGroup.DEFAULT_WILDCARD_GROUP);
    QMediaTypeGroup acceptGroup = Optional.ofNullable(acceptHeader)
        .map(mediaTypeParser::parse)
        .orElse(QMediaTypeGroup.DEFAULT_WILDCARD_GROUP);
    for (EndpointMetadataPair endpointEntry : endpointPairs)
    {
      Endpoint endpoint = endpointEntry.getEndpoint();
      EndpointMetadata metadata = endpointEntry.getEndpointMetadata();
      if (!metadata.getHttpMethods().contains(httpMethod))
      {
        continue;
      }
      // Note: This does not normalize the URI, or consider path params
      if (!metadata.getPath().equalsIgnoreCase(uri))
      {
        continue;
      }
      // MediaType matching formula:
      //   - Section: https://download.oracle.com/javaee-archive/jax-rs-spec.java.net/jsr339-experts/att-3593/spec.pdf#subsection.3.7.2
      //   - Starts on page: https://download.oracle.com/javaee-archive/jax-rs-spec.java.net/jsr339-experts/att-3593/spec.pdf#page=34
      List<QMediaType> consumesServerTypes = metadata.getMediaTypeConsumes()
          .getMediaTypes();
      if (consumesServerTypes.isEmpty())
      {
        consumesServerTypes = List.of(WrappedQMediaType.DEFAULT_WILDCARD);
      }
      for (QMediaType contentTypeClientType : contentTypeGroup.getMediaTypes())
      {
        for (QMediaType consumesServerType : consumesServerTypes)
        {
          contentTypeEntries.add(new CombinedEndpointMediaType(
              SimpleCombinedQMediaType.create(contentTypeClientType,
                  consumesServerType), endpoint));
        }
      }
      List<QMediaType> producesServerTypes = metadata.getMediaTypeProduces()
          .getMediaTypes();
      if (producesServerTypes.isEmpty())
      {
        producesServerTypes = List.of(WrappedQMediaType.DEFAULT_WILDCARD);
      }
      for (QMediaType acceptsClientType : acceptGroup.getMediaTypes())
      {
        for (QMediaType producesServerType : producesServerTypes)
        {
          acceptsEntries.add(new CombinedEndpointMediaType(
              SimpleCombinedQMediaType.create(acceptsClientType,
                  producesServerType), endpoint));
        }
      }
    }
    if (contentTypeEntries.isEmpty())
    {
      return null;
    }
    contentTypeEntries.sort(Comparator.reverseOrder());
    CombinedEndpointMediaType best = contentTypeEntries.get(0);
    if (contentTypeEntries.size() > 1)
    {
      CombinedEndpointMediaType second = contentTypeEntries.get(1);
      if (best.compareTo(second) == 0)
      {
        acceptsEntries.sort(Comparator.reverseOrder());
        best = acceptsEntries.get(0);
        if (acceptsEntries.size() > 1)
        {
          second = acceptsEntries.get(1);
          if (best.compareTo(second) == 0)
          {
            log.warn("Multiple matches found for {} {}, content type: {}, accept: {}",
                httpMethod, uri, contentTypeHeader, acceptHeader);
          }
        }
      }
    }
    return best.getEndpoint();
  }

  private static class EndpointMetadataPair
  {
    private final EndpointMetadata endpointMetadata;
    private final Endpoint         endpoint;

    public EndpointMetadataPair(EndpointMetadata endpointMetadata,
                                Endpoint endpoint)
    {
      this.endpointMetadata = endpointMetadata;
      this.endpoint = endpoint;
    }

    public EndpointMetadata getEndpointMetadata()
    {
      return endpointMetadata;
    }

    public Endpoint getEndpoint()
    {
      return endpoint;
    }
  }

  private static class CombinedEndpointMediaType
      implements Comparable<CombinedEndpointMediaType>
  {
    private final SimpleCombinedQMediaType combinedQMediaType;
    private final Endpoint                 endpoint;

    public CombinedEndpointMediaType(SimpleCombinedQMediaType combinedQMediaType,
                                     Endpoint endpoint)
    {
      this.combinedQMediaType = combinedQMediaType;
      this.endpoint = endpoint;
    }

    public SimpleCombinedQMediaType getCombinedQMediaType()
    {
      return combinedQMediaType;
    }

    public Endpoint getEndpoint()
    {
      return endpoint;
    }

    @Override
    public int compareTo(CombinedEndpointMediaType that)
    {
      return this.combinedQMediaType.compareTo(that.combinedQMediaType);
    }
  }

  @Override
  public void register(EndpointMetadata metadata, Endpoint endpoint)
  {
    endpointPairs.add(new EndpointMetadataPair(metadata, endpoint));
  }

}
