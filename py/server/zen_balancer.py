import pprint
import sys
import threading
from decimal import Decimal

import requests

from flask import Flask, request, json

COIN = 100000000  # 1 zen in zatoshis, aka zennies

def convertZenniesToZen(valueInZennies):
    return valueInZennies / COIN


ROSETTA_URL = "http://localhost:8080/"
ROSETTA_REQUEST_TEMPLATE = {
    "network_identifier": {
        "blockchain": "Zen",
        "network": "test"
    }
}

MOCK_ROSETTA_RESP = {
    "score": [
        {"address": "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
         "score": ((123456789)),
         "decimal": 8
         }
    ]
}

MC_ADDRESS_MAP = {
    "0x72661045bA9483EDD3feDe4A73688605b51d40c0": ["ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt"],
    "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959": ["ztbX9Kg53BYK8iJ8cydJp3tBYcmzT8Vtxn7", "ztUSSkdLdgCG2HnwjrEKorauUR2JXV26u7v"]
}


def print_incoming(component_tag, endpoint_tag, content):
    print("<<<< " + component_tag + " " + endpoint_tag + " received:")
    print(json.dumps(content, indent=4))

def print_outgoing(component_tag, endpoint_tag, content):
    print(">>>> " + component_tag + " " + endpoint_tag + " sending:")
    print(json.dumps(content, indent=4))


chain_tip_height = -1
chain_tip_hash = ""
mock_rosetta = False
mock_chain_tip = False

def api_server():
    app = Flask(__name__)


    @app.route('/')
    def hello_world():
        return 'Hello World'

    @app.route('/api/v1/createProposal', methods=['POST'])
    def create_proposal():

        global chain_tip_height
        global chain_tip_hash

        content = json.loads(request.data)
        print_incoming("BalancerApiServer", "/api/v1/createProposal", content)

        # update MC chain tip. This block will be used when retieving balances from Rosetta
        (chain_tip_height, chain_tip_hash) = get_chain_tip()

        response = {"status": "Ok"}
        print_outgoing("BalancerApiServer", "/api/v1/createProposal", response)

        return json.dumps(response)


    @app.route('/api/v1/getVotingPower', methods=['POST'])
    def get_voting_power():
        global chain_tip_height
        global chain_tip_hash

        content = json.loads(request.data)
        print_incoming("BalancerApiServer", "/api/v1/getVotingPower", content)

        # Parse requested addresses
        requested_address = content["addresses"][0]


        # Retrieve balance for the addresses
        response = get_address_balance(requested_address)

        # Answer back with the balance
        print_outgoing("BalancerApiServer", "/api/v1/getVotingPower", response)
        return json.dumps(response)
    
    def get_address_balance(sc_address):
        if mock_rosetta:
            # don't go on rosetta, just mock it
            return MOCK_ROSETTA_RESP

        if (chain_tip_height == -1):
            return {
                "error": {
                    "code": 100,
                    "description": "Reference MC block not defined",
                    "detail": "Reference block should be retrieved from Rosetta when a voting proposal is created"
                }
            }

        # Retrieve associated MC addresses balance
        balance = 0

        if sc_address in MC_ADDRESS_MAP:
            mc_addresses = MC_ADDRESS_MAP[sc_address]

            # Call Rosetta endpoint /account/balance
            for mc_address in mc_addresses:
                request_body = ROSETTA_REQUEST_TEMPLATE
                request_body["account_identifier"] = {"address": mc_address, "metadata": {}}
                request_body["block_identifier"] = {"index": int(chain_tip_height), "hash": str(chain_tip_hash) }
                print_outgoing("Rosetta", "/account/balance", request_body)

                response = requests.post(ROSETTA_URL+"account/balance", json.dumps(request_body))
                print_incoming("Rosetta", "/account/balance", response.json())

                amount = int(response.json()["balances"][0]["value"])
                balance += amount

        return {"score": [{"address": sc_address,"score": (balance)}]}


    def get_chain_tip():
        # Retrieve current MC tip

        if (mock_rosetta):
            return 100, "000439739cac7736282169bb10d368123ca553c45ea6d4509d809537cd31aa0d"

        # Call Rosetta endpoint /network/status
        request_body = ROSETTA_REQUEST_TEMPLATE
        print_outgoing("Rosetta", "/network/status", request_body)
        response = requests.post(ROSETTA_URL + "network/status", json.dumps(request_body))
        print_incoming("Rosetta", "/network/status", response.json())
        chain_height = int(response.json()["current_block_identifier"]["index"])
        bestblockhash = str(response.json()["current_block_identifier"]["hash"])

        return chain_height, bestblockhash

    app.run(host = "0.0.0.0", port = 5000)

if __name__ == '__main__':

    args = sys.argv[1:]

    # Options
    if len(args) > 0:
        option = args[0]
        if (option == "no_rosetta"):
            mock_rosetta = True
        elif (option == "default_tip"):
            mock_chain_tip = True
        else:
            print("Unsupported command!")

    # Start http server
    t = threading.Thread(target=api_server, args=())
    t.start()

    


