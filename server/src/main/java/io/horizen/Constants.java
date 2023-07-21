package io.horizen;

import java.util.*;

//todo maybe put constants in config file
public final class Constants {

    // Native smart contract is reachable via this end points
    public static final String NSC_URL = "http://zendao-tn-1.de.horizenlabs.io:8200/";
//    public static final String NSC_URL = "http://zendao-test-tn-1.us.horizenlabs.io/dev1/";
//    public static final String NSC_URL = "http://localhost:8200/";

    // This is the address which is used for calling eth_call which invokes the NSC. Some fund must be stored there
    // otherwise the call fails (no sufficient balance)
    public static final String ETH_CALL_FROM_ADDRESS = "0x00c8f107a09cd4f463afc2f1e6e5bf6022ad4600";

    // A rosetta instance is running locally (if not mocked)
    public static final String ROSETTA_URL = "http://localhost:8080/";

    public static final String NETWORK = "test";
//    public static final String NETWORK = "main";

    // set true if we can not have rosetta for getting balance
    public static final Boolean MOCK_ROSETTA = true;

    // set true if we can not interact with a real Native Smart Contract
    public static final Boolean MOCK_NSC = false;


    // when a Native smart contract is not available this can be helpful
    // see MOCK_NSC setting
    public static Map<String, List<String>> MOCK_MC_ADDRESS_MAP = new HashMap<>();

    public static List<String> MOCK_OWNER_SC_ADDR_LIST = new ArrayList<>();

    static {
        MOCK_MC_ADDRESS_MAP.put("0x72661045bA9483EDD3feDe4A73688605b51d40c0",
                new ArrayList<>(List.of("ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt")));
        MOCK_MC_ADDRESS_MAP.put("0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
                new ArrayList<>(List.of("ztbX9Kg53BYK8iJ8cydJp3tBYcmzT8Vtxn7", "ztUSSkdLdgCG2HnwjrEKorauUR2JXV26u7v")));
        MOCK_MC_ADDRESS_MAP.put("0xf43F35c1A3E36b5Fb554f5E830179cc460c35858",
                new ArrayList<>(List.of("ztYztK6dH2HiK1mTL1byWGY5hx1TaGNPuen")));

        MOCK_OWNER_SC_ADDR_LIST.add("0x72661045bA9483EDD3feDe4A73688605b51d40c0");
        MOCK_OWNER_SC_ADDR_LIST.add("0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959");
        MOCK_OWNER_SC_ADDR_LIST.add("0xf43F35c1A3E36b5Fb554f5E830179cc460c35858");
    }

    // Get the current working directory
    public static String currentDirectory = System.getProperty("user.dir");

    // Construct the path to the storage directory relative to the current working directory
    public static String proposalJsonDataPath = currentDirectory + "/server/src/main/resources/";

    // Construct the full path to the active_proposal.json file
    public static String proposalJsonDataFileName = "active_proposal.json";
}
