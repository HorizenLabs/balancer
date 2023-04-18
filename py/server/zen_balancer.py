import logging
import threading
import requests

from flask import Flask, request, json

ROSETTA_URL = "http://localhost:8080/"
ROSETTA_REQUEST_TEMPLATE = {"network_identifier": {
        "blockchain": "Zen",
        "network": "test"
    }}

MC_ADDRESS_MAP = {
    "0x72661045bA9483EDD3feDe4A73688605b51d40c0": ["ztbX9Kg53BYK8iJ8cydJp3tBYcmzT8Vtxn7", "ztUSSkdLdgCG2HnwjrEKorauUR2JXV26u7v"]
}

def api_server():
    app = Flask(__name__)

    @app.route('/')
    def hello_world():
        return 'Hello World'

    @app.route('/api/v1/createProposal', methods=['POST'])
    def create_proposal():
        content = json.loads(request.data)
        print(f"BalancerApiServer /api/v1/createProposal received request: {str(content)}")
        response = json.dumps({"status": "Ok"})
        return response

    # REQUEST
    #{
    #    "options": {
    #      "url": "https://dweet.io:443/dweet/quietly/for/victestapirose001",
    #      "type": "api-post"
    #    },
    #    "network": 80001,
    #    "snapshot": 34482521,
    #    "addresses": [
    #      "0x72661045bA9483EDD3feDe4A73688605b51d40c0"
    #    ]
    #  }
    # RESPONSE
    # {

    # }
    @app.route('/api/v1/getVotingPower', methods=['POST'])
    def get_voting_power():
        content = json.loads(request.data)
        print(f"SecureEnclaveApiServer /api/v1/getVotingPower received request: {content}")

        # Parse requested addresses
        requested_address = content["addresses"][0]
        # Retrieve balance for the addresses
        response = get_address_balance(requested_address)
        # Answer back with the balance
        return response
    
    def get_address_balance(sc_address):
        # Retrieve associated MC addresses
        balance = 0

        if sc_address in MC_ADDRESS_MAP:
            mc_addresses = MC_ADDRESS_MAP[sc_address]

            # Call Rosetta endpoint /account/balance
            for mc_address in mc_addresses:
                request_body = ROSETTA_REQUEST_TEMPLATE
                request_body["account_identifier"] = {"address": mc_address, "metadata": {}}
                print(f"Rosetta request: {request_body}")
                response = requests.post(ROSETTA_URL+"account/balance", json.dumps(request_body))
                amount = int(response.json()["balances"][0]["value"])
                balance += amount
        return json.dumps({"balance": balance})
        
    app.run(host = "0.0.0.0", port = 5000)

if __name__ == '__main__':
 
    # Start http server
    t = threading.Thread(target=api_server, args=())
    t.start()

    


