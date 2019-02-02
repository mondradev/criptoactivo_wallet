package com.cryptowallet.wallet.coinmarket.coins;

import com.cryptowallet.wallet.SupportedAssets;

public final class Mxn extends Fiat {
    public Mxn() {
        this(0);
    }

    public Mxn(long value) {
        super(SupportedAssets.MXN);
        setValue(value);
    }
}
