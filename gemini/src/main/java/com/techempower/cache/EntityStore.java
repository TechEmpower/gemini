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

import com.google.common.primitives.*;
import gnu.trove.map.*;
import gnu.trove.map.hash.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.reflections.*;

import com.techempower.*;
import com.techempower.cache.annotation.*;
import com.techempower.classloader.*;
import com.techempower.collection.*;
import com.techempower.data.*;
import com.techempower.data.EntityGroup.Builder;
import com.techempower.data.annotation.*;
import com.techempower.gemini.cluster.*;
import com.techempower.gemini.configuration.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.reflect.*;
import com.techempower.util.*;

/**
 * This class serves as a storage manager for several entity groups and the
 * relationships between those entity groups.  It also provides some extra 
 * functionality such as sorting.
 *   <p>
 * Although this class has evolved into a role of controlling storage for both
 * cached and non-cached entities, it remains in the com.techempower.cache 
 * package because most applications use caching to some degree (there is no 
 * parent class without caching from which this class derives functionality)
 * and also for compatibility reasons.
 *   <p>
 * When used as part of a Gemini application, Gemini's InfrastructureServlet
 * will automatically call the initialize method when the Servlet is
 * initialized.
 *   <p>
 * Configuration options:
 * <ul>
 * <li>EntityStore.CacheMethodValues - Cache the values of objects 
 * queried by reflection (in getObjectByMethod, etc.), which can improve the 
 * performance of future lookups at the cost of memory?  Default is no.</li>
 * </ul>
 */
