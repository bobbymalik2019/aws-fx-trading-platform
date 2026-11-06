package com.charter26.fix_initiator_client.fix;

import java.io.InputStream;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
//import org.springframework.beans.factory.annotation.Autowired;
import java.util.Locale;

import quickfix.*;

import com.charter26.fix_initiator_client.fix.FixClientApplication;

@Component 
public class FixInitiatorEngine implements SmartLifecycle {
    
    //@Autowired
    private final Environment environment;

 private static final Logger log =
            LoggerFactory.getLogger(FixInitiatorEngine.class);

    private final FixClientApplication application;

    private SocketInitiator initiator;
    private volatile boolean running;

    public FixInitiatorEngine(FixClientApplication application, Environment environment) {
        this.application = application;
        this.environment = environment;
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

            SessionSettings settings =new SessionSettings(stream);
            Iterator<SessionID> sessions = settings.sectionIterator();


            while (sessions.hasNext()) 
            {
                SessionID sessionId = sessions.next();

                String lp = sessionId.getTargetCompID();

                String prefix = "fix.lp." + lp.toLowerCase(Locale.ROOT);

                String host = environment.getRequiredProperty( prefix + ".host");

                int port = environment.getRequiredProperty(
                    prefix + ".port",
                    Integer.class
                );

                settings.setString(
                    sessionId,
                    "SocketConnectHost",
                    host
                );

                settings.setLong(
                    sessionId,
                    "SocketConnectPort",
                    port
                );  

                log.info("FIX endpoint configured: LP={}, Host={}, Port={}",lp, host, port);      
            }          

            MessageStoreFactory storeFactory =
                    new FileStoreFactory(settings);

            LogFactory logFactory =
                    new FileLogFactory(settings);

            MessageFactory messageFactory =
                    new DefaultMessageFactory();


            // SessionSettings settings = new SessionSettings(
            //         getClass().getClassLoader()
            //             .getResourceAsStream(configPath)
            //     );



                



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
