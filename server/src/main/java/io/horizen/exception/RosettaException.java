package io.horizen.exception;

public class RosettaException extends BalancerException {

    public RosettaException(String detail) {
        super(106, detail);
    }
}
