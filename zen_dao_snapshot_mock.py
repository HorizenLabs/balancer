import requests
import sys
import json

HTTP_SERVER_URL = "http://localhost:5000/"
CREATE_PROPOSAL_MOCK = "Proposal mock"
GET_VOTING_POWER_MOCK = {
        "options": {
          "url": "https://dweet.io:443/dweet/quietly/for/victestapirose001",
          "type": "api-post"
        },
        "network": 80001,
        "snapshot": 34482521,
        "addresses": [
          "0x72661045bA9483EDD3feDe4A73688605b51d40c0"
        ]
      }

def new_proposal():
    print(f"Calling new proposal with data {CREATE_PROPOSAL_MOCK}")
    post_data = {"message": CREATE_PROPOSAL_MOCK}
    response = requests.post(HTTP_SERVER_URL+"api/v1/createProposal", json.dumps(post_data))
    print(response.json())

def get_voting_power():
    print(f"Calling get voting power with data {GET_VOTING_POWER_MOCK}")
    response = requests.post(HTTP_SERVER_URL+"api/v1/getVotingPower", json.dumps(GET_VOTING_POWER_MOCK))
    print(response.json())

def main():
    args = sys.argv[1:]

    # Command to execute
    command = args[0]
    if (command == "new_proposal"):
        new_proposal()
    elif(command == "get_voting_power"):
        get_voting_power()
    else:
        print("Unsupported command!")

main()