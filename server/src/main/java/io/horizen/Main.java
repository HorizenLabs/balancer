package io.horizen;

import io.horizen.helpers.Definitions;
import io.horizen.utils.Balancer;
import io.horizen.utils.SnapshotMethods;
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
        setupSSL();

        port(8080);

        checkMocks();

        Balancer balancer = new Balancer();
        balancer.setupRoutes();
        SnapshotMethods.initActiveProposal();

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

    private static void checkMocks() {
        if (Definitions.MOCK_ROSETTA) {
            System.out.println("##################################");
            System.out.println("##    MOCKING ROSETTA MODULE    ##");
            System.out.println("##################################");
        }
        if (Definitions.MOCK_NSC) {
            System.out.println("##################################");
            System.out.println("##    MOCKING NSC MODULE    ##");
            System.out.println("##################################");
        }
    }
}