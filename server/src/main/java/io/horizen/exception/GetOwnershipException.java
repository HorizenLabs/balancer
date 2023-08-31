package io.horizen.exception;

public class GetOwnershipException extends BalancerException {

    public GetOwnershipException(String detail) {
        super(102, detail);
    }
}
