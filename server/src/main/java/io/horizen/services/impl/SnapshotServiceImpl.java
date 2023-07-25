package io.horizen.services.impl;

import com.google.inject.Inject;
import io.horizen.data_types.VotingProposal;
import io.horizen.exception.OwnerStringException;
import io.horizen.exception.OwnershipAlreadySetException;
import io.horizen.helpers.Definitions;
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

    @Inject // This annotation is important for Guice to perform injection
    public SnapshotServiceImpl(NscService nscService) {
        this.nscService = nscService;
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
        if (Definitions.MOCK_NSC)
            return Definitions.MOCK_MC_ADDRESS_MAP;
        else
            return nscService.getNscOwnerships(scAddress);
    }

    public List<String> getOwnerScAddrList() throws Exception {
        if (Definitions.MOCK_NSC)
            return Definitions.MOCK_OWNER_SC_ADDR_LIST;
        else
            return nscService.getNscOwnerScAddresses();
    }

    public void addOwnershipEntry(String address, String owner) throws AddressFormatException, OwnerStringException, OwnershipAlreadySetException {
        Base58.decodeChecked(address);

        if (owner.length() != 42  || !owner.substring(2).matches("[0-9A-Fa-f]+"))
            throw new OwnerStringException();
        else {
            if (Definitions.MOCK_MC_ADDRESS_MAP.containsKey(owner)) {
                List<String> addresses = Definitions.MOCK_MC_ADDRESS_MAP.get(owner);
                if (addresses.contains(address))
                    throw new OwnershipAlreadySetException();
                else
                    addresses.add(address);
            }
        }
    }

    public void initActiveProposal() {
        VotingProposal votingProposal = Helper.readProposalFromFile();
        if (votingProposal != null) {
            activeProposal = votingProposal;
            proposals.put(votingProposal.getId(), votingProposal);
        }
    }
}
