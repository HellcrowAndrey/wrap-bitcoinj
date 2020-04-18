package com.wrap.bitcoinj.models;

import lombok.*;
import org.bitcoinj.core.Utils;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
@Builder
public class TransactionData {

    private static final int RADIX = 16;

    private long height;

    private String hashBlock;

    private String hashTrx;

    private int confirmation;

    private List<TInput> inputs;

    private List<TOutput> outputs;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionData data = (TransactionData) o;
        return height == data.height &&
                confirmation == data.confirmation &&
                Objects.equals(hashBlock, data.hashBlock) &&
                Objects.equals(hashTrx, data.hashTrx) &&
                Objects.equals(inputs, data.inputs) &&
                Objects.equals(outputs, data.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                height, hashBlock, hashTrx,
                confirmation, inputs, outputs
        );
    }

    @Override
    public String toString() {
        return "TransactionData{" +
                "height=" + height +
                ", hashBlock=" + hashBlock +
                ", hashTrx=" + hashTrx +
                ", confirmation=" + confirmation +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                '}';
    }

}
