package io.horizen.config;

public class Settings {
    private final String proposalJsonDataFileName;
    private final String proposalJsonDataPath;
    private final String nscUrl;
    private final String rosettaUrl;
    private final String network;
    private final Boolean mockNsc;
    private final Boolean mockRosetta;
    private final Boolean ssl;
    private final int serverPort;

    public Settings(String proposalJsonDataFileName, String proposalJsonDataPath, String nscUrl, String rosettaUrl, String network, Boolean mockNsc, Boolean mockRosetta, Boolean ssl, int serverPort) {
        this.proposalJsonDataFileName = proposalJsonDataFileName;
        this.proposalJsonDataPath = proposalJsonDataPath;
        this.nscUrl = nscUrl;
        this.rosettaUrl = rosettaUrl;
        this.network = network;
        this.mockNsc = mockNsc;
        this.mockRosetta = mockRosetta;
        this.ssl = ssl;
        this.serverPort = serverPort;
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

    public int getServerPort() {
        return serverPort;
    }

    public Boolean getSsl() {
        return ssl;
    }
}
