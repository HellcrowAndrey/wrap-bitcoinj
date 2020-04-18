package com.wrap.bitcoinj.models;

import com.wrap.bitcoinj.utils.AddressType;

public interface FAddress extends Comparable<FAddress> {
    String getAddress();
    AddressType getType();
}
