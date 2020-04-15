/*******************************************************************************
 * Copyright (c) 2020, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
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
