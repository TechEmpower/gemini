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

import java.util.*;

/**
 * CollectionHelper provides utility functions for working with the Java
 * Collections API.  Most of the methods here were formerly in BasicHelper.
 */
public final class CollectionHelper
{
  
  //
  // Static methods.
  //

  /**
   * Convenience method to return the first object in an ordered collection,
   * or null if the collection is empty.
   */
  public static <E> E getFirst(Iterable<? extends E> objects)
  {
    if (objects == null)
    {
      return null;
    }
    Iterator<? extends E> iterator = objects.iterator();
    if (!iterator.hasNext())
    {
      return null;
    }
    return iterator.next();
  }

  /**
   * Convenience method to return the last object in an ordered collection,
   * or null if the collection is empty.
   */
  public static <E> E getLast(Iterable<? extends E> objects)
  {
    if (objects == null)
    {
      return null;
    }
    if (objects instanceof List)
    {
      List<? extends E> list = (List<? extends E>)objects;
      if (list.isEmpty())
      {
        return null;
      }
      return list.get(list.size() - 1);
    }
    E last = null;
    for (E element : objects)
    {
      last = element;
    }
    return last;
  }

  /**
   * Returns a random element from the list.  Returns {@code null} if the list
   * is null or empty.
   */
  public static <E> E getRandom(List<? extends E> list)
  {
    if (isEmpty(list))
    {
      return null;
    }
    return list.get((int)(Math.random() * list.size()));
  }

  /**
   * Convenience method to return the first <code>limit</code> elements in a
   * List. If <code>objects</code> is null, empty, or smaller than 
   * <code>limit</code> it will return <code>objects</code> unchanged.
   */
  public static <E> List<E> limit(List<E> objects, int limit)
  {
    if (isEmpty(objects) || objects.size() < limit)
    {
      return objects;
    }

    return objects.subList(0, limit);
  }

  /**
   * Determines if a Collection parameter is either 1) null, or 2) empty.
   */
  public static <E> boolean isNonEmpty(Collection<E> inputCollection)
  {
    return inputCollection != null && inputCollection.size() > 0;
  }

  /**
   * Determines if a Collection parameter is either 1) null, or 2) empty.
   */
  public static <E> boolean isEmpty(Collection<E> inputCollection)
  {
    return inputCollection == null || inputCollection.size() == 0;
  }

  /**
   * Determines if a Map parameter is 1) not null, and 2) not empty.
   *
   * @param inputMap the Map to check
   */
  public static <K,V> boolean isNonEmpty(Map<K,V> inputMap)
  {
    return inputMap != null && inputMap.size() > 0;
  }

  /**
   * Determines if a Map parameter is either 1) null, or 2) empty.
   */
  public static <K,V> boolean isEmpty(Map<K,V> inputMap)
  {
    return inputMap == null || inputMap.size() == 0;
  }

  /**
   * Determines if an Object array parameter is 1) not null, and 2) not empty.
   *
   * @param inputArray the Object array to check
   */
  public static <E> boolean isNonEmpty(E[] inputArray)
  {
    return inputArray != null && inputArray.length > 0;
  }

  /**
   * Determines if an Object array parameter is either 1) null, or 2) empty.
   */
  public static <E> boolean isEmpty(E[] inputArray)
  {
    return inputArray == null || inputArray.length == 0;
  }

