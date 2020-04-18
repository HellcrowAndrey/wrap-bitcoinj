package com.wrap.bitcoinj;

import com.google.common.collect.ImmutableList;
import com.wrap.bitcoinj.config.BitcoinFacade;
import com.wrap.bitcoinj.exception.ErrorFee;
import com.wrap.bitcoinj.exception.NotEnoughMoney;
import com.wrap.bitcoinj.models.UnspentOutput;
import com.wrap.bitcoinj.utils.Network;
import lombok.Getter;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.script.Script;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Getter
public class TransactionBuilder {

    private String trxHex;

    private List<UnspentOutput> spend;

    public TransactionBuilder(Builder b) {
        this.trxHex = b.trxHex;
        this.spend = b.spendOutputs;
    }

    public static class Builder {

        private NetworkParameters parameters;

        private String deterministic;

        private Transaction transaction;

        private String addressTo;

        private String addressChange;

        private BigDecimal defaultFee;

        private BigDecimal amount;

        private BigDecimal feePerKb;

        private BigDecimal totalAmount;

        private BigDecimal amountFromOutput = new BigDecimal(0L);

        private BigDecimal overFlow = new BigDecimal(0L);

        private List<UnspentOutput> outputs;

        private Map<String, DeterministicKey> keys = new HashMap<>();

        private List<UnspentOutput> spendOutputs = new ArrayList<>();

        private String trxHex;

        public Builder(Network network, String deterministic) {
            this.parameters = network.get();
            this.deterministic = deterministic;
            this.transaction = new Transaction(parameters);
        }

        public Builder addressChange(String addressChange) {
            this.addressChange = addressChange;
            return this;
        }

        public Builder addressTo(String addressTo) {
            this.addressTo = addressTo;
            return this;
        }

        public Builder payment(BigDecimal amount, BigDecimal feePerKb) {
            this.defaultFee = new BigDecimal(Transaction.DEFAULT_TX_FEE.value);
            this.amount = amount;
            this.feePerKb = feePerKb;
            return this;
        }

        public Builder unspent(List<UnspentOutput> outputs) {
            this.outputs = outputs;
            return this;
        }

        public Builder calculator(String privateKey, String chainCode) throws NotEnoughMoney {
            Address address = Address.fromString(this.parameters, this.addressTo);
            this.transaction.addOutput(Coin.parseCoin(this.amount.toString()), address);
            this.totalAmount = this.amount.add(this.defaultFee);
            for (UnspentOutput o : this.outputs) {
                this.overFlow = this.overFlow.add(o.getAmount());
                DeterministicKey key = getChildPrivKey(privateKey, chainCode, o.getIndexChain(), o.isChange());
                this.keys.putIfAbsent(o.getAddress(), key);
                if (this.overFlow.doubleValue() >= this.totalAmount.doubleValue()) {
                    break;
                }
            }
            if (this.overFlow.doubleValue() < this.totalAmount.doubleValue()) {
                throw new NotEnoughMoney("You don't have money for this operation.");
            }
            BigDecimal delivery = this.overFlow.subtract(totalAmount);
            if (delivery.doubleValue() != 0.0) {
                Address change;
                if (Objects.isNull(this.addressChange)) {
                    change = Address.fromString(this.parameters, this.outputs.get(0).getAddress());
                } else {
                    change = Address.fromString(this.parameters, this.addressChange);
                }
                this.transaction.addOutput(Coin.parseCoin(delivery.toString()), change);
            }
            return this;
        }

        public Builder genSignTrx() {
            for (UnspentOutput o : this.outputs) {
                DeterministicKey k = this.keys.get(o.getAddress());
                if (k != null) {
                    Sha256Hash sha256Hash = Sha256Hash.wrap(Utils.HEX.decode(o.getTxHash()));
                    TransactionOutPoint outPoint =
                            new TransactionOutPoint(this.parameters, o.getIndex(), sha256Hash);

                    Script script = new Script(Utils.HEX.decode(o.getTxoutScriptPubKey()));
                    this.transaction.addSignedInput(outPoint, script, k,
                            Transaction.SigHash.ALL, Boolean.TRUE);

                    this.spendOutputs.add(o);
                    this.amountFromOutput = this.amountFromOutput.add(o.getAmount());
                }
                if (this.amountFromOutput.doubleValue() >= this.totalAmount.doubleValue()) {
                    break;
                }
            }
            this.transaction.setPurpose(Transaction.Purpose.USER_PAYMENT);
            byte[] bytes = this.transaction.unsafeBitcoinSerialize();
            int txSizeKb = (int) Math.ceil(bytes.length / 1024.);
            BigDecimal minFee = this.feePerKb.multiply(new BigDecimal(txSizeKb));
            if (minFee.doubleValue() > this.defaultFee.doubleValue()) {
                throw new ErrorFee("Not normal commission.");
            }
            this.trxHex = Utils.HEX.encode(bytes);
            return this;
        }

        public TransactionBuilder build() {
            return new TransactionBuilder(this);
        }

        DeterministicKey
        getChildPrivKey(String parentPrivKeyHex, String chainCodeHex, int index, boolean isChange) {
            DeterministicKey childKey = getChildPrivate(parentPrivKeyHex, chainCodeHex, isChange);
            childKey = HDKeyDerivation.deriveChildKey(childKey, new ChildNumber(index, Boolean.FALSE));
            return childKey;
        }

        DeterministicKey
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

        DeterministicKey
        getChildKeyPairFromPrivate(String pathParent, String parentPrivKeyHex,
                                   String chainCodeHex, int index) {
            DeterministicKey parentPrivateKey =
                    getParentKeyFromPrivate(pathParent, parentPrivKeyHex, chainCodeHex);
            return HDKeyDerivation
                    .deriveChildKey(parentPrivateKey, new ChildNumber(index, Boolean.FALSE));
        }

        DeterministicKey
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

}
