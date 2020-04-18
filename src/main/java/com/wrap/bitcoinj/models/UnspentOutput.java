package com.wrap.bitcoinj.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnspentOutput {
    private String address;
    private int indexChain;
    private boolean isChange;
    private BigDecimal amount;
    private Integer index;
    private String txoutScriptPubKey;
    private String txHash;
    private String pubkeyHash;
}
