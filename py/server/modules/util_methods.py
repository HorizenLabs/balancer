import datetime
import json
import logging
from binascii import unhexlify
from json import JSONDecodeError

from eth_utils import remove_0x_prefix

from .definitions import PROPOSAL_JSON_DATA_PATH, PROPOSAL_JSON_DATA_FILE_NAME

logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s:\n%(message)s'
)


def print_incoming(component_tag, endpoint_tag, content):
    logging.info("<<<< " + component_tag + " " + endpoint_tag + " received:\n" + json.dumps(content, indent=4))


def print_outgoing(component_tag, endpoint_tag, content):
    logging.info(">>>> " + component_tag + " " + endpoint_tag + " sending:\n" + json.dumps(content, indent=4))


def print_log(msg):
    logging.info(msg)


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

    except FileNotFoundError as e:
        logging.warning("Warning: " + str(e))
    except JSONDecodeError as e:
        logging.error("Error: could not decode file [" + file_name + "]: " + str(e))

    return json_object


def write_proposal_to_file(prop):
    file_name = PROPOSAL_JSON_DATA_PATH + PROPOSAL_JSON_DATA_FILE_NAME

    # Serializing json
    json_object = json.dumps(prop.to_json(), indent=4)

    try:
        with open(file_name, 'w') as outfile:
            outfile.write(json_object)
        logging.info(
            "Proposal written to file: " + file_name + "\n" + json_object)
    except FileNotFoundError as e:
        logging.warning("Warning: " + str(e))
    except Exception as e:
        logging.error("Error: " + str(e))


def warn_if_proposal_not_active(prop):
    time_now = datetime.datetime.now(datetime.timezone.utc)
    if prop.fromTime > time_now:
        logging.warning(
            "######################################################################################\n" +
            "Proposal is not started yet!\nTime now: " + str(time_now) +
            ", proposal starts at time: " + str(prop.fromTime) + "\n" +
            "######################################################################################"
        )
    elif prop.toTime < time_now:
        logging.warning(
            "######################################################################################\n" +
            "Proposal is closed!\nTime now: " + str(time_now) +
            ", proposal closed at time: " + str(prop.toTime) + "\n" +
            "######################################################################################"
        )
    else:
        pass
