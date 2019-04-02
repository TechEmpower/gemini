package ${package}.handlers;

import com.techempower.gemini.*;
import com.techempower.gemini.path.annotation.*;
import com.techempower.gemini.pyxis.handler.*;

import ${package}.entities.*;

public class UserHandler extends SecureMethodUriHandler<Context, User>
{

  public UserHandler(GeminiApplication app)
  {
    super(app, "hUsr");
  }

  @Path
  @Get
  public boolean home()
  {
    return mustache("user");
  }

}
