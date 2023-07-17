import json
import requests

from .snapshot_methods import get_mc_address_map, get_active_proposal
from .definitions import mock_rosetta, MOCK_ROSETTA_GET_BALANCE_RESP, ROSETTA_REQUEST_TEMPLATE, ROSETTA_URL
from .util_methods import print_outgoing, print_incoming


def get_address_balance(sc_address):
    if get_active_proposal().is_null():
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
            request_body["block_identifier"] = {"index": int(get_active_proposal().block_height),
                                                "hash": str(get_active_proposal().block_hash)}
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
