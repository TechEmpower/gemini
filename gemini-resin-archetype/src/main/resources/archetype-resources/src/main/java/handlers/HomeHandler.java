package ${package}.handlers;

import com.techempower.gemini.*;
import com.techempower.gemini.path.*;
import com.techempower.gemini.path.annotation.*;

public class HomeHandler extends MethodSegmentHandler<Context>
{

  public HomeHandler(GeminiApplication app)
  {
    super(app);
  }

  @PathRoot
  @Get
  public boolean home()
  {
    return mustache("home");
  }

  @PathSegment("example")
  @Get
  public boolean example()
  {
    return mustache("example");
  }
  
  @PathDefault
  public boolean notFound()
  {
    return notFound("Not found");
  }
}
