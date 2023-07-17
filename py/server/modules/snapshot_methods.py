import datetime
import re
import string
import base58
from .definitions import MOCK_MC_ADDRESS_MAP, mock_nsc
from .nsc_methods import get_nsc_ownerships
from .proposal import VotingProposal

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


# used only when mocking nsc
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


def get_mc_address_map(sc_address=None):
    if mock_nsc:
        return MOCK_MC_ADDRESS_MAP
    else:
        return get_nsc_ownerships(sc_address)


def get_active_proposal():
    return active_proposal


proposal_dict = {}
