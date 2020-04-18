package com.wrap.bitcoinj.filters;

import org.bitcoinj.core.Transaction;

import java.math.BigInteger;

public abstract class Middleware {

    private Middleware middleware;

    public Middleware nextLink(Middleware next) {
        this.middleware = next;
        return this.middleware;
    }

    public abstract void transactionFilter(Transaction trx, String hash, long height);

    public void toNextFilter(Transaction trx, String hash, long height) {
        if (this.middleware != null) {
            middleware.transactionFilter(trx, hash, height);
        }
    }

}
