package com.charter26.fix_accepter_server;

// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import com.charter26.fix_accepter_server.config.FixProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// import jakarta.annotation.PostConstruct;
// import quickfix.*;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FixAccepterServerApplication {

	// private String configPath; 
	// @Autowired
	// private FixProperties fixProperties;
	public static void main(String[] args) {
		
		SpringApplication.run(FixAccepterServerApplication.class, args);
	}

// 	@PostConstruct
//     public void startFixEngine() throws Exception {
// 		configPath = fixProperties.getConfigPath();
// 		System.out.println(configPath);
//         SessionSettings settings =
//                 new SessionSettings(
//                         getClass()
//                                 .getClassLoader()
//                                 .getResourceAsStream(configPath));

//         Application application = new ApplicationAdapter() {

//             @Override
//             public void onCreate(SessionID sessionId) {
//                 System.out.println(
//                         "Session Created: "
//                                 + sessionId);
//             }

//             @Override
//             public void onLogon(SessionID sessionId) {
//                 System.out.println(
//                         "LOGON SUCCESS: "
//                                 + sessionId);
//             }

//             @Override
//             public void onLogout(SessionID sessionId) {
//                 System.out.println(
//                         "LOGOUT: "
//                                 + sessionId);
//             }
//         };

//         MessageStoreFactory storeFactory =
//                 new FileStoreFactory(settings);

//         LogFactory logFactory =
//                 new FileLogFactory(settings);

//         MessageFactory messageFactory =
//                 new DefaultMessageFactory();

//         SocketAcceptor acceptor =
//                 new SocketAcceptor(
//                         application,
//                         storeFactory,
//                         settings,
//                         logFactory,
//                         messageFactory);

//         acceptor.start();
//         System.out.println("FIX Acceptor Started...");
//     }

}
