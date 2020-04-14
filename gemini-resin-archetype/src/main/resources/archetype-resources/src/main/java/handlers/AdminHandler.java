package ${package}.handlers;

import com.techempower.gemini.*;
import com.techempower.gemini.path.annotation.*;
import com.techempower.gemini.pyxis.authorization.*;
import com.techempower.gemini.pyxis.handler.*;
import ${package}.entities.*;

public class AdminHandler extends SecureMethodUriHandler<BasicContext, User>
{

  public AdminHandler(GeminiApplication app)
  {
    super(app, "hAdm", new AuthorizerByAdmin(), app.getSecurity().getForceLoginRejector());
  }

  @Path
  @Get
  public boolean home()
  {
    return mustache("admin");
  }

}
