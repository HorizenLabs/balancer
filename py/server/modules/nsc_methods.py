import json
import requests
from eth_abi import decode
from eth_utils import remove_0x_prefix, to_checksum_address, function_signature_to_4byte_selector, encode_hex
from .definitions import NSC_URL, ETH_CALL_FROM_ADDRESS
from .util_methods import print_outgoing, print_incoming, check_sc_address, hex_str_to_bytes, print_log


# not used; useful for calling http api endpoint
def get_nsc_ownerships1(sc_address=None):
    # Call eon endpoint /transaction/getKeysOwnership which call NativeSmartContract interface
    request_body = {"scAddressOpt": sc_address}
    print_outgoing("NSC", "/transaction/getKeysOwnership", request_body)
    response = requests.post(NSC_URL + "transaction/getKeysOwnership", json.dumps(request_body),
                             auth=('user', 'Horizen'))
    print_incoming("NSC", "/transaction/getKeysOwnership", response.json())
    return response.json()['result']['keysOwnership']


# invokes eth_call RPC
def get_nsc_ownerships(sc_address=None):
    if sc_address is None:
        method = 'getAllKeyOwnerships()'
        abi_str = encode_hex(function_signature_to_4byte_selector(method))
    else:
        sc_address = remove_0x_prefix(sc_address)
        check_sc_address(sc_address)
        method = 'getKeyOwnerships(address)'
        abi_method_str = encode_hex(function_signature_to_4byte_selector(method))
        abi_str = abi_method_str + "000000000000000000000000" + sc_address

    request_body = {
        "jsonrpc": "2.0",
        "method": "eth_call",
        "params": [
            {
                "from": ETH_CALL_FROM_ADDRESS,
                "to": "0x0000000000000000000088888888888888888888",
                "value": "0x00",
                "gasLimit": "0x21000",
                "maxPriorityFeePerGas": "0x900000000",
                "maxFeePerGas": "0x900000000",
                "data": abi_str
            }
        ],
        "id": 1
    }

    abi_return_value = handle_response_from_req(request_body, 'getKeysOwnership')
    return get_key_ownership_from_abi(abi_return_value)


# invokes eth_call RPC
def get_nsc_owner_sc_addresses():
    method = 'getKeyOwnerScAddresses()'
    abi_str = encode_hex(function_signature_to_4byte_selector(method))

    request_body = {
        "jsonrpc": "2.0",
        "method": "eth_call",
        "params": [
            {
                "from": ETH_CALL_FROM_ADDRESS,
                "to": "0x0000000000000000000088888888888888888888",
                "value": "0x00",
                "gasLimit": "0x21000",
                "maxPriorityFeePerGas": "0x900000000",
                "maxFeePerGas": "0x900000000",
                "data": abi_str
            }
        ],
        "id": 1
    }

    abi_return_value = handle_response_from_req(request_body, 'getKeyOwnerScAddresses')
    return get_owner_sc_addr_from_abi(abi_return_value)

def handle_response_from_req(request_body, op_tag):
    print_outgoing("NSC", "/ethv1/eth_call (" + op_tag + ")", request_body)
    response = requests.post(NSC_URL + "ethv1", json.dumps(request_body), auth=('user', 'Horizen'))
    print_incoming("NSC", "/ethv1/eth_call (" + op_tag + ")", response.json())
    abi_return_value = remove_0x_prefix(response.json()['result'])
    return abi_return_value

def get_key_ownership_from_abi(abi_return_value):
    # the location of the data part of the first (the only one in this case) parameter (dynamic type), measured in bytes
    # from the start of the return data block. In this case 32 (0x20)
    start_data_offset = decode(['uint32'], hex_str_to_bytes(abi_return_value[0:64]))[0] * 2

    end_offset = start_data_offset + 64  # read 32 bytes
    list_size = decode(['uint32'], hex_str_to_bytes(abi_return_value[start_data_offset:end_offset]))[0]

    sc_associations_dict = {}
    for i in range(list_size):
        start_offset = end_offset
        end_offset = start_offset + 192  # read (32 + 32 + 32) bytes
        (address_pref, mca3, mca32) = decode(['address', 'bytes3', 'bytes32'],
                                             hex_str_to_bytes(abi_return_value[start_offset:end_offset]))
        sc_address_checksum_fmt = to_checksum_address(address_pref)
        if sc_associations_dict.get(sc_address_checksum_fmt) is not None:
            mc_addr_list = sc_associations_dict.get(sc_address_checksum_fmt)
        else:
            sc_associations_dict[sc_address_checksum_fmt] = []
            mc_addr_list = []
        mc_addr = (mca3 + mca32).decode('utf-8')
        mc_addr_list.append(mc_addr)
        print_log("sc_addr=" + sc_address_checksum_fmt + " / mc_addr=" + mc_addr)
        sc_associations_dict[sc_address_checksum_fmt] = mc_addr_list

    # print_log(json.dumps(sc_associations_dict, indent=4))
    return sc_associations_dict


def get_owner_sc_addr_from_abi(abi_return_value):
    # the location of the data part of the first (the only one in this case) parameter (dynamic type), measured in bytes
    # from the start of the return data block. In this case 32 (0x20)
    start_data_offset = decode(['uint32'], hex_str_to_bytes(abi_return_value[0:64]))[0] * 2

    end_offset = start_data_offset + 64  # read 32 bytes
    list_size = decode(['uint32'], hex_str_to_bytes(abi_return_value[start_data_offset:end_offset]))[0]

    sc_address_list = []
    for i in range(list_size):
        start_offset = end_offset
        end_offset = start_offset + 64  # read (32) bytes
        (address_pref) = decode(['address'], hex_str_to_bytes(abi_return_value[start_offset:end_offset]))
        sc_address_checksum_fmt = to_checksum_address(address_pref[0])
        print_log("sc addr=" + sc_address_checksum_fmt)

        sc_address_list.append(sc_address_checksum_fmt)

    #print_log(json.dumps(sc_address_list))
    return sc_address_list


proposal_dict = {}
