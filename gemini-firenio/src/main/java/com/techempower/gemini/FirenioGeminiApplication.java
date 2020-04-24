package com.techempower.gemini;

import com.firenio.codec.http11.HttpCodec;
import com.firenio.codec.http11.HttpConnection;
import com.firenio.codec.http11.HttpContentType;
import com.firenio.codec.http11.HttpFrame;
import com.firenio.component.*;
import com.techempower.data.ConnectionMonitor;
import com.techempower.data.ConnectorFactory;
import com.techempower.data.DatabaseAffinity;
import com.techempower.gemini.lifecycle.InitAnnotationDispatcher;
import com.techempower.gemini.monitor.FirenioMonitor;
import com.techempower.gemini.mustache.FirenioMustacheManager;
import com.techempower.gemini.mustache.MustacheManager;
import com.techempower.gemini.path.AnnotationDispatcher;
import com.techempower.gemini.session.HttpSessionManager;
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
    public final void start() throws Exception {
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
                httpFrame.setContentType(HttpContentType.text_plain);
                httpFrame.setConnection(HttpConnection.NONE);
                channel.writeAndFlush(httpFrame);
                channel.release(httpFrame);
            }
        };
        ChannelAcceptor context = new ChannelAcceptor(8300);
        context.addChannelEventListener(new LoggerChannelOpenListener());
        context.setIoEventHandle(eventHandleAdaptor);
        context.addProtocolCodec(new HttpCodec());
        context.bind();
    }
}
