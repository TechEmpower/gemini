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

import gnu.trove.iterator.*;
import gnu.trove.set.*;
import gnu.trove.set.hash.*;

import java.sql.*;
import java.util.*;

import com.techempower.cache.*;
import com.techempower.collection.relation.*;
import com.techempower.helper.*;
import com.techempower.util.*;

/**
 * A thread-safe {@link EntityRelation} that talks to the database on every
 * operation and delegates the enforcement of relation type (many-to-many, etc.)
 * to the database itself.
 *
 * <p><strong>Important difference from {@link CachedRelation}</strong>: It is
 * essential that the table enforces whatever uniqueness constraints are desired
 * for the relation.  In addition, if applicable, the uniqueness constraint
 * should be such that inserts silently fail on duplicates.  For instance, in MS
 * SQL Server, the IGNORE_DUP_KEY option should be enabled for that constraint.
 *
 * <p>Since more than one relation can exist for a given pair of classes, it's
 * likely that you'll want to retain a reference to the relation in your entity
 * store. Here are examples of registering relations:
 *
 * <pre>
 * private EntityRelation&lt;Foo, Bar&gt; mapFooToBar;
 * private EntityRelation&lt;Bar, Baz&gt; mapBarToBaz;
 *
 * public void initialize()
 * {
 *   // A relation stored in the table "mapfootobar", with columns "foo" and
 *   // "bar".  If you follow that naming convention for your relation tables,
 *   // then your calls to register will look like this.
 *   mapFooToBar = register(SqlEntityRelation.of(Foo.class, Bar.class));
 *
 *   // A relation stored in the table "BarsAndBazzes", with columns "BarID" and
 *   // "BazID".  Your calls to register will look like this if you want a
 *   // relation type other than many-to-many or you follow non-standard
 *   // naming conventions for your relation tables.
 *   mapBarToBaz = register(SqlEntityRelation.of(Bar.class, Baz.class)
 *       .table("BarsAndBazzes")
 *       .leftColumn("BarID")
 *       .rightColumn("BazID"));
 * }
 * </pre>
 *
 * @param <L> the type of the left values in this relation
 * @param <R> the type of the right values in this relation
 */
