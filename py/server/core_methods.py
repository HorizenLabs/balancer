import datetime
import json
import pprint
import re
import string
from binascii import unhexlify
import base58
import requests
from eth_abi import decode
from eth_utils import remove_0x_prefix, to_checksum_address, function_signature_to_4byte_selector, encode_hex

from definitions import MOCK_MC_ADDRESS_MAP, NSC_URL, mock_nsc, mock_rosetta, MOCK_ROSETTA_GET_BALANCE_RESP, \
    ROSETTA_REQUEST_TEMPLATE, ROSETTA_URL
from proposal import VotingProposal
from util_methods import print_outgoing, print_incoming, check_sc_address

active_proposal = VotingProposal(in_id=None)


def extract_body_attributes(body_string):
    # "Body": "Start: 18 Apr 23 13:40 UTC, End: 18 Apr 23 13:45 UTC, Author: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959"
    s1 = re.split(',', body_string)
    from_string = re.split('Start: ', s1[0])[1]
    to_string = re.split('End: ', s1[1])[1]
    auth_string = re.split('Author: ', s1[2])[1]

    from_time = datetime.datetime.strptime(from_string, '%d %b %y %H:%M %Z')
    to_time = datetime.datetime.strptime(to_string, '%d %b %y %H:%M %Z')

    return from_time, to_time, auth_string


def store_proposal_data(proposal_json, chain_tip_height, chain_tip_hash):
    global active_proposal

    prop_id = proposal_json['ProposalID']
    if prop_id in proposal_dict.keys():
        return

    from_time, to_time, author = extract_body_attributes(body_string=proposal_json['Body'])

    prop = VotingProposal(
        in_id=prop_id,
        bl_height=chain_tip_height,
        bl_hash=chain_tip_hash,
        from_time=from_time,
        to_time=to_time,
        author=author)

    proposal_dict[prop_id] = prop
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

            if new_owner in MOCK_MC_ADDRESS_MAP.keys():
                # this is actually a reference
                addresses = MOCK_MC_ADDRESS_MAP[data_json['owner']]
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
                MOCK_MC_ADDRESS_MAP[new_owner] = [new_addr]
                response = {"status": "Ok"}

    return response


def get_nsc_ownerships1(sc_address=None):
    # Call eon endpoint /transaction/getKeysOwnership which call NativeSmartContract interface
    request_body = {"scAddressOpt": sc_address}
    print_outgoing("NSC", "/transaction/getKeysOwnership", request_body)
    response = requests.post(NSC_URL + "transaction/getKeysOwnership", json.dumps(request_body),
                             auth=('user', 'Horizen'))
    print_incoming("NSC", "/transaction/getKeysOwnership", response.json())
    return response.json()['result']['keysOwnership']


def get_nsc_ownerships(sc_address=None):
    if sc_address is None:
        method = 'getAllKeyOwnerships()'
        abi_str = encode_hex(function_signature_to_4byte_selector(method))
    else:
        sc_address = remove_0x_prefix(sc_address)
        check_sc_address(sc_address)
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
    abi_return_value = remove_0x_prefix(response.json()['result'])

    ret = get_key_ownership_from_abi(abi_return_value)
    print_incoming("NSC", "/ethv1/eth_call (getKeysOwnership)", response.json())
    return ret


def hex_str_to_bytes(hex_str):
    return unhexlify(hex_str.encode('ascii'))


def get_key_ownership_from_abi(abi_return_value):
    # the location of the data part of the first (the only one in this case) parameter (dynamic type), measured in bytes
    # from the start of the return data block. In this case 32 (0x20)
    start_data_offset = decode(['uint32'], hex_str_to_bytes(abi_return_value[0:64]))[0] * 2

    end_offset = start_data_offset + 64  # read 32 bytes
    list_size = decode(['uint32'], hex_str_to_bytes(abi_return_value[start_data_offset:end_offset]))[0]

    sc_associations_dict = {}
    for i in range(list_size):
        start_offset = end_offset
        end_offset = start_offset + 192  # read (32 + 32 + 32) bytes
        (address_pref, mca3, mca32) = decode(['address', 'bytes3', 'bytes32'],
                                             hex_str_to_bytes(abi_return_value[start_offset:end_offset]))
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
    return sc_associations_dict


def get_mc_address_map(sc_address=None):
    if mock_nsc:
        return MOCK_MC_ADDRESS_MAP
    else:
        return get_nsc_ownerships(sc_address)


def get_address_balance(sc_address):
    if active_proposal.is_null():
        return {
            "error": {
                "code": 108,
                "description": "Reference MC block not defined",
                "detail": "Reference block should be retrieved from Rosetta when a voting proposal is created"
            }
        }

    # Retrieve associated MC addresses balance
    balance = 0

    try:
        mc_address_map = get_mc_address_map(sc_address)
    except Exception as e:
        return {
            "error": {
                "code": 108,
                "description": "Could not get ownership for sc address:" + sc_address,
                "detail": "An exception occurred: " + str(e)
            }
        }

    if sc_address in mc_address_map:
        mc_addresses = mc_address_map[sc_address]

        if mock_rosetta:
            # don't go on rosetta, just mock it
            print("-------------MOCK ROSETTA RESPONSE------------")
            return MOCK_ROSETTA_GET_BALANCE_RESP

        # Call Rosetta endpoint /account/balance
        for mc_address in mc_addresses:
            request_body = ROSETTA_REQUEST_TEMPLATE
            request_body["account_identifier"] = {"address": mc_address, "metadata": {}}
            request_body["block_identifier"] = {"index": int(active_proposal.block_height),
                                                "hash": str(active_proposal.block_hash)}
            print_outgoing("Rosetta", "/account/balance", request_body)

            response = requests.post(ROSETTA_URL + "account/balance", json.dumps(request_body))
            print_incoming("Rosetta", "/account/balance", response.json())

            amount = int(response.json()["balances"][0]["value"])
            print("address={} / amount={}".format(mc_address, amount))
            balance += amount

    return {
        "score": [
            {
                "address": sc_address,
                "score": balance
            }
        ]
    }


def get_chain_tip():
    # Retrieve current MC tip

    if mock_rosetta:
        return 100, "000439739cac7736282169bb10d368123ca553c45ea6d4509d809537cd31aa0d"

    # Call Rosetta endpoint /network/status
    request_body = ROSETTA_REQUEST_TEMPLATE
    print_outgoing("Rosetta", "/network/status", request_body)
    response = requests.post(ROSETTA_URL + "network/status", json.dumps(request_body))
    print_incoming("Rosetta", "/network/status", response.json())
    chain_height = int(response.json()["current_block_identifier"]["index"])
    best_block_hash = str(response.json()["current_block_identifier"]["hash"])

    return chain_height, best_block_hash


def get_active_proposal():
    return active_proposal


proposal_dict = {}
