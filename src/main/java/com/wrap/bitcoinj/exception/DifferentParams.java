package com.wrap.bitcoinj.exception;

public class DifferentParams extends RuntimeException {
    public DifferentParams(String message) {
        super(message);
    }
}
