package io.horizen.config;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

public class Settings {
    private final String proposalJsonDataFileName;
    private final String proposalJsonDataPath;
    private final String ethCallFromAddress;
    private final String nscUrl;
    private final String nscUrlPostfix;
    private final String rosettaUrl;
    private final String snapshotUrl;
    private final String username;
    private final String password;
    private final String network;
    private final String snapshotRequestQueryString;
    private final Boolean mockNsc;
    private final Boolean mockSnapshot;
    private final Boolean listeningOnHTTP;
    private final Boolean runningOnLocalhost;
    private final Boolean mockRosetta;
    private final Boolean usingWSGIProxy;
    private final int balancerPort;

    public Settings() {
        this.mockNsc = getBooleanEnv("MOCK_NSC");
        this.mockRosetta = getBooleanEnv("MOCK_ROSETTA");
        this.mockSnapshot = getBooleanEnv("MOCK_SNAPSHOT");
        this.listeningOnHTTP = getBooleanEnv("LISTENING_ON_HTTP");
        this.runningOnLocalhost = getBooleanEnv("RUNNING_ON_LOCALHOST");
        this.usingWSGIProxy = getBooleanEnv("USING_WSGI_PROXY");
        this.nscUrl = Optional.ofNullable(System.getenv("NSC_URL")).orElse("http://zendao-tn-1.de.horizenlabs.io:8200/");
        this.rosettaUrl = Optional.ofNullable(System.getenv("ROSETTA_URL")).orElse("http://localhost:8080/");
        this.snapshotUrl = Optional.ofNullable(System.getenv("SNAPSHOT_URL")).orElse("https://hub.snapshot.org/graphql");
        this.network = Optional.ofNullable(System.getenv("ROSETTA_NETWORK_TYPE")).orElse("test"); // The network type which rosetta is running on. Can be 'test' or 'main'
        this.ethCallFromAddress = Optional.ofNullable(System.getenv("ETH_CALL_FROM_ADDRESS")).orElse("0x00c8f107a09cd4f463afc2f1e6e5bf6022ad4600");
        this.proposalJsonDataPath = Optional.ofNullable(System.getenv("PROPOSAL_JSON_DATA_PATH")).orElse(Paths.get(System.getProperty("user.dir")).getParent().getParent().toString() + "/");
        this.proposalJsonDataFileName = Optional.ofNullable(System.getenv("PROPOSAL_JSON_DATA_FILE_NAME")).orElse("active_proposal.json");
        this.username = "user";
        this.password = "Horizen";
        this.nscUrlPostfix = "ethv1";
        this.balancerPort = Integer.parseInt(Optional.ofNullable(System.getenv("BALANCER_PORT")).orElse("5000"));
        this.snapshotRequestQueryString = "{\"query\": \"query Proposal {proposal(id: \\\"SNAPSHOT_PROPOSAL_ID\\\") {snapshot}}\"}";
    }

    private boolean getBooleanEnv(String envName) {
        String envValue = System.getenv(envName);
        if (envValue != null) {
            return envValue.toLowerCase(Locale.ROOT).matches("(true|1|y|yes)");
        }
        return false;
    }

    public String getNscUrl() {
        return nscUrl;
    }

    public String getRosettaUrl() {
        return rosettaUrl;
    }

    public String getNetwork() {
        return network;
    }

    public Boolean getMockNsc() {
        return mockNsc;
    }
    public Boolean getListeningOnHTTP() {
        return listeningOnHTTP;
    }
    public Boolean getRunningOnLocalhost() {
        return runningOnLocalhost;
    }

    public Boolean getMockRosetta() {
        return mockRosetta;
    }

    public String getProposalJsonDataFileName() {
        return proposalJsonDataFileName;
    }

    public String getProposalJsonDataPath() {
        return proposalJsonDataPath;
    }
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEthCallFromAddress() {
        return ethCallFromAddress;
    }
    public String getNscUrlPostfix() {
        return nscUrlPostfix;
    }

    public int getBalancerPort() {
        return balancerPort;
    }

    public String getSnapshotUrl() {
        return snapshotUrl;
    }

    public Boolean getMockSnapshot() {
        return mockSnapshot;
    }

    public String getSnapshotRequestQueryString() {
        return snapshotRequestQueryString;
    }

    public Boolean getUsingWSGIProxy() {
        return usingWSGIProxy;
    }
}
