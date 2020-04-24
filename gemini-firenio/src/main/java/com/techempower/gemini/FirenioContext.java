package com.techempower.gemini;

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