  /**
   * Returns true if the provided value is contained within the provided
   * array.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static <E> boolean arrayContains(E[] array, E valueToFind)
  {
    return (arrayIndexOf(array, valueToFind) >= 0);
  }

  /**
   * Returns the index of a provided parameter within the provided array.
   * Returns -1 if the item is not found.  This method assumes that the
   * array is <b>not</b> sorted.  If the array is sorted, see 
   * Arrays.binarySearch instead.
   *   <p>
   * This method uses an == operator rather than an .equals call to determine
   * if the provided reference is contained, not just an equivalent object.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static <E> int arrayIndexOf(E[] array, E valueToFind)
  {
    if (array != null)
    {
      for (int i = 0; i < array.length; i++)
      {
        if (array[i] == valueToFind)
        {
          return i;
        }
      }
    }

    return -1;
  }

  /**
   * Returns true if the provided value is contained within the provided
   * array.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static boolean arrayContains(int[] array, int valueToFind)
  {
    return (arrayIndexOf(array, valueToFind) >= 0);
  }

  /**
   * Returns the index of a provided parameter within the provided array.
   * Returns -1 if the item is not found.  This method assumes that the
   * array is <b>not</b> sorted.  If the array is sorted, see 
   * Arrays.binarySearch instead.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static int arrayIndexOf(int[] array, int valueToFind)
  {
    if (array != null)
    {
      for (int i = 0; i < array.length; i++)
      {
        if (array[i] == valueToFind)
        {
          return i;
        }
      }
    }

    return -1;
  }

  /**
   * Returns true if the provided value is contained within the provided
   * array.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static boolean arrayContains(long[] array, long valueToFind)
  {
    return (arrayIndexOf(array, valueToFind) >= 0);
  }

  /**
   * Returns the index of a provided parameter within the provided array.
   * Returns -1 if the item is not found.  This method assumes that the
   * array is <b>not</b> sorted.  If the array is sorted, see 
   * Arrays.binarySearch instead.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static int arrayIndexOf(long[] array, long valueToFind)
  {
    if (array != null)
    {
      for (int i = 0; i < array.length; i++)
      {
        if (array[i] == valueToFind)
        {
          return i;
        }
      }
    }

    return -1;
  }

  /**
   * Returns true if the provided value is contained within the provided
   * array.  Note that this method works on unsorted arrays.  If the array
   * is sorted, you should use Arrays.binarySearch instead.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static boolean arrayContains(char[] array, char valueToFind)
  {
    return (arrayIndexOf(array, valueToFind) >= 0);
  }

  /**
   * Returns the index of a provided parameter within the provided array.
   * Returns -1 if the item is not found.  This method assumes that the
   * array is <b>not</b> sorted.  If the array is sorted, see 
   * Arrays.binarySearch instead.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static int arrayIndexOf(char[] array, char valueToFind)
  {
    if (array != null)
    {
      for (int i = 0; i < array.length; i++)
      {
        if (array[i] == valueToFind)
        {
          return i;
        }
      }
    }

    return -1;
  }

  /**
   * Returns true if the provided value is contained within the provided
   * array.  Note that this method works on unsorted arrays.  If the array
   * is sorted, you should use Arrays.binarySearch instead.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static boolean arrayContains(String[] array, String valueToFind)
  {
    return (arrayIndexOf(array, valueToFind) >= 0);
  }

  /**
   * Returns the index of a provided parameter within the provided array.
   * Returns -1 if the item is not found.  This method assumes that the
   * array is -not- sorted.  If the array is sorted, see Arrays.binarySearch
   * instead.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static int arrayIndexOf(String[] array, String valueToFind)
  {
    if (array != null)
    {
      for (int i = 0; i < array.length; i++)
      {
        if (  (  (array[i] == null)
              && (valueToFind == null)
              )
           || ( array[i].equals(valueToFind) )
           )
        {
          return i;
        }
      }
    }

    // Default return value.
    return -1;
  }

  /**
   * Returns the index of a provided parameter within the provided array.
   * Returns -1 if the item is not found.  This method assumes that the
   * array is -not- sorted.  If the array is sorted, see Arrays.binarySearch
   * instead.
   *
   * @param array The array to search in.
   * @param valueToFind The value to search for.
   */
  public static int arrayIndexOfIgnoreCase(String[] array, String valueToFind)
  {
    if (array != null)
    {
      for (int i = 0; i < array.length; i++)
      {
        if (  (  (array[i] == null)
              && (valueToFind == null)
              )
           || ( array[i].equalsIgnoreCase(valueToFind) )
           )
        {
          return i;
        }
      }
    }

    // Default return value.
    return -1;
  }

