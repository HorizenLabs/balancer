import json
import logging
import re
from threading import Lock

import base58
from eth_utils import add_0x_prefix

from .balancerError import AddOwnershipError
from .definitions import MOCK_MC_ADDRESS_MAP, MOCK_NSC
from .nsc_methods import get_nsc_ownerships, get_nsc_owner_sc_addresses
from .proposal import VotingProposal
from .util_methods import write_proposal_to_file, print_log, warn_if_proposal_not_active, check_sc_address
from dateutil import parser

active_proposal = VotingProposal(in_id=None)
mutex = Lock()


def extract_body_attributes(body_string):
    from_time = None
    to_time = None
    auth_string = "NA"

    # "Body": "Starts on: 28 Jul 23 13:00 UTC\nEnds on: 31 Jul 23 13:00 UTC\nAuthor: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
    s1 = re.split('\n', body_string)

    START_TAG = 'Starts on: '
    END_TAG = 'Ends on: '
    AUTHOR_TAG = 'Author: '

    for t in s1:
        if t.startswith(START_TAG):
            from_string = re.split(START_TAG, t)[1]
            from_time = parser.parse(from_string)
        elif t.startswith(END_TAG):
            to_string = re.split(END_TAG, t)[1]
            to_time = parser.parse(to_string)
        elif t.startswith(AUTHOR_TAG):
            auth_string = re.split(AUTHOR_TAG, t)[1]
        else:
            # not handled
            print_log("Tag not currently handled in proposal body: {}".format(t))

    if from_time is None or to_time is None:
        raise Exception("Could not get valid time window borders from proposal msg")

    return from_time, to_time, auth_string


def store_proposal_data(proposal_json, chain_tip_height, chain_tip_hash):
    global active_proposal

    with mutex:
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
        write_proposal_to_file(active_proposal)


def init_active_proposal(deserialized_proposal_dict):
    global active_proposal
    with mutex:
        proposal = deserialized_proposal_dict['Proposal']

        prop_id = proposal['ID']
        from_time = proposal['from']
        to_time = proposal['to']
        author = proposal['Author']
        chain_tip_height = proposal['block_height']
        chain_tip_hash = proposal['block_hash']

        prop = VotingProposal(
            in_id=prop_id,
            bl_height=chain_tip_height,
            bl_hash=chain_tip_hash,
            from_time=parser.parse(from_time),
            to_time=parser.parse(to_time),
            author=author)

        proposal_dict[prop_id] = prop

        logging.info("Setting active voting proposal:\n" + json.dumps(prop.to_json(), indent=4))
        active_proposal = prop
        warn_if_proposal_not_active(prop)


# used only when mocking nsc
def add_mock_ownership_entry(data_json):
    try:
        new_owner = data_json['owner']
        new_addr = data_json['address']
        base58.b58decode_check(new_addr)

    except KeyError as e:
        response = AddOwnershipError("Invalid json request data, could not find field: " + str(e)).get()
    except ValueError as e:
        response = AddOwnershipError("Address not valid: " + str(e)).get()
    else:
        try:
            check_sc_address(new_owner)
        except Exception as e:
            response = AddOwnershipError("Invalid sc address string: " + new_owner + ", exception: " + str(e)).get()
        else:
            if new_owner in MOCK_MC_ADDRESS_MAP.keys():
                # this is actually a reference
                addresses = MOCK_MC_ADDRESS_MAP[data_json['owner']]
                if data_json['address'] not in addresses:
                    addresses.append(data_json['address'])
                    response = {"status": "Ok"}
                else:
                    response = AddOwnershipError("Ownership already set").get()
            else:
                MOCK_MC_ADDRESS_MAP[new_owner] = [new_addr]
                response = {"status": "Ok"}

    return response


def get_mc_address_map(sc_address):
    check_sc_address(sc_address)
    if MOCK_NSC:
        # build a mocked map for this address
        key = add_0x_prefix(sc_address)
        value = MOCK_MC_ADDRESS_MAP.get(key)
        if value is None:
            return {}
        else:
            return {key: value}
    else:
        return get_nsc_ownerships(sc_address)


def get_owner_sc_addr_list():
    if MOCK_NSC:
        return list(MOCK_MC_ADDRESS_MAP.keys())
    else:
        return get_nsc_owner_sc_addresses()


def get_active_proposal():
    return active_proposal


proposal_dict = {}
