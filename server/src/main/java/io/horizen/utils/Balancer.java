package io.horizen.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.horizen.config.Settings;
import io.horizen.data_types.MainchainTip;
import io.horizen.data_types.VotingProposal;
import io.horizen.exception.*;
import io.horizen.helpers.Mocks;
import io.horizen.helpers.MyGsonManager;
import io.horizen.services.RosettaService;
import io.horizen.services.SnapshotService;
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
    private final Settings settings;
    private final RosettaService rosettaService;
    private final SnapshotService snapshotService;

    @Inject
    public Balancer(Settings settings, RosettaService rosettaService, SnapshotService snapshotService) {
        this.settings = settings;
        this.rosettaService = rosettaService;
        this.snapshotService = snapshotService;
    }

    private static final Logger log =  LoggerFactory.getLogger(Balancer.class);

    public void setupRoutes() {
        get("/hello", (req, res) -> "Hello, World!");
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
            String detail = "invalid json request data, could not find field: " + ex;
            log.error("Error in add ownership " + ex);
            return new AddOwnershipException(detail).toString();
        }

        if (settings.getMockNsc()) {
            try {
                snapshotService.addMockOwnershipEntry(address, owner);
            }
            catch (AddressFormatException ex) {
                String detail = "address not valid: " + ex;
                log.error("Error in add ownership " + ex);
                return new AddOwnershipException(detail).toString();
            }
            catch (ScAddressFormatException ex) {
                String detail = "Invalid owner string length != 42 or not an hex string";
                log.error("Error in add ownership " + ex);
                return new AddOwnershipException(detail).toString();
            }
            catch (OwnershipAlreadySetException ex) {
                String detail = "Ownership already set";
                log.error("Error in add ownership " + ex);
                return new AddOwnershipException(detail).toString();
            }
            catch (Exception ex) {
                String detail = "Problem with adding ownership";
                log.error("Error in add ownership " + ex);
                return new AddOwnershipException(detail).toString();
            }
        }
        else {
            String detail = "Method not supported with real native smart contract. Pls set mock_nsc=true in balancer";
            log.error("Error in add ownership - " + detail);
            return new AddOwnershipException(detail).toString();
        }

        JsonObject jsonObject = new JsonObject();

        // Add the "ownerships" part
        JsonObject ownerships = new JsonObject();
        for (Map.Entry<String, List<String>> entry : Mocks.mockMcAddressMap.entrySet()) {
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

        try {
            ret = snapshotService.getOwnerScAddrList();
        }
        catch (Exception ex) {
            String detail = "An exception occurred: " + ex;
            log.error("Error in getOwnerScAddresses " + ex);
            return new GetOwnerScAddressesException(detail).toString();
        }

        log.info("getOwnerScAddresses response with data " + ret);

        return gson.toJson(ret);
    }

    private String getVotingPower(Request req, Response res) {
        String sc_address = req.queryParams("addresses");
        int snapshot = 0;

        if (req.queryParams().contains("snapshot"))
            snapshot = Integer.parseInt(req.queryParams("snapshot"));

        Gson gson = MyGsonManager.getGson();
        res.type("application/json");
        log.info("getVotingPower request with data " + req.queryParams());

        if (sc_address == null || snapshot == 0) {
            String detail = "Addresses or snapshot parameter missing";
            log.error("Error in getVotingPower - addresses or snapshot parameter missing");
            return new GenericException(detail).toString();
        }
        if (snapshotService.getActiveProposal(snapshot) == null) {
            String detail = "Proposal should be received before getting voting power";
            log.error("Error in getVotingPower - " + detail);
            return new GenericException(detail).toString();
        }
        double balance;
        try {
            balance = rosettaService.getAddressBalance(sc_address,snapshot);
        }
        catch (Exception ex) {
            String detail = "An exception occurred: " + ex;
            log.error("Error in getVotingPower " + ex);
            return new RosettaException(detail).toString();
        }

        JsonObject jsonObject = new JsonObject();
        JsonArray scoreArray = new JsonArray();
        JsonObject scoreObject = new JsonObject();
        scoreObject.addProperty("address", sc_address);
        scoreObject.addProperty("score", String.format("%.0f", balance)); // fix for outputting 123456789.0 as 1.23456789E8
        scoreArray.add(scoreObject);
        jsonObject.add("score", scoreArray);

        log.info("getVotingPower response with data " + gson.toJson(jsonObject));

        return gson.toJson(jsonObject);
    }

    private String createProposal(Request req, Response res) {
        MainchainTip mainchainTip;
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
            start = extractValueFromBody(body, "Starts on:");
            end = extractValueFromBody(body, "Ends on:");
            author = extractValueFromBody(body, "Author:");
            String proposalWithPrefix = jsonObject.get("ProposalID").getAsString();
            int startIndex = proposalWithPrefix.indexOf("proposal/") + "proposal/".length();
            proposalId = proposalWithPrefix.substring(startIndex);
        } catch (Exception ex) {
            String detail = "parameters format not expected or missing: " + ex;
            log.error("Error in createProposal " + ex);
            return new CreateProposalException(detail).toString();
        }

        SimpleDateFormat format = new SimpleDateFormat("dd MMM yy HH:mm z");
        try {
            startDate = format.parse(start);
            endDate = format.parse(end);
        } catch (ParseException ex) {
            String detail =  "proposal data format not expected: " + ex;
            log.error("Error in createProposal " + ex);
            return new CreateProposalException(detail).toString();
        }
        try {
            mainchainTip = rosettaService.getMainchainTip();
        } catch (Exception ex) {
            String detail = "Can not determine main chain best block: " + ex;
            log.error("Error in createProposal " + ex);
            return new RosettaException(detail).toString();
        }

        int snapshot;
        try {
            snapshot = snapshotService.getSnapshotProposal(proposalId);
        } catch (Exception ex) {
            return new SnapshotException(ex.toString()).toString();
        }

        VotingProposal proposal = new VotingProposal(proposalId, mainchainTip.getBlockHeight(), mainchainTip.getBlockHash(), startDate,endDate, author, snapshot);
        try {
            snapshotService.storeProposalData(proposal);
        } catch (Exception ex) {
            String detail = "Problem with storing proposal data: " + ex;
            log.error("Error in createProposal " + ex);
            return new SnapshotException(detail).toString();
        }
        JsonObject statusObject = new JsonObject();
        statusObject.addProperty("status", "OK");

        log.info("createProposal response with data " + gson.toJson(statusObject));

        return gson.toJson(statusObject);
    }

    private static String extractValueFromBody(String body, String key) throws Exception {
        int startIndex = body.indexOf(key);
        if (startIndex != -1) {
            startIndex += key.length();
            int endIndex = body.indexOf("\n", startIndex);
            if (endIndex != -1)
                return body.substring(startIndex, endIndex).trim();
            else
                return body.substring(startIndex).trim();

        }
        throw new Exception("key not contained in body");
    }

    private String getProposals(Request req, Response res) {
        res.type("application/json");
        log.info("getProposals request");
        Gson gson = MyGsonManager.getGson();

        Collection<VotingProposal> proposals = snapshotService.getProposals();
        log.info("getProposals response with data " + proposals);

        return gson.toJson(proposals);
    }

    private String getOwnerships(Request req, Response res) {
        Map<String, List<String>> ret;
        Gson gson = MyGsonManager.getGson();
        res.type("application/json");
        log.info("getOwnerships request with data " + req.body());


        String scAddress;
        try {
            JsonObject jsonObject = MyGsonManager.getGson().fromJson(req.body(), JsonObject.class);
            scAddress = jsonObject.get("scAddress").getAsString();
        }
        catch (Exception ex) {
            String detail = "Could not get ownership - no scAddress found";
            log.error("Error in getOwnerships - no scAddress found" + ex);
            return new GetOwnershipException(detail).toString();
        }
        try {
            ret = snapshotService.getMcAddressMap(scAddress);
        } catch (Exception ex) {
            String detail = "Could not get ownership for sc address:" + scAddress;
            log.error("Error in getOwnerships " + ex);
            return new GetOwnershipException(detail).toString();
        }

        log.info("getOwnerships response with data " + ret);

        return gson.toJson(ret);
    }
}
