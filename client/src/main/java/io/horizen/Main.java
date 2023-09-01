package io.horizen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main {
    private static final Logger log =  LoggerFactory.getLogger(Main.class);
    private static final String LOCAL_HTTP_SERVER_URL = "http://localhost:5000/"; // change if https not used
    private static final String REMOTE_HTTP_SERVER_URL = "http://zendao-tn-1.de.horizenlabs.io:5000/";
    private static final String HTTP_SERVER_URL = LOCAL_HTTP_SERVER_URL;

    private static final Map<String, Object> CREATE_PROPOSAL_MOCK = new HashMap<>();
    private static final Map<String, Object> GET_VOTING_POWER_MOCK = new HashMap<>();
    private static final Map<String, Object> ADD_OWNERSHIP_MOCK = new HashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static {
        CREATE_PROPOSAL_MOCK.put("Body",  "HAL new format of notification\nProposal Created\nStarts on: 28 Aug 23 13:27 UTC\nEnds on: 31 Oct 23 13:27 UTC\nAuthor: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959");
        CREATE_PROPOSAL_MOCK.put("ProposalEvent", "proposal/created");
        CREATE_PROPOSAL_MOCK.put("ProposalExpire", 0);
        CREATE_PROPOSAL_MOCK.put("ProposalID", "proposal/0xf8c1b7d5502a89433fda48cf9ec4396f2ff0af1559e02c6ba16e0679033a5f01");
        CREATE_PROPOSAL_MOCK.put("ProposalSpace", "victorbibiano.eth");

        GET_VOTING_POWER_MOCK.put("options", Map.of("url", HTTP_SERVER_URL + "api/v1/getVotingPower", "type", "api-post"));
        GET_VOTING_POWER_MOCK.put("network", "80001");
        GET_VOTING_POWER_MOCK.put("snapshot", 34522768);
        GET_VOTING_POWER_MOCK.put("addresses", new String[]{"0x92f375E3f733b010E7620FBed122B9d96f73c6CF"});

        ADD_OWNERSHIP_MOCK.put("owner", "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959");
        ADD_OWNERSHIP_MOCK.put("address", "ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt");
    }


    public static HttpURLConnection sendRequest(String urlName, byte[] dataToWrite, boolean isPost) throws IOException {
        URL url = new URL(HTTP_SERVER_URL + urlName);
        HttpURLConnection connection = HTTP_SERVER_URL.contains("https") ? (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();

        if (isPost) {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
        }
        else
            connection.setRequestMethod("GET");

        if (dataToWrite != null) {
            connection.setDoOutput(true);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(dataToWrite);
            }
        }

        return connection;
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
        CREATE_PROPOSAL_MOCK.put("ProposalID", "proposal/0xf8c1b7d5502a89433fda48cf9ec4396f2ff0af1559e02c6ba16e0679033a5f01");
        log.info("Calling new proposal with data: " + CREATE_PROPOSAL_MOCK);

        HttpURLConnection connection = sendRequest("api/v1/createProposal", gson.toJson(CREATE_PROPOSAL_MOCK).getBytes(), true);
        printResponse(connection);
    }

    private static void getVotingPower2() throws IOException {
        log.info("Calling get voting power with data: " + GET_VOTING_POWER_MOCK);
        HttpURLConnection connection = sendRequest("api/v1/getVotingPower", gson.toJson(GET_VOTING_POWER_MOCK).getBytes(), true);
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

        log.info("Calling get voting power with data: " + cmd);

        String queryString = getQueryString(cmd);
        String baseUrl = "api/v1/getVotingPower";
        String fullUrl = baseUrl + "?" + queryString;

        HttpURLConnection connection = sendRequest(fullUrl, null,false);
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

        log.info("Calling add ownership with data: " + data);

        HttpURLConnection connection = sendRequest("api/v1/addOwnership", gson.toJson(data).getBytes(),true);
        printResponse(connection);
    }

    private static void getProposals() throws IOException {
        log.info("Calling get proposals");

        HttpURLConnection connection = sendRequest("api/v1/getProposals", "{}".getBytes(),true);
        printResponse(connection);
    }

    private static void getOwnerships(String scAddress) throws IOException {
        log.info("Calling get ownerships");

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("scAddress", scAddress);

        HttpURLConnection connection = sendRequest("api/v1/getOwnerships", gson.toJson(requestData).getBytes(),true);
        printResponse(connection);
    }

    private static void getOwnerScAddresses() throws IOException {
        log.info("Calling get owner sc addresses");
        HttpURLConnection connection = sendRequest("api/v1/getOwnerScAddresses", null,true);
        printResponse(connection);
    }

    private static void printResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String responseJsonPretty = gson.toJson(gson.fromJson(response.toString(), Object.class));
                log.info(responseJsonPretty);
            }
        } else {
            log.error("HTTP request failed with response code: " + responseCode);
        }
    }

    public static void main(String[] args) {
        try {
            if (HTTP_SERVER_URL.contains("https"))
                setupSSL();

            if (args.length == 0) {
                log.error("Command not specified!");
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
                    log.error("Unsupported command!");
                    break;
            }
        } catch (Exception ex) {
            log.error(ex.toString());
            throw new RuntimeException(ex);
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