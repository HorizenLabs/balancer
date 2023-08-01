package io.horizen.helpers;

import com.google.gson.JsonObject;
import io.horizen.config.Settings;
import io.horizen.data_types.VotingProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class Helper {

    private static final Logger log =  LoggerFactory.getLogger(Helper.class);

    private static Settings settings;

    public static void initialize(Settings mySettings) {
        settings = mySettings;
    }

    public static HttpURLConnection sendRequest(String url, String data) throws Exception {
        URL endpointUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

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

        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        connection.setRequestProperty("Authorization", authHeader);
        connection.setRequestProperty("Content-Type", "application/json");

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(data);
        outputStream.flush();
        outputStream.close();

        return connection;
    }

    public static void writeProposalToFile(VotingProposal votingProposal) throws Exception {
        String filePath = settings.getProposalJsonDataPath() + settings.getProposalJsonDataFileName();
        String jsonProposal = votingProposal.toJson();

        // Create directories if they don't exist
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(jsonProposal);
        }
    }

    public static VotingProposal readProposalFromFile() {
        String filePath = settings.getProposalJsonDataPath() + settings.getProposalJsonDataFileName();

        // Check if the file exists before reading
        File file = new File(filePath);
        if (!file.exists()) {
            log.info("Proposal file does not exist.");
            return null;
        }

        StringBuilder jsonData = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null)
                jsonData.append(line);
        } catch (IOException ex) {
            log.error("Error in reading proposal file " + ex);
            return null;
        }

        return MyGsonManager.getGson().fromJson(jsonData.toString(), VotingProposal.class);
    }
}
