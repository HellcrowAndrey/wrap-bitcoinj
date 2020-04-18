package com.wrap.bitcoinj.filters;

import com.google.common.util.concurrent.ListenableFuture;
import com.wrap.bitcoinj.listeners.ListenersManager;
import com.wrap.bitcoinj.models.*;
import com.wrap.bitcoinj.utils.PushNewInfo;
import lombok.AllArgsConstructor;
import org.bitcoinj.core.Transaction;

@AllArgsConstructor
public class CollectorFilter extends Middleware {

    private StateTrx state;

    @Override
    public void transactionFilter(Transaction trx, String hash, long height) {
        TransactionData r = TransactionData.builder()
                .height(height)
                .hashBlock(hash)
                .hashTrx(trx.getTxId().toString())
                .confirmation(1)
                .inputs(this.state.getInputs())
                .outputs(this.state.getOutputs())
                .build();
        ListenableFuture<TransactionData> f = PushNewInfo.sendTrxData(r);
        ListenersManager.addFutureTrxData(f);
    }

}
