package com.wrap.bitcoinj.listeners;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.wrap.bitcoinj.filters.CollectorFilter;
import com.wrap.bitcoinj.filters.Middleware;
import com.wrap.bitcoinj.filters.TrxInFilter;
import com.wrap.bitcoinj.filters.TrxOutFilter;
import com.wrap.bitcoinj.models.NewBlock;
import com.wrap.bitcoinj.models.StateTrx;
import com.wrap.bitcoinj.utils.PushNewInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener;
import org.bitcoinj.kits.WalletAppKit;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class BlockListener implements BlocksDownloadedEventListener {

    private static final int TIME_OUT = 30;

    private WalletAppKit wallet;

    private NetworkParameters netParams;

    private ExecutorService executor;

    public BlockListener(WalletAppKit wallet,
                         NetworkParameters netParams,
                         ExecutorService executor) {
        this.wallet = wallet;
        this.netParams = netParams;
        this.executor = executor;
    }

    @Override
    public void
    onBlocksDownloaded(Peer peer, Block block,
                       @Nullable FilteredBlock filteredBlock, int blocksLeft) {
        try {
            Peer peer1 = this.wallet.peerGroup().getDownloadPeer();
            Block result = peer1
                    .getBlock(block.getHash())
                    .get(TIME_OUT, TimeUnit.SECONDS);
            String hash = result.getHash().toString();
            String prevHash = result.getPrevBlockHash().toString();
            long height = peer.getBestHeight();
            ListenableFuture<NewBlock> f = PushNewInfo.sendNewBlock(new NewBlock(height, hash, prevHash));
            ListenersManager.addFutureNewBlock(f);
            List<Transaction> trxs = result.getTransactions();
            if (trxs != null) {
                List<List<Transaction>> partitions = Lists.partition(trxs, 20);
                partitions.forEach(transactions -> {
                    CountDownLatch l = new CountDownLatch(transactions.size());
                    transactions.forEach(trx -> this.executor.execute(() -> {
                        StateTrx state = new StateTrx();
                        Middleware filters = new TrxInFilter(state, peer1, this.netParams);
                        filters.nextLink(new TrxOutFilter(state, this.netParams))
                                .nextLink(new CollectorFilter(state));
                        filters.transactionFilter(trx, hash, height);
                        l.countDown();
                    }));
                    try {
                        l.await();
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                });
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
           log.error(e.getMessage());
        }
    }

}
