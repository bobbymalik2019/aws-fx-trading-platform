package com.charter26.fix_initiator_client.fix;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import quickfix.*;

import com.charter26.fix_initiator_client.fix.FixClientApplication;

@Component 
public class FixInitiatorEngine implements SmartLifecycle {
 private static final Logger log =
            LoggerFactory.getLogger(FixInitiatorEngine.class);

    private final FixClientApplication application;

    private SocketInitiator initiator;
    private volatile boolean running;

    public FixInitiatorEngine(FixClientApplication application) {
        this.application = application;
    }

    @Override
    public synchronized void start() {
        if (running) return;

        try (InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream("initiator.cfg")) {

            if (stream == null) {
                throw new IllegalStateException(
                        "initiator.cfg not found");
            }

            SessionSettings settings =
                    new SessionSettings(stream);

            MessageStoreFactory storeFactory =
                    new FileStoreFactory(settings);

            LogFactory logFactory =
                    new FileLogFactory(settings);

            MessageFactory messageFactory =
                    new DefaultMessageFactory();

            initiator = new SocketInitiator(
                    application,
                    storeFactory,
                    settings,
                    logFactory,
                    messageFactory
            );

            initiator.start();
            running = true;

            log.info("FIX Initiator Started");

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to start FIX Initiator", e);
        }
    }

    @Override
    public synchronized void stop() {
        if (initiator != null) {
            initiator.stop();
            initiator = null;
        }

        running = false;
        log.info("FIX Initiator Stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
    
}
