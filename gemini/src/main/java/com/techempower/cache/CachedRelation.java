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

import gnu.trove.set.*;
import gnu.trove.set.hash.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.locks.*;

import com.techempower.collection.relation.*;
import com.techempower.data.*;
import com.techempower.gemini.cluster.*;
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * A thread-safe {@link EntityRelation} that caches its contents and enforces
 * the relation type (many-to-many, etc.) with an {@link LongRelation}.
 *
 * <p>Since more than one relation can exist for a given pair of classes, it's
 * likely that you'll want to retain a reference to the relation in your cache.
 * Here are examples of registering relations:
 *
 * <pre>
 * private EntityRelation&lt;Foo, Bar&gt; mapFooToBar;
 * private EntityRelation&lt;Bar, Baz&gt; mapBarToBaz;
 *
 * public void initialize()
 * {
 *   // A many-to-many relation stored in the table "mapfootobar", with columns
 *   // "foo" and "bar".  If you follow that naming convention for your relation
 *   // tables, then your calls to register will look like this.
 *   mapFooToBar = register(CachedRelation.of(Foo.class, Bar.class));
 *   
 *   // A one-to-one relation stored in the table "BarsAndBazzes", with columns
 *   // "BarID" and "BazID".  Your calls to register will look like this if you
 *   // want a relation type other than many-to-many or you follow non-standard
 *   // naming conventions for your relation tables.
 *   mapBarToBaz = register(CachedRelation.of(Bar.class, Baz.class)
 *       .table("BarsAndBazzes")
 *       .leftColumn("BarID")
 *       .rightColumn("BazID")
 *       .relation(new OneToOneLongRelation(true)));
 * }
 * </pre>
 *
 * @param <L> the type of the left values in this relation
 * @param <R> the type of the right values in this relation
 *
 * @see com.techempower.collection.relation.LongRelation
 */
