package com.techempower.gemini.firenio;

import com.techempower.gemini.Context;
import com.techempower.gemini.GeminiApplication;
import com.techempower.gemini.Request;
import com.techempower.gemini.context.Attachments;

public class FirenioContext extends Context {

    public FirenioContext(Request request, GeminiApplication application)
    {
        super(application, request);
    }

    @Override
    public Attachments files() {
        // FIXME
        return null;
    }
}
