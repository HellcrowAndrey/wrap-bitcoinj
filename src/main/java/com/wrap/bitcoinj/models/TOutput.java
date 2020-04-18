package com.wrap.bitcoinj.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TOutput {
    private String address;
    private int index;
    private String pubKeyHash;
    private String script;
    private long value;

}
