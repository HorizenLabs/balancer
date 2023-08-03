package io.horizen;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.horizen.config.AppModule;
import io.horizen.config.Settings;
import io.horizen.helpers.Helper;
import io.horizen.services.SnapshotService;
import io.horizen.utils.Balancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

import static spark.Spark.*;


public class Main {

    private static final Logger log =  LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Settings settings = new Settings();

        //setupSSL();

        port(5000); //change for different port

        checkMocks(settings);

        Helper.initialize(settings);

        Injector injector = Guice.createInjector(new AppModule(settings));
        Balancer balancer = injector.getInstance(Balancer.class);
        balancer.setupRoutes();

        SnapshotService snapshotService = injector.getInstance(SnapshotService.class);
        snapshotService.initActiveProposal();

        // Stop the server gracefully when the application shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            System.out.println("Server stopped");
        }));
    }

    private static void setupSSL() {
        String keystoreFilePath = "keystore/keystore.p12";
        String keystorePassword = "mypassword";

        try {
            KeyStore keystore =  KeyStore.getInstance("PKCS12");
            keystore.load(new FileInputStream(keystoreFilePath), keystorePassword.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keystorePassword.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        } catch (Exception ex) {
            log.error("Error in setup ssl " + ex);
            throw new RuntimeException(ex);
        }

        secure(keystoreFilePath, keystorePassword, null, null);
    }

    private static void checkMocks(Settings settings) {
        if (settings.getMockRosetta()) {
            System.out.println("##################################");
            System.out.println("##    MOCKING ROSETTA MODULE    ##");
            System.out.println("##################################");
        }
        if (settings.getMockNsc()) {
            System.out.println("##################################");
            System.out.println("##    MOCKING NSC MODULE    ##");
            System.out.println("##################################");
        }
    }
}