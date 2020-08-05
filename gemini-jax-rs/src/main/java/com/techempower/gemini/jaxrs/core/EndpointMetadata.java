package com.techempower.gemini.jaxrs.core;

import java.util.Set;

public class EndpointMetadata
{
  private final String path;
  private final Set<String> httpMethods;
  private final QMediaTypeGroup mediaTypeConsumes;
  private final QMediaTypeGroup mediaTypeProduces;

  public EndpointMetadata(String path,
                          Set<String> httpMethods,
                          QMediaTypeGroup mediaTypeConsumes,
                          QMediaTypeGroup mediaTypeProduces)
  {
    this.path = path;
    this.httpMethods = httpMethods;
    this.mediaTypeConsumes = mediaTypeConsumes;
    this.mediaTypeProduces = mediaTypeProduces;
  }

  public String getPath()
  {
    return path;
  }

  public Set<String> getHttpMethods()
  {
    return httpMethods;
  }

  public QMediaTypeGroup getMediaTypeConsumes()
  {
    return mediaTypeConsumes;
  }

  public QMediaTypeGroup getMediaTypeProduces()
  {
    return mediaTypeProduces;
  }
}
