import datetime
import pprint
import re
import string
import sys
import threading
from binascii import unhexlify

import base58
import requests
from eth_abi import decode
from eth_utils import remove_0x_prefix, to_checksum_address, function_signature_to_4byte_selector, encode_hex

from flask import Flask, request, json
from proposal import VotingProposal


NETWORK = "test"
#NETWORK = "main"

NSC_URL = "http://zendao-tn-1.de.horizenlabs.io:8200/"
#NSC_URL = "http://zendao-test-tn-1.us.horizenlabs.io/dev1/"
#NSC_URL = "http://localhost:8200/"


ROSETTA_URL = "http://localhost:8080/"
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

MC_ADDRESS_MAP = {
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


mock_rosetta = False
mock_nsc = False

proposal_dict = {}
active_proposal = None

def print_incoming(component_tag, endpoint_tag, content):
    print("<<<< " + component_tag + " " + endpoint_tag + " received:")
    print(json.dumps(content, indent=4))
    print()


def print_outgoing(component_tag, endpoint_tag, content):
    print(">>>> " + component_tag + " " + endpoint_tag + " sending:")
    print(json.dumps(content, indent=4))
    print()


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

def add_ownership_entry(data_json):
    try:
        new_owner = data_json['owner']
        new_addr = data_json['address']
        base58.b58decode_check(new_addr)

    except KeyError as e:
        response = {
            "error": {
                "code": 101,
                "description": "Can not add ownership",
                "detail": "invalid json request data, could not find field: " + str(e)
            }
        }
    except ValueError as e:
        response = {
            "error": {
                "code": 102,
                "description": "Can not add ownership",
                "detail": "address not valid: " + str(e)
            }
        }
    else:
        if len(new_owner) != 42 or not all(c in string.hexdigits for c in new_owner[2:]):
            response = {
                "error": {
                    "code": 103,
                    "description": "Can not add ownership",
                    "detail": "Invalid owner string length != 42 or not an hex string"
                }
            }
        else:

            if new_owner in MC_ADDRESS_MAP.keys():
                # this is actually a reference
                addresses = MC_ADDRESS_MAP[data_json['owner']]
                found = False
                for entry in addresses:
                    if entry == new_addr:
                        found = True
                        break
                if not found:
                    addresses.append(data_json['address'])
                    response = {"status": "Ok"}
                else:
                    response = {
                        "error": {
                            "code": 104,
                            "description": "Can not add ownership",
                            "detail": "Ownership already set"
                        }
                    }
            else:
                MC_ADDRESS_MAP[new_owner] = [new_addr]
                response = {"status": "Ok"}

    return response


def api_server():
    app = Flask(__name__)


    @app.route('/api/v1/addOwnership', methods=['POST'])
    def add_ownership():
        proposal = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/addOwnership", proposal)

        ret = {
            'result' : add_ownership_entry(proposal),
            'ownerships': MC_ADDRESS_MAP,
        }
        print_outgoing("BalancerApiServer", "/api/v1/addOwnership", ret)

        return json.dumps(ret)

    @app.route('/api/v1/getOwnerships', methods=['POST'])
    def get_ownerships():
        proposal = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getOwnerships", proposal)

        if mock_nsc:
            ret = MC_ADDRESS_MAP
        else:
            sc_address = remove_0x_prefix(proposal['scAddress'])
            ret = get_nsc_ownerships(sc_address)
        print_outgoing("BalancerApiServer", "/api/v1/getOwnerships", ret)

        return json.dumps(ret)

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
        try:
            # update MC chain tip. This block will be used when retieving balances from Rosetta
            (chain_tip_height, chain_tip_hash) = get_chain_tip()
        except Exception as e:
            response = {
                "error": {
                    "code": 105,
                    "description": "Can not create proposal",
                    "detail": "can not determine main chain best block: " + str(e)
                }
            }
        else:
            try:
                store_proposal_data(proposal,  chain_tip_height, chain_tip_hash)
            except Exception as e:
                response = {
                    "error": {
                        "code": 106,
                        "description": "Can not create proposal",
                        "detail": "proposal data format not expected: " + str(e)
                    }
                }
            else:
                response = {"status": "Ok"}

            print_outgoing("BalancerApiServer", "/api/v1/createProposal", response)

        return json.dumps(response)


    @app.route('/api/v1/getVotingPower', methods=['GET'])
    def get_voting_power():
        global active_proposal

        content = request.args
        print_incoming("BalancerApiServer", "/api/v1/getVotingPower", content)

        if (active_proposal == None):
            err = {
                "error": {
                    "code": 107,
                    "description": "No proposal have been received at this point",
                    "detail": "Proposal should be received before getting voting power"
                }
            }
            print_outgoing("BalancerApiServer", "/api/v1/getVotingPower", err)
            return err

        else:
            print("getting voting power for active proposal: " + json.dumps(active_proposal.toJSON(), indent=4))


        # Parse requested address. In GET this is one address actually
        requested_address = content["addresses"]

        # Retrieve balance for the owned MC addresses
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
                    "code": 108,
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

    def get_nsc_ownerships1(sc_address=None):
        # Call eon endpoint /transaction/getKeysOwnership which call NativeSmartContract interface
        request_body = {"scAddressOpt" : sc_address}
        print_outgoing("NSC", "/transaction/getKeysOwnership", request_body)
        response = requests.post(NSC_URL + "transaction/getKeysOwnership", json.dumps(request_body), auth=('user', 'Horizen'))
        print_incoming("NSC", "/transaction/getKeysOwnership", response.json())
        return response.json()['result']['keysOwnership']

    def get_nsc_ownerships(sc_address=None):
        if sc_address is None:
            method = 'getAllKeyOwnerships()'
            abi_str = encode_hex(function_signature_to_4byte_selector(method))
        else:
            method = 'getKeyOwnerships(address)'
            abi_method_str = encode_hex(function_signature_to_4byte_selector(method))
            abi_str = abi_method_str + "000000000000000000000000" + sc_address

        request_body = {
          "jsonrpc": "2.0",
          "method": "eth_call",
          "params": [
            {
              "from": "0x00c8f107a09cd4f463afc2f1e6e5bf6022ad4600",
              "to": "0x0000000000000000000088888888888888888888",
              "value": "0x00",
              "gasLimit": "0x21000",
              "maxPriorityFeePerGas": "0x900000000",
              "maxFeePerGas": "0x900000000",
              "data": abi_str
            }
          ],
          "id": 1
        }
        print_outgoing("NSC", "/ethv1/eth_call (getKeysOwnership)", request_body)
        response = requests.post(NSC_URL + "ethv1", json.dumps(request_body), auth=('user', 'Horizen'))
        abiReturnValue = remove_0x_prefix(response.json()['result'])

        ret = get_key_ownership_from_abi(abiReturnValue)
        print_incoming("NSC", "/ethv1/eth_call (getKeysOwnership)", response.json())
        return ret

    def hex_str_to_bytes(hex_str):
        return unhexlify(hex_str.encode('ascii'))

    def get_key_ownership_from_abi(abiReturnValue):
        # the location of the data part of the first (the only one in this case) parameter (dynamic type), measured in bytes from the start
        # of the return data block. In this case 32 (0x20)
        start_data_offset = decode(['uint32'], hex_str_to_bytes(abiReturnValue[0:64]))[0] * 2

        end_offset = start_data_offset + 64  # read 32 bytes
        list_size = decode(['uint32'], hex_str_to_bytes(abiReturnValue[start_data_offset:end_offset]))[0]

        sc_associations_dict = {}
        for i in range(list_size):
            start_offset = end_offset
            end_offset = start_offset + 192  # read (32 + 32 + 32) bytes
            (address_pref, mca3, mca32) = decode(['address', 'bytes3', 'bytes32'],
                                                 hex_str_to_bytes(abiReturnValue[start_offset:end_offset]))
            sc_address_checksum_fmt = to_checksum_address(address_pref)
            print("sc addr=" + sc_address_checksum_fmt)
            if sc_associations_dict.get(sc_address_checksum_fmt) is not None:
                mc_addr_list = sc_associations_dict.get(sc_address_checksum_fmt)
            else:
                sc_associations_dict[sc_address_checksum_fmt] = []
                mc_addr_list = []
            mc_addr = (mca3 + mca32).decode('utf-8')
            mc_addr_list.append(mc_addr)
            print("mc addr=" + mc_addr)
            sc_associations_dict[sc_address_checksum_fmt] = mc_addr_list

        pprint.pprint(sc_associations_dict)
        return (sc_associations_dict)

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

    