  /**
   * Converts the given array to a collection then to a delimiter-separated
   * String.
   */
  public static String toString(Object[] items, String delim)
  {
    return toString(toList(items), delim);
  }

  /**
   * Converts the given collection to a delimiter-separated String by calling
   * toString on each item in the collection.
   */
  public static String toString(Collection<? extends Object> collection, String delim)
  {
    return toString(collection, delim, "");
  }

  /**
   * Converts the given collection to a delimiter-separated String by calling
   * toString on each item in the collection.
   * 
   * @param collection The collection to use as input.
   * @param delim the delimiter.
   * @param prefix an optional prefix string to append to each value.
   */
  public static String toString(Collection<? extends Object> collection, String delim,
      String prefix)
  {
    if (isNonEmpty(collection))
    {
      final StringBuilder buffer = new StringBuilder();

      for (Object entry : collection)
      {
        if (buffer.length() > 0)
        {
          buffer.append(delim);
        }

        if (StringHelper.isNonEmpty(prefix))
        {
          buffer.append(prefix);
        }

        buffer.append(entry.toString());
      }

      return buffer.toString();
    }

    return "";
  }

  /**
   * Convert an interleaved array of objects into a Map.  Every even index
   * (starting at 0) will be a key followed by a value in the next index.
   * E.g., ['a', 'b', 'c', 'd'] yields {'a' => 'b', 'c' => 'd'}.
   *   <p>
   * This version assumes a traditional map without linking (order-
   * preservation) is desired.
   * 
   * @param sourceArray an interleaved array of objects pairs to act as keys 
   *        and values.
   */
  @SafeVarargs
  public static <O extends Object> Map<O,O> toMapFromInterleaved(O... sourceArray)
  {
    return toMapFromInterleavedImplementation(false, sourceArray);
  }
  
  /**
   * Convert an interleaved array of objects into a Map.  Every even index
   * (starting at 0) will be a key followed by a value in the next index.
   * E.g., ['a', 'b', 'c', 'd'] yields {'a' => 'b', 'c' => 'd'}.
   *   <p>
   * This version assumes a linked (order-preserving) map is desired so that
   * the resulting map is the same as the order of sourceArray.
   * 
   * @param sourceArray an interleaved array of objects pairs to act as keys 
   *        and values.
   */
  @SafeVarargs
  public static <O extends Object> Map<O,O> toOrderedMapFromInterleaved(O... sourceArray)
  {
    return toMapFromInterleavedImplementation(true, sourceArray);
  }

  /**
   * Convert an interleaved array of objects into a Map.  Every even index
   * (starting at 0) will be a key followed by a value in the next index.
   * E.g., ['a', 'b', 'c', 'd'] yields {'a' => 'b', 'c' => 'd'}.
   * 
   * @param preserveOrder Should a linked map be used so that iteration order
   *        of the resulting map is the same as the order of sourceArray?
   * @param sourceArray an interleaved array of objects pairs to act as keys 
   *        and values.
   */
  @SafeVarargs
  private static <O extends Object> Map<O,O> toMapFromInterleavedImplementation(boolean preserveOrder,
      O... sourceArray)
  {
    if (sourceArray == null)
    {
      return null;
    }
    
    if (sourceArray.length % 2 == 1)
    {
      throw new IllegalArgumentException("sourceArray must contain an even number of values.");
    }
    
    // Create a LinkedHashMap if we want to preserve order.  Otherwise, a
    // plain HashMap will suffice.
    Map<O,O> toReturn = preserveOrder 
        ? new LinkedHashMap<>(sourceArray.length / 2)
        : new HashMap<>(sourceArray.length / 2);
        
    for (int i = 0; i < sourceArray.length; i += 2)
    {
      toReturn.put(sourceArray[i], sourceArray[i + 1]);
    }
    
    return toReturn;
  }
  
