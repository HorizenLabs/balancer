import json
import requests

from .balancerError import GetProposalError, GenericError
from .snapshot_methods import get_mc_address_map, get_active_proposal
from .definitions import MOCK_ROSETTA, MOCK_ROSETTA_GET_BALANCE_RESP, ROSETTA_REQUEST_NETWORK_STATUS_TEMPLATE, \
    ROSETTA_URL, ROSETTA_REQUEST_BLOCK_TEMPLATE, MOCK_ROSETTA_BLOCK_HASH, MOCK_ROSETTA_BLOCK_HEIGHT
from .util_methods import print_outgoing, print_incoming, print_log


def get_address_balance(sc_address):
    if get_active_proposal().is_null():
        return GetProposalError(
            "No active proposal found. Reference block should be retrieved from Rosetta when a voting proposal is "
            "created").get()

        # Retrieve associated MC addresses balance
    balance = 0

    try:
        mc_address_map = get_mc_address_map(sc_address)
    except Exception as e:
        return GenericError(
            "Can not get mc addresses associated to sc address: " + sc_address + " - Exception: " + str(e)).get()

    if sc_address in mc_address_map:
        mc_addresses = mc_address_map[sc_address]

        if MOCK_ROSETTA:
            # don't go on rosetta, just mock it
            print("-------------MOCK ROSETTA RESPONSE------------")
            return MOCK_ROSETTA_GET_BALANCE_RESP

        bl_height = int(get_active_proposal().block_height)
        bl_hash = str(get_active_proposal().block_hash)

        # Check the MC block has not been reverted. In such a case use the block hash corresponding to that height
        actual_bl_hash = get_mainchain_block_hash(bl_height)
        if actual_bl_hash != bl_hash:
            print_log(
                "MC block hash mismatch. Using hash=" + actual_bl_hash + " (instead of " + bl_hash + ") for "
                "height=" + str(bl_height))
            bl_hash = actual_bl_hash

        # Call Rosetta endpoint /account/balance
        for mc_address in mc_addresses:
            request_body = ROSETTA_REQUEST_NETWORK_STATUS_TEMPLATE
            request_body["account_identifier"] = {"address": mc_address, "metadata": {}}
            request_body["block_identifier"] = {"index": bl_height, "hash": bl_hash}
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


def get_mainchain_tip():
    # Retrieve current MC tip

    if MOCK_ROSETTA:
        return MOCK_ROSETTA_BLOCK_HEIGHT, MOCK_ROSETTA_BLOCK_HASH

    # Call Rosetta endpoint /network/status
    request_body = ROSETTA_REQUEST_NETWORK_STATUS_TEMPLATE
    print_outgoing("Rosetta", "/network/status", request_body)
    response = requests.post(ROSETTA_URL + "network/status", json.dumps(request_body))
    print_incoming("Rosetta", "/network/status", response.json())
    chain_height = int(response.json()["current_block_identifier"]["index"])
    best_block_hash = str(response.json()["current_block_identifier"]["hash"])

    return chain_height, best_block_hash


def get_mainchain_block_hash(height):
    # Retrieve hash of the MC block at the given height

    if MOCK_ROSETTA:
        return MOCK_ROSETTA_BLOCK_HASH

    # Call Rosetta endpoint block
    request_body = ROSETTA_REQUEST_BLOCK_TEMPLATE
    request_body['block_identifier']['index'] = int(height)
    print_outgoing("Rosetta", "/block", request_body)
    response = requests.post(ROSETTA_URL + "block", json.dumps(request_body))
    print_incoming("Rosetta", "/block", response.json())
    block_hash = str(response.json()["block_identifier"]["hash"])

    return block_hash
