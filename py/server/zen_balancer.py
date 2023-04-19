import datetime
import re
import sys
import threading
import requests

from flask import Flask, request, json
from proposal import VotingProposal

ROSETTA_URL = "http://localhost:8080/"
ROSETTA_REQUEST_TEMPLATE = {
    "network_identifier": {
        "blockchain": "Zen",
        "network": "test"
    }
}

MOCK_ROSETTA_GET_BALANCE_RESP = {
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


mock_rosetta = False

proposal_dict = {}
active_proposal = None

def print_incoming(component_tag, endpoint_tag, content):
    print("<<<< " + component_tag + " " + endpoint_tag + " received:")
    print(json.dumps(content, indent=4))

def print_outgoing(component_tag, endpoint_tag, content):
    print(">>>> " + component_tag + " " + endpoint_tag + " sending:")
    print(json.dumps(content, indent=4))

def extract_body_attributes(body_string):
    # "Body": "Start: 18 Apr 23 13:40 UTC, End: 18 Apr 23 13:45 UTC, Author: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959"
    s1 = re.split(',', body_string)
    from_string = re.split('Start: ', s1[0])[1]
    to_string = re.split('End: ', s1[1])[1]
    auth_string = re.split('Author: ', s1[2])[1]

    fromTime = datetime.datetime.strptime(from_string, '%d %b %y %H:%M %Z')
    toTime = datetime.datetime.strptime(to_string, '%d %b %y %H:%M %Z')

    return (fromTime, toTime, auth_string)

def store_proposal_data(proposal_json, chain_tip_height, chain_tip_hash):

    global active_proposal
    global proposal_dict

    id = proposal_json['ProposalID']
    if id in proposal_dict.keys():
        return

    fromTime, toTime, author = extract_body_attributes(body_string = proposal_json['Body'])

    prop = VotingProposal(
        id=id,
        bl_height=chain_tip_height,
        bl_hash=chain_tip_hash,
        from_time=fromTime,
        to_time=toTime,
        author=author)

    proposal_dict[id] = prop
    active_proposal = prop


def api_server():
    app = Flask(__name__)


    @app.route('/api/v1/getProposals', methods=['POST'])
    def get_proposals():
        proposal = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getProposals", proposal)
        response = [prop.toJSON() for prop in proposal_dict.values()]
        print_outgoing("BalancerApiServer", "/api/v1/getProposals", response)

        return json.dumps(response)


    @app.route('/api/v1/createProposal', methods=['POST'])
    def create_proposal():

        proposal = json.loads(request.data)
        print_incoming("BalancerApiServer", "/api/v1/createProposal", proposal)

        # update MC chain tip. This block will be used when retieving balances from Rosetta
        (chain_tip_height, chain_tip_hash) = get_chain_tip()

        store_proposal_data(proposal,  chain_tip_height, chain_tip_hash)

        response = {"status": "Ok"}
        print_outgoing("BalancerApiServer", "/api/v1/createProposal", response)

        return json.dumps(response)


    @app.route('/api/v1/getVotingPower', methods=['POST'])
    def get_voting_power():
        global active_proposal

        content = json.loads(request.data)
        print_incoming("BalancerApiServer", "/api/v1/getVotingPower", content)

        if (active_proposal == None):
            err = {
                "error": {
                    "code": 101,
                    "description": "No proposal have been received at this point",
                    "detail": "Proposal should be received before getting voting power"
                }
            }
            print_outgoing("BalancerApiServer", "/api/v1/getVotingPower", err)
            return err

        else:
            print("getting voting power for active proposal: " + json.dumps(active_proposal.toJSON(), indent=4))


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
            return MOCK_ROSETTA_GET_BALANCE_RESP

        if active_proposal == None:
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
                request_body["block_identifier"] = {"index": int(active_proposal.block_height), "hash": str(active_proposal.block_hash) }
                print_outgoing("Rosetta", "/account/balance", request_body)

                response = requests.post(ROSETTA_URL+"account/balance", json.dumps(request_body))
                print_incoming("Rosetta", "/account/balance", response.json())

                amount = int(response.json()["balances"][0]["value"])
                print("address={} / amount={}".format(mc_address, amount))
                balance += amount

        return {
            "score": [
                {
                    "address": sc_address,
                    "score": (balance)
                }
            ]
        }


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
        else:
            print("Unsupported command!")

    # Start http server
    t = threading.Thread(target=api_server, args=())
    t.start()

    


