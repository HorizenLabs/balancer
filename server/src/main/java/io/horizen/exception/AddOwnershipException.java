package io.horizen.exception;

public class AddOwnershipException extends BalancerException {

    public AddOwnershipException(String detail) {
        super(101, detail);
    }
}
