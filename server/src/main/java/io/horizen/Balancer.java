package io.horizen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    public void setupRoutes() {
        get("/hello", (req, res) -> "Hello, World!"); // Define a route

        get("/api/v1/getVotingPower", this::getVotingPower); //done
        post("/api/v1/getOwnerships", this::getOwnerships);
        post("/api/v1/getProposals", this::getProposals); //done
        post("/api/v1/createProposal", this::createProposal); //done
        post("/api/v1/addOwnership", this::addOwnership);
    }

    private String addOwnership(Request req, Response res) {
        //todo should check what happenes if address and owner not set
        String address = req.queryParams("address");
        String owner = req.queryParams("owner");

        if (Constants.MOCK_NSC) {
            SnapshotMethods.addOwnershipEntry(address, owner);
        }
        else {
            int code = 109;
            String description = "Could not add ownership";
            String detail = "Method not supported with real native smart contract. Pls set mock_nsc=true in balancer"; //todo java spark automatically adds escaping
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(Constants.MOCK_MC_ADDRESS_MAP);
    }

    private String getVotingPower(Request req, Response res) throws Exception {

        String address = req.queryParams("addresses");

        if (SnapshotMethods.getActiveProposal() == null) {
            int code = 107;
            String description = "No proposal have been received at this point";
            String detail = "Proposal should be received before getting voting power";
            Gson gson = new Gson();
            return gson.toJson(Helper.buildErrorJsonObject(code, description, detail));
        }

        double balance = RosettaMethods.getAddressBalance(address);

        // Create the Java object representing the JSON structure
        JsonObject jsonObject = new JsonObject();
        JsonArray scoreArray = new JsonArray();
        JsonObject scoreObject = new JsonObject();
        scoreObject.addProperty("address", address);
        scoreObject.addProperty("score", balance);
        scoreObject.addProperty("decimal", 8);
        scoreArray.add(scoreObject);
        jsonObject.add("score", scoreArray);

        // Create Gson instance
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Convert the Java object to JSON string
        res.type("application/json");
        return gson.toJson(jsonObject);
    }

    private String createProposal(Request req, Response res) {
        ChainTip chainTip;

        //
        Gson gson = new Gson();
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
        //

        try {
            chainTip = RosettaMethods.getChainTip();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        ObjectMapper objectMapper = new ObjectMapper();
//        String jsonData = req.body(); // Assuming request.getBody() returns the JSON data as a string

        //todo check block height annotation why
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(ret);
    }
}
