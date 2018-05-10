package com.techempower.handlers;

import com.techempower.gemini.*;
import com.techempower.gemini.path.*;
import com.techempower.gemini.path.annotation.*;

public class HomeHandler extends MethodUriHandler<Context>
{

  public HomeHandler(GeminiApplication app)
  {
    super(app);
  }
  
  @Path
  @Get
  public boolean home()
  {
    return mustache("home");
  }
  
}
