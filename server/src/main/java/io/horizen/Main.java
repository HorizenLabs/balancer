package io.horizen;

import static spark.Spark.port;
import static spark.Spark.stop;


public class Main {
    public static void main(String[] args) {
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