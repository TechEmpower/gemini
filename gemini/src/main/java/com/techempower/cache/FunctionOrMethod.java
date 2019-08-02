package com.techempower.cache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This interface serves to mimic {@link Method#invoke(Object, Object...)}
 * with just one parameter, the object on which it was called. This is why the
 * method is named {@link #invoke(Object)} instead of extending
 * {@link java.util.function.Function Function} and using
 * {@link java.util.function.Function#apply(Object) Function.apply(Object)}.
 * Additionally, the {@link #invoke(Object)} method explicitly throws
 * {@link IllegalAccessException} and {@link InvocationTargetException} to
 * more closely match the method signature of
 * {@link Method#invoke(Object, Object...)}. All of this serves to reduce the
 * number of code changes necessary to transition from using {@link Method}
 * to {@link FunctionOrMethod}.
 *
 * @author ajohnston
 * @see com.techempower.cache.CacheController CacheController
 * @see com.techempower.cache.MethodValueCache MethodValueCache
 */
@FunctionalInterface
public interface FunctionOrMethod<T, R>
{
  R invoke(T t) throws IllegalAccessException, InvocationTargetException;
}
