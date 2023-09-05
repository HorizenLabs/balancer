package io.horizen.helpers;

import java.util.*;

public final class Mocks {

    // when a Native smart contract is not available this can be helpful
    // see mockNsc setting in conf file
    public static Map<String, List<String>> mockMcAddressMap = new HashMap<>();

    public static List<String> mockOwnerScAddrList = new ArrayList<>();

    public static final String MOCK_ROSETTA_BLOCK_HASH = "000439739cac7736282169bb10d368123ca553c45ea6d4509d809537cd31aa0d";
    public static final int MOCK_ROSETTA_BLOCK_HEIGHT = 100;
    public static final int MOCK_SNAPSHOT_VALUE = 123456;

    static {
        mockMcAddressMap.put("0x72661045bA9483EDD3feDe4A73688605b51d40c0",
                new ArrayList<>(List.of("ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt")));
        mockMcAddressMap.put("0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
                new ArrayList<>(List.of("ztbX9Kg53BYK8iJ8cydJp3tBYcmzT8Vtxn7", "ztUSSkdLdgCG2HnwjrEKorauUR2JXV26u7v")));
        mockMcAddressMap.put("0xf43F35c1A3E36b5Fb554f5E830179cc460c35858",
                new ArrayList<>(List.of("ztYztK6dH2HiK1mTL1byWGY5hx1TaGNPuen")));

        mockOwnerScAddrList.add("0x72661045bA9483EDD3feDe4A73688605b51d40c0");
        mockOwnerScAddrList.add("0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959");
        mockOwnerScAddrList.add("0xf43F35c1A3E36b5Fb554f5E830179cc460c35858");
    }
}
