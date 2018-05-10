package com.techempower;

import com.techempower.gemini.transport.*;

/**
 * Interface to the application. 
 */
@SuppressWarnings("serial")
public class Server
     extends InfrastructureServlet
{
    //
    // Member methods
    //

    @Override
    public Application getApplication() {
        return Application.getInstance();
    }
    
}
