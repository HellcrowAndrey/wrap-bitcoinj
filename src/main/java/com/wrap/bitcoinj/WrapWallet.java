package com.wrap.bitcoinj;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.KeyChainGroupStructure;

import javax.annotation.Nullable;
import java.io.File;

public class WrapWallet extends WalletAppKit {

    public WrapWallet(NetworkParameters params, File directory, String filePrefix) {
        super(params, directory, filePrefix);
    }

    public WrapWallet(NetworkParameters params, Script.ScriptType preferredOutputScriptType, @Nullable KeyChainGroupStructure structure, File directory, String filePrefix) {
        super(params, preferredOutputScriptType, structure, directory, filePrefix);
    }

    public WrapWallet(Context context, Script.ScriptType preferredOutputScriptType, @Nullable KeyChainGroupStructure structure, File directory, String filePrefix) {
        super(context, preferredOutputScriptType, structure, directory, filePrefix);
    }

    public void wrapStartAsync() {
        super.startAsync();
    }

    public void wrapAwaitRun() {
        super.awaitRunning();
    }

    public void addPeer(int number) {
        super.peerGroup().waitForPeers(number);
    }

}
