import com.google.gson.Gson;
import io.horizen.Main;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import spark.Spark;
import spark.utils.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class BalancerTest {

    @BeforeClass
    public static void setUp() throws Exception {
        // Set up Spark before running the tests
        Main.main(null); // Start your Spark application
        Spark.awaitInitialization(); // Wait until Spark is ready
    }

    @AfterClass
    public static void tearDown() {
        // Shut down Spark after running the tests
        Spark.stop();
    }

    @Test
    public void testHelloEndpoint() throws IOException {
        // Make a GET request to the "/hello" endpoint
        URL url = new URL("http://localhost:8080/hello");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Get the response
        String response = IOUtils.toString(connection.getInputStream());

        // Assert the response
        assertEquals("Hello, World!", response);
    }

    @Test
    public void testGetProposals() throws IOException {
        URL url = new URL("http://localhost:8080/api/v1/getProposals");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        // Get the response
        String response = IOUtils.toString(connection.getInputStream());

        // Assert the response
        assertEquals("[]", response);
    }

    @Test
    public void testCreateProposals() throws IOException {
        URL url = new URL("http://localhost:8080/api/v1/createProposal");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // Set the request headers
        connection.setRequestProperty("Content-Type", "application/json");

        // Write the request body
        String requestBody = "{'Body': 'Start: 18 Apr 23 13:40 UTC, End: 18 Apr 23 13:45 UTC, Author: 0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959', 'ProposalEvent': 'proposal/created', 'ProposalExpire': 0, 'ProposalID': 'proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133cb2', 'ProposalSpace': 'victorbibiano.eth'}";
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(requestBody);
        outputStream.flush();
        outputStream.close();

        // Get the response
        String response = IOUtils.toString(connection.getInputStream());

        // Assert the response
        assertEquals("OK", response);


        // test if new proposal was added
        URL urlGetProposals = new URL("http://localhost:8080/api/v1/getProposals");
        HttpURLConnection connectionGetProposals = (HttpURLConnection) urlGetProposals.openConnection();
        connectionGetProposals.setRequestMethod("POST");

        // Get the response
        String responseGetProposals = IOUtils.toString(connectionGetProposals.getInputStream());
        String expectedResponse = "[\n" +
                "  {\n" +
                "    \"id\": \"proposal/0xeca96e839070fff6f6c5140fcf4939779794feb6028edecc03d5f518133cb2\",\n" +
                "    \"blockHeight\": 100,\n" +
                "    \"blockHash\": \"000439739cac7736282169bb10d368123ca553c45ea6d4509d809537cd31aa0d\",\n" +
                "    \"fromTime\": \"Apr 18, 2023, 3:40:00 PM\",\n" +
                "    \"toTime\": \"Apr 18, 2023, 3:45:00 PM\",\n" +
                "    \"author\": \"0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959\"\n" +
                "  }\n" +
                "]";
        assertEquals(expectedResponse, responseGetProposals);
    }

    @Test
    public void testGetOwnerships() throws IOException {
        URL url = new URL("http://localhost:8080/api/v1/getOwnerships");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        // Get the response
        String response = IOUtils.toString(connection.getInputStream());
        Gson gson = new Gson();
        // Assert the response
        //assertEquals(gson.toJson(Constants.MOCK_MC_ADDRESS_MAP), response);
    }
}
