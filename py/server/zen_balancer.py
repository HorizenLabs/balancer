import threading

from flask import Flask, request, json

from modules.balancerError import GetOwnershipError, GetOwnerScAddressesError, \
    CreateProposalError, AddOwnershipError, GenericError
from modules.definitions import MOCK_NSC, MOCK_MC_ADDRESS_MAP, check_mocks, RUNNING_ON_LOCALHOST, LISTENING_ON_HTTP, \
    BALANCER_PORT, USING_WSGI_PROXY
from modules.rosetta_methods import get_mainchain_tip, get_address_balance
from modules.snapshot_methods import add_mock_ownership_entry, \
    get_mc_address_map, store_proposal_data, \
    proposal_dict, get_owner_sc_addr_list, init_active_proposals, get_proposal_snapshot
from modules.util_methods import print_incoming, print_outgoing, read_proposals_from_file


def api_server():
    app = Flask(__name__)

    if USING_WSGI_PROXY:
        # if we use a proxy
        from werkzeug.middleware.proxy_fix import ProxyFix
        app.wsgi_app = ProxyFix(app.wsgi_app)

    @app.route('/api/v1/getOwnerships', methods=['POST'])
    def get_ownerships():
        cmd_input = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/getOwnerships", cmd_input)

        if 'scAddress' not in cmd_input:
            ret = GenericError("Missing sc address in input").get()
        else:
            sc_address = cmd_input['scAddress']
            try:
                ret = get_mc_address_map(sc_address)
            except Exception as e:
                ret = GetOwnershipError("sc address: " + str(sc_address) + " - Exception: " + str(e)).get()

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

        # get snapshot value corresponding to this proposal via snapshot API
        snapshot_resp = get_proposal_snapshot(proposal)

        if 'data' not in snapshot_resp:
            # an error occurred trying to get snapshot value
            response = snapshot_resp
        else:
            snapshot_value = int(snapshot_resp['data']['proposal']['snapshot'])

            # update MC chain tip. This block will be used when retrieving balances from Rosetta
            rosetta_return = get_mainchain_tip()

            if 'error' in rosetta_return:
                response = rosetta_return
            else:
                chain_tip_height = int(rosetta_return["current_block_identifier"]["index"])
                chain_tip_hash = str(rosetta_return["current_block_identifier"]["hash"])
                try:
                    store_proposal_data(proposal, chain_tip_height, chain_tip_hash, snapshot_value)
                except Exception as e:
                    response = CreateProposalError("Proposal data format not expected: " + str(e)).get()
                else:
                    response = {"status": "Ok"}

        print_outgoing("BalancerApiServer", "/api/v1/createProposal", response)
        return json.dumps(response)

    @app.route('/api/v1/getVotingPower', methods=['GET'])
    def get_voting_power():

        cmd_input = request.args
        print_incoming("BalancerApiServer", "/api/v1/getVotingPower", cmd_input)

        if 'addresses' not in cmd_input or 'snapshot' not in cmd_input:
            response = GenericError("Missing sc address or snapshot in getVotingPower input: " + str(cmd_input)).get()
        else:
            # Parse requested address. In GET this is one only address actually
            sc_requested_address = cmd_input["addresses"]

            # Parse requested snapshot. This is the SC reference block, we use it for getting the active proposal
            snapshot = cmd_input["snapshot"]

            # Retrieve balance for the owned MC addresses at the height/hash block specified in the active proposal.
            # This also checks if proposal have been created
            response = get_address_balance(sc_requested_address, snapshot)

        # Answer back with the balance
        print_outgoing("BalancerApiServer", "/api/v1/getVotingPower", response)
        return json.dumps(response)

    # usable only when mocking native smart contract
    @app.route('/api/v1/addOwnership', methods=['POST'])
    def add_ownership():
        cmd_input = json.loads(request.data)

        print_incoming("BalancerApiServer", "/api/v1/addOwnership", cmd_input)

        if MOCK_NSC:
            ret = {
                'result': add_mock_ownership_entry(cmd_input),
                'ownerships': MOCK_MC_ADDRESS_MAP,
            }
        else:
            ret = AddOwnershipError(
                "Method not supported with real native smart contract. Pls set \'MOCK_NSC=true\' in env").get()

        print_outgoing("BalancerApiServer", "/api/v1/addOwnership", ret)

        return json.dumps(ret)

    # warn if some mock attribute is set
    check_mocks()

    # read proposals from local file and initialize active proposals if any
    props = read_proposals_from_file()
    if props is not None:
        init_active_proposals(props)

    if LISTENING_ON_HTTP:
        # listen on http port
        # ---------------------
        app.run(host="0.0.0.0", port=BALANCER_PORT)
    else:
        # listen on https port
        # ---------------------
        if RUNNING_ON_LOCALHOST:
            # localhost (cert generated via openssl, see:
            # https://kracekumar.com/post/54437887454/ssl-for-flask-local-development/
            context = ('/tmp/server.crt', '/tmp/server.key')  # certificate and key files
        else:
            # official server (certificates generated there via certbot)
            # $ export FQDN="zendao-tn-1.de.horizenlabs.io"
            # $ sudo certbot certonly  -n --agree-tos --register-unsafely-without-email --standalone -d $FQDN
            context = ('/etc/letsencrypt/archive/zendao-tn-1.de.horizenlabs.io/fullchain1.pem',
                       '/etc/letsencrypt/archive/zendao-tn-1.de.horizenlabs.io/privkey1.pem')

        app.run(host="0.0.0.0", port=BALANCER_PORT, ssl_context=context)


if __name__ == '__main__':
    # Start http(s) server
    t = threading.Thread(target=api_server, args=())
    t.start()
