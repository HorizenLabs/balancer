package io.horizen;

import org.bitcoinj.core.Base58;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotMethods {

    private static VotingProposal activeProposal;
    private static final Map<String, VotingProposal> proposals = new HashMap<>();

    public static void storeProposalData(VotingProposal votingProposal) {
        //todo check if proposal with same id exists
        activeProposal = votingProposal;
        proposals.put(votingProposal.getId(), votingProposal);
    }

    public static Collection<VotingProposal> getProposals() {
        return proposals.values();
    }

    public static VotingProposal getActiveProposal() {
        return activeProposal;
    }

    public static Map<String, List<String>> getMcAddressMap(String scAddress) throws Exception {
        if (Constants.MOCK_NSC)
            return Constants.MOCK_MC_ADDRESS_MAP;
        else
            return NscMethods.getNscOwnerships(scAddress);
    }

    public static void addOwnershipEntry(String address, String owner) {
        try {
            Base58.decodeChecked(address);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        if (owner.length() != 42  || !owner.substring(2).matches("[0-9A-Fa-f]+")) {
            throw new RuntimeException();
        }
        else {
            if (Constants.MOCK_MC_ADDRESS_MAP.containsKey(owner)) {
                List<String> addresses = Constants.MOCK_MC_ADDRESS_MAP.get(owner);
                if (addresses.contains(address)) {
                    throw new RuntimeException();
                }
                else {
                    addresses.add(address);
                }
            }
        }
    }
}
