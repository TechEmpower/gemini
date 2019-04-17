package ${package};

import javax.servlet.annotation.*;

import com.techempower.gemini.transport.*;

@SuppressWarnings("serial")
@WebServlet(name="DS", urlPatterns="*")
public class DefaultServlet
     extends InfrastructureServlet
{
  /**
   * Gets a GeminiApplication object for this application.
   */
  @Override
  public Application getApplication()
  {
    return Application.getInstance();
  }

}