package io.horizen.services;

import io.horizen.data_types.ChainTip;

public interface RosettaService {

    ChainTip getChainTip() throws Exception;

    Double getAddressBalance(String scAddress) throws Exception;
}
