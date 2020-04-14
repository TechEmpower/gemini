package com.techempower.gemini;

/**
 * Implementation of Request that's specific to HttpServlet. This request is a
 * wrapper for HttpServletRequest and HttpServletResponse.
 */
public interface HttpRequest extends Request {
    enum HttpMethod
    {
        GET,
        POST,
        PUT,
        DELETE,
        OPTIONS,
        TRACE,
        CONNECT,
        PATCH,
        HEAD;
    }

    String
            // Http Header names
            HEADER_AUTHORIZATION                    = "Authorization",
            HEADER_ORIGIN                           = "Origin",
            HEADER_VARY                             = "Vary",
            HEADER_WILDCARD                         = "*",
            HEADER_ACCESS_CONTROL_ALLOW_ORIGIN      = "Access-Control-Allow-Origin",
            HEADER_ACCESS_CONTROL_REQUEST_METHOD    = "Access-Control-Request-Method",
            HEADER_ACCESS_CONTROL_ALLOW_METHOD      = "Access-Control-Allow-Methods",
            HEADER_ACCESS_CONTROL_EXPOSED_HEADERS   = "Access-Control-Exposed-Headers",
            HEADER_ACCESS_CONTROL_REQUEST_HEADERS   = "Access-Control-Request-Headers",
            HEADER_ACCESS_CONTROL_ALLOW_HEADERS     = "Access-Control-Allow-Headers",
            HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials",
            HEADER_ACCESS_CONTROL_MAX_AGE           = "Access-Control-Max-Age";

    /**
     * Gets the request's method as a String.
     */
    HttpMethod getRequestMethod();
}
