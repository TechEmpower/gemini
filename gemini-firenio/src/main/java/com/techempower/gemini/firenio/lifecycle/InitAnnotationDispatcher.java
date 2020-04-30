package com.techempower.gemini.firenio.lifecycle;

import com.techempower.gemini.Dispatcher;
import com.techempower.gemini.GeminiApplication;
import com.techempower.gemini.lifecycle.InitializationTask;
import com.techempower.gemini.firenio.path.AnnotationDispatcher;

/**
 * Initializes the AnnotationDispatcher, if one is enabled within the
 * application.
 */
public class InitAnnotationDispatcher implements InitializationTask {
    @Override
    public void taskInitialize(GeminiApplication application)
    {
        final Dispatcher dispatcher = application.getDispatcher();
        if (dispatcher != null && dispatcher instanceof AnnotationDispatcher)
        {
            ((AnnotationDispatcher)dispatcher).initialize();
        }
    }
}
