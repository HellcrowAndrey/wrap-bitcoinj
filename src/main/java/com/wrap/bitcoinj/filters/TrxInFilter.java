package com.wrap.bitcoinj.filters;

import com.google.common.util.concurrent.ListenableFuture;
import com.wrap.bitcoinj.models.StateTrx;
import com.wrap.bitcoinj.models.TInput;
import com.wrap.bitcoinj.utils.Network;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.wallet.DefaultRiskAnalysis;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class TrxInFilter extends Middleware {

    private StateTrx state;

    private Peer peer;

    private NetworkParameters netParams;

    @Override
    public void transactionFilter(Transaction trx, String hash, long height) {
        List<TransactionInput> inputs = trx.getInputs();
        if (inputs != null && !inputs.isEmpty()) {
            inputs.forEach(this::getAddress);
        }
        toNextFilter(trx, hash, height);
    }

    private void getAddress(TransactionInput input) {
        try {
            if (input != null) {
                Script script = input.getScriptSig();
                byte[] result = ScriptPattern.extractHashFromP2SH(script);
                ECKey ecKey = ECKey.fromPublicOnly(result);
                Address address = LegacyAddress.fromKey(this.netParams, ecKey);
                TInput i = new TInput();
                i.setAddress(address.toString());
                i.setIndex(input.getOutpoint().getIndex());
                i.setHash(input.getOutpoint().getHash().toString());
                this.state.getInputs().add(i);
            }
        } catch (Exception e) {
            log.warn("Can't parse inputs.");
        } finally {
            if (Objects.nonNull(input)) {
                try {
                    Sha256Hash hash = input.getOutpoint().getHash();
                    long index = input.getOutpoint().getIndex();
                    ListenableFuture<Transaction> f = peer.getPeerMempoolTransaction(hash);
                    Transaction result = f.get(1, TimeUnit.SECONDS);
                    result.getOutputs().stream()
                            .filter(o -> o.getIndex() == index)
                            .findFirst()
                            .ifPresent(output -> {
                                String address = new Script(output.getScriptBytes())
                                        .getToAddress(this.netParams).toString();
                                log.info("Address, {}", address);
                                TInput i = new TInput(address, index, hash.toString());
                                this.state.getInputs().add(i);
                            });
                } catch (Exception ignore) {
                    log.warn(ignore.getMessage());
                }
            }
        }
    }

}
