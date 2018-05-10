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
package com.techempower.data;

import gnu.trove.map.*;
import gnu.trove.map.hash.*;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;

import com.esotericsoftware.reflectasm.*;
import com.techempower.cache.*;
import com.techempower.collection.*;
import com.techempower.data.mapping.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.reflect.*;
import com.techempower.util.*;

/**
 * Manages interactions with a SQL database to store and retrieve entities of a
 * given type.
 * <p>
 * Example usage in EntityStore#initialize():
 * <pre>
 * 
 *   public void initialize()
 *   {
 *     // 
 *     // Foo objects are stored in a table named "foo" with id column "id".
 *     // They are sorted by id and don't write to a log.
 *     // 
 *     register(EntityGroup.of(Foo.class));
 *     // 
 *     // Bar objects are stored in a table named "bars" with id column "barid".
 *     // They are sorted by name and don't write to a log.
 *     // 
 *     register(EntityGroup.of(Bar.class)
 *         .table("bars")
 *         .id("barid")
 *         .comparator(new Comparator&lt;Bar&gt;() {
 *           public int compare(Bar o1, Bar o2)
 *           {
 *             return ObjectHelper.compare(o1.getName(), o2.getName());
 *           }
 *         }));
 *     // 
 *     // Baz objects are stored in a table named "the_baz" with id column "id".
 *     // They are sorted by id.  They don't have a zero-argument constructor so
 *     // a factory method is supplied.
 *     // 
 *     register(EntityGroup.of(Baz.class)
 *         .table("the_baz")
 *         .maker(new EntityMaker&lt;Baz&gt;() {
 *           public Baz make()
 *           {
 *             return new Baz(System.currentTimeMillis());
 *           }
 *         });
 *   }
 * </pre>
 * @param <T> The type of entities managed by this object.
 */
public class EntityGroup<T extends Identifiable>
{

  private static final Class<?>[] NO_PARAMETERS  = new Class[0];
  private static final Object[]   NO_VALUES      = new Object[0];

  // 
  // Constants.
  // 

  /**
   * Compares entities by id.  This is the default comparator used for sorting
   * objects if no other is provided.
   */
  private static final Comparator<Identifiable> ID_COMPARATOR =
      new Comparator<Identifiable>() {
        @Override
        public int compare(Identifiable o1, Identifiable o2)
        {
          return Long.compare(o1.getId(), o2.getId());
        }
      };
      
  /**
   * Compare entities that implement Comparable using their built-in compareTo
   * method.  This should only be needed until Java 1.8 gives us 
   * Comparator.naturalOrder().
   */
  enum NaturalOrderComparator implements Comparator<Comparable<Object>> {
        INSTANCE;
   
