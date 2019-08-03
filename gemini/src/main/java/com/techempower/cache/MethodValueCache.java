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

package com.techempower.cache;

import com.techempower.helper.StringHelper;
import gnu.trove.iterator.*;
import gnu.trove.map.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.*;
import gnu.trove.set.hash.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.techempower.util.*;

/**
 * Caches the return values of methods for one type of cached object.  Allows
 * you to quickly retrieve objects by any field value.
 * <p>
 * It is expected that an application using this will notify it when a cached
 * object is modified.  EntityStore, if configured to use method value
 * caches, will take care of this assuming every modification of an object in
 * the cache is persisted to the database.
 * <p>
 * The cache is populated using lazy loading on a per method basis.  If the
 * {@code getObjects} method is called on method {@code getFoo}, the cache
 * for only {@code getFoo} gets populated.  If {@code getObjects} is never
 * called on method {@code getBar}, the cache for {@code getBar} is never
 * populated.
 *
 * @param <T> The type of objects whose values are being cached.
 */
public class MethodValueCache<T extends Identifiable>
{
  // Utility objects.
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final Bucket<T> rootBucket = new Bucket<>();
  private final Map<String, Method> mapMethodNameToMethod
      = new HashMap<>();
  private final Map<String, TLongObjectMap<Object>> mapMethodNameToIdToValue
      = new HashMap<>();
  private boolean loaded = false;

  static class Bucket<T extends Identifiable>
  {
    private final Map<String, Map<Object, TLongSet>> mapMethodNameToValueToIds
        = new HashMap<>();
    private final Map<String, Map<Object, Bucket<T>>> mapMethodNameToValueToBucket
        = new HashMap<>();
    private final Bucket<T> parent;
    private final String method;
    private final Object value;

    Bucket()
    {
      this(null, null, null);
    }

    Bucket(Bucket<T> parent, String method, Object value)
    {
      this.parent = parent;
      this.method = method;
      this.value = value;
    }

    boolean initTestEntity(long id, String method, Object value)
    {
      Bucket<T> bucket = this;
      boolean inParent = parent == null
          || parent.initTestEntity(id, method, value);
      if (!inParent)
      {
        return false;
      }
      Map<Object, TLongSet> mapValuesToIds = mapMethodNameToValueToIds
          .get(method);
      if (mapValuesToIds == null)
      {
        return false;
      }
      TLongSet ids = mapValuesToIds.get(value);
      return ids != null && ids.contains(id);
    }
  }

  // Assigned in the constructor.
  private final EntityStore cache;
  private final Class<T> type;

  /**
   * Creates a new method value cache.
   *
   * @param cache The cache that stores the objects.
   * @param type The type of objects whose values are being cached.
   */
  public MethodValueCache(EntityStore cache, Class<T> type)
  {
    this.cache = cache;
    this.type = type;
  }

