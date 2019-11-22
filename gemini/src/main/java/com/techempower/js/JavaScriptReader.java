package com.techempower.js;

import java.io.InputStream;
import java.lang.reflect.Type;

import com.techempower.gemini.GeminiApplication;

/**
 * Implementations of this interface can read JSON to Java objects. The
 * standard implementation uses the popular Jackson library.
 *
 * @see GeminiApplication#constructJavaScriptReader()
 */
public interface JavaScriptReader
{
  /**
   * De-serialize the given json to the given type.
   *
   * @param json The raw JSON string to de-serialize. Do not assume that
   *             this is properly formatted JSON.
   * @param type The type to de-serialize to. Note that this could be any
   *             type, including {@link Class}
   *             or {@link java.lang.reflect.ParameterizedType}.
   * @param <T>  The type this method is expected to return.
   * @return The de-serialized object.
   */
  <T> T read(String json, Type type);

  /**
   * De-serialize the given input stream to the given type. If possible
   * implementations can avoid reading the input stream into a String
   * before beginning de-serialization, making this more efficient in
   * cases where an input stream is given that is expected to contain
   * JSON data.
   *
   * @param inputStream The input stream to read JSON from. It's expected to
   *                    be read until there are no more bytes available.
   * @param type        The type to de-serialize to. Note that this could be any
   *                    type, including {@link Class}
   *                    or {@link java.lang.reflect.ParameterizedType}.
   * @param <T>  The type this method is expected to return.
   * @return The de-serialized object.
   */
  <T> T read(InputStream inputStream, Type type);
}