  /**
   * Convert the given Object array to a Vector.
   */
  public static <E> Vector<E> toVector(E[] objArray)
  {
    Vector<E> toReturn = new Vector<>(objArray != null ? objArray.length : 0);

    for (int i = 0; objArray != null && i < objArray.length; i ++)
    {
      toReturn.add(objArray[i]);
    }

    return toReturn;
  }

  /**
   * Creates a list from an array of objects.
   */
  @SafeVarargs
  public static <E> List<E> toList(E... objects)
  {
    final List<E> list = new ArrayList<>(objects.length);
    Collections.addAll(list, objects);
    return list;
  }

  /**
   * Creates a list from an iterable of objects.
   */
  public static <E> List<E> toList(Iterable<? extends E> objects)
  {
    if (objects instanceof Collection)
    {
      return new ArrayList<>((Collection<? extends E>)objects);
    }
    return toList(objects.iterator());
  }

  /**
   * Creates a list from an iterator of objects.
   */
  public static <E> List<E> toList(Iterator<? extends E> objects)
  {
    List<E> list = new ArrayList<>();
    while (objects.hasNext())
    {
      list.add(objects.next());
    }
    return list;
  }

  /**
   * Creates a set from an array of objects.
   */
  @SafeVarargs
  public static <E> Set<E> toSet(E... objects)
  {
    Set<E> set = new HashSet<>(objects.length);
    Collections.addAll(set, objects);
    return set;
  }

  /**
   * Creates a set from an iterable of objects.
   */
  public static <E> Set<E> toSet(Iterable<? extends E> objects)
  {
    if (objects instanceof Collection)
    {
      return new HashSet<>((Collection<? extends E>)objects);
    }
    return toSet(objects.iterator());
  }

  /**
   * Creates a set from an iterator of objects.
   */
  public static <E> Set<E> toSet(Iterator<? extends E> objects)
  {
    Set<E> set = new HashSet<>();
    while (objects.hasNext())
    {
      set.add(objects.next());
    }
    return set;
  }

  /**
   * Turns an int[] into a List of Integers.
   */
  public static List<Integer> toList(int[] array)
  {
    if (array == null)
    {
      return new ArrayList<>(0);
    }

    List<Integer> list = new ArrayList<>(array.length);

    for (int entry : array)
    {
      list.add(entry);
    }

    return list;
  }

  /**
   * Turns an long[] into a List of Longs.
   */
  public static List<Long> toList(long[] array)
  {
    if (array == null)
    {
      return new ArrayList<>(0);
    }

    List<Long> list = new ArrayList<>(array.length);

    for (long entry : array)
    {
      list.add(entry);
    }

    return list;
  }

  /**
   * Converts a String[] into an int[].  Values that cannot be parsed will be
   * inserted into the array as zeros.  E.g., ["1", "x"] will become int 
   * array [1, 0].
   */
  public static int[] toIntArray(String[] strArray)
  {
    int[] toReturn = new int[strArray.length];
    for (int i = 0; i < toReturn.length; i++)
    {
      toReturn[i] = NumberHelper.parseInt(strArray[i]);
    }
    return toReturn;
  }

  /**
   * Converts a String[] into an long[].  Values that cannot be parsed will be
   * inserted into the array as zeros.  E.g., ["1", "x"] will become int 
   * array [1, 0].
   */
  public static long[] toLongArray(String[] strArray)
  {
    long[] toReturn = new long[strArray.length];
    for (int i = 0; i < toReturn.length; i++)
    {
      toReturn[i] = NumberHelper.parseLong(strArray[i]);
    }
    return toReturn;
  }
  
