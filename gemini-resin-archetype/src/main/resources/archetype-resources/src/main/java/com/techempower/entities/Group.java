package com.techempower.entities;

import com.techempower.data.annotation.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.util.*;

/**
 * Represents a user group for the application.
 */
@CachedEntity
public class Group
      extends BasicUserGroup
      implements PersistenceAware
{

    //
    // Constructor.
    //
    public Group() {
        super();
    }

}
