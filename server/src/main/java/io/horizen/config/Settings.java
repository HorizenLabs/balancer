package io.horizen.config;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

public class Settings {
    private final String proposalJsonDataFileName;
    private final String proposalJsonDataPath;
    private final String ethCallFromAddress;
    private final String nscUrl;
    private final String rosettaUrl;
    private final String network;
    private final Boolean mockNsc;
    private final Boolean mockRosetta;

    public Settings() {
        this.mockNsc = getBooleanEnv("MOCK_NSC");
        this.mockRosetta = getBooleanEnv("MOCK_ROSETTA");
        this.nscUrl = Optional.ofNullable(System.getenv("NSC_URL")).orElse("http://zendao-tn-1.de.horizenlabs.io:8200/");
        this.rosettaUrl = Optional.ofNullable(System.getenv("ROSETTA_URL")).orElse("http://localhost:8080/");
        this.network = Optional.ofNullable(System.getenv("ROSETTA_NETWORK_TYPE")).orElse("test"); // The network type which rosetta is running on. Can be 'test' or 'main'
        this.ethCallFromAddress = Optional.ofNullable(System.getenv("ETH_CALL_FROM_ADDRESS")).orElse("0x00c8f107a09cd4f463afc2f1e6e5bf6022ad4600");
        this.proposalJsonDataPath = Optional.ofNullable(System.getenv("PROPOSAL_JSON_DATA_PATH")).orElse(Paths.get(System.getProperty("user.dir")).getParent().getParent().toString() + "/");
        this.proposalJsonDataFileName = Optional.ofNullable(System.getenv("PROPOSAL_JSON_DATA_FILE_NAME")).orElse("active_proposal.json");
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

    public Boolean getMockRosetta() {
        return mockRosetta;
    }

    public String getProposalJsonDataFileName() {
        return proposalJsonDataFileName;
    }

    public String getProposalJsonDataPath() {
        return proposalJsonDataPath;
    }

    public String getEthCallFromAddress() {
        return ethCallFromAddress;
    }
}
