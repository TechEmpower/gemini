package com.techempower.gemini.path.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.techempower.gemini.path.StringRequestBodyAdapter;

/**
 * <p>A convenience annotation that is equivalent to
 * {@code @Body(StringRequestBodyAdapter.class)}.</p>
 *
 * <p>Indicates that the request body should be read into a String
 * and passed as the last parameter to the handler method. The last
 * parameter must be a String.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Body(StringRequestBodyAdapter.class)
public @interface ConsumesString
{
}
