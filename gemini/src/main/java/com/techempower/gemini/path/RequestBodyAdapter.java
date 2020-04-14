package com.techempower.gemini.path;

import java.lang.reflect.Type;

import com.techempower.gemini.BasicContext;

/**
 * <p>An interface whose implementations are responsible for adapting
 * or transforming a request's body to a format for handlers to use.</p>
 *
 * <p>You may implement this interface and specify that your
 * implementation should be used for adapting requests using the
 * {@link com.techempower.gemini.path.annotation.Body} annotation.
 * Your implementation must have a public no-arguments constructor.
 * It will be instantiated once and used for the life of the handler
 * method. Instances are not shared between handler methods, meaning
 * there is an instance for each method annotated with @Body.</p>
 *
 * @param <C> Your application's Context type.
 * @see com.techempower.gemini.path.annotation.Body
 */
public interface RequestBodyAdapter<C extends BasicContext>
{
  /**
   * Transform a given request body and return the result.
   *
   * @param context The context of the request. Provides access to the
   *                request and corresponding input stream for the body.
   * @param type The type expected to be returned by this method. This
   *             corresponds to the declared type of the last argument
   *             of any handler method using this adapter.
   * @return The adapted value that was generated from the request's
   * body. The result is passed to the handler method as the last argument.
   * @throws RequestBodyException If an unexpected error occurs that cannot
   * be handled by this method.
   */
  Object read(C context, Type type) throws RequestBodyException;
}
