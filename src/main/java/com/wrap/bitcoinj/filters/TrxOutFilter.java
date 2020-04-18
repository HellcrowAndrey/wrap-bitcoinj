package com.wrap.bitcoinj.filters;

import com.wrap.bitcoinj.models.StateTrx;
import com.wrap.bitcoinj.models.TOutput;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class TrxOutFilter extends Middleware {

    private StateTrx state;

    private NetworkParameters netParams;

    @Override
    public void transactionFilter(Transaction trx, String hash, long height) {
        List<TransactionOutput> outputs = trx.getOutputs();
        if (outputs != null && !outputs.isEmpty()) {
            outputs.forEach(this::getAddress);
        }
        toNextFilter(trx, hash, height);
    }

    private void getAddress(TransactionOutput o) {
        try {
            Script script = new Script(o.getScriptBytes());
            String address = script.getToAddress(this.netParams,
                    Boolean.TRUE).toString();
            TOutput output = new TOutput(
                    address, o.getIndex(),
                    Utils.HEX.encode(script.getPubKeyHash()),
                    Utils.HEX.encode(o.getScriptBytes()),
                    o.getValue().value
            );
            this.state.getOutputs().add(output);
        } catch (Exception e) {
            log.warn("Can't parse outputs.");
        }
    }

}
