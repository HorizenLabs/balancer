package io.horizen;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;

import static spark.Spark.*;


public class Main {
    public static void main(String[] args) throws Exception {

        //System.setProperty("jdk.tls.server.protocols", "TLSv1.1");

//        String keystoreFilePath = "server/src/main/resources/server-keystore.jks";
//        String keystorePassword = "changeit";

        String keystoreFilePath = "/home/david/Desktop/balancer git ssh/balancer/client/src/main/resources/keystore.p12";
        String keystorePassword = "mypassword";

//        String truststorePath = "server/src/main/resources/server-truststore.jks";
//        String truststorePassword = "changeit";

//        System.setProperty("javax.net.ssl.trustStore", "server/src/main/resources/server-truststore.jks");
//        System.setProperty("javax.net.ssl.trustStorePassword", "changeit"); // Change this to your truststore password

        //
        //KeyStore keystore = KeyStore.getInstance("JKS");
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(keystoreFilePath), keystorePassword.toCharArray());

//        KeyStore truststore = KeyStore.getInstance("JKS");
//        truststore.load(new FileInputStream(truststorePath), truststorePassword.toCharArray());


        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, keystorePassword.toCharArray());

        // Create the SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
//        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//        trustManagerFactory.init(truststore);
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        //
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