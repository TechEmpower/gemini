package com.techempower.cache;

import java.util.*;
import com.techempower.collection.relation.*;
import com.techempower.data.*;
import com.techempower.util.*;


/**
 * In order for the CacheMessageManager to consistently apply messages to both CachedRelation and
 * LruSqlEntityRelation we use this interface of common methods. Methods in here were previously in
 * CachedRelation.
 *
 * @param <L> the type of the left values in this relation
 * @param <R> the type of the right values in this relation
 */
public interface CachingEntityRelation<L extends Identifiable, R extends Identifiable>
    extends EntityRelation<L, R>, Identifiable {

  /**
   * Adds the given listener to the relation.
   *
   * @param listener the listener to be added
   */
  default void addListener(CachedRelationListener listener) {
    // By default ignores messages.
  }

  /**
   * Returns a copy of the list of listeners to this relation.
   *
   * @return A copy of the list of listeners to this relation.
   */
  default List<CachedRelationListener> listeners() {
    // By default do not maintain a list of listeners.
    return new ArrayList<CachedRelationListener>(0);
  }

  /**
   * Internally marks this relation as "not loaded", which is understood to mean that it should be
   * reloaded from the database before being used again.
   *
   * @param notifyListeners whether to notify the listeners of the reset
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   */
  void reset(boolean notifyListeners, boolean notifyDistributionListeners);

  /**
   * Resets this relation if it maps objects of the specified type.
   *
   * @param type The type of the cache group being reset.
   * @param notifyListeners whether to notify the listeners of the reset
   */
  <T extends Identifiable> void reset(Class<T> type, boolean notifyListeners,
      boolean notifyDistributionListeners);

  /**
   * Resets this relation if it maps objects of the specified type.
   *
   * @param type The type of the cache group being reset.
   */
  <T extends Identifiable> void reset(Class<T> type);

  /**
   * Removes the specified pair of values from this relation.
   * 
   * @param leftID the id of the left value of the pair to be removed
   * @param rightID the id of the right value of the pair to be removed
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean remove(long leftID, long rightID, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners);

  /**
   * Removes the specified right value from this relation.
   * 
   * @param rightID the id of the right value to be removed from this relation
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean removeRightValue(long rightID, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners);

  /**
   * Removes the specified left value from this relation.
   * 
   * @param leftID the id of the left value to be removed from this relation
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   * @return <tt>true</tt> if the relation was modified as a result of the call
   */
  boolean removeLeftValue(long leftID, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners);

  /**
   * <p>
   * Clears the existing relation, then sets the relation to the passed in relation.
   * </p>
   * 
   * <p>
   * Note that this is generally preferable to doing the following:
   * </p>
   * 
   * <pre>
   * {@code
   * // foo is a CachedRelation.
   * foo.clear();
   * foo.addAll( .. );
   * }
   * </pre>
   * 
   * <p>
   * The above will cause foo to be empty for a certain window of time. Using replaceAll( .. ) will
   * achieve the same end goal of clearing the current relation and then adding the passed in
   * relation, but will never result in a call to this object seeing an empty relation.
   * </p>
   * 
   * <p>
   * This call will only block reads very briefly while switching to the new cached relation and
   * notifying listeners. If deferDatabaseUpdates is false, then this blocking extends until the
   * database writes have completed.
   * </p>
   * 
   * <p>
   * If called with a relation that is equivalent to the current relation, this function will
   * immediately return with a value of false and will not hit the db or notify listeners.
   * </p>
   * 
   * @param relationToReplace the pairs to be added
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean replaceAll(LongRelation relationToReplace, boolean updateDatabase,
      boolean notifyListeners, boolean notifyDistributionListeners);

  /**
   * Removes the given pairs from the relation and updates the database.
   * 
   * @param relationToRemove the pairs to be removed
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean removeAll(LongRelation relationToRemove, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners);

  /**
   * Clears the relation of all pairs.
   * 
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   */
  void clear(boolean updateDatabase, boolean notifyListeners, boolean notifyDistributionListeners);

  /**
   * Adds the given pairs to the relation and updates the database.
   * 
   * @param relationToAdd the pairs to be added
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean addAll(LongRelation relationToAdd, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners);

  /**
   * Adds the specified pair to the relation.
   * 
   * @param leftID the ID of the left value of the pair to be added
   * @param rightID the ID of the right value of the pair to be added
   * @param updateDatabase whether to update the database
   * @param notifyListeners whether to notify the listeners of the change
   * @param notifyDistributionListeners Whether to notify any DistributionListeners; only used When
   *        notifyListeners is true.
   * @return <tt>true</tt> if the relation changed as a result of the call
   */
  boolean add(long leftID, long rightID, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners);
}
