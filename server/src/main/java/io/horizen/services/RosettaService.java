package io.horizen.services;

import io.horizen.data_types.MainchainTip;

public interface RosettaService {

    MainchainTip getMainchainTip() throws Exception;

    Double getAddressBalance(String scAddress, int snapshot) throws Exception;

    String getMainchainBlockHash(int height) throws Exception;
}
