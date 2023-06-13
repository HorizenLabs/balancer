import requests
import sys
import json



LOCAL_HTTP_SERVER_URL = "http://localhost:5000/"
REMOTE_HTTP_SERVER_URL = "http://zendao-tn-1.de.horizenlabs.io:5000/"

HTTP_SERVER_URL = LOCAL_HTTP_SERVER_URL
#HTTP_SERVER_URL = REMOTE_HTTP_SERVER_URL


CREATE_PROPOSAL_MOCK = {
    "Body": "Start: 18 Apr 23 13:40 UTC, End: 18 Apr 23 13:45 UTC, Author: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
    "ProposalEvent": "proposal/created",
    "ProposalExpire": 0,
    "ProposalID": "proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133cabc5",
    "ProposalSpace": "victorbibiano.eth"
}
GET_VOTING_POWER_MOCK = {
    'options': {
        'url': HTTP_SERVER_URL+'api/v1/getVotingPower',
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
    print(f"Calling new proposal with data {CREATE_PROPOSAL_MOCK}")
    post_data = CREATE_PROPOSAL_MOCK
    response = requests.post(HTTP_SERVER_URL+"api/v1/createProposal", json.dumps(post_data))
    print(json.dumps(response.json(), indent=4))

def get_voting_power2():
    print(f"Calling get voting power with data {GET_VOTING_POWER_MOCK}")
    response = requests.post(HTTP_SERVER_URL+"api/v1/getVotingPower", json.dumps(GET_VOTING_POWER_MOCK))
    print(json.dumps(response.json(), indent=4))

def get_voting_power1():
    print(f"Calling get voting power with data {GET_VOTING_POWER_MOCK}")
    response = requests.get(HTTP_SERVER_URL+"api/v1/getVotingPower", GET_VOTING_POWER_MOCK)
    print(json.dumps(response.json(), indent=4))

def get_voting_power(address):
    if address is not None:
        cmd = {
            'options': {
                'url': HTTP_SERVER_URL+'/api/v1/getVotingPower',
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
    response = requests.get(HTTP_SERVER_URL + "api/v1/getVotingPower", cmd)
    print(json.dumps(response.json(), indent=4))

def add_ownership(owner, address):
    if owner== None or address== None:
        data = ADD_OWNERSHIP_MOCK
    else:
        data = {
                'owner': owner,
                'address': address
        }
    print(f"Calling add ownership with data {data}")
    response = requests.post(HTTP_SERVER_URL + "api/v1/addOwnership", json.dumps(data))
    print(json.dumps(response.json(), indent=4))

def get_proposals():
    print("Calling get proposals")
    response = requests.post(HTTP_SERVER_URL + "api/v1/getProposals", json.dumps({}))
    print(json.dumps(response.json(), indent=4))

def get_ownerships(sc_address=None):
    print("Calling get ownerships")
    response = requests.post(HTTP_SERVER_URL + "api/v1/getOwnerships", json.dumps({'scAddress' :sc_address}))
    print(json.dumps(response.json(), indent=4))

def main():
    args = sys.argv[1:]

    if len(args) == 0:
        print("command not specified!")
        return

    # Command to execute
    command = args[0]
    if (command == "new_proposal"):
        new_proposal()
    elif (command == "get_voting_power"):
        if len(args) > 1:
            get_voting_power(args[1])
        else:
            get_voting_power(None)

    elif (command == "get_proposals"):
        get_proposals()
    elif (command == "get_ownerships"):
        if len(args) > 1:
            get_ownerships(args[1])
        else:
            get_ownerships()
    elif (command == "add_ownership"):
        if len(args) > 2:
            add_ownership(args[1], args[2])
        else:
            add_ownership(None, None)
    else:
        print("Unsupported command!")

main()
