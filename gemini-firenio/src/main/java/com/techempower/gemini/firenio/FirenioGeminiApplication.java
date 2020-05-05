package com.techempower.gemini.firenio;

import com.firenio.Options;
import com.firenio.codec.http11.*;
import com.firenio.collection.ByteTree;
import com.firenio.common.Util;
import com.firenio.component.*;
import com.firenio.log.DebugUtil;
import com.techempower.data.ConnectionMonitor;
import com.techempower.data.ConnectorFactory;
import com.techempower.data.DatabaseAffinity;
import com.techempower.gemini.Context;
import com.techempower.gemini.Dispatcher;
import com.techempower.gemini.GeminiApplication;
import com.techempower.gemini.Request;
import com.techempower.gemini.firenio.lifecycle.InitAnnotationDispatcher;
import com.techempower.gemini.firenio.monitor.FirenioMonitor;
import com.techempower.gemini.firenio.mustache.FirenioMustacheManager;
import com.techempower.gemini.mustache.MustacheManager;
import com.techempower.gemini.firenio.path.AnnotationDispatcher;
import com.techempower.gemini.firenio.session.HttpSessionManager;
import com.techempower.gemini.session.SessionManager;
import com.techempower.util.EnhancedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public abstract class FirenioGeminiApplication
        extends GeminiApplication {

    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Overload: Constructs an instance of a subclass of Context, provided the
     * parameters used to construct Context objects.  Note that it is NO
     * LONGER necessary to overload this method if your application is not using
     * a special subclass of Context.
     */
    @Override
    public Context getContext(Request request)
    {
        return new FirenioContext(request, this);
    }

    @Override
    protected ConnectorFactory constructConnectorFactory() {
        return new ConnectorFactory() {
            @Override
            public ConnectionMonitor getConnectionMonitor() throws SQLException {
                return null;
            }

            @Override
            public void determineIdentifierQuoteString() {

            }

            @Override
            public String getIdentifierQuoteString() {
                return null;
            }

            @Override
            public DatabaseAffinity getDatabaseAffinity() {
                return null;
            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public void configure(EnhancedProperties props) {

            }
        };
    }

    @Override
    protected Dispatcher constructDispatcher() {
        return new AnnotationDispatcher<>(this);
    }

    @Override
    protected MustacheManager constructMustacheManager() {
        return new FirenioMustacheManager(this);
    }

    @Override
    protected SessionManager constructSessionManager() {
        return new HttpSessionManager(this);
    }

    @Override
    protected FirenioMonitor constructMonitor() {
        return new FirenioMonitor(this);
    }

    /**
     * Starts the FirenioGeminiApplication
     * todo
     *
     * @throws Exception
     */
    public final void start(String serverName, int port, int backlog) throws Exception {
        boolean lite      = Util.getBooleanProperty("lite");
        boolean read      = Util.getBooleanProperty("read");
        boolean pool      = Util.getBooleanProperty("pool");
        boolean epoll     = Util.getBooleanProperty("epoll");
        boolean direct    = Util.getBooleanProperty("direct");
        boolean inline    = Util.getBooleanProperty("inline");
        boolean nodelay   = Util.getBooleanProperty("nodelay");
        boolean cachedurl = Util.getBooleanProperty("cachedurl");
        boolean unsafeBuf = Util.getBooleanProperty("unsafeBuf");
        int     core      = Util.getIntProperty("core", 1);
        int     frame     = Util.getIntProperty("frame", 16);
        int     level     = Util.getIntProperty("level", 1);
        int     readBuf   = Util.getIntProperty("readBuf", 16);
        Options.setBufAutoExpansion(false);
        Options.setChannelReadFirst(read);
        Options.setEnableEpoll(epoll);
        Options.setEnableUnsafeBuf(unsafeBuf);
        log.info("lite: {}", lite);
        log.info("read: {}", read);
        log.info("pool: {}", pool);
        log.info("core: {}", core);
        log.info("epoll: {}", epoll);
        log.info("frame: {}", frame);
        log.info("level: {}", level);
        log.info("direct: {}", direct);
        log.info("inline: {}", inline);
        log.info("readBuf: {}", readBuf);
        log.info("nodelay: {}", nodelay);
        log.info("cachedurl: {}", cachedurl);
        log.info("unsafeBuf: {}", unsafeBuf);

        final FirenioGeminiApplication thiss = this;
        this.getLifecycle().addInitializationTask(new InitAnnotationDispatcher());
        // Initialize the application.
        this.initialize(null);

        int slept = 0;
        int maxSleep = 10_000;
        while (slept < maxSleep && !this.isRunning()) {
            // Wait until the application is initialized and configured.
            slept += 1000;
            Thread.sleep(1000);
        }

        if (slept > maxSleep && !this.isRunning()) {
            log.error("Failed to start Gemini application after {} seconds", slept);
            return;
        }

        final IoEventHandle eventHandleAdaptor = new IoEventHandle() {

            @Override
            public void accept(Channel channel, Frame frame) throws Exception {
                final HttpFrame httpFrame = (HttpFrame) frame;
                final HttpRequest request = new HttpRequest(channel, httpFrame, thiss);
                final FirenioContext context = new FirenioContext(request, thiss);
                getDispatcher().dispatch(context);

                // fixme - content type shouldn't be set here
//                httpFrame.setContentType(HttpContentType.text_plain);
                httpFrame.setConnection(HttpConnection.NONE);
                httpFrame.setDate(HttpDateUtil.getDateLine());
                channel.writeAndFlush(httpFrame);
                channel.release(httpFrame);
            }
        };

        int fcache    = 1024 * 16;
        int pool_cap  = 1024 * 128;
        int pool_unit = 256;
        if (inline) {
            pool_cap = 1024 * 8;
            pool_unit = 256 * 16;
        }
        HttpDateUtil.start();
        NioEventLoopGroup group   = new NioEventLoopGroup();
        ChannelAcceptor   context = new ChannelAcceptor(group, port);
        group.setMemoryPoolCapacity(pool_cap);
        group.setEnableMemoryPoolDirect(direct);
        group.setEnableMemoryPool(pool);
        group.setMemoryPoolUnit(pool_unit);
        group.setWriteBuffers(32);
        group.setChannelReadBuffer(1024 * readBuf);
        group.setEventLoopSize(Util.availableProcessors() * core);
        group.setConcurrentFrameStack(false);
        if (nodelay) {
            context.addChannelEventListener(new ChannelEventListenerAdapter() {

                @Override
                public void channelOpened(Channel ch) throws Exception {
                    ch.setOption(SocketOptions.TCP_NODELAY, 1);
                    ch.setOption(SocketOptions.SO_KEEPALIVE, 0);
                }
            });
        }
        ByteTree cachedUrls = null;
        if (cachedurl) {
            cachedUrls = new ByteTree();
            for (String route : getDispatcher().getRoutes()) {
                cachedUrls.add(route);
            }
        }
        context.addChannelEventListener(new LoggerChannelOpenListener());
        context.setIoEventHandle(eventHandleAdaptor);
        context.addProtocolCodec(new HttpCodec(serverName, fcache, lite, inline, cachedUrls));
        context.bind(backlog);
    }
}