public class CachedRelation<L extends Identifiable, R extends Identifiable>
  implements EntityRelation<L, R>, Identifiable
{
  // TODO: Consider whether database-level uniqueness constraints and "INSERT
  // IGNORE" behavior should be supported. This shouldn't matter for
  // single-server applications, but clustered applications could get into
  // situations where simultaneous writes cause bad data to be inserted.

  //
  // Constants
  //

  /**
   * This is the largest number of pairs to be inserted or deleted in a single
   * SQL statement.  MySQL breaks on extremely large SQL statements, so this
   * provides a safe upper limit.
   */
  private static final int MAX_SQL_SIZE = 1000;

  //
  // Static factories
  //

  /**
   * Creates a new {@link Builder}, which is used to construct a
   * {@link CachedRelation}.  Example usage:
   * 
   * <pre>
   * CachedRelation&lt;Foo, Bar&gt; = CachedRelation.of(Foo.class, Bar.class) // new Builder
   *     .table("MapFooToBar") // modified Builder
   *     .leftColumn("FooID") // modified Builder
   *     .rightColumn("BarID") // modified Builder
   *     .build(controller); // new CachedRelation
   * </pre>
   * 
   * <p>Note that a {@link EntityStore#register(
   * com.techempower.cache.CachedRelation.Builder)} method exists, and
   * it returns a {@link CachedRelation}, so in most cases calling
   * {@code .build(controller)} is unnecessary.  For example:
   * 
   * <pre>
   * mapFooToBar = register(CachedRelation.of(Foo.class, Bar.class) // new Builder
   *     .table("MapFooToBar") // modified Builder
   *     .leftColumn("FooID") // modified Builder
   *     .rightColumn("BarID") // modified Builder
   * ); // the register method calls .build(controller) for us and returns the result
   * </pre>
   * 
   * @param leftType the type of the left values in this relation
   * @param rightType the type of the right values in this relation
   * @return A new {@link Builder}.
   */
  public static <L extends Identifiable, R extends Identifiable> Builder<L, R> of(
      Class<L> leftType, Class<R> rightType)
  {
    return new Builder<>(leftType, rightType);
  }

  //
  // Fields
  //

  private final EntityStore store;
  private final ConnectorFactory cf;
  private final Class<L> leftType;
  private final Class<R> rightType;
  private final String table;
  private final String leftColumn;
  private final String rightColumn;
  private final String quotedTable;
  private final String quotedLeftColumn;
  private final String quotedRightColumn;
  private final LongRelation relation;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final Collection<CachedRelationListener> listeners
      = new ArrayList<>();
  private volatile boolean loaded = false;

  /**
   * A unique identifier for this cached relation to be assigned by the entity
   * store as the relation is registered.
   */
  private long id;

  //
  // Constructors
  //

  /**
   * Constructs a new relation with the specified parameters.  This constructor
   * is non-public because users should only instantiate this class by way of a
   * {@link Builder}, which can be obtained from a call to
   * {@link CachedRelation#of(Class, Class)}.
   * 
   * @param leftType the type of the left values in this relation
   * @param rightType the type of the right values in this relation
   * @param store the store that manages the objects in this relation
   * @param tableName the name of the table in the database that stores this 
   *    relation
   * @param leftColumn the name of the column that stores the identities of 
   *    the left values
   * @param rightColumn the name of the column that stores the identities of 
   *    the right values
   * @param relation the type of relation between left and right values, e.g.
   *    many-to-many, one-to-many...
   */
  protected CachedRelation(EntityStore store,
      Class<L> leftType, Class<R> rightType,
      String tableName, String leftColumn, String rightColumn,
      LongRelation relation)
  {
    this.store = store;
    this.cf = store.getConnectorFactory();
    this.leftType = leftType;
    this.rightType = rightType;
    this.table = (tableName == null)
        ? "map" + leftType.getSimpleName().toLowerCase()
            + "to" + rightType.getSimpleName().toLowerCase()
        : tableName;
    this.leftColumn = (leftColumn == null)
        ? leftType.getSimpleName().toLowerCase()
        : leftColumn;
    this.rightColumn = (rightColumn == null)
        ? rightType.getSimpleName().toLowerCase()
        : rightColumn;
    this.relation = (relation == null)
        ? new ManyToManyLongRelation(true)
        : (LongRelation)relation.clone(); // Defensive copy.
    this.quotedTable = DatabaseHelper.quoteTableOrColumn(
        this.cf, this.table);
    this.quotedLeftColumn = DatabaseHelper.quoteTableOrColumn(
        this.cf, this.leftColumn);
    this.quotedRightColumn = DatabaseHelper.quoteTableOrColumn(
        this.cf, this.rightColumn);
  }

  //
  // Public API
  //

  @Override
  public boolean add(long leftID, long rightID)
  {
    return add(leftID, rightID, true, true, true);
  }

  /**
   * Adds the specified pair to the relation. 
   * 
   * @param leftID the ID of the left value of the pair to be added
   * @param rightID the ID of the right value of the pair to be added
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  public boolean add(long leftID, long rightID, boolean updateDatabase, boolean notifyListeners, boolean notifyDistributionListeners)
  {
    if (!this.loaded)
    {
      load();
    }
    this.lock.writeLock().lock();
    try
    {
      if (!this.relation.add(leftID, rightID))
      {
        return false;
      }
      
      if (updateDatabase)
      {
        try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
        {
          try (PreparedStatement insertStatement = monitor.getConnection().prepareStatement(
              "INSERT INTO " + quotedTable  + " ("
                  + quotedLeftColumn + ", " + quotedRightColumn
                  + ") VALUES (?, ?);"))
          {
            insertStatement.setLong(1, leftID);
            insertStatement.setLong(2, rightID);
            insertStatement.executeUpdate();
          }
        }
      }
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.add(this.id, leftID, rightID);
          }
        }
      }
      
      return true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while adding relation, left " + leftID 
          + " and right " + rightID, e);
      return false;
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public boolean add(long leftID, R right)
  {
    return (right != null
            && add(leftID, right.getId()));
  }

  @Override
  public boolean add(L left, long rightID)
  {
    return (left != null
            && add(left.getId(), rightID));
  }

  @Override
  public boolean add(L left, R right)
  {
    return (left != null
            && right != null
            && add(left.getId(), right.getId()));
  }

  @Override
  public boolean addAll(LongRelation relationToAdd)
  {
    return addAll(relationToAdd, true, true, true);
  }

  /**
   * Adds the given pairs to the relation and updates the database.
   * 
   * @param relationToAdd the pairs to be added
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  public boolean addAll(LongRelation relationToAdd, boolean updateDatabase,
      boolean notifyListeners, boolean notifyDistributionListeners)
  {
    if (relationToAdd == null)
    {
      return false;
    }
    if (!this.loaded)
    {
      load();
    }

    this.lock.writeLock().lock();
    try
    {
      long[] newLefts = new long[relationToAdd.size()];
      long[] newRights = new long[relationToAdd.size()];
      int insertCount = 0; 
      
      for (LongRelationIterator iter = relationToAdd.iterator(); iter.hasNext();)
      {
        iter.next();
        long leftID = iter.left();
        long rightID = iter.right();
        if (this.relation.add(leftID, rightID))
        {
          newLefts[insertCount] = leftID;
          newRights[insertCount] = rightID;
          insertCount++;
        }
      }
      
      if (insertCount == 0)
      {
        // No changes.  We can leave now.
        return false;
      }
      
      if (updateDatabase)
      {
        // Release the write lock because all that's left is updating the DB.
        this.lock.writeLock().unlock();
        
        try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
        {
          if (insertCount <= MAX_SQL_SIZE)
          {
            // Easy.  We just run one statement.
            try (PreparedStatement insertStatementA = monitor.getConnection().prepareStatement(
                "INSERT INTO " + quotedTable + " ("
                    + quotedLeftColumn + "," + quotedRightColumn
                    + ") VALUES "
                    + StringHelper.join(",", Collections.nCopies(insertCount, "(?,?)"))
                    + ";"))
            {
              for (int i = 0; i < insertCount; i++)
              {
                insertStatementA.setLong(2 * i + 1, newLefts[i]);
                insertStatementA.setLong(2 * i + 2, newRights[i]);
              }
              insertStatementA.executeUpdate();
            }
          }
          else
          {
            // Darn, we'll have to run a bunch of statements.  First, a bunch of
            // large ones in a batch, then a smaller one for the remaining pairs.
            int smallInsertSize = insertCount % MAX_SQL_SIZE;
            int lastBigInsertIndex = (insertCount - smallInsertSize);
            int numLargeInserts = lastBigInsertIndex / MAX_SQL_SIZE;
            try (PreparedStatement insertStatementA = monitor.getConnection().prepareStatement(
                "INSERT INTO " + quotedTable + " ("
                    + quotedLeftColumn + "," + quotedRightColumn
                    + ") VALUES "
                    + StringHelper.join(",", Collections.nCopies(MAX_SQL_SIZE, "(?,?)"))
                    + ";"))
            {
              for (int i = 0; i < numLargeInserts; i++)
              {
                int startIndex = i * MAX_SQL_SIZE;
                for (int j = 0; j < MAX_SQL_SIZE; j++)
                {
                  insertStatementA.setLong(2 * j + 1, newLefts[j + startIndex]);
                  insertStatementA.setLong(2 * j + 2, newRights[j + startIndex]);
                }
                insertStatementA.addBatch();
              }
              insertStatementA.executeBatch();
            }
            
            // Now pick up any stragglers.
            if (smallInsertSize > 0)
            {
              try (PreparedStatement insertStatementB = monitor.getConnection().prepareStatement(
                  "INSERT INTO " + quotedTable + " ("
                      + quotedLeftColumn + "," + quotedRightColumn
                      + ") VALUES "
                      + StringHelper.join(",", Collections.nCopies(smallInsertSize, "(?,?)"))
                      + ";"))
              {
                for (int i = 0; i < smallInsertSize; i++)
                {
                  insertStatementB.setLong(2 * i + 1, newLefts[i + lastBigInsertIndex]);
                  insertStatementB.setLong(2 * i + 2, newRights[i + lastBigInsertIndex]);
                }
                insertStatementB.executeUpdate();
              }
            }
          }
        }
      }
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.addAll(this.id, relationToAdd);
          }
        }
      }
      
      return true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while adding relations.", e);
      return false;
    }
    finally
    {
      try
      {
        this.lock.writeLock().unlock();
      }
      catch (IllegalMonitorStateException exc)
      {
        // That's ok, it just means we already unlocked it prior to updating
        // the database.
      }
    }
  }

  /**
   * Adds the given listener to the relation.
   * 
   * @param listener the listener to be added
   */
  public void addListener(CachedRelationListener listener)
  {
    this.listeners.add(listener);
  }

  @Override
  public void clear()
  {
    clear(true, true, true);
  }

  /**
   * Clears the relation of all pairs.
   * 
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   */
  public void clear(boolean updateDatabase, boolean notifyListeners, boolean notifyDistributionListeners)
  {
    this.lock.writeLock().lock();
    try
    {
      this.relation.clear();
      this.lock.writeLock().unlock();
      
      if (updateDatabase)
      {
        try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
        {
          try (PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
              "DELETE FROM " + quotedTable + ";"))
          {
            deleteStatement.executeUpdate();
          }
        }
      }
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.clear(this.id);
          }
        }
      }
      
      this.loaded = true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while clearing relations.", e);
    }
    finally
    {
      try
      {
        this.lock.writeLock().unlock();
      }
      catch (IllegalMonitorStateException exc)
      {
        // That's ok, it just means we already unlocked it in deferDatabaseUpdates.
      }
    }
  }

  @Override
  public boolean contains(long leftID, long rightID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.contains(leftID, rightID);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public boolean contains(long leftID, R right)
  {
    return (right != null
            && contains(leftID, right.getId()));
  }

  @Override
  public boolean contains(L left, long rightID)
  {
    return (left != null
            && contains(left.getId(), rightID));
  }

  @Override
  public boolean contains(L left, R right)
  {
    return (left != null
            && right != null
            && contains(left.getId(), right.getId()));
  }

  @Override
  public boolean containsLeftValue(long leftID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.containsLeftValue(leftID);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsLeftValue(L left)
  {
    return (left != null
            && containsLeftValue(left.getId()));
  }

  @Override
  public boolean containsRightValue(long rightID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.containsRightValue(rightID);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsRightValue(R right)
  {
    return (right != null 
            && containsRightValue(right.getId()));
  }

  @Override
  public long[] leftIDArray(R right)
  {
    return (right == null)
        ? new long[0]
        : leftIDArray(right.getId());
  }

  @Override
  public long[] leftIDArray(long rightID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.leftValues(rightID);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public Set<Long> leftIDs(long rightID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      Set<Long> leftIDs = new HashSet<>();
      for (long i : this.relation.leftValues(rightID))
      {
        leftIDs.add(i);
      }
      return leftIDs;
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public Set<Long> leftIDs(R right)
  {
    return (right == null)
        ? Collections.emptySet()
        : leftIDs(right.getId());
  }

  @Override
  public int leftSize(long rightID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.leftSize(rightID);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public int leftSize(R right)
  {
    return (right == null)
        ? 0
        : leftSize(right.getId());
  }

  @Override
  public Class<L> leftType()
  {
    return this.leftType;
  }

  @Override
  public List<L> leftValueList(long rightID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.store.list(this.leftType,
          this.relation.leftValues(rightID));
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public List<L> leftValueList(R right)
  {
    return (right == null)
        ? Collections.emptyList()
        : leftValueList(right.getId());
  }

  @Override
  public Set<L> leftValueSet(long rightID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return new HashSet<>(this.store.list(this.leftType,
          this.relation.leftValues(rightID)));
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public Set<L> leftValueSet(R right)
  {
    return (right == null)
        ? Collections.emptySet()
        : leftValueSet(right.getId());
  }

  /**
   * Returns a copy of the list of listeners to this relation.
   * 
   * @return A copy of the list of listeners to this relation.
   */
  public List<CachedRelationListener> listeners()
  {
    return new ArrayList<>(this.listeners);
  }

  /**
   * Loads the contents of this relation from the database.
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
      
      this.relation.clear();
      
      try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
      {
        try (PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
            "SELECT " + quotedLeftColumn + ", "
                + quotedRightColumn + " FROM " + quotedTable
                + ";"))
        {
          try (ResultSet resultSet = selectStatement.executeQuery())
          {
            while (resultSet.next())
            {
              long leftID = resultSet.getLong(this.leftColumn);
              long rightID = resultSet.getLong(this.rightColumn);
              this.relation.add(leftID, rightID);
            }
          }
        }
      }
      
      this.loaded = true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while loading relations.", e);
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public LongRelation relation()
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return (LongRelation)this.relation.clone();
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public boolean remove(long leftID, long rightID)
  {
    return remove(leftID, rightID, true, true, true);
  }

  @Override
  public <T extends Identifiable> boolean removeEntity(T object)
  {
    return (object == null)
        ? false
        : removeEntity(object.getClass(), object.getId());
  }

  @Override
  public <T extends Identifiable> boolean removeEntity(Class<T> type, long idToRemove)
  {
    boolean changed = false;
    if (this.leftType == type)
    {
      changed = (removeLeftValue(idToRemove) || changed);
    }
    if (this.rightType == type)
    {
      changed = (removeRightValue(idToRemove) || changed);
    }
    return changed;
  }

  /**
   * Removes the specified pair of values from this relation.
   * 
   * @param leftID the id of the left value of the pair to be removed
   * @param rightID the id of the right value of the pair to be removed
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  public boolean remove(long leftID, long rightID, boolean updateDatabase, boolean notifyListeners, boolean notifyDistributionListeners)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.writeLock().lock();
    try
    {
      if (!this.relation.remove(leftID, rightID))
      {
        return false;
      }

      if (updateDatabase)
      {
        try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
        {
          try (PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
              "DELETE FROM " + quotedTable + " WHERE "
                  + quotedLeftColumn + " = ? AND "
                  + quotedRightColumn + " = ?;"))
          {
            deleteStatement.setLong(1, leftID);
            deleteStatement.setLong(2, rightID);
            deleteStatement.executeUpdate();
          }
        }
      }
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.remove(this.id, leftID, rightID);
          }
        }
      }
      
      return true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while removing relation, left " + leftID 
          + " and right " + rightID + ".", e);
      return false;
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public boolean remove(long leftID, R right)
  {
    return (right != null
            && remove(leftID, right.getId()));
  }

  @Override
  public boolean remove(L left, long rightID)
  {
    return (left != null
            && remove(left.getId(), rightID));
  }

  @Override
  public boolean remove(L left, R right)
  {
    return (left != null
            && right != null
            && remove(left.getId(), right.getId()));
  }

  @Override
  public boolean removeAll(LongRelation relationToRemove)
  {
    return removeAll(relationToRemove, true, true, true);
  }

  /**
   * Removes the given pairs from the relation and updates the database.
   * 
   * @param relationToRemove the pairs to be removed
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  public boolean removeAll(LongRelation relationToRemove, boolean updateDatabase,
      boolean notifyListeners, boolean notifyDistributionListeners)
  {
    if (relationToRemove == null)
    {
      return false;
    }
    if (!this.loaded)
    {
      load();
    }

    this.lock.writeLock().lock();
    try
    {
      long[] removedLefts = new long[relationToRemove.size()];
      long[] removedRights = new long[relationToRemove.size()];
      int deleteCount = 0; 
      
      for (LongRelationIterator iter = relationToRemove.iterator(); iter.hasNext();)
      {
        iter.next();
        long leftID = iter.left();
        long rightID = iter.right();
        if (this.relation.remove(leftID, rightID))
        {
          removedLefts[deleteCount] = leftID;
          removedRights[deleteCount] = rightID;
          deleteCount++;
        }
      }
      
      if (deleteCount == 0)
      {
        // No changes.  We can leave now.
        return false;
      }
      
      if (updateDatabase)
      {
        // Release the write lock because all that's left is updating the DB.
        this.lock.writeLock().unlock();
        
        try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
        {
          if (deleteCount <= MAX_SQL_SIZE)
          {
            // Easy.  We just run one statement.
            try (PreparedStatement deleteStatementA = monitor.getConnection().prepareStatement(
                "DELETE FROM " + quotedTable + " WHERE ("
                    + quotedLeftColumn + "," + quotedRightColumn
                    + ") IN ("
                    + StringHelper.join(",", Collections.nCopies(deleteCount, "(?,?)"))
                    + ");"))
            {
              for (int i = 0; i < deleteCount; i++)
              {
                deleteStatementA.setLong(2 * i + 1, removedLefts[i]);
                deleteStatementA.setLong(2 * i + 2, removedRights[i]);
              }
              deleteStatementA.executeUpdate();
            }
          }
          else
          {
            // Darn, we'll have to run a bunch of statements.  First, a bunch of
            // large ones in a batch, then a smaller one for the remaining pairs.
            int smallDeleteSize = deleteCount % MAX_SQL_SIZE;
            int lastBigDeleteIndex = (deleteCount - smallDeleteSize);
            int numLargeDeletes = lastBigDeleteIndex / MAX_SQL_SIZE;
            try (PreparedStatement deleteStatementA = monitor.getConnection().prepareStatement(
                "DELETE FROM " + quotedTable + " WHERE ("
                    + quotedLeftColumn + "," + quotedRightColumn
                    + ") IN ("
                    + StringHelper.join(",", Collections.nCopies(MAX_SQL_SIZE, "(?,?)"))
                    + ");"))
            {
              for (int i = 0; i < numLargeDeletes; i++)
              {
                int startIndex = i * MAX_SQL_SIZE;
                for (int j = 0; j < MAX_SQL_SIZE; j++)
                {
                  deleteStatementA.setLong(2 * j + 1, removedLefts[j + startIndex]);
                  deleteStatementA.setLong(2 * j + 2, removedRights[j + startIndex]);
                }
                deleteStatementA.addBatch();
              }
              deleteStatementA.executeBatch();
            }
            
            // Now pick up any stragglers.
            if (smallDeleteSize > 0)
            {
              try (PreparedStatement deleteStatementB = monitor.getConnection().prepareStatement(
                  "DELETE FROM " + quotedTable + " WHERE ("
                      + quotedLeftColumn + "," + quotedRightColumn
                      + ") IN ("
                      + StringHelper.join(",", Collections.nCopies(smallDeleteSize, "(?,?)"))
                      + ");"))
              {
                for (int i = 0; i < smallDeleteSize; i++)
                {
                  deleteStatementB.setLong(2 * i + 1, removedLefts[i + lastBigDeleteIndex]);
                  deleteStatementB.setLong(2 * i + 2, removedRights[i + lastBigDeleteIndex]);
                }
                deleteStatementB.executeUpdate();
              }
            }
          }
        }
      }
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.addAll(this.id, relationToRemove);
          }
        }
      }
      
      return true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while removing relations.", e);
      return false;
    }
    finally
    {
      try
      {
        this.lock.writeLock().unlock();
      }
      catch (IllegalMonitorStateException exc)
      {
        // That's ok, it just means we already unlocked it prior to updating
        // the database.
      }
    }
  }

  @Override
  public boolean removeLeftValue(long leftID)
  {
    return removeLeftValue(leftID, true, true, true);
  }

  /**
   * Removes the specified left value from this relation.
   * 
   * @param leftID the id of the left value to be removed from this relation
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  public boolean removeLeftValue(long leftID, boolean updateDatabase, boolean notifyListeners, boolean notifyDistributionListeners)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.writeLock().lock();
    try
    {
      if (!this.relation.removeLeftValue(leftID))
      {
        return false;
      }

      if (updateDatabase)
      {
        try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
        {
          try (PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
              "DELETE FROM " + quotedTable + " WHERE "
                  + quotedLeftColumn + " = ?;"))
          {
            deleteStatement.setLong(1, leftID);
            deleteStatement.executeUpdate();
          }
        }
      }
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.removeLeftValue(this.id, leftID);
          }
        }
      }
      
      return true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while removing all for left " + leftID + ".", e);
      return false;
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public boolean removeLeftValue(L left)
  {
    return (left != null
            && removeLeftValue(left.getId()));
  }

  @Override
  public boolean removeRightValue(long rightID)
  {
    return removeRightValue(rightID, true, true, true);
  }

  /**
   * Removes the specified right value from this relation.
   * 
   * @param rightID the id of the right value to be removed from this 
   *    relation
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  public boolean removeRightValue(long rightID, boolean updateDatabase, boolean notifyListeners, boolean notifyDistributionListeners)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.writeLock().lock();
    try
    {
      if (!this.relation.removeRightValue(rightID))
      {
        return false;
      }

      if (updateDatabase)
      {
        try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
        {
          try (PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
              "DELETE FROM " + quotedTable + " WHERE "
                  + quotedRightColumn + " = ?;"))
          {
            deleteStatement.setLong(1, rightID);
            deleteStatement.executeUpdate();
          }
        }
      }
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.removeRightValue(this.id, rightID);
          }
        }
      }
      
      return true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while removing all for right " + rightID + ".", e);
      return false;
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public boolean removeRightValue(R right)
  {
    return (right != null
            && removeRightValue(right.getId()));
  }

  /**
   * {@inheritDoc}
   *
   * <p>If called with a relation that is equivalent to the current relation, 
   * this function will immediately return with a value of false and will
   * not hit the db or notify listeners.</p>
   */
  @Override
  public boolean replaceAll(LongRelation relationToReplace)
  {
    return replaceAll(relationToReplace, true, true, true);
  }

  /**
   * <p>Clears the existing relation, then sets the relation to the passed in
   * relation.</p>
   * 
   * <p>Note that this is generally preferable to doing the following:</p>
   * 
   * <pre>
   * {@code
   * // foo is a CachedRelation.
   * foo.clear();
   * foo.addAll( .. );
   * }
   * </pre>
   * 
   * <p>The above will cause foo to be empty for a certain window of time.  
   * Using replaceAll( .. ) will achieve the same end goal of clearing the
   * current relation and then adding the passed in relation, but will never 
   * result in a call to this object seeing an empty relation.</p>
   * 
   * <p>This call will only block reads very briefly while switching to the
   * new cached relation and notifying listeners.  If deferDatabaseUpdates is
   * false, then this blocking extends until the database writes have 
   * completed.</p>
   * 
   * <p>If called with a relation that is equivalent to the current relation, 
   * this function will immediately return with a value of false and will
   * not hit the db or notify listeners.</p>
   * 
   * @param relationToReplace the pairs to be added
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  public boolean replaceAll(LongRelation relationToReplace, boolean updateDatabase,
      boolean notifyListeners, boolean notifyDistributionListeners)
  {
    if (relationToReplace == null)
    {
      return false;
    }
    if (!this.loaded)
    {
      load();
    }

    this.lock.writeLock().lock();
    try
    {
      if (this.relation.containsAll(relationToReplace)
          && relationToReplace.containsAll(this.relation))
      {
        // The relations are equivalent so this is a no-op.
        return false;
      }
      
      this.relation.clear();
      long[] newLefts = new long[relationToReplace.size()];
      long[] newRights = new long[relationToReplace.size()];
      int insertCount = 0; 
      
      for (LongRelationIterator iter = relationToReplace.iterator(); iter.hasNext();)
      {
        iter.next();
        long leftID = iter.left();
        long rightID = iter.right();
        if (this.relation.add(leftID, rightID))
        {
          newLefts[insertCount] = leftID;
          newRights[insertCount] = rightID;
          insertCount++;
        }
      }
      
      if (updateDatabase)
      {
        // Release the write lock because all that's left is updating the DB.
        this.lock.writeLock().unlock();
        
        try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
        {
          try (PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
              "DELETE FROM " + quotedTable + ";"))
          {
            deleteStatement.executeUpdate();
          }
          
          if (insertCount > 0)
          {
            if (insertCount <= MAX_SQL_SIZE)
            {
              // Easy.  We just run one statement.
              try (PreparedStatement insertStatementA = monitor.getConnection().prepareStatement(
                  "INSERT INTO " + quotedTable + " ("
                      + quotedLeftColumn + "," + quotedRightColumn
                      + ") VALUES "
                      + StringHelper.join(",", Collections.nCopies(insertCount, "(?,?)"))
                      + ";"))
              {
                for (int i = 0; i < insertCount; i++)
                {
                  insertStatementA.setLong(2 * i + 1, newLefts[i]);
                  insertStatementA.setLong(2 * i + 2, newRights[i]);
                }
                insertStatementA.executeUpdate();
              }
            }
            else
            {
              // Darn, we'll have to run a bunch of statements.  First, a bunch of
              // large ones in a batch, then a smaller one for the remaining pairs.
              int smallInsertSize = insertCount % MAX_SQL_SIZE;
              int lastBigInsertIndex = (insertCount - smallInsertSize);
              int numLargeInserts = lastBigInsertIndex / MAX_SQL_SIZE;
              int count = 0;
              try (PreparedStatement insertStatementA = monitor.getConnection().prepareStatement(
                  "INSERT INTO " + quotedTable + " ("
                      + quotedLeftColumn + "," + quotedRightColumn
                      + ") VALUES "
                      + StringHelper.join(",", Collections.nCopies(MAX_SQL_SIZE, "(?,?)"))
                      + ";"))
              {
                for (int i = 0; i < numLargeInserts; i++)
                {
                  int startIndex = i * MAX_SQL_SIZE;
                  for (int j = 0; j < MAX_SQL_SIZE; j++)
                  {
                    insertStatementA.setLong(2 * j + 1, newLefts[j + startIndex]);
                    insertStatementA.setLong(2 * j + 2, newRights[j + startIndex]);
                  }
                  insertStatementA.addBatch();
                  // So many statements that we should batch-this-batch.
                  if (++count % 100 == 0)
                  {
                    insertStatementA.executeBatch();
                  }
                }
                insertStatementA.executeBatch();
              }
              
              // Now pick up any stragglers.
              if (smallInsertSize > 0)
              {
                try (PreparedStatement insertStatementB = monitor.getConnection().prepareStatement(
                    "INSERT INTO " + quotedTable + " ("
                        + quotedLeftColumn + "," + quotedRightColumn
                        + ") VALUES "
                        + StringHelper.join(",", Collections.nCopies(smallInsertSize, "(?,?)"))
                        + ";"))
                {
                  for (int i = 0; i < smallInsertSize; i++)
                  {
                    insertStatementB.setLong(2 * i + 1, newLefts[i + lastBigInsertIndex]);
                    insertStatementB.setLong(2 * i + 2, newRights[i + lastBigInsertIndex]);
                  }
                  insertStatementB.executeUpdate();
                }
              }
            }
          }
        }
      }
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.addAll(this.id, relationToReplace);
          }
        }
      }
      
      return true;
    }
    catch (SQLException e)
    {
      store.getLog().log("Exception while replacing relations.", e);
      return false;
    }
    finally
    {
      try
      {
        this.lock.writeLock().unlock();
      }
      catch (IllegalMonitorStateException exc)
      {
        // That's ok, it just means we already unlocked it prior to updating
        // the database.
      }
    }
  }

  /**
   * Internally marks this relation as "not loaded", which is understood to 
   * mean that it should be reloaded from the database before being used again.
   */
  public void reset()
  {
    reset(true, true);
  }

  /**
   * Internally marks this relation as "not loaded", which is understood to
   * mean that it should be reloaded from the database before being used
   * again.
   * 
   * @param notifyListeners whether to notify the listeners of the reset
   * @param notifyDistributionListeners Whether to notify any
   * DistributionListeners; only used When notifyListeners is true.
   */
  public void reset(boolean notifyListeners, boolean notifyDistributionListeners)
  {
    this.lock.writeLock().lock();
    try
    {
      this.loaded = false;
      
      if (notifyListeners)
      {
        for (CachedRelationListener listener : this.listeners)
        {
          if (!(listener instanceof DistributionListener)
              || notifyDistributionListeners)
          {
            listener.reset(this.id);
          }
        }
      }
    }
    finally
    {
      this.lock.writeLock().unlock();
    }
  }

  /**
   * Resets this relation if it maps objects of the specified type.
   * 
   * @param type The type of the cache group being reset.
   */
  public <T extends Identifiable> void reset(Class<T> type)
  {
    reset(type, true, true);
  }

  /**
   * Resets this relation if it maps objects of the specified type.
   * 
   * @param type The type of the cache group being reset.
   * @param notifyListeners whether to notify the listeners of the reset
   */
  public <T extends Identifiable> void reset(Class<T> type, boolean notifyListeners, boolean notifyDistributionListeners)
  {
    if (type.equals(this.leftType)
        || type.equals(this.rightType))
    {
      reset(notifyListeners, notifyDistributionListeners);
    }
  }

  @Override
  public long[] rightIDArray(L left)
  {
    return (left == null)
        ? new long[0]
        : rightIDArray(left.getId());
  }

  @Override
  public long[] rightIDArray(long leftID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.rightValues(leftID);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public TLongSet rightIDsLongSet(long leftID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.rightValuesLongSet(leftID);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public Set<Long> rightIDs(long leftID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      Set<Long> rightIDs = new HashSet<>();
      for (long i : this.relation.rightValues(leftID))
      {
        rightIDs.add(i);
      }
      return rightIDs;
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public Set<Long> rightIDs(L left)
  {
    return (left == null)
        ? Collections.emptySet()
        : rightIDs(left.getId());
  }

  @Override
  public TLongSet rightIDsLongSet(L left)
  {
    return (left == null)
        ? new TLongHashSet(0)
        : rightIDsLongSet(left.getId());
  }

  @Override
  public int rightSize(long leftID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.rightSize(leftID);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public int rightSize(L left)
  {
    return (left == null)
        ? 0
        : rightSize(left.getId(), (Collection<Long>)null);
  }

  @Override
  public int rightSize(L left, Collection<Long> filterRightIds)
  {
    return (left == null)
        ? 0
        : rightSize(left.getId(), filterRightIds);
  }

  @Override
  public int rightSize(L left, TLongSet filterRightIds)
  {
    return (left == null)
        ? 0
        : rightSize(left.getId(), filterRightIds);
  }

  @Override
  public int rightSize(long leftID, Collection<Long> filterRightIds)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.rightSize(leftID, filterRightIds);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public int rightSize(long leftID, TLongSet filterRightIds)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.rightSize(leftID, filterRightIds);
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public Class<R> rightType()
  {
    return this.rightType;
  }

  @Override
  public List<R> rightValueList(long leftID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.store.list(this.rightType,
          this.relation.rightValues(leftID));
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public List<R> rightValueList(L left)
  {
    return (left == null)
        ? Collections.emptyList()
        : rightValueList(left.getId());
  }

  @Override
  public Set<R> rightValueSet(long leftID)
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return new HashSet<>(this.store.list(this.rightType,
          this.relation.rightValues(leftID)));
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public Set<R> rightValueSet(L left)
  {
    return (left == null)
        ? Collections.emptySet()
        : rightValueSet(left.getId());
  }

  @Override
  public int size()
  {
    if (!this.loaded)
    {
      load();
    }

    this.lock.readLock().lock();
    try
    {
      return this.relation.size();
    }
    finally
    {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public long getId()
  {
    return this.id;
  }

  @Override
  public void setId(long identity)
  {
    this.id = identity;
  }

  @Override
  public String tableName()
  {
    return this.table;
  }
  
  @Override
  public String toString()
  {
    return "CachedRelation [" 
        + leftType.getSimpleName() 
        + "," + rightType.getSimpleName() 
        + "]";
  }

  //
  // Inner classes
  //

  /**
   * Creates new instances of {@link CachedRelation}.
   *
   * @param <L> the type of the left values in the relation
   * @param <R> the type of the right values in the relation
   */
  public static class Builder<L extends Identifiable, R extends Identifiable>
      implements EntityRelation.Builder<L, R, CachedRelation<L,R>>
  {
    private final Class<L> leftType;
    private final Class<R> rightType;
    private String table;
    private String leftColumn;
    private String rightColumn;
    private LongRelation relation;

    /**
     * Returns a new builder of {@link CachedRelation} instances.
     * 
     * @param leftType The type of left objects.
     * @param rightType The type of right objects.
     */
    protected Builder(Class<L> leftType, Class<R> rightType)
    {
      Objects.requireNonNull(leftType);
      Objects.requireNonNull(rightType);
      this.leftType = leftType;
      this.rightType = rightType;
    }

    @Override
    public CachedRelation<L,R> build(EntityStore store)
    {
      Objects.requireNonNull(store);
      return new CachedRelation<>(
          store,
          this.leftType, 
          this.rightType, 
          this.table, 
          this.leftColumn, 
          this.rightColumn, 
          this.relation);
    }

    /**
     * Sets the name of the table in the database that stores the relation.
     */
    public Builder<L, R> table(String tableName)
    {
      Objects.requireNonNull(tableName);
      this.table = tableName;
      return this;
    }

    /**
     * Sets the name of the column in the database that holds the left ids in
     * the relation.
     */
    public Builder<L, R> leftColumn(String leftColumnName)
    {
      Objects.requireNonNull(leftColumnName);
      this.leftColumn = leftColumnName;
      return this;
    }

    /**
     * Sets the name of the column in the database that holds the right ids in
     * the relation.
     */
    public Builder<L, R> rightColumn(String rightColumnName)
    {
      Objects.requireNonNull(rightColumnName);
      this.rightColumn = rightColumnName;
      return this;
    }

    /**
     * Sets the relation type.
     * 
     * @see ManyToManyLongRelation
     * @see ManyToOneLongRelation
     * @see OneToManyLongRelation
     * @see OneToOneLongRelation
     */
    public Builder<L, R> relation(LongRelation longRelation)
    {
      Objects.requireNonNull(longRelation);
      this.relation = longRelation;
      return this;
    }
  }
}
