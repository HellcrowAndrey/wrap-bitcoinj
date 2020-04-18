package com.wrap.bitcoinj.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wrap.bitcoinj.exception.ErrorFee;
import com.wrap.bitcoinj.exception.NotEnoughMoney;
import com.wrap.bitcoinj.models.UnspentOutput;
import com.wrap.bitcoinj.utils.Network;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NewTransaction {

    private static final BigDecimal DEFAULT_FEE =
            new BigDecimal(Transaction.DEFAULT_TX_FEE.value);

    private BigDecimal totalAmount;

    private List<UnspentOutput> unspentOutputs;

    private Map<Integer, Boolean> indexes;

    private BigDecimal change;

    private BigDecimal fee;

    private String trxHex;

    private NewTransaction(RawBuilder b) {
        this.totalAmount = b.totalAmount;
        this.unspentOutputs = b.unspentOutputs;
        this.indexes = b.indexes;
        this.change = b.change;
        this.fee = b.fee;
        this.trxHex = b.trxHex;
    }

    private NewTransaction(SignBuilder sb) {
        this.trxHex = sb.signedTrx;
    }

    public static class RawBuilder {

        // Generate params.

        private NetworkParameters parameters;

        private IBitcoinFacade facade;

        private String addressFrom;

        private String addressTo;

        private String addressChange;

        private BigDecimal amount;

        private List<UnspentOutput> outputs;

        private BigDecimal feePerKb;

        //Transaction params

        private Transaction transaction;

        private BigDecimal totalAmount;

        private List<UnspentOutput> unspentOutputs;

        private BigDecimal change;

        private BigDecimal fee;

        private String trxHex;

        private Map<Integer, Boolean> indexes;

        //end

        public RawBuilder parameters(Network network, IBitcoinFacade facade) {
            this.parameters = network.get();
            this.facade = facade;
            return this;
        }

        public RawBuilder addressFrom(String addressFrom) {
            this.addressFrom = addressFrom;
            return this;
        }

        public RawBuilder addressTo(String addressTo) {
            this.addressTo = addressTo;
            return this;
        }

        public RawBuilder addressChange(String addressChange) {
            this.addressChange = addressChange;
            return this;
        }

        public RawBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public RawBuilder outputs(List<UnspentOutput> outputs) {
            this.outputs = outputs;
            return this;
        }

        public RawBuilder feePerKb(BigDecimal feePerKb) {
            this.feePerKb = feePerKb;
            return this;
        }

        public RawBuilder calcUnspentOutput() {
            calculation(DEFAULT_FEE);
            return this;
        }

        public RawBuilder transaction() {
            notSignTrx();
            BigDecimal fee = calculateFee();
            calculation(fee);
            notSignTrx();
            byte[] bytes = this.transaction.unsafeBitcoinSerialize();
            this.fee = fee;
            this.trxHex = hex(bytes);
            return this;
        }

        public RawBuilder transaction(String privateKey, String chainCode) {
            notSignTrx();
            BigDecimal fee = calculateFee();
            calculation(fee);
            signTrx(privateKey, chainCode);
            byte[] bytes = this.transaction.unsafeBitcoinSerialize();
            this.fee = fee;
            this.trxHex = hex(bytes);
            return this;
        }

        public NewTransaction build() {
            return new NewTransaction(this);
        }

        private void calculation(BigDecimal fee) {
            BigDecimal overFlow = BigDecimal.ZERO;
            this.totalAmount = this.facade.amountRight(this.amount).add(fee);
            this.unspentOutputs = Lists.newArrayList();
            for (UnspentOutput o : this.outputs) {
                overFlow = overFlow.add(o.getAmount());
                this.unspentOutputs.add(o);
                if (overFlow.doubleValue() >= this.totalAmount.doubleValue()) {
                    break;
                }
            }
            if (overFlow.doubleValue() < this.totalAmount.doubleValue()) {
                throw new NotEnoughMoney("Balance is not enough.");
            }
            this.change = overFlow.subtract(this.totalAmount);
        }

        private void notSignTrx() {
            this.transaction = new Transaction(this.parameters);
            this.indexes = Maps.newHashMap();
            Address addressTo = Address.fromString(this.parameters, this.addressTo);
            this.transaction.addOutput(Coin.parseCoin(
                    this.amount.toPlainString()), addressTo
            );
            if (change.doubleValue() != 0.0) {
                Address addressChange = chooseAddress();
                this.transaction.addOutput(
                        Coin.parseCoin(this.change.toPlainString()),
                        addressChange
                );
            }
            this.unspentOutputs.forEach(o -> {
                Sha256Hash sha256Hash = Sha256Hash.wrap(Utils.HEX.decode(o.getTxHash()));
                Script script = new Script(Utils.HEX.decode(o.getTxoutScriptPubKey()));
                this.indexes.put(o.getIndex(), o.isChange());
                this.transaction.addInput(sha256Hash, o.getIndex(), script);
            });
            this.transaction.setPurpose(Transaction.Purpose.USER_PAYMENT);
        }

        private void signTrx(String privateKey, String chainCode) {
            this.transaction = new Transaction(this.parameters);
            Address addressTo = Address.fromString(this.parameters, this.addressTo);
            this.transaction.addOutput(Coin.parseCoin(this.amount.toPlainString()), addressTo);
            if (Objects.nonNull(change) && change.doubleValue() != 0.0) {
                Address addressChange = chooseAddress();
                this.transaction.addOutput(
                        Coin.parseCoin(this.change.toPlainString()),
                        addressChange
                );
            }
            this.unspentOutputs.forEach(o -> {
                DeterministicKey key = this.facade.restoreChildPrivateKey(
                        privateKey, chainCode, o.getIndexChain(), o.isChange()
                );
                TransactionOutPoint outPoint = input(o);
                Script script = new Script(Utils.HEX.decode(o.getTxoutScriptPubKey()));
                this.transaction.addSignedInput(outPoint, script, key,
                        Transaction.SigHash.ALL, Boolean.TRUE);
            });
            this.transaction.setPurpose(Transaction.Purpose.USER_PAYMENT);
        }

        private BigDecimal calculateFee() {
            byte[] bytes = this.transaction.unsafeBitcoinSerialize();
            int txSizeKb = (int) Math.ceil(bytes.length / 1024.);
            BigDecimal fee = this.feePerKb.multiply(new BigDecimal(txSizeKb));
            if (fee.doubleValue() > DEFAULT_FEE.doubleValue()) {
                throw new ErrorFee("Not normal commission.");
            }
            return fee;
        }

        private String hex(byte[] bytes) {
            return Utils.HEX.encode(bytes);
        }

        private TransactionOutPoint input(UnspentOutput o) {
            Sha256Hash sha256Hash = Sha256Hash.wrap(Utils.HEX.decode(o.getTxHash()));
            return new TransactionOutPoint(this.parameters, o.getIndex(), sha256Hash);
        }

        private Address chooseAddress() {
            if (Objects.isNull(this.addressChange)) {
                return Address.fromString(this.parameters, this.addressFrom);
            } else {
                return Address.fromString(this.parameters, this.addressChange);
            }
        }

    }

    public static class SignBuilder {

        private String trxHex;

        private String privateKey;

        private String chainCode;

        private NetworkParameters parameters;

        private IBitcoinFacade facade;

        private String signedTrx;

        public SignBuilder(Network network, IBitcoinFacade facade) {
            this.parameters = network.get();
            this.facade = facade;
        }

        public SignBuilder keyPair(String privateKey, String chainCode) {
            this.privateKey = privateKey;
            this.chainCode = chainCode;
            return this;
        }

        public SignBuilder transaction(String trxHex) {
            this.trxHex = trxHex;
            return this;
        }

        public SignBuilder sign(Map<Integer, Boolean> indexes) {
            byte[] array = Utils.HEX.decode(this.trxHex);
            Transaction t = new Transaction(this.parameters, array);
            List<TransactionInput> inputs = Lists.newArrayList(t.getInputs());
            t.clearInputs();
            Map<String, DeterministicKey> keys = keys(indexes);
            signInputs(t, inputs, keys);
            byte[] bytes = t.unsafeBitcoinSerialize();
            this.signedTrx = Utils.HEX.encode(bytes);
            return this;
        }

        private void signInputs(Transaction t,
                                List<TransactionInput> inputs,
                                Map<String, DeterministicKey> keys) {
            inputs.forEach(i -> {
                TransactionOutPoint out = i.getOutpoint();
                Script s = i.getScriptSig();
                String address = address(s);
                DeterministicKey k = keys.get(address);
                t.addSignedInput(out, s, k, Transaction.SigHash.ALL, Boolean.TRUE);
            });
        }

        private Map<String, DeterministicKey> keys(Map<Integer, Boolean> indexes) {
            return indexes.entrySet().stream()
                    .map(e -> restore(e.getKey(), e.getValue()))
                    .collect(Collectors.toMap(this::address, Function.identity()));
        }

        private DeterministicKey restore(Integer index, Boolean type) {
            return this.facade.restoreChildPrivateKey(
                    this.privateKey, this.chainCode, index, type
            );
        }

        private String address(DeterministicKey k) {
            return LegacyAddress.fromPubKeyHash(
                    this.parameters, k.getPubKeyHash()).toBase58();
        }

        private String address(Script s) {
            byte[] hash = s.getPubKeyHash();
            return LegacyAddress.fromPubKeyHash(parameters, hash).toBase58();
        }

        public NewTransaction build() {
            return new NewTransaction(this);
        }

    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<UnspentOutput> getUnspentOutputs() {
        return unspentOutputs;
    }

    public BigDecimal getChange() {
        return change;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public String getTrxHex() {
        return trxHex;
    }

    public Map<Integer, Boolean> getIndexes() {
        return indexes;
    }
}
