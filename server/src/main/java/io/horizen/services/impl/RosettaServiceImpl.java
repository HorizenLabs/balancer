package io.horizen.services.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import io.horizen.config.Settings;
import io.horizen.data_types.ChainTip;
import io.horizen.exception.GetMcAddressMapException;
import io.horizen.helpers.Helper;
import io.horizen.helpers.Mocks;
import io.horizen.helpers.MyGsonManager;
import io.horizen.services.RosettaService;
import io.horizen.services.SnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.IOUtils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RosettaServiceImpl implements RosettaService {
    private final SnapshotService snapshotService;
    private final Settings settings;

    private static final Logger log =  LoggerFactory.getLogger(RosettaService.class);


    @Inject
    public RosettaServiceImpl(SnapshotService snapshotService, Settings settings) {
        this.snapshotService = snapshotService;
        this.settings = settings;
    }

    public ChainTip getMainchainTip() throws Exception {
        if (settings.getMockRosetta())
            return new ChainTip(Mocks.MOCK_ROSETTA_BLOCK_HEIGHT, Mocks.MOCK_ROSETTA_BLOCK_HASH);

        // Call Rosetta endpoint /network/status
        String url = settings.getRosettaUrl() + "network/status";
        String requestBody = "{\"network_identifier\": {\"blockchain\": \"Zen\", \"network\": \"" + settings.getNetwork() + "\"}}";

        try {
            HttpURLConnection connection = Helper.sendRequest(url, requestBody);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = IOUtils.toString(connection.getInputStream());
                JsonObject responseObject = JsonParser.parseString(response).getAsJsonObject();

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
            log.error("Error in getMainchainTip " + ex);
            throw ex;
        }
    }

    public String getMainchainBlockHash(int height) throws Exception{
        if (settings.getMockRosetta()) {
            return Mocks.MOCK_ROSETTA_BLOCK_HASH;
        }

        String requestBodyJson = buildRosettaRequestBodyForBlock(height);

        URL apiUrl = new URL(settings.getRosettaUrl() + "block");
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        String response = IOUtils.toString(connection.getInputStream());
        JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();

        return responseObj.getAsJsonObject("block")
                .getAsJsonObject("block")
                .getAsJsonObject("block_identifier")
                .get("hash")
                .getAsString();
    }

    private String buildRosettaRequestBodyForBlock(int height) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> networkIdentifier = new HashMap<>();
        networkIdentifier.put("blockchain", "Zen");
        networkIdentifier.put("network", settings.getNetwork());
        Map<String, Integer> blockIdentifier = new HashMap<>();
        blockIdentifier.put("index", height);
        requestBody.put("network_identifier", networkIdentifier);
        requestBody.put("block_identifier", blockIdentifier);
        return MyGsonManager.getGson().toJson(requestBody);
    }

    public Double getAddressBalance(String scAddress) throws Exception {
        double balance = 0;

        Helper.warnIfProposalNotActive(snapshotService.getActiveProposal());

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

            // Call Rosetta endpoint /account/balance In the query we use just the block height (omitting the hash) in the
            // 'block_identifier', this is for handling also the case of a chain reorg which reverts the block whose hash
            // was red when the proposal has been received

            for (String mcAddress : mcAddresses) {
                String body = buildRosettaRequestBodyForNetworkStatus(mcAddress);
                HttpURLConnection connection = Helper.sendRequest(settings.getRosettaUrl() + "account/balance", body);

                if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String response = IOUtils.toString(connection.getInputStream());
                    JsonElement jsonElement = JsonParser.parseString(response);

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


    private String buildRosettaRequestBodyForNetworkStatus(String mcAddress) {
        JsonObject rosettaRequestTemplate = new JsonObject();
        rosettaRequestTemplate.addProperty("blockchain", "Zen");
        rosettaRequestTemplate.addProperty("network", settings.getNetwork());

        JsonObject requestBody = new JsonObject();
        requestBody.add("network_identifier", rosettaRequestTemplate);

        JsonObject accountIdentifier = new JsonObject();
        accountIdentifier.addProperty("address", mcAddress);
        accountIdentifier.add("metadata", new JsonObject());
        requestBody.add("account_identifier", accountIdentifier);

        JsonObject blockIdentifier = new JsonObject();
        blockIdentifier.addProperty("index", snapshotService.getActiveProposal().getBlockHeight());
        requestBody.add("block_identifier", blockIdentifier);

        return MyGsonManager.getGson().toJson(requestBody);
    }
}
