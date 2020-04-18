package com.wrap.bitcoinj.config;

import com.google.common.util.concurrent.FutureCallback;
import com.wrap.bitcoinj.models.*;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.DeterministicKey;

import java.math.BigDecimal;
import java.util.List;

public interface IBitcoinFacade {

    KeysBag generateKeys();

    List<ChainAddress> addressesMain(KeysBag data, int start, int amount);

    List<ChainAddress> addressesChange(KeysBag data, int start, int amount);

    void startWallet();

    void sendTrx(String trx, FutureCallback<Transaction> f);

    String send(String trx);

    DeterministicKey
    restoreChildPrivateKey(String prKey, String chainCodeHex, int index, boolean isChange);

    BigDecimal amountRight(BigDecimal amount);

    BigDecimal amountLeft(BigDecimal amount);

}
