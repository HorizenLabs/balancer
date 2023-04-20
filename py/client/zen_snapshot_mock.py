import requests
import sys
import json



LOCAL_HTTP_SERVER_URL = "http://localhost:5000/"
REMOTE_HTTP_SERVER_URL = "http://testnet-zendao-1.de.horizenlabs.io:5000/"

#HTTP_SERVER_URL = LOCAL_HTTP_SERVER_URL
HTTP_SERVER_URL = REMOTE_HTTP_SERVER_URL


CREATE_PROPOSAL_MOCK = {
    "Body": "Start: 18 Apr 23 13:40 UTC, End: 18 Apr 23 13:45 UTC, Author: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",
    "ProposalEvent": "proposal/created",
    "ProposalExpire": 0,
    "ProposalID": "proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133cabc6",
    "ProposalSpace": "victorbibiano.eth"
}
GET_VOTING_POWER_MOCK = {
    'options': {
        'url': 'http://testnet-zendao-1.de.horizenlabs.io:5000/api/v1/getVotingPower',
        'type': 'api-post'
    },
    'network': '80001',
    'snapshot': 34522768,
    'addresses': [
        '0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959'
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

def get_voting_power():
    print(f"Calling get voting power with data {GET_VOTING_POWER_MOCK}")
    response = requests.post(HTTP_SERVER_URL+"api/v1/getVotingPower", json.dumps(GET_VOTING_POWER_MOCK))
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
        get_voting_power()
    elif (command == "get_proposals"):
        get_proposals()
    elif (command == "add_ownership"):
        if len(args) > 2:
            add_ownership(args[1], args[2])
        else:
            add_ownership(None, None)
    else:
        print("Unsupported command!")

main()
