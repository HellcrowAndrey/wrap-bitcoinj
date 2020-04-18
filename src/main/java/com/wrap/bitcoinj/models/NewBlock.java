package com.wrap.bitcoinj.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewBlock {

    private long height;

    private String hash;

    private String prevHash;

}
