package com.wrap.bitcoinj.exception;

public class ErrorFee extends RuntimeException {

    public ErrorFee() {
    }

    public ErrorFee(String message) {
        super(message);
    }
}