        @Override
        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
          return c1.compareTo(c2);
        }
      }
  
  /**
   * Gets a natural-ordering Comparator implementation for a Comparable.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
      return (Comparator<T>) EntityGroup.NaturalOrderComparator.INSTANCE;
  } 
  
  /**
   * Gets a suitable default Comparator for the group, using the natural order
   * if the type is Comparable, and the IDs if not.
   */
  @SuppressWarnings("unchecked")
  public static <C extends Identifiable> Comparator<? super C> defaultComparator(Class<C> type)
  {
    if (Comparable.class.isAssignableFrom(type))
    {
      return (Comparator<C>)naturalOrder();
    }
    else
    {
      return ID_COMPARATOR;
    }
  }

  /**
   * This is used to indicate that a field does not have a custom type adapter.
   */
  private static final TypeAdapter<Object, Object> NO_ADAPTER
      = new TypeAdapter<Object, Object>() {
          @Override
          public Object write(Object value)
          {
            throw new UnsupportedOperationException();
          }
          @Override
          public Object read(Object value)
          {
            throw new UnsupportedOperationException();
          }
        };

  // 
  // Protected fields.
  // 

  private final EntityStore entityStore;
  private final ConnectorFactory cf;
  private final Class<T> type;
  private final String table;
  private final String id;
  private final String where;
  private final String[] whereArguments;
  private final EntityMaker<T> maker;
  private final Comparator<? super T> comparator;
  private final MethodAccess access;
  private final ComponentLog log;
  private final String quotedTable;
  private final String quotedIdField;
  private final String getSingleQuery;
  private final String deleteSingleQuery;
  private final boolean readOnly;
  
  private DataFieldToMethodMap[] setMethods = null;
  private DataFieldToMethodMap[] getMethods = null;
  private DataFieldToMethodMap[] getMethodsWithoutId = null;
  private String fieldPartsForUpdate = null;
  
  /**
   * This maps fields to type adapters.  If a field does not exist as a key in
   * this map, it is unknown whether it has a type adapter.  If a field is in
   * this map and its value is {@link #NO_ADAPTER}, this the field does not have
   * a type adapter.
   */
  private final Map<String, TypeAdapter<?, ?>> typeAdaptersByFieldName
      = new ConcurrentHashMap<>();
  
  /**
   * A unique identifier for this cache group to be assigned by the entity
   * store as the group is registered.
   */
  private int groupNumber;

  // 
  // Protected constructor.
  // 

  /**
   * Returns a new entity group of the given type.  This constructor is
   * non-public because users should only instantiate this class by way of a
   * {@link Builder}, which can be obtained from a call to
   * {@link EntityGroup#of(Class)}.
   * 
   * @param entityStore The EntityStore that manages this group.
   * @param type The type of the entities.
   * @param table The name of the database table that stores the entities.
   * @param id The name of the database column that holds the identities of the
   *           entities.
   * @param maker The generator to use when creating entities of this type.
   * @param comparator The comparator to use when sorting entities of this type.
   * @param where An optional WHERE clause (not including the "WHERE" keyword)
   *              in PreparedStatement form.
   * @param whereArguments The arguments to insert into the WHERE clause.
   */
  @SuppressWarnings("unchecked")
  protected EntityGroup(EntityStore entityStore, 
      Class<T> type, 
      String table, 
      String id, 
      EntityMaker<T> maker, 
      Comparator<? super T> comparator,
      String where, 
      String[] whereArguments,
      boolean readOnly)
  {
    Objects.requireNonNull(entityStore, "EntityStore cannot be null.");
    
    // 
    // Required fields.
    // 

    this.type = type;
    this.access = MethodAccess.get(this.type);
    this.entityStore = entityStore;
    this.cf = entityStore.getConnectorFactory();
    this.readOnly = readOnly;

    // 
    // Optional fields.
    // 

    this.maker = (maker == null)
        ? new EntityMaker<T>() {
              @Override
              public T make() {
                try
                {
                  return EntityGroup.this.type.getConstructor(NO_PARAMETERS)
                      .newInstance();
                }
                catch (InstantiationException 
                    | IllegalAccessException 
                    | NoSuchMethodException 
                    | InvocationTargetException e) 
                { 
                  // Do nothing.  We'll be returning null.
                }
                return null;
              }
          }
        : maker;
    this.table = (table == null)
        ? type.getSimpleName().toLowerCase()
        : table;
    this.id = (id == null)
        ? "id"
        : id;
    this.where = where;
    this.whereArguments = (whereArguments != null ? whereArguments.clone() : null);
    this.comparator = (Comparator<? super T>)((comparator == null)
        ? defaultComparator(type)
        : comparator);
    
    //
    // SQL Queries.
    //
    
    this.quotedIdField = enquote(this.id);
    this.quotedTable = enquote(this.table);
    this.getSingleQuery = "SELECT * FROM " + quotedTable
        + " WHERE " + quotedIdField + " = ?";
    this.deleteSingleQuery = "DELETE FROM " + quotedTable
        + " WHERE " + quotedIdField + " = ?";

    // 
    // Internal use only.
    // 
    
    this.log = entityStore.getEntityLog(); 
  }

  // 
  // Field getter methods.
  // 

  /**
   * Returns the type of the entities.
   */
  public Class<T> type()
  {
    return this.type;
  }
  
  /**
   * Returns the simple name of the type of the entities.
   */
  public String name()
  {
    return this.type.getSimpleName();
  }

  /**
   * Returns the name of the database table that stores the entities.
   */
  public String table()
  {
    return this.table;
  }
  
  /**
   * Returns the read-only state of the group.
   */
  public boolean readOnly()
  {
    return this.readOnly;
  }

  /**
   * Returns the name of the database column that holds the identities of the
   * entities.
   */
  public String id()
  {
    return this.id;
  }

  /**
   * Returns the generator to use when creating entities of this type.
   */
  public EntityMaker<T> maker()
  {
    return this.maker;
  }

  /**
   * Resets this group of entities.  In the base class, this doesn't do
   * anything, but subclasses such as CacheGroup act differently.
   */
  public void reset()
  {
    // Does nothing here.
  }

  /**
   * Returns the comparator to use when sorting entities of this type.
   */
  public Comparator<? super T> comparator()
  {
    return this.comparator;
  }

  /**
   * Gets the unique group number assigned by the entity store.
   * 
   * @return the groupNumber
   */
  public int getGroupNumber()
  {
    return this.groupNumber;
  }

  /**
   * Allows the entity store to set the unique group number.
   * 
   * @param groupNumber the groupNumber to set
   */
  public void setGroupNumber(int groupNumber)
  {
    this.groupNumber = groupNumber;
  }

  /**
   * Gets a reference to the type (Class) managed by this group.
   */
  public Class<T> getType()
  {
    return this.type;
  }

  // 
  // Database operations.
  // 

  /**
   * Returns the object with the given id, or null if there is no such object.
   */
  public T get(long idToGet)
  {
    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            this.getSingleQuery + getWhereClause(" AND ") + ";",
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY)
        )
    {
      statement.setLong(1, idToGet);
      attachWhereArguments(2, statement);
      try (ResultSet resultSet = statement.executeQuery())
      {
        if (resultSet.next())
        {
          return make(resultSet);
        }
      }
    }
    catch (Exception e)
    {
      throw new EntityException("Exception during SELECT.", e);
    }
    return null;
  }

  /**
   * Put an object into the data store (and cache if applicable).  This will
   * always persist the object to the data store; caching is only applicable
   * when using the CacheGroup subclass or something similar.  Sets the
   * initialized flag on the entity as a precaution, in case the calling
   * code did not do so.
   * 
   * @param object The object to put ("put" means persist to disk and update in 
   * cache if this is a cache group.)
   */
  public void put(T object)
  {
    if (readOnly)
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"put\" method is not permitted.");
    }
    
    // Call the initialize method to set the initialized flag as a precaution
    // if the calling code did not do so.
    if (object instanceof Initializable)
    {
      final Initializable initializable = (Initializable)object;
      if (!initializable.isInitialized())
      {
        initializable.initialize();
      }
    }
    
    if (isPersisted(object))
    {
      update(object);
    }
    else
    {
      insert(object);
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
  public final void putAll(T... objects)
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
  public void putAll(Collection<T> objects)
  {
    if (readOnly)
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"putAll\" method is not permitted.");
    }

    if (  (objects == null)
       || (objects.isEmpty())
       )
    {
      return;
    }
    
    // If the size of the collection is fewer than 100, just call put()
    // in a loop.  In testing, we've not observed a benefit to using batch
    // updates (as provided by updateAll).
    if (objects.size() < 100)
    {
      for (T object : objects)
      {
        put(object);
      }
    }
    else
    {
      List<T> persisted = null;
      List<T> nonPersisted = null;
      for (T object : objects)
      {
        if (isPersisted(object))
        {
          if (persisted == null)
          {
            persisted = new ArrayList<>(objects.size());
          }
          persisted.add(object);
        }
        else
        {
          if (nonPersisted == null)
          {
            nonPersisted = new ArrayList<>(objects.size());
          }
          nonPersisted.add(object);
        }
      }
      
      updateAll(persisted);
      insertAll(nonPersisted);
    }
  }

  /**
   * Remove an entity from the database (and cache if applicable).
   */
  public void remove(long idToRemove)
  {
    if (readOnly)
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"remove\" method is not permitted.");
    }

    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            this.deleteSingleQuery + getWhereClause(" AND ") + ";")
        )
    {
      statement.setLong(1, idToRemove);
      attachWhereArguments(2, statement);
      //this.log.log(statement.toString(), LogLevel.DEBUG);
      statement.executeUpdate();
    }
    catch (Exception e)
    {
      throw new EntityException("Exception during DELETE.", e);
    }
  }

  /**
   * Remove the given entities from the database (and cache if applicable).
   */
  public void removeAll(Collection<Long> ids)
  {
    if (readOnly)
    {
      throw new EntityException("EntityGroup for " + name() 
          + " is read-only. The \"removeAll\" method is not permitted.");
    }

    if (ids.isEmpty())
    {
      return;
    }
    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            "DELETE FROM " + quotedTable
                + " WHERE " + quotedIdField + " IN ("
                + StringHelper.join(",", Collections.nCopies(ids.size(), "?"))
                + ")" + getWhereClause(" AND ") + ";")
        )
    {
      int i = 0;
      for (long longToDelete : ids)
      {
        statement.setLong(++i, longToDelete);
      }
      attachWhereArguments(ids.size() + 1, statement);
      //this.log.log(statement.toString(), LogLevel.DEBUG);
      statement.executeUpdate();
    }
    catch (Exception e)
    {
      throw new EntityException("Exception during DELETE (removeAll).", e);
    }
  }

  /**
   * Returns the current size of the entity group.
   */
  public int size()
  {
    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            "SELECT COUNT(*) FROM " + quotedTable 
                + getWhereClause(" WHERE ")
                + ";",
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY)
        )
    {
      attachWhereArguments(1, statement);
      //this.log.log(statement.toString(), LogLevel.DEBUG);
      try (ResultSet resultSet = statement.executeQuery())
      {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
    catch (Exception e)
    {
      throw new EntityException("Exception during SELECT (size).", e);
    }
  }

  /**
   * Returns a sorted list of all objects in the database (or cache if 
   * applicable).
   */
  public List<T> list()
  {
    final List<T> objects = new ArrayList<>();
    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            "SELECT * FROM " + quotedTable 
                + getWhereClause(" WHERE ")
                + ";",
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY)
        )
    {
      attachWhereArguments(1, statement);
      //this.log.log(statement.toString(), LogLevel.DEBUG);
      try (ResultSet resultSet = statement.executeQuery())
      {
        while (resultSet.next())
        {
          T object = make(resultSet);
          objects.add(object);
        }
      }
    }
    catch (Exception e)
    {
      throw new EntityException("Exception during SELECT (list).", e);
    }
    Collections.sort(objects, this.comparator);
    return objects;
  }

  /**
   * Returns a list of objects with the given ids.  The objects are in the
   * order specified by the given ids.  The returned list will not include
   * nulls.
   */
  public List<T> list(Collection<Long> ids)
  {
    final TLongObjectMap<T> map = map(ids);
    final List<T> list = new ArrayList<>(ids.size());
    for (long idToList : ids)
    {
      final T object = map.get(idToList);
      if (object != null)
      {
        list.add(object);
      }
    }
    return list;
  }

  /**
   * Returns a map of all objects in the database (or cache if applicable),
   * mapped by id.
   */
  public TLongObjectMap<T> map()
  {
    final TLongObjectMap<T> objects = new TLongObjectHashMap<>();
    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            "SELECT * FROM " + quotedTable
                + getWhereClause(" WHERE ")
                + ";",
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY)
        )
    {
      attachWhereArguments(1, statement);
      //this.log.log(statement.toString(), LogLevel.DEBUG);
      try (ResultSet resultSet = statement.executeQuery())
      {
        while (resultSet.next())
        {
          T object = make(resultSet);
          objects.put(object.getId(), object);
        }
      }
    }
    catch (Exception e)
    {
      throw new EntityException("Exception during SELECT (map).", e);
    }
    return objects;
  }

  /**
   * Returns a map of objects with the given ids.
   */
  public TLongObjectMap<T> map(Collection<Long> ids)
  {
    if (ids.isEmpty())
    {
      return new TLongObjectHashMap<>(0);
    }
    
    final TLongObjectMap<T> objects = new TLongObjectHashMap<>(ids.size());

    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            "SELECT * FROM " + quotedTable
                + " WHERE " + quotedIdField + " IN ("
                + StringHelper.join(",", Collections.nCopies(ids.size(), "?"))
                + ")" + getWhereClause(" AND ") + ";",
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY)
        )
    {
      int i = 0;
      for (long idToSet : ids)
      {
        statement.setLong(++i, idToSet);
      }
      attachWhereArguments(ids.size() + 1, statement);
      //this.log.log(statement.toString(), LogLevel.DEBUG);
      try (ResultSet resultSet = statement.executeQuery())
      {
        while (resultSet.next())
        {
          T object = make(resultSet);
          objects.put(object.getId(), object);
        }
      }
    }
    catch (Exception e)
    {
      throw new EntityException("Exception during SELECT (map).", e);
    }
    return objects;
  }
  
  /**
   * Returns the lowest identity assigned to an entity.  Returns 0 if no
   * result can be computed.
   */
  public long lowest()
  {
    return identityAggregate("MIN");
  }

  /**
   * Returns the highest identity assigned to an entity.  Returns 0 if no
   * result can be computed.
   */
  public long highest()
  {
    return identityAggregate("MAX");
  }

  /**
   * Runs an arbitrary SQL query that <b>must</b> return a resultset that is
   * exactly comparable to the standard resultsets used by the list() method,
   * including the order of the columns.  ResultSet indexes rather than field
   * names are used to deserialize results.
   *   <p>
   * The results are captured into a List and returned.  Does not use the 
   * usual comparator to sort the results.
   *   <p>
   * Use this method at your own risk since its usage is considered non-
   * standard.
   * 
   * @param query Any old SQL query.  Can use "?" marks in place of values.
   * @param arguments The values to substitute for the "?" marks in the query.
   * @return A list of entities, hopefully.
   * @throws SQLException If you messed up.
   */
  public List<T> query(String query, Object... arguments) throws SQLException
  {
    final List<T> objects = new ArrayList<>();
    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(query)
        )
    {
      attachArguments(statement, arguments);
      try (ResultSet resultSet = statement.executeQuery())
      {
        while (resultSet.next())
        {
          T object = make(resultSet);
          objects.add(object);
        }
      }
    }
    return objects;
  }

  /**
   * Runs an arbitrary SQL query that <b>must</b> return a resultset that is
   * exactly comparable to the standard resultsets used by the list() method,
   * including the order of the columns.  ResultSet indexes rather than field
   * names are used to deserialize results.
   *   <p>
   * The first result is captured and returned, assuming a result is present
   * at all.
   *   <p>
   * Use this method at your own risk since its usage is considered non-
   * standard.
   * 
   * @param query Any old SQL query.  Can use "?" marks in place of values.
   * @param arguments The values to substitute for the "?" marks in the query.
   * @return A single entity, assuming a compatible resultset with at least
   *         one row is returned by the query.
   * @throws SQLException If you messed up.
   */
  public T querySingle(String query, Object... arguments) throws SQLException
  {
    T object = null;
    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(query)
        )
    {
      attachArguments(statement, arguments);
      try (ResultSet resultSet = statement.executeQuery())
      {
        if (resultSet.next())
        {
          object = make(resultSet);
        }
      }
    }
    return object;
  }
  
  /**
   * Attach an arbitrary list of arguments to a PreparedStatement.
   */
  private void attachArguments(PreparedStatement statement, Object... arguments)
    throws SQLException
  {
    int index = 1;
    for (Object argument : arguments)
    {
      if (argument instanceof Date)
      {
        statement.setDate(index++, new java.sql.Date(((Date)argument).getTime()));
      }
      else
      {
        statement.setObject(index++, argument);
      }
    }
  }

  /**
   * Called by put(object) to insert the object into the database.  If its 
   * identity is zero, it is assumed the database will generate and return an
   * auto-incremented id.  If its identity is greater than zero, the object 
   * will be inserted with that id.
   */
  protected void insert(T object)
  {
    // Include the ID field if it has been specified already by the object.
    final DataFieldToMethodMap[] fields = (object.getId() > 0)
        ? getGetMethodMappingCache()
        : getGetMethodMappingCacheWithoutId();
    
    final StringList fieldsPart = new StringList(", ");
    for (DataFieldToMethodMap field : fields)
    {
      fieldsPart.add(enquote(field.getFieldName()));
    }

    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
           "INSERT INTO " + quotedTable + " ("
               + fieldsPart.toString() + ") VALUES ("
               + StringHelper.join(", ", Collections.nCopies(fields.length, "?"))
               + ");",
               Statement.RETURN_GENERATED_KEYS)
        )
    {
      int index = 1;
      for (DataFieldToMethodMap field : fields)
      {
        final Object value = readValueForUpdate(object, field);
        applyValueToStatement(field, value, statement, index++);
      }
      //this.log.log(statement.toString(), LogLevel.DEBUG);
      statement.executeUpdate();
      
      // If the entity is persistence aware, let's inform it that it has been
      // persisted.
      if (object instanceof PersistenceAware)
      {
        ((PersistenceAware)object).setPersisted(true);
      }
      
      if (object.getId() <= 0)
      {
        // Gather the new identity from the Statement.
        try (ResultSet resultSet = statement.getGeneratedKeys())
        {
          if (resultSet.next())
          {
            object.setId(resultSet.getLong(1));
          }
          else
          {
            throw new EntityException("Identity not returned from INSERT.");
          }
        }
      }
    }
    catch (SQLException e)
    {
      throw new EntityException("Exception during INSERT.", e);
    }
  }

  /**
   * Called by putAll(objects) to insert the objects into the database.  If a
   * given object's identity is zero, it is assumed the database will generate
   * and return an auto-incremented id.  If its identity is greater than zero,
   * the object will be inserted with that id.
   */
  protected void insertAll(Collection<T> objects)
  {
    if (  (objects == null)
       || (objects.isEmpty())
       )
    {
      return;
    }
    
    // First, subdivide this into objects with id and those without.  Different
    // logic will be used to insert each.
    List<T> objectsWithId = new ArrayList<>(objects.size());
    List<T> objectsWithoutId = new ArrayList<>(objects.size());
    for (T object : objects)
    {
      if (object.getId() > 0)
      {
        objectsWithId.add(object);
      }
      else
      {
        objectsWithoutId.add(object);
      }
    }
    
    DataFieldToMethodMap[] cache = getGetMethodMappingCache();
    
    // Find the list of fields to be included in the update.  The id will be
    // included if it's greater than zero.
    List<DataFieldToMethodMap> fieldsWithId = new ArrayList<>();
    List<DataFieldToMethodMap> fieldsWithoutId = new ArrayList<>();
    for (DataFieldToMethodMap field : cache)
    {
      fieldsWithId.add(field);
      if (!field.getFieldName().equalsIgnoreCase(this.id))
      {
        fieldsWithoutId.add(field);
      }
    }
    StringList fieldsPartWithId = new StringList(", ");
    for (DataFieldToMethodMap field : fieldsWithId)
    {
      fieldsPartWithId.add(enquote(field.getFieldName()));
    }
    StringList fieldsPartWithoutId = new StringList(", ");
    for (DataFieldToMethodMap field : fieldsWithoutId)
    {
      fieldsPartWithoutId.add(enquote(field.getFieldName()));
    }

    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
    {
      try (PreparedStatement statementWithId = monitor.getConnection().prepareStatement(
          "INSERT INTO " + quotedTable + " ("
              + fieldsPartWithId.toString() + ") VALUES ("
              + StringHelper.join(", ", Collections.nCopies(fieldsWithId.size(), "?"))
              + ");"))
      {
        try (PreparedStatement statementWithoutId = monitor.getConnection().prepareStatement(
            "INSERT INTO " + quotedTable + " ("
                + fieldsPartWithoutId.toString() + ") VALUES ("
                + StringHelper.join(", ", Collections.nCopies(fieldsWithoutId.size(), "?"))
                + ");",
            Statement.RETURN_GENERATED_KEYS))
        {
          for (T object : objectsWithId)
          {
            int index = 1;
            for (DataFieldToMethodMap field : fieldsWithId)
            {
              Object value = readValueForUpdate(object, field);
              applyValueToStatement(
                  field,
                  value,
                  statementWithId,
                  index++);
            }
            statementWithId.addBatch();
          }
          
          for (T object : objectsWithoutId)
          {
            int index = 1;
            for (DataFieldToMethodMap field : fieldsWithoutId)
            {
              Object value = readValueForUpdate(object, field);
              applyValueToStatement(
                  field,
                  value,
                  statementWithoutId,
                  index++);
            }
            statementWithoutId.addBatch();
          }
          
          if (!objectsWithId.isEmpty())
          {
            //this.log.log(statementWithId.toString(), LogLevel.DEBUG);
            statementWithId.executeBatch();
          }
          
          if (!objectsWithoutId.isEmpty())
          {
            //this.log.log(statementWithoutId.toString(), LogLevel.DEBUG);
            statementWithoutId.executeBatch();
            
            // Gather the new ids from the Statement.
            try (ResultSet resultSet = statementWithoutId.getGeneratedKeys())
            {
              int i = 0;
              while (resultSet.next())
              {
                long identity = resultSet.getLong(1);
                objectsWithoutId.get(i++).setId(identity);
              }
              if (i != objectsWithoutId.size())
              {
                throw new EntityException("One or more identities not returned after INSERT.");
              }
            }
          }
          
          for (T object : objects)
          {
            // If the entity is persistence aware, let's inform it that it has been
            // persisted.
            if (object instanceof PersistenceAware)
            {
              ((PersistenceAware)object).setPersisted(true);
            }
          }
        }
      }
    }
    catch (SQLException e)
    {
      throw new EntityException("Exception during INSERT.", e);
    }
  }

  /**
   * Called by put(object) to update the object in the database and returns 
   * its id.
   */
  protected void update(T object)
  {
    // Include every field in the update except the id.
    final DataFieldToMethodMap[] fields = getGetMethodMappingCacheWithoutId();
    final String fieldParts = getFieldPartsForUpdate();

    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        final PreparedStatement statement = monitor.getConnection().prepareStatement(
            "UPDATE " + quotedTable + " SET " + fieldParts
                + " WHERE " + quotedIdField + " = ?"
                + getWhereClause(" AND ") + ";")
        )
    {
      statement.setLong(fields.length + 1, object.getId());
      attachWhereArguments(fields.length + 2, statement);
      int index = 1;
      for (DataFieldToMethodMap field : fields)
      {
        final Object value = readValueForUpdate(object, field);
        applyValueToStatement(field, value, statement, index++);
      }
      //this.log.log(statement.toString(), LogLevel.DEBUG);
      statement.executeUpdate();
    }
    catch (SQLException e)
    {
      throw new EntityException("Exception during UPDATE.", e);
    }
  }

  /**
   * Called by put(objects) to update the objects in the database.
   */
  protected void updateAll(Collection<T> objects)
  {
    if (  (objects == null) 
       || (objects.isEmpty())
       )
    {
      return;
    }
    
    final DataFieldToMethodMap[] fields = getGetMethodMappingCacheWithoutId();
    final String fieldParts = getFieldPartsForUpdate();

    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            "UPDATE " + quotedTable + " SET " + fieldParts
                + " WHERE " + quotedIdField + " = ?"
                + getWhereClause(" AND ") + ";")
        )
    {
      for (T object : objects)
      {
        statement.setLong(fields.length + 1, object.getId());
        attachWhereArguments(fields.length + 2, statement);
        int index = 1;
        for (DataFieldToMethodMap field : fields)
        {
          final Object value = readValueForUpdate(object, field);
          applyValueToStatement(field, value, statement, index++);
        }
        statement.addBatch();
      }
      statement.executeBatch();
    }
    catch (SQLException e)
    {
      throw new EntityException("Exception during UPDATE.", e);
    }
  }

  /**
   * Runs a simple SQL aggregate function on the identity column.  Returns 0 
   * if no result can be computed.
   */
  protected long identityAggregate(String sqlAggregateFunction)
  {
    long result = 0;
    try (
        ConnectionMonitor monitor = this.cf.getConnectionMonitor();
        PreparedStatement statement = monitor.getConnection().prepareStatement(
            "SELECT " + sqlAggregateFunction + "(" + quotedIdField + ") " +
               "AS Result FROM " + quotedTable + ";",
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY)
        )
    {
      try (ResultSet resultSet = statement.executeQuery())
      {
        if (resultSet.next())
        {
          result = resultSet.getLong(1);
        }
      }
    }
    catch (SQLException e)
    {
      throw new EntityException("Exception during identity aggregate.", e);
    }
    return result;  
  }

  /**
   * Reorder entities within this group.  In the base class, this doesn't
   * do anything, but subclasses such as CacheGroup act differently.
   * 
   * @param ids the ids of the objects
   */
  public void reorder(long... ids)
  {
    // Does nothing here.
  }

  /**
   * Refresh the in-memory cache state of an entity, if applicable (such as 
   * within CacheGroup).  Ignored by the base class.
   * 
   * @param ids the ids of the objects
   */
  public void refresh(long... ids)
  {
    // Does nothing here.
  }

  /**
   * Gets the WHERE clause if it's non-null.
   */
  private String getWhereClause(String prefix)
  {
    if (this.where != null)
    {
      return prefix + "(" + this.where + ")";
    }
    else
    {
      return "";
    }
  }

  /**
   * Attaches the WHERE clause arguments to a PreparedStatement if the
   * WHERE clause has been specified.
   * 
   * @return The next usable argument index. 
   */
  private int attachWhereArguments(int startingIndex,
      PreparedStatement statement)
    throws SQLException
  {
    int index = startingIndex;
    if (this.whereArguments != null)
    {
      for (String argument : this.whereArguments)
      {
        statement.setString(index++, argument);
      }
    }
    return index;
  }

  // 
  // Utility methods.
  // 

  /**
   * Updates the entity's field values from a map.  The input to this method is
   * meant to be generated by {@link #writeMap(Identifiable)}.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void readMap(T object, Map<String, Object> properties)
  {
    for (DataFieldToMethodMap map : getSetMethodMappingCache(null))
    {
      try
      {
        Object value = properties.get(map.getFieldName());
        if (value instanceof String
            && map.getMethod().getParameterTypes()[0].isEnum())
        {
          // Enums are serialized as strings.
          value = Enum.valueOf(
              (Class<? extends Enum>)map.getMethod().getParameterTypes()[0],
              String.valueOf(value));
        }
        this.access.invoke(object, map.getMethodIndex(), value);
        //map.method.invoke(object, value);
      }
      catch (IllegalArgumentException e)
      {
        log.log("::readMap caught exception for " + object + ", properties: "
            + properties, e);
      }
      //catch (IllegalAccessException e) {}
      //catch (InvocationTargetException e) {}
    }
  }
  
  /**
   * Creates a new object from a map.  The input to this method is meant to 
   * be generated by {@link #writeMap(Identifiable)}.
   */
  public T newObjectFromMap(Map<String, Object> properties)
  {
    T object = maker().make();
    readMap(object, properties);
    return object;
  }

  /**
   * Updates an exiting object from the given map of properties.
   */
  public T updateObjectFromMap(T object, Map<String, Object> properties)
  {
    readMap(object, properties);
    return object;
  }

  /**
   * Writes the entity's field values to a map.  The output from this method is
   * meant to be consumed by {@link #readMap(Identifiable, Map)}.
   */
  public Map<String, Object> writeMap(T object)
  {
    Map<String, Object> properties = new HashMap<>();
    for (DataFieldToMethodMap map : getGetMethodMappingCache())
    {
      try
      {
        //Object value = map.method.invoke(object);
        Object value = this.access.invoke(object, map.getMethodIndex(), NO_VALUES);
        if (value != null && map.getMethod().getReturnType().isEnum())
        {
          // Enums are serialized as strings.
          value = ((Enum<?>)value).name();
        }
        properties.put(map.getFieldName(), value);
      }
      catch (IllegalArgumentException e)
      {
        log.log("::writeMap caught exception for " + object
            + ", properties: " + properties, e);
      }
      //catch (IllegalAccessException e) {}
      //catch (InvocationTargetException e) {}
    }
    return properties;
  }

  /**
   * Returns a Map of field names to field values as a result of calling
   * each Get method in the Get method cache.
   */
  public Map<String, String> writeStringMap(T object)
  {
    Map<String, Object> map = this.writeMap(object);
    Map<String, String> stringMap = new HashMap<>(map.size());
    for (Map.Entry<String, Object> entry : map.entrySet())
    {
      stringMap.put(entry.getKey(), "" + entry.getValue());
    }
    return stringMap;
  }

  //
  // Private utility methods.
  //

  /**
   * Returns the value to be used in a prepared SQL statement for the given
   * field.
   */
  private Object readValueForUpdate(T object, DataFieldToMethodMap field)
  {
    Object value = null;
    try
    {
      value = this.access.invoke(object, field.getMethodIndex(), NO_VALUES);
    }
    catch (IllegalArgumentException e) {}
    return serialize(field, value);
  }

  /**
   * Returns the custom type adapter for the given field, or {@code null} if
   * one does not exist.
   *
   * @param isGetMethod {@code true} if the given field has a reference to a
   *                    "get" method, or {@code false} if it has a reference to
   *                    a "set" method.
   */
  @SuppressWarnings("unchecked")
  private TypeAdapter<Object, Object> getTypeAdapter(
      DataFieldToMethodMap field, boolean isGetMethod)
  {
    TypeAdapter<Object, Object> knownAdapter
        = (TypeAdapter<Object, Object>)this.typeAdaptersByFieldName.get(
            field.getFieldName());
    if (knownAdapter == null)
    {
      for (TypeAdapter<?, ?> adapter : this.entityStore.getTypeAdapters())
      {
        if (isGetMethod && adapter.appliesToGetMethod(field.getMethod())
            || !isGetMethod && adapter.appliesToSetMethod(field.getMethod()))
        {
          knownAdapter = (TypeAdapter<Object, Object>)adapter;
          break;
        }
      }
      if (knownAdapter == null)
      {
        this.typeAdaptersByFieldName.put(field.getFieldName(), NO_ADAPTER);
      }
    }
    return (knownAdapter == NO_ADAPTER)
        ? null
        : knownAdapter;
  }

  /**
   * Converts the given field value to a form the database will understand.
   */
  private Object serialize(DataFieldToMethodMap field, Object value)
  {
    Object toRet = value;
    if (value instanceof Enum)
    {
      // Enums are stored as strings.
      toRet = ((Enum<?>)value).name();
    }
    else if (value instanceof Calendar)
    {
      // Calendars are stored as dates.
      toRet = ((Calendar)value).getTime();
    }
    else if (value instanceof Character)
    {
      // Characters are stored as strings.
      toRet = value.toString();
    }
    final TypeAdapter<Object, Object> adapter = getTypeAdapter(field, true);
    if (adapter != null)
    {
      toRet = adapter.write(value);
    }
    return toRet;
  }

  /**
   * Reads the given field value from the result set.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object deserialize(DataFieldToMethodMap f, ResultSet rs)
      throws SQLException
  {
    Object value = null;
    
    final DataFieldToMethodMap.Type fieldType = f.getType();
    final int ci = f.getColumnIndex();
    
    if (f.isPrimitive())
    {
      if (fieldType == DataFieldToMethodMap.Type.IntPrimitive)
      {
        value = ci > 0 ? rs.getInt(ci) : rs.getInt(f.getFieldName());
      }
      else if (fieldType == DataFieldToMethodMap.Type.LongPrimitive)
      {
        value = ci > 0 ? rs.getLong(ci) : rs.getLong(f.getFieldName());
      }
      else if (fieldType == DataFieldToMethodMap.Type.BooleanPrimitive)
      {
        value = ci > 0 ? rs.getBoolean(ci) : rs.getBoolean(f.getFieldName());
      }
      else if (fieldType == DataFieldToMethodMap.Type.DoublePrimitive)
      {
        value = ci > 0 ? rs.getDouble(ci) : rs.getDouble(f.getFieldName());
      }
      else if (fieldType == DataFieldToMethodMap.Type.FloatPrimitive)
      {
        value = ci > 0 ? rs.getFloat(ci) : rs.getFloat(f.getFieldName());
      }
      else if (fieldType == DataFieldToMethodMap.Type.BytePrimitive)
      {
        value = ci > 0 ? rs.getByte(ci) : rs.getByte(f.getFieldName());
      }
      else if (fieldType == DataFieldToMethodMap.Type.CharPrimitive)
      {
        // Characters are stored as strings.
        value = StringHelper.emptyDefault(
            ci > 0 ? rs.getString(ci) : rs.getString(f.getFieldName()),
            "\0").charAt(0);
      }
      else if (fieldType == DataFieldToMethodMap.Type.ShortPrimitive)
      {
        value = ci > 0 ? rs.getShort(ci) : rs.getShort(f.getFieldName());
      }
      else
      {
        throw new AssertionError("Unknown primitive type.");
      }
    }
    else
    {
      // ResultSet.getString will return null correctly if the SQL value is 
      // null, so we can use it directly.
      if (fieldType == DataFieldToMethodMap.Type.String)
      {
        value = ci > 0 ? rs.getString(ci) : rs.getString(f.getFieldName());
      }
      else
      {
        // Deal with nullable integers and so on by first fetching the SQL
        // value as an Object.  If the returned value is non-null, then use
        // the appropriate ResultSet method.
        value = ci > 0 ? rs.getObject(ci) : rs.getObject(f.getFieldName());
        if (value != null)
        {
          if (fieldType == DataFieldToMethodMap.Type.IntegerObject)
          {
            value = ci > 0 ? rs.getInt(ci) : rs.getInt(f.getFieldName());
          }
          else if (fieldType == DataFieldToMethodMap.Type.LongObject)
          {
            value = ci > 0 ? rs.getLong(ci) : rs.getLong(f.getFieldName());
          }
          else if (fieldType == DataFieldToMethodMap.Type.Date)
          {
            // Reduce Timestamp objects to java.util.Date objects.
            final Date temporary = new Date();
            temporary.setTime(((Date)value).getTime());
            value = temporary;
          }
          else if (fieldType == DataFieldToMethodMap.Type.Calendar)
          {
            // Calendars are stored as dates.
            value = DateHelper.getCalendarInstance(
                ((java.util.Date)value).getTime());
          }
          else if (fieldType == DataFieldToMethodMap.Type.BooleanObject)
          {
            value = ci > 0 ? rs.getBoolean(ci) : rs.getBoolean(f.getFieldName());
          }
          else if (fieldType == DataFieldToMethodMap.Type.DoubleObject)
          {
            value = ci > 0 ? rs.getDouble(ci) : rs.getDouble(f.getFieldName());
          }
          else if (fieldType == DataFieldToMethodMap.Type.FloatObject)
          {
            value = ci > 0 ? rs.getFloat(ci) : rs.getFloat(f.getFieldName());
          }
          else if (fieldType == DataFieldToMethodMap.Type.ShortObject)
          {
            value = ci > 0 ? rs.getShort(ci) : rs.getShort(f.getFieldName());
          }
          else if (fieldType == DataFieldToMethodMap.Type.ByteObject)
          {
            value = ci > 0 ? rs.getByte(ci) : rs.getByte(f.getFieldName());
          }
          else if (fieldType == DataFieldToMethodMap.Type.CharacterObject)
          {
            // Characters are stored as strings.
            value = StringHelper.emptyDefault(
                ci > 0 ? rs.getString(ci) : rs.getString(f.getFieldName()),
                "\0").charAt(0);
          }
          else if (fieldType == DataFieldToMethodMap.Type.Enum)
          {
            // Enums are stored as strings.
            if (!StringHelper.isEmpty(String.valueOf(value)))
            {
              value = Enum.valueOf(
                  (Class<? extends Enum>)f.getJavaFieldType(),
                  String.valueOf(value));
            }
            else
            {
              value = null;
            }
          }
        }
      }
    }
    
    // Ask any assigned Adapter to modify the value as it sees fit.
    TypeAdapter<Object, Object> adapter = getTypeAdapter(f, false);
    if (adapter != null)
    {
      value = adapter.read(value);
    }
    
    return value;
  }

  /**
   * Applies an object as a parameter into an update or insert 
   * PreparedStatement.
   */
  private void applyValueToStatement(DataFieldToObjectEntityMap field, Object value, 
      PreparedStatement statement, int index)
      throws SQLException
  {
    // java.sql will not properly convert from java.util.Date to a TIMESTAMP
    // type.  So we force that by using the millisecond value of the Date
    // to construct a Timestamp of our own.
    if (field.getFieldType() == Types.TIMESTAMP)
    {
      final java.util.Date dateValue = (java.util.Date)value;
      statement.setTimestamp(index, 
          dateValue == null 
          ? null 
          : new Timestamp(dateValue.getTime()));
    }
    // In most cases, it is sufficient to just call setObject and provide
    // the target SQL data type as a parameter.
    else
    {
      statement.setObject(index, value, field.getFieldType());
    }
  }

  /**
   * Returns the cache of get methods for this entity type.  The cache is lazy-
   * initialized.
   */
  private DataFieldToMethodMap[] getGetMethodMappingCache()
  {
    // The bindToDatabase method is idempotent so this does not need to be
    // synchronized.
    if (this.getMethods == null)
    {
      bindToDatabase(null);
    }
    return this.getMethods;
  }

  /**
   * Returns the cache of get methods for this entity type, without the ID
   * field.  The cache is lazy-initialized.
   */
  private DataFieldToMethodMap[] getGetMethodMappingCacheWithoutId()
  {
    // This method is idempotent so this does not need to be synchronized.
    if (this.getMethodsWithoutId == null)
    {
      final DataFieldToMethodMap[] cache = getGetMethodMappingCache();
      final List<DataFieldToMethodMap> fields = new ArrayList<>(cache.length - 1);
      for (DataFieldToMethodMap field : cache)
      {
        if (!field.getFieldName().equalsIgnoreCase(this.id))
        {
          fields.add(field);
        }
      }
      final DataFieldToMethodMap[] result = new DataFieldToMethodMap[fields.size()]; 
      this.getMethodsWithoutId = fields.toArray(result);
    }
    return this.getMethodsWithoutId;
  }

  /**
   * Gets the comma-delimited String of "FieldName = ?" field-parts for an
   * UPDATE statement, not including the ID field. 
   */
  private String getFieldPartsForUpdate() 
  {
    // This method is idempotent so this does not need to be synchronized.
    if (this.fieldPartsForUpdate == null)
    {
      final StringList fieldParts = new StringList(", ");
      final DataFieldToMethodMap[] fields = getGetMethodMappingCacheWithoutId();
      for (DataFieldToObjectEntityMap field : fields)
      {
        fieldParts.add(enquote(field.getFieldName()) + " = ?");
      }
      this.fieldPartsForUpdate = fieldParts.toString();
    }
    return this.fieldPartsForUpdate;
  }

  /**
   * Returns the cache of set methods for this entity type.  The cache is lazy-
   * initialized.
   */
  private DataFieldToMethodMap[] getSetMethodMappingCache(ResultSet resultSet)
  {
    // The bindToDatabase method is idempotent so this does not need to be
    // synchronized.
    if (this.setMethods == null)
    {
      bindToDatabase(resultSet);
    }
    return this.setMethods;
  }

  /**
   * Attempts to construct database metadata from a ResultSet.
   */
  private List<DatabaseColumnMetaData> getMetadataFromResultSet(ResultSet resultSet)
  {
    final List<DatabaseColumnMetaData> metaData = new ArrayList<>();

    if (resultSet != null)
    {
      try
      {
        final ResultSetMetaData rsmd = resultSet.getMetaData();
        // ResultSetMetaData is indexed from 1, how quaint.
        for (int i = 1; i <= rsmd.getColumnCount(); i++)
        {
          final DatabaseColumnMetaData dcmd = new DatabaseColumnMetaData(
              rsmd.getColumnName(i),
              rsmd.getColumnType(i));
          metaData.add(dcmd);
        }
      }
      catch (SQLException sqlexc) 
      { }
    }
    
    // If neither the table or result set has worked, let's fail.
    if (CollectionHelper.isEmpty(metaData))
    {
      throw new EntityException("Could not read meta data for table \""
            + this.table + "\".");
    }
    
    return metaData;
  }

  /**
   * Finds a method pair given the provided prefixes.  Returns null if a valid
   * method pair could not be found (that is, if only one or neither method in
   * the pair can be found).
   */
  private MethodPair findMethodPair(
      Method[] methods, 
      DatabaseColumnMetaData columnInfo, 
      String getPrefix, 
      String setPrefix,
      boolean idColumn)
  {
    final MethodPair toReturn = new MethodPair();
    final String columnName = columnInfo.getColumnName();
    final int columnIndex = columnInfo.getOrdinalPosition();
    final int dataType = columnInfo.getDataType();
    
    for (Method method : methods)
    {
      final int methodParameterCount = method.getParameterTypes().length;
      final String methodName = method.getName();
      
      // Check for a "set" method first.  Set methods need to have only
      // 1 parameter.
      if (methodParameterCount == 1)
      {
        // Does the method name match what we'd expect?
        if ((idColumn && methodName.equalsIgnoreCase("setId"))
            || methodName.equalsIgnoreCase(setPrefix + columnName))
        {
          try
          {
            int index = this.access.getIndex(methodName, method.getParameterTypes()[0]);
            // Create a mapping.
            toReturn.setter = new DataFieldToMethodMap(method, columnName, 
                columnIndex, dataType, index);
          }
          catch (IllegalArgumentException iaexc)
          {
            // Consider this a failed mapping.
          }
        }
      }
      
      // Check for "get" methods next.  Get methods need to have zero
      // parameters.
      else if (methodParameterCount == 0)
      {
        // Does the method name match what we'd expect?
        if ((idColumn && methodName.equalsIgnoreCase("getId"))
            || methodName.equalsIgnoreCase(getPrefix + columnName))
        {
          try
          {
            int index = this.access.getIndex(methodName, NO_PARAMETERS);
            // Create a mapping.
            toReturn.getter = new DataFieldToMethodMap(method, columnName, 
                columnIndex, dataType, index);
          }
          catch (IllegalArgumentException iaexc)
          {
            // Consider this a failed mapping.
          }
        }
      }
    }
    
    // Only return non-null if both the setter and getter were found.
    if (  (toReturn.getter != null)
       && (toReturn.setter != null)
       )
    {
      return toReturn;
    }
    return null;
  }
  
  /**
   * A data structure returned by findMethodPair.
   */
  private static class MethodPair
  {
    private DataFieldToMethodMap setter;
    private DataFieldToMethodMap getter;
  }
  
  /**
   * Binds the DataEntity's field and methods to their corresponding columns in
   * the database table.  The mappings will be derived from database meta data
   * if it is available.
   */
  private void bindToDatabase(ResultSet resultSet)
  {
    try (DatabaseConnector connector = this.cf.getConnector())
    {
      // Query for the meta data of the DataEntity's table.
      Collection<DatabaseColumnMetaData> metaData = connector.getColumnMetaDataForTable(table);
      
      // If the table is not found, but we have a ResultSet, attempt to
      // bind using the result set.  This can be useful when Entities are
      // constructed from joined queries (not a single table) for read-only
      // use.
      if (CollectionHelper.isEmpty(metaData))
      {
        metaData = getMetadataFromResultSet(resultSet);
      }
      
      final Method[] methods = type.getMethods();

      // Create lists for set and get method mappings.
      final List<DataFieldToMethodMap> setMethodList = new ArrayList<>(metaData.size());
      final List<DataFieldToMethodMap> getMethodList = new ArrayList<>(metaData.size());

      for (DatabaseColumnMetaData columnInfo : metaData)
      {
        MethodPair methodPair = null;
        // First try to match the identity column in case it is not named "id".
        if (this.id().equalsIgnoreCase(columnInfo.getColumnName()))
        {
          methodPair = findMethodPair(methods, columnInfo, "get", "set", true);
        }
        // Try to find suitably-matching pairs of methods based on standard
        // Java conventions first, then jQuery-style (with no prefix).
        if (methodPair == null)
        {
          methodPair = findMethodPair(methods, columnInfo, "get", "set", false);
        }
        if (methodPair == null)
        {
          methodPair = findMethodPair(methods, columnInfo, "is", "set", false);
        }
        if (methodPair == null)
        {
          methodPair = findMethodPair(methods, columnInfo, "has", "set", false);
        }
        if (methodPair == null)
        {
          methodPair = findMethodPair(methods, columnInfo, "", "", false);
        }
        
        // Capture the method pair if any of the above matched.
        if (methodPair != null)
        {
          setMethodList.add(methodPair.setter);
          getMethodList.add(methodPair.getter);
        }
        else
        {
          entityStore.getLog().log("Unable to bind "
              + table + "." + columnInfo.getColumnName()
              + " to " + type.getSimpleName() + " class.", 
              LogLevel.ALERT);
        }
      }
      
      // Store the mapping arrays into the local cache arrays.
      final DataFieldToMethodMap[] sets = new DataFieldToMethodMap[setMethodList.size()];
      setMethodList.toArray(sets);
      this.setMethods = sets;
      
      final DataFieldToMethodMap[] gets = new DataFieldToMethodMap[getMethodList.size()];
      getMethodList.toArray(gets);
      this.getMethods = gets;
    }
  }

  /**
   * Makes an entity instance from current row of a query result set.  This
   * is not conventionally called directly, but it may be.  Use with caution;
   * the columns of the result set must be compatible with the entity. 
   * 
   * @param resultSet A SQL result set wherein the cursor lies on a row that
   *                  contains fields that can be used to initialize this
   *                  data entity.
   */
  public T make(ResultSet resultSet)
  {
    final T object = this.maker.make();
    
    // Set the identity.  We do this as a distinct operation because we
    // require that objects provide getId/setId via the Identifiable interface
    // but allow the Identity column on the database representation to be
    // customized (e.g., "SubscriptionID").  We should not expect that classes
    // provide both getId/setId and class-specific versions such as 
    // setSubscriptionId.
    try
    {
      final long idValue = resultSet.getLong(this.id);
      object.setId(idValue);
    }
    catch (SQLException e)
    {
      throw new EntityException("Exception while fetching identity during object initialization.", e);
    }
    
    final DataFieldToMethodMap[] mappings = getSetMethodMappingCache(resultSet);
    if (mappings == null)
    {
      throw new IllegalStateException("No set method mappings available for " + name());
    }
    
    // Go through the cache and call the methods as specified by the
    // map objects.
    for (DataFieldToMethodMap map : mappings)
    {
      try
      {
        final Object value = deserialize(map, resultSet);
        this.access.invoke(object, map.getMethodIndex(), value);
      }
      catch (Exception e)
      {
        throw new EntityException("Exception during object initialization (" + map.getMethod().getName() + ").", e);
      }
    }
    
    // If the entity implements PersistenceAware, let's tell the entity that
    // it is persisted (since we've fetched it from a persistence medium.
    if (object instanceof PersistenceAware)
    {
      ((PersistenceAware)object).setPersisted(true);
    }
    
    // Provide a reference to the EntityStore if the object is CacheAware. 
    if (object instanceof CacheAware)
    {
      ((CacheAware)object).setCacheController(this.entityStore);
    }
    
    // If the entity implements Initializable, let's call initialize to notify
    // the entity that we've completed construction and configuration (calls
    // to set methods).
    if (object instanceof Initializable)
    {
      ((Initializable)object).initialize();
    }
    
    return object;
  }

  /**
   * Wraps a SQL table name or column name in the identifier quote strings used
   * by the database.  For example, MySQL uses the "`" character.  Table or
   * column names wrapped in these characters can be used safely in SQL queries
   * even if they are reserved keywords.
   */
  private String enquote(String tableOrColumn)
  {
    String quote = this.cf.getIdentifierQuoteString();
    return new StringBuilder(tableOrColumn.length() + 2)
        .append(quote)
        .append(tableOrColumn)
        .append(quote)
        .toString();
  }

  /**
   * Determine if a provided Identifiable has been persisted or is new.  If
   * the parameter doesn't implement PersistenceAware, an identity of 0 
   * indicates "new" and non-0 indicates "persisted."  If the parameter does
   * implement PersistenceAware, the isPersisted method will be called.
   */
  protected boolean isPersisted(Identifiable entity)
  {
    if (entity instanceof PersistenceAware)
    {
      return ((PersistenceAware)entity).isPersisted();
    }
    else
    {
      return !(entity.getId() == 0);
    }
  }

  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "EntityGroup [" + name() + "]";
  }

  //
  // Static methods.
  //

  /**
   * Creates a new {@link Builder}, which is used to construct an
   * {@link EntityGroup}.  Example usage:
   * 
   * <pre>
   * EntityGroup&lt;Foo&gt; = EntityGroup.of(Foo.class) // new Builder
   *     .table("foos") // modified Builder
   *     .id("fooID") // modified Builder
   *     .build(entityStore); // new EntityGroup
   * </pre>
   * 
   * <p>Note that a {@link EntityStore#register(
   * com.techempower.data.EntityGroup.Builder)} method exists, so
   * in the common case where you only want to register the group and don't
   * care to retain your own reference to it, calling {@code .build(entityStore)}
   * is unnecessary.  For example:
   * 
   * <pre>
   * register(EntityGroup.of(Foo.class) // new Builder
   *     .table("foos") // modified Builder
   *     .id("fooID") // modified Builder
   * ); // the register method calls .build(entityStore) for us
   * </pre>
   * 
   * @param type The type of the entities.
   * @return A new {@link Builder}.
   */
  public static <T extends Identifiable> Builder<T> of(Class<T> type)
  {
    return new Builder<>(type);
  }

  // 
  // Inner classes.
  // 

  /**
   * Creates new instances of {@code EntityGroup}.
   */
  public static class Builder<T extends Identifiable>
  {
    protected final Class<T> type;
    protected String table;
    protected String id;
    protected EntityMaker<T> maker;
    protected Comparator<? super T> comparator;
    protected String where;
    protected String[] whereArguments;
    protected boolean readOnly = false;

    /**
     * Returns a new builder of {@link EntityGroup} instances.
     * 
     * @param type The type of objects in the group.
     */
    protected Builder(Class<T> type)
    {
      if (type == null)
      {
        throw new NullPointerException();
      }

      this.type = type;
    }

    /**
     * Returns a new {@link EntityGroup} with parameters set by the builder.
     */
    public EntityGroup<T> build(EntityStore entityStore)
    {
      return new EntityGroup<>(
          entityStore,
          this.type,
          this.table,
          this.id,
          this.maker,
          this.comparator,
          this.where,
          this.whereArguments,
          this.readOnly);
    }

    /**
     * Sets the name of the database table that stores the entities.
     */
    public Builder<T> table(String tableName)
    {
      this.table = tableName;
      return this;
    }

    /**
     * Sets the name of the database column that holds the identities of the
     * entities.
     */
    public Builder<T> id(String idField)
    {
      this.id = idField;
      return this;
    }

    /**
     * Sets the generator to use when creating entities of this type.
     */
    public Builder<T> maker(EntityMaker<T> entityMaker)
    {
      this.maker = entityMaker;
      return this;
    }

    /**
     * Sets the comparator to use when sorting entities of this type.
     */
    public Builder<T> comparator(Comparator<? super T> entityComparator)
    {
      this.comparator = entityComparator;
      return this;
    }

    /**
     * Sets the name of a single method that returns naturally-ordered values.
     */
    public Builder<T> comparator(String methodName)
    {
      this.comparator = new ReflectiveComparator<>(methodName, ReflectiveComparator.BY_METHOD);
      return this;
    }
    
    /**
     * Specifies that only read-operations should be permitted on the 
     * resulting EntityGroup.
     */
    public Builder<T> readOnly()
    {
      this.readOnly = true;
      return this;
    }

    /**
     * Sets the WHERE clause and arguments to use in queries for entities of
     * this type.
     * 
     * @param whereClause An optional WHERE clause (not including the "WHERE" keyword)
     *        in PreparedStatement form.
     * @param arguments The arguments to insert into the WHERE clause.
     */
    public Builder<T> where(String whereClause, String... arguments)
    {
      this.where = whereClause;
      this.whereArguments = arguments;
      return this;
    }

    /**
     * Sets the generator to use when creating entities of this type by
     * providing arguments for a constructor.  This can be a useful shorthand
     * when the constructor arguments do not vary with time, such as a 
     * reference to the Application instance.
     *   <p>
     * Also note that an entity class may implement CacheAware to receive
     * a reference to the EntityStore when an instance is instantiated.
     */
    @SuppressWarnings("unchecked")
    public <O extends Object> Builder<T> constructorArgs(final O... arguments)
    {
      // If we have been given arguments, attempt to find the matching
      // constructor.
      Class<?>[] classes = new Class[arguments.length];
      final Constructor<T> constructor;
      int index = 0;
      for (O arg : arguments)
      {
        classes[index++] = arg.getClass();
      }
      try
      {
        constructor = this.type.getConstructor(classes);
      }
      catch (NoSuchMethodException nsme)
      {
        throw new IllegalArgumentException("Cannot find specified constructor.", nsme);
      }

      this.maker = new EntityMaker<T>() {
        @Override
        public T make() {
          try
          {
            return constructor.newInstance(arguments);
          }
          catch (IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {}
          return null;
        }
      };
      
      return this;
    }

  } // End Builder.

} // End EntityGroup.
