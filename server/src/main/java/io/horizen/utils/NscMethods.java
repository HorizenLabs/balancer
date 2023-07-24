package io.horizen.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.horizen.helpers.Definitions;
import io.horizen.helpers.Helper;
import io.horizen.helpers.MyGsonManager;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Bytes3;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class NscMethods {
    public static Map<String, List<String>> getNscOwnerships(String scAddress) throws Exception {
        String method;
        String abiString;
        if (scAddress == null) {
            method = "getAllKeyOwnerships()";
            // Generate the 4-byte selector
            byte[] selector = Arrays.copyOf(Hash.sha3(method.getBytes()),4);
            abiString = "0x" + Numeric.toHexStringNoPrefix(selector);
        } else {
            // Remove the "0x" prefix
            if (scAddress.startsWith("0x"))
                scAddress = scAddress.substring(2);

            if (scAddress.length() != 40 || !Pattern.matches("[0-9A-Fa-f]+", scAddress))
                throw new Exception("Invalid sc address length: {}, expected 40");

            method = "getKeyOwnerships(address)";
            byte[] selector = Arrays.copyOf(Hash.sha3(method.getBytes()),4);
            String abiMethodString = "0x" + Numeric.toHexStringNoPrefix(selector);
            abiString = abiMethodString + "000000000000000000000000" + scAddress;
        }

        String requestBody = buildNscRequestBody(abiString);
        HttpURLConnection connection = Helper.sendRequestWithAuth(Definitions.NSC_URL + "ethv1", requestBody, "user", "Horizen");

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception(connection.getResponseCode() + " " + connection.getResponseMessage());
        }

        // Read the response body
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Print the response body
        System.out.println("Response Body: " + response);

        // Parse the JSON response
        JsonObject responseObject = JsonParser.parseString(response.toString()).getAsJsonObject();

        // Get the result value
        String abiReturnValue = responseObject.get("result").getAsString();

        // Remove the "0x" prefix
        if (abiReturnValue.startsWith("0x")) {
            abiReturnValue = abiReturnValue.substring(2);
        }

        return getKeyOwnershipFromAbi(abiReturnValue);
    }


    public static List<String> getNscOwnerScAddresses() throws Exception {
        String method = "getKeyOwnerScAddresses()";
        byte[] selector = Arrays.copyOf(Hash.sha3(method.getBytes()),4);
        String abiString = "0x" + Numeric.toHexStringNoPrefix(selector);

        String requestBody = buildNscRequestBody(abiString);
        HttpURLConnection connection = Helper.sendRequestWithAuth(Definitions.NSC_URL + "ethv1", requestBody, "user", "Horizen");

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Problem with getKeyOwnerScAddresses()");
        }

        // Read the response body
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Print the response body
        System.out.println("Response Body: " + response);

        // Parse the JSON response
        JsonObject responseObject = JsonParser.parseString(response.toString()).getAsJsonObject();

        // Get the result value
        String abiReturnValue = responseObject.get("result").getAsString();

        // Remove the "0x" prefix
        if (abiReturnValue.startsWith("0x"))
            abiReturnValue = abiReturnValue.substring(2);

        return getOwnerScAddrFromAbi(abiReturnValue);
    }



    public static Map<String, List<String>> getKeyOwnershipFromAbi(String abiReturnValue) {
        int startDataOffset = TypeDecoder.decode(abiReturnValue.substring(0,64), Uint32.class).getValue().intValue() * 2;
        int endDataOffset = startDataOffset + 64;
        int listSize = TypeDecoder.decode(abiReturnValue.substring(startDataOffset, endDataOffset), Uint32.class).getValue().intValue();

        Map<String, List<String>> scAssociations = new HashMap<>();
        for (int i = 0; i < listSize; i++) {
            startDataOffset = endDataOffset;
            endDataOffset = startDataOffset + 64;
            Address addressPref = TypeDecoder.decode(abiReturnValue.substring(startDataOffset, endDataOffset), Address.class);
            startDataOffset = endDataOffset;
            endDataOffset = startDataOffset + 64;
            Bytes3 mca3  = TypeDecoder.decode(abiReturnValue.substring(startDataOffset, endDataOffset), Bytes3.class);
            startDataOffset = endDataOffset;
            endDataOffset = startDataOffset + 64;
            Bytes32 mca32  = TypeDecoder.decode(abiReturnValue.substring(startDataOffset, endDataOffset), Bytes32.class);

            String scAddressChecksumFmt = Keys.toChecksumAddress(addressPref.getValue());
            byte[] combinedArray = ByteBuffer.allocate(35)
                    .put(mca3.getValue())
                    .put(mca32.getValue())
                    .array();
            String mcAddr = new String(combinedArray, StandardCharsets.UTF_8);

            List<String> mcAddresses;
            if (scAssociations.containsKey(scAddressChecksumFmt)) {
                mcAddresses = scAssociations.get(scAddressChecksumFmt);
                mcAddresses.add(mcAddr);
            }
            else {
                mcAddresses = new LinkedList<>();
                mcAddresses.add(mcAddr);
                scAssociations.put(scAddressChecksumFmt, mcAddresses);
            }
        }

        return scAssociations;
    }

    public static List<String> getOwnerScAddrFromAbi(String abiReturnValue) {
        int startDataOffset = TypeDecoder.decode(abiReturnValue.substring(0,64), Uint32.class).getValue().intValue() * 2;
        int endDataOffset = startDataOffset + 64;
        int listSize = TypeDecoder.decode(abiReturnValue.substring(startDataOffset, endDataOffset), Uint32.class).getValue().intValue();

        List<String> scAddresses = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            startDataOffset = endDataOffset;
            endDataOffset = startDataOffset + 64;
            Address addressPref = TypeDecoder.decode(abiReturnValue.substring(startDataOffset, endDataOffset), Address.class);

            String scAddressChecksumFmt = Keys.toChecksumAddress(addressPref.getValue());
            scAddresses.add(scAddressChecksumFmt);
        }

        return scAddresses;
    }

    private static String buildNscRequestBody(String abiString) {
        // Create the request body JSON object
        JsonObject requestBody = new JsonObject();

        // Set the properties of the request body
        requestBody.addProperty("jsonrpc", "2.0");
        requestBody.addProperty("method", "eth_call");

        // Create the params JSON array
        JsonArray paramsArray = new JsonArray();

        // Create the params JSON object
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("from", Definitions.ETH_CALL_FROM_ADDRESS);
        paramsObject.addProperty("to", "0x0000000000000000000088888888888888888888");
        paramsObject.addProperty("value", "0x00");
        paramsObject.addProperty("gasLimit", "0x21000");
        paramsObject.addProperty("maxPriorityFeePerGas", "0x900000000");
        paramsObject.addProperty("maxFeePerGas", "0x900000000");
        paramsObject.addProperty("data", abiString);

        // Add the params JSON object to the params JSON array
        paramsArray.add(paramsObject);

        // Add the params JSON array to the request body
        requestBody.add("params", paramsArray);

        // Set the id property of the request body
        requestBody.addProperty("id", 1);

        // Create Gson instance
        Gson gson = MyGsonManager.getGson();

        // Convert the Java object to JSON string
        return gson.toJson(requestBody);
    }
}
