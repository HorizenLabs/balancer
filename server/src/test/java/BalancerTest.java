import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.horizen.Main;
import io.horizen.helpers.Mocks;
import io.horizen.helpers.MyGsonManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import spark.Spark;
import spark.utils.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BalancerTest {

    @BeforeClass
    public static void setUp() {
        Main.main(new String[]{""});
        Spark.awaitInitialization();
    }

    @AfterClass
    public static void tearDown() {
        Spark.stop();
    }

    @Test
    public void testCreateProposal() throws IOException {
        URL url = new URL("http://localhost:5000/api/v1/createProposal");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        String requestBody = "{'Body': 'HAL new format of notification\nProposal Created\nStarts on: 28 Jul 23 13:27 UTC\nEnds on: 31 Oct 23 13:27 UTC\nAuthor: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959', 'ProposalEvent': 'proposal/created', 'ProposalExpire': 0, 'ProposalID': 'proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133cb2', 'ProposalSpace': 'victorbibiano.eth'}";
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(requestBody);
        outputStream.flush();
        outputStream.close();

        String response = IOUtils.toString(connection.getInputStream());

        assertTrue(response.toLowerCase().contains("ok"));
    }

    @Test
    public void testGetProposals() throws IOException {
        URL url = new URL("http://localhost:5000/api/v1/getProposals");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        String response = IOUtils.toString(connection.getInputStream());
        JsonElement jsonElement = JsonParser.parseString(response);
        String proposalId = jsonElement.getAsJsonArray().get(0).getAsJsonObject().get("ID").getAsString();
        assertEquals(proposalId, "0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133cb2");
    }

    @Test
    public void testGetOwnerships() throws IOException {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("scAddress", "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959");

        URL url = new URL("http://localhost:5000/api/v1/getOwnerships");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(MyGsonManager.getGson().toJson(requestData).getBytes());
        }

        String response = IOUtils.toString(connection.getInputStream());
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> resultMap = MyGsonManager.getGson().fromJson(response, mapType);

        Map<String, List<String>> expectedMap = new HashMap<>();
        List<String> addressList = new ArrayList<>();
        addressList.add("ztbX9Kg53BYK8iJ8cydJp3tBYcmzT8Vtxn7");
        addressList.add("ztUSSkdLdgCG2HnwjrEKorauUR2JXV26u7v");
        expectedMap.put("0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959",addressList);

        assertEquals(expectedMap, resultMap);
    }

    @Test
    public void testXAddOwnership() throws IOException { // X in the name because the test are run in NAME_ASCENDING order which is crucial
        Map<String, Object> addOwnershipData = new HashMap<>();
        addOwnershipData.put("owner", "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959");
        addOwnershipData.put("address", "ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt");

        URL url = new URL("http://localhost:5000/api/v1/addOwnership");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(MyGsonManager.getGson().toJson(addOwnershipData).getBytes());
        }

        String response = IOUtils.toString(connection.getInputStream());

        JsonElement jsonElement = JsonParser.parseString(response);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        JsonObject ownershipsObject = jsonObject.getAsJsonObject("ownerships");
        JsonObject resultObject = jsonObject.getAsJsonObject("result");

        String statusValue = resultObject.get("status").getAsString();
        assertEquals("Ok", statusValue);

        Map<String, List<String>> ownershipsMap = parseOwnershipsMap(ownershipsObject);

        assertEquals(Mocks.mockMcAddressMap, ownershipsMap);
        assertTrue(ownershipsMap.containsKey("0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959"));
        assertTrue(ownershipsMap.get("0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959").contains("ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt"));
    }

    @Test
    public void testVotingPower() throws IOException {
        URL url = new URL("http://localhost:5000/api/v1/getVotingPower?network=80001&snapshot=34522768&addresses=0xf43F35c1A3E36b5Fb554f5E830179cc460c35858&options=api-post&options=https://localhost:5000/api/v1/getVotingPower");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        String response = IOUtils.toString(connection.getInputStream());

        JsonElement jsonElement = JsonParser.parseString(response);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        JsonArray scoreArray = jsonObject.getAsJsonArray("score");
        JsonObject scoreEntry = scoreArray.get(0).getAsJsonObject();
        String address = scoreEntry.get("address").getAsString();
        String score = scoreEntry.get("score").getAsString();

        assertEquals("123456789", score);
        assertEquals("0xf43F35c1A3E36b5Fb554f5E830179cc460c35858", address);
    }

    @Test
    public void testGetOwnerScAddresses() throws IOException {
        URL url = new URL("http://localhost:5000/api/v1/getOwnerScAddresses");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        String response = IOUtils.toString(connection.getInputStream());

        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> addrestList = MyGsonManager.getGson().fromJson(response, listType);

        assertEquals(Mocks.mockOwnerScAddrList, addrestList);
    }

    private Map<String, List<String>> parseOwnershipsMap(JsonObject ownershipsObject) {
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        return MyGsonManager.getGson().fromJson(ownershipsObject, mapType);
    }
}