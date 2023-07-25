package io.horizen.services;

import io.horizen.data_types.VotingProposal;
import io.horizen.exception.OwnerStringException;
import io.horizen.exception.OwnershipAlreadySetException;
import org.bitcoinj.core.AddressFormatException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SnapshotService {

    void storeProposalData(VotingProposal votingProposal) throws Exception;

    VotingProposal getActiveProposal();

    List<String> getOwnerScAddrList() throws Exception;

    void addOwnershipEntry(String address, String owner) throws AddressFormatException, OwnerStringException, OwnershipAlreadySetException;

    Map<String, List<String>> getMcAddressMap(String scAddress) throws Exception;

    void initActiveProposal();

    Collection<VotingProposal> getProposals();
}
