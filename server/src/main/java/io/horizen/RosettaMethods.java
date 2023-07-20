package io.horizen;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class RosettaMethods {

    public static ChainTip getChainTip() throws Exception {
        if (Constants.MOCK_ROSETTA)
            return new ChainTip(100, "000439739cac7736282169bb10d368123ca553c45ea6d4509d809537cd31aa0d");

        // Call Rosetta endpoint /network/status
        String url = Constants.ROSETTA_URL + "network/status";
        String requestBody = "{\"network_identifier\": {\"blockchain\": \"Zen\", \"network\": \"" + Constants.NETWORK + "\"}}";

        try {
            // Get the response code
            HttpURLConnection connection = Helper.sendRequest(url, requestBody);
            int responseCode = connection.getResponseCode();

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Handle the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("POST request successful");
                System.out.println("Response: " + response);

                // Parse the JSON response
                JsonObject responseObject = JsonParser.parseString(response.toString()).getAsJsonObject();

                // Retrieve chain_height and best_block_hash
                int chainHeight = responseObject
                        .getAsJsonObject("current_block_identifier")
                        .get("index")
                        .getAsInt();
                String bestBlockHash = responseObject
                        .getAsJsonObject("current_block_identifier")
                        .get("hash")
                        .getAsString();

                return new ChainTip(chainHeight, bestBlockHash);
            } else
                throw new Exception(connection.getResponseCode() + " " + connection.getResponseMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public static Double getAddressBalance(String scAddress) throws Exception {

        double balance = 0;

        Map<String, List<String>> mcAddressMap = SnapshotMethods.getMcAddressMap(scAddress);

        if (mcAddressMap.containsKey(scAddress)) {
            List<String> mcAddresses = mcAddressMap.get(scAddress);

            if (Constants.MOCK_ROSETTA) {
                System.out.println("MOCK ROSETTA RESPONSE");
                return 123456789.0;
            }

            for (String mcAddress : mcAddresses) {
                String body = buildRosettaRequestBody(mcAddress);
                HttpURLConnection connection = Helper.sendRequest(Constants.ROSETTA_URL + "account/balance", body);

                if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // Parse the JSON response
                    JsonElement jsonElement = JsonParser.parseString(connection.getResponseMessage());

                    // Retrieve the value from the parsed JSON
                    int amount = jsonElement
                            .getAsJsonObject()
                            .getAsJsonArray("balances")
                            .get(0)
                            .getAsJsonObject()
                            .get("value")
                            .getAsInt();
                    balance += amount;
                }
                else
                    throw new Exception();
            }
        }

        return balance;
    }


    private static String buildRosettaRequestBody(String mcAddress) {
        // Define ROSETTA_REQUEST_TEMPLATE
        JsonObject rosettaRequestTemplate = new JsonObject();
        rosettaRequestTemplate.addProperty("blockchain", "Zen");
        rosettaRequestTemplate.addProperty("network", Constants.NETWORK);

        // Create the request body
        JsonObject requestBody = new JsonObject();
        requestBody.add("network_identifier", rosettaRequestTemplate);

        JsonObject accountIdentifier = new JsonObject();
        accountIdentifier.addProperty("address", mcAddress);
        accountIdentifier.add("metadata", new JsonObject());
        requestBody.add("account_identifier", accountIdentifier);

        JsonObject blockIdentifier = new JsonObject();
        blockIdentifier.addProperty("index", SnapshotMethods.getActiveProposal().getBlockHeight());
        blockIdentifier.addProperty("hash", SnapshotMethods.getActiveProposal().getBlockHash());
        requestBody.add("block_identifier", blockIdentifier);

        // Print the outgoing request
        System.out.println("Outgoing Request:");
        System.out.println(requestBody);

        // Create Gson instance
        Gson gson = MyGsonManager.getGson();

        // Convert the Java object to JSON string
        return gson.toJson(requestBody);
    }
}
