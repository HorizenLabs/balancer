import json

import requests

from .balancerError import GetProposalError, GenericError, RosettaError
from .definitions import MOCK_ROSETTA, MOCK_ROSETTA_GET_BALANCE_RESP, ROSETTA_REQUEST_NETWORK_STATUS_TEMPLATE, \
    ROSETTA_URL, ROSETTA_REQUEST_BLOCK_TEMPLATE, MOCK_ROSETTA_BLOCK_HASH, MOCK_ROSETTA_NETWORK_STATUS_RETURN
from .snapshot_methods import get_mc_address_map, get_active_proposal
from .util_methods import print_outgoing, print_incoming, print_log, warn_if_proposal_not_active


def get_address_balance(sc_address, snapshot):
    prop = get_active_proposal(snapshot)

    if prop.is_null():
        return GetProposalError(
            "No active proposal found for snapshot " + str(snapshot)).get()

    # Retrieve associated MC addresses balance
    balance = 0

    warn_if_proposal_not_active(prop)

    print_log(
        "getting voting power for active proposal:\n" + json.dumps(prop.to_json(), indent=4))

    try:
        mc_address_map = get_mc_address_map(sc_address)
    except Exception as e:
        return GenericError(
            "Can not get mc addresses associated to sc address: " + str(sc_address) + " - Exception: " + str(e)).get()

    if sc_address in mc_address_map:
        mc_addresses = mc_address_map[sc_address]

        if MOCK_ROSETTA:
            # don't go on rosetta, just mock it
            print("-------------MOCK ROSETTA RESPONSE------------")
            return MOCK_ROSETTA_GET_BALANCE_RESP

        bl_height = int(prop.mc_block_height)

        # Call Rosetta endpoint /account/balance In the query we use just the block height (omitting the hash) in the
        # 'block_identifier', this is for handling also the case of a chain reorg which reverts the block whose hash
        # was red when the proposal has been received
        for mc_address in mc_addresses:
            request_body = ROSETTA_REQUEST_NETWORK_STATUS_TEMPLATE
            request_body["account_identifier"] = {"address": mc_address, "metadata": {}}
            request_body["block_identifier"] = {"index": bl_height}
            print_outgoing("Rosetta", "/account/balance", request_body)

            try:
                response = requests.post(ROSETTA_URL + "account/balance", json.dumps(request_body))
            except Exception as e:
                return RosettaError("An exception occurred trying to get balance from rosetta: " + str(e)).get()

            print_incoming("Rosetta", "/account/balance", response.json())

            if 'balances' in response.json():
                amount = int(response.json()["balances"][0]["value"])
                print("address={} / amount={}".format(mc_address, amount))
                balance += amount
            else:
                # this may happen if rosetta is reindexing
                err_msg = str(response.json()['message'])
                if 'details' in response.json():
                    err_details = str(response.json()['details'])
                else:
                    err_details = "N.A."

                return RosettaError("Error occurred trying to get balance from rosetta. " +
                                    "message: " + err_msg + ", details: " + err_details).get()

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
        return MOCK_ROSETTA_NETWORK_STATUS_RETURN

    # Call Rosetta endpoint /network/status
    request_body = ROSETTA_REQUEST_NETWORK_STATUS_TEMPLATE
    print_outgoing("Rosetta", "/network/status", request_body)

    try:
        response = requests.post(ROSETTA_URL + "network/status", json.dumps(request_body))
        print_incoming("Rosetta", "/network/status", response.json())
        if 'message' in response.json():
            return RosettaError(
                "An exception occurred trying to get mainchain tip from rosetta: " + str(response.json())).get()
    except Exception as e:
        return RosettaError("An exception occurred trying to get mainchain tip from rosetta: " + str(e)).get()

    return response.json()


# currently not used
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
    block_hash = str(response.json()['block']["block_identifier"]["hash"])

    return block_hash
