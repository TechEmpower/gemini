package com.techempower.entities;

import com.techempower.data.*;
import com.techempower.data.annotation.*;

/**
 * Represents a user group relation for the application.
 */
@Relation
public class UserToGroup
  implements EntityRelationDescriptor<User, Group>
{
    @Left
    User userId;

    @Right
    Group groupId;
}
