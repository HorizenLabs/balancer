import json
from binascii import unhexlify
from json import JSONDecodeError

from eth_utils import remove_0x_prefix

from .definitions import PROPOSAL_JSON_DATA_PATH, PROPOSAL_JSON_DATA_FILE_NAME


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


def read_proposal_from_file():
    file_name = PROPOSAL_JSON_DATA_PATH + PROPOSAL_JSON_DATA_FILE_NAME

    json_object = None
    try:
        with open(file_name, 'r') as openfile:
            json_object = json.load(openfile)
            # print(json_object)

    except FileNotFoundError as e:
        print("Warning: " + str(e))
    except JSONDecodeError as e:
        print("Error: could not decode file [" + file_name + "]: " + str(e))

    return json_object


def write_proposal_to_file(prop):
    file_name = PROPOSAL_JSON_DATA_PATH + PROPOSAL_JSON_DATA_FILE_NAME

    # Serializing json
    json_object = json.dumps(prop.to_json(), indent=4)
    # print(json_object)

    try:
        with open(file_name, 'w') as outfile:
            outfile.write(json_object)
    except FileNotFoundError as e:
        print("Warning: " + str(e))
    except Exception as e:
        print("Error: " + str(e))