public class EntityStore
  implements Configurable
{

  //
  // Constants.
  //

  public static final String     COMPONENT_CODE = "cach";
  
  private static final Class<?>[] NO_PARAMETERS  = new Class[0];
  private static final Object[]   NO_VALUES      = new Object[0];
  private static final int        INITIAL_GROUPS_SIZE = 20;
  private static final String     ERROR_METHOD_ACCESS = "Method cannot be accessed: ";
  private static final String     ERROR_FIELD_ACCESS = "Field cannot be accessed: ";

  //
  // Member variables.
  //

  private final TechEmpowerApplication                  application;
  private final Map<Class<? extends Identifiable>,EntityGroup<? extends Identifiable>>
                                                        groups;
  private final ConnectorFactory                        connectorFactory;
  private final ComponentLog                            log;
  private final ComponentLog                            entityLog;
  private volatile CacheListener[]                      listeners;

  private ExecutorService preinitializationTasks = Executors.newSingleThreadExecutor();
  private Reflections     reflections       = null;
  private boolean         initialized       = false;
  private boolean         cacheMethodValues = false;

  /**
   * The registered method value caches.  These allow you to quickly find 
   * entities by the value of a given field.
   */
  private Map<Class<? extends Identifiable>, MethodValueCache<?>> methodValueCaches = new HashMap<>();

  private final TLongObjectMap<Class<? extends Identifiable>> entityGroupClassesById = new TLongObjectHashMap<>();

  /**
   * The list of all relations managed by this entity store.  This is a 
   * superset of the {@link #cachedRelations} list.
   */
  private final List<EntityRelation<? extends Identifiable,? extends Identifiable>> relations = new ArrayList<>();

  /**
   * The list of all cached relations managed by this entity store.  This is a
   * subset of the {@link #relations} list.  Some operations, such as
   * communicating about cache resets, only affect cached relations, so this
   * list is provided (in addition to the other) as a convenience.
   */
  private final List<CachedRelation<? extends Identifiable,? extends Identifiable>> cachedRelations = new ArrayList<>();

  /**
   * A map from definition classes (in practice, any arbitrary class) to
   * relations.
   */
  private final ConcurrentMap<Class<EntityRelationDescriptor<? extends Identifiable, ? extends Identifiable>>, EntityRelation<? extends Identifiable, ? extends Identifiable>> relationsMap = new ConcurrentHashMap<>();

  private ConcurrentMap<String, String> cachedResponses = new ConcurrentHashMap<>();

  /**
   * Keeps track of the methods that are annotated with either @Indexed or 
   * @NotIndexed.
   */
  private final Map<Class<? extends Identifiable>, Map<String, Boolean>> indexedAnnotatedMethods = new HashMap<>();

  /**
   * Keeps track of the classes that are annotated with either @Indexed or 
   * @NotIndexed.
   */
  private final Map<Class<? extends Identifiable>, Boolean> indexedAnnotatedClasses = new HashMap<>();

  /**
   * The type adapters used by entity groups in this cache.  These allow
   * non-standard data entity field types to be stored in the database.
   */
  private final List<TypeAdapter<?, ?>> typeAdapters = new ArrayList<>();
  
  //
  // Member methods.
  //

  /**
   * Constructor.
   *
   * @param application a reference to the application using this
   *        EntityStore.
   * @param connectorFactory A ConnectorFactory to use to
   *        communicate with the database.
   */
  public EntityStore(TechEmpowerApplication application,
    final ConnectorFactory connectorFactory)
  {
    this.application      = application;
    this.connectorFactory = connectorFactory;
    this.groups           = new HashMap<>(INITIAL_GROUPS_SIZE);
    this.listeners        = new CacheListener[0];
    this.log              = application.getLog(COMPONENT_CODE);
    this.entityLog        = application.getLog("data");
    
    // Start constructing Reflections on a new thread since it takes a
    // bit of time.
    preinitializationTasks.submit(new Runnable() {
      @Override
      public void run() {
        try
        {
          log.log("Instantiating Reflections component.");
          reflections = PackageClassLoader.getReflectionClassLoader(application);
          log.log("Reflections component instantiated: " + reflections);
        }
        catch (Exception exc)
        {
          log.log("Exception while instantiating Reflections component.", exc);
        }
      }
    });
  }
  
  /**
   * Configure the cache.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // First read the legacy name for this property and then the new name.
    cacheMethodValues = props.getBoolean("CacheController.CacheMethodValues", false);
    cacheMethodValues = props.getBoolean("EntityStore.CacheMethodValues", cacheMethodValues);
    
    methodValueCaches = new HashMap<>();
    
    // This should only happen when the application is reconfigured.
    if (CollectionHelper.isNonEmpty(groups))
    {
      for (EntityGroup<?> group : groups.values())
      {
        methodValueCaches.put(group.type(), 
            new MethodValueCache<>(this, group.type()));
      }
    }
  }

  /**
   * Are we caching all referenced method's values?  This is dependent on the
   * value of {@code cacheMethodValues} and also whether or not each method
   * has been annotated with {@code @Indexed} or {@code @NotIndexed}
   * annotations. In order for the intersection to be indexed, <b>every</b>
   * referenced method must be indexed.
   *
   * <p>{@code forceIndexedMethods} and {@code forceNotIndexedMethods} must be
   * initialized for this class type before calling this.  This should be
   * done when registering a cache group.
   *
   * @return true if this intersection has its values cached.  Returns false if
   * this intersection does not have its values cached or this class type is
   * not registered in the cache group.
   */
  protected <T extends Identifiable> boolean isIndexedInt(
      FieldIntersection<T> fieldIntersection)
  {
    Class<T> type = fieldIntersection.getType();
    // This is not a cached data entity, so immediately return false.
    if (!groups.containsKey(type))
    {
      return false;
    }

    Boolean classIndexed = indexedAnnotatedClasses.get(type);
    // The class wasn't specifically annotated, so use the global cacheMethodValues
    // value.
    if (classIndexed == null)
    {
      classIndexed = cacheMethodValues;
    }

    Map<String, Boolean> indexedAnnotatedMethods = this
        .indexedAnnotatedMethods.get(type);
    for(String method : fieldIntersection.getMethodNames())
    {
      Boolean methodIndexed = indexedAnnotatedMethods.get(method);
      // If we are caching method values by default, then we only need to check
      // if this method was explicitly marked as @NotIndexed.
      if (classIndexed)
      {
        // This will always return true unless the method was specifically
        // annotated and explicitly marked as @NotIndexed.
        if (!(methodIndexed == null || methodIndexed))
        {
          return false;
        }
      }
      // If we are not caching method values by default, then we only need to check
      // if this method was explicitly marked as @Indexed.
      else
      {
        // This method was specifically annotated and explicitly marked as
        // @Indexed.
        if (!(methodIndexed != null && methodIndexed))
        {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Are we caching this method's values?  This is dependent on the value of
   * {@code cacheMethodValues} and also whether or not the method has been
   * annotated with {@code @Indexed} or {@code @NotIndexed} annotations.
   *
   * <p>{@code forceIndexedMethods} and {@code forceNotIndexedMethods} must be
   * initialized for this class type before calling this.  This should be
   * done when registering a cache group.
   *
   * @return true if this method has its values cached.  Returns false if this
   * method does not have its values cached or this class type is not registered
   * in the cache group.
   */
  protected <T extends Identifiable> boolean isIndexed(Class<T> type, String methodName)
  {
    // This is not a cached data entity, so immediately return false.
    if (!groups.containsKey(type))
    {
      return false;
    }
    
    Boolean classIndexed = indexedAnnotatedClasses.get(type);
    // The class wasn't specifically annotated, so use the global cacheMethodValues
    // value.
    if (classIndexed == null)
    {
      classIndexed = cacheMethodValues;
    }
    
    // If we are caching method values by default, then we only need to check
    // if this method was explicitly marked as @NotIndexed.
    if (classIndexed)
    {
      Boolean methodIndexed = indexedAnnotatedMethods.get(type).get(methodName);
      // This will always return true unless the method was specifically
      // annotated and explicitly marked as @NotIndexed.
      return methodIndexed == null || methodIndexed;
    }
    // If we are not caching method values by default, then we only need to check
    // if this method was explicitly marked as @Indexed.
    else
    {
      Boolean methodIndexed = indexedAnnotatedMethods.get(type).get(methodName);
      // This method was specifically annotated and explicitly marked as 
      // @Indexed.
      return methodIndexed != null && methodIndexed;
    }
  }
  
  /**
   * Gets a ComponentLog for entities to use.
   */
  public ComponentLog getEntityLog()
  {
    return entityLog;
  }

  /**
   * Reset all entity groups controlled by this controller.
   */
  public void reset()
  {
    reset(true, true);
  }

  /**
   * Reset each entity group controlled by this controller.  Note that 
   * listeners are only notified of the full reset as a coarse-grained
   * notification.  That is, the listeners will not receive individual
   * calls to cacheTypeReset for all entity types during a full reset.
   *
   * @param notifyListeners Should CacheListeners be notified of this reset?
   */
  public void reset(boolean notifyListeners, boolean notifyDistributionListeners)
  {
    log.log("Full reset.", LogLevel.DEBUG);

    // Reset entity groups.
    for (EntityGroup<?> group : groups.values())
    {
      group.reset();
    }

    // Reset relations.
    for (CachedRelation<?,?> relation : cachedRelations)
    {
      relation.reset(notifyListeners, notifyDistributionListeners);
    }
    
    // Reset method value cache.
    for (MethodValueCache<?> methodValueCache : methodValueCaches.values())
    {
      methodValueCache.reset();
    }

    // Notify the listeners of the full reset only.  We do not call
    // cacheTypeReset for each entity type.
    if (notifyListeners)
    {
      final CacheListener[] toNotify = listeners;
      for (CacheListener listener : toNotify)
      {
        listener.cacheFullReset();
      }
    }
  }

  /**
   * Reset the entity group specified by the type id.
   */
  public <T extends Identifiable> void reset(Class<T> type)
  {
    reset(type, true, true);
  }

  /**
   * Reset the entity group specified by the type id.
   *
   * @param type the object type of the entity group to be reset.
   * @param notifyListeners Should CacheListeners be notified of this reset?
   * @param notifyDistributionListeners Whether to notify any
   *        DistributionListeners; only used When notifyListeners is true.
   */
  public <T extends Identifiable> void reset(Class<T> type, 
      boolean notifyListeners, 
      boolean notifyDistributionListeners)
  {
    for (CachedRelation<?,?> relation : cachedRelations)
    {
      relation.reset(type, notifyListeners, notifyListeners);
    }
    
    final MethodValueCache<?> methodValueCache = methodValueCaches.get(type);
    if (methodValueCache != null)
    {
      methodValueCache.reset();
    }

    final EntityGroup<?> group = groups.get(type);
    if (group != null)
    {
      group.reset();

      if (notifyListeners)
      {
        // Notify the listeners.
        final CacheListener[] toNotify = listeners;
        for (CacheListener listener : toNotify)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.cacheTypeReset(group.type());
          }
        }
      }
    }
  }

  /**
   * Adds a CacheListener to be notified of cache events.
   */
  public synchronized void addListener(CacheListener listener)
  {
    // Do nothing if we already have this listener in our list.
    if (CollectionHelper.arrayContains(listeners, listener))
    {
      return;
    }
    
    // Copy existing listeners.
    final CacheListener[] newArray = new CacheListener[listeners.length + 1];
    if (listeners.length > 0)
    {
      System.arraycopy(listeners, 0, newArray, 0, listeners.length);
    }
    // Add our new listener.
    newArray[newArray.length - 1] = listener;
    
    // Replace the member variable.
    listeners = newArray;
  }

  /**
   * Removes a CacheListener.
   */
  public synchronized void removeListener(CacheListener listener)
  {
    if (CollectionHelper.arrayContains(listeners, listener))
    {
      final CacheListener[] newArray = new CacheListener[listeners.length - 1];
      int index = 0;
      for (CacheListener l : listeners)
      {
        if (l != listener)
        {
          newArray[index++] = l;
        }
      }
      
      listeners = newArray;
    }
  }

  /**
   * Gets an entity group by type.  Returns null if no such entity group
   * is found.
   */
  @SuppressWarnings("unchecked")
  public <T extends Identifiable> EntityGroup<T> getGroup(Class<T> type)
  {
    return (EntityGroup<T>)groups.get(type);
  }
  
  /**
   * Gets an entity group by type, throwing a ControllerError if no such
   * group is found.
   */
  protected <T extends Identifiable> EntityGroup<T> getGroupSafe(Class<T> type)
  {
    if (type == null)
    {
      throw new ControllerError("Invalid parameter: type is null.");
    }
      
    EntityGroup<T> toReturn = getGroup(type);
    if (toReturn == null)
    {
      throw new ControllerError(type.getSimpleName() + " is not registered with the EntityStore.");
    }
    else
    {
      return toReturn;
    }
  }
  
  /**
   * Gets a cache group by its unique group number.
   *   <p>
   * This method is intended for internal use within the Gemini core, not 
   * broad use within Gemini applications.
   */
  public EntityGroup<? extends Identifiable> getGroup(int groupNumber)
  {
    return getGroup(entityGroupClassesById.get(groupNumber));
  }

  /**
   * Gets a cached relation by its unique id.
   * <p>
   * This method is intended for internal use within the Gemini core, not broad
   * use within Gemini applications.
   */
  public CachedRelation<? extends Identifiable,? extends Identifiable> getCachedRelation(long relationId)
  {
    return cachedRelations.get(Ints.saturatedCast(relationId - 1));
  }
  
  /**
   * Collects @Indexed and @NotIndexed annotations for data entity methods.
   * This should be called before adding the entity to the cache group.
   */
  protected <T extends Identifiable> void collectIndexedMethodAnnotations(EntityGroup<T> group)
  {
    indexedAnnotatedMethods.put(group.type(), new HashMap<String, Boolean>());
    
    for (Method method : group.type().getMethods())
    {
      if (method.isAnnotationPresent(Indexed.class))
      {
        indexedAnnotatedMethods.get(group.type()).put(method.getName(), true);
      }
      else if (method.getAnnotation(NotIndexed.class) != null)
      {
        indexedAnnotatedMethods.get(group.type()).put(method.getName(), false);
      }
    }
    
    if (group.type().isAnnotationPresent(Indexed.class))
    {
      indexedAnnotatedClasses.put(group.type(), true);
    }
    else if (group.type().isAnnotationPresent(NotIndexed.class))
    {
      indexedAnnotatedClasses.put(group.type(), false);
    }
  }

  /**
   * Add a new cache group to this Controller.
   *
   * @param group The entities to be cached.
   */
  public <T extends Identifiable> void register(EntityGroup.Builder<T> group)
  {
    register(group.build(this));
  }

  /**
   * Add a new cache group to this Controller.
   *
   * @param group The entities to be cached.
   */
  public <T extends Identifiable> EntityGroup<T> register(EntityGroup<T> group)
  {
    // Collect @Indexed and @NotIndexed annotations
    collectIndexedMethodAnnotations(group);
    
    // Add the group.
    groups.put(group.type(), group);
    
    // Give the group a unique ID.
    group.setGroupNumber(groups.size());
    entityGroupClassesById.put(group.getGroupNumber(), group.type());
    
    methodValueCaches.put(group.type(), 
        new MethodValueCache<>(this, group.type()));
    
    log.log("Registered " + group + " with id " + group.getGroupNumber(),
        LogLevel.DEBUG);
    return group;
  }

  /**
   * Returns a list of registered entity groups.
   */
  public List<EntityGroup<? extends Identifiable>> getGroupList()
  {
    return new ArrayList<>(groups.values());
  }

  /**
   * Get the connector factory for this CC.
   */
  public ConnectorFactory getConnectorFactory()
  {
    return connectorFactory;
  }
  
  /**
   * Gets the Application reference.
   */
  public TechEmpowerApplication getApplication()
  {
    return application;
  }
  
  /**
   * Gets the ComponentLog reference.
   */
  public ComponentLog getLog()
  {
    return log;
  }

  /**
   * Initialize the EntityStore.  The basic implementation provided here
   * will attempt to find any classes annotated with the Entity and
   * CachedEntity annotations within the application's package hierarchy.
   */
  public void initialize()
  {
    // Wait for pre-initialization tasks to complete.
    try
    {
      log.log("Completing preinitialization tasks.");
      preinitializationTasks.shutdown();
      log.log("Awaiting termination of preinitialization tasks.");
      preinitializationTasks.awaitTermination(5L, TimeUnit.MINUTES);
      log.log("Preinitialization tasks complete.");
      log.log("Reflections component: " + reflections);
    }
    catch (InterruptedException iexc)
    {
      log.log("Preinitialization interrupted.", iexc);
    }
    
    // Throw an exception if Reflections is not ready.
    if (reflections == null)
    {
      throw new ConfigurationError("Reflections not ready; application cannot start.");
    }
    
    // The default behavior is to search com.techempower and the package of 
    // the main application for any cache annotations and auto register any 
    // entity types found.
    register();
    
    initialized = true;
  }

  /**
   * Return the number of the objects contained in the entity group specified
   * by type. Throws ControllerError if no such group is registered.
   */
  public <T extends Identifiable> int size(Class<T> type)
  {
    return getGroupSafe(type).size();
  }

  /**
   * Return all the objects contained in the entity group specified by 
   * type.  Throws ControllerError if no such group is registered.
   */
  public <T extends Identifiable> List<T> list(Class<T> type)
  {
    return getGroupSafe(type).list();
  }

  /**
   * Return all the objects contained in the entity group specified by
   * type, mapped by id.  Throws ControllerError if no such group is registered.
   */
  public <T extends Identifiable> TLongObjectMap<T> map(Class<T> type)
  {
    return getGroupSafe(type).map();
  }

  /**
   * Return a builder-style cache accessor in the entity group for the given
   * entity type.
   */
  public <T extends Identifiable> EntitySelector<T> select(Class<T> type)
  {
    return new EntitySelector<>(type, this);
  }

  /**
   * Return a builder-style cache accessor in the entity group. Capable of
   * matching any of the listed types.
   *
   * Note: One benefit of this method over the more automatic
   * {@link #selectAny(Class)} is that due to how Java's generics work, this
   * can pick up on any shared interfaces that the listed classes have, rather
   * than just a specific one.
   *
   * For example, suppose you had a Person interface extending Identifiable, a
   * Rich interface, a Doctor class implementing both, and a Lawyer class
   * implementing both. A selection stemming from this method passing in both
   * Lawyer.class and Doctor.class could reference any Person or Rich method.
   * On the other hand, {@link #selectAny(Class)}, while convenient, is limited
   * to only one filter class (in this case it would have to be Person, as Rich
   * is not Identifiable).
   */
  public <T extends Identifiable> MultiEntitySelector<T> select(
      Collection<Class<? extends T>> types)
  {
    return new MultiEntitySelector<>(types, this);
  }

  /**
   * Return a builder-style cache accessor in the entity group. Capable of
   * matching any registered entity type that is assignable from the given one.
   */
  @SuppressWarnings("unchecked")
  public <T extends Identifiable> MultiEntitySelector<T> selectAny(
      Class<? extends T> type)
  {
    List<Class<? extends T>> types = getGroupList()
        .stream()
        .map(EntityGroup::getType)
        .filter(type::isAssignableFrom)
        .map(otherType -> (Class<? extends T>)otherType)
        .collect(Collectors.toList());
    return select(types);
  }

  /**
   * Return a collection of objects contained in the entity group based
   * on a method name value and object type.  Returns empty collection in the 
   * event of an error or if no objects cannot be found.
   */
  @SuppressWarnings("unchecked")
  public <T extends Identifiable> List<T> list(Class<T> type, String methodName, Object value)
  {
    if (isIndexed(type, methodName))
    {
      MethodValueCache<T> methodValueCache = (MethodValueCache<T>)methodValueCaches.get(type);
      if (methodValueCache != null)
      {
        return methodValueCache.getObjects(methodName, value);
      }
    }
    
    return list(type, methodName, NO_PARAMETERS, value);
  }

  /**
   * A var args version of getObjects that returns an intersection (not a
   * union!) of multiple method name/value pairs. Basically, this allows you
   * to lookup entity objects based on multiple method values, instead of a
   * singular value. A real world example:
   * Suppose we want to grab all Foo's with the following attributes:
   * <ul>
   * <li>Enabled set to true</li>
   * <li>BarTypeId set to 1</li>
   * </ul>
   * The following code would accomplish the above:
   * <pre>
   * {@code
   *  list(Foo.class, "isEnabled", true, "getBarTypeId", 1);
   * }
   * </pre>
   * Returns empty collection in the event of an error or if no objects
   * cannot be found.
   *
   * @param methodNameThenValuePairs must be pairs of method name then value
   *                                 objects. If the length of the objects
   *                                 passed in is not a multiple of two, an
   *                                 IllegalArgumentException will be thrown.
   */
  public <T extends Identifiable> List<T> list(Class<T> type,
                                               String methodName,
                                               Object value,
                                               Object... methodNameThenValuePairs)
  {
    return list(new FieldIntersection<>(type, methodName, value,
        methodNameThenValuePairs));
  }

  @SuppressWarnings("unchecked")
  <T extends Identifiable> List<T> list(FieldIntersection<T> fieldIntersection)
  {
    Class<T> type = fieldIntersection.getType();
    if (isIndexedInt(fieldIntersection))
    {
      MethodValueCache<T> methodValueCache = (MethodValueCache<T>)this.methodValueCaches
          .get(type);
      if (methodValueCache != null)
      {
        return methodValueCache.getObjectsInt(fieldIntersection);
      }
    }
    // Not indexed. Get the intersection manually.
    Map<String, Method> mapMethodNameToMethods = new HashMap<>();
    final List<T> list = list(type);
    final List<T> toReturn = new ArrayList<>();
    // Store a reference in case an exception is thrown.
    String currentMethodName = null;
    try
    {
      List<String> methodNames = fieldIntersection.getMethodNames();
      List<Object> values = fieldIntersection.getValues();
      for (T object : list)
      {
        boolean matching = true;
        for (int i = 0; matching && i < methodNames.size(); i++)
        {
          String methodName = methodNames.get(i);
          currentMethodName = methodName;
          Object value = values.get(i);
          Method method = mapMethodNameToMethods.get(methodName);
          if (method == null)
          {
            method = object.getClass().getMethod(methodName, NO_PARAMETERS);
            mapMethodNameToMethods.put(methodName, method);
          }
          // Check the value of the field within this object.
          final Object objValue = method.invoke(object, NO_VALUES);

          if (value instanceof WhereInSet)
          {
            matching = matching && ((WhereInSet)value).hasValue(objValue);
          }
          else
          {
            matching = matching && Objects.equals(value, objValue);
          }
        }
        if (matching)
        {
          toReturn.add(object);
        }
      }
    }
    catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e)
    {
      throw new ControllerError(ERROR_METHOD_ACCESS + currentMethodName, e);
    }

    return toReturn;
  }

  /**
   * Simplified list convenience method.
   */
  public <T extends Identifiable> List<T> list(Class<T> type, 
      String methodName, Object[] args, Object value)
  {
    return list(type, methodName, NO_PARAMETERS, args, value);
  }

  /**
   * Simplified list convenience method.
   */
  public <T extends Identifiable> List<T> list(Class<T> type,
      String methodName, Class<?>[] paramTypes, Object[] args, Object value)
  {
    return list(type, methodName, paramTypes, args, value, false);
  }

  /**
   * Simplified list convenience method.
   */
  public <T extends Identifiable> List<T> list(Class<T> type, 
      String methodName, Object value, boolean loose)
  {
    return list(type, methodName, NO_PARAMETERS, NO_VALUES, value, loose);
  }
  
  /**
   * Return a collection of objects contained in the entity group based
   * on a method name value and object type.  Returns empty collection in the
   * event of an error or if no IdentifiableObjects cannot be found.
   *
   * @param methodName a method from which the return value is compared to the
   *        "value" parameter.
   * @param paramTypes the types of the parameters this method takes, 
   *        required if you are also providing an "args" parameter.
   * @param args method parameters passed to "methodName" method when invoked.
   * @param value the value for which to search.
   * @param loose if true, the value returned by the method will be matched 
   *        both literally and after a call to "toString"
   */
  public <T extends Identifiable> List<T> list(Class<T> type,
    String methodName, Class<?>[] paramTypes, Object[] args, Object value, 
    boolean loose)
  {
    List<T> toReturn = new ArrayList<>();

    List<T> objects = list(type);

    if (CollectionHelper.isNonEmpty(objects))
    {
      // Try to get the method.
      Method method;
      try
      {
        method = type.getDeclaredMethod(methodName, paramTypes);
      }
      catch (NoSuchMethodException exc)
      {
        try
        {
          method = type.getMethod(methodName, paramTypes);
        }
        catch (NoSuchMethodException etexc)
        {
          throw new ControllerError("No method found: " + methodName, etexc);
        }
      }
      
      // Continue if we have a method.
      if (method != null)
      {
        try
        {
          Iterator<T> iterator = objects.iterator();
          T object;

          while (iterator.hasNext())
          {
            object = iterator.next();

            // Check the value returned by the method.
            Object objValue = method.invoke(object, args);
            if (value.equals(objValue))
            {
              toReturn.add(object);
            }
            else if ((loose) && (value.equals(objValue.toString())))
            {
              toReturn.add(object);
            }
          }
        }
        catch (InvocationTargetException etexc)
        {
          throw new ControllerError("Unable to invoke method: " + methodName, etexc);
        }
        catch (SecurityException | IllegalAccessException e)
        {
          throw new ControllerError(ERROR_METHOD_ACCESS + methodName, e);
        }
      }
    }

    return toReturn;
  }

  /**
   * Return a particular IdentifiableObject contained in the entity group
   * based on ID and object type.  Returns null if no such entity group
   * is found.
   *
   * @param identifier identifier for the object
   */
  public <T extends Identifiable> T get(Class<T> type, long identifier)
  {
    return getGroupSafe(type).get(identifier);
  }
  
  /**
   * Return a particular IdentifiableObject contained in the entity group
   * based on ID and object type.  Returns null if no such entity group
   * is found.
   *
   * @param identifier A string representation of an integer identity for 
   * the object; must be a number.  If the parse fails, id -1 will be used
   * (which should return null).
   * 
   * @return The object of the given type and with the given identifier if 
   * found, <tt>null</tt> if the type and/or identifier was null or invalid 
   * or if the entity could not be found
   */
  public <T extends Identifiable> T get(Class<T> type, String identifier)
  {
    return get(type, NumberHelper.parseIntPermissive(identifier, -1));
  }

  /**
   * Finds the object in a given entity group with the highest identity.
   */
  public <T extends Identifiable> Identifiable getHighestIdentity(Class<T> type)
  {
    long highestID = getGroupSafe(type).highest();
    return get(type, highestID);
  }

  /**
   * Finds the object in a given entity group with the lowest identity.
   */
  public <T extends Identifiable> Identifiable getLowestIdentity(Class<T> type)
  {
    long lowestID = getGroupSafe(type).lowest();
    return get(type, lowestID);
  }

  /**
   * Return the set of IdentifiableObjects contained in a entity group sorted
   * by a field.
   *
   * @param unsorted The original unsorted collection of objects.
   * @param sortField the field or method by which to sort the
   *        IdentifiableObjects.
   * @param comparisonType the comparison type as specified by
   *        ReflectiveComparator.
   * @param ordering either ASCENDING or DESCENDING, as specified by
   *        ReflectiveComparator.
   * @param ignoreCase If comparing Strings, should case be ignored?
   *        Has no effect if comparing anything other than Strings.
   */
  public <T extends Identifiable> List<T> sortedList(Collection<T> unsorted, 
      String sortField, int comparisonType, int ordering, boolean ignoreCase)
  {
    if (unsorted != null)
    {
      final List<T> results = new ArrayList<>(unsorted);
      final ReflectiveComparator<T> comparator = new ReflectiveComparator<>(
          sortField, comparisonType, ordering);
      comparator.setIgnoreCase(ignoreCase);

      // Sort the results.
      Collections.sort(results, comparator);

      return results;
    }
    else
    {
      return Collections.emptyList();
    }
  }

  /**
   * Return the set of IdentifiableObjects contained in a entity group sorted
   * by a field.
   *
   * @param unsorted The original unsorted collection of objects.
   * @param sortField the field or method by which to sort the
   *        IdentifiableObjects.
   * @param comparisonType the comparison type as specified by
   *        ReflectiveComparator.
   * @param ordering either ASCENDING or DESCENDING, as specified by
   *        ReflectiveComparator.
   */
  public <T extends Identifiable> List<T> sortedList(Collection<T> unsorted,
      String sortField, int comparisonType, int ordering)
  {
    return sortedList(unsorted, sortField, comparisonType, ordering, false);
  }

  /**
   * Return the set of IdentifiableObjects contained in a entity group sorted
   * by a field.  Assumes an ASCENDING ordering.
   *
   * @param unsorted The original unsorted collection of objects.
   * @param sortField the field or method by which to sort the
   *        IdentifiableObjects.
   * @param comparisonType the comparison type as specified by
   *        ReflectiveComparator.
   */
  public <T extends Identifiable> List<T> sortedList(Collection<T> unsorted, 
      String sortField, int comparisonType)
  {
    return sortedList(unsorted, sortField, comparisonType,
      ReflectiveComparator.ASCENDING, false);
  }

  /**
   * Return the set of IdentifiableObjects contained in a entity group sorted
   * by a field.  Assumes an ASCENDING ordering.
   *
   * @param sortField the field or method by which to sort the
   *        IdentifiableObjects.
   * @param comparisonType the comparison type as specified by
   *        ReflectiveComparator.
   */
  public <T extends Identifiable> List<T> sortedList(Class<T> type, String sortField,
    int comparisonType)
  {
    return sortedList(list(type), sortField, comparisonType);
  }

  /**
   * Return the set of IdentifiableObjects contained in a entity group sorted
   * by a field.
   *
   * @param sortField the field or method by which to sort the
   *        IdentifiableObjects.
   * @param comparisonType the comparison type as specified by
   *        ReflectiveComparator.
   * @param ordering either ASCENDING or DESCENDING, as specified by
   *        ReflectiveComparator.
   * @param ignoreCase If comparing Strings, should case be ignored?
   *        Has no effect if comparing anything other than Strings.
   */
  public <T extends Identifiable> List<T> sortedList(Class<T> type, 
      String sortField, int comparisonType, int ordering, boolean ignoreCase)
  {
    return sortedList(list(type), sortField, comparisonType, ordering, 
        ignoreCase);
  }

  /**
   * Return the set of IdentifiableObjects contained in a entity group sorted
   * by a field.
   *
   * @param sortField the field or method by which to sort the
   *        IdentifiableObjects.
   * @param comparisonType the comparison type as specified by
   *        ReflectiveComparator.
   * @param ordering either ASCENDING or DESCENDING, as specified by
   *        ReflectiveComparator.
   */
  public <T extends Identifiable> List<T> sortedList(Class<T> type, String sortField,
    int comparisonType, int ordering)
  {
    return sortedList(list(type), sortField, comparisonType, ordering);
  }

  /**
   * Return a particular IdentifiableObject contained in the entity group 
   * based on a field value and object type.  Returns null in the event of an
   * error or if the entity group cannot be found.
   *
   * @param fieldName the field to match the value to
   * @param value the value on which to search
   */
  public <T extends Identifiable> T getByField(Class<T> type, String fieldName, Object value)
  {
    Field field = null;
    final List<T> list = list(type);

    try
    {
      for (T object : list)
      {
        // Cache a reference to the Field if not yet done so.
        if (field == null)
        {
          field = object.getClass().getDeclaredField(fieldName);
        }

        // Check the value of the field within this object.
        Object objValue = field.get(object);
        if (value.equals(objValue))
        {
          return object;
        }
      }
    }
    catch (NoSuchFieldException | SecurityException | IllegalAccessException e)
    {
      throw new ControllerError(ERROR_FIELD_ACCESS + fieldName, e);
    }

    // If we get here, return null.
    return null;
  }

  /**
   * Return a particular IdentifiableObject contained in the entity group 
   * based on a method name and object type.  Returns null in the event of an
   * error or if the entity group cannot be found.
   *
   * @param methodName the method to call
   * @param value the value on which to search
   */
  @SuppressWarnings("unchecked")
  public <T extends Identifiable> T get(Class<T> type, String methodName, Object value)
  {
    if (isIndexed(type, methodName))
    {
      MethodValueCache<T> methodValueCache = (MethodValueCache<T>)methodValueCaches.get(type);
      if (methodValueCache != null)
      {
        return methodValueCache.getObject(methodName, value);
      }
    }
    
    Method method = null;
    final List<T> list = list(type);

    try
    {
      for (T object : list)
      {
        // Cache a reference to the Method if not yet done so.
        if (method == null)
        {
          method = object.getClass().getMethod(methodName, NO_PARAMETERS);
        }

        // Check the value of the field within this object.
        final Object objValue = method.invoke(object, NO_VALUES);
        
        if (Objects.equals(value, objValue))
        {
          return object;
        }
      }
    }
    catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e)
    {
      throw new ControllerError(ERROR_METHOD_ACCESS + methodName, e);
    }

    // If we get here, return null.
    return null;
  }

  /**
   * Return a particular IdentifiableObject contained in the entity group
   * based on a set of method names and expected values.  Returns null in the
   * event of an error or if the entity group cannot be found.
   *
   * @param methodName the method to call
   * @param value the value on which to search
   * @param methodNameThenValuePairs must be pairs of method name then value
   *                                 objects. If the length of the objects
   *                                 passed in is not a multiple of two, an
   *                                 IllegalArgumentException will be thrown.
   */
  @SuppressWarnings("unchecked")
  public <T extends Identifiable> T get(Class<T> type,
                                        String methodName,
                                        Object value,
                                        Object... methodNameThenValuePairs)
  {
    return get(new FieldIntersection<>(type, methodName, value,
        methodNameThenValuePairs));
  }

  @SuppressWarnings("unchecked")
  <T extends Identifiable> T get(FieldIntersection<T> fieldIntersection)
  {
    Class<T> type = fieldIntersection.getType();
    if (isIndexedInt(fieldIntersection))
    {
      MethodValueCache<T> methodValueCache = (MethodValueCache<T>)this.methodValueCaches
          .get(type);
      if (methodValueCache != null)
      {
        return methodValueCache.getObjectInt(fieldIntersection);
      }
    }
    // Not indexed. Get the intersection manually.
    Map<String, Method> mapMethodNameToMethods = new HashMap<>();
    final List<T> list = list(type);
    // Store a reference in case an exception is thrown.
    String currentMethodName = null;

    try
    {
      List<String> methodNames = fieldIntersection.getMethodNames();
      List<Object> values = fieldIntersection.getValues();
      for (T object : list)
      {
        boolean matching = true;
        for (int i = 0; matching && i < methodNames.size(); i++)
        {
          String methodName = methodNames.get(i);
          currentMethodName = methodName;
          Object value = values.get(i);
          Method method = mapMethodNameToMethods.get(methodName);
          if (method == null)
          {
            method = object.getClass().getMethod(methodName, NO_PARAMETERS);
            mapMethodNameToMethods.put(methodName, method);
          }
          // Check the value of the field within this object.
          final Object objValue = method.invoke(object, NO_VALUES);
          if (value instanceof WhereInSet)
          {
            matching = matching && ((WhereInSet)value).hasValue(objValue);
          }
          else
          {
            matching = matching && Objects.equals(value, objValue);
          }
        }
        if (matching)
        {
          return object;
        }
      }
    }
    catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e)
    {
      throw new ControllerError(ERROR_METHOD_ACCESS + currentMethodName, e);
    }

    // If we get here, return null.
    return null;
  }

  /**
   * Refreshes a single object in the cache.
   * 
   * @param id the id of the object
   */
  public void refresh(Class<? extends Identifiable> type, long... ids)
  {
    getGroupSafe(type).refresh(ids);
    
    final MethodValueCache<?> methodValueCache = methodValueCaches.get(type);
    if (methodValueCache != null)
    {
      for (long id : ids) {
        methodValueCache.update(id);
      }
    }

    // Notify the listeners.
    final CacheListener[] toNotify = listeners;
    for (CacheListener listener : toNotify) {
      for (long id : ids) {
        listener.cacheObjectExpired(type, id);
      }
    }
  }
  
  /**
   * Puts a data entity into the database/data-store.  This will also cache
   * the entity if a cache is in use.  If the entity is new and is assigned
   * a new Identity during the put operation, the entity will be affected
   * directly.
   */
  @SuppressWarnings("unchecked")
  public <T extends Identifiable> void put(T entity)
  {
    if (entity == null)
    {
      throw new ControllerError("Cannot put null entity.");
    }
    
    getGroupSafe((Class<T>)entity.getClass()).put(entity);
    
    // Update method value caches.
    final MethodValueCache<?> methodValueCache = methodValueCaches.get(entity.getClass());
    if (methodValueCache != null)
    {
      methodValueCache.update(entity.getId());
    }
    
    // Notify the listeners.
    final CacheListener[] toNotify = listeners;
    for (CacheListener listener : toNotify)
    {
      listener.cacheObjectExpired(entity.getClass(), entity.getId());
    }
  }

  /**
   * Remove a data entity from the database/data-store.  This will also 
   * remove the entity from the cache if a cache is in use.
   */
  public <T extends Identifiable> void remove(Class<T> type, long id)
  {
    T entity = get(type, id);
    if (entity != null)
    {
      remove(entity);
    }
  }
  
  /**
   * Remove a data entity from the database/data-store.  This will also 
   * remove the entity from the cache if a cache is in use.
   */
  public <T extends Identifiable> void remove(T entity)
  {
    if (entity == null)
    {
      throw new ControllerError("Cannot remove null entity.");
    }

    getGroupSafe(entity.getClass()).remove(entity.getId());
    
    // Update relations.
    for (EntityRelation<?,?> relation : relations)
    {
      relation.removeEntity(entity);
    }
    
    // Update method value cache.
    final MethodValueCache<?> methodValueCache = methodValueCaches.get(entity.getClass());
    if (methodValueCache != null)
    {
      methodValueCache.delete(entity.getId());
    }
    
    // Notify the listeners.
    final CacheListener[] toNotify = listeners;
    for (CacheListener listener : toNotify)
    {
      listener.removeFromCache(entity.getClass(), entity.getId());
    }
  }

  //
  // Custom Caching
  //
  // The following methods allow for additional custom caching to be defined
  //

  /**
   * Registers the given relation with the cache and notifies the listeners.
   *
   * @param relation a relation between Identifiable objects
   */
  public <L extends Identifiable, R extends Identifiable, E extends EntityRelation<L,R>> 
      E register(EntityRelation.Builder<L, R, E> relation)
  {
    return register(relation.build(this), null);
  }
  
  /**
   * Registers the given relation with the cache and notifies the listeners. Once registered,
   * you will be able to get this relation from the cache via the provided Class.
   */
  public <L extends Identifiable, R extends Identifiable, 
      E extends EntityRelation<L,R>, D extends EntityRelationDescriptor<L,R>> 
      E register(EntityRelation.Builder<L,R,E> relation, Class<D> definition)
  {
    return register(relation.build(this), definition);
  } 

  /**
   * Registers the given relation with the cache and notifies the listeners.
   *
   * @param relation a relation between Identifiable objects
   */
  public <L extends Identifiable, R extends Identifiable, E extends 
      EntityRelation<L,R>> E register(E relation)
  {
    return register(relation, null);
  }
  
  /**
   * Registers the given relation with the cache and notifies the listeners. 
   * If definition is not null, then once registered, you will be able to get 
   * this relation from the cache via the provided definition Class. Useful if
   * you have two CachedRelations on the same two classes.
   * 
   * @param definition a Class that will be used later on to look up this
   * relation
   */
  @SuppressWarnings("unchecked")
  public <L extends Identifiable, R extends Identifiable, 
      E extends EntityRelation<L,R>, D extends EntityRelationDescriptor<L, R>>
      E register(E relation, Class<D> definition)
  {
    relations.add(relation);

    if (relation instanceof CachedRelation)
    {
      final CachedRelation<?, ?> cr = (CachedRelation<?, ?>) relation;
      cachedRelations.add(cr);
      // Give the relation a unique ID.
      cr.setId(cachedRelations.size());
      log.log("Registered " + cr + " with id " + cr.getId(),
          LogLevel.DEBUG);
    }

    // if we're provided with a definition Class, we need to add this relation
    // to our relationMap for quick lookup via the definition class
    if (definition != null)
    {
      relationsMap.put((Class<EntityRelationDescriptor<?,?>>)definition, 
          relation);
    }

    return relation;
  }
  
  /**
   * Registers all entities, cached entities and cached relations in a given 
   * package. Uses the annotations in com.techempower.data.annotation.
   */
  @SuppressWarnings("unchecked")
  public void register()
  {
    log.log("Registering annotated entities, relations, and type adapters.");
    try
    {
      final ExecutorService service = Executors.newFixedThreadPool(5);
      
      // @Entity-annoted classes.
      service.submit(new Runnable() {
        @Override
        public void run() {
          for (Class<?> clazz : reflections.getTypesAnnotatedWith(Entity.class)) 
          {
            // The annotation can contain some additional information on how to
            // build this EntityGroup.
            final Entity annotation = clazz.getAnnotation(Entity.class);
            final Builder<?> builder = EntityGroup.of(
                (Class<? extends Identifiable>)clazz);

            if (!annotation.table().isEmpty()) {
              builder.table(annotation.table());
            }
            if (!annotation.id().isEmpty()) {
              builder.id(annotation.id());
            }
            if (!annotation.comparator().isEmpty()) {
              builder.comparator(annotation.comparator());
            }

            // finally register the EntityGroup
            register(builder);
          }
        }
      });
      
      // @CachedEntity-annotated classes.
      service.submit(new Runnable() {
        @Override
        public void run() {
          for (Class<?> clazz : reflections.getTypesAnnotatedWith(CachedEntity.class))
          {
            // The annotation can contain some additional information on how to
            // build this CacheGroup.
            final CachedEntity annotation = clazz.getAnnotation(CachedEntity.class);
            final Builder<?> builder;
            if (annotation.lruSize() > 0)
            {
              final LruCacheGroup.Builder<?> lruBuilder = LruCacheGroup.of((Class<? extends Identifiable>)clazz);
              lruBuilder.maximumSize(annotation.lruSize());
              builder = lruBuilder;
            }
            else
            {
              builder = CacheGroup.of((Class<? extends Identifiable>)clazz);
            }
            
            if (!annotation.table().isEmpty())
            {
              builder.table(annotation.table());
            }
            if (!annotation.id().isEmpty())
            {
              builder.id(annotation.id());
            }
            if (!annotation.comparator().isEmpty())
            {
              builder.comparator(annotation.comparator());
            }
            
            // finally register the CacheGroup
            register(builder);
          }
        }
      });

      // @PureMemoryEntity-annotated classes.
      service.submit(new Runnable() {
        @Override
        public void run() {
          for (Class<?> clazz : reflections.getTypesAnnotatedWith(PureMemoryEntity.class))
          {
            register(PureMemoryGroup.of((Class<? extends Identifiable>)clazz));
          }
        }
      });

      // Relations.
      service.submit(new Runnable() {
        @Override
        public void run() {
          for (@SuppressWarnings("rawtypes") 
               Class<? extends EntityRelationDescriptor> clazz : 
              reflections.getSubTypesOf(EntityRelationDescriptor.class))
          {
            // We check for the existence of any @Relation annotations.
            if (clazz.isAnnotationPresent(Relation.class))
            {
              final Relation annotation = clazz.getAnnotation(Relation.class);
              // A class uses the @Relation annotation must specify 2 fields, one
              // with @Left and one with @Right. To help us determine which two 
              // Identifiable classes make up this relation.
              final Field[] fields = clazz.getDeclaredFields();
              Field left  = null;
              Field right = null;
              
              for (Field field : fields)
              {
                if (field.isAnnotationPresent(Left.class))
                {
                  left = field;
                }
                else if (field.isAnnotationPresent(Right.class))
                {
                  right = field;
                }
              }
              
              // We need to have both a left column and right column specified via
              // the @Left and @Right annotations.  TODO The classes defined by 
              // these annotations must also be of subclasses of Identifiable.
              if (left == null || right == null)
              {
                // If anything here fails I don't want to start up, I want to
                // be notified immediately.
                throw new RuntimeException(
                    "Cannot create CachedRelation from @Relation definition class without specifying @Left and @Right Identifiables.");
              }

              final Left leftAnnotation = left.getAnnotation(Left.class);
              final Right rightAnnotation = right.getAnnotation(Right.class);
              
              // We're ready to register this CachedRelation.  The table name will
              // be inferred from the class name.  The left and right column names
              // will use the name of the parameters.
              // Don't register it more than once.
              if (getRelation((Class<EntityRelationDescriptor<Identifiable, Identifiable>>)clazz) == null) {
                final CachedRelation.Builder<?, ?> builder = CachedRelation.of(
                        (Class<? extends Identifiable>) left.getType(),
                        (Class<? extends Identifiable>) right.getType());
                if (!annotation.table().isEmpty())
                {
                  builder.table(annotation.table());
                }
                if (!leftAnnotation.column().isEmpty())
                {
                  builder.leftColumn(leftAnnotation.column());
                }
                if (!rightAnnotation.column().isEmpty())
                {
                  builder.rightColumn(rightAnnotation.column());
                }

                // Finally register the Relation
                register(builder, clazz);
              }
            }
          }
        }
      });

      // Relations.
      service.submit(new Runnable() {
        @Override
        public void run() {
          // Finally, look for any TypeAdapter classes that are annotated
          for (Class<? extends TypeAdapter> clazz :
              reflections.getSubTypesOf(TypeAdapter.class))
          {
            // We check for the existence of any known annotations, including
            // @Entity, @Cached, @Relation
            if (clazz.isAnnotationPresent(EntityTypeAdapter.class))
            {
              try
              {
                register(clazz.getConstructor(NO_PARAMETERS).newInstance());
              }
              catch (InstantiationException
                  | IllegalAccessException
                  | NoSuchMethodException
                  | InvocationTargetException e)
              {
                throw new RuntimeException("Warn: Could not register TypeAdapter", e);
              }
            }
          }
        }
      });

      try
      {
        service.shutdown();
        service.awaitTermination(1L, TimeUnit.HOURS);
      }
      catch (InterruptedException iexc)
      {
        log.log("Unable to register all entities in 1 hour!", LogLevel.CRITICAL);
      }
    
      log.log("Done registering annotated items.");
    }
    catch (ReflectionsException e) 
    {
      throw new RuntimeException("Warn: problem registering class with reflection", e);
    } 
  }

  /**
   * Returns the relation based on the definition class. This method requires 
   * that you provided a definition class when registering this cache group.
   */
  @SuppressWarnings("unchecked")
  public <L extends Identifiable, R extends Identifiable> EntityRelation<L,R>
    getRelation(Class<? extends EntityRelationDescriptor<L, R>> definition)
  {
    return (EntityRelation<L,R>) relationsMap.get(definition);
  }
  
  /**
   * Returns the relation whose table name matches the given table name.
   * 
   * @param tableName The name of the table in the relation to be returned.
   * @return The relation whose table name matches the given table name.
   */
  public EntityRelation<? extends Identifiable, ? extends Identifiable> 
    getRelation(String tableName)
  {
    for (EntityRelation<?, ?> relation : relations)
    {
      if (relation.tableName().equals(tableName))
      {
        return relation;
      }
    }
    
    return null;
  }
  
  /**
   * Returns a copy of the list of all relations in the cache.  This is a
   * superset of the relations returned by {@link #getCachedRelations()}.
   */
  public List<EntityRelation<? extends Identifiable, ? extends Identifiable>> 
    getRelations()
  {
    return new ArrayList<>(relations);
  }
  
  /**
   * Returns a copy of the list of all cached relations in the cache.  
   * This is a subset of the relations returned by {@link #getRelations()}.
   */
  public List<CachedRelation<? extends Identifiable, ? extends Identifiable>>
    getCachedRelations()
  {
    return new ArrayList<>(cachedRelations);
  }
  
  /**
   * Returns the cached response text associated with the given parameter keys.
   * @param parameterKeys The key of expected cached response.
   * @return The cached response payload for the given parameterKeys or 
   *         <code>null</code> if there is no cached response for the given 
   *         request.
   */
  public String getCachedResponse(String parameterKeys)
  {
    return cachedResponses.get(parameterKeys);
  }
  
  /**   
   * Caches the response text for the parameters associated with the given
   * request.
   * @param parameterKeys The key for the given responseText string.
   * @param responseText The responseText to cache for the given request.
   */
  public void setCachedResponse(String parameterKeys, String responseText)
  {
    // Remove the previous.
    cachedResponses.remove(parameterKeys);
    // Try to put it in.
    cachedResponses.putIfAbsent(parameterKeys, responseText);
    // This is inherently NOT thread-safe, but in cases where this
    // comes up, we don't really care: some other thread put a response
    // value in at or around the same time we removed and tried to put.
    // If out put returns their value, then w/e. If it returns null, then
    // we can assume ours was there first.
  }
  
  /**
   * Clears all cached responses (in memory and on disk).
   */
  public void clearCachedResponses()
  {
    // Brand new one, let the old one get garbage collected.
    cachedResponses = new ConcurrentHashMap<>();
  }

  /**
   * Returns a list of objects with the given ids.  The objects are in the
   * order specified by the given ids.  The returned list will not include
   * nulls.
   */
  public <T extends Identifiable> List<T> list(Class<T> type, long... ids)
  {
    return list(type, CollectionHelper.toList(ids));
  }

  /**
   * Returns a list of objects with the given ids.  The objects are in the
   * order specified by the given ids.  The returned list will not include
   * nulls.
   */
  public <T extends Identifiable> List<T> list(Class<T> type, Collection<Long> ids)
  {
    return getGroupSafe(type).list(ids);
  }

  /**
   * Returns a map of objects with the given ids.
   */
  public <T extends Identifiable> TLongObjectMap<T> map(Class<T> type, long... ids)
  {
    return map(type, CollectionHelper.toList(ids));
  }

  /**
   * Returns a map of objects with the given ids.
   */
  public <T extends Identifiable> TLongObjectMap<T> map(Class<T> type, Collection<Long> ids)
  {
    return getGroupSafe(type).map(ids);
  }

  /**
   * Remove the given entities from the database (and cache if applicable).
   */
  public <T extends Identifiable> void removeAll(Class<T> type, long... ids)
  {
    removeAll(type, CollectionHelper.toList(ids));
  }

  /**
   * Remove the given entities from the database (and cache if applicable).
   */
  public <T extends Identifiable> void removeAll(Class<T> type, Collection<Long> ids)
  {
    getGroupSafe(type).removeAll(ids);
    
    // Update relations.
    for (EntityRelation<?,?> relation : relations)
    {
      for (long id : ids)
      {
        relation.removeEntity(type, id);
      }
    }
    
    // Update method value caches.
    MethodValueCache<?> methodValueCache = methodValueCaches.get(type);
    if (methodValueCache != null)
    {
      for (long id : ids)
      {
        methodValueCache.delete(id);
      }
    }
    
    // Notify the listeners.
    final CacheListener[] toNotify = listeners;
    for (CacheListener listener : toNotify)
    {
      for (long id : ids)
      {
        listener.removeFromCache(type, id);
      }
    }
  }

  /**
   * Put objects into the data store (and cache if applicable).  This will
   * always persist the objects to the data store; caching is only applicable
   * when using the CacheGroup subclass or something similar.
   * 
   * @param objects The objects to put ("put" means persist to disk and update
   * in cache if this is a cache group.)
   */
  @SafeVarargs
  public final <T extends Identifiable> void putAll(T... objects)
  {
    putAll(CollectionHelper.toList(objects));
  }

  /**
   * Put objects into the data store (and cache if applicable).  This will
   * always persist the objects to the data store; caching is only applicable
   * when using the CacheGroup subclass or something similar.
   * 
   * @param objects The objects to put ("put" means persist to disk and update
   * in cache if this is a cache group.)
   */
  @SuppressWarnings("unchecked")
  public <T extends Identifiable> void putAll(Collection<T> objects)
  {
    if (objects == null)
    {
      throw new ControllerError("Cannot putAll of a null collection.");
    }

    // Group the objects by type.
    Map<Class<T>, Collection<T>> map = new HashMap<>();
    for (T object : objects)
    {
      Collection<T> collection = map.get(object.getClass());
      if (collection == null)
      {
        collection = new ArrayList<>();
        map.put((Class<T>)object.getClass(), collection);
      }
      collection.add(object);
    }
    
    // For each type, update the cache.
    for (Map.Entry<Class<T>, Collection<T>> entry : map.entrySet())
    {
      Class<T> type = entry.getKey();
      Collection<T> collection = entry.getValue();
      
      // Update the group.
      getGroupSafe(type).putAll(collection);
      
      // Update method value caches.
      MethodValueCache<T> methodValueCache = 
          (MethodValueCache<T>)methodValueCaches.get(type);
      if (methodValueCache != null)
      {
        for (T object : collection)
        {
          methodValueCache.update(object.getId());
        }
      }
      
      // Notify the listeners.
      final CacheListener[] toNotify = listeners;
      for (CacheListener listener : toNotify)
      {
        for (T object : collection)
        {
          listener.cacheObjectExpired(object.getClass(), object.getId());
        }
      }
    }
  }

  /**
   * Remove the given entities from the database (and cache if applicable).
   */
  @SafeVarargs
  public final <T extends Identifiable> void removeAll(T... objects)
  {
    removeAll(CollectionHelper.toList(objects));
  }

  /**
   * Remove the given entities from the database (and cache if applicable).
   */
  @SuppressWarnings("unchecked")
  public <T extends Identifiable> void removeAll(Collection<T> objects)
  {
    if (objects == null)
    {
      throw new ControllerError("Cannot removeAll of a null collection.");
    }

    // Group the objects by type.
    Map<Class<T>, Collection<Long>> map =
        new HashMap<>();
    for (T object : objects)
    {
      Collection<Long> collection = map.get(object.getClass());
      if (collection == null)
      {
        collection = new ArrayList<>();
        map.put((Class<T>)object.getClass(), collection);
      }
      collection.add(object.getId());
    }
    
    // For each type, update the cache.
    for (Map.Entry<Class<T>, Collection<Long>> entry : map.entrySet())
    {
      Class<T> type = entry.getKey();
      Collection<Long> collection = entry.getValue();
      
      // Update the group.
      getGroupSafe(type).removeAll(collection);
      
      // Update relations.
      for (EntityRelation<?,?> relation : relations)
      {
        for (long id : collection)
        {
          relation.removeEntity(type, id);
        }
      }
      
      // Update method value caches.
      MethodValueCache<T> methodValueCache = 
          (MethodValueCache<T>)methodValueCaches.get(type);
      if (methodValueCache != null)
      {
        for (long id : collection)
        {
          methodValueCache.delete(id);
        }
      }

      // Notify the listeners.
      final CacheListener[] toNotify = listeners;
      for (CacheListener listener : toNotify) {
        for (long id : collection) {
          listener.removeFromCache(type, id);
        }
      }
    }
  }

  /**
   * Registers the given type adapter with this controller.  The adapter will be
   * used to translate all entity fields of one type to another type when
   * persisting entities to the database.  This is useful when you would like to
   * persist custom or non-standard fields to the database.  For example:
   * <pre>
   * // Allows all data entities to have DateMidnight (from Joda Time) fields
   * // that get persisted like java.util.Date in the database.
   * register(
   *     new TypeAdapter&lt;DateMidnight, Date&gt;() {
   *       public Date write(DateMidnight value)
   *       {
   *         return (value == null)
   *             ? null
   *             : value.toDate();
   *       }
   *       public DateMidnight read(Date value)
   *       {
   *         return (value == null)
   *             ? null
   *             : new DateMidnight(value);
   *       }
   *     });
   * </pre>
   *
   * @param <F> The adapter converts values <em>from</em> this type.
   * @param <T> The adapter converts values <em>to</em> this type.
   */
  public <F, T> void register(TypeAdapter<F, T> adapter)
  {
    if (adapter == null)
    {
      throw new ControllerError("Cannot register a null type adapter.");
    }
    typeAdapters.add(adapter);
  }

  /**
   * Returns an unmodifiable view of the type adapters registered with this
   * controller.  This method is intended for use in {@link EntityGroup} only.
   */
  public List<TypeAdapter<?, ?>> getTypeAdapters()
  {
    return Collections.unmodifiableList(typeAdapters);
  }

  @Override
  public String toString()
  {
    return "EntityStore " + (initialized
        ? "[" + groups.size() + " group" + StringHelper.pluralize(groups.size())
        + "; " + listeners.length + " listener" + StringHelper.pluralize(listeners.length)
        + "]"
        : "[Not yet initialized]"
        );
  }

}  // End EntityStore.
