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
import java.util.Properties;

import static spark.Spark.*;


public class Main {

    private static final Logger log =  LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("conf file path not specified");
        }

        Settings settings;
        try {
            settings = readConfigFromFile(args[0]);
        } catch (Exception ex) {
            log.error("Error in reading config file " + ex);
            throw new RuntimeException(ex);
        }

        if (settings.getSsl())
            setupSSL();

        port(settings.getServerPort());

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

    private static Settings readConfigFromFile(String configFilePath) throws Exception {
        Properties properties = new Properties();
        FileInputStream fis = new FileInputStream(configFilePath);
        properties.load(fis);

        // Read the properties from the loaded configuration
        String nscUrl = properties.getProperty("nscUrl");
        String rosettaUrl = properties.getProperty("rosettaUrl");
        String network = properties.getProperty("network");
        Boolean mockNsc = Boolean.parseBoolean(properties.getProperty("mockNsc"));
        Boolean mockRosetta = Boolean.parseBoolean(properties.getProperty("mockRosetta"));
        Boolean ssl = Boolean.parseBoolean(properties.getProperty("ssl"));
        String proposalJsonDataFileName = properties.getProperty("proposalJsonDataFileName");
        String proposalJsonDataPath = properties.getProperty("proposalJsonDataPath");
        int serverPort = Integer.parseInt(properties.getProperty("port"));

        // Create an instance of the Settings class with the loaded values
        return new Settings(
                proposalJsonDataFileName,
                proposalJsonDataPath,
                nscUrl,
                rosettaUrl,
                network,
                mockNsc,
                mockRosetta,
                ssl, serverPort);
    }
}