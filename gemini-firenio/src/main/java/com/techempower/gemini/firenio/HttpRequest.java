package com.techempower.gemini.firenio;

import com.firenio.codec.http11.HttpFrame;
import com.firenio.codec.http11.HttpHeader;
import com.firenio.codec.http11.HttpStatus;
import com.firenio.component.Channel;
import com.firenio.component.Frame;
import com.techempower.gemini.Cookie;
import com.techempower.gemini.GeminiApplication;
import com.techempower.gemini.Infrastructure;
import com.techempower.gemini.Request;
import com.techempower.gemini.session.Session;
import com.techempower.util.UtilityConstants;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

public class HttpRequest implements Request {

    //
    // Member variables.
    //

    private final GeminiApplication application;
    private final Channel channel;
    private final HttpFrame frame;

    public HttpRequest(Channel channel, HttpFrame frame, GeminiApplication application)
    {
        this.application = application;
        this.channel = channel;
        this.frame = frame;
    }

    @Override
    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
        // fixme - this throws an error in Firenio if the service is already
        //         running. Not sure if this is intended; need to investigate.
//        channel.getContext().setCharset(Charset.forName(encoding));
    }

    @Override
    public String getRequestCharacterEncoding() {
        return channel.getCharset().displayName();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        final Vector<String> headerNames = new Vector<>();
        frame.getRequestHeaders().scan();
        while(frame.getRequestHeaders().hasNext()) {
            HttpHeader header = HttpHeader.get(frame.getRequestHeaders().key());
            headerNames.add(header.name());
        }
        return (Enumeration<String>) headerNames;
    }

    @Override
    public String getHeader(String name) {
        try {
            return frame.getRequestHeader(HttpHeader.valueOf(name));
        } catch (IllegalArgumentException iae) {
            // fixme - this is thrown when the header isn't there
            return null;
        }
    }

    @Override
    public Enumeration<String> getParameterNames() {
        final Vector<String> paramNames = new Vector<>();
        for (String param : frame.getRequestParams().values()) {
            paramNames.add(param);
        }
        return (Enumeration<String>) paramNames;
    }

    @Override
    public String getParameter(String name) {
        return frame.getRequestParam(name);
    }

    @Override
    public void putParameter(String name, String value) {
        frame.getRequestParams().put(name, value);
    }

    @Override
    public void removeParameter(String name) {
        frame.getRequestParams().remove(name);
    }

    @Override
    public void removeAllRequestValues() {
        frame.getRequestParams().clear();
    }

    @Override
    public String[] getParameterValues(String name) {
        // FIXME - not sure how HttpFrame handles multiple param values.
        return null;
    }

    @Override
    @Deprecated
    /**
     * @deprecated
     */
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public void print(String text) throws IOException {
        // fixme
        frame.setContent(text.getBytes());
    }

    @Override
    @Deprecated
    /**
     * @deprecated
     */
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    @Override
    @Deprecated
    /**
     * @deprecated
     */
    public String getRequestSignature() {
        return null;
    }

    @Override
    @Deprecated
    /**
     * @deprecated
     */
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        // fixme
        return new StringBuffer(frame.getRequestURL());
    }

    @Override
    public String getRequestURI() {
        return frame.getRequestURL();
    }

    @Override
    public <C extends Cookie> C getCookie(String name) {
        final String cookieString = frame.getRequestHeader(HttpHeader.Cookie);
        // fixme
        return null;
    }

    @Override
    public void setCookie(String name, String value, String domain, String path, int age, boolean secure) {
        // fixme
    }

    @Override
    public void deleteCookie(String name, String path) {
        //fixme
    }

    @Override
    public String getClientId() {
        return channel.getRemoteAddr();
    }

    @Override
    public HttpMethod getRequestMethod() {
        return HttpMethod.valueOf(frame.getMethod().getValue());
    }

    @Override
    @Deprecated
    /**
     * @deprecated
     */
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public boolean redirect(String redirectDestinationUrl) {
        // fixme
        return false;
    }

    @Override
    public boolean redirectPermanent(String redirectDestinationUrl) {
        // fixme
        return false;
    }

    @Override
    public void setResponseHeader(String headerName, String value) {
        frame.setResponseHeader(HttpHeader.valueOf(headerName), value.getBytes());
    }

    @Override
    @Deprecated
    /**
     * @deprecated Output stream was how blocking servers handled IO, but
     * Firenio uses the NIO message model, so by the time an HttpRequest is
     * constructed and available, the entire HttpMessage has been read.
     * @see #getChannel()
     * @see #getFrame()
     */
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    /**
     * Returns the underlying reference to HttpChannel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Returns the underlying reference to the HttpFrame
     */
    public Frame getFrame() {
        return frame;
    }

    @Override
    public String getRequestContentType() {
        return frame.getRequestHeader(HttpHeader.Content_Type);
    }

    @Override
    public void setContentType(String contentType) {
        frame.setResponseHeader(HttpHeader.Content_Type, contentType.getBytes());
    }

    @Override
    public void setExpiration(int secondsFromNow) {
        frame.setResponseHeader(HttpHeader.Expires,
                (System.currentTimeMillis() + (secondsFromNow * UtilityConstants.SECOND) + "").getBytes());
    }

    @Override
    public String getCurrentURI() {
        // fixme
        return null;
    }

    @Deprecated
    @Override
    /**
     * @deprecated
     */
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean isCommitted() {
        return !channel.isOpen();
    }

    @Override
    public String getQueryString() {
        // fixme
        return null;
    }

    @Override
    public Session getSession(boolean create) {
        // fixme
        return null;
    }

    @Override
    public void setAttribute(String name, Object o) {
        // fixme
    }

    @Override
    public Object getAttribute(String name) {
        // fixme
        return null;
    }

    @Override
    public Infrastructure getInfrastructure() {
        return application.getInfrastructure();
    }

    @Override
    public boolean isHead() {
        // fixme
        return false;
//        return frame.isHead();
    }

    @Override
    public boolean isGet() {
        return frame.isGet();
    }

    @Override
    public boolean isPost() {
        // fixme
        return false;
//        return frame.isPost();
    }

    @Override
    public boolean isPut() {
        // fixme
        return false;
//        return frame.isPut();
    }

    @Override
    public boolean isDelete() {
        // fixme
        return false;
//        return frame.isDelete();
    }

    @Override
    public boolean isTrace() {
        // fixme
        return false;
//        return frame.isTrace();
    }

    @Override
    public boolean isOptions() {
        // fixme
        return false;
//        return frame.isOptions();
    }

    @Override
    public boolean isConnect() {
        // fixme
        return false;
    }

    @Override
    public boolean isPatch() {
        // fixme
        return false;
//        return frame.isPatch();
    }

    @Override
    public void setStatus(int status) {
        frame.setStatus(HttpStatus.get(status));
    }
}
