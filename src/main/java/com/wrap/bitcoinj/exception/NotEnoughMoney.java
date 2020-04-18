package com.wrap.bitcoinj.exception;

public class NotEnoughMoney extends RuntimeException {

    public NotEnoughMoney() {
    }

    public NotEnoughMoney(String message) {
        super(message);
    }
}
