# Native smart contract is reachable via this end points
NSC_URL = "http://zendao-tn-1.de.horizenlabs.io:8200/"
# NSC_URL = "http://zendao-test-tn-1.us.horizenlabs.io/dev1/"
# NSC_URL = "http://localhost:8200/"

# This is the address which is used for calling eth_call which invokes the NSC. Some fund must be stored there
# otherwise the call fails (no sufficient balance)
ETH_CALL_FROM_ADDRESS = "0x00c8f107a09cd4f463afc2f1e6e5bf6022ad4600"

# A rosetta instance is running locally (if not mocked)
ROSETTA_URL = "http://localhost:8080/"

NETWORK = "test"
# NETWORK = "main"

# set true if we can not have rosetta for getting balance
mock_rosetta = False

# set true if we can not interact with a real Native Smart Contract
mock_nsc = False

ROSETTA_REQUEST_TEMPLATE = {
    "network_identifier": {
        "blockchain": "Zen",
        "network": NETWORK
    }
}


MOCK_ROSETTA_GET_BALANCE_RESP = {
    "score": [
        {
            "address": "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
            "score": 123456789,
            "decimal": 8
        }
    ]
}


# when a Native smart contract is not available this can be helpful
# see mock_nsc setting
MOCK_MC_ADDRESS_MAP = {
    "0x72661045bA9483EDD3feDe4A73688605b51d40c0": [
        "ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt"
    ],
    "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959": [
        "ztbX9Kg53BYK8iJ8cydJp3tBYcmzT8Vtxn7",
        "ztUSSkdLdgCG2HnwjrEKorauUR2JXV26u7v"
    ],
    "0xf43F35c1A3E36b5Fb554f5E830179cc460c35858": [
        "ztYztK6dH2HiK1mTL1byWGY5hx1TaGNPuen"
    ]
}

MOCK_OWNER_SC_ADDR_LIST = [
    "0x72661045bA9483EDD3feDe4A73688605b51d40c0",
    "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
    "0xf43F35c1A3E36b5Fb554f5E830179cc460c35858"
]

import os
dirname = os.path.dirname(__file__)
PROPOSAL_JSON_DATA_PATH=dirname+'/../storage/'
PROPOSAL_JSON_DATA_FILE_NAME = 'active_proposal.json'


def check_mocks():
    if mock_rosetta:
        print("##################################")
        print("##    MOCKING ROSETTA MODULE    ##")
        print("##################################")

    if mock_nsc:
        print("##################################")
        print("##    MOCKING ROSETTA MODULE    ##")
        print("##################################")
