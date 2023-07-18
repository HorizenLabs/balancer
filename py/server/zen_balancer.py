import threading
from flask import Flask, request, json

from modules.snapshot_methods import add_ownership_entry, \
    get_mc_address_map, store_proposal_data, get_active_proposal, \
    proposal_dict
from modules.rosetta_methods import get_chain_tip, get_address_balance
from modules.definitions import mock_nsc, MOCK_MC_ADDRESS_MAP
from modules.util_methods import print_incoming, print_outgoing


from werkzeug.middleware.proxy_fix import ProxyFix

def api_server():
    app = Flask(__name__)
    # should we use a proxy
    #app.wsgi_app = ProxyFix(app.wsgi_app)

    @app.route('/api/v1/getOwnerships', methods=['POST'])
    def get_ownerships():
        proposal = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getOwnerships", proposal)

        if mock_nsc:
            ret = MOCK_MC_ADDRESS_MAP
        else:
            sc_address = proposal['scAddress']
            try:
                ret = get_mc_address_map(sc_address)
            except Exception as e:
                ret = {
                    "error": {
                        "code": 108,
                        "description": "Could not get ownership for sc address:" + sc_address,
                        "detail": "An exception occurred: " + str(e)
                    }
                }

        print_outgoing("BalancerApiServer", "/api/v1/getOwnerships", ret)

        return json.dumps(ret)

    @app.route('/api/v1/getProposals', methods=['POST'])
    def get_proposals():
        proposal = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getProposals", proposal)
        response = [prop.to_json() for prop in proposal_dict.values()]
        print_outgoing("BalancerApiServer", "/api/v1/getProposals", response)

        return json.dumps(response)

    @app.route('/api/v1/createProposal', methods=['POST'])
    def create_proposal():

        proposal = json.loads(request.data)
        print_incoming("BalancerApiServer", "/api/v1/createProposal", proposal)
        try:
            # update MC chain tip. This block will be used when retrieving balances from Rosetta
            (chain_tip_height, chain_tip_hash) = get_chain_tip()
        except Exception as e:
            response = {
                "error": {
                    "code": 105,
                    "description": "Can not create proposal",
                    "detail": "can not determine main chain best block: " + str(e)
                }
            }
        else:
            try:
                store_proposal_data(proposal, chain_tip_height, chain_tip_hash)
            except Exception as e:
                response = {
                    "error": {
                        "code": 106,
                        "description": "Can not create proposal",
                        "detail": "proposal data format not expected: " + str(e)
                    }
                }
            else:
                response = {"status": "Ok"}

            print_outgoing("BalancerApiServer", "/api/v1/createProposal", response)

        return json.dumps(response)

    @app.route('/api/v1/getVotingPower', methods=['GET'])
    def get_voting_power():

        content = request.args
        print_incoming("BalancerApiServer", "/api/v1/getVotingPower", content)

        if get_active_proposal().is_null():
            err = {
                "error": {
                    "code": 107,
                    "description": "No proposal have been received at this point",
                    "detail": "Proposal should be received before getting voting power"
                }
            }
            print_outgoing("BalancerApiServer", "/api/v1/getVotingPower", err)
            return err

        print(
            "getting voting power for active proposal: " + json.dumps(get_active_proposal().to_json(), indent=4))

        # Parse requested address. In GET this is one address actually
        requested_address = content["addresses"]

        # Retrieve balance for the owned MC addresses
        response = get_address_balance(requested_address)

        # Answer back with the balance
        print_outgoing("BalancerApiServer", "/api/v1/getVotingPower", response)
        return json.dumps(response)

    # usable only when mocking native smart contract
    @app.route('/api/v1/addOwnership', methods=['POST'])
    def add_ownership():
        proposal = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/addOwnership", proposal)

        if mock_nsc:
            ret = {
                'result': add_ownership_entry(proposal),
                'ownerships': MOCK_MC_ADDRESS_MAP,
            }
        else:
            ret = {
                "error": {
                    "code": 109,
                    "description": "Could not add ownership",
                    "detail": "Method not supported with real native smart contract. Pls set \'mock_nsc=true\'"
                              " in balancer"
                }
            }
        print_outgoing("BalancerApiServer", "/api/v1/addOwnership", ret)

        return json.dumps(ret)

    # listen on http port
    app.run(host="0.0.0.0", port=5000)

    # listen on https port
    #context = ('/tmp/server.crt', '/tmp/server.key')  # certificate and key files
    #app.run(host="0.0.0.0", port=5000, ssl_context=context)


if __name__ == '__main__':
    # Start http server
    t = threading.Thread(target=api_server, args=())
    t.start()