  /**
   * Converts a delimited list of numbers in a String into an int array.
   * For example "1,2,3,4,5" = int array [1, 2, 3, 4, 5].  Tokens that cannot
   * be parsed will be inserted into the array as zeros.  E.g., "1,2,x,4,5"
   * will become int array [1, 2, 0, 4, 5].
   *   <p>
   * Returns an empty int[] if either delimitedString or delimiter are null.
   * 
   * @param delimitedString A string composed of numbers separated by the 
   *        delimiter.
   * @param delimiter The character or characters separating each number.
   */
  public static int[] toIntArray(String delimitedString, String delimiter)
  {
    if (  (delimitedString == null)
       || (delimiter == null)
       )
    {
      return new int[0];
    }
    
    StringTokenizer tokenizer = new StringTokenizer(delimitedString, delimiter);
    int[] toReturn = new int[tokenizer.countTokens()];
    int position = 0;
    while (tokenizer.hasMoreTokens())
    {
      toReturn[position++] = NumberHelper.parseInt(tokenizer.nextToken());
    }

    return toReturn;
  }
  
  /**
   * Converts a delimited list of numbers in a String into a long array.
   * For example "1,2,3,4,5" = long array [1, 2, 3, 4, 5].  Tokens that cannot
   * be parsed will be inserted into the array as zeros.  E.g., "1,2,x,4,5"
   * will become int array [1, 2, 0, 4, 5].
   *   <p>
   * Returns an empty long[] if either delimitedString or delimiter are null.
   * 
   * @param delimitedString A string composed of numbers separated by the 
   *        delimiter.
   * @param delimiter The character or characters separating each number.
   */
  public static long[] toLongArray(String delimitedString, String delimiter)
  {
    if (  (delimitedString == null)
       || (delimiter == null)
       )
    {
      return new long[0];
    }
    
    StringTokenizer tokenizer = new StringTokenizer(delimitedString, delimiter);
    long[] toReturn = new long[tokenizer.countTokens()];
    int position = 0;
    while (tokenizer.hasMoreTokens())
    {
      toReturn[position++] = NumberHelper.parseLong(tokenizer.nextToken());
    }

    return toReturn;
  }
  
  /**
   * Converts a Collection of Integer objects to a simple int[].  If the
   * objects are not, in fact, Integers, an exception will be thrown.
   */
  public static int[] toIntArray(Collection<Integer> collectionOfIntegers)
  {
    int[] toReturn = new int[collectionOfIntegers.size()];
    int position = 0;
    for (Integer i : collectionOfIntegers)
    {
      toReturn[position++] = i;
    }

    return toReturn;
  }

  /**
   * Converts a Collection of Long objects to a simple long[].  If the
   * objects are not, in fact, Longs, an exception will be thrown.
   */
  public static long[] toLongArray(Collection<Long> collectionOfLongs)
  {
    long[] toReturn = new long[collectionOfLongs.size()];
    int position = 0;
    for (Long i : collectionOfLongs)
    {
      toReturn[position++] = i;
    }

    return toReturn;
  }

  /**
   * Converts a Collection of String objects to a simple int[] by parsing each
   * String using NumberHelper.parseInt.
   */
  public static int[] toIntArrayFromStrings(Collection<String> collectionOfStrings)
  {
    int[] toReturn = new int[collectionOfStrings.size()];
    int position = 0;
    for (String s : collectionOfStrings)
    {
      toReturn[position++] = NumberHelper.parseInt(s);
    }

    return toReturn;
  }

  /**
   * Converts a Collection of String objects to a simple long[] by parsing each
   * String using NumberHelper.parseLong.
   */
  public static long[] toLongArrayFromStrings(Collection<String> collectionOfStrings)
  {
    long[] toReturn = new long[collectionOfStrings.size()];
    int position = 0;
    for (String s : collectionOfStrings)
    {
      toReturn[position++] = NumberHelper.parseLong(s);
    }

    return toReturn;
  }

