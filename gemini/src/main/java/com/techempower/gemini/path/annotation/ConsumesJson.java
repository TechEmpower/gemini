package com.techempower.gemini.path.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.techempower.gemini.path.JsonRequestBodyAdapter;

/**
 * <p>A convenience annotation that is equivalent to
 * {@code @Body(JsonRequestBodyAdapter.class)}.</p>
 *
 * <p>Indicates that the request body should be de-serialized from
 * JSON and passed as the last parameter to the handler method.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Body(JsonRequestBodyAdapter.class)
public @interface ConsumesJson
{
}
