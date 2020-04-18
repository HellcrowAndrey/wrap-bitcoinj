package com.wrap.bitcoinj.config;

import com.wrap.bitcoinj.models.ChainAddress;
import com.wrap.bitcoinj.models.KeysBag;
import com.wrap.bitcoinj.utils.DeterministicPath;
import com.wrap.bitcoinj.utils.Network;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

public class NewTransactionTest {

    @Test
    public void keyPair() {
        IBitcoinFacade f = new BitcoinFacade(
                Network.MAIN,
                DeterministicPath.DETERMINISTIC_PATH_MAIN
        );
        KeysBag keys = f.generateKeys();
        assertNotNull(keys.getMnemonic());
        assertNotNull(keys.getPrivateKey());
        assertNotNull(keys.getPublicKey());
        assertNotNull(keys.getChainCode());
        assertNotEquals(0, keys.getTimeCreating());
    }

    @Test
    public void generateAddressFrom() {
        IBitcoinFacade f = new BitcoinFacade(
                Network.MAIN,
                DeterministicPath.DETERMINISTIC_PATH_MAIN
        );
        KeysBag k = new KeysBag(
                Mocks.PUBLIC_KEY_FROM,
                Mocks.CHAIN_CODE_FROM
        );
        List<ChainAddress> act = f.addressesMain(k, 0, 1);
        List<ChainAddress> exp = f.addressesMain(k, 0, 1);
        assertArrayEquals(act.toArray(), exp.toArray());
    }

    @Test
    public void generateAddressTo() {
        IBitcoinFacade f = new BitcoinFacade(
                Network.MAIN,
                DeterministicPath.DETERMINISTIC_PATH_MAIN
        );
        KeysBag k = new KeysBag(
                Mocks.PUBLIC_KEY_TO,
                Mocks.CHAIN_CODE_TO
        );
        List<ChainAddress> act = f.addressesMain(k, 0, 1);
        List<ChainAddress> exp = f.addressesMain(k, 0, 1);
        assertArrayEquals(act.toArray(), exp.toArray());
    }

    @Test
    public void transactionWithoutSign() {
        IBitcoinFacade f = new BitcoinFacade(
                Network.MAIN,
                DeterministicPath.DETERMINISTIC_PATH_MAIN
        );
        NewTransaction t = transaction(f);
        String trxHex = t.getTrxHex();
        NewTransaction act = new NewTransaction.SignBuilder(Network.MAIN, f)
                .keyPair(Mocks.PRIVATE_KEY_FROM, Mocks.CHAIN_CODE_FROM)
                .transaction(trxHex)
                .sign(t.getIndexes())
                .build();
        NewTransaction exp = new NewTransaction.SignBuilder(Network.MAIN, f)
                .keyPair(Mocks.PRIVATE_KEY_FROM, Mocks.CHAIN_CODE_FROM)
                .transaction(trxHex)
                .sign(t.getIndexes())
                .build();
        assertEquals(exp.getTrxHex(), act.getTrxHex());
    }

    private NewTransaction transaction(IBitcoinFacade f) {
        return new NewTransaction
                .RawBuilder()
                .parameters(Network.MAIN, f)
                .addressFrom(Mocks.ADDRESS_FROM)
                .addressTo(Mocks.ADDRESS_TO)
                .amount(new BigDecimal("0.001"))
                .outputs(Mocks.unspentOutputs(Network.MAIN))
                .feePerKb(new BigDecimal("10"))
                .calcUnspentOutput()
                .transaction()
                .build();
    }

}