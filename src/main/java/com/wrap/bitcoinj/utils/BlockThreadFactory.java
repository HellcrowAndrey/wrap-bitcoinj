package com.wrap.bitcoinj.utils;

import java.util.concurrent.ThreadFactory;

public class BlockThreadFactory implements ThreadFactory {

    private int count = 1;

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, "block-thread-" + count ++);
    }

}
