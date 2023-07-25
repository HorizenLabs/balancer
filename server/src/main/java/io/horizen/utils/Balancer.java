package io.horizen.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.horizen.data_types.VotingProposal;
import io.horizen.exception.GetMcAddressMapException;
import io.horizen.exception.OwnerStringException;
import io.horizen.exception.OwnershipAlreadySetException;
import io.horizen.data_types.ChainTip;
import io.horizen.helpers.Definitions;
import io.horizen.helpers.Helper;
import io.horizen.helpers.MyGsonManager;
import org.bitcoinj.core.AddressFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;

public class Balancer {

    private static final Logger log =  LoggerFactory.getLogger(Balancer.class);

    public void setupRoutes() {
        get("/api/v1/getVotingPower", this::getVotingPower);
        post("/api/v1/getOwnerships", this::getOwnerships);
        post("/api/v1/getProposals", this::getProposals);
        post("/api/v1/createProposal", this::createProposal);
        post("/api/v1/addOwnership", this::addOwnership);
        post("/api/v1/getOwnerScAddresses", this::getOwnerScAddresses);
    }

    private String addOwnership(Request req, Response res) {
        Gson gson = MyGsonManager.getGson();
        log.info("addOwnership request with data " + req.body());
        res.type("application/json");

        String address;
        String owner;
        try {
            JsonObject jsonObject = gson.fromJson(req.body(), JsonObject.class);
            address = jsonObject.get("address").getAsString();
            owner = jsonObject.get("owner").getAsString();
        } catch (Exception ex) {
            int code = 101;
            String description = "Could not add ownership";
            String detail = "invalid json request data, could not find field: " + ex;
            log.error("Error in add ownership " + ex);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        if (Definitions.MOCK_NSC) {
            try {
                SnapshotMethods.addOwnershipEntry(address, owner);
            }
            catch (AddressFormatException ex) {
                int code = 102;
                String description = "Can not add ownership";
                String detail = "address not valid: " + ex;
                log.error("Error in add ownership " + ex);
                return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
            }
            catch (OwnerStringException ex) {
                int code = 103;
                String description = "Can not add ownership";
                String detail = "Invalid owner string length != 42 or not an hex string";
                log.error("Error in add ownership " + ex);
                return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
            }
            catch (OwnershipAlreadySetException ex) {
                int code = 104;
                String description = "Can not add ownership";
                String detail = "Ownership already set";
                log.error("Error in add ownership " + ex);
                return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
            }
        }
        else {
            int code = 306;
            String description = "Could not add ownership";
            String detail = "Method not supported with real native smart contract. Pls set mock_nsc=true in balancer";
            log.error("Error in add ownership - " + detail);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        JsonObject jsonObject = new JsonObject();

        // Add the "ownerships" part
        JsonObject ownerships = new JsonObject();
        for (Map.Entry<String, List<String>> entry : Definitions.MOCK_MC_ADDRESS_MAP.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();

            // Convert the list of addresses to a JsonArray
            JsonArray jsonArray = new JsonArray();
            for (String mcAddress : value)
                jsonArray.add(mcAddress);

            ownerships.add(key, jsonArray);
        }
        jsonObject.add("ownerships", ownerships);

        // Add the "result" part
        JsonObject result = new JsonObject();
        result.addProperty("status", "Ok");
        jsonObject.add("result", result);

        log.info("addOwnership response with data " + MyGsonManager.getGson().toJson(jsonObject));

        return MyGsonManager.getGson().toJson(jsonObject);
    }

    private String getOwnerScAddresses(Request req, Response res) {
        Gson gson = MyGsonManager.getGson();
        List<String> ret;
        res.type("application/json");
        log.info("getOwnerScAddresses request");

        if (Definitions.MOCK_NSC)
            ret = Definitions.MOCK_OWNER_SC_ADDR_LIST;
        else {
            try {
                ret = SnapshotMethods.getOwnerScAddrList();
            }
            catch (Exception ex) {
                int code = 302;
                String description = "Could not get owner sc addresses";
                String detail = "An exception occurred: " + ex;
                log.error("Error in getOwnerScAddresses " + ex);
                return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
            }
        }

        log.info("getOwnerScAddresses response with data " + ret);

        return gson.toJson(ret);
    }

    private String getVotingPower(Request req, Response res) {
        String address = req.queryParams("addresses");
        Gson gson = MyGsonManager.getGson();
        res.type("application/json");
        log.info("getVotingPower request with data " + req.queryParams());

        if (address == null) {
            int code = 107;
            String description = "Cannot get voting power";
            String detail = "Addresses parameter missing";
            log.error("Error in getVotingPower - addresses parameter missing");
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }
        if (SnapshotMethods.getActiveProposal() == null) {
            int code = 305;
            String description = "No proposal have been received at this point";
            String detail = "Proposal should be received before getting voting power";
            log.error("Error in getVotingPower - " + detail);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }
        double balance;
        try {
            balance = RosettaMethods.getAddressBalance(address);
        }
        catch (GetMcAddressMapException ex) {
            int code = 202;
            String description = "Could not get ownership for sc address:" + address;
            String detail = "An exception occurred: " + ex;
            log.error("Error in getVotingPower " + ex);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }
        catch (Exception ex) {
            int code = 108;
            String description = "Can not get address balance";
            String detail = "An exception occurred: " + ex;
            log.error("Error in getVotingPower " + ex);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        // Create the Java object representing the JSON structure
        JsonObject jsonObject = new JsonObject();
        JsonArray scoreArray = new JsonArray();
        JsonObject scoreObject = new JsonObject();
        scoreObject.addProperty("address", address);
        scoreObject.addProperty("score", balance);
        scoreObject.addProperty("decimal", 8);
        scoreArray.add(scoreObject);
        jsonObject.add("score", scoreArray);

        log.info("getVotingPower response with data " + gson.toJson(jsonObject));

        return gson.toJson(jsonObject);
    }

    private String createProposal(Request req, Response res) {
        ChainTip chainTip;
        res.type("application/json");
        Gson gson = MyGsonManager.getGson();
        log.info("createProposal request with data " + req.body());

        Date startDate;
        Date endDate;
        String start;
        String end;
        String author;
        String proposalId;
        try {
            JsonObject jsonObject = gson.fromJson(req.body(), JsonObject.class);

            String body = jsonObject.get("Body").getAsString();
            start = extractValueFromBody(body, "Start");
            end = extractValueFromBody(body, "End");
            author = extractValueFromBody(body, "Author");
            proposalId = jsonObject.get("ProposalID").getAsString();
        } catch (Exception ex) {
            int code = 304;
            String description = "Can not create proposal";
            String detail = "parameters format not expected or missing: " + ex;
            log.error("Error in createProposal " + ex);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        SimpleDateFormat format = new SimpleDateFormat("dd MMM yy HH:mm z");
        try {
            startDate = format.parse(start);
            endDate = format.parse(end);
            System.out.println(startDate);
        } catch (ParseException ex) {
            int code = 304;
            String description = "Can not create proposal";
            String detail =  "proposal data format not expected: " + ex;
            log.error("Error in createProposal " + ex);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }
        try {
            chainTip = RosettaMethods.getChainTip();
        } catch (Exception ex) {
            int code = 303;
            String description = "Can not create proposal";
            String detail = "Can not determine main chain best block: " + ex;
            log.error("Error in createProposal " + ex);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        VotingProposal proposal = new VotingProposal(proposalId, chainTip.getBlockHeight(), chainTip.getBlockHash(), startDate,endDate, author);
        try {
            SnapshotMethods.storeProposalData(proposal);
        } catch (Exception ex) {
            int code = 304;
            String description = "Can not create proposal";
            String detail = "Problem with storing proposal data: " + ex;
            log.error("Error in createProposal " + ex);
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }
        JsonObject statusObject = new JsonObject();
        statusObject.addProperty("status", "OK");

        log.info("createProposal response with data " + gson.toJson(statusObject));

        return gson.toJson(statusObject);
    }

    private static String extractValueFromBody(String body, String key) {
        String prefix = key + ": ";
        int startIndex = body.indexOf(prefix);
        int endIndex = body.indexOf(",", startIndex);
        if (endIndex == -1) {
            endIndex = body.length();
        }
        return body.substring(startIndex + prefix.length(), endIndex).trim();
    }

    private String getProposals(Request req, Response res) {
        res.type("application/json");
        log.info("getProposals request");
        Gson gson = MyGsonManager.getGson();

        Collection<VotingProposal> proposals = SnapshotMethods.getProposals();
        log.info("getProposals response with data " + proposals);

        return gson.toJson(proposals);
    }

    private String getOwnerships(Request req, Response res) {
        Map<String, List<String>> ret;
        Gson gson = MyGsonManager.getGson();
        res.type("application/json");
        log.info("getOwnerships request with data " + req.body());

        if (Definitions.MOCK_NSC)
            ret = Definitions.MOCK_MC_ADDRESS_MAP;
        else {
            String scAddress;
            try {
                JsonObject jsonObject = MyGsonManager.getGson().fromJson(req.body(), JsonObject.class);
                scAddress = jsonObject.get("scAddress").getAsString();
            }
            catch (Exception ex) {
                scAddress = null;
            }
            try {
                ret = SnapshotMethods.getMcAddressMap(scAddress);
            } catch (Exception ex) {
                int code = 301;
                String description = "Could not get ownership for sc address:" + scAddress;
                String detail = "An exception occurred: " + ex;
                log.error("Error in getOwnerships " + ex);
                return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
            }
        }
        log.info("getOwnerships response with data " + ret);

        return gson.toJson(ret);
    }
}
