package com.wrap.bitcoinj.utils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.wrap.bitcoinj.models.NewBlock;
import com.wrap.bitcoinj.models.TransactionData;

public class PushNewInfo {

    public static ListenableFuture<NewBlock> sendNewBlock(NewBlock b) {
        SettableFuture<NewBlock> future = SettableFuture.create();
        if (b == null) {
            future.setException(new NullPointerException());
        } else {
            future.set(b);
        }
        return future;
    }

    public static ListenableFuture<TransactionData> sendTrxData(TransactionData trx) {
        SettableFuture<TransactionData> future = SettableFuture.create();
        if (trx == null) {
            future.setException(new NullPointerException());
        } else {
            future.set(trx);
        }
        return future;
    }

}
