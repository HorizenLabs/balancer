package io.horizen.services.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import io.horizen.config.Settings;
import io.horizen.data_types.ChainTip;
import io.horizen.exception.GetMcAddressMapException;
import io.horizen.helpers.Helper;
import io.horizen.helpers.MyGsonManager;
import io.horizen.services.RosettaService;
import io.horizen.services.SnapshotService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class RosettaServiceImpl implements RosettaService {
    private final SnapshotService snapshotService;
    private final Settings settings;


    @Inject // This annotation is important for Guice to perform injection
    public RosettaServiceImpl(SnapshotService snapshotService, Settings settings) {
        this.snapshotService = snapshotService;
        this.settings = settings;
    }

    public ChainTip getChainTip() throws Exception {
        if (settings.getMockRosetta())
            return new ChainTip(100, "000439739cac7736282169bb10d368123ca553c45ea6d4509d809537cd31aa0d");

        // Call Rosetta endpoint /network/status
        String url = settings.getRosettaUrl() + "network/status";
        String requestBody = "{\"network_identifier\": {\"blockchain\": \"Zen\", \"network\": \"" + settings.getNetwork() + "\"}}";

        try {
            // Get the response code
            HttpURLConnection connection = Helper.sendRequest(url, requestBody);
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

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
            }
            else
                throw new Exception(connection.getResponseCode() + " " + connection.getResponseMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public Double getAddressBalance(String scAddress) throws Exception {
        double balance = 0;
        Map<String, List<String>> mcAddressMap;
        try {
            mcAddressMap = snapshotService.getMcAddressMap(scAddress);
        }
        catch (Exception ex) {
            throw new GetMcAddressMapException();
        }
        if (mcAddressMap.containsKey(scAddress)) {
            List<String> mcAddresses = mcAddressMap.get(scAddress);

            if (settings.getMockRosetta()) {
                System.out.println("MOCK ROSETTA RESPONSE");
                return 123456789.0;
            }

            for (String mcAddress : mcAddresses) {
                String body = buildRosettaRequestBody(mcAddress);
                HttpURLConnection connection = Helper.sendRequest(settings.getRosettaUrl() + "account/balance", body);

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
            }
        }
        return balance;
    }


    private String buildRosettaRequestBody(String mcAddress) {
        // Define ROSETTA_REQUEST_TEMPLATE
        JsonObject rosettaRequestTemplate = new JsonObject();
        rosettaRequestTemplate.addProperty("blockchain", "Zen");
        rosettaRequestTemplate.addProperty("network", settings.getNetwork());

        // Create the request body
        JsonObject requestBody = new JsonObject();
        requestBody.add("network_identifier", rosettaRequestTemplate);

        JsonObject accountIdentifier = new JsonObject();
        accountIdentifier.addProperty("address", mcAddress);
        accountIdentifier.add("metadata", new JsonObject());
        requestBody.add("account_identifier", accountIdentifier);

        JsonObject blockIdentifier = new JsonObject();
        blockIdentifier.addProperty("index", snapshotService.getActiveProposal().getBlockHeight());
        blockIdentifier.addProperty("hash", snapshotService.getActiveProposal().getBlockHash());
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