  /**
   * Removes the values for the given entity from this cache.
   *
   * @param id The id of the entity to be removed.
   */
  public void delete(long id)
  {
    // If the cache hasn't been loaded, there is nothing to delete.
    if (!this.loaded)
    {
      return;
    }

    this.lock.writeLock().lock();
    try
    {
      for (String methodName : this.mapMethodNameToMethod.keySet())
      {
        TLongObjectMap<Object> mapIdToValue = this.mapMethodNameToIdToValue.get(methodName);
        Map<Object, TLongSet> mapValueToIds = this.rootBucket.mapMethodNameToValueToIds.get(methodName);

        Object value = mapIdToValue.get(id);
        getBucketAndChildren(rootBucket)
            .stream()
            .map(bucket -> bucket.mapMethodNameToValueToIds.get(value))
            .filter(Objects::nonNull)
            .forEach(otherMapValueToIds -> {
              TLongSet ids = otherMapValueToIds.get(value);
              ids.remove(id);
              if (ids.isEmpty())
              {
                otherMapValueToIds.remove(value);
              }
            });

        TLongSet ids = mapValueToIds.get(value);
        if (ids != null)
        {
          ids.remove(id);
          if (ids.isEmpty())
          {
            mapValueToIds.remove(value);
          }
        }
        mapIdToValue.remove(id);
      }
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  private List<Bucket<T>> getBucketAndChildren(Bucket<T> bucket)
  {
    return Stream.concat(Stream.of(bucket),
        Stream.of(bucket)
            .map(theBucket -> theBucket.mapMethodNameToValueToBucket.values())
            .flatMap(Collection::stream)
            .map(Map::values)
            .flatMap(Collection::stream)
            .map(this::getBucketAndChildren)
            .flatMap(Collection::stream))
        .collect(Collectors.toList());
  }

  /**
   * Returns the entity who have the given value for the given method.  For
   * example, if methodName is "getName" and value is "Foo", then the following
   * is true about the returned entity:  entity.getName().equals("Foo").  If no
   * entities have that value, {@code null} is returned.  If more than one
   * entity has that value, the first one encountered is returned.
   *
   * @param methodName The name of the method to call.
   * @param value The desired value of the method.
   * @return The first entity that has the given value.
   */
  public T getObject(String methodName, Object value)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      Map<Object, TLongSet> mapValueToIds = this.rootBucket.mapMethodNameToValueToIds.get(methodName);

      if (mapValueToIds != null)
      {
        // If we're here, then this method has been called before.
        TLongSet ids = mapValueToIds.get(value);

        if (ids == null || ids.isEmpty())
        {
          return null;
        }

        long id = ids.iterator().next();
        return this.cache.get(this.type, id);
      }
    }
    finally
    {
      this.lock.readLock().unlock();
    }

    // If we're here, then this is the first time this method has been called.
    this.lock.writeLock().lock();
    try
    {
      addMethod(methodName);
    }
    finally
    {
      this.lock.writeLock().unlock();
    }

    return getObject(methodName, value);
  }

  @SuppressWarnings("unchecked")
  T getObjectInt(FieldIntersection<T> fieldIntersection)
  {
    if (!this.loaded)
    {
      this.load();
    }

    try
    {
      this.lock.readLock()
          .lock();
      IdSearchResult idsSearchResult = getIdsForMethodValues(rootBucket,
          fieldIntersection.getMethodNames(), fieldIntersection.getValues(), 0);

      if (idsSearchResult.initialized)
      {
        // If we're here, then this method has been called before.
        TLongSet ids = idsSearchResult.ids;

        if (ids == null || ids.isEmpty())
        {
          return null;
        }

        long id = ids.iterator().next();
        return this.cache.get(this.type, id);
      }
    }
    finally
    {
      this.lock.readLock()
          .unlock();
    }

    // If we're here, then this is the first time this method has been called.
    try
    {
      this.lock.writeLock()
          .lock();
      this.addIndex(fieldIntersection);
    }
    finally
    {
      this.lock.writeLock()
          .unlock();
    }

    return this.getObjectInt(fieldIntersection);
  }

  /**
   * Returns the entities who have the given value for the given method.  For
   * example, if methodName is "getName" and value is "Foo", then the following
   * is true about the returned entities:  entity.getName().equals("Foo").
   *
   * @param methodName The name of the method to call.
   * @param value The desired value of the method.
   * @return The entities that have the given value.
   */
  public List<T> getObjects(String methodName, Object value)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      Map<Object, TLongSet> mapValueToIds = this.rootBucket.mapMethodNameToValueToIds.get(methodName);

