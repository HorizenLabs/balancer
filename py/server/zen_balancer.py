import threading
from flask import Flask, request, json

from modules.snapshot_methods import add_ownership_entry, \
    get_mc_address_map, store_proposal_data, get_active_proposal, \
    proposal_dict, get_owner_sc_addr_list, init_active_proposal
from modules.rosetta_methods import get_chain_tip, get_address_balance
from modules.definitions import MOCK_NSC, MOCK_MC_ADDRESS_MAP, MOCK_OWNER_SC_ADDR_LIST, check_mocks
from modules.util_methods import print_incoming, print_outgoing, read_proposal_from_file

# see below for proxy usage
# from werkzeug.middleware.proxy_fix import ProxyFix


def api_server():
    app = Flask(__name__)

    # should we use a proxy
    # app.wsgi_app = ProxyFix(app.wsgi_app)

    @app.route('/api/v1/getOwnerships', methods=['POST'])
    def get_ownerships():
        cmd_input = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getOwnerships", cmd_input)

        if MOCK_NSC:
            ret = MOCK_MC_ADDRESS_MAP
        else:
            sc_address = cmd_input['scAddress']
            try:
                ret = get_mc_address_map(sc_address)
            except Exception as e:
                ret = {
                    "error": {
                        "code": 301,
                        "description": "Could not get ownership for sc address:" + sc_address,
                        "detail": "An exception occurred: " + str(e)
                    }
                }

        print_outgoing("BalancerApiServer", "/api/v1/getOwnerships", ret)

        return json.dumps(ret)

    @app.route('/api/v1/getOwnerScAddresses', methods=['POST'])
    def get_owner_sc_addresses():
        cmd_input = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getOwnerScAddresses", cmd_input)

        if MOCK_NSC:
            ret = MOCK_OWNER_SC_ADDR_LIST
        else:
            try:
                ret = get_owner_sc_addr_list()
            except Exception as e:
                ret = {
                    "error": {
                        "code": 302,
                        "description": "Could not get owner sc addresses",
                        "detail": "An exception occurred: " + str(e)
                    }
                }

        print_outgoing("BalancerApiServer", "/api/v1/getOwnerScAddresses", ret)

        return json.dumps(ret)

    @app.route('/api/v1/getProposals', methods=['POST'])
    def get_proposals():
        cmd_input = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getProposals", cmd_input)
        response = [proposal.to_json() for proposal in proposal_dict.values()]
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
                    "code": 303,
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
                        "code": 304,
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
                    "code": 305,
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

        if MOCK_NSC:
            ret = {
                'result': add_ownership_entry(proposal),
                'ownerships': MOCK_MC_ADDRESS_MAP,
            }
        else:
            ret = {
                "error": {
                    "code": 306,
                    "description": "Could not add ownership",
                    "detail": "Method not supported with real native smart contract. Pls set \'mock_nsc=true\'"
                              " in balancer"
                }
            }
        print_outgoing("BalancerApiServer", "/api/v1/addOwnership", ret)

        return json.dumps(ret)

    # warn if some mock attribute is set
    check_mocks()

    # read proposal from local file and initialize active proposal if any
    prop = read_proposal_from_file()
    if prop is not None:
        init_active_proposal(prop)

    # listen on http port
    # ---------------------
    # app.run(host="0.0.0.0", port=5000)

    # listen on https port
    # ---------------------
    # localhost (cert generated via openssl, see:
    # https://kracekumar.com/post/54437887454/ssl-for-flask-local-development/
    # context = ('/tmp/server.crt', '/tmp/server.key')  # certificate and key files

    # official server (certificates generated there via certbot)
    # $ export FQDN="zendao-tn-1.de.horizenlabs.io"
    # $ sudo certbot certonly  -n --agree-tos --register-unsafely-without-email --standalone -d $FQDN
    context = ('/etc/letsencrypt/archive/zendao-tn-1.de.horizenlabs.io/fullchain1.pem',
               '/etc/letsencrypt/archive/zendao-tn-1.de.horizenlabs.io/privkey1.pem')

    app.run(host="0.0.0.0", port=5000, ssl_context=context)


if __name__ == '__main__':
    # Start http(s) server
    t = threading.Thread(target=api_server, args=())
    t.start()
