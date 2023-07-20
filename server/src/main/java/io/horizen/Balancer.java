package io.horizen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;

public class Balancer {

    private static final Logger log =  LoggerFactory.getLogger(Main.class);

    public void setupRoutes() {
        get("/hello", (req, res) -> "Hello, World!"); // Define a route

        get("/api/v1/getVotingPower", this::getVotingPower);
        post("/api/v1/getOwnerships", this::getOwnerships);
        post("/api/v1/getProposals", this::getProposals);
        post("/api/v1/createProposal", this::createProposal);
        post("/api/v1/addOwnership", this::addOwnership);
    }

    private String addOwnership(Request req, Response res) {
        //todo should check what happenes if address and owner not set
        String address = req.queryParams("address");
        String owner = req.queryParams("owner");

        Gson gson = MyGsonManager.getGson();

        if (Constants.MOCK_NSC) {
            try {
                SnapshotMethods.addOwnershipEntry(address, owner);
            } catch (Exception ex) {
                int code = 103;
                String description = "Can not add ownership";
                String detail = "Invalid owner string length != 42 or not an hex string";
                return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
            }
        }
        else {
            int code = 109;
            String description = "Could not add ownership";
            String detail = "Method not supported with real native smart contract. Pls set mock_nsc=true in balancer"; //todo java spark automatically adds escaping
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        return gson.toJson(Constants.MOCK_MC_ADDRESS_MAP);
    }

    private String getVotingPower(Request req, Response res) {

        String address = req.queryParams("addresses");
        Gson gson = MyGsonManager.getGson();

        if (SnapshotMethods.getActiveProposal() == null) {
            int code = 107;
            String description = "No proposal have been received at this point";
            String detail = "Proposal should be received before getting voting power";
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        double balance;
        try {
            balance = RosettaMethods.getAddressBalance(address);
        } catch (Exception ex) {
            int code = 108;
            String description = "Reference MC block not defined";
            String detail = "Reference block should be retrieved from Rosetta when a voting proposal is created";
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


        // Convert the Java object to JSON string
        res.type("application/json");
        return gson.toJson(jsonObject);
    }

    private String createProposal(Request req, Response res) {
        ChainTip chainTip;

        Gson gson = MyGsonManager.getGson();
        JsonObject jsonObject = gson.fromJson(req.body(), JsonObject.class);

        String body = jsonObject.get("Body").getAsString();
        Date startDate = null;
        Date endDate = null;
        String start = extractValueFromBody(body, "Start");
        String end = extractValueFromBody(body, "End");
        String author = extractValueFromBody(body, "Author");
        String proposalId = jsonObject.get("ProposalID").getAsString();

        SimpleDateFormat format = new SimpleDateFormat("dd MMM yy HH:mm z");
//        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            startDate = format.parse(start);
            endDate = format.parse(end);
            System.out.println(startDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            chainTip = RosettaMethods.getChainTip();
        } catch (Exception e) {
            log.error(e.getMessage());
            int code = 105;
            String description = "Can not create proposal";
            String detail = "can not determine main chain best block: " + e.getMessage();
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        VotingProposal proposal = new VotingProposal(proposalId, chainTip.getBlockHeight(), chainTip.getBlockHash(), startDate,endDate, author);
        SnapshotMethods.storeProposalData(proposal);
        return "OK";
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
        Gson gson = MyGsonManager.getGson();
        return gson.toJson(SnapshotMethods.getProposals());
    }

    private String getOwnerships(Request req, Response res) {
        Map<String, List<String>> ret;
        if (Constants.MOCK_NSC)
            ret = Constants.MOCK_MC_ADDRESS_MAP;
        else {
            String address = req.queryParams("address");
            try {
                ret = SnapshotMethods.getMcAddressMap(address);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Gson gson = MyGsonManager.getGson();
        return gson.toJson(ret);
    }
}
