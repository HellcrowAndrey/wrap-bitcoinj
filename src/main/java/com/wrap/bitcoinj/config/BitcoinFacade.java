package com.wrap.bitcoinj.config;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.wrap.bitcoinj.WrapWallet;
import com.wrap.bitcoinj.listeners.BlockListener;
import com.wrap.bitcoinj.models.*;
import com.wrap.bitcoinj.utils.BlockThreadFactory;
import com.wrap.bitcoinj.utils.Network;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class BitcoinFacade implements IBitcoinFacade {

    private static final int RATE = 8;

    private static final int PEERS_NUMBER = 1;

    private NetworkParameters netParams;

    private String deterministic;

    private WrapWallet wallet;

    public BitcoinFacade(Network network, String deterministic,
                         String path, String walletName) {
        this.netParams = network.get();
        this.deterministic = deterministic;
        this.wallet = new WrapWallet(network.get(),
                new File(path), walletName);
    }

    public BitcoinFacade(Network network, String deterministic) {
        this.netParams = network.get();
        this.deterministic = deterministic;
    }

    @Override
    public KeysBag generateKeys() {
        SecureRandom sr = new SecureRandom();
        DeterministicSeed seed = new DeterministicSeed(sr, 128, "");
        String mnemonic = String.join(" ",
                Objects.requireNonNull(seed.getMnemonicCode()));
        long timeCreating = System.currentTimeMillis();
        DeterministicKeyChain kc = DeterministicKeyChain.builder()
                .seed(seed).outputScriptType(Script.ScriptType.P2PKH)
                .accountPath(DeterministicKeyChain.ACCOUNT_ZERO_PATH).build();
        List<ChildNumber> result = HDUtils.parsePath(this.deterministic);
        DeterministicKey keys = kc.getKeyByPath(result, Boolean.TRUE);
        String chainCode = Utils.HEX.encode(keys.getChainCode());
        BigInteger tmp = keys.getPrivKey();
        byte[] array = tmp.toByteArray();
        String privateKey = Utils.HEX.encode(array);
        array = keys.getPubKey();
        String publicKey = Utils.HEX.encode(array);
        return new KeysBag(mnemonic, privateKey, publicKey, chainCode, timeCreating);
    }

    @Override
    public List<ChainAddress> addressesMain(KeysBag data, int start, int amount) {
        return this.getAddresses(data, 0, start, amount);
    }

    @Override
    public List<ChainAddress> addressesChange(KeysBag data, int start, int amount) {
        return this.getAddresses(data, 1, start, amount);
    }

    private List<ChainAddress> getAddresses(KeysBag data, int index, int start, int amount) {
        byte[] pubKey = Utils.HEX.decode(data.getPublicKey());
        byte[] chainCode = Utils.HEX.decode(data.getChainCode());
        ImmutableList<ChildNumber> pathList = ImmutableList.<ChildNumber>builder()
                .addAll(HDUtils.parsePath(this.deterministic)).build();
        ECKey publicOnly = DeterministicKey.fromPublicOnly(pubKey);
        DeterministicKey keys = new DeterministicKey(pathList, chainCode,
                publicOnly.getPubKeyPoint(), (BigInteger) null, (DeterministicKey) null);
        DeterministicKey parentKey = HDKeyDerivation
                .deriveChildKey(keys, new ChildNumber(index, Boolean.FALSE));
        return IntStream.rangeClosed(start, amount + start)
                .mapToObj(i -> this.createAddress(i, parentKey))
                .collect(Collectors.toList());
    }

    private ChainAddress createAddress(int index, DeterministicKey parentKey) {
        DeterministicKey childKey = HDKeyDerivation
                .deriveChildKey(parentKey, new ChildNumber(index, Boolean.FALSE));
        return new ChainAddress(index, LegacyAddress.fromPubKeyHash(this.netParams,
                childKey.getPubKeyHash()).toString());
    }

    @Override
    public void startWallet() {
        BriefLogFormatter.init();
        this.wallet.wrapStartAsync();
        this.wallet.wrapAwaitRun();
        this.wallet.addPeer(PEERS_NUMBER);
        BlockListener bl = new BlockListener(
                this.wallet,
                this.netParams,
                Executors.newFixedThreadPool(20, new BlockThreadFactory())
        );
        this.wallet.peerGroup();
        this.wallet.peerGroup().addBlocksDownloadedEventListener(bl);

    }

    @Override
    public void sendTrx(String trx, FutureCallback<Transaction> f) {
        byte[] array = Utils.HEX.decode(trx);
        Transaction t = new Transaction(this.netParams, array);
        SendRequest request = SendRequest.forTx(t);
        try {
            Wallet.SendResult result = this.wallet.wallet().sendCoins(request);
            Futures.addCallback(result.broadcastComplete, f, MoreExecutors.directExecutor());
        } catch (InsufficientMoneyException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String send(String trx) {
        byte[] array = Utils.HEX.decode(trx);
        Transaction t = new Transaction(this.netParams, array);
        SendRequest request = SendRequest.forTx(t);
        try {
            Wallet.SendResult result = this.wallet.wallet().sendCoins(request);
            Transaction resultTx = result.broadcastComplete.get(30, TimeUnit.SECONDS);
            return resultTx.getTxId().toString();
        } catch (InsufficientMoneyException | InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Enter: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public DeterministicKey
    restoreChildPrivateKey(String prKey, String chainCodeHex, int index, boolean isChange) {
        DeterministicKey childKey = getChildPrivate(prKey, chainCodeHex, isChange);
        childKey = HDKeyDerivation.deriveChildKey(childKey, new ChildNumber(index, Boolean.FALSE));
        return childKey;
    }

    @Override
    public BigDecimal amountRight(BigDecimal amount) {
        return amount.movePointRight(RATE);
    }

    @Override
    public BigDecimal amountLeft(BigDecimal amount) {
        return amount.movePointLeft(RATE);
    }

    private DeterministicKey
    getChildPrivate(String parentPrivKeyHex, String chainCodeHex, boolean isChange) {
        DeterministicKey childKey;
        if (isChange) {
            childKey = getChildKeyPairFromPrivate(
                    this.deterministic, parentPrivKeyHex, chainCodeHex, 1);
        } else {
            childKey = getChildKeyPairFromPrivate(
                    this.deterministic, parentPrivKeyHex, chainCodeHex, 0);
        }
        return childKey;
    }

    private DeterministicKey
    getChildKeyPairFromPrivate(String pathParent, String parentPrivKeyHex,
                               String chainCodeHex, int index) {
        DeterministicKey parentPrivateKey =
                getParentKeyFromPrivate(pathParent, parentPrivKeyHex, chainCodeHex);
        return HDKeyDerivation
                .deriveChildKey(parentPrivateKey, new ChildNumber(index, Boolean.FALSE));
    }

    private DeterministicKey
    getParentKeyFromPrivate(String pathParent, String parentPrivKeyHex, String chainCodeHex) {
        byte[] parentPrivKey = Utils.HEX.decode(parentPrivKeyHex);
        BigInteger parentBigKey = new BigInteger(parentPrivKey);
        ECKey parentECKey = ECKey.fromPrivate(parentPrivKey);
        byte[] chainCode = Utils.HEX.decode(chainCodeHex);
        ImmutableList<ChildNumber> pathList = ImmutableList.<ChildNumber>builder()
                .addAll(HDUtils.parsePath(pathParent)).build();
        return new DeterministicKey(pathList, chainCode,
                parentECKey.getPubKeyPoint(), parentBigKey, (DeterministicKey) null);
    }

}
