package io.horizen.exception;

public class GenericException extends BalancerException {

    public GenericException(String detail) {
        super(100, detail);
    }
}
