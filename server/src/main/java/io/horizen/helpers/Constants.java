package io.horizen.helpers;

import java.util.*;

public final class Constants {

    // This is the address which is used for calling eth_call which invokes the NSC. Some fund must be stored there
    // otherwise the call fails (no sufficient balance)
    public static final String ETH_CALL_FROM_ADDRESS = "0x00c8f107a09cd4f463afc2f1e6e5bf6022ad4600";

    // when a Native smart contract is not available this can be helpful
    // see mockNsc setting in conf file
    public static Map<String, List<String>> mockMcAddressMap = new HashMap<>();

    public static List<String> mockOwnerScAddrList = new ArrayList<>();

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
