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

import java.util.*;

import com.techempower.data.*;
import com.techempower.util.*;

/**
 * A Pure-Memory Entity Group is a specialization of the standard CacheGroup
 * that does away entirely with the persistence of Entities in a database.
 * Instead, client code is expected to provide an Initializer reference that
 * is capable of providing the initial Entities.
 *   <p>
 * The use case for a PureMemoryGroup is the maintenance of derived data
 * structures, or any sort of application state that does not warrant 
 * persistence, while retaining EntityGroup / EntityStore semantics for
 * put/get/etc. and listener notifications. 
 */
public class PureMemoryGroup<T extends Identifiable>
     extends CacheGroup<T>
{
  //
  // Member variables.
  //
  
  private final GroupInitializer<T> initializer;
  
  //
  // Constructors.
  //
  
  /**
   * Constructor.
   */
  protected PureMemoryGroup(EntityStore entityStore, 
      Class<T> type,
      EntityMaker<T> maker, 
      Comparator<? super T> comparator, 
      GroupInitializer<T> initializer,
      boolean readOnly,
      boolean distribute)
  {
    super(entityStore, type, null, null, maker, comparator, null, null, 
        readOnly, distribute);
    this.initializer = initializer;
  }
  
  // 
  // Builder methods.
  // 

  /**
   * Creates a new {@link Builder}, which is used to construct an
   * {@link PureMemoryGroup}.  Example usage:
   * 
   * <pre>
   * PureMemoryGroup&lt;Foo&gt; = PureMemoryGroup.of(Foo.class) // new Builder
   *     .build(entityStore); // new CacheGroup
   * </pre>
   * 
   * <p>Note that a {@link EntityStore#register(EntityGroup.Builder)} method
   * exists, so in the common case where you only want to register the group and
   * don't care to retain your own reference to it, calling
   * {@code .build(entityStore)} is unnecessary.  For example:
   * 
   * <pre>
   * register(CacheGroup.of(Foo.class) // new Builder
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
  // Member methods.
  //
  
  /**
   * Specialization of CacheGroup.putPersistent to disable the persistence
   * of entities.
   */
  @Override
  protected void putPersistent(T object)
  {
    // Does nothing.
  }
  
  /**
   * Specialization of CacheGroup.putAllPersistent to disable the persistence
   * of entities.
   */
  @Override
  protected void putAllPersistent(Collection<T> objects)
  {
    // Does nothing.
  }

  /**
   * Specialization of CacheGroup.removePersistent to disable the persistence
   * of entities.
   */
  @Override
  protected void removePersistent(long id)
  {
    // Does nothing.
  }

  /**
   * Specialization of CacheGroup.removeAllPersistent to disable the 
   * persistence of entities.
   */
  @Override
  protected void removeAllPersistent(Collection<Long> ids)
  {
    // Does nothing.
  }
  
  /**
   * Fetches all persisted objects.  Call the provided Initializer or return
   * an empty list in the default case.
   */
  @Override
  protected List<T> fetchAllPersistedObjects()
  {
    // We sort whatever the initializer gives us because there is no
    // guarantee that it's in the right order.
    List<T> result = initializer.list();
    Collections.sort(result, comparator());
    return result;
  }
  
  /**
   * The Refresh method does nothing for the PureMemoryGroup because once 
   * initialized, there is no other source from which to fetch objects.
   */
  @Override
  public void refresh(long... ids)
  {
    // Does nothing.
  }

  @Override
  protected boolean isPersisted(Identifiable entity)
  {
    // We consider anything that the map contains to have been "persisted"
    // for our purposes.
    return containsRaw(entity);
  }

  @Override
  public String toString()
  {
    return "PureMemoryGroup [" + name() + "; ro: " + this.readOnly() + "; distribute: " + this.distribute() + "]";
  }

  //
  // Inner classes.
  //

  /**
   * Creates new instances of {@code PureMemoryGroup}.
   */
  public static class Builder<T extends Identifiable>
      extends CacheGroup.Builder<T>
  {
    private static final String ERROR_NO_INITIALIZER = "CacheInitializer may not be null.";
    
    private GroupInitializer<T> initializer = new EmptyInitializer<>();
    
    protected Builder(Class<T> type)
    {
      super(type);
    }
    
    @Override
    public PureMemoryGroup<T> build(EntityStore entityStore)
    {
      return new PureMemoryGroup<>(
          entityStore,
          this.type,
          this.maker,
          this.comparator,
          this.initializer,
          this.readOnly,
          this.distribute);
    }

    /**
     * Sets the GroupInitializer to pre-populate this group with a list of
     * entities.  If no initializer is specified, the group will be set to
     * an empty list during initialization.
     */
    public Builder<T> initializer(GroupInitializer<T> groupInitializer)
    {
      if (groupInitializer == null)
      {
        throw new IllegalArgumentException(ERROR_NO_INITIALIZER);
      }
      this.initializer = groupInitializer;
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
    public Builder<T> constructorArgs(Object... arguments)
    {
      super.constructorArgs(arguments);
      return this;
    }

  } // End Builder.
}
