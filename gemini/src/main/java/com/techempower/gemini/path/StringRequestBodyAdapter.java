package com.techempower.gemini.path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Type;

import com.techempower.gemini.BasicContext;

/**
 * Reads the request body into a String.
 *
 * @see com.techempower.gemini.path.annotation.ConsumesString
 */
public class StringRequestBodyAdapter implements RequestBodyAdapter<BasicContext>
{
  private static final int BUFFER_SIZE = 8 * 1024;

  @Override
  public Object read(BasicContext context, Type type) throws RequestBodyException
  {
    try (StringWriter out = new StringWriter())
    {
      BufferedReader reader = new BufferedReader(
              new InputStreamReader(context.getRequest().getInputStream(),
                      context.getRequest().getRequestCharacterEncoding()));
      char[] buffer = new char[BUFFER_SIZE];
      int n;
      while ((n = reader.read(buffer)) != -1)
      {
        out.write(buffer, 0, n);
      }

      return out.toString();
    }
    catch (IOException e)
    {
      throw new RequestBodyException(500, "Error reading request body.", e);
    }
  }
}
