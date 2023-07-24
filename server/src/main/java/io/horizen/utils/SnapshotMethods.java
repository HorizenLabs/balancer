package io.horizen.utils;

import io.horizen.data_types.VotingProposal;
import io.horizen.exception.OwnerStringException;
import io.horizen.exception.OwnershipAlreadySetException;
import io.horizen.helpers.Definitions;
import io.horizen.helpers.Helper;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotMethods {

    private static VotingProposal activeProposal;
    private static final Map<String, VotingProposal> proposals = new HashMap<>();

    public static void storeProposalData(VotingProposal votingProposal) throws Exception {
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
        if (Definitions.MOCK_NSC)
            return Definitions.MOCK_MC_ADDRESS_MAP;
        else
            return NscMethods.getNscOwnerships(scAddress);
    }

    public static List<String> getOwnerScAddrList() throws Exception {
        if (Definitions.MOCK_NSC)
            return Definitions.MOCK_OWNER_SC_ADDR_LIST;
        else
            return NscMethods.getNscOwnerScAddresses();
    }

    public static void addOwnershipEntry(String address, String owner) throws AddressFormatException, OwnerStringException, OwnershipAlreadySetException {
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

    public static void initActiveProposal() {
        VotingProposal votingProposal = Helper.readProposalFromFile();
        if (votingProposal != null) {
            activeProposal = votingProposal;
            proposals.put(votingProposal.getId(), votingProposal);
        }
    }
}
