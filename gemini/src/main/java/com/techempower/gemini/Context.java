package com.techempower.gemini;

import com.techempower.gemini.context.*;
import com.techempower.gemini.session.Session;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

public interface Context {

    /**
     * Gets the client's identifier, for web requests, this will be the client's
     * IP address.
     */
    String getClientId();

    /**
     * Returns the current URI of the request.
     */
    String getCurrentUri();

    /**
     * Gets the time since the start of this Context, in milliseconds.
     */
    long getDuration();

    /**
     * Gets a user session's locale.  If no locale it set, the default locale
     * is returned.  This is merely a pass-through convenience method for
     * calling localeManager.getLocale.
     *   <p>
     * In a locale-aware application, it is more typical to just call
     * getResources to get a reference to the GeminiResources to use for this
     * user's current request.
     */
    Locale getLocale();

    /**
     * Gets a reference to the output stream from the response.  This is a
     * pass-through to response.getOutputStream().
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Gets the query string of the request.
     */
    String getQueryString();

    /**
     * Pass-though to request.getRealPath.
     */
    String getRealPath(String path);

    /**
     * Get the associated Request.
     */
    Request getRequest();

    /**
     * Gets the current request's "signature;" that is, the URL including all
     * of the parameters.  This will construct a URL that will always use a
     * "GET" form even if the request itself was POSTed.
     */
    String getRequestSignature();

    /**
     * Returns the request URI.  A URI is the portion of a URI after the
     * protocol and domain.  The URL http://techempower.com/admin/users has a
     * URI of admin/users.
     */
    String getRequestUri();

    /**
     * Get a named-value interface to the Session.
     */
    SessionNamedValues session();

    /**
     * Gets the request's file attachments.
     */
    Attachments files();

    /**
     * Get a named-value interface to the Delivery map.
     */
    Delivery delivery();

    /**
     * Gets an interface for working with Cookies.
     */
    Cookies cookies();

    /**
     * Gets an interface for working with request and response Headers.
     */
    Headers headers();

    /**
     * Gets an interface for working with session Messages.
     */
    Messages messages();

    /**
     * Gets an interface for working with the request's query parameters.
     */
    Query query();

    /**
     * Gets the associated user session.
     *
     * @param create Whether to force the creation of a session. In general,
     * pass false. When you go to store a session value, it will create a
     * session at that point if necessary.
     */
    Session getSession(boolean create);

    /**
     * Gets the response writer.
     * @return
     * @throws IOException
     */
    PrintWriter getWriter() throws IOException;

    /**
     * Returns whether or not the response has been committed. This means that
     * you can no longer write to the outputstream.
     */
    boolean isCommitted();

    /**
     * Is the session new?  This method returns true if the Session was marked
     * as new by the Servlet API.
     */
    boolean isNewSession();

    /**
     * Is the request a HEAD request.
     */
    boolean isHead();

    /**
     * Is the request a GET request.
     */
    boolean isGet();

    /**
     * Is the request a POST request.
     */
    boolean isPost();

    /**
     * Is the request a PUT request.
     */
    boolean isPut();

    /**
     * Is the request a DELETE request.
     */
    boolean isDelete();

    /**
     * Is the request a TRACE request.
     */
    boolean isTrace();

    /**
     * Is the request an OPTIONS request.
     */
    boolean isOptions();

    /**
     * Is the request a CONNECT request.
     */
    boolean isConnect();

    /**
     * Is the request a PATCH request.
     */
    boolean isPatch();

    /**
     * Is a PriorRequest bound?  Determine if a PriorRequest has been
     * bound to this Context by a previous call to bindPriorRequest.
     */
    boolean isPriorRequestBound();

    /**
     * Return whether the current request is secure.
     */
    boolean isSecure();

    /**
     * Output text directly to the response via a PrintWriter.  This is not
     * intended for extensive use, but rather for quick debugging purposes.
     * If an application wants to extensively interact with the response
     * directly, use of getResponse is preferred.
     */
    void print(String text);
}
