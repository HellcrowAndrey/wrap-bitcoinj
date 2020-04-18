package com.wrap.bitcoinj.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.wrap.bitcoinj.models.UnspentOutput;
import com.wrap.bitcoinj.utils.DeterministicPath;
import com.wrap.bitcoinj.utils.Network;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class Mocks {

    public static final String TRANSACTION_HASH =
            "962cde6f90030e36fc3facd8de8f62d3bd412e4f5cca4dce0946a320f1e3fec3";

    //====================================================
    //================ FROM ==============================
    //====================================================

    public static final String MNEMONIC_FROM =
            "hunt decline team erase day fox claim pet jar stable history baby";

    public static final String PRIVATE_KEY_FROM =
            "0089effa766f0ba79183599bea8ff7c11473a5fb2c7ca13d6c2b6655ab168451fe";

    public static final String PUBLIC_KEY_FROM =
            "03de1cc541cbd7dd7d7e795f7f30299a0360b21e533cb715967bb2fac8a46f9b4e";

    public static final String CHAIN_CODE_FROM =
            "f4efa44e94cf50849207ae94f4bff63ab0561391b428cd7f38977ec3142085cb";

    public static final String ADDRESS_FROM =
            "1MjUikzbBWpybJDgZ1NYvgedZMUCtNPVr7";

    public static final int INDEX_FROM = 0;

    //====================================================
    //================ TO ================================
    //====================================================

    public static final String MNEMONIC_TO =
            "cereal survey sugar edge legal fame mobile milk document exercise cage trap";

    public static final String PRIVATE_KEY_TO =
            "00a02efd8ff08d96d523121826547a9d89f52642b5e18a839491a412bd0ef802a2";

    public static final String PUBLIC_KEY_TO =
            "03f299171cc7545a108203a38fce08abe234dc53cd22660d3ff690e72671ca4cb7";

    public static final String CHAIN_CODE_TO =
            "ef85442357f82536ebb405588502e2960b3e38dbafed47d8bb85cac70f2dea71";

    public static final String ADDRESS_TO =
            "12EHp2okQfV7WcnRTfKWj3udGMayDm62ji";

    public static final int INDEX_TO = 0;

    //====================================================
    //================ Unspent Output ====================
    //====================================================

    public static List<UnspentOutput> unspentOutputs(String network) {
        NetworkParameters parameters = Network.chooser(network);
        byte[] chainCode = Utils.HEX.decode(CHAIN_CODE_FROM);
        Address a = LegacyAddress.fromBase58(parameters, ADDRESS_FROM);
        ImmutableList<ChildNumber> pathList = ImmutableList.<ChildNumber>builder()
                .addAll(HDUtils.parsePath(DeterministicPath.DETERMINISTIC_PATH_MAIN)).build();
        ECKey e = DeterministicKey.fromPublicOnly(Utils.HEX.decode(PUBLIC_KEY_FROM));
        DeterministicKey keys = new DeterministicKey(pathList, chainCode,
                e.getPubKeyPoint(), (BigInteger) null, (DeterministicKey) null);
        DeterministicKey parentKey = HDKeyDerivation
                .deriveChildKey(keys, new ChildNumber(0, Boolean.FALSE));
        DeterministicKey childKey = HDKeyDerivation
                .deriveChildKey(parentKey, new ChildNumber(0, Boolean.FALSE));
        Script script = ScriptBuilder.createP2PKHOutputScript(childKey.getPubKeyHash());
        UnspentOutput o = new UnspentOutput(
                ADDRESS_FROM,
                0,
                false,
                new BigDecimal("2000000"),
                0,
                Utils.HEX.encode(script.getProgram()),
                TRANSACTION_HASH,
                Utils.HEX.encode(script.getPubKeyHash())
        );
        return Lists.newArrayList(o);
    }

}
