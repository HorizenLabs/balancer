package io.horizen.services;

import io.horizen.data_types.VotingProposal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SnapshotService {

    void storeProposalData(VotingProposal votingProposal) throws Exception;

    VotingProposal getActiveProposal(int snapshot);

    List<String> getOwnerScAddrList() throws Exception;

    void addMockOwnershipEntry(String address, String owner) throws Exception;

    Map<String, List<String>> getMcAddressMap(String scAddress) throws Exception;

    void initActiveProposals();

    Collection<VotingProposal> getProposals();

    int getSnapshotProposal(String proposalID) throws Exception;
}
