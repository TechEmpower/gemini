package com.techempower.entities;

import com.fasterxml.jackson.annotation.*;
import com.techempower.util.*;

/**
 * The base class for all data entities in the Application. This class provides
 * the implementation of the Identifiable interface expected of all entities,
 * cached or otherwise.
 */
public abstract class DataEntity
  implements Identifiable
{
    /**
     * The identity for this object.
     */
    private long id;

    @JsonProperty("id")
    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public void setId(long newIdentity) {
        this.id = newIdentity;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getId() + "]";
    }

}
