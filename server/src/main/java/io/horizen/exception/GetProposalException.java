package io.horizen.exception;

public class GetProposalException extends BalancerException {

    public GetProposalException(String detail) {
        super(105, detail);
    }
}
