
package com.charter26.fix_accepter_server.fix;

import com.charter26.fix_accepter_server.config.FixProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import quickfix.*;

import java.io.InputStream;

@Component
public class FixEngine implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FixEngine.class);

    private final FixProperties fixProperties;
    private final FixApplication fixApplication;

    private SocketAcceptor acceptor;
    private volatile boolean running = false;

    public FixEngine(FixProperties fixProperties, FixApplication fixApplication) {

        this.fixProperties = fixProperties;
        this.fixApplication = fixApplication;
    }

    @Override
    public synchronized void start() {

        if (running) {
            return;
        }

        try (InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream(fixProperties.getConfigPath())) {

            if (stream == null) {
                throw new IllegalStateException(
                        "FIX config file not found: "
                        + fixProperties.getConfigPath());
            }

            SessionSettings settings =new SessionSettings(stream);
            MessageStoreFactory storeFactory =new FileStoreFactory(settings);
            LogFactory logFactory =new FileLogFactory(settings);
            MessageFactory messageFactory =new DefaultMessageFactory();

            acceptor = new SocketAcceptor(
                    fixApplication,
                    storeFactory,
                    settings,
                    logFactory,
                    messageFactory
            );

            acceptor.start();

            running = true;

            log.info("FIX Acceptor Started Successfully");

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to start FIX Acceptor", e);
        }
    }

    @Override
    public synchronized void stop() {

        if (acceptor != null) {
            acceptor.stop();
            acceptor = null;
        }

        running = false;

        log.info("FIX Acceptor Stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
