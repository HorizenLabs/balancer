import os
from pathlib import Path


def get_project_root() -> Path:
    return Path(__file__).parent.parent


# The environment for setting the definitions is loaded from .env file
# --------------------------------------------------------------------

# Running on local host (for testing/debug)
RUNNING_ON_LOCALHOST = os.getenv("RUNNING_ON_LOCALHOST").lower() in ('true', '1', 'y', 'yes')

# Listening on HTTP (not on HTTPS). This option should be set to true if USING_WSGI_PROXY
LISTENING_ON_HTTP = os.getenv("LISTENING_ON_HTTP").lower() in ('true', '1', 'y', 'yes')

# port where balancer is listening
BALANCER_PORT = os.getenv("BALANCER_PORT")

# Balancer is using a wsgi proxy (nginx)
USING_WSGI_PROXY = os.getenv("USING_WSGI_PROXY").lower() in ('true', '1', 'y', 'yes')

# Native smart contract is reachable via this end point
NSC_URL = str(os.getenv("NSC_URL"))

# This is the address which is used for calling eth_call which invokes the NSC. Some fund must be stored there
# otherwise the call fails (no sufficient balance)
ETH_CALL_FROM_ADDRESS = str(os.getenv("ETH_CALL_FROM_ADDRESS"))

# A rosetta instance is running locally (if not mocked)
ROSETTA_URL = str(os.getenv("ROSETTA_URL"))

# The network type which rosetta is running on. Can be 'test' or 'main'
ROSETTA_NETWORK_TYPE = str(os.getenv("ROSETTA_NETWORK_TYPE"))

# Snapshot API server (if not mocked)
SNAPSHOT_URL = str(os.getenv("SNAPSHOT_URL"))

# The json file where the active proposal is stored
PROPOSAL_JSON_DATA_PATH = str(os.getenv("PROPOSAL_JSON_DATA_PATH"))
PROPOSAL_JSON_DATA_FILE_NAME = str(os.getenv("PROPOSAL_JSON_DATA_FILE_NAME"))

# set true if we can not have rosetta for getting balance
MOCK_ROSETTA = os.getenv("MOCK_ROSETTA").lower() in ('true', '1', 'y', 'yes')

# set true if we do not have snapshot.org for getting proposal details
MOCK_SNAPSHOT = os.getenv("MOCK_SNAPSHOT").lower() in ('true', '1', 'y', 'yes')

# set true if we can not interact with a real Native Smart Contract
MOCK_NSC = os.getenv("MOCK_NSC").lower() in ('true', '1', 'y', 'yes')

# ----------------------------------------------------------------------------------------
# these constants are used in case we are mocking some module
MOCK_SNAPSHOT_VALUE = 123456

MOCK_ROSETTA_GET_BALANCE_RESP = {
    "score": [
        {
            "address": "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
            "score": 123456789
        }
    ]
}

MOCK_ROSETTA_BLOCK_HASH = "000439739cac7736282169bb10d368123ca553c45ea6d4509d809537cd31aa0d"
MOCK_ROSETTA_BLOCK_HEIGHT = 100
MOCK_ROSETTA_NETWORK_STATUS_RETURN = {
    "current_block_identifier": {
        "index": MOCK_ROSETTA_BLOCK_HEIGHT,
        "hash": MOCK_ROSETTA_BLOCK_HASH}
}

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


def check_mocks():
    if MOCK_ROSETTA:
        print("##################################")
        print("##    MOCKING ROSETTA MODULE    ##")
        print("##################################")

    if MOCK_NSC:
        print("##################################")
        print("##    MOCKING NSC MODULE        ##")
        print("##################################")

    if MOCK_SNAPSHOT:
        print("##################################")
        print("##    MOCKING SNAPSHOT MODULE   ##")
        print("##################################")


# these definitions are used for querying rosetta component and should not be modified
# ------------------------------------------------------------------------------------
ROSETTA_REQUEST_NETWORK_STATUS_TEMPLATE = {
    "network_identifier": {
        "blockchain": "Zen",
        "network": ROSETTA_NETWORK_TYPE
    }
}

ROSETTA_REQUEST_BLOCK_TEMPLATE = {
    "network_identifier": {
        "blockchain": "Zen",
        "network": ROSETTA_NETWORK_TYPE
    },
    "block_identifier": {
        "index": 0
    }
}

# these definitions are used for calling snapshot API and should not be modified
# unless different queries are needed
# See: https://docs.snapshot.org/tools/api
# ------------------------------------------------------------------------------------
SNAPSHOT_REQUEST_QUERY_STRING = "query Proposal {proposal(id: SNAPSHOT_PROPOSAL_ID) {snapshot}}"
