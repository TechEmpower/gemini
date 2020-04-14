package ${package};

import com.techempower.data.*;
import com.techempower.data.jdbc.*;
import com.techempower.gemini.*;
import com.techempower.gemini.exceptionhandler.*;
import com.techempower.gemini.path.*;
import com.techempower.gemini.pyxis.*;
import com.techempower.gemini.pyxis.handler.*;
import ${package}.handlers.*;

/**
 * Application. As a subclass of GeminiApplication, this class acts as
 * the central "hub" of references to components such as the Dispatcher,
 * Security, and EntityStore.
 */
public class Application
    extends ResinGeminiApplication
{

  //
  // Static variables.
  //

  private static final Application INSTANCE = new Application();

  //
  // Constructor
  //

  /**
   * Constructor. This method can be extended to construct references to
   * custom components for the application.
   */
  public Application()
  {
    super();
  }
  
  //
  // Static methods
  //

  /**
   * Get the single running instance of this application.
   */
  public static Application getInstance()
  {
    return Application.INSTANCE;
  }

  //
  // Public methods
  //
  
  /**
   * Return and application-specific reference to the Security.
   */
  @Override
  public Security getSecurity()
  {
    return (Security)super.getSecurity();
  }

  //
  // Protected methods.
  //

  /**
   * Constructs a Security reference. This is overloaded to return a custom
   * Security.
   */
  @Override
  protected PyxisSecurity constructSecurity()
  {
    return new Security(this);
  }

  /**
   * Constructs a Dispatcher.
   */
  @Override
  protected Dispatcher constructDispatcher()
  {
    final PathDispatcher.Configuration<BasicContext> config =
        new PathDispatcher.Configuration<BasicContext>();

    // Handlers
    config
      .setDefault(new HomeHandler(this))
      .add("user", new UserHandler(this))
      .add("admin", new AdminHandler(this))
      .add("login", new LoginHandler<BasicContext>(this))
      .add("logout", new LogoutHandler<BasicContext>(this));
    
    // Add ExceptionHandlers
    config.add(new BasicExceptionHandler(this));

    return new PathDispatcher<>(this, config);
  }

  @Override
  protected ConnectorFactory constructConnectorFactory()
  {
    return new BasicConnectorFactory(this, null);
    // To use HikariCP, also add gemini-hikaricp to the pom.xml.
    //return new HikariCPConnectorFactory(this, null);
  }
}

