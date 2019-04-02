package com.techempower;

import com.techempower.gemini.transport.*;
import javax.servlet.annotation.MultipartConfig;

/**
 * Interface to the application. 
 */
@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 20, maxRequestSize = 1024 * 1024 * 50)
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
