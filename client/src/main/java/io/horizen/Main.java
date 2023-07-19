package io.horizen;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main {
    private static final String LOCAL_HTTP_SERVER_URL = "http://localhost:8080/";
    private static final String REMOTE_HTTP_SERVER_URL = "http://zendao-tn-1.de.horizenlabs.io:5000/";

    private static final String HTTP_SERVER_URL = LOCAL_HTTP_SERVER_URL;
    // private static final String HTTP_SERVER_URL = REMOTE_HTTP_SERVER_URL;

    private static final Map<String, Object> CREATE_PROPOSAL_MOCK = new HashMap<>();
    private static final Map<String, Object> GET_VOTING_POWER_MOCK = new HashMap<>();
    private static final Map<String, Object> ADD_OWNERSHIP_MOCK = new HashMap<>();

    static {
        CREATE_PROPOSAL_MOCK.put("Body", "Start: 18 Apr 23 13:40 UTC, End: 18 Apr 23 13:45 UTC, " +
                "Author: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959");
        CREATE_PROPOSAL_MOCK.put("ProposalEvent", "proposal/created");
        CREATE_PROPOSAL_MOCK.put("ProposalExpire", 0);
        CREATE_PROPOSAL_MOCK.put("ProposalID", "proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133cabc5");
        CREATE_PROPOSAL_MOCK.put("ProposalSpace", "victorbibiano.eth");

        GET_VOTING_POWER_MOCK.put("options", Map.of("url", HTTP_SERVER_URL + "api/v1/getVotingPower", "type", "api-post"));
        GET_VOTING_POWER_MOCK.put("network", "80001");
        GET_VOTING_POWER_MOCK.put("snapshot", 34522768);
        GET_VOTING_POWER_MOCK.put("addresses", new String[]{"0xf43F35c1A3E36b5Fb554f5E830179cc460c35858"});

        ADD_OWNERSHIP_MOCK.put("owner", "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959");
        ADD_OWNERSHIP_MOCK.put("address", "ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt");
    }

    private static void newProposal() throws IOException {
        Random random = new Random();
        int val = random.nextInt(255);
        CREATE_PROPOSAL_MOCK.put("ProposalID", "proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133c" + val);
        System.out.println("Calling new proposal with data: " + CREATE_PROPOSAL_MOCK);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/createProposal");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(new Gson().toJson(CREATE_PROPOSAL_MOCK).getBytes());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(response.toString());
            }
        } else {
            System.out.println("HTTP request failed with response code: " + responseCode);
        }
    }

    private static void getVotingPower2() throws IOException {
        System.out.println("Calling get voting power with data: " + GET_VOTING_POWER_MOCK);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getVotingPower");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(new Gson().toJson(GET_VOTING_POWER_MOCK).getBytes());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(response.toString());
            }
        } else {
            System.out.println("HTTP request failed with response code: " + responseCode);
        }
    }
    private static void getVotingPower1() throws IOException {
        System.out.println("Calling get voting power with data: " + GET_VOTING_POWER_MOCK);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getVotingPower");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(response.toString());
            }
        } else {
            System.out.println("HTTP request failed with response code: " + responseCode);
        }
    }

    private static void getVotingPower(String address) throws IOException {
        Map<String, Object> cmd;
        if (address != null) {
            cmd = new HashMap<>();
            cmd.put("options", Map.of("url", HTTP_SERVER_URL + "/api/v1/getVotingPower", "type", "api-post"));
            cmd.put("network", "80001");
            cmd.put("snapshot", 34522768);
            cmd.put("addresses", new String[]{address});
        } else {
            cmd = GET_VOTING_POWER_MOCK;
        }

        System.out.println("Calling get voting power with data: " + cmd);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getVotingPower");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(response.toString());
            }
        } else {
            System.out.println("HTTP request failed with response code: " + responseCode);
        }
    }

    private static void addOwnership(String owner, String address) throws IOException {
        Map<String, Object> data;
        if (owner == null || address == null) {
            data = ADD_OWNERSHIP_MOCK;
        } else {
            data = new HashMap<>();
            data.put("owner", owner);
            data.put("address", address);
        }

        System.out.println("Calling add ownership with data: " + data);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/addOwnership");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(new Gson().toJson(data).getBytes());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(response.toString());
            }
        } else {
            System.out.println("HTTP request failed with response code: " + responseCode);
        }
    }

    private static void getProposals() throws IOException {
        System.out.println("Calling get proposals");

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getProposals");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write("{}".getBytes());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(response.toString());
            }
        } else {
            System.out.println("HTTP request failed with response code: " + responseCode);
        }
    }

    private static void getOwnerships(String scAddress) throws IOException {
        System.out.println("Calling get ownerships");

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("scAddress", scAddress);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getOwnerships");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(new Gson().toJson(requestData).getBytes());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(response.toString());
            }
        } else {
            System.out.println("HTTP request failed with response code: " + responseCode);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Command not specified!");
            return;
        }

        // Command to execute
        String command = args[0];
        switch (command) {
            case "new_proposal":
                newProposal();
                break;
            case "get_voting_power":
                if (args.length > 1) {
                    getVotingPower(args[1]);
                } else {
                    getVotingPower(null);
                }
                break;
            case "get_proposals":
                getProposals();
                break;
            case "get_ownerships":
                if (args.length > 1) {
                    getOwnerships(args[1]);
                } else {
                    getOwnerships(null);
                }
                break;
            case "add_ownership":
                if (args.length > 2) {
                    addOwnership(args[1], args[2]);
                } else {
                    addOwnership(null, null);
                }
                break;
            default:
                System.out.println("Unsupported command!");
                break;
        }
    }
}