public class SqlEntityRelation<L extends Identifiable, R extends Identifiable>
    implements EntityRelation<L, R>
{
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
   * {@link SqlEntityRelation}.  Example usage:
   *
   * <pre>
   * EntityRelation&lt;Foo, Bar&gt; = SqlEntityRelation.of(Foo.class, Bar.class) // new Builder
   *     .table("MapFooToBar") // modified Builder
   *     .leftColumn("FooID") // modified Builder
   *     .rightColumn("BarID") // modified Builder
   *     .build(store); // new EntityRelation
   * </pre>
   *
   * <p>Note that a {@link EntityStore#register(
   * com.techempower.data.EntityRelation.Builder)} method exists, and
   * it returns a {@link EntityRelation}, so in most cases calling
   * {@code .build(store)} is unnecessary.  For example:
   *
   * <pre>
   * mapFooToBar = register(SqlEntityRelation.of(Foo.class, Bar.class) // new Builder
   *     .table("MapFooToBar") // modified Builder
   *     .leftColumn("FooID") // modified Builder
   *     .rightColumn("BarID") // modified Builder
   * ); // the register method calls .build(store) for us and returns the result
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

  //
  // Constructors
  //

  /**
   * Constructs a new relation with the specified parameters.  This constructor
   * is non-public because users should only instantiate this class by way of a
   * {@link Builder}, which can be obtained from a call to
   * {@link SqlEntityRelation#of(Class, Class)}.
   *
   * @param store the store that manages the objects in this relation
   * @param leftType the type of the left values in this relation
   * @param rightType the type of the right values in this relation
   * @param tableName the name of the table in the database that stores this
   *                  relation
   * @param leftColumn the name of the column that stores the identities of
   *                   the left values
   * @param rightColumn the name of the column that stores the identities of
   *                    the right values
   */
  protected SqlEntityRelation(EntityStore store,
      Class<L> leftType, Class<R> rightType,
      String tableName, String leftColumn, String rightColumn)
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
    this.quotedTable = DatabaseHelper.quoteTableOrColumn(
        this.cf, this.table);
    this.quotedLeftColumn = DatabaseHelper.quoteTableOrColumn(
        this.cf, this.leftColumn);
    this.quotedRightColumn = DatabaseHelper.quoteTableOrColumn(
        this.cf, this.rightColumn);
  }

  //
  // Helper methods
  //

  /**
   * Returns an INSERT statement for the given number of insertions.  This will
   * add the "IGNORE" keyword if the database supports it.  Otherwise, that
   * behavior should be enforced at the database level.  For instance, in MS SQL
   * Server, the IGNORE_DUP_KEY option can be applied to the unique index.
   *
   * @param insertCount the number of insertions to be made
   * @return an INSERT statement for the given number of insertions
   */
  private String newInsertStatement(int insertCount)
  {
    if (insertCount < 1)
    {
      throw new IllegalArgumentException(
          "Argument 'insertCount' must be greater than zero.");
    }
    String insertKeyword = (this.cf.getDatabaseAffinity() == DatabaseAffinity.MS_SQL_SERVER)
        ? "INSERT" // They better have IGNORE_DUP_KEY set!
        : "INSERT IGNORE";
    String questionMarks = (insertCount == 1)
        ? "(?,?)"
        : StringHelper.join(",", Collections.nCopies(insertCount, "(?,?)"));
    return insertKeyword + " INTO " + this.quotedTable + " ("
        + this.quotedLeftColumn + ", " + this.quotedRightColumn + ") VALUES "
        + questionMarks + ";";
  }

  //
  // Public API
  //

  @Override
  public boolean add(long leftID, long rightID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement insertStatement = monitor.getConnection().prepareStatement(
             newInsertStatement(1)))
    {
      insertStatement.setLong(1, leftID);
      insertStatement.setLong(2, rightID);
      return insertStatement.executeUpdate() > 0;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean add(long leftID, R right)
  {
    return right != null
        && add(leftID, right.getId());
  }

  @Override
  public boolean add(L left, long rightID)
  {
    return left != null
        && add(left.getId(), rightID);
  }

  @Override
  public boolean add(L left, R right)
  {
    return left != null
        && right != null
        && add(left.getId(), right.getId());
  }

  @Override
  public boolean addAll(LongRelation relationToAdd)
  {
    if (relationToAdd == null)
    {
      return false;
    }
    int insertCount = relationToAdd.size();
    LongRelationIterator iterator = relationToAdd.iterator();
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
    {
      if (insertCount <= MAX_SQL_SIZE)
      {
        // Easy.  We just run one statement.
        try (PreparedStatement insertStatementA = monitor.getConnection().prepareStatement(
                 newInsertStatement(insertCount)))
        {
          for (int i = 0; i < insertCount; i++)
          {
            iterator.next();
            insertStatementA.setLong(2 * i + 1, iterator.left());
            insertStatementA.setLong(2 * i + 2, iterator.right());
          }
          return insertStatementA.executeUpdate() > 0;
        }
      }
      else
      {
        // Darn, we'll have to run a bunch of statements.  First, a bunch of
        // large ones in a batch, then a smaller one for the remaining pairs.
        int smallInsertSize = insertCount % MAX_SQL_SIZE;
        int lastBigInsertIndex = (insertCount - smallInsertSize);
        int numLargeInserts = lastBigInsertIndex / MAX_SQL_SIZE;
        boolean changed = false;
        try (PreparedStatement insertStatementA = monitor.getConnection().prepareStatement(
                 newInsertStatement(MAX_SQL_SIZE)))
        {
          for (int i = 0; i < numLargeInserts; i++)
          {
            for (int j = 0; j < MAX_SQL_SIZE; j++)
            {
              iterator.next();
              insertStatementA.setLong(2 * j + 1, iterator.left());
              insertStatementA.setLong(2 * j + 2, iterator.right());
            }
            insertStatementA.addBatch();
          }
          int[] updateCounts = insertStatementA.executeBatch();
          for (int count : updateCounts)
          {
            if (count > 0)
            {
              changed = true;
              break;
            }
          }
        }

        // Now pick up any stragglers.
        if (smallInsertSize > 0)
        {
          try (PreparedStatement insertStatementB = monitor.getConnection().prepareStatement(
                   newInsertStatement(smallInsertSize)))
          {
            for (int i = 0; i < smallInsertSize; i++)
            {
              iterator.next();
              insertStatementB.setLong(2 * i + 1, iterator.left());
              insertStatementB.setLong(2 * i + 2, iterator.right());
            }
            return insertStatementB.executeUpdate() > 0
                || changed;
          }
        }

        return changed;
      }
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public void clear()
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
             "DELETE FROM " + quotedTable + ";"))
    {
      deleteStatement.executeUpdate();
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean contains(long leftID, long rightID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT COUNT(*) AS 'count' FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ? AND "
                 + quotedRightColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, leftID);
      selectStatement.setLong(2, rightID);
      ResultSet resultSet = selectStatement.executeQuery();
      resultSet.next();
      return resultSet.getInt("count") > 0;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean contains(long leftID, R right)
  {
    return right != null
        && contains(leftID, right.getId());
  }

  @Override
  public boolean contains(L left, long rightID)
  {
    return left != null
        && contains(left.getId(), rightID);
  }

  @Override
  public boolean contains(L left, R right)
  {
    return left != null
        && right != null
        && contains(left.getId(), right.getId());
  }

  @Override
  public boolean containsLeftValue(long leftID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT COUNT(*) AS 'count' FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, leftID);
      ResultSet resultSet = selectStatement.executeQuery();
      resultSet.next();
      return resultSet.getInt("count") > 0;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean containsLeftValue(L left)
  {
    return left != null
        && containsLeftValue(left.getId());
  }

  @Override
  public boolean containsRightValue(long rightID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT COUNT(*) AS 'count' FROM " + quotedTable
                 + " WHERE " + quotedRightColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, rightID);
      ResultSet resultSet = selectStatement.executeQuery();
      resultSet.next();
      return resultSet.getInt("count") > 0;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean containsRightValue(R right)
  {
    return right != null
        && containsRightValue(right.getId());
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
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT " + quotedLeftColumn
                 + " FROM " + quotedTable
                 + " WHERE " + quotedRightColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, rightID);
      ResultSet resultSet = selectStatement.executeQuery();
      TLongSet leftIDs = new TLongHashSet();
      while (resultSet.next())
      {
        leftIDs.add(resultSet.getLong(leftColumn));
      }
      return leftIDs.toArray();
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public Set<Long> leftIDs(long rightID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT " + quotedLeftColumn
                 + " FROM " + quotedTable
                 + " WHERE " + quotedRightColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, rightID);
      ResultSet resultSet = selectStatement.executeQuery();
      Set<Long> leftIDs = new HashSet<>();
      while (resultSet.next())
      {
        leftIDs.add(resultSet.getLong(leftColumn));
      }
      return leftIDs;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
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
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT COUNT(*) AS 'count' FROM " + quotedTable
                 + " WHERE " + quotedRightColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, rightID);
      ResultSet resultSet = selectStatement.executeQuery();
      resultSet.next();
      return resultSet.getInt("count");
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
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
    return leftType;
  }

  @Override
  public List<L> leftValueList(long rightID)
  {
    return store.list(leftType, leftIDs(rightID));
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
    return new HashSet<>(store.list(leftType, leftIDs(rightID)));
  }

  @Override
  public Set<L> leftValueSet(R right)
  {
    return (right == null)
        ? Collections.emptySet()
        : leftValueSet(right.getId());
  }

  @Override
  public LongRelation relation()
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT " + quotedLeftColumn + ", " + quotedRightColumn
                 + " FROM " + quotedTable + ";",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      ResultSet resultSet = selectStatement.executeQuery();
      LongRelation relation = new ManyToManyLongRelation();
      while (resultSet.next())
      {
        relation.add(
            resultSet.getLong(leftColumn),
            resultSet.getLong(rightColumn));
      }
      return relation;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean remove(long leftID, long rightID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
             "DELETE FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ? AND "
                 + quotedRightColumn + " = ?;"))
    {
      deleteStatement.setLong(1, leftID);
      deleteStatement.setLong(2, rightID);
      return deleteStatement.executeUpdate() > 0;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean remove(long leftID, R right)
  {
    return right != null
        && remove(leftID, right.getId());
  }

  @Override
  public boolean remove(L left, long rightID)
  {
    return left != null
        && remove(left.getId(), rightID);
  }

  @Override
  public boolean remove(L left, R right)
  {
    return left != null
        && right != null
        && remove(left.getId(), right.getId());
  }

  @Override
  public boolean removeAll(LongRelation relationToRemove)
  {
    if (relationToRemove == null)
    {
      return false;
    }
    int deleteCount = relationToRemove.size();
    LongRelationIterator iterator = relationToRemove.iterator();
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
    {
      if (deleteCount <= MAX_SQL_SIZE)
      {
        // Easy.  We just run one statement.
        try (PreparedStatement deleteStatementA = monitor.getConnection().prepareStatement(
            "DELETE FROM " + quotedTable + " ("
                + quotedLeftColumn + "," + quotedRightColumn
                + ") VALUES "
                + StringHelper.join(",", Collections.nCopies(deleteCount, "(?,?)"))
                + ";"))
        {
          for (int i = 0; i < deleteCount; i++)
          {
            iterator.next();
            deleteStatementA.setLong(2 * i + 1, iterator.left());
            deleteStatementA.setLong(2 * i + 2, iterator.right());
          }
          return deleteStatementA.executeUpdate() > 0;
        }
      }
      else
      {
        // Darn, we'll have to run a bunch of statements.  First, a bunch of
        // large ones in a batch, then a smaller one for the remaining pairs.
        int smallDeleteSize = deleteCount % MAX_SQL_SIZE;
        int lastBigDeleteIndex = (deleteCount - smallDeleteSize);
        int numLargeDeletes = lastBigDeleteIndex / MAX_SQL_SIZE;
        boolean changed = false;
        try (PreparedStatement deleteStatementA = monitor.getConnection().prepareStatement(
            "DELETE FROM " + quotedTable + " WHERE ("
                + quotedLeftColumn + "," + quotedRightColumn
                + ") IN ("
                + StringHelper.join(",", Collections.nCopies(MAX_SQL_SIZE, "(?,?)"))
                + ");"))
        {
          for (int i = 0; i < numLargeDeletes; i++)
          {
            for (int j = 0; j < MAX_SQL_SIZE; j++)
            {
              iterator.next();
              deleteStatementA.setLong(2 * j + 1, iterator.left());
              deleteStatementA.setLong(2 * j + 2, iterator.right());
            }
            deleteStatementA.addBatch();
          }
          int[] updateCounts = deleteStatementA.executeBatch();
          for (int count : updateCounts)
          {
            if (count > 0)
            {
              changed = true;
              break;
            }
          }
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
              iterator.next();
              deleteStatementB.setLong(2 * i + 1, iterator.left());
              deleteStatementB.setLong(2 * i + 2, iterator.right());
            }
            return deleteStatementB.executeUpdate() > 0
                || changed;
          }
        }

        return changed;
      }
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
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

  @Override
  public boolean removeLeftValue(long leftID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
             "DELETE FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ?;"))
    {
      deleteStatement.setLong(1, leftID);
      return deleteStatement.executeUpdate() > 0;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean removeLeftValue(L left)
  {
    return left != null
        && removeLeftValue(left.getId());
  }

  @Override
  public boolean removeRightValue(long rightID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
             "DELETE FROM " + quotedTable
                 + " WHERE " + quotedRightColumn + " = ?;"))
    {
      deleteStatement.setLong(1, rightID);
      return deleteStatement.executeUpdate() > 0;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public boolean removeRightValue(R right)
  {
    return right != null
        && removeRightValue(right.getId());
  }

  @Override
  public boolean replaceAll(LongRelation relationToReplace)
  {
    if (relationToReplace == null)
    {
      return false;
    }
    int insertCount = relationToReplace.size();
    LongRelationIterator iterator = relationToReplace.iterator();
    boolean changed = false;
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor())
    {
      try (PreparedStatement deleteStatement = monitor.getConnection().prepareStatement(
          "DELETE FROM " + quotedTable + ";"))
      {
        changed |= deleteStatement.executeUpdate() > 0;
      }
      if (insertCount == 0)
      {
        return changed;
      }

      if (insertCount <= MAX_SQL_SIZE)
      {
        // Easy.  We just run one statement.
        try (PreparedStatement insertStatementA = monitor.getConnection().prepareStatement(
                 newInsertStatement(insertCount)))
        {
          for (int i = 0; i < insertCount; i++)
          {
            iterator.next();
            insertStatementA.setLong(2 * i + 1, iterator.left());
            insertStatementA.setLong(2 * i + 2, iterator.right());
          }
          return insertStatementA.executeUpdate() > 0
              || changed;
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
                 newInsertStatement(MAX_SQL_SIZE)))
        {
          for (int i = 0; i < numLargeInserts; i++)
          {
            for (int j = 0; j < MAX_SQL_SIZE; j++)
            {
              iterator.next();
              insertStatementA.setLong(2 * j + 1, iterator.left());
              insertStatementA.setLong(2 * j + 2, iterator.right());
            }
            insertStatementA.addBatch();
            // So many statements that we should batch-this-batch.
            if (++count % 100 == 0)
            {
              int[] updateCounts = insertStatementA.executeBatch();
              if (!changed)
              {
                for (int c : updateCounts)
                {
                  if (c > 0)
                  {
                    changed = true;
                    break;
                  }
                }
              }
            }
          }
          int[] updateCounts = insertStatementA.executeBatch();
          if (!changed)
          {
            for (int c : updateCounts)
            {
              if (c > 0)
              {
                changed = true;
                break;
              }
            }
          }
        }

        // Now pick up any stragglers.
        if (smallInsertSize > 0)
        {
          try (PreparedStatement insertStatementB = monitor.getConnection().prepareStatement(
                   newInsertStatement(smallInsertSize)))
          {
            for (int i = 0; i < smallInsertSize; i++)
            {
              iterator.next();
              insertStatementB.setLong(2 * i + 1, iterator.left());
              insertStatementB.setLong(2 * i + 2, iterator.right());
            }
            return insertStatementB.executeUpdate() > 0
                || changed;
          }
        }

        return changed;
      }
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
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
    return rightIDsLongSet(leftID).toArray();
  }

  @Override
  public TLongSet rightIDsLongSet(long leftID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT " + quotedRightColumn + " FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, leftID);
      ResultSet resultSet = selectStatement.executeQuery();
      TLongSet rightIDsLongSet = new TLongHashSet();
      while (resultSet.next())
      {
        rightIDsLongSet.add(resultSet.getLong(rightColumn));
      }
      return rightIDsLongSet;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public Set<Long> rightIDs(long leftID)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT " + quotedRightColumn + " FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, leftID);
      ResultSet resultSet = selectStatement.executeQuery();
      Set<Long> rightIDs = new HashSet<>();
      while (resultSet.next())
      {
        rightIDs.add(resultSet.getLong(rightColumn));
      }
      return rightIDs;
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
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
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT COUNT(*) AS 'count' FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ?;",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      selectStatement.setLong(1, leftID);
      ResultSet resultSet = selectStatement.executeQuery();
      resultSet.next();
      return resultSet.getInt("count");
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public int rightSize(L left)
  {
    return (left == null)
        ? 0
        : rightSize(left.getId());
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
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT COUNT(*) AS 'count' FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ? AND "
                 + quotedRightColumn + " IN ("
                 + StringHelper.join(",", Collections.nCopies(filterRightIds.size(), "?"))
                 + ");",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      int index = 0;
      selectStatement.setLong(++index, leftID);
      for (long rightID : filterRightIds)
      {
        selectStatement.setLong(++index, rightID);
      }
      ResultSet resultSet = selectStatement.executeQuery();
      resultSet.next();
      return resultSet.getInt("count");
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public int rightSize(long leftID, TLongSet filterRightIds)
  {
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT COUNT(*) AS 'count' FROM " + quotedTable
                 + " WHERE " + quotedLeftColumn + " = ? AND "
                 + quotedRightColumn + " IN ("
                 + StringHelper.join(",", Collections.nCopies(filterRightIds.size(), "?"))
                 + ");",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      int index = 0;
      selectStatement.setLong(++index, leftID);
      for (TLongIterator iterator = filterRightIds.iterator(); iterator.hasNext();)
      {
        selectStatement.setLong(++index, iterator.next());
      }
      ResultSet resultSet = selectStatement.executeQuery();
      resultSet.next();
      return resultSet.getInt("count");
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public Class<R> rightType()
  {
    return rightType;
  }

  @Override
  public List<R> rightValueList(long leftID)
  {
    return store.list(rightType, rightIDs(leftID));
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
    return new HashSet<>(store.list(rightType, rightIDs(leftID)));
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
    try (ConnectionMonitor monitor = this.cf.getConnectionMonitor();
         PreparedStatement selectStatement = monitor.getConnection().prepareStatement(
             "SELECT COUNT(*) AS 'count' FROM " + quotedTable + ";",
             ResultSet.TYPE_FORWARD_ONLY,
             ResultSet.CONCUR_READ_ONLY))
    {
      ResultSet resultSet = selectStatement.executeQuery();
      resultSet.next();
      return resultSet.getInt("count");
    }
    catch (SQLException e)
    {
      throw new EntityException(e);
    }
  }

  @Override
  public String tableName()
  {
    return this.table;
  }

  @Override
  public String toString()
  {
    return "SqlEntityRelation ["
        + leftType.getSimpleName()
        + "," + rightType.getSimpleName()
        + "]";
  }

  //
  // Inner classes
  //

  /**
   * Creates new instances of {@link SqlEntityRelation}.
   *
   * @param <L> the type of the left values in the relation
   * @param <R> the type of the right values in the relation
   */
  public static class Builder<L extends Identifiable, R extends Identifiable>
      implements EntityRelation.Builder<L, R, SqlEntityRelation<L, R>>
  {
    protected final Class<L> leftType;
    protected final Class<R> rightType;
    protected String table;
    protected String leftColumn;
    protected String rightColumn;

    /**
     * Returns a new builder of {@link SqlEntityRelation} instances.
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
    public SqlEntityRelation<L, R> build(EntityStore store)
    {
      Objects.requireNonNull(store);
      return new SqlEntityRelation<>(
          store,
          this.leftType,
          this.rightType,
          this.table,
          this.leftColumn,
          this.rightColumn);
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
  }

}
