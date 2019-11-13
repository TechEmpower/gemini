package com.techempower.js;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Uses the Jackson JSON serialization library to read JSON to
 * Java objects.
 */
public class JacksonJavaScriptReader implements JavaScriptReader
{
  private final ObjectMapper mapper;

  public JacksonJavaScriptReader()
  {
    this.mapper = new ObjectMapper();
  }

  @Override
  public <T> T read(String json, Type type)
  {
    try
    {
      JavaType javaType = mapper.getTypeFactory().constructType(type);
      return mapper.readValue(json, javaType);
    }
    catch (IOException e)
    {
      throw new JavaScriptError("Jackson exception.", e);
    }
  }

  @Override
  public <T> T read(InputStream inputStream, Type type)
  {
    try
    {
      JavaType javaType = mapper.getTypeFactory().constructType(type);
      return mapper.readValue(inputStream, javaType);
    }
    catch (IOException e)
    {
      throw new JavaScriptError("Jackson exception.", e);
    }
  }
}
