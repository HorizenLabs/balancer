package io.horizen;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static JsonObject buildErrorJsonObject(int code, String description, String detail) {
        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("code", code);
        errorObject.addProperty("description", description);
        errorObject.addProperty("detail", detail);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("error", errorObject);

        return jsonObject;
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

    public static void writeProposalToFile(VotingProposal votingProposal) {
        String filePath = Constants.proposalJsonDataPath + Constants.proposalJsonDataFileName;
        String jsonProposal = votingProposal.toJson();

        try {
            // Create directories if they don't exist
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());

            // Write the JSON data to the file
            try (FileWriter fileWriter = new FileWriter(filePath)) {
                fileWriter.write(jsonProposal);
            }
        } catch (IOException e) {
            System.err.println("Error writing the JSON data to the file: " + e.getMessage());
        }
    }

    public static VotingProposal readProposalFromFile() {
        String filePath = Constants.proposalJsonDataPath + Constants.proposalJsonDataFileName;

        // Check if the file exists before reading
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return null;
        }

        StringBuilder jsonData = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return MyGsonManager.getGson().fromJson(jsonData.toString(), VotingProposal.class);
    }
}
