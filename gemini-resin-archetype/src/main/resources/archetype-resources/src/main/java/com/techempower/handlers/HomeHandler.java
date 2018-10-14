package com.techempower.handlers;

import com.techempower.gemini.*;
import com.techempower.gemini.path.*;
import com.techempower.gemini.path.annotation.*;

public class HomeHandler extends MethodSegmentHandler<Context>
{

  public HomeHandler(GeminiApplication app)
  {
    super(app);
  }

  //@Path
  @PathRoot
  @Get
  public boolean home()
  {
    return mustache("home");
  }

  //@Path("example")
  @PathSegment("example")
  @Get
  public boolean example()
  {
    return mustache("example");
  }
  
  @PathDefault
  public boolean notFound()
  {
    //mustache("common/error");
    return notFound("Not found");
  }
}
