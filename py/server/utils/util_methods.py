import json
from binascii import unhexlify
from eth_utils import remove_0x_prefix


def print_incoming(component_tag, endpoint_tag, content):
    print("<<<< " + component_tag + " " + endpoint_tag + " received:")
    print(json.dumps(content, indent=4))
    print()


def print_outgoing(component_tag, endpoint_tag, content):
    print(">>>> " + component_tag + " " + endpoint_tag + " sending:")
    print(json.dumps(content, indent=4))
    print()


def check_sc_address(sc_address):
    sc_address = remove_0x_prefix(sc_address)

    if len(sc_address) != 40:
        raise Exception("Invalid sc address length: {}, expected 40".format(len(sc_address)))

    # this throws an exception if not a hex string
    hex_str_to_bytes(sc_address)


def hex_str_to_bytes(hex_str):
    return unhexlify(hex_str.encode('ascii'))
