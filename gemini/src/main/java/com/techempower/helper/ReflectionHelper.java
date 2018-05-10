/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.techempower.helper;

import java.lang.reflect.*;
import java.util.*;

/**
 * ReflectionHelper provides utility functions for working with Reflection.
 */
public final class ReflectionHelper
{
  
  //
  // Static variables.
  //
  
  public static final Object[]   NO_VALUES     = new Object[0];
  public static final Class<?>[] NO_PARAMETERS = new Class[0];

  //
  // Static methods.
  //

  /**
   * Copies the values returned by the given get method when called on the 
   * elements in source to a new array of the same length as source. In the 
   * case of an error a zero length array will be returned in its place.
   *
   * @param source The array to copy values from.
   * @param getMethodName The method to call in order to retrieve the values.
   * @return A new array containing the results of calling the given method on
   *         each element in source, or a zero length array in the case of an
   *         error.
   */
  public static Object[] copy(Object[] source, String getMethodName)
  {
    final Object[] result = new Object[source.length];
    Class<? extends Object> previousClass = null;
    Method method = null;

    for(int i = 0; i < source.length; i++)
    {
      final Object sourceObject = source[i];

      if (sourceObject != null)
      {
        Class<? extends Object> objClass = sourceObject.getClass();
        if (objClass != previousClass)
        {
          previousClass = objClass;

          method = findMethod(objClass, getMethodName);

          if (method == null)
          {
            return new Object[0];
          }
        }

        try
        {
          if (method != null)
          {
            result[i] = method.invoke(sourceObject, (Object[])null);
          }
          else
          {
            return new Object[0];
          }
        }
        catch (IllegalAccessException 
            | IllegalArgumentException 
            | InvocationTargetException iaexc)
        {
          return new Object[0];
        }
      }
    }

    return result;
  }

  /**
   * Copies the values returned by the given get method when called on the 
   * elements in source to a new Collection. In the case of an empty 
   * Collection will be returned in its place.
   *
   * @param source The Collection to copy values from.
   * @param getMethodName The method to call in order to retrieve the values.
   * @return A new Collection containing the results of calling the given 
   *         method on each element in source, or an empty Collection in the 
   *         case of an error.
   */
  public static Collection<? extends Object> 
    copy(Collection<? extends Object> source, String getMethodName)
  {
    final Collection<Object> result = new ArrayList<>(source.size());
    Class<? extends Object> previousClass = null;
    Method method = null;

    for (Object sourceObject : source)
    {
      Class<? extends Object> objClass = sourceObject.getClass();
      if (objClass != previousClass)
      {
        previousClass = objClass;

        method = findMethod(objClass, getMethodName);

        if (method == null)
        {
          return new ArrayList<>(0);
        }
      }

      try
      {
        if (method != null)
        {
          result.add(method.invoke(sourceObject, (Object[])null));
        }
        else
        {
          return new ArrayList<>(0);
        }
      }
      catch (IllegalAccessException 
          | IllegalArgumentException 
          | InvocationTargetException iaexc)
      {
        return Collections.emptyList();
      }
    }

    return result;
  }

  /**
   * Creates a Map of the elements in source keyed by the values returned by 
   * the given get method when called on each of those elements. Elements that
   * cannot be added due to an error will be skipped.
   *
   * @param source The Collection to copy values from.
   * @param keyMethodName The method to call in order to retrieve the key.
   * @return A new Map containing the elements in source, keyed by the results
   *         of calling the given method on each element in source.
   */
  public static <V> Map<Object,V> mappify(Collection<V> source, 
      String keyMethodName)
  {
    final Map<Object,V> result = new HashMap<>(source.size());
    mappify(source, result, keyMethodName);
    return result;
  }

  /**
   * Populates a Map with the elements in source keyed by the values returned
   * by the given get method when called on each of those elements. Elements 
   * that cannot be added due to an error will be skipped.
   *
   * @param source The Collection to copy values from.
   * @param destination The Map to copy values to.
   * @param keyMethodName The method to call in order to retrieve the key.
   */
  public static <V> void mappify(Collection<V> source, 
      Map<Object,V> destination, String keyMethodName)
  {
    Class<?> previousClass = null;
    Method method;

    for (V sourceObject : source)
    {
      final Class<?> objClass = sourceObject.getClass();
      if (objClass != previousClass)
      {
        previousClass = objClass;

        method = findMethod(objClass, keyMethodName);

        if (method != null)
        {
          try
          {
            destination.put(method.invoke(sourceObject, (Object[])null),
                sourceObject);
          }
          catch (IllegalAccessException | InvocationTargetException e)
          {
            // skip it
          }
        }
      }
    }

    // mappification is complete
  }

  /**
   * Finds a Method with the given name and no parameters for the given Class.
   * Will search up the class hierarchy until a Method is found. If the method
   * cannot be found null will be returned.
   * @param objClass Class in which to find the method
   * @param methodName name of method to find
   * @return Method or null if no such method could be found
   */
  public static Method findMethod(Class<? extends Object> objClass, 
      String methodName)
  {
    return findMethod(objClass, methodName, new Class<?>[]{});
  }

