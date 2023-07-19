package io.horizen;

import static spark.Spark.port;
import static spark.Spark.stop;


public class Main {
    public static void main(String[] args) {
        port(8080); // Set the server port

        Balancer balancer = new Balancer();
        balancer.setupRoutes();

        // Stop the server gracefully when the application shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            System.out.println("Server stopped");
        }));
    }
}