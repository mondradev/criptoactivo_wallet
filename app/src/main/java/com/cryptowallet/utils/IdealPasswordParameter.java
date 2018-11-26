package com.cryptowallet.utils;

import org.bitcoinj.crypto.KeyCrypterScrypt;

public class IdealPasswordParameter {
    public final int realIterations;
    public final long realTargetTime;

    public IdealPasswordParameter(String password) {
        final int targetTimeMsec = 2000;

        int iterations = 16384;
        KeyCrypterScrypt scrypt = new KeyCrypterScrypt(iterations);
        long now = System.currentTimeMillis();
        scrypt.deriveKey(password);
        long time = System.currentTimeMillis() - now;

        while (time > targetTimeMsec) {
            iterations >>= 1;
            time /= 2;
        }

        realIterations = iterations;

        realTargetTime = (long) (time * 1.1);
    }
}