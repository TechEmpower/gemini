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

import gnu.trove.map.*;
import gnu.trove.map.hash.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * This class is a simple data structure that holds objects of a particular
 * unspecified type, keyed by an long identifier. It can be limited to a
 * particular size, and will trim itself if it exceeds that size.
 * <p>
 * This is a default implementation of a CacheGroup that can be extended by
 * applications to have custom behaviors. For example, the trim methods may be
 * extended to use a LRU (Least-Recently Used) replacement algorithm.
 * <p>
 * We avoid thread synchronization on read operations (getting objects). Write
 * operations (adding, removing objects for example) are synchronized.
 * <p>
 * Read operations are made threadsafe without synchronization through the use
 * of CopyOnWriteArrayList (from the Java concurrency package) and
 * ConcurrentLongMap, which is a simple re-working of ConcurrentHashMap also
 * from the Java concurrency package.
 * <p>
 * Testing so far indicates read performance that is greatly improved over
 * previous versions that used synchronization for both reads and writes.
 */
public class CacheGroup<T extends Identifiable>
     extends EntityGroup<T>
  implements Initializable
{

  //
  // Member variables.
  //

  private ConcurrentMap<Long, T>  objects             = new ConcurrentHashMap<>();
  private List<T>                 objectsInOrder      = new CopyOnWriteArrayList<>();
  private volatile boolean        initialized         = false;
  private boolean                 errorOnInitialize   = false;
  private long                    lowestIdentity      = Long.MAX_VALUE;
  private long                    highestIdentity     = 0;

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  protected CacheGroup(EntityStore controller,
      Class<T> type, 
      String table, 
      String id, 
      EntityMaker<T> maker, 
      Comparator<? super T> comparator,
      String where, 
      String[] whereArguments,
      boolean readOnly)
  {
    super(controller, type, table, id, maker, comparator,
        where, whereArguments, readOnly);
  }

  /**
   * Creates a new {@link Builder}, which is used to construct an
   * {@link CacheGroup}.  Example usage:
   * 
   * <pre>
   * CacheGroup&lt;Foo&gt; = CacheGroup.of(Foo.class) // new Builder
   *     .table("foos") // modified Builder
   *     .id("fooID") // modified Builder
   *     .build(controller); // new CacheGroup
   * </pre>
   * 
   * <p>Note that a {@link EntityStore#register(EntityGroup.Builder)} method
   * exists, so in the common case where you only want to register the group and
   * don't care to retain your own reference to it, calling
   * {@code .build(controller)} is unnecessary.  For example:
   * 
   * <pre>
   * register(CacheGroup.of(Foo.class) // new Builder
   *     .table("foos") // modified Builder
   *     .id("fooID") // modified Builder
   * ); // the register method calls .build(controller) for us
   * </pre>
   * 
   * @param type The type of the entities.
   * @return A new {@link Builder}.
   */
  public static <T extends Identifiable> Builder<T> of(Class<T> type)
  {
    return new Builder<>(type);
  }

  /**
   * Sets the objects list to the contents of the provided Collection.
   */
  public void setObjects(Collection<T> objects)
  {
    if (readOnly())
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"setObjects\" method is not permitted.");
    }

    synchronized (this)
    {
      if (objects == null)
      {
        this.objects = new ConcurrentHashMap<>();
        this.objectsInOrder = new CopyOnWriteArrayList<>();
      }
      else
      {
        final ConcurrentMap<Long, T> workMap = new ConcurrentHashMap<>(
            objects.size());
        final List<T> workList = new ArrayList<>(objects.size());
        
        for (T object : objects)
        {
          workMap.put(object.getId(), object);
          workList.add(object);
        }

        // Replace the member variables.
        this.objects = workMap; 
        this.objectsInOrder = new CopyOnWriteArrayList<>(workList);

        // If we're setting objects from somewhere else, we should assume
        // that this is initializing the cache group.
        setInitialized(true);
      }
      
      // Recalculate high and low identities if these high/low values have
      // been used in the past.
      calculateHighLowIdentitiesRecalc();
    }
  }

  /**
   * Resets this group, removing all the objects, and setting the initialized
   * flag to false.  The group will be rebuilt from the database on next use.
   */
  @Override
  public void reset()
  {
    synchronized (this)
    {
      setInitialized(false);
      // Instantiate a new object so that any threads currently working with
      // an old reference outside of a synchronized block will not be
      // affected.
      this.objects = new ConcurrentHashMap<>();
      this.objectsInOrder = new CopyOnWriteArrayList<>();
      setErrorOnInitialize(false);
      resetHighLowIdentities();
    }
  }

  @Override
  public T get(long id)
  {
    initializeIfNecessary();
    
    return this.objects.get(id);
  }
  
  /**
   * Gets an object from the objects map in a raw manner--that is, calling
   * the map's get method directly without any pre-initialization.
   */
  protected T getRaw(long id)
  {
    return this.objects.get(id);
  }

  @Override
  public List<T> list()
  {
    initializeIfNecessary();
    
    return new ArrayList<>(this.objectsInOrder);
  }
  
  @Override
  public TLongObjectMap<T> map()
  {
    initializeIfNecessary();
    
    // Grab a reference.
    final ConcurrentMap<Long, T> map = this.objects;

    final TLongObjectMap<T> toReturn = new TLongObjectHashMap<>(map.size());
    final Iterator<T> values = map.values().iterator();
    T object;
    while (values.hasNext())
    {
      object = values.next();
      toReturn.put(object.getId(), object);
    }
    
    return toReturn;
  }

  /**
   * Add an object to the cache and the data store.
   */
  @Override
  public void put(T object)
  {
    if (readOnly())
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"put\" method is not permitted.");
    }
    
    initializeIfNecessary();
    
    // Get a snapshot of the current persistence state.
    final boolean persisted = isPersisted(object);
    
    // Persist the object.
    putPersistent(object);
    
    if (persisted)
    {
      // Object was already persisted, so let's reorder in case the field
      // we are ordered by was changed.
      reorder(object.getId());
    }
    else
    {
      // Object was not persisted, so let's add it to the cache.
      addToCache(object);
    }
  }
  
  /**
   * Called by CacheGroup.put to persist an entity to the database via a
   * call to the method of the same name provided by the superclass: 
   * EntityGroup.put.
   *   <p>
   * This method is exposed as a protected class to allow subclasses to 
   * specialize the persistence behavior.  
   */
  protected void putPersistent(T object)
  {
    // Ask the EntityGroup parent to persist the changes.
    super.put(object);
  }

  /**
   * Add objects to the cache and the data store.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void putAll(Collection<T> objectsToPut)
  {
    if (readOnly())
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"putAll\" method is not permitted.");
    }

    initializeIfNecessary();
    
    // Get a snapshot of the current persistence state.
    final List<Long> persisted = new ArrayList<>(objectsToPut.size());
    final List<T> nonPersisted = new ArrayList<>(objectsToPut.size());
    for (T object : objectsToPut)
    {
      if (isPersisted(object))
      {
        persisted.add(object.getId());
      }
      else
      {
        nonPersisted.add(object);
      }
    }
    
    // Persist the changes.
    putAllPersistent(objectsToPut);
    
    // Reorder any previously-persisted objects in case the field we are 
    // ordered by was changed.
    reorder(CollectionHelper.toLongArray(persisted));
    
    // Add any objects we did not previously know about.
    addToCache(nonPersisted.toArray((T[])Array.newInstance(type(), nonPersisted.size())));
  }
  
  /**
   * Called by CacheGroup.putAll to persist a list of entities to the
   * database via a call to the method of the same name provided by the
   * superclass: EntityGroup.putAll.
   *   <p>
   * This method is exposed as a protected class to allow subclasses to 
   * specialize the persistence behavior.  
   */
  protected void putAllPersistent(Collection<T> objectsToPut)
  {
    // Ask the EntityGroup parent to persist the objects.
    super.putAll(objectsToPut);
  }

  /**
   * Remove an object from the cache and the data store.
   */
  @Override
  public void remove(long id)
  {
    if (readOnly())
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"remove\" method is not permitted.");
    }

    initializeIfNecessary();
    
    // Remove the persistent instance of the object.
    removePersistent(id);

    // Remove it from the cache.
    removeFromCache(id);
  }
  
  /**
   * Called by CacheGroup.remove to remove an object from the persistent
   * store (database).
   *   <p>
   * This method is exposed as a protected class to allow subclasses to 
   * specialize the persistence behavior.  
   */
  protected void removePersistent(long id)
  {
    // Ask the EntityGroup parent to remove the persisted object.
    super.remove(id);
  }

  /**
   * Add objects to the cache only, not affecting the data store.
   */
  @SafeVarargs
  public final void addToCache(T... objectsToAdd)
  {
    // Grab references.
    final ConcurrentMap<Long, T> map = this.objects;
    final List<T> orderedList = this.objectsInOrder;
    
    synchronized (this)
    {
      for (T object : objectsToAdd)
      {
        // Only proceed if we don't already have this reference in the cache.
        if (!map.containsValue(object))
        {
          // If we already have a reference with the same ID, let's remove it.
          if (map.containsKey(object.getId()))
          {
            // Remove the existing reference from the ordered list.
            orderedList.remove(map.get(object.getId()));
          }
          
          map.put(object.getId(), object);
          int search = Collections.binarySearch(orderedList, object, comparator());
          if (search < 0)
          {
            orderedList.add(-search - 1, object);
          }
          else
          {
            orderedList.add(object);
          }
    
          if (areHighLowIdentitiesInitialized())
          {
            if (object.getId() < this.lowestIdentity)
            {
              this.lowestIdentity = object.getId();
            }
            if (object.getId() > this.highestIdentity)
            {
              this.highestIdentity = object.getId();
            }
          }
        }
      }
    }
  }

  /**
   * Removes an object from the cache only, not affecting the data store.
   */
  public boolean removeFromCache(T object)
  {
    return removeFromCache(object.getId());
  }

  /**
   * Removes objects from the cache only, not affecting the data store.
   */
  public boolean removeFromCache(long... ids)
  {
    // Grab references.
    final ConcurrentMap<Long, T> map = this.objects;
    final List<T> orderedList = this.objectsInOrder;
    
    synchronized (this)
    {
      for (long id : ids)
      {
        orderedList.remove(map.remove(id));
      }

      // Recalculate high/low identities if needed.
      calculateHighLowIdentitiesRecalc();
      
      return true;
    }
  }

  /**
   * Does the cacheGroup contain this particular object?
   *
   * @param object The object to search for.
   */
  public boolean contains(T object)
  {
    initializeIfNecessary();

    return this.objects.containsValue(object);
  }

  /**
   * Does the cacheGroup contain this particular object as specified 
   * by the identifier?
   *
   * @param id The ID (identifier) to search for.
   */
  public boolean contains(long id)
  {
    initializeIfNecessary();

    return this.objects.containsKey(id);
  }

  /**
   * Returns the result of calling the objects map's contains method without
   * any pre-initialization.
   */
  protected boolean containsRaw(long id)
  {
    return this.objects.containsKey(id);
  }

  /**
   * Returns the result of calling the objects map's contains method without
   * any pre-initialization.
   */
  protected boolean containsRaw(Identifiable object)
  {
    return this.objects.containsValue(object);
  }

  /**
   * If true, this indicates that a database failure caused initialization
   * to fail.
   */
  public boolean isErrorOnInitialize()
  {
    return this.errorOnInitialize;
  }

  /**
   * When fetching a cache group's contents from the database, the cache
   * controller may set this flag to true to indicate that an error was
   * encountered during initialization.
   */
  public void setErrorOnInitialize(boolean errorOnInitialize)
  {
    this.errorOnInitialize = errorOnInitialize;
  }

  /**
   * Calls initialize() if the Cache Group has not already been initialized.
   * That is, if the initialized flag is false.
   */
  public void initializeIfNecessary()
  {
    if (!this.initialized)
    {
      synchronized (this)
      {
        if (!this.initialized)
        {
          initialize();
        }
      }
    }
  }

  /**
   * Initializes this CacheGroup.
   */
  @Override
  public void initialize()
  {
    synchronized (this)
    {
      this.objectsInOrder = new CopyOnWriteArrayList<>(fetchAllPersistedObjects());
      copyOrderedObjectsToMap();
      
      // Reset the high and low identities.
      resetHighLowIdentities();
      
      setInitialized(true);
      
      // Execute custom post-initialization processing.
      customPostInitialization();
    }
  }
  
  /**
   * Called by initialize to fetch a list of persistent entities using the
   * EntityGroup.list method.
   */
  protected List<T> fetchAllPersistedObjects()
  {
    return super.list();
  }
  
  /**
   * Executes custom post-initialization processing for the group.  Note that
   * the group will remain blocked (within the initialization "synchronized"
   * block) until this method returns.  Post-initialization processing should
   * therefore be as quick as possible.
   */
  protected void customPostInitialization()
  {
    // Does nothing in this base class.
  }

  /**
   * Copies ordered objects to the LongMap.
   */
  protected void copyOrderedObjectsToMap()
  {
    final Iterator<T> iter = this.objectsInOrder.iterator();
    
    // Create a new map, work, to populate.
    final ConcurrentMap<Long, T> work = new ConcurrentHashMap<>(
        this.objectsInOrder.size());
    T co;
    while (iter.hasNext())
    {
      co = iter.next();
      work.put(co.getId(), co);
    }
    
    // Replace the member variable.
    this.objects = work;
  }

  /**
   * Sets the initialized flag.
   *   <p>
   * Calls to initialize should set the initialized flag to true.
   *   <p>
   * Calls to reset will set the initialized flag to false.
   */
  protected void setInitialized(boolean initialized)
  {
    this.initialized = initialized;
  }

  /**
   * Gets the initialized variable.  See setInitialized.
   */
  @Override
  public boolean isInitialized()
  {
    return this.initialized;
  }

  @Override
  public long lowest()
  {
    calculateHighLowIdentitiesInitial();
    return this.lowestIdentity;
  }

  @Override
  public long highest()
  {
    calculateHighLowIdentitiesInitial();
    return this.highestIdentity;
  }

  /**
   * Resets the highest and lowest identity member variables. 
   */
  protected void resetHighLowIdentities()
  {
    this.lowestIdentity  = Long.MAX_VALUE;
    this.highestIdentity = 0;
  }

  /**
   * Determines if the highest and lowest identity member variables have
   * been initialized (whether they have been used).
   */
  protected boolean areHighLowIdentitiesInitialized()
  {
    return ( (this.lowestIdentity < Long.MAX_VALUE)
          || (this.highestIdentity > 0)
          );
  }

  /**
   * Determines the highest and lowest identity of objects in the group
   * and store these values in the member variables.
   */
  protected void calculateHighLowIdentities()
  {
    final Iterator<?> iter = this.objectsInOrder.iterator();
    
    Identifiable co;
    long id;
    while (iter.hasNext())
    {
      co = (Identifiable)iter.next();
      id = co.getId();
      if (id < this.lowestIdentity)
      {
        this.lowestIdentity = id;
      }
      if (id > this.highestIdentity)
      {
        this.highestIdentity = id;
      }
    }
  }

  /**
   * Calculates the high and low identities if they have not yet been
   * calculated.  This is called by getLowestIdentity and getHighestIdentity
   * to allow for lazy-initialization of these member variables.
   */
  protected void calculateHighLowIdentitiesInitial()
  {
    if (!areHighLowIdentitiesInitialized())
    {
      calculateHighLowIdentities();
    }
  }

  /**
   * Recalculates high and low identities only if they have been initialized.
   * This is used when objects are added to the collection but only when
   * the high/low values have been used in the past.  This avoids the
   * computation expense if the application never uses these methods.
   */
  protected void calculateHighLowIdentitiesRecalc()
  {
    if (areHighLowIdentitiesInitialized())
    {
      calculateHighLowIdentities();
    }
  }

  /**
   * Returns the current size of the cacheGroup.
   */
  @Override
  public int size()
  {
    initializeIfNecessary();

    return this.objects.size();
  }

  /**
   * This method ensures that the next time the specified objects are 
   * fetched, they will be at least as current as the time of this method 
   * call.
   * 
   * @param ids the ids of the objects
   */
  @Override
  public void refresh(long... ids)
  {
    // Grab references.
    final ConcurrentMap<Long, T> map = this.objects;
    final List<T> orderedList = this.objectsInOrder;

    synchronized (this)
    {
      // If the group is not initialized, we're done.
      if (!this.initialized)
      {
        return;
      }
      
      // Fetch the new objects.
      final TLongObjectMap<T> objectsMap = super.map(CollectionHelper.toList(ids));
      
      for (long id : ids)
      {
        // Remove the object with this id from the cache, if it's there.
        orderedList.remove(map.remove(id));
        final T object = objectsMap.get(id);
        
        // Put the newly loaded object into the cache.
        if (object != null)
        {
          map.put(id, object);
          // Use the comparator to insert it at the appropriate position.
          int search = Collections.binarySearch(orderedList, object, comparator());
          if (search < 0)
          {
            orderedList.add(-search - 1, object);
          }
          else
          {
            orderedList.add(object);
          }
        }
      }
    }
  }

  /**
   * This method ensures that the next time the ordered objects are fetched, 
   * the specified objects will be in the correct positions.
   * 
   * @param ids the ids of the objects
   */
  @Override
  public void reorder(long... ids)
  {
    // Grab references.
    final ConcurrentMap<Long, T> map = this.objects;
    final List<T> orderedList = this.objectsInOrder;

    synchronized (this)
    {
      // If the group is not initialized, we're done.
      if (!this.initialized)
      {
        return;
      }
      
      for (long id : ids)
      {
        final T object = map.get(id);
        if (object != null)
        {
          // Since writes to the list are expensive, let's see if it's already
          // in the correct order before modifying anything.
          final int index = orderedList.indexOf(object);
          final T previous = (index == 0)
              ? null
              : orderedList.get(index - 1);
          final T next = (index == this.objectsInOrder.size() - 1)
              ? null
              : orderedList.get(index + 1);
          if ((previous == null || comparator().compare(object, previous) >= 0)
              && (next == null || comparator().compare(object, next) <= 0))
          {
            return;
          }
          // Boo, it's out of order.  We need to do two write operations now,
          // which causes the list to copy itself twice.
          orderedList.remove(object);
          final int search = Collections.binarySearch(orderedList, object, 
              comparator());
          if (search < 0)
          {
            orderedList.add(-search - 1, object);
          }
          else
          {
            orderedList.add(object);
          }
        }
      }
    }
  }

  @Override
  public String toString()
  {
    return "CacheGroup [" + name() + "]";
  }

  @Override
  public void removeAll(Collection<Long> ids)
  {
    if (readOnly())
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"removeAll\" method is not permitted.");
    }

    initializeIfNecessary();
    
    // Ask the EntityGroup parent to persist the changes.
    removeAllPersistent(ids);
    
    // Remove it from the cache.
    this.removeFromCache(CollectionHelper.toLongArray(ids));
  }
  
  /**
   * Called by CacheGroup.removeAll to remove a collection of objects from the
   * persistent store (database).
   *   <p>
   * This method is exposed as a protected class to allow subclasses to 
   * specialize the persistence behavior.  
   */
  protected void removeAllPersistent(Collection<Long> ids)
  {
    // Ask the EntityGroup parent to persist the changes.
    super.removeAll(ids);
  }

  @Override
  public List<T> list(Collection<Long> ids)
  {
    initializeIfNecessary();
    
    final List<T> theObjects = new ArrayList<>(ids.size());
    for (long id : ids)
    {
      final T object = this.objects.get(id);
      if (object != null)
      {
        theObjects.add(object);
      }
    }
    return theObjects;
  }

  @Override
  public TLongObjectMap<T> map(Collection<Long> ids)
  {
    initializeIfNecessary();
    
    final TLongObjectMap<T> theObjects = new TLongObjectHashMap<>(ids.size());
    for (long id : ids)
    {
      final T object = this.objects.get(id);
      if (object != null)
      {
        theObjects.put(id, object);
      }
    }
    return theObjects;
  }

  // 
  // Inner classes.
  // 

  /**
   * Creates new instances of {@code CacheGroup}.
   */
  public static class Builder<T extends Identifiable>
      extends EntityGroup.Builder<T>
  {
    protected Builder(Class<T> type)
    {
      super(type);
    }
    
    @Override
    public CacheGroup<T> build(EntityStore controller)
    {
      if (controller == null)
      {
        throw new NullPointerException();
      }
      
      return new CacheGroup<>(
          controller,
          this.type,
          this.table,
          this.id,
          this.maker,
          this.comparator,
          this.where,
          this.whereArguments,
          this.readOnly);
    }

    @Override
    public Builder<T> table(String tableName)
    {
      super.table(tableName);
      return this;
    }

    @Override
    public Builder<T> id(String idFieldName)
    {
      super.id(idFieldName);
      return this;
    }
    
    @Override
    public Builder<T> readOnly()
    {
      super.readOnly();
      return this;
    }

    @Override
    public Builder<T> maker(EntityMaker<T> entityMaker)
    {
      super.maker(entityMaker);
      return this;
    }

    @Override
    public Builder<T> comparator(Comparator<? super T> entityComparator)
    {
      super.comparator(entityComparator);
      return this;
    }

    @Override
    public Builder<T> comparator(String methodName)
    {
      super.comparator(methodName); 
      return this;
    }

    @Override
    public Builder<T> where(String whereClause, String... arguments)
    {
      super.where(whereClause, arguments);
      return this;
    }

    @Override
    public Builder<T> constructorArgs(Object... arguments)
    {
      super.constructorArgs(arguments);
      return this;
    }

  } // End Builder.
  
}   // End CacheGroup.