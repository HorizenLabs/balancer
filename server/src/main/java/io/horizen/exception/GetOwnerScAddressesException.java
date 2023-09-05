package io.horizen.exception;

public class GetOwnerScAddressesException extends BalancerException {

    public GetOwnerScAddressesException(String detail) {
        super(103, detail);
    }
}
