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
        if (proposals.containsKey(votingProposal.getId()))
            return;

        activeProposal = votingProposal;
        proposals.put(votingProposal.getId(), votingProposal);
        Helper.writeProposalToFile(votingProposal);
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

    public static void addOwnershipEntry(String address, String owner) throws Exception {
        try {
            Base58.decodeChecked(address);
        }
        catch (Exception ex) {
            throw ex;
        }

        if (owner.length() != 42  || !owner.substring(2).matches("[0-9A-Fa-f]+")) {
            throw new Exception();
        }
        else {
            if (Constants.MOCK_MC_ADDRESS_MAP.containsKey(owner)) {
                List<String> addresses = Constants.MOCK_MC_ADDRESS_MAP.get(owner);
                if (addresses.contains(address)) {
                    throw new Exception();
                }
                else {
                    addresses.add(address);
                }
            }
        }
    }

    public static void initActiveProposal() {
        VotingProposal votingProposal = Helper.readProposalFromFile();
        activeProposal = votingProposal;
        proposals.put(votingProposal.getId(), votingProposal);
    }
}
