package io.horizen.services.impl;

import com.google.inject.Inject;
import io.horizen.config.Settings;
import io.horizen.data_types.VotingProposal;
import io.horizen.exception.ScAddressFormatException;
import io.horizen.exception.OwnershipAlreadySetException;
import io.horizen.helpers.Mocks;
import io.horizen.helpers.Helper;
import io.horizen.services.NscService;
import io.horizen.services.SnapshotService;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;

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

    public VotingProposal getActiveProposal() {
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

    public void initActiveProposal() {
        VotingProposal votingProposal;
        try {
            votingProposal = Helper.readProposalFromFile();
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
