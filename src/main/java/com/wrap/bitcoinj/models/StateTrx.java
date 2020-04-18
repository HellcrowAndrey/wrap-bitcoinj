package com.wrap.bitcoinj.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StateTrx {
    private List<TInput> inputs = new ArrayList<>();
    private List<TOutput> outputs = new ArrayList<>();
}
