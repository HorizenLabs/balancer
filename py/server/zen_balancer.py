import threading
from flask import Flask, request, json

from modules.snapshot_methods import add_mock_ownership_entry, \
    get_mc_address_map, store_proposal_data, get_active_proposal, \
    proposal_dict, get_owner_sc_addr_list, init_active_proposal
from modules.rosetta_methods import get_mainchain_tip, get_address_balance
from modules.definitions import MOCK_NSC, MOCK_MC_ADDRESS_MAP, check_mocks
from modules.util_methods import print_incoming, print_outgoing, read_proposal_from_file, print_log


# see below for proxy usage
# from werkzeug.middleware.proxy_fix import ProxyFix

from modules.balancerError import GetOwnershipError, GetOwnerScAddressesError, \
    CreateProposalError, AddOwnershipError


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
                ret = GetOwnershipError("sc address: " + sc_address + " - Exception: " + str(e)).get()

        print_outgoing("BalancerApiServer", "/api/v1/getOwnerships", ret)

        return json.dumps(ret)

    @app.route('/api/v1/getOwnerScAddresses', methods=['POST'])
    def get_owner_sc_addresses():
        cmd_input = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getOwnerScAddresses", cmd_input)

        try:
            ret = get_owner_sc_addr_list()
        except Exception as e:
            ret = GetOwnerScAddressesError("An exception occurred: " + str(e)).get()

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
            (chain_tip_height, chain_tip_hash) = get_mainchain_tip()
        except Exception as e:
            response = CreateProposalError("Can not determine main chain best block: " + str(e)).get()
        else:
            try:
                store_proposal_data(proposal, chain_tip_height, chain_tip_hash)
            except Exception as e:
                response = CreateProposalError("Proposal data format not expected: " + str(e)).get()
            else:
                response = {"status": "Ok"}

        print_outgoing("BalancerApiServer", "/api/v1/createProposal", response)
        return json.dumps(response)

    @app.route('/api/v1/getVotingPower', methods=['GET'])
    def get_voting_power():

        content = request.args
        print_incoming("BalancerApiServer", "/api/v1/getVotingPower", content)

        print_log(
            "getting voting power for active proposal:\n" + json.dumps(get_active_proposal().to_json(), indent=4))

        # Parse requested address. In GET this is one address actually
        requested_address = content["addresses"]

        # Retrieve balance for the owned MC addresses at the height/hash block specified in the active proposal.
        # This also checks if proposal have been created
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
                'result': add_mock_ownership_entry(proposal),
                'ownerships': MOCK_MC_ADDRESS_MAP,
            }
        else:
            ret = AddOwnershipError(
                "Method not supported with real native smart contract. Pls set \'MOCK_NSC=true\' in env").get()

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
