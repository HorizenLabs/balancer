from random import randrange

import requests
import sys
import json

import urllib3

# squelch warning due to a cert missing an optional field
urllib3.disable_warnings(urllib3.exceptions.SubjectAltNameWarning)

# LOCAL_HTTP_SERVER_URL = "http://localhost:5000/"
LOCAL_HTTP_SERVER_URL = "https://localhost:5000/"
REMOTE_HTTP_SERVER_URL = "https://zendao-tn-1.de.horizenlabs.io:5000/"

#HTTP_SERVER_URL = LOCAL_HTTP_SERVER_URL
HTTP_SERVER_URL = REMOTE_HTTP_SERVER_URL

# if the server is on https we should use a cert
# ---
# for local dev and self-signed cert we use the same cert as server
#VERIFY_PARAM = '/tmp/server.crt'
# if https we get a InsecureRequestWarning
# VERIFY_PARAM=False
# ---
# valid cert
VERIFY_PARAM=True

'''
# Old format
CREATE_PROPOSAL_MOCK = {
    "Body": "Start: 18 Apr 23 13:40 UTC, End: 18 Apr 23 13:45 UTC, Author: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
    "ProposalEvent": "proposal/created",
    "ProposalExpire": 0,
    "ProposalID": "proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133cabc5",
    "ProposalSpace": "victorbibiano.eth"
}
'''
CREATE_PROPOSAL_MOCK = {
    "Body": "HAL new format of notification\nProposal Created\nStarts on: 4 Aug 23 12:27 UTC\nEnds on: 31 Aug 23 13:27 UTC\nAuthor: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
    "ProposalID": "proposal/0x33142d2c78802c3e56194fe7d4e6f49b760b7dea26d379d84e417e6c0ad09009",
    "ProposalEvent": "proposal/created",
    "ProposalSpace": "victorbibiano.eth",
    "ProposalExpire": 0
}

GET_VOTING_POWER_MOCK = {
    'options': {
        'url': HTTP_SERVER_URL + 'api/v1/getVotingPower',
        'type': 'api-post'
    },
    'network': '80001',
    'snapshot': 34522768,
    'addresses': [
        '0xf43F35c1A3E36b5Fb554f5E830179cc460c35858'
    ]
}

ADD_OWNERSHIP_MOCK = {
    'owner': '0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959',
    'address': 'ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt'
}


def new_proposal():
    val = hex(randrange(255))[2:]
    CREATE_PROPOSAL_MOCK[
        'ProposalID'] = "proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133c" + str(val)
    print(f"Calling new proposal with data {CREATE_PROPOSAL_MOCK}")
    post_data = CREATE_PROPOSAL_MOCK
    response = requests.post(HTTP_SERVER_URL + "api/v1/createProposal", json.dumps(post_data), verify=VERIFY_PARAM)
    print(json.dumps(response.json(), indent=4))


def get_voting_power2():
    print(f"Calling get voting power with data {GET_VOTING_POWER_MOCK}")
    response = requests.post(HTTP_SERVER_URL + "api/v1/getVotingPower", json.dumps(GET_VOTING_POWER_MOCK),
                             verify=VERIFY_PARAM)
    print(json.dumps(response.json(), indent=4))


def get_voting_power1():
    print(f"Calling get voting power with data {GET_VOTING_POWER_MOCK}")
    response = requests.get(HTTP_SERVER_URL + "api/v1/getVotingPower", GET_VOTING_POWER_MOCK, verify=VERIFY_PARAM)
    print(json.dumps(response.json(), indent=4))


def get_voting_power(address):
    if address is not None:
        cmd = {
            'options': {
                'url': HTTP_SERVER_URL + '/api/v1/getVotingPower',
                'type': 'api-post'
            },
            'network': '80001',
            'snapshot': 34522768,
            'addresses': [
                address
            ]
        }
    else:
        cmd = GET_VOTING_POWER_MOCK

    print(f"Calling get voting power with data {cmd}")
    response = requests.get(HTTP_SERVER_URL + "api/v1/getVotingPower", cmd, verify=VERIFY_PARAM)
    print(json.dumps(response.json(), indent=4))


def add_ownership(owner, address):
    if owner is None or address is None:
        data = ADD_OWNERSHIP_MOCK
    else:
        data = {
            'owner': owner,
            'address': address
        }
    print(f"Calling add ownership with data {data}")
    response = requests.post(HTTP_SERVER_URL + "api/v1/addOwnership", json.dumps(data), verify=VERIFY_PARAM)
    print(json.dumps(response.json(), indent=4))


def get_proposals():
    print("Calling get proposals")
    response = requests.post(HTTP_SERVER_URL + "api/v1/getProposals", json.dumps({}), verify=VERIFY_PARAM)
    print(json.dumps(response.json(), indent=4))


def get_ownerships(sc_address=None):
    print("Calling get ownerships")
    response = requests.post(HTTP_SERVER_URL + "api/v1/getOwnerships", json.dumps({'scAddress': sc_address}),
                             verify=VERIFY_PARAM)
    print(json.dumps(response.json(), indent=4))


def get_owner_sc_addresses():
    print("Calling get owner sc addresses")
    response = requests.post(HTTP_SERVER_URL + "api/v1/getOwnerScAddresses", json.dumps({}), verify=VERIFY_PARAM)
    print(json.dumps(response.json(), indent=4))


def main():
    args = sys.argv[1:]

    if len(args) == 0:
        print("command not specified!")
        return

    # Command to execute
    command = args[0]
    if command == "new_proposal":
        new_proposal()
    elif command == "get_voting_power":
        if len(args) > 1:
            get_voting_power(args[1])
        else:
            get_voting_power(None)

    elif command == "get_proposals":
        get_proposals()
    elif command == "get_ownerships":
        if len(args) > 1:
            get_ownerships(args[1])
        else:
            get_ownerships()
    elif command == "add_ownership":
        if len(args) > 2:
            add_ownership(args[1], args[2])
        else:
            add_ownership(None, None)
    elif command == "get_owner_sc_addresses":
        get_owner_sc_addresses()
    else:
        print("Unsupported command!")


main()
