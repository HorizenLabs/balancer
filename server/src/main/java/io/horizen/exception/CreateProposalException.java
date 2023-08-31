package io.horizen.exception;

public class CreateProposalException extends BalancerException {

    public CreateProposalException(String detail) {
        super(104, detail);
    }
}
