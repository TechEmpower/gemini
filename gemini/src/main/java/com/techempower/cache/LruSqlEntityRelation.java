package com.techempower.cache;

import java.util.*;
import com.google.common.cache.*;
import com.techempower.collection.relation.*;
import com.techempower.data.*;
import com.techempower.gemini.cluster.*;
import com.techempower.util.*;

/**
 * A least-recently-used style caching EntityRelation based on the Guava library's Cache. The guts
 * are provided by Cache but the usage semantics are similar to CachedRelation.
 */
public class LruSqlEntityRelation<L extends Identifiable, R extends Identifiable>
    extends SqlEntityRelation<L, R> implements CachingEntityRelation<L, R> {

  /**
   * Creates a new {@link Builder}, which is used to construct a {@link LruSqlEntityRelation}. See
   * example usage in {@link SqlEntityRelation}.
   */
  public static <L extends Identifiable, R extends Identifiable> Builder<L, R> of(Class<L> leftType,
      Class<R> rightType) {
    return new LruSqlEntityRelation.Builder<>(leftType, rightType);
  }

  /**
   * A unique identifier for this relation to be assigned by the entity store as the relation is
   * registered.
   */
  private long id;

  /**
   * Our LRU cache supports Many-to-Many.
   */
  private final Cache<Long, Set<Long>> leftMap;
  private final Cache<Long, Set<Long>> rightMap;
  private final Collection<CachedRelationListener> listeners = new ArrayList<>();

  /**
   * Constructor.
   */
  protected LruSqlEntityRelation(EntityStore store, Class<L> leftType, Class<R> rightType,
      String tableName, String leftColumn, String rightColumn, int lruCacheSize) {
    super(store, leftType, rightType, tableName, leftColumn, rightColumn);
    this.leftMap = CacheBuilder.newBuilder().maximumSize(lruCacheSize).build();
    this.rightMap = CacheBuilder.newBuilder().maximumSize(lruCacheSize).build();
  }

  /**
   * Tries to satisfy first from the LRU cache, only querying the database if required.
   */
  @Override
  public Set<Long> rightIDs(long leftID) {
    Set<Long> rightSet = leftMap.getIfPresent(leftID);
    if (rightSet != null) {
      return rightSet;
    } else {
      rightSet = super.rightIDs(leftID);
      if (!rightSet.isEmpty()) {
        leftMap.put(leftID, rightSet);
      }
      return rightSet;
    }
  }

  /**
   * Tries to satisfy first from the LRU cache, only querying the database if required.
   */
  @Override
  public Set<Long> leftIDs(long rightID) {
    Set<Long> leftSet = rightMap.getIfPresent(rightID);
    if (leftSet != null) {
      return leftSet;
    } else {
      leftSet = super.leftIDs(rightID);
      if (!leftSet.isEmpty()) {
        rightMap.put(rightID, leftSet);
      }
      return leftSet;
    }
  }

  @Override
  public boolean remove(long leftID, long rightID, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners) {
    // Invalidate the requested items from our LRU cache. This is potentially invalidating more than
    // required (in the case of many-to-many relations) but it's not worth the complexity to be
    // exact about these invalidations.
    this.leftMap.invalidate(leftID);
    this.rightMap.invalidate(rightID);
    boolean toReturn = false; // Not important for this to be strictly accurate.
    if (updateDatabase) {
      toReturn = super.remove(leftID, rightID);
    }
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.remove(this.id, leftID, rightID);
        }
      }
    }
    return toReturn;
  }

  @Override
  public boolean removeLeftValue(long leftID, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners) {
    boolean toReturn = false; // Not important for this to be strictly accurate.
    // Invalidate relevant values in our LRU cache.
    Set<Long> rightValues = this.leftMap.getIfPresent(leftID);
    if (rightValues != null && !rightValues.isEmpty()) {
      for (Long rightId : rightValues) {
        Set<Long> leftValues = this.rightMap.getIfPresent(rightId);
        leftValues.remove(leftID);
        if (leftValues.isEmpty()) {
          this.rightMap.invalidate(rightId);
        }
      }
      this.leftMap.invalidate(leftID);
      toReturn = true;
    }
    if (updateDatabase) {
      toReturn = super.removeLeftValue(leftID);
    }
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.removeLeftValue(this.id, leftID);
        }
      }
    }
    return toReturn;
  }

  @Override
  public boolean removeRightValue(long rightID, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners) {
    boolean toReturn = false; // Not important for this to be strictly accurate.
    // Invalidate relevant values in our LRU cache.
    Set<Long> leftValues = this.rightMap.getIfPresent(rightID);
    if (leftValues != null && !leftValues.isEmpty()) {
      for (Long leftId : leftValues) {
        Set<Long> rightValues = this.leftMap.getIfPresent(leftId);
        rightValues.remove(rightID);
        if (rightValues.isEmpty()) {
          this.leftMap.invalidate(leftId);
        }
      }
      this.rightMap.invalidate(rightID);
      toReturn = true;
    }
    if (updateDatabase) {
      toReturn = super.removeRightValue(rightID);
    }
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.removeRightValue(this.id, rightID);
        }
      }
    }
    return toReturn;
  }

  @Override
  public boolean replaceAll(LongRelation relationToReplace, boolean updateDatabase,
      boolean notifyListeners, boolean notifyDistributionListeners) {
    // Invalidate our LRU cache.
    this.leftMap.invalidateAll();
    this.rightMap.invalidateAll();
    boolean toReturn = false; // Not important for this to be strictly accurate.
    if (updateDatabase) {
      toReturn = super.replaceAll(relationToReplace);
    }
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.replaceAll(this.id, relationToReplace);
        }
      }
    }
    return toReturn;
  }

  @Override
  public void reset(boolean notifyListeners, boolean notifyDistributionListeners) {
    // Invalidate our LRU cache.
    this.leftMap.invalidateAll();
    this.rightMap.invalidateAll();
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.reset(this.id);
        }
      }
    }
  }

  @Override
  public <T extends Identifiable> void reset(Class<T> type, boolean notifyListeners,
      boolean notifyDistributionListeners) {
    reset(notifyListeners, notifyDistributionListeners);
  }

  @Override
  public <T extends Identifiable> void reset(Class<T> type) {
    reset(true, true);
  }

  @Override
  public void addListener(CachedRelationListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public List<CachedRelationListener> listeners() {
    return new ArrayList<>(this.listeners);
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setId(long identity) {
    this.id = identity;
  }

  @Override
  public boolean removeAll(LongRelation relationToRemove, boolean updateDatabase,
      boolean notifyListeners, boolean notifyDistributionListeners) {
    // Invalidate our LRU cache.
    this.leftMap.invalidateAll();
    this.rightMap.invalidateAll();
    boolean toReturn = false; // Not important for this to be strictly accurate.
    if (updateDatabase) {
      toReturn = super.removeAll(relationToRemove);
    }
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.removeAll(this.id, relationToRemove);
        }
      }
    }
    return toReturn;
  }

  @Override
  public void clear(boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners) {
    // Invalidate our LRU cache.
    this.leftMap.invalidateAll();
    this.rightMap.invalidateAll();
    if (updateDatabase) {
      super.clear();
    }
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.clear(this.id);
        }
      }
    }
  }

  @Override
  public boolean addAll(LongRelation relationToAdd, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners) {
    if (relationToAdd == null) {
      return false;
    }
    // Do not update our LRU cache. Wait until a relationship is requested before caching it.
    boolean toReturn = false; // Not important for this to be strictly accurate.
    if (updateDatabase) {
      toReturn = super.addAll(relationToAdd);
    }
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.addAll(this.id, relationToAdd);
        }
      }
    }
    return toReturn;
  }

  @Override
  public boolean add(long leftID, long rightID, boolean updateDatabase, boolean notifyListeners,
      boolean notifyDistributionListeners) {
    // Do not update our LRU cache. Wait until a relationship is requested before caching it.
    boolean toReturn = false; // Not important for this to be strictly accurate.
    if (updateDatabase) {
      toReturn = super.add(leftID, rightID);
    }
    if (notifyListeners) {
      for (CachedRelationListener listener : this.listeners) {
        if (!(listener instanceof DistributionListener) || notifyDistributionListeners) {
          listener.add(this.id, leftID, rightID);
        }
      }
    }
    return toReturn;
  }

  @Override
  public String toString()
  {
    return "LruSqlEntityRelation ["
        + leftType().getSimpleName()
        + "," + rightType().getSimpleName()
        + "]";
  }

  /**
   * Creates new instances of {@link LruSqlEntityRelation}.
   *
   * @param <L> the type of the left values in the relation
   * @param <R> the type of the right values in the relation
   */
  public static class Builder<L extends Identifiable, R extends Identifiable>
      extends SqlEntityRelation.Builder<L, R> {
    /**
     * The default size limit is 10,000.
     */
    public static final int DEFAULT_SIZE = 10000;
    protected int lruCacheSize = DEFAULT_SIZE;

    /**
     * Returns a new builder of {@link LruSqlEntityRelation} instances.
     *
     * @param leftType The type of left objects.
     * @param rightType The type of right objects.
     */
    protected Builder(Class<L> leftType, Class<R> rightType) {
      super(leftType, rightType);
    }

    @Override
    public LruSqlEntityRelation<L, R> build(EntityStore store) {
      Objects.requireNonNull(store);
      return new LruSqlEntityRelation<>(store, this.leftType, this.rightType, this.table,
          this.leftColumn, this.rightColumn, this.lruCacheSize);
    }

    /**
     * Maximum size of the LRU cache.
     */
    public Builder<L, R> lruCacheSize(int size) {
      this.lruCacheSize = size;
      return this;
    }
  }

}
