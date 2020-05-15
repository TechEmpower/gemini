package com.techempower.cache;

import java.sql.*;
import java.util.*;

import org.slf4j.*;

import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.util.*;

import gnu.trove.map.*;

/**
 * A variation on CacheGroup that only caches a subset of records. All the
 * normal operations work on all records, but it only loads into memory those
 * records returned by the provided initialization query.
 * <p>
 * If no query is provided to setInitializeCacheQuery() then everything is
 * cached and this behaves like a normal CacheGroup with worse performance.
 * (This is because it calls various raw* methods in EntityGroup, assuming that
 * the requested entity exists but isn't part of the cached subset.)
 * <p>
 * Notes:
 * <ul>
 * <li>contains() is not overridden as it doesn't exist in EntityGroup. You are
 * testing whether the *cache* contains it.
 * <li>size() uses CacheGroup's implementation, and so returns the cached size.
 * <li>Outside of initialization, nothing automatically adds entities to the
 * cache or removes them from the cache. You must manage that yourself.
 * </ul>
 */
public class SubsetCacheGroup<T extends Identifiable> extends CacheGroup<T>
{
  private String queryInitializeCache = null;
  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Keep same Constructor/Builder signature as CacheGroup to be simple to swap
   * in.
   */
  protected SubsetCacheGroup(EntityStore controller, Class<T> type, String table, String id, EntityMaker<T> maker,
      Comparator<? super T> comparator, String where, String[] whereArguments, boolean readOnly, boolean distribute)
  {
    super(controller, type, table, id, maker, comparator, where, whereArguments, readOnly, distribute);
  }

  /**
   * Needed so we use our SubsetCacheGroup builder instead of the CacheGroup
   * builder.
   */
  public static <T extends Identifiable> Builder<T> of(Class<T> type)
  {
    return new Builder<>(type);
  }

  /**
   * Overridden to only pull in the subset of objects intended to be cached.
   */
  @Override
  protected List<T> fetchAllPersistedObjects()
  {
    if (StringHelper.isEmptyTrimmed(queryInitializeCache)) {
      // No special query provided so act like a normal CacheGroup.
      log.warn(
          "Initializing {} with all entities because no query provided to initialize cache. Recommend either providing a query to setQueryInitializeCache() or using a normal CacheGroup.",
          this.getType());
      return super.fetchAllPersistedObjects();
    }
    try {
      // Use provided query to initialize cache.
      return query(queryInitializeCache);
    } catch (SQLException e) {
      throw new EntityException("Exception during SubsetCacheGroup (list).", e);
    }
  }

  /**
   * Log statistics about how many entities are cached.
   */
  @Override
  protected void customPostInitialization()
  {
    log.info("{} initialized. Caching {} out of {} entities.", this.getType(), size(), rawSize());
    super.customPostInitialization();
  }

  /**
   * Query used to initialize in-memory cache.
   * 
   * @param queryInitializeCache
   */
  public void setQueryInitializeCache(String queryInitializeCache)
  {
    this.queryInitializeCache = queryInitializeCache;
  }

  /**
   * Try to pull requested ID from the cache, but fetch un-cached if needed.
   */
  @Override
  public T get(long id)
  {
    T o = super.get(id);
    if (o == null) {
      log.trace("{} map() calling rawGet() because not found in cache: {}", this.getType(), id);
      return rawGet(id);
    }
    return o;
  }

  /**
   * Include everything, cached and un-cached.
   */
  @Override
  public List<T> list()
  {
    return rawList();
  }

  /**
   * Try to pull requested IDs from the cache, but fetch un-cached as needed.
   */
  @Override
  public List<T> list(Collection<Long> ids)
  {
    // Get what we already have in our cache.
    List<T> toReturn = super.list(ids);

    // Find any IDs not cached.
    Collection<Long> missingIds = new ArrayList<>(ids);
    for (Identifiable o : toReturn) {
      missingIds.remove(o.getId());
    }

    // Fetch missing entities and add it to our list to return.
    if (!missingIds.isEmpty()) {
      log.trace("{} map() calling rawList() because not found in cache: {}", this.getType(), missingIds);
      toReturn.addAll(rawList(missingIds));
    }
    return toReturn;
  }

  /**
   * Include everything, cached and un-cached.
   */
  @Override
  public TLongObjectMap<T> map()
  {
    return rawMap();
  }

  /**
   * Try to pull requested IDs from the cache, but fetch un-cached as needed.
   */
  @Override
  public TLongObjectMap<T> map(Collection<Long> ids)
  {
    // Get what we already have in our cache.
    TLongObjectMap<T> toReturn = super.map(ids);

    // Find any IDs not cached.
    Collection<Long> missingIds = new ArrayList<>(ids);
    for (Long id : toReturn.keys()) {
      missingIds.remove(id);
    }

    // Fetch missing entities and add it to our list to return.
    if (!missingIds.isEmpty()) {
      log.trace("{} map() calling rawMap() because not found in cache: {}", this.getType(), missingIds);
      toReturn.putAll(rawMap(missingIds));
    }
    return toReturn;
  }

  /**
   * We override this to ensure that we only ask superclass to remove from cache
   * objects that are already in there. We want this call to be as inexpensive as
   * possible in case it is called often.
   */
  @Override
  public boolean removeFromCache(long... ids)
  {
    List<Long> toRemove = new ArrayList<>();
    // Build list of IDs that are cached, and should be removed.
    for (long id : ids) {
      if (this.contains(id)) {
        toRemove.add(id);
      }
    }

    // Only call super if we have IDs that need to be removed.
    if (!toRemove.isEmpty()) {
      return super.removeFromCache(CollectionHelper.toLongArray(toRemove));
    } else {
      // Indicating whether the cache was modified.
      return false;
    }
  }

  /**
   * Use EntityGroup's implementation to include both cached and un-cached.
   */
  @Override
  public long lowest()
  {
    return rawLowest();
  }

  /**
   * Use EntityGroup's implementation to include both cached and un-cached.
   */
  @Override
  public long highest()
  {
    return rawHighest();
  }

  @Override
  public String toString()
  {
    return "SubsetCacheGroup [" + name() + "; ro: " + this.readOnly() + "; distribute: " + this.distribute() + "]";
  }

  /**
   * Creates new instances of {@code SubsetCacheGroup}.
   */
  public static class Builder<T extends Identifiable> extends CacheGroup.Builder<T>
  {
    protected Builder(Class<T> type)
    {
      super(type);
    }

    @Override
    public SubsetCacheGroup<T> build(EntityStore controller)
    {
      if (controller == null) {
        throw new NullPointerException();
      }
      return new SubsetCacheGroup<>(controller, this.type, this.table, this.id, this.maker, this.comparator, this.where,
          this.whereArguments, this.readOnly, this.distribute);
    }
  }
}
