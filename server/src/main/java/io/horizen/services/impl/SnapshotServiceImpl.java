package io.horizen.services.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import io.horizen.config.Settings;
import io.horizen.data_types.VotingProposal;
import io.horizen.exception.OwnershipAlreadySetException;
import io.horizen.exception.ScAddressFormatException;
import io.horizen.exception.SnapshotException;
import io.horizen.helpers.Helper;
import io.horizen.helpers.Mocks;
import io.horizen.services.NscService;
import io.horizen.services.SnapshotService;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import spark.utils.IOUtils;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotServiceImpl implements SnapshotService {
    private final NscService nscService;
    private final Settings settings;

    @Inject
    public SnapshotServiceImpl(NscService nscService, Settings settings) {
        this.nscService = nscService;
        this.settings = settings;
    }

    private static VotingProposal activeProposal;
    private static final Map<String, VotingProposal> proposals = new HashMap<>();

    public void storeProposalData(VotingProposal votingProposal) throws Exception {
        if (proposals.containsKey(votingProposal.getId()))
            return;

        activeProposal = votingProposal;
        proposals.put(votingProposal.getId(), votingProposal);
        Helper.writeProposalToFile(votingProposal);
    }

    public Collection<VotingProposal> getProposals() {
        return proposals.values();
    }

    @Override
    public int getSnapshotProposal(String proposalID) throws Exception {
        if (settings.getMockSnapshot())
            return Mocks.MOCK_SNAPSHOT_VALUE;

        String graphqlQuery = settings.getSnapshotRequestQueryString().replace("SNAPSHOT_PROPOSAL_ID", proposalID);

        try {
            HttpURLConnection connection = Helper.sendRequest(settings.getSnapshotUrl(), graphqlQuery, false);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = IOUtils.toString(connection.getInputStream());
                JsonObject responseObject = JsonParser.parseString(response).getAsJsonObject();

                if (!responseObject.has("data"))
                    throw new SnapshotException("An exception occurred trying to get proposal details from snapshot: " + responseObject);
                else if (!responseObject.get("data").getAsJsonObject().has("proposal") || !responseObject.get("data").getAsJsonObject().get("proposal").getAsJsonObject().has("snapshot"))
                    throw new SnapshotException("An exception occurred trying to get proposal details from snapshot: " + responseObject);
                else
                    return responseObject.get("data").getAsJsonObject().get("proposal").getAsJsonObject().get("snapshot").getAsInt();
            }
            else
                throw new SnapshotException("An exception occurred trying to get proposal details from snapshot: "+ connection.getResponseCode() + " " + connection.getResponseMessage());
        } catch (Exception ex) {
            throw new SnapshotException(ex.toString());
        }
    }

    public VotingProposal getActiveProposal(int snapshot) {
        //todo get the proposal from the dict having the input snapshot
        return activeProposal;
    }

    public Map<String, List<String>> getMcAddressMap(String scAddress) throws Exception {
        scAddress = Helper.checkScAddress(scAddress);

        if (settings.getMockNsc()) {
            if (!scAddress.startsWith("0x"))
                scAddress = "0x" + scAddress;
            Map<String, List<String>> ret = new HashMap<>();
            ret.put(scAddress, Mocks.mockMcAddressMap.get(scAddress));
            return ret;
        }
        else
            return nscService.getNscOwnerships(scAddress);
    }

    public List<String> getOwnerScAddrList() throws Exception {
        if (settings.getMockNsc())
            return Mocks.mockOwnerScAddrList;
        else
            return nscService.getNscOwnerScAddresses();
    }

    public void addMockOwnershipEntry(String address, String owner) throws AddressFormatException, ScAddressFormatException, OwnershipAlreadySetException {
        Base58.decodeChecked(address);
        Helper.checkScAddress(owner);

        if (Mocks.mockMcAddressMap.containsKey(owner)) {
            List<String> addresses = Mocks.mockMcAddressMap.get(owner);
            if (addresses.contains(address))
                throw new OwnershipAlreadySetException();
            else
                addresses.add(address);
        }
    }

    public void initActiveProposals() {
        // todo initialize the full dict of proposals
        VotingProposal votingProposal;
        try {
            votingProposal = Helper.readProposalsFromFile();
        } catch (Exception ex) {
            votingProposal = null;
        }
        if (votingProposal != null) {
            activeProposal = votingProposal;
            proposals.put(votingProposal.getId(), votingProposal);
            Helper.warnIfProposalNotActive(activeProposal);
        }
    }
}