  /**
   * Finds a Method with the given name and parameter types for the given 
   * Class. Will search up the class hierarchy until a Method is found. If 
   * the method cannot be found null will be returned.
   * 
   * @param objClass Class in which to find the method
   * @param methodName name of method to find
   * @param parameterTypes types of parameters the method being searched for 
   *        should have
   * @return Method or null if no such method could be found
   */
  public static Method findMethod(Class<? extends Object> objClass, 
      String methodName, Class<? extends Object>[] parameterTypes)
  {
    if (StringHelper.isEmptyTrimmed(methodName))
    {
      return null;
    }

    String name = methodName.trim();

    Method method = null;
    Class<?> localClass = objClass;
    while (method == null && localClass != null)
    {
      try
      {
        method = localClass.getDeclaredMethod(name, parameterTypes); 
      } 
      catch (NoSuchMethodException e)
      {
        // Does nothing.
      }
      localClass = localClass.getSuperclass();
    }

    return method;
  }

  /**
   * Finds all Methods with the given name for the given Class.
   * Will search up the class hierarchy.
   * 
   * @param objClass Class in which to find the method
   * @param methodName name of method to find
   * @param ignoreCase ignore case, include method names that are the same as
   *        this parameter but in different case
   * @return Method or null if no such method could be found
   */
  public static Collection<Method> findMethodsByName(Class<? extends Object> objClass, String methodName, boolean ignoreCase)
  {
    if (StringHelper.isEmptyTrimmed(methodName))
    {
      return new ArrayList<>(0);
    }

    Collection<Method> result = new ArrayList<>();
    Class<?> work = objClass;
    while (work != null)
    {
      Method[] methods = work.getDeclaredMethods();

      for (Method method : methods)
      {
        if ((method.getName().equals(methodName)
            || (ignoreCase && method.getName().equalsIgnoreCase(methodName)))
            )
        {
          result.add(method);
        }
      }
      work = work.getSuperclass();
    }

    return result;
  }

  /**
   * Finds a Method with the given name and no parameters for the given Object's Class.
   * Will search up the class hierarchy until a Method is found. If the method
   * cannot be found null will be returned.
   * 
   * @param obj Object of Class to be searched
   * @param methodName name of method to search for
   * @return The method
   */
  public static Method findMethod(Object obj, String methodName)
  {
    return findMethod(obj.getClass(), methodName);
  }

  /**
   * Finds a Method with the given name and parameter types for the given Object's Class.
   * Will search up the class hierarchy until a Method is found. If the method
   * cannot be found null will be returned.
   * 
   * @param obj Object of Class to be searched
   * @param methodName name of method to find
   * @param parameterTypes types of parameters the method being searched for should have
   * @return Method or null if no such method could be found
   */
  public static Method findMethod(Object obj, String methodName, Class<? extends Object>[] parameterTypes)
  {
    return findMethod(obj.getClass(), methodName, parameterTypes);
  }

  /**
   * Invokes a Method with the given name and no parameters on the given Object.
   * Will search up the class hierarchy until a Method is found. If the method
   * cannot be found or called null will be returned.
   * 
   * @param obj Object on which to invoke the method
   * @param methodName name of method to invoke
   * @return return value
   */
  public static Object invokeIgnoringErrors(Object obj, String methodName)
  {
    return invokeIgnoringErrors(obj, methodName, new Object[] {});
  }

  /**
   * Invokes a Method with the given name using the given parameters on the given Object.
   * Will search up the class hierarchy until a Method is found. If the method
   * cannot be found or called null will be returned.
   * 
   * @param obj Object on which to invoke the method
   * @param methodName name of method to invoke
   * @param parameters parameters to pass to method
   * @return return value
   */
  public static Object invokeIgnoringErrors(Object obj, String methodName, Object[] parameters)
  {
    try
    {
      return invoke(obj, methodName, parameters);
    }
    catch (NoSuchMethodException e)
    {
      return null;
    }
    catch (InvocationTargetException e)
    {
      return null;
    }
    catch (IllegalAccessException e)
    {
      return null;
    }
  }

  /**
   * Invokes a Method with the given name and no parameters on the given Object.
   * Will search up the class hierarchy until a Method is found. If the method
   * cannot be found or called an Exception will be thrown.
   * 
   * @param obj  Object on which to invoke the method
   * @param methodName name of method to invoke
   * @return return value
   * @throws NoSuchMethodException there is no method of that name on that object
   * @throws InvocationTargetException there was an exception thrown by the method
   * @throws IllegalAccessException object privacy violation
   */
  public static Object invoke(Object obj, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
  {
    return invoke(obj, methodName, new Object[] {});
  }

  /**
   * Invokes a Method with the given name using the given parameters on the given Object.
   * Will search up the class hierarchy until a Method is found. If the method
   * cannot be found or called an Exception will be thrown.
   * @param obj Object on which to invoke the method
   * @param methodName name of method to invoke
   * @param parameters arguments for the method
   * @return return value
   * @throws NoSuchMethodException there is no method of that name on that object
   * @throws InvocationTargetException there was an exception thrown by the method
   * @throws IllegalAccessException object privacy violation
   */
  public static Object invoke(Object obj, String methodName, Object[] parameters)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
  {
    Class<? extends Object>[] parameterTypes = (Class<?>[])copy(parameters, "getClass");
    Method method = findMethod(obj, methodName, parameterTypes);

    if (method == null)
    {
      throw new NoSuchMethodException("No method named '" + methodName
          + "' with parameter types '" + StringHelper.join(", ", parameterTypes)
          + "' could be found in class '" + obj.getClass() + "'");
    }

    return method.invoke(obj, parameters);
  }

  /**
   * You may not instantiate this class.
   */
  private ReflectionHelper()
  {
    // Does nothing.
  }

}  // End ReflectionHelper.

