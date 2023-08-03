package io.horizen.services;

import io.horizen.data_types.ChainTip;

public interface RosettaService {

    ChainTip getMainchainTip() throws Exception;

    Double getAddressBalance(String scAddress) throws Exception;

    String getMainchainBlockHash(int height) throws Exception;
}
