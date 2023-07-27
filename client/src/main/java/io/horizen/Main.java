package io.horizen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main {
    private static final String LOCAL_HTTP_SERVER_URL = "https://localhost:8080/";
    private static final String REMOTE_HTTP_SERVER_URL = "http://zendao-tn-1.de.horizenlabs.io:5000/";

    private static final String HTTP_SERVER_URL = LOCAL_HTTP_SERVER_URL;
    // private static final String HTTP_SERVER_URL = REMOTE_HTTP_SERVER_URL;

    private static final Map<String, Object> CREATE_PROPOSAL_MOCK = new HashMap<>();
    private static final Map<String, Object> GET_VOTING_POWER_MOCK = new HashMap<>();
    private static final Map<String, Object> ADD_OWNERSHIP_MOCK = new HashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

    // Helper method to convert a map to a query string
    private static String getQueryString(Map<String, Object> params) {
        StringBuilder result = new StringBuilder();
        String[] addressArray = (String[]) params.get("addresses");
        Map<String,String> optionsMap = (Map<String,String>) params.get("options");
        result.append("network").append("=").append(params.get("network")).append("&");
        result.append("snapshot").append("=").append(params.get("snapshot")).append("&");
        result.append("addresses").append("=").append(addressArray[0]).append("&");
        result.append("options").append("=").append(optionsMap.get("type")).append("&");
        result.append("options").append("=").append(optionsMap.get("url"));

        return result.toString();
    }

    private static void newProposal() throws IOException {
        Random random = new Random();
        int val = random.nextInt(255);
        CREATE_PROPOSAL_MOCK.put("ProposalID", "proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133c" + val);
        System.out.println("Calling new proposal with data: " + CREATE_PROPOSAL_MOCK);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/createProposal");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(gson.toJson(CREATE_PROPOSAL_MOCK).getBytes());
        }

        printResponse(connection);
    }

    private static void getVotingPower2() throws IOException {
        System.out.println("Calling get voting power with data: " + GET_VOTING_POWER_MOCK);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getVotingPower");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(gson.toJson(GET_VOTING_POWER_MOCK).getBytes());
        }

        printResponse(connection);
    }
    private static void getVotingPower1() throws IOException {
        System.out.println("Calling get voting power with data: " + GET_VOTING_POWER_MOCK);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getVotingPower");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        printResponse(connection);
    }

    private static void getVotingPower(String address) throws IOException {
        Map<String, Object> cmd;
        if (address != null) {
            cmd = new HashMap<>();
            cmd.put("options", Map.of("url", HTTP_SERVER_URL + "api/v1/getVotingPower", "type", "api-post"));
            cmd.put("network", "80001");
            cmd.put("snapshot", 34522768);
            cmd.put("addresses", new String[]{address});
        } else {
            cmd = GET_VOTING_POWER_MOCK;
        }

        System.out.println("Calling get voting power with data: " + cmd);

        String queryString = getQueryString(cmd);
        String baseUrl = HTTP_SERVER_URL + "api/v1/getVotingPower";
        String fullUrl = baseUrl + "?" + queryString;

        URL url = new URL(fullUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        printResponse(connection);
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
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(gson.toJson(data).getBytes());
        }

        printResponse(connection);
    }

    private static void getProposals() throws IOException {
        System.out.println("Calling get proposals");

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getProposals");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write("{}".getBytes());
        }

        printResponse(connection);
    }

    private static void getOwnerships(String scAddress) throws IOException {
        System.out.println("Calling get ownerships");

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("scAddress", scAddress);

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getOwnerships");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(gson.toJson(requestData).getBytes());
        }

        printResponse(connection);
    }

    private static void getOwnerScAddresses() throws IOException {
        System.out.println("Calling get owner sc addresses");

        URL url = new URL(HTTP_SERVER_URL + "api/v1/getOwnerScAddresses");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        printResponse(connection);
    }

    private static void printResponse(HttpsURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String responseJsonPretty = gson.toJson(gson.fromJson(response.toString(), Object.class));
                System.out.println(responseJsonPretty);
            }
        } else {
            System.out.println("HTTP request failed with response code: " + responseCode);
        }
    }

    public static void main(String[] args) throws Exception {
        setupSSL();

        if (args.length == 0) {
            System.out.println("Command not specified!");
            return;
        }

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
            case "get_owner_sc_addresses":
                getOwnerScAddresses();
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

    private static void setupSSL() throws Exception {
        // Load the keystore with the certificate
        String keystorePath = "keystore/keystore.p12";
        String keystorePassword = "mypassword";
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, keystorePassword.toCharArray());

        // create a custom trustmanager
        TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Create and set up the SSLContext
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustAllCertificates, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}