  /**
   * Converts a Collection of Strings to a String array.
   */
  public static String[] toStringArray(Collection<String> collectionOfStrings)
  {
    return collectionOfStrings.toArray(new String[collectionOfStrings.size()]);
  }

  /**
   * Converts a long[] to a String[].  Does not catch any exceptions.
   */
  public static String[] toStringArray(long[] intArray)
  {
    String[] toReturn = new String[intArray.length];
    for (int i = 0; i < toReturn.length; i++)
    {
      toReturn[i] = "" + intArray[i];
    }
    return toReturn;
  }

  /**
   * Converts an int[] to a String[].  Does not catch any exceptions.
   */
  public static String[] toStringArray(int[] intArray)
  {
    String[] toReturn = new String[intArray.length];
    for (int i = 0; i < toReturn.length; i++)
    {
      toReturn[i] = "" + intArray[i];
    }
    return toReturn;
  }
  
  /**
   * Converts an byte[] to a String[] using hexadecimal.  Does not catch 
   * any exceptions.
   */
  public static String[] toStringArray(byte[] byteArray)
  {
    String[] toReturn = new String[byteArray.length];
    for (int i = 0; i < toReturn.length; i++)
    {
      toReturn[i] = Integer.toString(byteArray[i] < 0 ? byteArray[i] + 256 : byteArray[i], 16);
    }
    return toReturn;
  }
  
  /**
   * Renders an int array as a comma-delimited String for debug purposes.
   */
  public static String toString(int[] intArray)
  {
    String[] strArray = toStringArray(intArray);
    return toString(strArray, ",");
  }
  
  /**
   * Renders an byte array as a comma-delimited String for debug purposes.
   */
  public static String toString(byte[] intArray)
  {
    String[] strArray = toStringArray(intArray);
    return toString(strArray, ",");
  }
  
  /**
   * Merges and optionally sorts multiple String arrays.
   */
  public static String[] arrayMerge(boolean sort, String[]... sourceArrays)
  {
    // Determine the total size of the new array.
    int totalSize = 0;
    if (sourceArrays != null)
    {
      for (String[] sourceArray : sourceArrays)
      {
        if (sourceArray != null)
        {
          totalSize += sourceArray.length;
        }
      }
    }

    // Create the new array.
    String[] toReturn = new String[totalSize];

    // Populate the new array.
    int position = 0;
    if (sourceArrays != null)
    {
      for (String[] sourceArray : sourceArrays)
      {
        if (sourceArray != null)
        {
          System.arraycopy(sourceArray, 0, toReturn, position,
              sourceArray.length);
          position += sourceArray.length;
        }
      }
    }

    // Sort if requested.
    if (sort)
    {
      Arrays.sort(toReturn);
    }

    return toReturn;
  }

  /**
   * Merges and optionally sorts multiple character arrays.
   */
  public static char[] arrayMerge(boolean sort, char[]... sourceArrays)
  {
    // Determine the total size of the new array.
    int totalSize = 0;
    if (sourceArrays != null)
    {
      for (char[] sourceArray : sourceArrays)
      {
        if (sourceArray != null)
        {
          totalSize += sourceArray.length;
        }
      }
    }

    // Create the new array.
    char[] toReturn = new char[totalSize];

    // Populate the new array.
    int position = 0;
    if (sourceArrays != null)
    {
      for (char[] sourceArray : sourceArrays)
      {
        if (sourceArray != null)
        {
          System.arraycopy(sourceArray, 0, toReturn, position,
              sourceArray.length);
          position += sourceArray.length;
        }
      }
    }

    // Sort if requested.
    if (sort)
    {
      Arrays.sort(toReturn);
    }

    return toReturn;
  }

  /**
   * Generates a filled array of the provided value (all references to the
   * same object!).
   */
  public static String[] getFilledArray(String value, int size)
  {
    String[] toReturn = new String[size];
    for (int i = 0; i < size; i++)
    {
      toReturn[i] = value;
    }

    return toReturn;
  }

