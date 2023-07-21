package io.horizen;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

import static spark.Spark.*;


public class Main {
    public static void main(String[] args) throws Exception {
        String keystoreFilePath = "keystore/keystore.p12";
        String keystorePassword = "mypassword";

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(keystoreFilePath), keystorePassword.toCharArray());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, keystorePassword.toCharArray());

        // Create the SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        secure(keystoreFilePath, keystorePassword, null, null);

        port(8080); // Set the server port

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

    private static void checkMocks() {
        if (Constants.MOCK_ROSETTA) {
            System.out.println("##################################");
            System.out.println("##    MOCKING ROSETTA MODULE    ##");
            System.out.println("##################################");
        }
        if (Constants.MOCK_NSC) {
            System.out.println("##################################");
            System.out.println("##    MOCKING NSC MODULE    ##");
            System.out.println("##################################");
        }
    }
}