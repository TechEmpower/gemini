package com.techempower;

import com.techempower.cache.*;
import com.techempower.entities.*;
import com.techempower.gemini.pyxis.*;

/**
 * Security provides Pyxis-based security services for the
 * application.
 */
public class Security extends BasicSecurity<User,Group>
{
    public final EntityStore store;

    /**
     * Constructor.
     */
    public Security(Application application)
    {
      super(application, User.class, Group.class, UserToGroup.class,
          UserToLogin.class);

      this.store = application.getStore();
    }

    //
    // Member methods.
    //

    @Override
    public User constructUser() {
        return new User(this);
    }

    @Override
    public Group constructUserGroup() {
        return new Group();
    }

    @Override
    public Group getUserGroup(String name) {
        // Use the EntityStore's ability to find objects by method call.
        return this.store.get(Group.class, "getName", name);
    }

    @Override
    public Group getUserGroup(long identity) {
        return this.store.get(Group.class, identity);
    }

}