  /**
   * Generates a filled array of the provided value.
   */
  public static boolean[] getFilledArray(boolean value, int size)
  {
    boolean[] toReturn = new boolean[size];
    Arrays.fill(toReturn, value);

    return toReturn;
  }

  /**
   * Generates a filled array of the provided value.
   */
  public static int[] getFilledArray(int value, int size)
  {
    int[] toReturn = new int[size];
    Arrays.fill(toReturn, value);

    return toReturn;
  }
  
  /**
   * Creates a new Map that has been reduced to only the specified attributes,
   * while also allowing the attributes to be renamed.  This is useful in
   * generating a Map for rendering as JavaScript/JSON from a Map that may
   * contain elements that are not desired for rendering.
   *   <p>
   * originalAndNewNames is an interleaved String array of original
   * attribute names followed by their new names.  Any attributes in the
   * input map that are not identified in this array will not be copied.
   *   <p>
   * Example usage: getReducedMap(userAttributes, "userFullname", "name")
   *   <p>
   * This would generate a new Map containing a single element, "name". 
   *   <p>
   * @param originalAndNewNames an interleaved String array of original
   *   attribute names followed by their new names.
   */
  public static <E> Map<String,E> getReducedMap(Map<String,E> input, 
      String... originalAndNewNames)
  {
    if (originalAndNewNames.length % 2 != 0)
    {
      throw new IllegalArgumentException(
          "The original and new names array must be of even length.  Length of parameter: " + originalAndNewNames.length);
    }
    
    int length = originalAndNewNames.length / 2;
    HashMap<String,E> toReturn = new HashMap<>(length);
    for (int i = 0; i < length; i++)
    {
      // Skip null original names.
      if (originalAndNewNames[i * 2] != null)
      {
        E value = input.get(originalAndNewNames[i * 2]);
        toReturn.put(originalAndNewNames[i * 2 + 1], value);
      }
    }
    
    return toReturn;
  }

  /**
   * Returns a new Collection that represents the intersection, i.e., the
   * elements that are the same, between two collections.  Implements the
   * returned collection as an ArrayList.
   */
  public static <E> Collection<E> getIntersection(Collection<E> c1, 
      Collection<E> c2)
  {
    Collection<E> returnCollection = new ArrayList<>();
    if (  (c1 != null)
       && (c2 != null)
       )
    {
      for (E o : c1)
      {
        try
        {
          if (c2.contains(o))
          {
            returnCollection.add(o);
          }
        }
        catch (Exception e)
        {}
      }
    }
    return returnCollection;
  }

  /**
   * Creates a new list containing the given objects in natural order.
   */
  public static <E extends Comparable<? super E>> List<E> getSorted(
      Iterable<? extends E> objects)
  {
    return sort(toList(objects));
  }

  /**
   * Creates a new list containing the given objects in the order defined by
   * the given comparator
   */
  public static <E> List<E> getSorted(Iterable<? extends E> objects,
      Comparator<? super E> comparator)
  {
    return sort(toList(objects), comparator);
  }

  /**
   * Sorts and returns the given list.
   */
  public static <E extends Comparable<? super E>> List<E> sort(List<E> list)
  {
    Collections.sort(list);
    return list;
  }

  /**
   * Sorts and returns the given list using the given comparator.
   */
  public static <E> List<E> sort(List<E> list, Comparator<? super E> comparator)
  {
    Collections.sort(list, comparator);
    return list;
  }

  /**
   * Reverses and returns the given list.
   */
  public static <E> List<E> reverse(List<E> list)
  {
    Collections.reverse(list);
    return list;
  }

  /**
   * Shuffles and returns the given list.
   */
  public static <E> List<E> shuffle(List<E> list)
  {
    Collections.shuffle(list);
    return list;
  }

  /**
   * You may not instantiate this class.
   */
  private CollectionHelper()
  {
    // Does nothing.
  }

}  // End CollectionHelper.

