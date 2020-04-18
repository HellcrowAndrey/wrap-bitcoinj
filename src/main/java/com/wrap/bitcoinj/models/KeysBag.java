package com.wrap.bitcoinj.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeysBag {
    private String mnemonic;
    private String privateKey;
    private String publicKey;
    private String chainCode;
    private long timeCreating;

    public KeysBag(String publicKey, String chainCode) {
        this.publicKey = publicKey;
        this.chainCode = chainCode;
    }
}