      if (mapValueToIds != null)
      {
        // If we're here, then this method has been called before.
        TLongSet ids = mapValueToIds.get(value);

        if (ids == null || ids.isEmpty())
        {
          return new ArrayList<>(0);
        }

        List<T> values = new ArrayList<>();

        for (TLongIterator iterator = ids.iterator(); iterator.hasNext();)
        {
          long id = iterator.next();
          values.add(this.cache.get(this.type, id));
        }

        return values;
      }
    }
    finally
    {
      this.lock.readLock().unlock();
    }

    // If we're here, then this is the first time this method has been called.
    this.lock.writeLock().lock();
    try
    {
      addMethod(methodName);
    }
    finally
    {
      this.lock.writeLock().unlock();
    }

    return getObjects(methodName, value);
  }

  private static class IdSearchResult
  {
    static final IdSearchResult NOT_INITIALIZED = new IdSearchResult();
    final TLongSet ids;
    final boolean initialized;

    IdSearchResult(TLongSet ids)
    {
      this.ids = ids;
      this.initialized = true;
    }

    IdSearchResult()
    {
      this.ids = null;
      this.initialized = false;
    }
  }

  /**
   * Finds the set of ids for the given method name/value pair. If any
   * method mapping that should be initialized is not found, this returns
   * {@link IdSearchResult.NOT_INITIALIZED}. Otherwise an IdsSearchResult is
   * returned that has the found ids.
   * @param bucket
   * @param methodNames
   * @param values
   * @return
   */
  private IdSearchResult getIdsForMethodValues(Bucket<T> bucket,
                                               List<String> methodNames,
                                               List<Object> values,
                                               int index)
  {
    String currentMethod = methodNames.get(index);
    Object currentValue  = values.get(index);
    boolean moreThanOneLeft = methodNames.size() - index > 1;
    if (moreThanOneLeft)
    {
      Map<Object, Bucket<T>> mapValueToBucket = bucket
          .mapMethodNameToValueToBucket.get(currentMethod);
      if (mapValueToBucket == null)
      {
        return IdSearchResult.NOT_INITIALIZED;
      }
      Bucket<T> nextBucket = mapValueToBucket.get(currentValue);
      if (nextBucket == null)
      {
        return new IdSearchResult(null);
      }
      return getIdsForMethodValues(nextBucket, methodNames, values, index + 1);
    }
    else
    {
      Map<Object, TLongSet> mapValueToIds = bucket.mapMethodNameToValueToIds
          .get(currentMethod);
      if (mapValueToIds == null)
      {
        return IdSearchResult.NOT_INITIALIZED;
      }
      return new IdSearchResult(mapValueToIds.get(currentValue));
    }
  }

  @SuppressWarnings("unchecked")
  List<T> getObjectsInt(FieldIntersection<T> fieldIntersection)
  {
    if (!this.loaded)
    {
      this.load();
    }

    try
    {
      this.lock.readLock()
          .lock();

      IdSearchResult idsSearchResult = getIdsForMethodValues(rootBucket,
          fieldIntersection.getMethodNames(), fieldIntersection.getValues(), 0);

      if (idsSearchResult.initialized)
      {
        // If we're here, then this method has been called before.
        TLongSet ids = idsSearchResult.ids;

        if (ids == null || ids.isEmpty())
        {
          return new ArrayList<>(0);
        }

        List<T> values = new ArrayList<>();

        for (TLongIterator iterator = ids.iterator(); iterator.hasNext();)
        {
          long id = iterator.next();
          values.add(this.cache.get(this.type, id));
        }

        return values;
      }
    }
    finally
    {
      this.lock.readLock()
          .unlock();
    }

    // If we're here, then this is the first time this method has been called.
    try
    {
      this.lock.writeLock()
          .lock();
      this.addIndex(fieldIntersection);
    }
    finally
    {
      this.lock.writeLock()
          .unlock();
    }

    return this.getObjectsInt(fieldIntersection);
  }

  /**
   * Resets this cache so that it will be rebuilt the next time it is used.
   */
  public void reset()
  {
    // If the cache hasn't been loaded, there is nothing to reset.
    if (!this.loaded)
    {
      return;
    }

    this.lock.writeLock().lock();
    try
    {
      this.loaded = false;
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  /**
   * Updates the values for the given entity in this cache.
   *
   * @param id The id of the entity to be updated.
   */
  public void update(long id)
  {
    // If the cache hasn't been loaded, there is nothing to update.
    if (!this.loaded)
    {
      return;
    }

    this.lock.writeLock().lock();
    try
    {
      T object = this.cache.get(this.type, id);

      // Clear out the previous id/value mappings.
      for (String methodName : this.mapMethodNameToMethod.keySet())
      {
        TLongObjectMap<Object> mapIdToValue = this.mapMethodNameToIdToValue.get(methodName);
        Map<Object, TLongSet> mapValueToIds = this.rootBucket.mapMethodNameToValueToIds.get(methodName);

        Object oldValue = mapIdToValue.get(id);
        TLongSet oldIds = mapValueToIds.get(oldValue);
        if (oldIds != null)
        {
          oldIds.remove(id);
          if (oldIds.isEmpty())
          {
            mapValueToIds.remove(oldValue);
          }
        }
        getBucketAndChildren(rootBucket)
            .stream()
            .map(bucket -> bucket.mapMethodNameToValueToIds.get(oldValue))
            .filter(Objects::nonNull)
            .forEach(otherMapValueToIds -> {
              TLongSet ids = otherMapValueToIds.get(oldValue);
              ids.remove(id);
              if (ids.isEmpty())
              {
                otherMapValueToIds.remove(oldValue);
              }
            });
        mapIdToValue.remove(id);

        if (object != null)
        {
          // Add in the new id/value mapping.
          Object newValue = invokeMethod(object, methodName);
          mapIdToValue.put(id, newValue);
          TLongSet ids = mapValueToIds.get(newValue);
          if (ids == null)
          {
            ids = new TLongHashSet();
            mapValueToIds.put(newValue, ids);
          }
          ids.add(id);

          getBucketAndChildren(rootBucket)
              .forEach(bucket -> bucket
                  .initTestEntity(id, methodName, newValue));
        }
      }
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  /**
   * Stores the given method and the values of that method for all entities in
   * this cache.
   *
   * @param methodName The name of the method to be stored.
   */
  protected void addMethod(String methodName)
  {
    try
    {
      Method method = this.type.getMethod(methodName);
      this.mapMethodNameToMethod.put(methodName, method);
      this.rootBucket.mapMethodNameToValueToIds.put(methodName, new HashMap<Object, TLongSet>());
      this.mapMethodNameToIdToValue.put(methodName, new TLongObjectHashMap<>());

      indexMethod(methodName);
    }
    catch (NoSuchMethodException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Stores the values of the given method for all entities in this cache.
   *
   * @param methodName The name of the method to be stored.
   */
  protected void indexMethod(String methodName)
  {
    TLongObjectMap<Object> mapIdToValue = this.mapMethodNameToIdToValue.get(methodName);
    Map<Object, TLongSet> mapValueToIds = this.rootBucket.mapMethodNameToValueToIds.get(methodName);

    if (mapIdToValue == null)
    {
      mapIdToValue = new TLongObjectHashMap<>();
      this.mapMethodNameToIdToValue.put(methodName, mapIdToValue);
    }
    else
    {
      mapIdToValue.clear();
    }

    if (mapValueToIds == null)
    {
      mapValueToIds = new HashMap<>();
      this.rootBucket.mapMethodNameToValueToIds.put(methodName, mapValueToIds);
    }
    else
    {
      mapValueToIds.clear();
    }

    for (T object : this.cache.list(this.type))
    {
      Object value = invokeMethod(object, methodName);
      long id = object.getId();
      mapIdToValue.put(id, value);

      TLongSet ids = mapValueToIds.get(value);
      if (ids == null)
      {
        ids = new TLongHashSet();
        mapValueToIds.put(value, ids);
      }
      ids.add(id);
    }
  }

  /**
   * Invokes the given method of the given object.
   *
   * @param object The object.
   * @param methodName The method to be invoked.
   * @return The return value of the invoked method.
   */
  protected Object invokeMethod(T object, String methodName)
  {
    try
    {
      return this.mapMethodNameToMethod.get(methodName).invoke(object);
    }
    catch (IllegalAccessException e)
    {
      return null;
    }
    catch (InvocationTargetException e)
    {
      return null;
    }
  }

  /**
   * Initializes this cache.  Queries all entities on each method that is
   * currently known by this cache.
   */
  protected void load()
  {
    this.lock.writeLock().lock();
    try
    {
      if (this.loaded)
      {
        return;
      }

      for (String methodName : this.mapMethodNameToMethod.keySet())
      {
        indexMethod(methodName);
      }
      rootBucket.mapMethodNameToValueToBucket.clear();

      this.loaded = true;
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  private void ensureAllMethodsRegistered(
      FieldIntersection<T> fieldIntersection)
  {
    List<String> nonRegisteredMethods = this
        .getNonRegisteredMethods(fieldIntersection);
    if (!nonRegisteredMethods.isEmpty())
    {
      // Not all methods have been registered
      nonRegisteredMethods.forEach(this::addMethod);
      List<String> remainingNonRegisteredMethods = this
          .getNonRegisteredMethods(fieldIntersection);
      if (!remainingNonRegisteredMethods.isEmpty())
      {
        // One or more of the methods was not registered. This can only happen
        // if a method is not defined.
        int numberOfNonRegisteredMethods = remainingNonRegisteredMethods
            .size();
        String wasWere = numberOfNonRegisteredMethods > 1 ? "were" : "was";
        throw new RuntimeException(String.format(
            "The following method%s %s not found: %s",
            StringHelper.pluralize(numberOfNonRegisteredMethods),
            wasWere,
            remainingNonRegisteredMethods.stream()
                .map(methodName -> "\n\"" + methodName + "\"")
                .collect(Collectors.joining(", ")))
            .toString());
      }
    }
  }

  private void indexBucketForMethods(Bucket<T> bucket,
                                     List<String> methodNames,
                                     int index)
  {
    String currentMethod = methodNames.get(index);
    boolean initializationRequired = !bucket
        .mapMethodNameToValueToBucket
        .containsKey(currentMethod);
    Map<Object, Bucket<T>> mapValueToBucket = bucket
        .mapMethodNameToValueToBucket
        .computeIfAbsent(currentMethod, ignored -> new HashMap<>());
    Map<Object, TLongSet> mapValueToIds = this.rootBucket
        .mapMethodNameToValueToIds.get(currentMethod);
    Map<Object, TLongSet> nextMapValueToIds = bucket
        .mapMethodNameToValueToIds
        .computeIfAbsent(currentMethod, ignored -> new HashMap<>());
    Map<Object, MethodValueCache.Bucket<T>> nextMapValueToBucket = bucket
        .mapMethodNameToValueToBucket
        .computeIfAbsent(currentMethod, ignored -> new HashMap<>());
    boolean dependsOnParentIds = initializationRequired
        && index > 0
        && bucket.parent != null;
    TLongSet parentIds = dependsOnParentIds
        ? Optional.ofNullable(bucket.method)
        .map(bucket.parent.mapMethodNameToValueToIds::get)
        .map(parentMapValueToIds -> parentMapValueToIds.get(bucket.value))
        .orElse(null)
        : null;
    mapValueToIds.keySet().forEach(value -> {
      if (initializationRequired)
      {
        TLongSet rootIds = Optional.of(currentMethod)
            .map(rootBucket.mapMethodNameToValueToIds::get)
            .map(rootMapValueToIds -> rootMapValueToIds.get(value))
            .orElse(null);
        TLongSet intersectingIds;
        if (bucket.parent == null && rootIds != null)
        {
          intersectingIds = new TLongHashSet(rootIds);
        }
        else if (parentIds != null && rootIds != null)
        {
          intersectingIds = new TLongHashSet(parentIds);
          intersectingIds.retainAll(rootIds);
        }
        else
        {
          intersectingIds = null;
        }
        nextMapValueToIds.put(value, intersectingIds);
      }
      nextMapValueToBucket.computeIfAbsent(value,
          val -> new Bucket<>(bucket, currentMethod, val));
      if (index + 1 < methodNames.size())
      {
        indexBucketForMethods(nextMapValueToBucket.get(value), methodNames,
            index + 1);
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void addIndex(FieldIntersection<T> fieldIntersection)
  {
    ensureAllMethodsRegistered(fieldIntersection);
    indexBucketForMethods(rootBucket, fieldIntersection.getMethodNames(), 0);
  }

  private List<String> getNonRegisteredMethods(
      FieldIntersection<T> fieldIntersection)
  {
    return fieldIntersection.getMethodNames()
        .stream()
        .filter(methodName -> !this.rootBucket.mapMethodNameToValueToIds
            .containsKey(methodName))
        .collect(Collectors.toList());
  }
}
