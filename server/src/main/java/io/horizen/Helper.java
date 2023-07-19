package io.horizen;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Helper {

    public static HttpURLConnection sendRequest(String url, String data) throws Exception {
        URL endpointUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // Set the request headers
        connection.setRequestProperty("Content-Type", "application/json");

        // Write the request body
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(data);
        outputStream.flush();
        outputStream.close();

        return connection;
    }

    public static HttpURLConnection sendRequestWithAuth(String url, String data, String username, String password) throws Exception {
        URL endpointUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // Set basic authentication
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        // Set the request headers
        connection.setRequestProperty("Authorization", authHeader);
        connection.setRequestProperty("Content-Type", "application/json");

        // Write the request body
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(data);
        outputStream.flush();
        outputStream.close();

        return connection;
    }
}
