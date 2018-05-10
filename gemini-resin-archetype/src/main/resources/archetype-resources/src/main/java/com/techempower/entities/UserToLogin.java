package com.techempower.entities;

import com.techempower.data.*;
import com.techempower.data.annotation.*;
import com.techempower.gemini.pyxis.*;

@Relation
public class UserToLogin
  implements EntityRelationDescriptor<User, Login>
{
  @Left
  User userId;
  
  @Right
  Login loginId;
}
