package com.wrap.bitcoinj.listeners;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.wrap.bitcoinj.models.NewBlock;
import com.wrap.bitcoinj.models.TransactionData;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ListenersManager {

    private static ExecutorService EXECUTOR_SERVICE;

    private static FutureCallback<NewBlock> BLOCK_LISTENER;

    private static FutureCallback<TransactionData> TRX_DATA_LISTENER;

    static {
        EXECUTOR_SERVICE = Executors.newFixedThreadPool(20);
    }

    public static void addBlockListener(FutureCallback<NewBlock> l) {
        BLOCK_LISTENER = l;
    }

    public static void addTransactionListener(FutureCallback<TransactionData> l) {
        TRX_DATA_LISTENER = l;
    }

    public static void addFutureNewBlock(ListenableFuture<NewBlock> f) {
        Futures.addCallback(f, BLOCK_LISTENER, EXECUTOR_SERVICE);
    }

    public static void addFutureTrxData(ListenableFuture<TransactionData> f) {
        Futures.addCallback(f, TRX_DATA_LISTENER, EXECUTOR_SERVICE);
    }

}
